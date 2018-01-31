<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_button_common.ftl"/>
<component>
    <content><![CDATA[
        <#assign btnTag=util.getActionTag(util.get(el, "action",""))>
        <#assign id=util.id(el)>
        <a id="${id}" class="${action?then("mdl-button mdl-js-button mdl-button--raised mdl-button--colored","mdl-navigation__link")} ${btnTag}" ${action?then("","href='" + util.get(el, "path") + "'")}>
            <#include "inc_icon.ftl"/>
            <#if hasBadge>
                ${ctx.processElement(util.child(el,"badge")).content()}
            <#else>
                ${util.get(el, "text", "")}
            </#if>
        </a>
    ]]></content>
    <script><![CDATA[
        <#-- TODO: need to lookup the action on see if it's a list action, and if so, then don't bind it here -->
        <#if action && !onList>
        $('#${id}').removeAttr("href").click(${util.actionFunctionName(el)});
        </#if>
    ]]></script>
</component>