<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_common.ftl">
<component>
    <content><![CDATA[
        <#if onSideNav>
            <nav class="mdl-navigation">
                <#if util.has(el, "title")>
                    <h2>${util.get(el, "title")}</h2>
                </#if>
                ${ctx.processChildren(el).content()}
            </nav>
        <#else>
            ${ctx.processChildren(el).content()}
        </#if>
    ]]></content>
</component>
