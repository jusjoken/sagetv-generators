package tv.sage.tools.web;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static tv.sage.tools.web.COMMON.*;

/**
 * Created by seans on 26/03/17.
 */
public class AppGenerator
{
  Configuration templateConfiguration;

  Document document;
  List<StringBuilder> scripts = new ArrayList<>();
  List<StringBuilder> templates = new ArrayList<>();
  List<StringBuilder> body = new ArrayList<>();
  Map<String, String> requires = new HashMap<>();
  List<String> inlineJSLibs = new ArrayList<>();
  List<String> css = new ArrayList<>();

  List<Document> modules = new ArrayList<>();
  Map<String,String> routes = new LinkedHashMap<>();

  private File dirOut;
  private File fileIn;

  Map<String, Element> actions = new HashMap<>();
  Map<String, Element> dataSources = new HashMap<>();

  boolean logging = true;

  public AppGenerator()
  {
    templateConfiguration = new Configuration(Configuration.VERSION_2_3_23);
    templateConfiguration.setDefaultEncoding("UTF-8");
    templateConfiguration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    templateConfiguration.setLogTemplateExceptions(false);
    templateConfiguration.setClassLoaderForTemplateLoading(AppGenerator.class.getClassLoader(), "mdl");
  }

  // NEED to figure out a way to do automatic data binding {{value}} when it changes, then update the dom
  // we have html, so we need to dynamically update parts of it when it changes.


  public void generateApp(File in, File dirOut) throws JDOMException, IOException, InvocationTargetException, IllegalAccessException
  {
    System.out.println("Processing App: " + in);
    this.dirOut = dirOut;
    this.fileIn = in;
    SAXBuilder builder = new SAXBuilder();
    document = builder.build(in);
    Element rootEl=document.getRootElement();
    if (!"app".equals(rootEl.getName())) throw new RuntimeException("App " + in + " must use 'app' element as the root element");
    setIfEmpty(rootEl, "name", in.getName().substring(0, in.getName().lastIndexOf(".")));
    processElement(document.getRootElement());
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
    processElement(document.getRootElement());

    // keep track of the modules, we'll need it later
    modules.add(document);
  }

  private void writeModule(Document document) throws IOException
  {
    StringBuilder html = new StringBuilder();
    html.append("<div class='module-body'>\n");
    if (templates.size() > 0)
    {
      for (StringBuilder s : templates)
      {
        html.append(s.toString()).append("\n");
      }
    }

    if (body.size() > 0)
    {
      for (StringBuilder s : body)
      {
        html.append(s.toString()).append("\n");
      }
    }

    if (scripts.size() > 0)
    {
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      writeScripts(document.getRootElement(), pw);
      html.append(sw.getBuffer().toString());
    }
    html.append("</div>\n");

    // write the output
    if (!dirOut.exists()) dirOut.mkdirs();
    File outFile = new File(dirOut, get(document.getRootElement(),"name")+".html");
    System.out.println("Writing: " + outFile);
    if (outFile.exists()) outFile.delete();
    Files.write(Paths.get(outFile.toURI()), html.toString().getBytes(), StandardOpenOption.CREATE_NEW);
    //System.out.println(html.toString());
  }

  void writeScripts(Element el, PrintWriter html) {
    if (scripts.size() > 0)
    {
      html.append("<script language='JavaScript'>\n");

      if (isChildOf(el, "app")) {
        // need to add in the application handler
        // require js stuff
        html.println("requirejs.config({\n" +
          "    baseUrl: '.',\n" +
          "    paths: {\n" +
          "        root: '',\n" +
          "        js: 'js',\n" +
          "        lib: 'lib'\n" +
          "    }\n" +
          "});\n");
      }

      List<String> names = new ArrayList<>(requires.keySet());
      html.append("require([\n");
      if (names.size() > 0)
      {
        for (int i = 0; i < names.size(); i++)
        {
          if (i > 0) html.append(",");
          html.append("'" + requires.get(names.get(i)) + "'");
        }
      }
      html.append("\n],\n");
      html.append("function(");
      for (int i = 0; i < names.size(); i++)
      {
        if (i > 0) html.append(",");
        html.append(names.get(i));
      }
      html.append("){\n");

      // dump the scripts...
      for (StringBuilder s : scripts)
      {
        html.append(s).append("\n");
      }

      html.append("});\n");
      html.append("</script>");
    }
  }


