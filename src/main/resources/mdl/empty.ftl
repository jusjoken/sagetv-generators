<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component debug="false">
    <content><![CDATA[
        <div class="empty">
        ${ctx.processChildren(el).content()}
        </div>
    ]]></content>
</component>
