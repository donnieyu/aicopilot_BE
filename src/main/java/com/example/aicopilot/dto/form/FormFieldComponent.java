package com.example.aicopilot.dto.form;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Form Field Component
 *
 * @author Julia Oh
 * @version 26.0.0
 * @since 26.0.0
 */
public enum FormFieldComponent {
    INPUT_TEXT("input_text"),
    INPUT_TEXTAREA("input_textarea"),
    INPUT_NUMBER("input_number"),
    CHECKBOX("checkbox"),
    CHIPS("chips"),
    DROPDOWN("dropdown"),
    MULTIPLE_DROPDOWN("multiple_dropdown"),
    DATE_PICKER("date_picker"),
    MULTIPLE_DATE_PICKER("multiple_date_picker"),
    DATE_TIME_PICKER("date_time_picker"),
    TIME_PICKER("time_picker"),
    FILE_UPLOAD("file_upload"),
    FILE_LIST("file_list"),
    SIGNATURE("signature"),
    TRI_STATE_CHECKBOX("tri_state_checkbox");

    private final String value;

    FormFieldComponent(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Used by Jackson to deserialize incoming string values from JSON into FormFieldComponent enum constants.
     * Performs a case-insensitive match to find the corresponding enum.
     *
     * @param value The string value from JSON to convert.
     * @return The matching FormFieldComponent enum constant.
     */
    @JsonCreator
    public static FormFieldComponent fromValue(String value) {
        for (FormFieldComponent component : values()) {
            if (component.value.equalsIgnoreCase(value)) {
                return component;
            }
        }
        throw new IllegalArgumentException("Unknown enum component " + value);
    }
}