  /**
   * Processing an 'app' tag writes the main app to either index.html or the page attribute.
   *
   * @param el
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   */
  public void visit_app(Element el) throws InvocationTargetException, IllegalAccessException, IOException, JDOMException
  {
    System.out.println("Processing App");

    // before we process the app, let's process the modules, so that we can collect information
    // about routing

    File modulesDir = new File(fileIn.getParentFile(), "modules");
    System.out.println("Processing Modules in " + modulesDir);
    File files[] = modulesDir.listFiles((file, s) -> s.toLowerCase().endsWith(".xml"));

    if (files != null)
    {
      for (File f : files)
      {
        AppGenerator g = new AppGenerator();
        g.generateModule(f, new File(dirOut, "modules"));
        this.routes.putAll(g.routes);
      }
    }
    System.out.println("Done Processing Modules in " + modulesDir);

    // now that we know about the modules, let's generate the app shell

    Element sideNav = child(el, "side-nav");
    Element headerNav = child(el, "nav-header");
    StringBuilder styles = new StringBuilder(" mdl-layout mdl-js-layout ");
    if (sideNav != null)
    {
      if ("true".equals(get(sideNav, "fixed", "true")))
      {
        styles.append(" mdl-layout--fixed-drawer ");
      } else
      {
        styles.append(" mdl-layout--drawer ");
      }
    }
    if (headerNav != null)
    {
      if ("true".equals(get(headerNav, "fixed", "true")))
      {
        styles.append(" mdl-layout--fixed-header ");
      } else
      {
        styles.append(" mdl-layout--header ");
      }
    }
    body.add(appendln("<div class='", styles.toString(), "'>"));
    processChildren(el);
    body.add(appendln("</div>"));

    css.add("https://fonts.googleapis.com/icon?family=Material+Icons");
    css.add("mdl/material.min.css");
    css.add("lib/animate.min.css");
    css.add("app.css");

    inlineJSLibs.add("mdl/material.min.js");
    inlineJSLibs.add("lib/jquery-3.2.0.min.js");
    inlineJSLibs.add("lib/linq.min.js");
    inlineJSLibs.add("lib/eventbus.min.js");
    inlineJSLibs.add("lib/dialog-polyfill.js");
    inlineJSLibs.add("lib/require.js");

    // for development
    if (System.getenv("SAGE_API")!=null) {
      scripts.add(appendln(" // DEV MODE ONLY, SHOULD NOT BE IN PRODUCTION"));
      scripts.add(appendln("window.SageTVAPIBaseUrl=\"", System.getenv("SAGE_API") ,"\";\n"));
    }

    // add in polyfills
    scripts.add(appendln("    if (!window.fetch) {\n" +
      "        var fetchImpl = require('lib/fetch');\n" +
      "        window.fetch=fetchImpl;\n" +
      "    }\n"));
    scripts.add(appendln("    if (!window.Promise) {\n" +
      "        var promiseImpl = require('lib/promise.min');\n" +
      "        window.Promise = promiseImpl;\n" +
      "    }\n"));

    scripts.add(appendln("console.log('App Started')"));

    // configure page routing...
    // scripts.add(appendln("var page = require(\"lib/page\");"));
    requires.put("page", "lib/page");
    scripts.add(appendln("var content = $('#content');"));

    // main page
    scripts.add(appendln("    page('/', function() {\n" +
      "        page.redirect('", get(el, "default-route", "/"), "');\n" +
      "    });\n"));

    // add in all the module routes
    for (Map.Entry<String, String> r : routes.entrySet())
    {
      scripts.add(appendln("page('", r.getKey(), "', function() {"));
      scripts.add(appendln("   console.log('Loading ",r.getValue(),"');"));
      scripts.add(appendln("fetch(\"", r.getValue(), "\").then(function(result) {"));

      scripts.add(appendln("  if (!result.ok) {"));
      scripts.add(appendln("    throw \"Load Failed\""));
      scripts.add(appendln("  }"));
      scripts.add(appendln("  return result.text();"));
      scripts.add(appendln("}).then(function(html) {"));
      scripts.add(appendln("  $(content).html(html);"));
      scripts.add(appendln("}).catch(function(error) {"));
      scripts.add(appendln("  console.log(\"LOAD FAILED\", error);"));
      scripts.add(appendln("  $(content).html(\"Unable to load ",r.getValue(),"\");"));
      scripts.add(appendln("});"));
      scripts.add(appendln("});"));
    }

    // add in default page config
    scripts.add(appendln("    page('*', function(ctx) {\n" +
      "        $(content).html(\"PAGE: NOT HANDLED\" + ctx.path);\n" +
      "    });\n" +
      "\n" +
      "    page({\n" +
      "        hashbang: true\n" +
      "    });\n"));


    // write it out
    writeApp(el);
  }

//  public void visit_nav(Element el) throws InvocationTargetException, IllegalAccessException {
//    processChildren(el);
//  }

