package tv.sage.tools.api;

import com.google.gson.*;
import tv.sage.tools.api.model.APIGroup;
import tv.sage.tools.api.model.APIMethod;
import tv.sage.tools.api.model.APIModel;
import tv.sage.tools.api.model.APIType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;

/**
 * Created by seans on 18/03/17.
 */
public class SwaggerUIGenerator {

    public static void main(String args[]) throws IOException {
        if (args.length != 2) {
            System.out.println("Using the api.json, generateApp a Swagger Json API for Swagger UI");
            System.out.println("Usage: java SwaggerUIGenerator sageApi.json swaggerApi.json");
            return;
        }

        File sageApi = new File(args[0]);
        if (!sageApi.exists()) throw new RuntimeException("Missing " + sageApi + "; Need to run APIASTGenerator.");

        File swaggerOut = new File(args[1]);
        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        APIModel model = gson.fromJson(new FileReader(sageApi), APIModel.class);

        model.getGroups().sort(Comparator.comparing(APIGroup::getClassName));
        model.getGroups().forEach(apiGroup -> apiGroup.getMethods().sort(Comparator.comparing(APIMethod::getName)));
        SwaggerUIGenerator gen = new SwaggerUIGenerator();
        gen.generate(model, swaggerOut);
    }

    private void generate(APIModel model, File swaggerOut) throws IOException {
        swaggerOut.getParentFile().mkdirs();
        JsonObject o = new JsonObject();

        o.addProperty("swagger", 3.0);
        //o.addProperty("openapi", 3.0);
        o.addProperty("host", "localhost:8080");
        o.addProperty("basePath", "/sagex/api");

        JsonArray schemes = new JsonArray();
        o.add("schemes", schemes);
        schemes.add("http");
        schemes.add("https");

        JsonArray produces = new JsonArray();
        o.add("produces", produces);
        produces.add("application/json");
        produces.add("application/xml");

        JsonArray consumes = new JsonArray();
        o.add("consumes", consumes);
        consumes.add("application/json");

        JsonObject info = new JsonObject();
        o.add("info", info);
        info.addProperty("version", model.getVersion());
        info.addProperty("title", "SageTV REST API");
        info.addProperty("description", "Access SageTV core features over http");

        JsonObject contact = new JsonObject();
        info.add("contact", contact);
        contact.addProperty("name", "SageTV Team");
        //contact.addProperty("email", "sagetv-dev@google.com");
        contact.addProperty("url", "https://github.com/google/sagetv");

        JsonObject license = new JsonObject();
        info.add("license", license);
        license.addProperty("name", "Apache 2.0");
        license.addProperty("url", "https://github.com/google/sagetv/blob/master/LICENSE");

        JsonObject externalDocs = new JsonObject();
        o.add("externalDocs", externalDocs);
        externalDocs.addProperty("description", "SageTV Forums");
        externalDocs.addProperty("url", "https://forums.sagetv.com/forums/");


        JsonArray tags = new JsonArray();
        o.add("tags", tags);

        JsonObject definitions = new JsonObject();
        o.add("definitions", definitions);

        JsonObject errorModel = new JsonObject();
        definitions.add("ErrorModel", errorModel);
        errorModel.addProperty("type", "object");
        addRequred(errorModel, "code", "message");
        addProperty(errorModel, "code", "integer");
        addProperty(errorModel, "message", "string");

        JsonObject responseModel = new JsonObject();
        definitions.add("ResponseModel", responseModel);
        responseModel.addProperty("type", "object");
        addProperty(responseModel, "Result", "object");

        generateTags(model, tags);
        generateSageDefinitions(model, definitions);

        JsonObject defaultParams = new JsonObject();
        o.add("parameters", defaultParams);

        // add in common params
        defaultParams.add("filter", newParam("filter", false, "Pipe (|) separated list of fields to ONLY include in the reply", "string", null));

        defaultParams.add("context", newParam("context", false, "SageTV UI Context", "string", null));

        // add in encoder
        JsonObject encoder = new JsonObject();
        encoder.addProperty("in", "query");
        encoder.addProperty("name", "encoder");
        //encoder.addProperty("required", "false");
        encoder.addProperty("type", "string");
        encoder.add("enum", arrayOf("json", "xml", "nielm", "image", "raw"));
        encoder.addProperty("default", "json");
        defaultParams.add("encoder", encoder);


        JsonObject paths = new JsonObject();
        o.add("paths", paths);

        for (APIGroup api : model.getGroups()) {
            generateApi(api, paths);
        }

        // save it
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        FileWriter fw = new FileWriter(swaggerOut);
        gson.toJson(o, fw);
        fw.flush();
        fw.close();
    }

