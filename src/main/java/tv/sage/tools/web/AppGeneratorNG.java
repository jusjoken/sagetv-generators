package tv.sage.tools.web;

import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateHashModel;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static tv.sage.tools.web.COMMON.get;
import static tv.sage.tools.web.COMMON.setIfEmpty;
import static tv.sage.tools.web.COMMON.text;

/**
 * Created by seans on 14/04/17.
 */
public class AppGeneratorNG
{
  final String templates;

  Configuration templateConfiguration;

  GeneratorContext ctx;
  Document document;

  File dirOut;
  File fileIn;

  private boolean debug_template = true;

  public AppGeneratorNG(String templates) {
    if (templates==null) templates="mdl";
    this.templates=templates;
    templateConfiguration = new Configuration(Configuration.VERSION_2_3_23);
    templateConfiguration.setDefaultEncoding("UTF-8");
    templateConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    templateConfiguration.setLogTemplateExceptions(false);
    templateConfiguration.setClassLoaderForTemplateLoading(AppGenerator.class.getClassLoader(), templates);
  }

  public GeneratorContext context() {
    return ctx;
  }

  public void generateApp(File in, File dirOut) throws JDOMException, IOException, InvocationTargetException, IllegalAccessException
  {
    System.out.println("Processing App: " + in);
    this.dirOut = dirOut;
    this.fileIn = in;

    // process the modules first
    File modulesDir = new File(fileIn.getParentFile(), "modules");
    System.out.println("Processing Modules in " + modulesDir);
    File files[] = modulesDir.listFiles((file, s) -> s.toLowerCase().endsWith(".xml"));

    GeneratorContext generatorContext = new GeneratorContext(this);
    this.ctx=generatorContext;

    if (files != null)
    {
      for (File f : files)
      {
        AppGeneratorNG g = new AppGeneratorNG(templates);
        g.generateModule(f, new File(dirOut, "modules"));
        Map<String,String> routes = g.context().routes();
        System.out.println(routes.size() + " ROUTES FROM MODULE " + f);
        routes.forEach((k,v)->{
          System.out.println("Adding Route " + k + " to app.");
          if (generatorContext.routes().put(k,v)!=null) {
            throw new RuntimeException("Duplicate Route for " + k);
          }
        });
        System.out.println();
      }
    }
    System.out.println("Done Processing Modules in " + modulesDir + " with " + generatorContext.routes().size() + " routes");


    SAXBuilder builder = new SAXBuilder();
    document = builder.build(in);
    Element rootEl=document.getRootElement();
    if (!"app".equals(rootEl.getName())) throw new RuntimeException("App " + in + " must use 'app' element as the root element");
    setIfEmpty(rootEl, "name", in.getName().substring(0, in.getName().lastIndexOf(".")));

    // we process the APP context without create a new context, so it can have access to it's children
    processElement(generatorContext, document.getRootElement(), false);

    File out = new File(dirOut, get(document.getRootElement(), "page", "index.html"));
    System.out.println("Writing App " + out);
    PrintWriter w = new PrintWriter(out);
    w.print(generatorContext.content().toString().trim());
    w.flush();
    w.close();
  }

  public void generateModule(File in, File dirOut) throws JDOMException, IOException, InvocationTargetException, IllegalAccessException
  {
    System.out.println("Processing Module: " + in);
    this.dirOut = dirOut;
    this.fileIn = in;

    SAXBuilder builder = new SAXBuilder();
    document = builder.build(in);
    Element rootEl=document.getRootElement();
    if (!"module".equals(rootEl.getName())) throw new RuntimeException("Module " + in + " must use 'module' element as the root element");

    setIfEmpty(rootEl, "name", in.getName().substring(0, in.getName().lastIndexOf(".")));
    GeneratorContext generatorContext = new GeneratorContext(this);
    this.ctx=generatorContext;
    processElement(generatorContext, rootEl);

    if (!dirOut.exists()) dirOut.mkdirs();
    File outFile = new File(dirOut, get(rootEl,"name")+".html");
    System.out.println("Writing: " + outFile);
    if (outFile.exists()) outFile.delete();
    PrintWriter w = new PrintWriter(outFile);
    w.print(generatorContext.content().toString().trim());
    w.flush();
    w.close();
  }

  public GeneratorContext processElement(GeneratorContext ctx, Element el) throws InvocationTargetException, IllegalAccessException
  {
    return processElement(ctx, el, true);
  }

  public GeneratorContext processElement(GeneratorContext ctx, Element el, boolean newContext) throws InvocationTargetException, IllegalAccessException
  {
    if (el==null) return ctx;

    GeneratorContext ctxNew = newContext?ctx.createNew():ctx;

    Method m = getMethod(JS.functionName(el.getName()));
    if (m != null)
    {
      m.invoke(ctxNew, el);
    } else
    {
      // process templates in our context
      processTemplate(ctxNew, el, false);
    }

    if (newContext) ctx.merge(ctxNew);

    return ctx;
  }

  public GeneratorContext processChildren(GeneratorContext ctx, Element el) throws InvocationTargetException, IllegalAccessException
  {
    if (el==null) return ctx;
    System.out.println("Processing Children " + el);
    return processElements(ctx, el.getChildren());
  }

