package tv.sage.tools.api;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import tv.sage.tools.api.model.APIGroup;
import tv.sage.tools.api.model.APIMethod;
import tv.sage.tools.api.model.APIModel;
import tv.sage.tools.api.model.APIType;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Generates JavaScript ES6 API for SageTV Remote APIs
 * <p>
 * Created by seans on 14/03/17.
 */
public abstract class JSGenerator {
    AtomicInteger genClasses = new AtomicInteger();
    AtomicInteger genMethods = new AtomicInteger();

    public void generate(APIModel model, File dir) throws FileNotFoundException {
        for (APIGroup group : model.getGroups()) {
            System.out.println("Processing: " + group.getClassName());
            System.out.println("Generating: " + group.getClassName() + ".js");
            File out = dir;
            out.mkdirs();
            generateJSAPI(model, group, out);
            genClasses.incrementAndGet();
        }
    }

    void generateJSMethodBody(PrintWriter w, APIMethod m, APIGroup group) {
        genMethods.getAndIncrement();

        if (!m.isExported()) return;

        if (m.isCanSerialize()) {
            String type = m.getParameters().get(0).getName();
            w.printf("  if (typeof %s.%s !== 'undefined') return new Promise(function(resolve, reject) {\n", type, m.getSerializedField());
            w.printf("     resolve(%s.%s);\n", type, m.getSerializedField());
            w.printf("  });\n");
            w.printf("  return this.sageAPI.invoke(\"%s\", [", m.getName());
            genMethodParameters(w, m, group);
            w.printf("]).then((json)=>{\n");
            w.printf("  if (!json || !json.Result) return null;\n");
            w.printf("  %s.%s=json.Result;\n", type, m.getSerializedField());
            w.printf("  return json.Result;\n");
            w.printf("});\n");

        } else {
            // service API
            w.printf("  return this.sageAPI.invoke(\"%s\", [", m.getName());
            genMethodParameters(w, m, group);
            w.printf("])");
            if (m.getReturnType() != null && !m.getReturnType().equals("void")) {
                w.printf(".then((json)=>{\n");
                w.printf("   if (json && json.Result) return json.Result;\n");
                w.printf("   return json;\n");
                w.printf("});\n");
            } else {
                w.printf(";");
            }
            w.printf("\n");
        }
    }

    private void genMethodParameters(PrintWriter w, APIMethod m, APIGroup group) {
        int i = 0;
        for (APIType t : m.getParameters()) {
            if (i > 0) w.printf(",");
            if (t.isSageObject()) {
                // if the parameter is an Airing object, then SageTV allows for MediaFiles to be passed
                // as well.  So, if our source is a MediaFile, we need to account for that.
                // so the resolver needs to be something like
                // (item.MediaFileID?mediafile:mfid:airiing:airingid")
                if ("Airing".equals(group.getForType())) {
                    w.printf("(%s.MediaFileID?'mediafile:'+%s.MediaFileID:'%s:'+%s.%sID)", t.getName(), t.getName(), group.getForType().toLowerCase(), t.getName(), group.getForType());
                } else {
                    w.printf("'%s:'+%s.%sID", group.getForType().toLowerCase(), t.getName(), group.getForType());
                }
            } else {
                w.printf("%s", t.getName());
            }
            i++;
        }
    }

    public abstract void generateJSAPI(APIModel model, APIGroup group, File dir) throws FileNotFoundException;

    String getJSArgList(APIMethod m) {
        if (m.getParameters() == null | m.getParameters().size() == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (APIType t : m.getParameters()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(t.getName());
        }
        return sb.toString();
    }

    String getAPIClass(APIGroup group) {
        return group.getClassName();
    }

    void writeClassFieldConstants(PrintWriter w, String apiClass, APIGroup group) {
        w.printf("// Field Constants for %s\n", group.getForType());
        for (APIMethod m : group.getMethods()) {
            if (m.isExported() && m.isCanSerialize() && m.isInstance()) {
                w.printf("%s.%s=\"%s\";\n", apiClass, m.getSerializedField(), m.getSerializedField());
            }
        }

        if (apiClass.equals("MediaFileAPI")) {
            // add in metadata constants as well
            String mdKey="MetadataProperties";
            w.printf("// Metadata Field Constants for a MediaFile\n");
            w.printf("%s.%s={};\n", apiClass, mdKey);
            for (APIHelper.Property p: APIHelper.METADATA_PROPERTIES) {
                w.printf("%s.%s.%s=\"%s\";\n", apiClass, mdKey, p.name.replace('.','_'), p.name);
            }
        }
    }


    static void run(String args[], JSGenerator gen) throws FileNotFoundException {
        if (args.length != 2) {
            System.out.println("Uses the SageTV API AST to generateApp JS Remote API Objects");
            System.out.println("Usage: java JSAPIGenerator api.json baseApiOutputDir");
            return;
        }

        File api = new File(args[0]);
        if (!api.exists()) {
            throw new RuntimeException("Not Found: " + api.getAbsolutePath());
        }

        Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
        APIModel model = gson.fromJson(new FileReader(api), APIModel.class);

        File out = new File(args[1]);
        if (!out.exists()) {
            out.mkdirs();
        }
        gen.generate(model, out);

        System.out.printf("Generated %d Classes\n", gen.genClasses.get());
        System.out.printf("Generated %d Methods/APIs\n", gen.genMethods.get());
    }
}
