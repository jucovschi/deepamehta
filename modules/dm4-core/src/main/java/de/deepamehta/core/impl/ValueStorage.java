package de.deepamehta.core.impl;

import de.deepamehta.core.AssociationDefinition;
import de.deepamehta.core.Topic;
import de.deepamehta.core.Type;
import de.deepamehta.core.model.AssociationModel;
import de.deepamehta.core.model.CompositeValueModel;
import de.deepamehta.core.model.DeepaMehtaObjectModel;
import de.deepamehta.core.model.RelatedTopicModel;
import de.deepamehta.core.model.SimpleValue;
import de.deepamehta.core.model.TopicModel;
import de.deepamehta.core.model.TopicReferenceModel;
import de.deepamehta.core.model.TopicRoleModel;
import de.deepamehta.core.service.ClientState;
import de.deepamehta.core.service.Directives;
import de.deepamehta.core.service.ResultList;
import de.deepamehta.core.util.JavaUtils;

import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;



/**
 * Helper for storing/fetching simple values and composite value models.
 */
class ValueStorage {

    // ------------------------------------------------------------------------------------------------------- Constants

    private static final String LABEL_CHILD_SEPARATOR = " ";
    private static final String LABEL_TOPIC_SEPARATOR = ", ";

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private EmbeddedService dms;

    private Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    ValueStorage(EmbeddedService dms) {
        this.dms = dms;
    }

    // ----------------------------------------------------------------------------------------- Package Private Methods

    /**
     * Recursively fetches the composite value (child topic models) of the given parent object model and updates it
     * in-place.
     */
    void fetchCompositeValue(DeepaMehtaObjectModel parent) {
        try {
            Type type = getType(parent);
            if (!type.getDataTypeUri().equals("dm4.core.composite")) {
                return;
            }
            //
            for (AssociationDefinition assocDef : type.getAssocDefs()) {
                fetchChildTopics(parent, assocDef);
            }
        } catch (Exception e) {
            throw new RuntimeException("Fetching composite value of object " + parent.getId() + " failed (" +
                parent + ")", e);
        }
    }

    /**
     * Recursively fetches the child topic models of the given parent object model and updates it in-place.
     * <p>
     * Works for both, "one" and "many" association definitions.
     *
     * @param   assocDef    The child topic models according to this association definition are fetched.
     */
    void fetchChildTopics(DeepaMehtaObjectModel parent, AssociationDefinition assocDef) {
        CompositeValueModel comp = parent.getCompositeValueModel();
        String cardinalityUri = assocDef.getChildCardinalityUri();
        String childTypeUri   = assocDef.getChildTypeUri();
        if (cardinalityUri.equals("dm4.core.one")) {
            RelatedTopicModel childTopic = fetchChildTopic(parent.getId(), assocDef);
            // Note: topics just created have no child topics yet
            if (childTopic != null) {
                comp.put(childTypeUri, childTopic);
                fetchCompositeValue(childTopic);    // recursion
            }
        } else if (cardinalityUri.equals("dm4.core.many")) {
            for (RelatedTopicModel childTopic : fetchChildTopics(parent.getId(), assocDef)) {
                comp.add(childTypeUri, childTopic);
                fetchCompositeValue(childTopic);    // recursion
            }
        } else {
            throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
        }
    }

    // ---

    /**
     * Stores and indexes the specified model's value, either a simple value or a composite value (child topics).
     * Depending on the model type's data type dispatches either to storeSimpleValue() or to storeCompositeValue().
     * <p>
     * Called to store the initial value of a newly created topic/association.
     */
    void storeValue(DeepaMehtaObjectModel model, ClientState clientState, Directives directives) {
        if (getType(model).getDataTypeUri().equals("dm4.core.composite")) {
            storeCompositeValue(model, clientState, directives);
            refreshLabel(model);
        } else {
            storeSimpleValue(model);
        }
    }

    // ---

    /**
     * Prerequisite: this is a composite object.
     */
    void refreshLabel(DeepaMehtaObjectModel model) {
        try {
            String label = buildLabel(model);
            setSimpleValue(model, new SimpleValue(label));
        } catch (Exception e) {
            throw new RuntimeException("Refreshing label of object " + model.getId() + " failed (" + model + ")", e);
        }
    }