    private JsonElement newParam(String name, boolean required, String desc, String type, String defValue) {
        JsonObject o = new JsonObject();
        o.addProperty("in", "query");
        o.addProperty("name", name);
        if (desc != null)
            o.addProperty("description", desc);
        o.addProperty("required", required);
        o.addProperty("type", type);
        //o.add("enum",arrayOf("json", "xml", "nielm", "image", "raw"));
        if (defValue != null)
            o.addProperty("default", defValue);
        return o;
    }

    private JsonArray arrayOf(String... items) {
        JsonArray a = new JsonArray();
        for (String s : items) {
            a.add(s);
        }
        return a;
    }

    private void generateTags(APIModel model, JsonArray tags) {
        for (APIGroup g : model.getGroups()) {
            JsonObject o = new JsonObject();
            tags.add(o);
            o.addProperty("name", g.getClassName());
            o.addProperty("description", g.getComment());
        }
    }

    private void generateSageDefinitions(APIModel model, JsonObject definitions) {
        for (APIGroup g : model.getGroups()) {
            generateSageDefinition(model, g, definitions);
        }

        // add in mediafile properties
        generateSageMetadataDefinition(definitions);

        // 'java' types that we are returning as definiations
        addJavaDefinition(definitions, "Object", "object");
        addJavaDefinition(definitions, "any", "object");
        addJavaDefinition(definitions, "MetaImage", "object");

        addJavaDefinition(definitions, "java.util.Vector", "object");
        addJavaDefinition(definitions, "java.io.File", "object");
        addJavaDefinition(definitions, "java.awt.Color", "object");
        addJavaDefinition(definitions, "java.util.Map", "object");
        addJavaDefinition(definitions, "java.util.Properties", "object");
        addJavaDefinition(definitions, "java.util.Collection", "object");
        addJavaDefinition(definitions, "java.awt.image.BufferedImage", "object");
        addJavaDefinition(definitions, "sage.SageTVPlugin", "object");
        addJavaDefinition(definitions, "sage.SageTVPluginRegistry", "object");
        addJavaDefinition(definitions, "Process", "object");
        addJavaDefinition(definitions, "java.awt.Panel", "object");
//    addJavaDefinition(definitions, "", "object");
//    addJavaDefinition(definitions, "", "object");
//    addJavaDefinition(definitions, "", "object");
//    addJavaDefinition(definitions, "", "object");

    }

    private void addJavaDefinition(JsonObject definitions, String javaType, String jsonType) {
        JsonObject o = new JsonObject();
        definitions.add(javaType, o);
        o.addProperty("type", jsonType);
    }

    private void generateSageDefinition(APIModel model, APIGroup g, JsonObject definitions) {
        if (g.isSerializable()) {
            JsonObject o = new JsonObject();

            definitions.add(g.getForType(), o);

            o.addProperty("type", "object");
            for (APIMethod m : g.getMethods()) {
                if (m.isCanSerialize()) {

                    String f = m.getSerializedField();
                    if (m.getReturnType()!=null && "boolean".equals(m.getReturnType().getType().toLowerCase())) {
                        f= "Is" + f;
                    }
                    if ("ID".equals(f)) {
                        f = g.getForType() + f;
                    }
                    // Hack -- if we have circular refs,
                    // swagger will not render the object type
                    // so we are forcing ints for these refernces in the documentation
                    if (g.getForType().equals(m.getReturnType().getType())) {
                        // our method return type is a circular reference
                        addProperty(o, f, "integer");
                        continue;
                    }
                    if ("MediaFileForAiring".equals(m.getSerializedField())) {
                        // our method return type is a circular reference
                        addProperty(o, m.getSerializedField(), "integer");
                        continue;
                    }

                    if ("".equals(m.getSerializedField())) {
                        addProperty(o, m.getSerializedField(), "#/definitions/"+m.getSerializedField());
                    } else {
                        addProperty(o, f, getSwaggerType(m.getReturnType(), false));
                    }
                }
            }
        }
    }


