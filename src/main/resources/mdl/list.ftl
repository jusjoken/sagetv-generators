<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_common.ftl">
<component debug="false">
<#assign datasrc=util.get(el, "data-src")>
<#--${ctx.verifyDatasource(datasrc)}-->

<#assign id=util.setIfEmpty(el, "id", util.id(el))>
<#assign itemVar=util.setIfEmpty(el, "item-var", "item")>
<#assign itemVarHtml=util.setIfEmpty(el, "item-html-var", "itemHtml")>
<#assign listVar=util.setIfEmpty(el, "list-var", util.functionName(util.id(el)))>

<#assign buffer=ctx.processChildren(el)>

    <content><![CDATA[
        <div id='${id}' class='list'>
            <div class='content mdl-grid'></div>
            ${buffer.content()}
        </div>
    ]]></content>

    <script>
        ${ctx.require("STVManagedList", "js/STVManagedList")}

        ${buffer.script().toString()?default("")}

        <#assign fnDataSource = util.datasourceFunctionName(datasrc)>
        <#assign defaultBinder = util.getDefaultBinderId(el)>
        <#assign binders = util.getJsonBinderInfo(el)>

        <#-- add in binder functions -->
        <#list util.children(el, "item") as item>
            function ${util.templateFunctionName(item)}(${itemVar}, ${itemVar}Html, ${listVar}Id, ${listVar}) {
                <#list util.childrenAll(el, "action") as action>
                    $(${itemVar}Html).find(".${util.getActionTag(action)}").click(function(evt) {
                        ${util.actionFunctionName(action)}(${listVar}, ${itemVar});
                    });
                </#list>
            }
        </#list>

        var ${listVar} = new STVManagedList('${id}', ${fnDataSource}, ${binders}, '${defaultBinder}');
        ${listVar}.render();
    </script>
</component>
