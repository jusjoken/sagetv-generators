<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component>
    <content><![CDATA[
        <#assign brand = util.child(el,"brand")/>
        <#assign buttons = util.children(el,"button")/>

        <header class="mdl-layout__header">
            <div class="mdl-layout__header-row">
                <#if brand??>
                    ${ctx.processElement(brand).content()}
                    <div class="mdl-layout-spacer"> </div>
                </#if>

                <#if buttons?is_collection>
                    <nav class="mdl-navigation">
                        ${ctx.processElements(buttons).content()}
                    </nav>
                </#if>
            </div>
        </header>
    ]]></content>
</component>
