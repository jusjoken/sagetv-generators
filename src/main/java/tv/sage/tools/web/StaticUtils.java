package tv.sage.tools.web;

import com.google.gson.Gson;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jdom2.Element;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by seans on 14/04/17.
 */
public class StaticUtils extends COMMON
{
  public static String actionFunctionName(Element el) {
    return actionFunctionName(get(el, "action"));
  }

  public static String actionFunctionName(String name) {
    return JS.functionName(name);
  }

  public static String getParentActionId(Element el)
  {
    return JS.functionName(el.getParentElement(),"action");
  }

  public static String datasourceFunctionName(String name)
  {
    return JS.functionName("get_datasource_" + name);
  }

  public static String getDefaultBinderId(Element list) {
    Element item = list.getChildren("item").get(0); // first one is the default
    return id(item);
  }

  public static String routeId(String route) {
     return JS.functionName("route_"+route.replace('/','_'));
  }

  public static String getJsonBinderInfo(Element list) {
    StringBuilder binders = new StringBuilder();
    binders.append("{");

    Iterator<Element> iter = list.getChildren("item").iterator();
    while (iter.hasNext()) {
      Element binder = iter.next();
      String binderId = id(binder);
      binders.append("'").append(binderId).append("': {");
      binders.append("'templateId':'").append(templateId(binder)).append("',");
      binders.append("'fnBinder':").append(templateFunctionName(binder));
      binders.append("}");
      if (iter.hasNext()) {
        binders.append(",");
      }
    }

    binders.append("}");
    return binders.toString();
  }

  public static String templateFunctionName(Element binder)
  {
    return JS.functionName("binder_"+id(binder));
  }

  public static String templateId(Element template)
  {
    return "template_"+id(template);
  }

  public static String functionName(String js) {
    js = js.toLowerCase().replace('-','_');
    return js;
  }

  public static String functionName(String pre, String name, String post) {
    pre=(pre==null?"":(pre+"_"));
    post=(post==null?"":("_"+post));
    return functionName(pre+name+post);
  }

  public static String functionName(String pre, Element e, String name, String post) {
    String val = e.getAttributeValue(name);
    if (val==null) throw new RuntimeException("Missing Attribute: " + name + " on Element: " + e);
    return functionName(pre, val, post);
  }

  public static String functionName(String pre, Element e, String name) {
    return functionName(pre, e, name, null);
  }

  public static String functionName(Element e, String name, String post) {
    return functionName(null, e, name, post);
  }

  public static String functionName(Element e, String name) {
    return functionName(null, e, name, null);
  }

  public static String escape(String js) {
    return StringEscapeUtils.escapeEcmaScript(js);
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

  public static String getActionTag(Element action) {
    return JS.functionName("btn_", get(action, "action"), "action");
  }

  public static String getActionTag(String actionStr) {
    if (actionStr==null||actionStr.trim().isEmpty()) return "";
    return JS.functionName("btn_", actionStr, "action");
  }
}
