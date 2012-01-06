function AssociationRenderer() {

    var assoc_type_menu   // a GUIToolkit Menu object
    var role_type_menu_1, role_type_menu_2

    var topic_1, topic_2
    var role_type_1, role_type_2

    // -------------------------------------------------------------------------------------------------- Public Methods



    // ************************************
    // *** Page Renderer Implementation ***
    // ************************************



    this.render_page = function(assoc) {
        var assoc_type = dm4c.type_cache.get_association_type(assoc.type_uri)
        dm4c.render.field_label("Association Type")
        dm4c.render.field_value(assoc_type.value)
        //
        topic_1 = assoc.get_topic_1()
        topic_2 = assoc.get_topic_2()
        role_type_1 = assoc.get_role_type_1()
        role_type_2 = assoc.get_role_type_2()
        //
        render_assoc_role(topic_1, role_type_1)
        render_assoc_role(topic_2, role_type_2)
    }

    this.render_form = function(assoc) {
        assoc_type_menu = dm4c.render.topic_menu("dm4.core.assoc_type", assoc.type_uri)
        dm4c.render.field_label("Association Type")
        dm4c.render.field_value(assoc_type_menu.dom)
        //
        role_type_menu_1 = dm4c.render.topic_menu("dm4.core.role_type", assoc.role_1.role_type_uri)
        role_type_menu_2 = dm4c.render.topic_menu("dm4.core.role_type", assoc.role_2.role_type_uri)
        //
        render_assoc_role_editor(topic_1, role_type_menu_1)
        render_assoc_role_editor(topic_2, role_type_menu_2)
    }

    this.process_form = function(assoc) {
        var assoc_model = build_association_model()
        assoc = dm4c.do_update_association(assoc, assoc_model)
        dm4c.trigger_plugin_hook("post_submit_form", assoc)

        /**
         * Reads out values from GUI elements and builds an association model object from it.
         *
         * @return  an association model object
         */
        function build_association_model() {
            return {
                id: assoc.id,
                type_uri: assoc_type_menu.get_selection().value,
                role_1: {
                    topic_id: assoc.role_1.topic_id,
                    role_type_uri: role_type_menu_1.get_selection().value
                },
                role_2: {
                    topic_id: assoc.role_2.topic_id,
                    role_type_uri: role_type_menu_2.get_selection().value
                }
            }
        }
    }

    // ----------------------------------------------------------------------------------------------- Private Functions

    function render_assoc_role(topic, role_type) {
        _render_assoc_role(topic, role_type.value)
    }

    function render_assoc_role_editor(topic, role_type_menu) {
        _render_assoc_role(topic, role_type_menu.dom)
    }

    function _render_assoc_role(topic, role_type_element) {
        var topic_div = $("<div>").text(topic.get_type().value + ": \"" + topic.value + "\"")
        var role_type_div = $("<div>")
            .append($("<div>").addClass("field-label").text("Role Type"))
            .append($("<div>").append(role_type_element))
        dm4c.render.page($("<div>").addClass("assoc-role").addClass("ui-state-default")
            .append(topic_div).append(role_type_div))
    }
}