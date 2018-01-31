<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_common.ftl">
<component debug="false">
    <!-- html for page -->
    <#assign name = util.actionFunctionName(el)>
    <#assign action = util.get(el, "action")>
    ${ctx.verifyActionNotExists(action)}
    ${ctx.actions(action, el)}

    <script><![CDATA[

        function ${name} (${onList?then("list, item" , "")}) {
            ${ctx.processChildren(el).script()}
        }

    ]]></script>
</component>
