package tv.sage.tools.web;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jdom2.Element;

/**
 * Created by seans on 01/04/17.
 */
public class HTML extends COMMON
{

  public static String escape(String in) {
    return StringEscapeUtils.escapeHtml4(in);
  }

  public static String escape(Element el, String attr) {
    return escape(el, attr, null);
  }

  public static String escape(Element el, String attr, String defVal) {
    String val = el.getAttributeValue(attr);
    if (val==null) val=defVal;
    if (val==null) throw new RuntimeException("Missing attribute value for " + attr + " on Element " + el );
    return escape(val);
  }
}
