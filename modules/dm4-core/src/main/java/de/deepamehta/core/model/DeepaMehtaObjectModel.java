package de.deepamehta.core.model;

import de.deepamehta.core.Identifiable;
import de.deepamehta.core.JSONEnabled;

import org.codehaus.jettison.json.JSONObject;



public abstract class DeepaMehtaObjectModel implements Identifiable, JSONEnabled, Cloneable {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    // ### TODO: make these private?
    protected long id;                  // is -1 in models used for a create operation. ### FIXDOC
                                        // is never -1 in models used for an update operation.
    protected String uri;               // is never null in models used for a create operation, may be empty. ### FIXDOC
                                        // may be null in models used for an update operation.
    protected String typeUri;           // is never null in models used for a create operation. ### FIXDOC
                                        // may be null in models used for an update operation.
    protected SimpleValue value;        // is never null in models used for a create operation, may be constructed
                                        //                                                   on empty string. ### FIXDOC
                                        // may be null in models used for an update operation.
    protected ChildTopicsModel childTopics; // is never null, may be empty. ### FIXDOC

    // ---------------------------------------------------------------------------------------------------- Constructors

    public DeepaMehtaObjectModel(String typeUri) {
        this(-1, typeUri);
    }

    public DeepaMehtaObjectModel(String typeUri, SimpleValue value) {
        this(null, typeUri, value);
    }

    public DeepaMehtaObjectModel(String typeUri, ChildTopicsModel childTopics) {
        this(null, typeUri, childTopics);
    }

    public DeepaMehtaObjectModel(String uri, String typeUri) {
        this(-1, uri, typeUri, null, null);
    }

    public DeepaMehtaObjectModel(String uri, String typeUri, SimpleValue value) {
        this(-1, uri, typeUri, value, null);
    }

    public DeepaMehtaObjectModel(String uri, String typeUri, ChildTopicsModel childTopics) {
        this(-1, uri, typeUri, null, childTopics);
    }

    public DeepaMehtaObjectModel(long id) {
        this(id, null, null);
    }

    public DeepaMehtaObjectModel(long id, ChildTopicsModel childTopics) {
        this(id, null, childTopics);
    }

    public DeepaMehtaObjectModel(long id, String typeUri) {
        this(id, typeUri, null);
    }

    public DeepaMehtaObjectModel(long id, String typeUri, ChildTopicsModel childTopics) {
        this(id, null, typeUri, null, childTopics);
    }

    /**
     * @param   id          Optional (-1 is a valid value and represents "not set").
     * @param   uri         Optional (<code>null</code> is a valid value).
     * @param   typeUri     Mandatory in the context of a create operation.
     *                      Optional (<code>null</code> is a valid value) in the context of an update opereation.
     * @param   value       Optional (<code>null</code> is a valid value).
     * @param   childTopics Optional (<code>null</code> is a valid value and is transformed into an empty composite).
     */
    public DeepaMehtaObjectModel(long id, String uri, String typeUri, SimpleValue value, ChildTopicsModel childTopics) {
        this.id = id;
        this.uri = uri;
        this.typeUri = typeUri;
        this.value = value;
        this.childTopics = childTopics != null ? childTopics : new ChildTopicsModel();
    }

    public DeepaMehtaObjectModel(DeepaMehtaObjectModel model) {
        this(model.id, model.uri, model.typeUri, model.value, model.childTopics);
    }

    /**
     * Used for regular objects: topics and associations.
     */
    public DeepaMehtaObjectModel(JSONObject model) {
        try {
            this.id          = model.optLong("id", -1);
            this.uri         = model.optString("uri", null);
            this.typeUri     = model.optString("type_uri", null);
            this.value       = model.has("value") ? new SimpleValue(model.get("value")) : null;
            this.childTopics = model.has("childs") ? new ChildTopicsModel(model.getJSONObject("childs"))
                                                   : new ChildTopicsModel();
        } catch (Exception e) {
            throw new RuntimeException("Parsing DeepaMehtaObjectModel failed (JSONObject=" + model + ")", e);
        }
    }

    /**
     * Used for types: topic types and association types.
     */
    public DeepaMehtaObjectModel(JSONObject typeModel, String typeUri) {
        try {
            this.id = typeModel.optLong("id", -1);
            this.uri = typeModel.optString("uri");
            this.typeUri = typeUri;
            this.value = new SimpleValue(typeModel.get("value"));
            this.childTopics = new ChildTopicsModel();
        } catch (Exception e) {
            throw new RuntimeException("Parsing DeepaMehtaObjectModel failed (JSONObject=" + typeModel +
                ", typeUri=\"" + typeUri + "\")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // --- ID ---

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    // --- URI ---

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    // --- Type URI ---

    public String getTypeUri() {
        return typeUri;
    }

    public void setTypeUri(String typeUri) {
        this.typeUri = typeUri;
    }

    // --- Simple Value ---

    public SimpleValue getSimpleValue() {
        return value;
    }

    // ---

    public void setSimpleValue(String value) {
        setSimpleValue(new SimpleValue(value));
    }

    public void setSimpleValue(int value) {
        setSimpleValue(new SimpleValue(value));
    }

    public void setSimpleValue(long value) {
        setSimpleValue(new SimpleValue(value));
    }

    public void setSimpleValue(boolean value) {
        setSimpleValue(new SimpleValue(value));
    }

    public void setSimpleValue(SimpleValue value) {
        this.value = value;
    }

    // --- Child Topics ---

    public ChildTopicsModel getChildTopicsModel() {
        return childTopics;
    }

    public void setChildTopicsModel(ChildTopicsModel childTopics) {
        this.childTopics = childTopics;
    }

    // ---

    public void set(DeepaMehtaObjectModel model) {
        setId(model.getId());
        setUri(model.getUri());
        setTypeUri(model.getTypeUri());
        setSimpleValue(model.getSimpleValue());
        setChildTopicsModel(model.getChildTopicsModel());
    }

    // ---

    public abstract RoleModel createRoleModel(String roleTypeUri);



    // === Serialization ===

    @Override
    public JSONObject toJSON() {
        try {
            // Note: for models used for topic/association enrichment (e.g. timestamps, permissions)
            // default values must be set in case they are not fully initialized.
            setDefaults();
            //
            JSONObject o = new JSONObject();
            o.put("id", id);
            o.put("uri", uri);
            o.put("type_uri", typeUri);
            o.put("value", value.value());
            o.put("childs", childTopics.toJSON());
            return o;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }



    // === Java API ===

    @Override
    public DeepaMehtaObjectModel clone() {
        try {
            DeepaMehtaObjectModel model = (DeepaMehtaObjectModel) super.clone();
            model.childTopics = childTopics.clone();
            return model;
        } catch (Exception e) {
            throw new RuntimeException("Cloning a DeepaMehtaObjectModel failed", e);
        }
    }

    @Override
    public boolean equals(Object o) {
        return ((DeepaMehtaObjectModel) o).id == id;
    }

    @Override
    public int hashCode() {
        return ((Long) id).hashCode();
    }

    @Override
    public String toString() {
        return "id=" + id + ", uri=\"" + uri + "\", typeUri=\"" + typeUri + "\", value=\"" + value +
            "\", childTopics=" + childTopics;
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    // ### TODO: a principal copy exists in Neo4jStorage.
    // Should this be public? It is not meant to be called by the user.
    private void setDefaults() {
        if (getUri() == null) {
            setUri("");
        }
        if (getSimpleValue() == null) {
            setSimpleValue("");
        }
    }
}
