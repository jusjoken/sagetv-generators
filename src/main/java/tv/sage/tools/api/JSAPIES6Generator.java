package tv.sage.tools.api;

import tv.sage.tools.api.model.APIGroup;
import tv.sage.tools.api.model.APIMethod;
import tv.sage.tools.api.model.APIModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Generates JavaScript ES6 API for SageTV Remote APIs
 * <p>
 * Created by seans on 14/03/17.
 */
public class JSAPIES6Generator extends JSGenerator {
    public void generateJSAPI(APIModel model, APIGroup group, File dir) throws FileNotFoundException {
        String apiClass = getAPIClass(group);

        PrintWriter w = new PrintWriter(new File(dir, apiClass + ".js"));

        w.printf("class %s {\n", apiClass);
        w.printf("   constructor(sageAPI) {\n");
        w.printf("      this.sageAPI=sageAPI;\n");
        w.printf("   }\n\n");

        for (APIMethod m : group.getMethods()) {
            if (m.isExported()) {
                w.printf("\n");
                w.printf("    // %s\n", m.getName());

                w.printf("   %s(%s) {\n", m.getName(), getJSArgList(m));
                generateJSMethodBody(w, m, group);
                w.printf("   }\n");
            }
        }

        w.printf("}\n\n");

        writeClassFieldConstants(w, apiClass, group);

        w.printf("    export default %s;\n", apiClass);

        w.flush();
        w.close();
    }

    public static void main(String args[]) throws FileNotFoundException {
        run(args, new JSAPIES6Generator());
    }

}