    void setSimpleValue(DeepaMehtaObjectModel model, SimpleValue value) {
        if (value == null) {
            throw new IllegalArgumentException("Tried to set a null SimpleValue (" + this + ")");
        }
        // update memory
        model.setSimpleValue(value);
        // update DB
        storeSimpleValue(model);
    }



    // === Helper ===

    /**
     * Creates an association between the given parent object ("Parent" role) and the referenced topic ("Child" role).
     * The association type is taken from the given association definition.
     *
     * @return  the resolved child topic, including its composite values.
     */
    Topic associateReferencedChildTopic(DeepaMehtaObjectModel parent, TopicReferenceModel childTopicRef,
                                                              AssociationDefinition assocDef, ClientState clientState) {
        if (childTopicRef.isReferenceById()) {
            long childTopicId = childTopicRef.getId();
            associateChildTopic(parent, childTopicId, assocDef, clientState);
            // Note: the resolved topic must be fetched including its composite value.
            // It might be required at client-side.
            return dms.getTopic(childTopicId, true);                            // fetchComposite=true
        } else if (childTopicRef.isReferenceByUri()) {
            String childTopicUri = childTopicRef.getUri();
            associateChildTopic(parent, childTopicUri, assocDef, clientState);
            // Note: the resolved topic must be fetched including its composite value.
            // It might be required at client-side.
            return dms.getTopic("uri", new SimpleValue(childTopicUri), true);   // fetchComposite=true
        } else {
            throw new RuntimeException("Invalid topic reference (" + childTopicRef + ")");
        }
    }

    void associateChildTopic(DeepaMehtaObjectModel parent, long childTopicId, AssociationDefinition assocDef,
                                                                              ClientState clientState) {
        associateChildTopic(parent, new TopicRoleModel(childTopicId, "dm4.core.child"), assocDef, clientState);
    }

    void associateChildTopic(DeepaMehtaObjectModel parent, String childTopicUri, AssociationDefinition assocDef,
                                                                                 ClientState clientState) {
        associateChildTopic(parent, new TopicRoleModel(childTopicUri, "dm4.core.child"), assocDef, clientState);
    }

    // ---

    /**
     * Convenience method to get the (attached) type of a DeepaMehta object model.
     * The type is obtained from the core service's type cache.
     */
    Type getType(DeepaMehtaObjectModel model) {
        if (model instanceof TopicModel) {
            return dms.getTopicType(model.getTypeUri());
        } else if (model instanceof AssociationModel) {
            return dms.getAssociationType(model.getTypeUri());
        }
        throw new RuntimeException("Unexpected model: " + model);
    }



    // ------------------------------------------------------------------------------------------------- Private Methods

    /**
     * Stores and indexes the simple value of the specified topic or association model.
     * Determines the index key and index modes.
     */
    private void storeSimpleValue(DeepaMehtaObjectModel model) {
        Type type = getType(model);
        if (model instanceof TopicModel) {
            dms.storageDecorator.storeTopicValue(
                model.getId(),
                model.getSimpleValue(),
                type.getIndexModes(),
                type.getUri(),
                getIndexValue(model)
            );
        } else if (model instanceof AssociationModel) {
            dms.storageDecorator.storeAssociationValue(
                model.getId(),
                model.getSimpleValue(),
                type.getIndexModes(),
                type.getUri(),
                getIndexValue(model)
            );
        }
    }

    /**
     * Called to store the initial value of a newly created topic/association.
     * Just prepares the arguments and calls storeChildTopics() repetitively.
     * <p>
     * Note: the given model can contain childs not defined in the type definition.
     * Only the childs defined in the type definition are stored.
     */
    private void storeCompositeValue(DeepaMehtaObjectModel parent, ClientState clientState, Directives directives) {
        CompositeValueModel model = null;
        try {
            model = parent.getCompositeValueModel();
            for (AssociationDefinition assocDef : getType(parent).getAssocDefs()) {
                String childTypeUri   = assocDef.getChildTypeUri();
                String cardinalityUri = assocDef.getChildCardinalityUri();
                TopicModel childTopic        = null;     // only used for "one"
                List<TopicModel> childTopics = null;     // only used for "many"
                if (cardinalityUri.equals("dm4.core.one")) {
                    childTopic = model.getTopic(childTypeUri, null);        // defaultValue=null
                    // skip if not contained in create request
                    if (childTopic == null) {
                        continue;
                    }
                } else if (cardinalityUri.equals("dm4.core.many")) {
                    childTopics = model.getTopics(childTypeUri, null);      // defaultValue=null
                    // skip if not contained in create request
                    if (childTopics == null) {
                        continue;
                    }
                } else {
                    throw new RuntimeException("\"" + cardinalityUri + "\" is an unexpected cardinality URI");
                }
                //
                storeChildTopics(childTopic, childTopics, parent, assocDef, clientState, directives);
            }
        } catch (Exception e) {
            throw new RuntimeException("Storing composite value of object " + parent.getId() + " failed (" +
                model + ")", e);
        }
    }