  public void visit_nav_header(Element el) throws InvocationTargetException, IllegalAccessException, TemplateException
  {
    body.add(appendln("<header class=\"mdl-layout__header\">"));
    body.add(appendln("<div class=\"mdl-layout__header-row\">"));
    Element brand = el.getChild("brand");
    List<Element> buttons = el.getChildren("button");
    if (brand!=null) {
      processElement(brand);
      body.add(appendln("<div class=\"mdl-layout-spacer\"></div>"));
    }
    if (buttons.size()>0) {
      body.add(appendln("<nav class=\"mdl-navigation\">"));
      for (Element e: buttons) processElement(e);
      body.add(appendln("</nav>"));
    }
    body.add(appendln("</div>"));
    body.add(appendln("</header>"));
  }

  public void visit_side_nav(Element el) throws InvocationTargetException, IllegalAccessException {
    body.add(appendln("<div class=\"mdl-layout__drawer\">"));
    processChildren(el);
    body.add(appendln("</div>"));
  }

  public void visit_section(Element el) throws InvocationTargetException, IllegalAccessException {
    boolean onSideNav = isChildOf(el, "side-nav");
    StringBuilder styles = new StringBuilder();
    if (onSideNav) styles.append("  ");
    if (onSideNav && hasChild(el, "button")) {
      body.add(appendln("<nav class=\"mdl-navigation\">"));
      if (has(el, "title")) {
        body.add(appendln("<h2>",get(el, "title"), "</h2>"));
      }
      processChildren(el);
      body.add(appendln("</nav>"));
    }
  }

  public void visit_brand(Element el) throws InvocationTargetException, IllegalAccessException {
    body.add(appendln("<span class=\"mdl-layout-title\">",el.getText(),"</span>"));
  }

  private void writeApp(Element el) throws FileNotFoundException
  {
    File out = new File(dirOut, get(el, "page", "index.html"));
    System.out.println("Writing App " + out);
    PrintWriter w = new PrintWriter(out);

    w.println("<!doctype html>");
    w.println("<html lang=\"en\">");
    w.println("<head>");
    w.printf(appendln("<title>",get(el, "title", "Application"),"</title>").toString());
    w.printf("<meta name=\"x-generator\" content=\"SageTV\">\n");
    w.printf("<meta name=\"x-generator-datetime\" content=\"%s\">\n", DateFormat.getDateTimeInstance().format(new Date()));

    w.println("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0, minimum-scale=1.0\">");

    // dump css
    for (String c: css) {
      w.printf("<link rel=\"stylesheet\" href=\"%s\">\n", c);
    }

    w.println("</head>");

    w.println("<body>");

    // build the body
    for (StringBuilder s: templates) {
      w.println(s.toString());
    }

    for (StringBuilder s: body) {
      w.println(s.toString());
    }

    // dump js libs
    for (String js: inlineJSLibs) {
      w.printf("<script src=\"%s\"></script>\n", js);
    }

    writeScripts(el, w);

    w.println("</body>");
    w.println("</html>");

    w.flush();
    w.close();
  }

  private void processElement(Element el) throws InvocationTargetException, IllegalAccessException
  {
    if (el==null) return;

    Method m = getMethod(JS.functionName(el.getName()));
    if (m != null)
    {
      m.invoke(this, el);
      return;
    }

    try
    {
      Template template = templateConfiguration.getTemplate(el.getName()+".ftl");
      Map<String, Object> args = new HashMap<>();
      args.put("el", el);
      StringWriter out = new StringWriter();
      template.process(args, out);
      System.out.println(out.toString());
      System.out.println("Processed " + el.getName() + ".ftl template");
    } catch (IOException e)
    {
      System.out.println("Unhandled Element: " + el.getName() + " in xml");
    } catch (TemplateException e)
    {
      throw new RuntimeException(e);
    }

  }

  void processChildren(Element el) throws InvocationTargetException, IllegalAccessException
  {
    if (el==null) return;
    List<Element> children = el.getChildren();
    if (children != null && children.size() > 0)
    {
      for (int i = 0; i < children.size(); i++)
      {
        processElement(children.get(i));
      }
    }
  }

