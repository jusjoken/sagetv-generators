<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component debug="false">
    <!-- html for page -->

    <#assign actionId = util.getParentActionId(el)>
    <#assign modalId = actionId + "_modal">

    <#assign title=util.get(el, "title","")>

    <content><![CDATA[
        <dialog id="${modalId}" class="mdl-dialog mdl-cell--6-col">
        <#if title?has_content>
            <h4 class="mdl-dialog__title">${title}</h4>
        </#if>

        <div class="mdl-dialog__content">
            <p>${util.get(el, "msg","")}</p>
        </div>
        <div class="mdl-dialog__actions">
            <button type="button" class="mdl-button btn-yes">${util.get(el, "positive-button","OK")}</button>
            <button type="button" class="mdl-button btn-no close">${util.get(el, "negative-button", "CANCEL")}</button>
        </div>
        </dialog>
    ]]></content>

    <script><![CDATA[
        var m = document.querySelector('#${modalId}');
        $(m).find(".btn-no").click(function() {m.close()});
        $(m).find(".btn-yes").click(function() {
            m.close();
            <#if el.getChild("positive")??>
            ${ctx.processChildren(el.getChild("positive")).script()}
            </#if>
        });

        if (! m.showModal) {
            dialogPolyfill.registerDialog(m);
        }
        m.showModal();
    ]]></script>
</component>