    // ---

    private void storeChildTopics(TopicModel childTopic, List<TopicModel> childTopics, DeepaMehtaObjectModel parent,
                                       AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        String assocTypeUri = assocDef.getTypeUri();
        boolean one = childTopic != null;
        if (assocTypeUri.equals("dm4.core.composition_def")) {
            if (one) {
                storeCompositionOne(childTopic, parent, assocDef, clientState, directives);
            } else {
                storeCompositionMany(childTopics, parent, assocDef, clientState, directives);
            }
        } else if (assocTypeUri.equals("dm4.core.aggregation_def")) {
            if (one) {
                storeAggregationOne(childTopic, parent, assocDef, clientState, directives);
            } else {
                storeAggregationMany(childTopics, parent, assocDef, clientState, directives);
            }
        } else {
            throw new RuntimeException("Association type \"" + assocTypeUri + "\" not supported");
        }
    }

    // --- Composition ---

    private void storeCompositionOne(TopicModel model, DeepaMehtaObjectModel parent,
                                     AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        // == create child ==
        // update DB
        Topic childTopic = dms.createTopic(model, clientState);
        associateChildTopic(parent, childTopic.getId(), assocDef, clientState);
        // Note: memory is already up-to-date. The child topic ID is updated in-place.
    }

    private void storeCompositionMany(List<TopicModel> models, DeepaMehtaObjectModel parent,
                                      AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        for (TopicModel model : models) {
            // == create child ==
            // update DB
            Topic childTopic = dms.createTopic(model, clientState);
            associateChildTopic(parent, childTopic.getId(), assocDef, clientState);
            // Note: memory is already up-to-date. The child topic ID is updated in-place.
        }
    }

    // --- Aggregation ---

    private void storeAggregationOne(TopicModel model, DeepaMehtaObjectModel parent,
                                     AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        if (model instanceof TopicReferenceModel) {
            // == create assignment ==
            // update DB
            Topic childTopic = associateReferencedChildTopic(parent, (TopicReferenceModel) model, assocDef,
                clientState);
            // update memory
            putInCompositeValue(parent, childTopic, assocDef);
        } else {
            // == create child ==
            // update DB
            Topic childTopic = dms.createTopic(model, clientState);
            associateChildTopic(parent, childTopic.getId(), assocDef, clientState);
            // Note: memory is already up-to-date. The child topic ID is updated in-place.
        }
    }

    private void storeAggregationMany(List<TopicModel> models, DeepaMehtaObjectModel parent,
                                      AssociationDefinition assocDef, ClientState clientState, Directives directives) {
        for (TopicModel model : models) {
            if (model instanceof TopicReferenceModel) {
                // == create assignment ==
                // update DB
                Topic childTopic = associateReferencedChildTopic(parent, (TopicReferenceModel) model, assocDef,
                    clientState);
                // update memory
                replaceReference(model, childTopic);
            } else {
                // == create child ==
                // update DB
                Topic childTopic = dms.createTopic(model, clientState);
                associateChildTopic(parent, childTopic.getId(), assocDef, clientState);
                // Note: memory is already up-to-date. The child topic ID is updated in-place.
            }
        }
    }

    // ---

    /**
     * For single-valued childs
     */
    private void putInCompositeValue(DeepaMehtaObjectModel parent, Topic childTopic, AssociationDefinition assocDef) {
        parent.getCompositeValueModel().put(assocDef.getChildTypeUri(), childTopic.getModel());
    }

