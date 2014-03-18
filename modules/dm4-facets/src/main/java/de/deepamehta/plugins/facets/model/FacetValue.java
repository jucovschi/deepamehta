package de.deepamehta.plugins.facets.model;

import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.TopicModel;

import org.codehaus.jettison.json.JSONObject;

import java.util.List;



/**
 * A facet value as used in update facet calls.
 * Used for both, single-valued facets and multiple-valued facets.
 */
public class FacetValue extends CompositeValueModel {

    private String childTypeUri;

    // ---------------------------------------------------------------------------------------------------- Constructors

    public FacetValue(String childTypeUri) {
        this.childTypeUri = childTypeUri;
    }

    public FacetValue(JSONObject obj) {
        super(obj);
        try {
            if (size() != 1) {
                throw new RuntimeException("There are " + size() + " child type entries (expected is 1)");
            }
            //
            this.childTypeUri = iterator().next();
        } catch (Exception e) {
            throw new RuntimeException("Parsing FacetValue failed (JSONObject=" + obj + ")", e);
        }
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    /**
     * Accesses a single-valued facet.
     */
    public TopicModel getTopic() {
        return getTopic(childTypeUri);
    }

    /**
     * Accesses a multiple-valued facet.
     */
    public List<TopicModel> getTopics() {
        return getTopics(childTypeUri);
    }

    // ---

    /**
     * Convenience method to put a *simple* value in a single-valued facet.
     */
    public FacetValue put(Object value) {
        return (FacetValue) put(childTypeUri, value);
    }

    /**
     * Convenience method to put a *composite* value in a single-valued facet.
     */
    public FacetValue put(CompositeValueModel value) {
        return (FacetValue) put(childTypeUri, value);
    }

    // ---

    /**
     * Adds a by-ID topic reference to a multiple-valued facet.
     */
    public FacetValue addRef(long refTopicId) {
        return (FacetValue) addRef(childTypeUri, refTopicId);
    }

    /**
     * Adds a by-URI topic reference to a multiple-valued facet.
     */
    public FacetValue addRef(String refTopicUri) {
        return (FacetValue) addRef(childTypeUri, refTopicUri);
    }
}