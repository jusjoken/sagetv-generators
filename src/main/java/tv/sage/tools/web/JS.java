package tv.sage.tools.web;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jdom2.Element;

/**
 * Created by seans on 01/04/17.
 */
public class JS extends COMMON
{
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

}
