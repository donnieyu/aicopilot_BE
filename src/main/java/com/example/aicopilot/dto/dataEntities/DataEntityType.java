package com.example.aicopilot.dto.dataEntities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Data Entity Type
 *
 * @author Julia Oh
 * @version 26.0.0
 * @since 26.0.0
 */
public enum DataEntityType {
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    DATE("date"),
    DATETIME("datetime"),
    TIME("time"),
    UTC_DATE("utc_date"),
    UTC_DATETIME("utc_datetime"),
    UTC_TIME("utc_time"),
    FILE("file"),
    LOOKUP("lookup"),
    SIGN("sign"),
    TRISTATE("tristate"),
    STRING_ARRAY("string_array"),
    INTEGER_ARRAY("integer_array"),
    NUMBER_ARRAY("number_array"),
    DATE_ARRAY("date_array"),
    UTC_DATE_ARRAY("utc_date_array"),
    LOOKUP_ARRAY("lookup_array");

    private final String value;

    DataEntityType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Used by Jackson to deserialize incoming string values from JSON into DataEntityType enum constants.
     * Performs a case-insensitive match to find the corresponding enum.
     *
     * @param value The string value from JSON to convert.
     * @return The matching DataEntityType enum constant.
     */
    @JsonCreator
    public static DataEntityType fromValue(String value) {
        for (DataEntityType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown enum type " + value);
    }
}
