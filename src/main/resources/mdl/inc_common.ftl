<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<#assign inModule = util.isChildOf(el, "module")>
<#assign onCard = util.isChildOf(el, "card")>
<#assign onNav = util.isChildOf(el, "nav")>
<#assign onNavHeader = util.isChildOf(el, "nav-header")>
<#assign onSideNav = util.isChildOf(el, "side-nav")>
<#assign onList = util.onList(el)>