    private void generateSageMetadataDefinition(JsonObject definitions) {
        JsonObject o = new JsonObject();

        definitions.add("MetadataProperties", o);

        o.addProperty("type", "object");
        addProperty(o, "Title", "string");
        addProperty(o, "EpisodeName", "string");
        addProperty(o, "Genre", "array");
        addProperty(o, "GenreID", "string");
        addProperty(o, "Description", "string");
        addProperty(o, "Year", "integer");
        addProperty(o, "Language", "string");
        addProperty(o, "Rated", "string");
        addProperty(o, "ParentalRating", "string");
        addProperty(o, "RunningTime", "string");
        addProperty(o, "OriginalAirDate", "integer");
        addProperty(o, "ExtendedRatings", "string");
        addProperty(o, "Misc", "string");
        addProperty(o, "PartNumber", "integer");
        addProperty(o, "TotalParts", "integer");
        addProperty(o, "HDTV", "boolean");
        addProperty(o, "CC", "boolean");
        addProperty(o, "Stereo", "boolean");
        addProperty(o, "Subtitled", "boolean");
        addProperty(o, "Premiere", "boolean");
        addProperty(o, "SeasonPremiere", "boolean");
        addProperty(o, "SeriesPremiere", "boolean");
        addProperty(o, "ChannelPremiere", "boolean");
        addProperty(o, "SeasonFinal", "boolean");
        addProperty(o, "SeriesFinale", "boolean");
        addProperty(o, "SAP", "boolean");
        addProperty(o, "ExternalID", "string");
        addProperty(o, "Width", "integer");
        addProperty(o, "Height", "integer");
        addProperty(o, "Track", "integer");
        addProperty(o, "TotalTracks", "integer");
        addProperty(o, "Comment", "string");
        addProperty(o, "AiringTime", "date");
        addProperty(o, "ThumbnailOffset", "integer");
        addProperty(o, "ThumbnailSize", "integer");
        addProperty(o, "ThumbnailDesc", "string");
        addProperty(o, "Duration", "integer");
        addProperty(o, "Picture.Resolution", "string");
        addProperty(o, "MediaTitle", "string");
        addProperty(o, "MediaType", "string");
        addProperty(o, "SeasonNumber", "integer");
        addProperty(o, "EpisodeNumber", "string");
        addProperty(o, "IMDBID", "string");
        addProperty(o, "DiscNumber", "string");
        addProperty(o, "MediaProviderID", "string");
        addProperty(o, "MediaProviderDataID", "string");
        addProperty(o, "UserRating", "integer");
        addProperty(o, "Fanart", "array");
        addProperty(o, "TrailerUrl", "string");
        addProperty(o, "SeriesInfoID", "integer");
        addProperty(o, "EpisodeCount", "integer");
        addProperty(o, "CollectionName", "string");
        addProperty(o, "CollectionID", "integer");
        addProperty(o, "CollectionOverview", "string");
        addProperty(o, "DefaultPoster", "string");
        addProperty(o, "DefaultBanner", "string");
        addProperty(o, "DefaultBackground", "string");
        addProperty(o, "ScrapedBy", "string");
        addProperty(o, "ScrapedDate", "long");
        addProperty(o, "TagLine", "string");
        addProperty(o, "Quotes", "string");
        addProperty(o, "Trivia", "string");
    }

    private void generateApi(APIGroup api, JsonObject paths) {
        for (APIMethod m : api.getMethods()) {
            if (m.isExported())
                generateMethod(api, m, paths);
        }
    }

