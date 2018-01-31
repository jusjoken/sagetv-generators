<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<component debug="false">
    <template><![CDATA[
        <div class="template" id="${util.templateId(el)}">
        ${ctx.processChildren(el).content()}
        ${ctx.content("")} <#-- import to clear out content since we are using it in the template -->
        </div>
    ]]></template>
</component>
