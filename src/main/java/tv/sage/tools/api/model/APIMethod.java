package tv.sage.tools.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by seans on 12/03/17.
 */
public class APIMethod {
    private APIType returnType;
    private String name;
    private String comment;
    private String since;
    private List<APIType> parameters = new ArrayList<>();
    private boolean isInstance;
    private boolean canSerialize;
    private boolean isForId;
    private String serializedField;
    private boolean exported;

    public APIMethod() {
    }

    public APIType getReturnType() {
        return returnType;
    }

    public APIMethod setReturnType(APIType returnType) {
        this.returnType = returnType;
        return this;
    }

    public String getName() {
        return name;
    }

    public APIMethod setName(String name) {
        this.name = name;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public APIMethod setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public String getSince() {
        return since;
    }

    public APIMethod setSince(String since) {
        this.since = since;
        return this;
    }

    public List<APIType> getParameters() {
        return parameters;
    }

    public APIMethod setParameters(List<APIType> parameters) {
        this.parameters = parameters;
        return this;
    }

    public boolean isInstance() {
        return isInstance;
    }

    public APIMethod setInstance(boolean instance) {
        isInstance = instance;
        return this;
    }

    public boolean isCanSerialize() {
        return canSerialize;
    }

    public APIMethod setCanSerialize(boolean canSerialize) {
        this.canSerialize = canSerialize;
        return this;
    }

    public boolean isForId() {
        return isForId;
    }

    public APIMethod setForId(boolean forId) {
        isForId = forId;
        return this;
    }

    public void setSerializedField(String serializedField) {
        this.serializedField = serializedField;
    }

    public String getSerializedField() {
        return serializedField;
    }

    public void setExported(boolean exported) {
        this.exported = exported;
    }

    public boolean isExported() {
        return exported;
    }
}