    private void generateMethod(APIGroup api, APIMethod m, JsonObject paths) {
        JsonObject path = new JsonObject();
        paths.add("?c=" + m.getName(), path);

        JsonArray params = new JsonArray();
        path.add("parameters", params);
        params.add(newRef("#/parameters/encoder"));
        params.add(newRef("#/parameters/context"));
        params.add(newRef("#/parameters/filter"));

        JsonObject get = new JsonObject();
        path.add("get", get);

        JsonArray tags = new JsonArray();
        tags.add(api.getClassName());
        get.add("tags", tags);

        get.addProperty("description", m.getComment());
        get.addProperty("operationId", m.getName());

        JsonArray produces = new JsonArray();
        get.add("produces", produces);
        produces.add("application/json");

        JsonArray parameters = new JsonArray();
        get.add("parameters", parameters);

        for (int i = 0; i < m.getParameters().size(); i++) {
            APIType param = m.getParameters().get(i);
            generateApiMethodParam(i, m, param, parameters);
        }

        JsonObject responses = new JsonObject();
        get.add("responses", responses);

        JsonObject r200 = new JsonObject();
        responses.add("200", r200);

        if (m.getReturnType() != null) {
            r200.addProperty("description", "Normal response");

            if (m.getReturnType().isArray()) {
                JsonObject schema = new JsonObject();
                r200.add("schema", schema);

                schema.addProperty("type", "array");
                JsonObject items = new JsonObject();
                schema.add("items", items);

                if (isPrimitive(m.getReturnType())) {
                    items.addProperty("type", getSwaggerType(m.getReturnType(), false));
                } else {
                    items.addProperty("$ref", getSwaggerType(m.getReturnType(), false));
                }
            } else {
                if (m.getReturnType().getType().equalsIgnoreCase("void")) {
                    r200.addProperty("description", "Normal response (code: 0 - OK)");
                    JsonObject schema = new JsonObject();
                    r200.add("schema", schema);
                    schema.addProperty("$ref", "#/definitions/ErrorModel"); // code/response
                } else {
                    if (isPrimitive(m.getReturnType())) {
                        generate200PrimitiveReply(r200, m.getReturnType());
                        //r200.addProperty("type", getSwaggerType(m.getReturnType()));
                    } else {
                        r200.addProperty("description", "Normal response (value type is " + m.getReturnType().getType() + ")");
                        JsonObject schema = new JsonObject();
                        r200.add("schema", schema);
                        schema.addProperty("$ref", getSwaggerType(m.getReturnType(), false));
                    }
                }
            }
        }

        JsonObject error = new JsonObject();
        responses.add("500", error);
        error.addProperty("description", "Unexpected Error");

        JsonObject schema = new JsonObject();
        error.add("schema", schema);
        schema.addProperty("$ref", "#/definitions/ErrorModel");
    }

    private JsonObject newRef(String refValue) {
        JsonObject o = new JsonObject();
        o.addProperty("$ref", refValue);
        return o;
    }


    private void generate200PrimitiveReply(JsonObject r200, APIType returnType) {
        JsonObject schema = new JsonObject();
        r200.add("schema", schema);
        schema.addProperty("$ref", "#/definitions/ResponseModel");
    }

    private boolean isPrimitive(APIType apiType) {
        return apiType.isPrimitive();
    }

    private void generateApiMethodParam(int i, APIMethod m, APIType param, JsonArray parameters) {
        JsonObject p = new JsonObject();
        parameters.add(p);

        p.addProperty("name", String.valueOf(i + 1));
        p.addProperty("type", getSwaggerType(param, true));
        p.addProperty("required", true);
        p.addProperty("description", param.getName());
        p.addProperty("in", "query");
    }

    private String getSwaggerType(APIType param, boolean isMethodParam) {
        String type = param.getType();
        if (type == null) return "";
        type = type.toLowerCase();
        if (type.equals("string") || param.isPrimitive()) {
            if (type.equals("int") || type.equals("long")) return "integer";

            return type;
        }

        if (isMethodParam && param.isSageObject()) {
            return "string";
        } else {
            return "#/definitions/" + param.getType();
        }
    }

    private void addProperty(JsonObject def, String name, String type) {
        JsonObject properties = null;
        if (!def.has("properties")) {
            properties = new JsonObject();
            def.add("properties", properties);
        } else {
            properties = def.getAsJsonObject("properties");
        }

        JsonObject prop = null;
        if (properties.has(name)) {
            prop = properties.getAsJsonObject(name);
        } else {
            if (type!=null && type.startsWith("#/")) {
                if ("MetadataProperties".equals(name)) {
                    // fix for sagex apis
                    name = "MediaFileMetadataProperties";
                    prop = newRef("#/definitions/MetadataProperties");
                } else {
                    prop = newRef(type);
                }
            } else {
                prop = new JsonObject();
            }
            properties.add(name, prop);
        }

        if (type!=null && type.startsWith("#/")) {
        } else {
            prop.addProperty("type", type);
        }

    }

    private void addRequred(JsonObject def, String... fields) {
        JsonArray required = null;
        if (!def.has("required")) {
            required = new JsonArray();
            def.add("required", required);
        } else {
            required = def.getAsJsonArray("required");
        }

        for (String f : fields) {
            required.add(f);
        }
    }

}
