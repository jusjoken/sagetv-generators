<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#assign text = el.getAttributeValue("text")??>
<#assign icon = el.getAttributeValue("icon")??>
<#assign path = el.getAttributeValue("path")??>
<#assign action = el.getAttributeValue("action")??>
<#assign hasBadge = util.hasChild(el, "badge")>

<#include "inc_common.ftl">