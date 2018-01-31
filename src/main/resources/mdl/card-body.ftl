<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_common.ftl">
<component debug="false">
    <content><![CDATA[
        <div class="mdl-card__supporting-text">
            ${ctx.processChildren(el).content()}
        </div>
    ]]></content>
</component>
