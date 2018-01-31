package tv.sage.tools.web;


import org.apache.commons.lang3.StringUtils;
import org.jdom2.Content;
import org.jdom2.Element;
import org.jdom2.filter.ElementFilter;
import org.jdom2.filter.Filter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by seans on 01/04/17.
 */
public class COMMON
{
  public static StringBuilder appendln(String... parts) {
    return appendln(null, null, parts);
  }

  public static StringBuilder appendln(StringBuilder sb, String... parts) {
    return appendln(null, sb, parts);
  }

  public static StringBuilder appendln(String sep, StringBuilder sb, String... parts) {
    if (sb==null) sb = new StringBuilder();
    append(sep, sb, parts);
    sb.append("\n");
    return sb;
  }

  public static StringBuilder appendln(String s) {
    StringBuilder sb = new StringBuilder(s);
    sb.append("\n");
    return sb;
  }










  public static StringBuilder append(String... parts) {
    return append(null, null, parts);
  }

  public static StringBuilder append(StringBuilder sb, String... parts) {
    return append(null, sb, parts);
  }

  public static StringBuilder append(String sep, StringBuilder sb, String... parts) {
    if (sb==null) sb = new StringBuilder();
    for (String p: parts) {
      if (p!=null) {
        if (sep!=null) {
          sb.append(sep);
        }
        sb.append(p);
      }
    }
    return sb;
  }

  public static StringBuilder append(String s) {
    return new StringBuilder(s);
  }

  public static String root(Element el, String name) {
    return get(el.getDocument().getRootElement(), name, null);
  }


  public static String get(Element el, String name) {
    return get(el, name, null);
  }

  public static String get(Element el, String name, String defValue) {
    String val = el.getAttributeValue(name);
    if (StringUtils.isEmpty(val)) val=defValue;
    if (val==null) throw new RuntimeException("Missing Attribute Value for Attribute: " + name + " on Element " + el);
    return val;
  }

  public static String setIfEmpty(Element el, String name, String defValue) {
    String val = get(el, name, defValue);
    el.setAttribute(name, val);
    return val;
  }


  public static Element findParent(String parentElName, Element me) {
    if (parentElName.equals(me.getName())) {
      return me;
    }
    else
    {
      if (me.getParentElement() == null)
      {
        return null;
      }
      else
      {
        return findParent(parentElName, me.getParentElement());
      }
    }
  }

  public static boolean isChildOf(Element me, String parentElName) {
    return findParent(parentElName, me)!=null;
  }

  public static boolean onList(Element el) {
    return isChildOf(el, "list");
  }

  public static Element getList(Element el) {
    return findParent("list", el);
  }

  public static String text(Element el) {
    return el.getTextTrim();
  }

  public static String join(List<StringBuilder> strings) {
    StringBuilder sb = new StringBuilder();
    for (StringBuilder s: strings) {
      sb.append(s.toString());
    }
    return sb.toString();
  }

  public static boolean hidePhone(Element el) {
    return "phone".equals(get(el, "hide-when", "never"));
  }

  public static boolean hasChild(Element par, String childName) {
    Iterator<Element> iter = par.getDescendants(new ElementFilter(childName));
    return (iter!=null && iter.hasNext());
  }

  public static boolean has(Element el, String attr) {
    return get(el, attr,"").trim().length()>0;
  }

  public static Element child(Element par, String childName) {
    Iterator<Element> iter = par.getDescendants(new ElementFilter(childName));
    if (iter.hasNext()) return iter.next();
    return null;
  }

  public static List<Element> childrenAll(Element par, String childName) {
    Iterator<Element> iter = par.getDescendants(new ElementFilter(childName));
    List<Element> all = new ArrayList<>();
    while (iter.hasNext()) all.add(iter.next());
    return all;
  }


  public static List<Element> children(Element par, String childName) {
    return par.getChildren(childName);
  }

  public static String id(Element el) {
    String id = get(el, "id", "");
    if (id.isEmpty()) {
      // generate id...
      id="el"+String.valueOf(el.hashCode());
      el.setAttribute("id", id);
    }
    return id;
  }

}
