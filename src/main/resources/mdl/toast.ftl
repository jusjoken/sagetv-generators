<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component>
    <script><![CDATA[
        ${ctx.require("toastr", "https://cdnjs.cloudflare.com/ajax/libs/toastr.js/latest/toastr.min.js")}
        ${ctx.css("https://cdnjs.cloudflare.com/ajax/libs/toastr.js/latest/toastr.min.css")}
        <#assign msg=util.get(el, "message")>
        <#assign type=util.get(el, "type", "info")>
        <#assign title=util.get(el, "title", "")>
        <#assign timeout=util.get(el, "timeout","")>
        <#if title?has_content && timeout?has_content>
            toastr.${type}('${msg}','${title}',{timeOut: ${timeout});
        <#elseif title?has_content>
            toastr.${type}('${msg}','${title}');
        <#elseif timeout?has_content>
            toastr.${type}('${msg}',{timeOut: ${timeout});
        <#else>
            toastr.${type}('${msg}');
        </#if>

    ]]></script>
</component>
