package tv.sage.tools.api;

import tv.sage.tools.api.model.APIGroup;
import tv.sage.tools.api.model.APIMethod;
import tv.sage.tools.api.model.APIModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Generates JavaScript RequireJS API for SageTV Remote APIs
 * <p>
 * Created by seans on 14/03/17.
 */
public class JSAPIRequireJSGenerator extends JSGenerator {
    public void generateJSAPI(APIModel model, APIGroup group, File dir) throws FileNotFoundException {
        String apiClass = getAPIClass(group);

        PrintWriter w = new PrintWriter(new File(dir, apiClass + ".js"));

        w.printf("define(function() {\n" +
                "    \"use strict\";\n" +
                "\n" +
                "    function %s(sageAPI) {\n" +
                "        this.sageAPI = sageAPI;\n" +
                "    }\n" +
                "\n", apiClass);


        for (APIMethod m : group.getMethods()) {
            if (m.isExported()) {
                w.printf("\n");
                w.printf("    // %s\n", m.getName());

                w.printf("%s.prototype.%s = function(%s) {\n", apiClass, m.getName(), getJSArgList(m));
                generateJSMethodBody(w, m, group);
                w.printf("};\n");
            }
        }

        writeClassFieldConstants(w, apiClass, group);

        w.printf("    return %s;\n" +
                "});\n", apiClass);

        w.flush();
        w.close();
    }

    public static void main(String args[]) throws FileNotFoundException {
        run(args, new JSAPIRequireJSGenerator());
    }
}
