<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component debug="false">
    <script>
        ${ctx.processChildren(el).script()}
    </script>
</component>
