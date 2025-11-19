package com.example.aicopilot.dto.form;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record FormDefinitions(
        @JsonProperty("formName")
        @JsonPropertyDescription("A concise and descriptive name for the form.")
        String formName,
        @JsonProperty("formDescription")
        @JsonPropertyDescription("Brief explanation of the form.")
        String formDescription,
        @JsonProperty("fieldGroups")
        @JsonPropertyDescription("""
                An array of field groups that organize related form fields for a user-friendly form layout.
                While all fields MUST map to an entity, the grouping of those fields can be optimized for the user experience.
                Use the data entity groups as a primary guide, but you have the flexibility to create different groups on the form if it makes the form clearer and more intuitive.
                For example, you can split a large data group into smaller form groups (like creating a 'Requestor Info' group) or combine related fields from different data groups into a single form group.
                """)
        List<FormFieldGroup> fieldGroups
) {
}