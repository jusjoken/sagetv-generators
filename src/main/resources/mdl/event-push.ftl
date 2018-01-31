<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_common.ftl">
<component debug="false">
    <script><![CDATA[

    EventBus.dispatch("${util.get(el, "event")}", null, "${util.get(el, "value","true")}");

    ]]></script>
</component>
