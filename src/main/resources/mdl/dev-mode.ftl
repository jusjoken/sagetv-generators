<#-- @ftlvariable name="System" type="java.lang.System.static" -->
<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component debug="false">
    <#if System.getenv("DEVMODE")??>
        ${ctx.processChildren(el)}
    </#if>
</component>
