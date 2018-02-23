package tv.sage.tools.api.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by seans on 12/03/17.
 */
public class APIModel {
    private String version;
    private List<APIGroup> groups = new ArrayList<>();
    private APICollectedTypes types = new APICollectedTypes();

    public String getVersion() {
        return version;
    }

    public APIModel setVersion(String version) {
        this.version = version;
        return this;
    }

    public List<APIGroup> getGroups() {
        return groups;
    }

    public APIModel setGroups(List<APIGroup> groups) {
        this.groups = groups;
        return this;
    }

    public APIModel() {

    }

    public APICollectedTypes getTypes() {
        return types;
    }

    public APIGroup findAPIForType(APIType type) {
        for (APIGroup g : getGroups()) {
            if (type.getType().equals(g.getForType())) return g;
        }
        return null;
    }
}
