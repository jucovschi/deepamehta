package de.deepamehta.plugins.topicmaps.service;

import de.deepamehta.plugins.topicmaps.ClusterCoords;
import de.deepamehta.plugins.topicmaps.TopicmapRenderer;
import de.deepamehta.plugins.topicmaps.ViewmodelCustomizer;
import de.deepamehta.plugins.topicmaps.model.TopicmapViewmodel;

import de.deepamehta.core.Topic;
import de.deepamehta.core.model.ChildTopicsModel;
import de.deepamehta.core.service.PluginService;



public interface TopicmapsService extends PluginService {

    /**
     * @param   includeChilds   if true the topics contained in the topicmap will include their child topics.
     */
    TopicmapViewmodel getTopicmap(long topicmapId, boolean includeChilds);

    // ---

    Topic createTopicmap(String name,             String topicmapRendererUri);
    Topic createTopicmap(String name, String uri, String topicmapRendererUri);

    // ---

    void addTopicToTopicmap(long topicmapId, long topicId, ChildTopicsModel viewProps);

    /**
     * Convenience method to add a topic with the standard view properties.
     */
    void addTopicToTopicmap(long topicmapId, long topicId, int x, int y, boolean visibility);

    void addAssociationToTopicmap(long topicmapId, long assocId);

    // ---

    boolean isTopicInTopicmap(long topicmapId, long topicId);

    // ---

    void setViewProperties(long topicmapId, long topicId, ChildTopicsModel viewProps);

    /**
     * Convenience method to update the "dm4.topicmaps.x" and "dm4.topicmaps.y" standard view properties.
     */
    void setTopicPosition(long topicmapId, long topicId, int x, int y);

    /**
     * Convenience method to update the "dm4.topicmaps.visibility" standard view property.
     */
    void setTopicVisibility(long topicmapId, long topicId, boolean visibility);

    void removeAssociationFromTopicmap(long topicmapId, long assocId);

    // ---

    void setClusterPosition(long topicmapId, ClusterCoords coords);

    void setTopicmapTranslation(long topicmapId, int trans_x, int trans_y);

    // ---

    void registerTopicmapRenderer(TopicmapRenderer renderer);
    // ### TODO: unregister needed? Might a renderer hold a stale dms instance?

    void registerViewmodelCustomizer(ViewmodelCustomizer customizer);
    void unregisterViewmodelCustomizer(ViewmodelCustomizer customizer);
}
