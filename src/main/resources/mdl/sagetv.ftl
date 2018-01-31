<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_common.ftl">

<!--suppress ALL -->
<component debug="false">
    <script><![CDATA[
        <#assign service = util.get(el, "service")>
        <#assign api = util.get(el,"api")>

        ${ctx.require("SageTVAPI", "js/SageTVAPI")}
        ${ctx.require(service+"API", "js/"+ service+"API")}

        var api = new ${service}API(new SageTVAPI());

        <#assign filter = util.hasChild(el,"filter")>
        <#assign order = util.hasChild(el, "order")>

        return api.${api}().then(function(result) {
            <#--if we are filtering or sortint -->
            <#if filter || order>
                result = Enumerable.from(result)
                <#if filter>
                    .where(function (x) {
                       return x.${util.get(util.child(el, "filter"), "by")};
                    })
                </#if>
                <#if order>
                    <#assign desc = util.get(util.child(el, "order"), "desc", "false") == "true">
                    .orderBy${desc?then("Descending","")}(function (x) {
                        return x.${util.get(util.child(el, "order"), "by")};
                    })
                </#if>
                .toArray();
            </#if>

            <#if (el.getChild("success"))??>
                ${ctx.processChildren(el.getChild("success")).script()}
            </#if>

            return result;
            <#if (el.getChild("failure"))??>
                }).catch(function (error) {
                    ${ctx.processChildren(el.getChild("failure")).script()}
            </#if>
            });

    ]]></script>
</component>
