<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_common.ftl">
<component debug="false">
    <content><![CDATA[
        <div class="mdl-card mdl-shadow--2dp ${util.widths(el)} card">
            <div class="mdl-card__title">
                <h2 class="mdl-card__title-text">${util.get(el, "title","")}</h2>
            </div>
            ${ctx.processChildren(el).content()}
        </div>
    ]]></content>
</component>
