<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component debug="false">
    <!-- html for page -->
    <script><![CDATA[
        EventBus.addEventListener('${util.get(el, "name")}', function(${util.get(el, "event-arg","event")}, ${util.get(el, "data-arg", "data")}) {
            ${ctx.processChildren(el).script()}
        });
    ]]></script>
</component>