  private Method getMethod(String name)
  {
    name = JS.functionName(name);
    try
    {
      return this.getClass().getMethod("visit_" + name, Element.class);
    } catch (NoSuchMethodException e)
    {
      return null;
    }
  }

  public void visit_module(Element el) throws InvocationTargetException, IllegalAccessException, IOException
  {
    System.out.println("Module: " + fileIn);

//    // TODO: We should build a module structure and post that
//    scripts.add(appendln("EventBus.dispatch(\"on-set-app-title\", null, '",
//      js(el.getAttributeValue("title")), "');"));

    processChildren(el);

    writeModule(document);
  }

  public void visit_actions(Element el) throws InvocationTargetException, IllegalAccessException
  {
    processChildren(el);
  }

  public void visit_data_sources(Element el) throws InvocationTargetException, IllegalAccessException
  {
    // data sources are just holder element for now
    processChildren(el);
  }

  String datasourceFunctionName(String name)
  {
    return JS.functionName("get_datasource_" + name);
  }

  public void visit_data_source(Element el) throws InvocationTargetException, IllegalAccessException
  {
    // a data source will setup a js function that return a promise
    String name = datasourceFunctionName(JS.get(el, "name"));
    if (dataSources.containsKey(JS.get(el, "name")))
    {
      throw new RuntimeException("Duplicate Data Source " + JS.get(el, "name"));
    }
    dataSources.put(JS.get(el, "name"), el);
    scripts.add(appendln("function ", name, "() {"));
    processChildren(el);
    scripts.add(appendln("}"));
  }

  String getActionFunctionName(Element el) {
    return getActionFunctionName(get(el, "action"));
  }

  String getActionFunctionName(String name) {
    return JS.functionName(name);
  }

  public void visit_action(Element el) throws InvocationTargetException, IllegalAccessException
  {
    String name = getActionFunctionName(el);

    if (actions.containsKey(el.getAttributeValue("action")))
    {
      throw new RuntimeException("Action: " + el.getAttributeValue("action") + " already defined");
    }
    actions.put(el.getAttributeValue("action"), el);

    scripts.add(appendln("function ", name, "(", (onList(el) ? "list, item" : ""), ") {"));
    processChildren(el);
    scripts.add(appendln("}"));
  }

  public void visit_content(Element el) throws InvocationTargetException, IllegalAccessException {
    body.add(appendln("<main class=\"mdl-layout__content\"><div id=\"content\">"));
    processChildren(el);
    body.add(appendln("</div></main>"));
  }

  public void visit_page(Element el) throws InvocationTargetException, IllegalAccessException
  {
    // map the route
    routes.put(get(el, "route"), "/modules/"+get(document.getRootElement(),"name")+".html");
    processChildren(el);
  }

  public void visit_button_group(Element el) throws InvocationTargetException, IllegalAccessException
  {
    body.add(appendln("<div class='button-group'>"));
    processChildren(el);
    body.add(appendln("</div>"));
  }

  public void visit_icon(Element el) {
    if (has(el, "icon")) {
      body.add(append("<i class='material-icons'>",get(el, "icon"),"</i>"));
    }
  }


  String getButtonActionClassName(Element action) {
    return JS.functionName("btn_", action, "action");
  }

