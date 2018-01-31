<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_common.ftl">
<component>
    <content><![CDATA[
        <main class="mdl-layout__content"><div id="content">
            ${ctx.processChildren(el).content()}
        </div></main>
    ]]></content>
</component>