  public GeneratorContext processElements(GeneratorContext ctx, Collection<Element> els) throws InvocationTargetException, IllegalAccessException
  {
    if (els==null || els.size()==0) return ctx;
    StringBuilder content = new StringBuilder();
    StringBuilder script = new StringBuilder();

    GeneratorContext ctxNew = ctx.createNew();
    //ctxNew.contentMergeType(GeneratorContext.MergeType.Append);

    for (Element ch: els) {
      processElement(ctxNew, ch);
      if (ctxNew.content().toString().trim().length()>0)
      {
        content.append(ctxNew.content()+"\n");
      }
      if (ctxNew.script().toString().trim().length()>0)
      {
        script.append(ctxNew.script()+"\n");
      }
    }

    ctx.merge(ctxNew);

    if (content.toString().trim().length()>0)
    {
      ctx.content(content.toString());
    }
    if (script.toString().trim().length()>0)
    {
      ctx.script(script.toString());
    }
    return ctx;
  }


  GeneratorContext processTemplate(GeneratorContext ctx, Element el)
  {
    return processTemplate(ctx, el, true);
  }

  GeneratorContext processTemplate(GeneratorContext ctx, Element el, boolean newContext) {
    try
    {
      GeneratorContext ctxNew = newContext?ctx.createNew():ctx;

      Template template = templateConfiguration.getTemplate(el.getName() + ".ftl");
      Map<String, Object> args = new HashMap<>();
      args.put("el", el);
      args.put("ctx", ctxNew);

      BeansWrapper wrapper = BeansWrapper.getDefaultInstance();
      TemplateHashModel staticModels = wrapper.getStaticModels();
      TemplateHashModel commonStatics =
        (TemplateHashModel) staticModels.get(StaticUtils.class.getName());
      TemplateHashModel systemStatics =
        (TemplateHashModel) staticModels.get(System.class.getName());

      args.put("util", commonStatics);
      args.put("System", systemStatics);

      StringWriter out = new StringWriter();
      template.process(args, out);

      updateContextFromTemplate(ctxNew, out.getBuffer().toString());
      if (newContext) ctx.merge(ctxNew);
    }
    catch (IOException e)
    {
      if (e.getMessage().contains("ParseException") || e.getMessage().contains("Syntax")) throw new RuntimeException(e);
      System.out.println("Unhandled Element: " + el.getName() + " in xml");
      unhandledTags.add(el.getName());
    } catch (TemplateException e)
    {
      throw new RuntimeException("Template Error in " + el.getName(), e);
    } catch (JDOMException e)
    {
      throw new RuntimeException("JDOM Error in " + el.getName(), e);
    }

    return ctx;
  }

  private void updateContextFromTemplate(GeneratorContext ctx, String buffer) throws JDOMException, IOException
  {
    SAXBuilder builder = new SAXBuilder();
    document = builder.build(new StringReader(buffer));
    Element rootEl=document.getRootElement();

    boolean debug = "true".equals(get(rootEl, "debug", "false"));

    for (Element el: rootEl.getChildren("js")) {
      ctx.js(get(el, "url"));
    }
    for (Element el: rootEl.getChildren("css")) {
      ctx.css(get(el, "url"));
    }
    for (Element el: rootEl.getChildren("jsLazy")) {
      ctx.jsLazy(get(el, "url"));
    }
    for (Element el: rootEl.getChildren("require")) {
      ctx.require(get(el, "name"), get(el, "url"));
    }

    Element style = rootEl.getChild("style");
    if (style!=null) {
      ctx.style(text(style));
    }

    Element script = rootEl.getChild("script");
    if (script!=null) {
      ctx.script(text(script));
      if (debug) {
        System.out.println("Begin SCRIPT");
        System.out.println(ctx.script());
        System.out.println("End SCRIPT");
      }
    }

    Element template = rootEl.getChild("template");
    if (template!=null) {
      String text = text(template);
      if (text.trim().length()==0) {
        if (template.getChildren().size()>0) {
          throw new RuntimeException("Template Sections should be in <![CDATA[ ]]> tag");
        }
      }
      ctx.template(text);
      if (debug) {
        System.out.println("Begin TEMPLATE");
        System.out.println(ctx.templates());
        System.out.println("End TEMPLATE");
      }
    }

    Element content = rootEl.getChild("content");
    if (content!=null) {
      if (content.getChildren().size()>0) {
       // <content> blocks should have CDATA wrapper, but if not, then serialize the contents
        XMLOutputter outp = new XMLOutputter();
        String s = outp.outputElementContentString(content);
        ctx.content(s);
      } else
      {
        ctx.content(text(content));
      }
      if (debug) {
        System.out.println("Begin CONTENT");
        System.out.println(ctx.content());
        System.out.println("End CONTENT");
      }
    }
  }

  private Method getMethod(String name)
  {
    name = JS.functionName(name);
    try
    {
      return this.getClass().getMethod("visit_" + name, GeneratorContext.class, Element.class);
    } catch (NoSuchMethodException e)
    {
      return null;
    }
  }


  // manage set of unhandled elements for reporting at end
  static Set<String> unhandledTags = new TreeSet<>();

  public static void main(String args[]) throws JDOMException, InvocationTargetException, IllegalAccessException, IOException
  {
    if (args.length != 3)
    {
      System.out.println("Generates SageTV Web Modules from Web Module Xml");
      System.out.println("Usage: java AppModuleGenerator xmlInput html_output_dir [templates (default 'mdl')]");
      return;
    }

    File xml = new File(args[0]);
    if (!xml.exists()) {
      throw new RuntimeException("Not Found: " + xml.getAbsolutePath());
    }

    File outDir = new File(args[1]);
    if (!outDir.exists()) {
      System.out.println("Creating Ouput Dir: " + outDir.getAbsolutePath());
    }

    String templates = args[2];

    AppGeneratorNG gen = new AppGeneratorNG(templates);
    gen.generateApp(xml, outDir);

    if (unhandledTags.size()>0)
    {
      System.out.println("\n\nUnhandled Tags");
      for (String s: unhandledTags) {
        System.out.println("TAG: "+s);
      }
    }
  }
}