  public void visit_button(Element el) throws InvocationTargetException, IllegalAccessException
  {
    //<a class="waves-effect btn btn-dismiss-all">Dismiss All</a>
    String btntag = null;
    String text = el.getAttributeValue("text");
    Element a = actions.get(el.getAttributeValue("action"));
    String path = el.getAttributeValue("path");
    if (a == null && path == null)
    {
      throw new RuntimeException("Missing Button Action " + el.getAttributeValue("action"));
    }

    if (text == null && a != null)
    {
      text = a.getAttributeValue("title");
    }

    boolean hasAction = a!=null;
    boolean inModule = isChildOf(el, "module");
    boolean onCard = isChildOf(el, "card");
    boolean onNav = isChildOf(el, "nav");
    boolean onNavHeader = isChildOf(el, "nav-header");
    boolean onSideNav = isChildOf(el, "side-nav");
    boolean isLink = !StringUtils.isEmpty(path);
    boolean hasBadge = hasChild(el, "badge");
    if (a!=null)
    {
      btntag = getButtonActionClassName(el);
    }

    if (isLink || onNav) {
      StringBuilder styles = new StringBuilder();
      if (onNav) styles.append(" mdl-navigation__link ");
      if (onNavHeader)
      {
        if (hidePhone(el)) styles.append(" mdl-cell--hide-phone ");
      }
      if (!hasAction)
      {
        body.add(append("<a class=\"", styles.toString(), "\" href=\"", path, "\">"));
      } else {
        styles = new StringBuilder();
        styles.append(" ").append(btntag);
        styles.append(" mdl-button mdl-js-button mdl-button--raised mdl-button--colored ");
        body.add(append("<a class=\"", styles.toString(), "\">"));
      }
      visit_icon(el);
      // if it has a badge the badge will render the text
      if (!hasBadge)
      {
        if (!StringUtils.isEmpty(text))
        {
          body.add(new StringBuilder(text));
        }
      }
      processChildren(el);
      body.add(appendln("</a>"));
    } else {
      body.add(appendln("<a class=\"mdl-button mdl-button--colored ", (!onCard)?"mdl-button--raised":"", " mdl-js-button mdl-js-ripple-effect ", btntag, "\">", text, "</a>"));
    }

    if (!onList(el) && a!=null) {
      // buttons in lists are managed differently, but we need to bind these
      StringBuilder sb = new StringBuilder();
      appendln(sb, "$('.",btntag,"').click(",getActionFunctionName(el),");");
      scripts.add(sb);
    }
  }


  public void visit_confirm(Element el) throws InvocationTargetException, IllegalAccessException
  {
    // create a confirmation dialog and process the action

    String actionId = getParentActionId(el);
    String modalId = actionId + "_modal";

    // build the html...
    StringBuilder sb = new StringBuilder();

    appendln(sb, "<dialog id=\"", modalId, "\" class=\"mdl-dialog mdl-cell--6-col\">");
    if (has(el, "title")) {
      appendln(sb, "<h4 class=\"mdl-dialog__title\">",HTML.escape(get(el, "title")),"</h4>");
    }
    appendln(sb, "<div class=\"mdl-dialog__content\">");
    appendln(sb, "<p>", HTML.escape(el, "msg") , "</p>");
    appendln(sb, "</div>");
    appendln(sb, "<div class=\"mdl-dialog__actions\">");
    appendln(sb, "<button type=\"button\" class=\"mdl-button btn-yes\">",HTML.escape(el, "positive-button"),"</button>");
    appendln(sb, "<button type=\"button\" class=\"mdl-button btn-no close\">",HTML.escape(el, "negative-button"),"</button>");
    appendln(sb, "</div>");
    appendln(sb, "</dialog>");

    templates.add(sb);

    // build the js function
    StringBuilder js = new StringBuilder();
    appendln(js, "  var m = document.querySelector('#",modalId,"');");
    //appendln(js, "  componentHandler.upgradeElement(m);");
    appendln(js, "  $(m).find(\".btn-no\").click(function() {m.close()});");
    appendln(js, "  $(m).find(\".btn-yes\").click(function() {" +
      "m.close();");

    scripts.add(js);

    Element pos = el.getChild("positive");
    if (pos != null)
    {
      processChildren(pos);
    }

    js = new StringBuilder();
    appendln(js, "});");
    appendln(js, "if (! m.showModal) {");
    appendln(js, "  dialogPolyfill.registerDialog(m);");
    appendln(js, "}");
    appendln(js, "m.showModal();");

    scripts.add(js);
  }

  public void visit_script(Element el) {
    scripts.add(appendln(text(el)));
  }

  AtomicInteger toasts = new AtomicInteger(0);
  public void visit_toast(Element el) {
    String id = "toast" + toasts.incrementAndGet();
    body.add(appendln("<div id=\"",id,"\" class=\"mdl-js-snackbar mdl-snackbar\">"));
    body.add(appendln("<div class=\"mdl-snackbar__text\"></div>"));
    // note no button action but MDL meeds it for a toast
    body.add(appendln("<button class=\"mdl-snackbar__action\" type=\"button\"></button>"));
    body.add(appendln("</div>"));

    String msg = JS.escape(get(el, "message"));

    scripts.add(appendln("componentHandler.upgradeElement(document.querySelector('#",id,"'));"));
    scripts.add(appendln("document.querySelector('#",id,"').MaterialSnackbar.showSnackbar({message: '",msg,"'});"));
  }

