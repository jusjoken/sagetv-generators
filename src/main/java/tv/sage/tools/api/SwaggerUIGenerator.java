package tv.sage.tools.api;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import tv.sage.tools.api.model.APIGroup;
import tv.sage.tools.api.model.APIMethod;
import tv.sage.tools.api.model.APIModel;
import tv.sage.tools.api.model.APIType;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Comparator;
import java.util.function.Consumer;

/**
 * Created by seans on 18/03/17.
 */
public class SwaggerUIGenerator
{

  public static void main(String args[]) throws IOException
  {
    if (args.length != 2)
    {
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

  private void generate(APIModel model, File swaggerOut) throws IOException
  {
    swaggerOut.getParentFile().mkdirs();
    JsonObject o = new JsonObject();

    o.addProperty("swagger", 2.0);
    o.addProperty("host", "localhost:7777");
    o.addProperty("basePath", "/sage/api");

    JsonArray schemes = new JsonArray();
    o.add("schemes", schemes);
    schemes.add("http");

    JsonArray produces = new JsonArray();
    o.add("produces", produces);
    produces.add("application/json");

    JsonArray consumes = new JsonArray();
    o.add("consumes", consumes);
    consumes.add("application/json");

    JsonObject info = new JsonObject();
    o.add("info", info);
    info.addProperty("version", model.getVersion());
    info.addProperty("title", "SageTV JSON Rest API");
    info.addProperty("description", "Api to access SageTV core features over http/json");

    JsonObject contact = new JsonObject();
    info.add("contact", contact);
    contact.addProperty("name", "SageTV Team");
    contact.addProperty("email", "sagetv-dev@google.com");
    contact.addProperty("url", "https://github.com/google/sagetv");

    JsonObject license = new JsonObject();
    info.add("license", license);
    license.addProperty("name","Apache 2.0");
    license.addProperty("name","https://github.com/google/sagetv/blob/master/LICENSE");

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
    addRequred(responseModel, "value");
    addProperty(responseModel, "value", "any");
    addProperty(responseModel, "type", "string");

    generateTags(model, tags);
    generateSageDefinitions(model, definitions);

    JsonObject paths = new JsonObject();
    o.add("paths", paths);

    for (APIGroup api: model.getGroups()) {
      generateApi(api, paths);
    }

    // save it
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    FileWriter fw = new FileWriter(swaggerOut);
    gson.toJson(o, fw);
    fw.flush();
    fw.close();
  }

  private void generateTags(APIModel model, JsonArray tags)
  {
    for (APIGroup g: model.getGroups()) {
      JsonObject o = new JsonObject();
      tags.add(o);
      o.addProperty("name", g.getClassName());
      o.addProperty("description",g.getComment());
    }
  }

  private void generateSageDefinitions(APIModel model, JsonObject definitions)
  {
    for (APIGroup g: model.getGroups()) {
      generateSageDefinition(model, g, definitions);
    }

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

  private void generateSageDefinition(APIModel model, APIGroup g, JsonObject definitions)
  {
    if (g.isSerializable()) {
      JsonObject o = new JsonObject();

      definitions.add(g.getForType(), o);

      o.addProperty("type","object");
      for (APIMethod m: g.getMethods()) {
        if (m.isCanSerialize()) {
          addProperty(o, m.getSerializedField(), m.getReturnType().getType());
        }
      }
    }
  }

  private void generateApi(APIGroup api, JsonObject paths)
  {
    for (APIMethod m: api.getMethods()) {
      if (m.isExported())
        generateMethod(api, m, paths);
    }
  }

  private void generateMethod(APIGroup api, APIMethod m, JsonObject paths)
  {
    JsonObject path = new JsonObject();
    paths.add("/" + api.getClassName() + "/" + m.getName(), path);

    JsonObject get = new JsonObject();
    path.add("get", get);

    JsonArray tags =new JsonArray();
    tags.add(api.getClassName());
    get.add("tags", tags);

    get.addProperty("description", m.getComment());
    get.addProperty("operationId", m.getName());

    JsonArray produces = new JsonArray();
    get.add("produces", produces);
    produces.add("application/json");

    JsonArray parameters = new JsonArray();
    get.add("parameters", parameters);

    for (APIType param: m.getParameters()) {
      generateApiMethodParam(m, param, parameters);
    }

    JsonObject responses = new JsonObject();
    get.add("responses", responses);

    JsonObject r200 = new JsonObject();
    responses.add("200", r200);

    if (m.getReturnType()!=null) {
      r200.addProperty("description", "Normal response");

      if (m.getReturnType().isArray()) {
        JsonObject schema = new JsonObject();
        r200.add("schema", schema);

        schema.addProperty("type", "array");
        JsonObject items = new JsonObject();
        schema.add("items", items);

        if (isPrimitive(m.getReturnType())) {
          items.addProperty("type", getSwaggerType(m.getReturnType()));
        }
        else
        {
          items.addProperty("$ref", getSwaggerType(m.getReturnType()));
        }
      } else {
        if (m.getReturnType().getType().equalsIgnoreCase("void")) {
          r200.addProperty("description", "Normal response (code: 0 - OK)");
          JsonObject schema = new JsonObject();
          r200.add("schema", schema);
          schema.addProperty("$ref", "#/definitions/ErrorModel"); // code/response
        }
        else
        {
          if (isPrimitive(m.getReturnType()))
          {
            generate200PrimitiveReply(r200, m.getReturnType());
            //r200.addProperty("type", getSwaggerType(m.getReturnType()));
          } else
          {
            r200.addProperty("description", "Normal response (value type is "+ m.getReturnType().getType() +")");
            JsonObject schema = new JsonObject();
            r200.add("schema", schema);
            schema.addProperty("$ref", getSwaggerType(m.getReturnType()));
          }
        }
      }
    }

    JsonObject error = new JsonObject();
    responses.add("default", error);
    error.addProperty("description", "Unexpected Error");

    JsonObject schema =new JsonObject();
    error.add("schema", schema);
    schema.addProperty("$ref", "#/definitions/ErrorModel");
  }

  private void generate200PrimitiveReply(JsonObject r200, APIType returnType)
  {
    JsonObject schema = new JsonObject();
    r200.add("schema", schema);
    schema.addProperty("$ref", "#/definitions/ResponseModel");
  }

  private boolean isPrimitive(APIType apiType)
  {
    return apiType.isPrimitive();
  }

  private void generateApiMethodParam(APIMethod m, APIType param, JsonArray parameters)
  {
    JsonObject p = new JsonObject();
    parameters.add(p);

    p.addProperty("name",param.getName());
    p.addProperty("type",getSwaggerType(param));
    p.addProperty("required", true);
    p.addProperty("in","query");
  }

  private String getSwaggerType(APIType param)
  {
    String type = param.getType();
    if (type==null) return "";
    type = type.toLowerCase();
    if (type.equals("string") || type.equals("boolean") || type.equals("float") || type.equals("double") || type.equals("integer")) return type;
    if (type.equals("int")) return "integer";

    return "#/definitions/" + param.getType();
  }

  private void addProperty(JsonObject def, String name, String type)
  {
    JsonObject properties = null;
    if (!def.has("properties")) {
      properties=new JsonObject();
      def.add("properties", properties);
    }else {
      properties=def.getAsJsonObject("properties");
    }

    JsonObject prop = null;
    if (properties.has(name)) {
      prop=properties.getAsJsonObject(name);
    } else {
      prop = new JsonObject();
      properties.add(name, prop);
    }

    prop.addProperty("type", type);
  }

  private void addRequred(JsonObject def, String... fields)
  {
    JsonArray required = null;
    if (!def.has("required"))
    {
      required = new JsonArray();
      def.add("required", required);
    }
    else
    {
      required = def.getAsJsonArray("required");
    }

    for (String f: fields) {
      required.add(f);
    }
  }

}
