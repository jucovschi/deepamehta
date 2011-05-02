package de.deepamehta.core.model;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;



public class AssociationRole {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private long   assocId;
    private String roleTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public AssociationRole(long assocId, String roleTypeUri) {
        this.assocId = assocId;
        this.roleTypeUri = roleTypeUri;
    }

    public AssociationRole(JSONObject assocRole) {
        try {
            this.assocId = assocRole.getLong("assoc_id");
            this.roleTypeUri = assocRole.getString("role_type_uri");
        } catch (Exception e) {
            throw new RuntimeException("Parsing AssociationRole failed (JSONObject=" + assocRole + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    public long getAssociationId() {
        return assocId;
    }

    public String getRoleTypeUri() {
        return roleTypeUri;
    }

    // ---

    public JSONObject toJSON() {
        try {
            JSONObject o = new JSONObject();
            o.put("assoc_id", assocId);
            o.put("role_type_uri", roleTypeUri);
            return o;
        } catch (JSONException e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }

    // ---

    @Override
    public String toString() {
        return "\n        association role (roleTypeUri=\"" + roleTypeUri + "\", assocId=" + assocId + ")";
    }
}