  public void visit_event_handlers(Element el) throws InvocationTargetException, IllegalAccessException
  {
    processChildren(el);
  }

  String id(Element el) {
    String id = get(el, "id", "");
    if (id.isEmpty()) {
      // generate id...
      id=String.valueOf(el.hashCode());
    }
    return id;
  }

  public void visit_badge(Element el) {
    String text = get(el.getParentElement(), "text", "");
    body.add(appendln("<span id='",id(el),"' class='mdl-badge' data-badge='",get(el, "text",""),"'>",text,"</span>"));
  }

  public void visit_event_handler(Element el) throws InvocationTargetException, IllegalAccessException
  {
    scripts.add(appendln("EventBus.addEventListener('",get(el, "name"),"', function(",get(el, "event-arg","event"),", ",get(el, "data-arg", "data"),") {"));
    processChildren(el);
    scripts.add(appendln("});"));
  }


  public void visit_event_push(Element el) {
    scripts.add(appendln("EventBus.dispatch(\"",get(el, "event"),"\", null, ",get(el, "value","true"),");"));
  }

  public void visit_sagetv(Element el) throws InvocationTargetException, IllegalAccessException, TemplateException
  {
    requires.put("SageTVAPI", "js/SageTVAPI");

    String service = el.getAttributeValue("service");
    requires.put(service+"API", "js/"+ service+"API");

    String api = el.getAttributeValue("api");
    StringBuilder js = new StringBuilder();
    appendln(js, "var api = new ",service,"API(new SageTVAPI());");

    appendln(js, "return ", "api.", api, "(",getArgs(el),").then(function(result) {");

    Element filter = el.getChild("filter");
    Element order = el.getChild("order");
    if (filter!=null||order!=null) {
      appendln(js, "result = Enumerable.from(result)");
      if (filter!=null) {
        appendln(js, ".where(function (x) {");
        appendln(js, "   return x.",get(filter, "by"),";");
        appendln(js, "})");
      }
      if (order!=null) {
        if ("true".equalsIgnoreCase(get(order, "desc", "false"))) {
          appendln(js, ".orderByDescending(function (x) {");
        } else
        {
          appendln(js, ".orderBy(function (x) {");
        }
        appendln(js, "return x.",get(order, "by"),";");
        appendln(js, "})");
      }
      appendln(js, ".toArray();");
    }

    scripts.add(js);

    // note will only process sucess block OR on-success-toast attribute, not both
    Element ok = el.getChild("success");
    if (ok!=null) {
      processChildren(ok);
    } else {
      String msg = get(el, "on-success-toast", "");
      if (!StringUtils.isEmpty(msg)) {
        Element t = new Element("toast");
        t.setAttribute("message", msg);
        processElement(t);
      }
    }
    js = new StringBuilder();
    appendln(js, "  return result;");
    appendln(js, "}).catch(function (error) {");
    scripts.add(js);
    js = new StringBuilder();
    Element err = el.getChild("failure");
    if (err!=null) {
      processChildren(err);
    } else
    {
      appendln(js, "  console.log(\"ERROR: \", error);");
    }
    appendln(js, "});");
    scripts.add(js);
  }

  private String getArgs(Element el)
  {
    List<Element> args = el.getChildren("arg");
    if (args.size()>0) {
      StringBuilder sb = new StringBuilder();
      for (Element arg: args) {
        if (sb.length()>0) sb.append(", ");
        sb.append(get(arg, "value"));
      }
      return sb.toString();
    } else {
      return "";
    }
  }

  public void visit_card(Element el) throws InvocationTargetException, IllegalAccessException {
    body.add(appendln("<div class=\"mdl-card mdl-shadow--2dp ",widths(el)," card\">"));
    body.add(appendln("  <div class=\"mdl-card__title\">"));
    body.add(appendln("     <h2 class=\"mdl-card__title-text\">",el.getAttributeValue("title"),"</h2>"));
    body.add(appendln("  </div>"));
    processChildren(el);
    body.add(appendln("</div>"));
  }

  public void visit_text(Element el) throws InvocationTargetException, IllegalAccessException {
    body.add(appendln("<span class=\"text\">",HTML.escape(text(el)),"</span>"));
  }

