package tv.sage.tools.api;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tv.sage.tools.api.model.APIGroup;
import tv.sage.tools.api.model.APIMethod;
import tv.sage.tools.api.model.APIModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * Generates GSON Json Serializers for the SageTV API Objects
 * <p>
 * Created by seans on 14/03/17.
 */
public class JSONSerializationGenerator {
    public void generate(APIModel model, String pkg, File dir) throws FileNotFoundException {
        File outFile = new File(dir, "sage/remote/GSONConfigurator.java");
        outFile.getParentFile().mkdirs();
        PrintWriter w = new PrintWriter(outFile);

        w.write("package sage.remote;\n\n");
        w.write("import sage.epg.sd.gson.GsonBuilder;\n");


        w.write("public class GSONConfigurator\n");
        w.write("{\n");
        w.write("  public static GsonBuilder configureTypes(GsonBuilder builder) {\n");

        for (APIGroup group : model.getGroups()) {
            System.out.println("Processing: " + group.getClassName());
            if (group.isSerializable()) {
                System.out.println("Generating: " + group.getClassName());
                File out = new File(dir, packageToDir(pkg));
                out.mkdirs();
                generateSerializer(model, group, pkg, out);

                w.printf("builder.registerTypeAdapter(%s.%s.class, new %s());\n", group.getPackageName(), group.getForType(), getAPIClass(group));

            }
        }

        w.write("    return builder;\n");
        w.write("  }\n");
        w.write("}\n");

        w.flush();
        w.close();
    }

    private String packageToDir(String pkg) {
        return pkg.replace('.', '/');
    }

    private void generateSerializer(APIModel model, APIGroup group, String pkg, File dir) throws FileNotFoundException {
        PrintWriter w = new PrintWriter(new File(dir, getAPIClass(group) + ".java"));

        w.printf("package %s;\n", pkg);
        w.printf("import %s.%s;\n", group.getPackageName(), group.getForType());
        w.printf("import sage.epg.sd.gson.JsonElement;\n" +
                "import sage.epg.sd.gson.JsonObject;\n" +
                "import sage.epg.sd.gson.JsonSerializationContext;\n" +
                "import java.lang.reflect.InvocationTargetException;\n" +
                "import java.lang.reflect.Type;\n\n");
        w.printf("public class %s extends BaseJsonSerializer<%s> {\n", getAPIClass(group), group.getForType());
        w.printf("  public %sJsonSerializer() {\n" +
                "  }\n\n", group.getForType());

        w.printf("  public JsonElement serialize(%s src, Type typeOfSrc, JsonSerializationContext context) throws InvocationTargetException {\n", group.getForType());

        w.printf("     if (sage.Sage.DBG) System.out.println(\"JSON Serializing: \"+typeOfSrc);\n");

        w.printf("  JsonObject o = new JsonObject();\n");
        for (APIMethod m : group.getMethods()) {
            if (m.isCanSerialize() && m.isExported()) {
                w.printf("\n");
                w.printf("    // %s\n", m.getName());
                if (m.getReturnType().isSageObject()) {
                    w.printf("     if (sage.Sage.DBG) System.out.print(\"Serializing Referenced Object: %s.%s\\n\");\n", group.getClassName(), m.getName());
                    APIGroup otherType = model.findAPIForType(m.getReturnType());
                    if (otherType == null) {
                        System.out.println("No Sage Type: '" + m.getReturnType().getType() + "' in " + m.getName());
                    }
                    if (otherType != null && otherType.getGetIdMethod() != null) {
                        // dump nested object as ID reference
                        w.printf("    o.add(\"%sId\", context.serialize(safeApi(\"%s\", safeApi(\"%s\", src))));\n", m.getSerializedField(), otherType.getGetIdMethod(), m.getName());
                    } else {
                        // dump the full object
                        w.printf("    o.add(\"%s\", context.serialize(safeApi(\"%s\", src)));\n", m.getSerializedField(), m.getName());
                    }
                } else {
                    w.printf("     if (sage.Sage.DBG) System.out.print(\"Serializing Field: %s.%s\\n\");\n", group.getClassName(), m.getName());
                    w.printf("    o.add(\"%s\", context.serialize(safeApi(\"%s\", src)));\n", m.getSerializedField(), m.getName());
                }
            }
        }

        w.printf("   return o;\n");
        w.printf("}\n");
        w.printf("}\n");
        w.flush();
        w.close();
    }

    private String getAPIClass(APIGroup group) {
        return group.getForType() + "JsonSerializer";
    }

    public static void main(String args[]) throws FileNotFoundException {
        if (args.length != 2) {
            System.out.println("Uses the SageTV API AST to generateApp Json Serialization Objects");
            System.out.println("Usage: java JSONSerializationGenerator api.json baseApiOutputDir");
            return;
        }

        File api = new File(args[0]);
        if (!api.exists()) {
            throw new RuntimeException("Not Found: " + api.getAbsolutePath());
        }

        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        APIModel model = gson.fromJson(new FileReader(api), APIModel.class);

        JSONSerializationGenerator gen = new JSONSerializationGenerator();
        File out = new File(args[1]);
        if (!out.exists()) {
            out.mkdirs();
        }
        gen.generate(model, "sage.remote", out);
    }

}
