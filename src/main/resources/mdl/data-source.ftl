<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component debug="false">
    <script>
        <#-- data source will setup a js function that return a promise -->
        <#assign name = util.get(el, "name")>
        <#assign dsname = util.datasourceFunctionName(name)>
        ${ctx.registerDatasource(name, el)}
        function ${dsname}() { // begin datasource
            ${ctx.processChildren(el).script()}
        } // end datasource
    </script>
</component>