  /**
   * Given element and desktop-width, phone-width, tablet-width then return css sizes.
   *
   * @param el
   */
  public static String widths(Element el) {
    String desktopWidth = get(el,"desktop-width","");
    String phoneWidth = get(el,"phone-width","");
    String tabletWidth = get(el,"tablet-width","");
    StringBuilder sb = new StringBuilder();

    System.out.println("DESKTOP WIDTH: " + desktopWidth);

    if (!desktopWidth.isEmpty()) {
      int i=Integer.parseInt(desktopWidth);
      if (i>12) throw new RuntimeException("desktop-width was " + i + " but can only be 1-12");
      sb.append(" mdl-cell--").append(i).append("-col-desktop ");
    }

    if (!tabletWidth.isEmpty()) {
      int i=Integer.parseInt(tabletWidth);
      if (i>8) throw new RuntimeException("tablet-width was " + i + " but can only be 1-8");
      sb.append(" mdl-cell--").append(i).append("-col-tablet ");
    }

    if (!phoneWidth.isEmpty()) {
      int i=Integer.parseInt(phoneWidth);
      if (i>4) throw new RuntimeException("phone-width was " + i + " but can only be 1-4");
      sb.append(" mdl-cell--").append(i).append("-col-phone ");
    }

    return sb.toString();
  }

  /**
   * Only processes children in dev mode.  Need to set DEVMODE in the Environment
   *
   * @param el
   * @throws InvocationTargetException
   * @throws IllegalAccessException
   */
  public void visit_dev_mode(Element el) throws InvocationTargetException, IllegalAccessException
  {
    if (System.getenv("DEVMODE")!=null) {
      processChildren(el);
    }
  }

  public void visit_card_actions(Element el) throws InvocationTargetException, IllegalAccessException {
    body.add(appendln("<div class=\"mdl-card__actions mdl-card--border\">"));
    processChildren(el);
    body.add(appendln("</div>"));
  }

  public void visit_card_body(Element el) throws InvocationTargetException, IllegalAccessException {
    body.add(appendln("<div class=\"mdl-card__supporting-text\">"));
    processChildren(el);
    body.add(appendln("</div>"));
  }

  boolean haveListRemove = false;
  public void visit_list(Element el) throws InvocationTargetException, IllegalAccessException, TemplateException
  {
    if (!haveListRemove) {
      // for lists we are likely to remove by element, so add a utility method
      haveListRemove=true;
      StringBuilder sb = new StringBuilder();
      appendln(sb, "function __animatedListItemRemove(item) {");
      appendln(sb, "var htmlItem = item._htmlItem;");
      appendln(sb, "this.splice(this.indexOf(item),1);");

      if (logging) {
        appendln(sb, "console.log(\"__animatedListItemRemove\", item, htmlItem);");
      }
      appendln(sb, "if (!htmlItem) return;");
      appendln(sb, "$(htmlItem).addClass(\"animated zoomOut\");");
      appendln(sb, "$(htmlItem).one('webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend', function(){$(htmlItem).remove();});");
      appendln(sb, "}");
      scripts.add(sb);
    }

    // we are using handlerbars for list items
    requires.put("HandleBars", "lib/handlebars.min");

    // <list data-src="system-messages" item-var="item" item-html-var="itemHtml" list-var="list">
    String datasrc= JS.get(el, "data-src");
    if (!dataSources.containsKey(datasrc)) {
      throw new RuntimeException("Missing Data Source " + datasrc);
    }

    String id = setIfEmpty(el, "id", "list");
    setIfEmpty(el, "item-var", "item");
    setIfEmpty(el, "item-html-var", "itemHtml");
    setIfEmpty(el, "list-var", "list");

    body.add(appendln("<div id='"+id+"' class='list'>"));
    body.add(appendln("<div class='content mdl-grid'></div>"));
    body.add(appendln("<div class='empty-list'>"));
    processChildren(el.getChild("empty"));
    body.add(appendln("</div>"));
    body.add(appendln("</div>"));

    // process actions
    processChildren(el.getChild("actions"));

    // lists can have multiple items, so create a list item renderer for each
    List<Element> items = el.getChildren("item");
    for (Element item: items) {
      // build a binder function for this list item
      buildListItemBinder(el, item);
    }

    // for list, we need a reload function and then hook it into a reload event

    String renderFunc = JS.functionName((String)null, id,"render");
    Element item = el.getChild("item");
    StringBuilder script = new StringBuilder();
    appendln(script, "function ", renderFunc, "(data) {");
    appendln(script, "   var listEl = $('#",id,"').find('.content')");
    appendln(script, "   listEl.html('');");
    appendln(script, "   if (!data || !data.length) {");
    appendln(script, "      $('#",id," .empty-list').show();");
    appendln(script, "      return;");
    appendln(script, "   }");
    appendln(script, "   $('#",id," .empty-list').hide();");
    appendln(script, "   data.remove=$.proxy(__animatedListItemRemove, data);");
    appendln(script, "   data.forEach(function(item) {");
    appendln(script, "       ", getItemBinderFunctionName(el, item),"(data, item, listEl);");
    appendln(script, "   });");
    appendln(script, "}");

    // reload function
    String reloadFunc = JS.functionName((String)null,id,"reload");
    appendln(script, "function ", reloadFunc, "() {");
    appendln(script, "  var list = ", datasourceFunctionName(get(el, "data-src")), "();");
    appendln(script, "  list.then(",renderFunc,");");
    appendln(script, "}");

    // register reload event (list ID + -reload)
    appendln(script, "EventBus.addEventListener('",id+"-reload',",reloadFunc,");");

    // force first reload/render
    appendln(script, reloadFunc, "();");

    scripts.add(script);



  }

