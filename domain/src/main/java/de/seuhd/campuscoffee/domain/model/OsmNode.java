package de.seuhd.campuscoffee.domain.model;

import lombok.Builder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * Represents an OpenStreetMap node with relevant Point of Sale information.
 * This is the domain model for OSM data before it is converted to a POS object.
 *
 * @param nodeId The OpenStreetMap node ID.
 * @param tags Map of OSM tags containing metadata like name, address, amenity type, etc.
 */
@Builder
public record OsmNode(
        @NonNull Long nodeId,
        @NonNull Map<String, String> tags
) {
    /**
     * Gets a tag value by key, returning null if the tag doesn't exist.
     */
    public @Nullable String getTag(@NonNull String key) {
        return tags.get(key);
    }

    /**
     * Gets a tag value by key with a default value if the tag doesn't exist.
     */
    public @NonNull String getTag(@NonNull String key, @NonNull String defaultValue) {
        return tags.getOrDefault(key, defaultValue);
    }
}
