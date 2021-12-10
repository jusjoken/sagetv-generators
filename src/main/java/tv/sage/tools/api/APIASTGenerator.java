package tv.sage.tools.api;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.Comment;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import tv.sage.tools.api.model.APIGroup;
import tv.sage.tools.api.model.APIMethod;
import tv.sage.tools.api.model.APIModel;
import tv.sage.tools.api.model.APIType;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates an API Abstract Syntax Tree for the SageTV API.  This api.json is used to generateApp other SageTV
 * objects and documentation.
 * <p>
 * Created by seans on 09/03/17.
 */
public class APIASTGenerator {
    static List<String> NON_INSTANCE_FIELDS = Arrays.asList("MediaFileForAiring");

    static Map<String, APIHelper> helpers = new HashMap<>();

    static {
        helpers.put("MediaNode", new APIHelper().setPackageName("sage.vfs"));
        helpers.put("SystemMessage", new APIHelper().setPackageName("sage.msg"));
    }

    public APIASTGenerator() {
    }

    APIModel buildAPIs(File dir) throws FileNotFoundException {
        APIModel apiModel = new APIModel();
        apiModel.setVersion("9.2");
        List<APIGroup> apiGroups = apiModel.getGroups();

        File[] files = dir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return file.getName().toLowerCase().endsWith(".java");
            }
        });

        for (File f : files) {
            apiGroups.add(processFile(apiModel, f));
        }

        apiModel.getTypes().getNonPrimitiveTypes().removeAll(apiModel.getTypes().getSageTypes());

        postProcessModel(apiModel);

        return apiModel;
    }

    private void postProcessModel(APIModel model) {
        findSageObjectArraysInReturns(model);
        sortGroups(model);
    }

    private void sortGroups(APIModel model) {
        Collections.sort(model.getGroups(), (a1, a2) -> a1.getClassName().compareTo(a2.getClassName()));
        for (APIGroup g : model.getGroups()) {
            Collections.sort(g.getMethods(), (a1, a2) -> a1.getName().compareTo(a2.getName()));
        }
    }

    private void findSageObjectArraysInReturns(APIModel model) {
        for (APIGroup g : model.getGroups()) {
            for (APIMethod m : g.getMethods()) {
                if (model.getTypes().getSageTypes().contains(m.getReturnType().getType())) {
                    m.getReturnType().setIsSageObject(true);
                }
            }
        }
    }

    APIGroup processFile(APIModel model, File file) throws FileNotFoundException {
        APIGroup apiGroup = new APIGroup();
        apiGroup.setFileName(file.getName());

        CompilationUnit cu = JavaParser.parse(file);

        //apiGroup.setPackageName(cu.getPackageDeclaration().get().getName().asString());
        apiGroup.setPackageName("sage");

        NodeList<TypeDeclaration<?>> types = cu.getTypes();
        TypeDeclaration<?> t = types.get(0); // sage api has single type

        String apiName = t.getNameAsString();
        String typeName = apiName;
        apiGroup.setClassName(t.getNameAsString());
        if (apiName.endsWith("API")) {
            typeName = apiName.substring(0, apiName.length() - 3);
            apiGroup.setForType(typeName);
            APIHelper helper = helpers.get(typeName);
            if (typeName != null && helper != null && helper.getPackageName() != null) {
                apiGroup.setPackageName(helper.getPackageName());
            }
            model.getTypes().getSageTypes().add(typeName);
        }

        apiGroup.setComment(getComments(t.getComment()));

        List<APIMethod> methods = apiGroup.getMethods();

        for (MethodDeclaration m : t.getMethods()) {
            addMethod(model, m, apiGroup, methods, typeName);
        }

        return apiGroup;
    }

    private void addMethod(APIModel model, MethodDeclaration m, APIGroup api, List<APIMethod> methods, String apiType) {
        APIMethod apiMethod = new APIMethod();
        methods.add(apiMethod);
        apiMethod.setName(m.getNameAsString());
        String comments = getComments(m.getComment());
        apiMethod.setComment(comments);
        String since = parseSince(comments);
        if (since != null) {
            apiMethod.setSince(since.trim());
        }

        APIType ret = new APIType();
        apiMethod.setReturnType(ret);

        String retType = m.getType().toString();
        if (isArray(retType)) {
            retType = getTypeFromArray(retType);
            ret.setArray(true);
        }
        ret.setType(retType);
        ret.setIsPrimitive(isPrimitive(retType));
        if (isPrimitive(retType)) {
            model.getTypes().getPrimitiveTypes().add(retType);
        } else {
            model.getTypes().getNonPrimitiveTypes().add(retType);
        }

        if (m.getNameAsString().equals("Is" + apiType + "Object")) {
            api.setIsTypeMethod(m.getNameAsString());
        }

        apiMethod.setExported(parseExported(comments));

        NodeList<Parameter> parameters = m.getParameters();
        if (parameters.size() > 0) {
            List<APIType> params = apiMethod.getParameters();

            boolean first = true;
            boolean instanceLevel = false;
            String ptype = null;
            for (Parameter param : parameters) {
                APIType p = new APIType();
                params.add(p);

                ptype = param.getType().toString();
                if (isArray(ptype)) {
                    ptype = getTypeFromArray(ptype);
                    p.setArray(true);
                }
                p.setType(ptype);
                p.setIsPrimitive(isPrimitive(ptype));

                if (isPrimitive(ptype)) {
                    model.getTypes().getPrimitiveTypes().add(ptype);
                } else {
                    model.getTypes().getNonPrimitiveTypes().add(ptype);
                }

                p.setName(param.getNameAsString());
                if (first) {
                    first = false;
                    if (ptype.equals(apiType) || (ptype.equals("Object") && param.getNameAsString().equals(apiType))) {
                        instanceLevel = true;
                    }
                }
                if (param.getNameAsString().equals(apiType)) {
                    p.setIsSageObject(true);
                }
            }
            if (instanceLevel) {
                apiMethod.setInstance(true);
                api.setSerializable(true);
            }
            if (params.size() == 1 && instanceLevel && !m.getType().toString().equalsIgnoreCase("void")) {
                String mname = m.getNameAsString();
                if (!mname.equals("Is" + apiType + "Object")) {
                    if (mname.startsWith("Can") || mname.startsWith("Is") || mname.startsWith("Has") || mname.startsWith("Get")) {
                        // methods that are instance but also return our type are not instance
                        if (!apiMethod.getReturnType().getType().equals(apiType)) {
                            if (!NON_INSTANCE_FIELDS.contains(getSerializedField(mname, api.getForType()))) {
                                apiMethod.setCanSerialize(true);
                                apiMethod.setSerializedField(getSerializedField(mname, api.getForType()));
                            }
                        }
                    }
                }
            }
            String firstParmType = null;
            firstParmType = params.get(0).getType();
            if (retType.equals(apiType) && m.getNameAsString().toLowerCase().endsWith("forid") && params.size() == 1 && (firstParmType.equals("Object") || firstParmType.equals("String") || firstParmType.equals("int"))) {
                apiMethod.setForId(true);
                api.setForIdMethod(m.getNameAsString());
            }
            if (m.getNameAsString().equalsIgnoreCase("Get" + apiType + "ID")) {
                api.setGetIdMethod(m.getNameAsString());
            }
        }
    }

    static Pattern EXPORTED_PATTERN = Pattern.compile("@exported\\s*([a-z]+)");

    private boolean parseExported(String in) {
        if (in == null) return true;
        if (!in.contains("@exported")) return true;

        Matcher m = EXPORTED_PATTERN.matcher(in);
        if (m.find()) {
            return m.group(1).equalsIgnoreCase("true");
        }

        return false;
    }

    boolean isArray(String in) {
        if (in == null) return false;
        return in.endsWith("[]");
    }

    String getTypeFromArray(String in) {
        if (in == null) return null;
        if (isArray(in)) {
            in = in.substring(0, in.length() - 2);
            return getTypeFromArray(in);
        }
        return in;
    }

    private boolean isPrimitive(String retType) {
        if (retType == null) return true;
        return retType.equalsIgnoreCase("String") || retType.equalsIgnoreCase("Integer")
                || retType.equalsIgnoreCase("Float") || retType.equalsIgnoreCase("Double") || retType.equalsIgnoreCase("Char")
                || retType.equalsIgnoreCase("Short") || retType.equalsIgnoreCase("int") || retType.equalsIgnoreCase("Byte")
                || retType.equalsIgnoreCase("Long") || retType.equalsIgnoreCase("Boolean") || retType.equalsIgnoreCase("Number")
                ;
    }

    private String getSerializedField(String mname, String forType) {
        if (mname.startsWith("Get")) {
            mname = mname.substring(3);
        }

        // to be compatible with normal sagex, we'll keep this with the object prefix
//    if (mname.startsWith(forType)) {
//      if (!mname.endsWith("ID"))
//        mname = mname.substring(forType.length());
//    }
        return mname;
    }

    private String getComments(Optional<Comment> c) {
        String comment = null;
        if (c.isPresent())
            comment = c.get().getContent();
        if (comment != null) {
            comment = comment.replaceAll(Pattern.quote("/*"), "");
            comment = comment.replaceAll(Pattern.quote("*/"), "");
            comment = comment.replaceAll(Pattern.quote("*"), "");
        }
        return comment.trim();
    }

    static Pattern SINCE_PATTERN = Pattern.compile("@since\\s*([0-9.]+)");

    private String parseSince(String in) {
        Matcher m = SINCE_PATTERN.matcher(in);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    public static void main(String args[]) throws FileNotFoundException {
        if (args.length != 2) {
            System.out.println("Using the output from StudioAPIGenerator, this will create an AST of the SageTV APIs.");
            System.out.println("Usage: java APIASTGenerator tmpGenApi api.json");
            return;
        }

        File tmpApiDir = new File(args[0]);
        if (!tmpApiDir.exists()) throw new RuntimeException("Missing Generated Studio APIs.  Run StudioAPIProcess.");

        File apiJsonOutputFile = new File(args[1]);
        if (!apiJsonOutputFile.getParentFile().exists()) {
            apiJsonOutputFile.getParentFile().mkdirs();
        }

        APIASTGenerator g = new APIASTGenerator();
        APIModel api = g.buildAPIs(tmpApiDir);

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .registerTypeAdapter(Boolean.class, new TypeAdapter<Boolean>() {
                    @Override
                    public void write(JsonWriter out, Boolean value) throws IOException {
                        if (!value) {
                            out.nullValue();
                        } else {
                            out.value(value);
                        }
                    }

                    @Override
                    public Boolean read(JsonReader in) throws IOException {
                        return in.nextBoolean();
                    }
                })
                .create();
        String json = gson.toJson(api);
        // System.out.println(json);
        PrintWriter pw = new PrintWriter(apiJsonOutputFile);
        pw.print(json);
        pw.flush();
        pw.close();

        System.out.println("Created: " + apiJsonOutputFile.getAbsolutePath());
    }
}
