package tv.sage.tools.web;

import org.jdom2.Element;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by seans on 14/04/17.
 */
public class GeneratorContext
{
  private final AppGeneratorNG gen;

  List<Element> modules = new ArrayList<>();
  Map<String, Element> actions = new HashMap<>();

  List<String> cssIncludes  = new ArrayList<>();
  List<String> jsIncludes = new ArrayList<>();
  List<String> jsLazyIncludes = new ArrayList<>();
  Map<String,String> requires = new LinkedHashMap<>(); // name, path
  StringBuilder content = new StringBuilder();
  StringBuilder style = new StringBuilder();
  StringBuilder templates = new StringBuilder();

  Map<String, String> routes = new LinkedHashMap<>();
  Map<String, Element> datasources = new LinkedHashMap<>();

  public GeneratorContext(AppGeneratorNG gen) {
    this.gen=gen;
  }

  public List<String> css()
  {
    return cssIncludes;
  }

  public List<String> js()
  {
    return jsIncludes;
  }

  public List<String> jsLazy()
  {
    return jsLazyIncludes;
  }

  public Map<String, String> requires()
  {
    return requires;
  }

  public StringBuilder content()
  {
    return content;
  }

  public StringBuilder style()
  {
    return style;
  }

  public StringBuilder script()
  {
    return script;
  }

  StringBuilder script = new StringBuilder();

  public GeneratorContext createNew() {
    return new GeneratorContext(gen);
  }

  public void css(String url) {
    if (!cssIncludes.contains(url))
      cssIncludes.add(url);
  }

  public void js(String url) {
    if (!jsIncludes.contains(url))
      jsIncludes.add(url);
  }

  public void jsLazy(String url) {
    if (!jsLazyIncludes.contains(url))
      jsLazyIncludes.add(url);
  }

  public void require(String name, String path) {
    requires.put(name, path);
  }

  public void content(String content) {
    this.content = new StringBuilder(content);
  }

  public void template(String text)
  {
    this.templates = new StringBuilder(text);
  }

  public void style(String style) {
    this.style = new StringBuilder(style);
  }

  public void script(String script) {
    this.script = new StringBuilder(script);
  }

  public void merge(GeneratorContext ctxNew)
  {
    this.modules().addAll(ctxNew.modules());
    this.actions().putAll(ctxNew.actions());
    this.requires().putAll(ctxNew.requires());
    this.datasources.putAll(ctxNew.datasources());
    this.routes.putAll(ctxNew.routes());
    this.jsIncludes.addAll(ctxNew.js());
    this.cssIncludes.addAll(ctxNew.css());
    this.jsLazyIncludes.addAll(ctxNew.jsLazy());
    this.style().append(ctxNew.style());
    this.templates.append(ctxNew.templates());

    this.script(ctxNew.script().toString());
    this.content(ctxNew.content().toString());
  }

  public StringBuilder templates()
  {
    return templates;
  }

  public Map<String, Element> actions()
  {
    return actions;
  }

  public void actions(String action, Element el) {
    actions.put(action, el);
  }

  public void verifyActionNotExists(String action) {
    if (actions.containsKey(action)) {
      throw new RuntimeException("Action: " + action + " already exists");
    }
  }

  public List<Element> modules()
  {
    return modules;
  }

  public GeneratorContext processChildren(Element el) {
    try
    {
      return gen.processChildren(this, el);
    } catch (InvocationTargetException | IllegalAccessException e)
    {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public GeneratorContext processElements(Collection<Element> els) {
    try
    {
      return gen.processElements(this, els);
    } catch (InvocationTargetException | IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
  }


  public GeneratorContext processElement(Element el) {
    try
    {
      return gen.processElement(this, el);
    } catch (InvocationTargetException e)
    {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e)
    {
      throw new RuntimeException(e);
    }
  }

  public Map<String, String> routes()
  {
    return routes;
  }

  public Map<String, Element> datasources() {
    return datasources;
  }

  public void registerDatasource(String name, Element el) {
    Element elOld = datasources.put(name, el);
    if (elOld!=null) {
      throw new RuntimeException("Duplicate Datasource: " + name);
    }
  }

  public void verifyDatasource(String name) {
    if (!datasources.containsKey(name)) throw new RuntimeException("Unknown Datasource " + name );
  }

  public void registerRoute(String name, String page) {
    System.out.println("Route: " + name + " => " + page);
    String old = routes.put(name, page);
    if (old!=null) {
      throw new RuntimeException("Duplicate Route: " + name);
    }
  }

//  @Override
//  public String toString()
//  {
//    throw new RuntimeException("Assigning Generator Context to String!!!!!");
//  }
}
