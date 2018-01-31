<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component>
    <!-- html for page -->
    <content>
        ${ctx.processChildren(el).content()}
    </content>
</component>