  public void visit_list_item_remove(Element el) throws InvocationTargetException, IllegalAccessException {
    scripts.add(appendln("list.remove(item);"));
  }


  void processChildren(Element el, List<StringBuilder> htmlBuffer, List<StringBuilder> tplBuffer, List<StringBuilder> jsBuffer) throws InvocationTargetException, IllegalAccessException
  {
    List<StringBuilder> oldHtml=this.body;
    List<StringBuilder> oldTemplates=this.templates;
    List<StringBuilder> oldJS = this.scripts;

    if (htmlBuffer!=null) this.body=htmlBuffer;
    if (tplBuffer!=null) this.templates=tplBuffer;
    if (jsBuffer!=null) this.scripts=jsBuffer;

    processChildren(el);

    this.body = oldHtml;
    this.scripts=oldJS;
    this.templates=oldTemplates;
  }

  String processChildrenHtmlAsString(Element el) throws InvocationTargetException, IllegalAccessException
  {
    List<StringBuilder> html = new ArrayList<>();
    processChildren(el, html, null, null);
    return join(html);
  }

  private String getListItemBaseID(Element list, Element item) {
    return String.join("_", get(list,"id","list"), get(item, "id", "item"));
  }

  private String getItemBinderFunctionName(Element list, Element item) {
    return JS.functionName(String.join("_", getListItemBaseID(list, item), "binder"));
  }

  private void buildListItemBinder(Element list, Element item) throws InvocationTargetException, IllegalAccessException
  {
    // now that we have processed the child, let's get it back as HTML
    String html = processChildrenHtmlAsString(item);

    StringBuilder binder = new StringBuilder();
    String fname = getItemBinderFunctionName(list, item);
    appendln(binder, "function ", fname, "(list, item, domListContent) {");
    appendln(binder, "  var tplStr = '", JS.escape(html), "';");
    appendln(binder, "  var tpl = HandleBars.compile(tplStr);");
    appendln(binder, "  var child = tpl(item);");
    appendln(binder, "  child = $(child);");
    appendln(binder, "  item._htmlItem = child; // add a reference back to the dom Item");

    // bind the actions that are defined to the buttons it the the html
    Element actionsEl = list.getChild("actions");
    if (actionsEl!=null)
    {
      List<Element> actions = actionsEl.getChildren("action");
      if (actions.size() > 0)
      {
        for (Element action : actions)
        {
          String btnTag = getButtonActionClassName(action);
          if (html.contains(btnTag))
          {
            appendln(binder, "$(child).find(\".", btnTag, "\").click(function(evt) {");
            appendln(binder, "   ", getActionFunctionName(action), "(list, item);");
            appendln(binder, "});");
          }
        }
      }
    }


    appendln(binder, "  domListContent.append(child);");
    appendln(binder, "}");
    scripts.add(binder);
  }

  private String getParentActionId(Element el)
  {
    return JS.functionName(el.getParentElement(),"action");
  }

  private String js(String val)
  {
    return JS.escape(val);
  }

  public static void main(String args[]) throws JDOMException, InvocationTargetException, IllegalAccessException, IOException
  {
    if (args.length != 2)
    {
      System.out.println("Generates SageTV Web Modules from Web Module Xml");
      System.out.println("Usage: java AppModuleGenerator xmlInput html_output_dir");
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

    AppGenerator gen = new AppGenerator();
    gen.generateApp(xml, outDir);
  }
}