    /**
     * Replaces a topic reference with the resolved topic.
     *
     * Used for multiple-valued childs.
     */
    private void replaceReference(TopicModel topicRef, Topic topic) {
        // Note: we must update the topic reference in-place.
        // Replacing the entire topic in the list of child topics would cause ConcurrentModificationException.
        topicRef.set(topic.getModel());
    }



    // === Label ===

    private String buildLabel(DeepaMehtaObjectModel model) {
        Type type = getType(model);
        if (type.getDataTypeUri().equals("dm4.core.composite")) {
            List<String> labelConfig = type.getLabelConfig();
            if (labelConfig.size() > 0) {
                return buildLabelFromConfig(model, labelConfig);
            } else {
                return buildDefaultLabel(model);
            }
        } else {
            return model.getSimpleValue().toString();
        }
    }

    /**
     * Builds the specified object model's label according to a label configuration.
     */
    private String buildLabelFromConfig(DeepaMehtaObjectModel model, List<String> labelConfig) {
        StringBuilder builder = new StringBuilder();
        for (String childTypeUri : labelConfig) {
            appendLabel(buildChildLabel(model, childTypeUri), builder, LABEL_CHILD_SEPARATOR);
        }
        return builder.toString();
    }

    private String buildDefaultLabel(DeepaMehtaObjectModel model) {
        Iterator<AssociationDefinition> i = getType(model).getAssocDefs().iterator();
        // Note: types just created might have no child types yet
        if (!i.hasNext()) {
            return "";
        }
        //
        String childTypeUri = i.next().getChildTypeUri();
        return buildChildLabel(model, childTypeUri);
    }

    // ---

    private String buildChildLabel(DeepaMehtaObjectModel parent, String childTypeUri) {
        Object value = parent.getCompositeValueModel().get(childTypeUri);
        // Note: topics just created have no child topics yet
        if (value == null) {
            return "";
        }
        //
        if (value instanceof TopicModel) {
            TopicModel childTopic = (TopicModel) value;
            return buildLabel(childTopic);                                          // recursion
        } else if (value instanceof List) {
            StringBuilder builder = new StringBuilder();
            for (TopicModel childTopic : (List<TopicModel>) value) {
                appendLabel(buildLabel(childTopic), builder, LABEL_TOPIC_SEPARATOR);  // recursion
            }
            return builder.toString();
        } else {
            throw new RuntimeException("Unexpected value in a CompositeValueModel: " + value);
        }
    }

    private void appendLabel(String label, StringBuilder builder, String separator) {
        // add separator
        if (builder.length() > 0 && label.length() > 0) {
            builder.append(separator);
        }
        //
        builder.append(label);
    }



    // === Helper ===

    /**
     * Fetches and returns a child topic or <code>null</code> if no such topic extists.
     */
    private RelatedTopicModel fetchChildTopic(long parentId, AssociationDefinition assocDef) {
        return dms.storageDecorator.fetchRelatedTopic(
            parentId,
            assocDef.getInstanceLevelAssocTypeUri(),
            "dm4.core.parent", "dm4.core.child",
            assocDef.getChildTypeUri()
        );
    }

    private ResultList<RelatedTopicModel> fetchChildTopics(long parentId, AssociationDefinition assocDef) {
        return dms.storageDecorator.fetchRelatedTopics(
            parentId,
            assocDef.getInstanceLevelAssocTypeUri(),
            "dm4.core.parent", "dm4.core.child",
            assocDef.getChildTypeUri()
        );
    }

    // ---

    private void associateChildTopic(DeepaMehtaObjectModel parent, TopicRoleModel child, AssociationDefinition assocDef,
                                                                                         ClientState clientState) {
        dms.createAssociation(assocDef.getInstanceLevelAssocTypeUri(),
            parent.createRoleModel("dm4.core.parent"), child, clientState
        );
    }

    // ---

    /**
     * Calculates the simple value that is to be indexed for this object.
     *
     * HTML tags are stripped from HTML values. Non-HTML values are returned directly.
     */
    private SimpleValue getIndexValue(DeepaMehtaObjectModel model) {
        SimpleValue value = model.getSimpleValue();
        if (getType(model).getDataTypeUri().equals("dm4.core.html")) {
            return new SimpleValue(JavaUtils.stripHTML(value.toString()));
        } else {
            return value;
        }
    }
}
