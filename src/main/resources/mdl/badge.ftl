<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component>
    <content>
        <span id='${util.id(el)}' class='mdl-badge' data-badge='${util.get(el, "text","")}'>${util.get(el.getParentElement(), "text", "")}</span>
    </content>
</component>
