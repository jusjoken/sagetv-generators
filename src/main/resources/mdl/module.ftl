<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#include "inc_common.ftl">
<component>
    <!-- process the children in a temp buffer so that we know everything about
     the app before we start creating the html for it -->
    <#assign buffer=ctx.processChildren(el) >

    <content><![CDATA[
        <div class='module-body'>
            <div class="templates">
            ${buffer.templates()}
            </div>

            <!-- all the styles -->
            <#if ctx.style()?has_content>
                <style>
                    ${ctx.style()}
                </style>
            </#if>

            <!-- begin content -->
            ${buffer.content()}
            <!-- end content -->

            <#list ctx.css() as css>
                <link rel="stylesheet" href="${css}">
            </#list>

            <#list ctx.js() as js>
                <script src="${js}"></script>
            </#list>

            <#list ctx.jsLazy() as js>
                <script src="${js}"></script>
            </#list>

            <script>
                <#assign vars="">
                <#assign funcs="">
                <#assign requires=ctx.requires()>
                <#list requires?keys as req>
                    <#assign vars>${vars}${req}</#assign>
                    <#assign funcs>${funcs}"${requires[req]}"</#assign>
                    <#if req_has_next>
                        <#assign vars>${vars},</#assign>
                        <#assign funcs>${funcs},</#assign>
                    </#if>
                </#list>

                require([${funcs}], function(${vars}) {
                    ${buffer.script()}
                });
            </script>
        </div>
    ]]></content>
</component>
