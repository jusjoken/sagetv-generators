<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component debug="false">
    <#assign id=util.id(el)>
    <content>
        <div id="${id}" class="page ${util.routeId(util.get(el, "route"))}">
        ${ctx.registerRoute(util.get(el, "route"), "/modules/"+util.root(el,"name")+".html")}
        ${ctx.processChildren(el).content()}
        </div>
    </content>
    <#--<script>-->
        <#--console.log("PATH:["+window.location.hash+"]", '${util.get(el, 'route')}');-->
        <#--if (window.location.hash.endsWith('${util.get(el, 'route')}')) {-->
            <#--console.log("Showing Page", '${util.get(el, 'route')}');-->
            <#--$('.page').hide();-->
            <#--$('#${id}').show();-->
        <#--}-->
    <#--</script>-->
</component>
