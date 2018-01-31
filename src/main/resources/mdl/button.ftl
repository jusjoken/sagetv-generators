<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_button_common.ftl"/>
<#if onNav>
    <#include "inc_button_nav.ftl"/>
<#else>
    <#-- TODO: need to impl normal button -->
    <#include "inc_button_nav.ftl"/>
</#if>
