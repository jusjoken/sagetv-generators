<#-- @ftlvariable name="util" type="tv.sage.tools.web.StaticUtils.static" -->
<#-- @ftlvariable name="el" type="org.jdom2.Element" -->
<#-- @ftlvariable name="System" type="java.lang.System.static" -->
<#-- @ftlvariable name="ctx" type="tv.sage.tools.web.GeneratorContext" -->
<component>
    <!-- lazy will add to end of body, otherwise head -->
    ${ctx.css("http://fonts.googleapis.com/icon?family=Material+Icons")}
    ${ctx.css("https://cdnjs.cloudflare.com/ajax/libs/materialize/0.98.2/css/materialize.min.css")}
    ${ctx.css("app.css")}

    ${ctx.jsLazy("lib/linq.min.js")}
    ${ctx.jsLazy("lib/eventbus.min.js")}
    ${ctx.jsLazy("lib/dialog-polyfill.js")}
    ${ctx.jsLazy("lib/require.js")}
    <#--${ctx.jsLazy("lib/jquery-3.2.0.min.js")}-->
    <#--${ctx.jsLazy("https://cdnjs.cloudflare.com/ajax/libs/materialize/0.98.2/js/materialize.min.js")}-->

    <!-- becomes part of the requires for page -->
    ${ctx.require("page","lib/page")}
    ${ctx.require("jquery","lib/jquery-3.2.0.min.js")}
    ${ctx.require("materialize", "https://cdnjs.cloudflare.com/ajax/libs/materialize/0.98.2/js/materialize.min.js")}

    <!-- global style for page -->

    <!-- process the children in a temp buffer so that we know everything about
         the app before we start creating the html for it -->
    <#assign buffer=ctx.processChildren(el) >

    <!-- global script for page -->

    <!-- html for page -->
    <content><![CDATA[
        <!doctype html>
        <html lang="en">
        <head>
            <title>${util.get(el, "title", "Application")}</title>

            <meta name="x-generator" content="SageTV">
            <meta name="x-generator-datetime" content="">

            <meta name="viewport" content="width=device-width, initial-scale=1.0, minimum-scale=1.0">

            <#list ctx.css() as css>
                <link rel="stylesheet" href="${css}">
            </#list>

            <#list ctx.js() as js>
                <script src="${js}"></script>
            </#list>

            <!-- all the styles -->
            <style>
                ${ctx.style()}
            </style>
        </head>

        <body>
        <#assign sideNav = util.child(el, "side-nav")>
        <#assign headerNav = util.child(el, "nav-header")>
        <#assign fixedDrawer = ("true" == util.get(sideNav, "fixed", "true"))>
        <#assign fixedHeader = ("true" == util.get(headerNav, "fixed", "true"))>

        <div class="mdl-layout mdl-js-layout ${fixedDrawer?then("mdl-layout--fixed-drawer","mdl-layout--drawer")} ${fixedHeader?then("mdl-layout--fixed-header","mdl-layout--header")}">

            <div class="templates">
                ${buffer.templates()}
            </div>

            ${buffer.content()}

            <!-- dump the body scripts -->
            <#list ctx.jsLazy() as js>
                <script src="${js}"></script>
            </#list>

            <!-- do the scripts -->
            <script language='JavaScript'>
                requirejs.config({
                    baseUrl: '.',
                    paths: {
                        root: '',
                        js: 'js',
                        lib: 'lib'
                    }
                });

                <#if System.getenv("SAGE_API")??>
                // DEV MODE ONLY, SHOULD NOT BE IN PRODUCTION"
                window.SageTVAPIBaseUrl="${System.getenv("SAGE_API")}";
                </#if>

                if (!window.fetch) {
                    var fetchImpl = require('lib/fetch');
                    window.fetch=fetchImpl;
                }

                if (!window.Promise) {
                    var promiseImpl = require('lib/promise.min');
                    window.Promise = promiseImpl;
                }

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
                    // configure page routing...
                    var content = $('#content');

                    // main page
                    page('/', function() {
                        page.redirect('${util.get(el, "default-route", "/")}');
                    });

                    <#assign routes=ctx.routes()>

                    // ROUTES:
                    ${ctx.routes()?size}
                    // END ROUTES:

                    <#list routes?keys as route>
                        // ROUTE: ${route}
                        page('${route}', function() {
                           console.log('Loading ${route} -> ${routes[route]}');
                           fetch("${routes[route]}").then(function(result) {
                           if (!result.ok) {
                             throw "Load Failed: ${routes[route]}";
                           }
                          return result.text();
                        }).then(function(html) {
                          $(content).html(html);
                          $(content).find(".page").hide();
                          $(content).find(".${util.routeId('${route}')}").show();
                        }).catch(function(error) {
                          console.log("LOAD FAILED", error);
                          $(content).html("Unable to load ${routes[route]}");
                        });
                        });

                    </#list>

                    // default NOT found route
                    page('*', function(ctx) {
                        $(content).html("PAGE: NOT HANDLED" + ctx.path);
                    });

                    // use hashbang routing
                    page({
                        hashbang: true
                    });

                    // script
                    ${ctx.script()}

                    console.log('App Started');
                });

            </script>
            </div>
        </body>
        </html>
    ]]></content>
</component>
