package de.deepamehta.core.impl;

import de.deepamehta.core.model.Topic;
import de.deepamehta.core.model.TopicType;
import de.deepamehta.core.model.TopicValue;
import de.deepamehta.core.storage.Storage;
import de.deepamehta.core.storage.Transaction;

import de.deepamehta.hypergraph.HyperEdge;
import de.deepamehta.hypergraph.HyperGraph;
import de.deepamehta.hypergraph.HyperNode;
import de.deepamehta.hypergraph.IndexMode;

import java.util.logging.Logger;



/**
 * Implementation of the DeepaMehta storage interface.
 */
class DeepaMehtaStorage implements Storage {

    // ---------------------------------------------------------------------------------------------- Instance Variables

    private HyperGraph hg;

    private final Logger logger = Logger.getLogger(getClass().getName());

    // ---------------------------------------------------------------------------------------------------- Constructors

    DeepaMehtaStorage(HyperGraph hg) {
        this.hg = hg;
    }

    // -------------------------------------------------------------------------------------------------- Public Methods

    // --- Topics ---

    @Override
    public Topic getTopic(String key, TopicValue value) {
        HyperNode node = hg.getHyperNode(key, value.value());
        return node != null ? buildTopic(node) : null;
    }

    @Override
    public Topic createTopic(Topic topic) {
        // create node
        HyperNode node = hg.createHyperNode();
        node.setAttribute("uri", topic.getUri(), IndexMode.KEY);
        node.setAttribute("value", topic.getValue());
        // associate with type
        HyperNode topicType = lookupTopicType(topic.getTypeUri());
        HyperEdge edge = hg.createHyperEdge("dm3.core.instantiation");
        edge.addHyperNode(topicType, "dm3.core.type");
        edge.addHyperNode(node, "dm3.core.instance");
        //
        return buildTopic(node);
    }

    // --- Types ---

    @Override
    public TopicType createTopicType(TopicType topicType) {
        return null;
    }

    // --- DB ---

    @Override
    public Transaction beginTx() {
        return new DeepaMehtaTransaction(hg);
    }

    @Override
    public boolean init() {
        // init migration number
        boolean isCleanInstall = false;
        if (!hg.getHyperNode(0).hasAttribute("core_migration_nr")) {
            logger.info("Starting with a fresh DB -- Setting migration number to 0");
            setMigrationNr(0);
            isCleanInstall = true;
        }
        return isCleanInstall;
    }

    @Override
    public void shutdown() {
        hg.shutdown();
    }

    @Override
    public int getMigrationNr() {
        return hg.getHyperNode(0).getInteger("core_migration_nr");
    }

    @Override
    public void setMigrationNr(int migrationNr) {
        hg.getHyperNode(0).setAttribute("core_migration_nr", migrationNr);
    }

    // ------------------------------------------------------------------------------------------------- Private Methods

    private HyperNode getTopicType(HyperNode node) {
        return node.traverse("dm3.core.instance", "dm3.core.instantiation", "dm3.core.type");
    }

    private HyperNode lookupTopicType(String typeUri) {
        HyperNode topicType = hg.getHyperNode("uri", typeUri);
        if (topicType == null) {
            throw new RuntimeException("Topic type \"" + typeUri + "\" is unknown");
        }
        return topicType;
    }

    private Topic buildTopic(HyperNode node) {
        if (node == null) {
            throw new NullPointerException("Tried to build a Topic from a null HyperNode");
        }
        return new Topic(node.getId(), node.getString("uri"), new TopicValue(node.get("value")),
            getTopicType(node).getString("uri"), null);     // composite=null
    }
}