package com.example.aicopilot.dto.dataEntities;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Model for DataEntitiesResponse
 *
 * @author Donnie Yu
 * @version 26.0.0
 * @since 26.0.0
 */
public record DataEntitiesResponse(
		@JsonProperty("entities") @JsonPropertyDescription("""
				A comprehensive list of all unique data entities that will be captured throughout the process.
				Each entity represents a single, distinct piece of data.
				An entity should not represent a collection or group of other data points; it should be a fundamental data unit.
				These entities are then organized into logical groups using the 'groups' field below.
				""") List<DataEntity> entities,
		@JsonProperty("groups") @JsonPropertyDescription("""
				An array of data entity groups.
				Each group clusters related data entities that belong together in the same logical context.
				The 'entityIds' within each group must refer to the ids of entities defined in the 'entities' list.
				""") List<DataEntitiesGroup> groups) {
}
