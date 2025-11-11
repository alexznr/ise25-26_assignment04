package de.seuhd.campuscoffee.domain.impl;

import de.seuhd.campuscoffee.domain.exceptions.DuplicatePosNameException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeMissingFieldsException;
import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.model.Pos;
import de.seuhd.campuscoffee.domain.exceptions.PosNotFoundException;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import de.seuhd.campuscoffee.domain.ports.PosDataService;
import de.seuhd.campuscoffee.domain.ports.PosService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Implementation of the POS service that handles business logic related to POS entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PosServiceImpl implements PosService {
    private final PosDataService posDataService;
    private final OsmDataService osmDataService;

    @Override
    public void clear() {
        log.warn("Clearing all POS data");
        posDataService.clear();
    }

    @Override
    public @NonNull List<Pos> getAll() {
        log.debug("Retrieving all POS");
        return posDataService.getAll();
    }

    @Override
    public @NonNull Pos getById(@NonNull Long id) throws PosNotFoundException {
        log.debug("Retrieving POS with ID: {}", id);
        return posDataService.getById(id);
    }

    @Override
    public @NonNull Pos upsert(@NonNull Pos pos) throws PosNotFoundException {
        if (pos.id() == null) {
            // Create new POS
            log.info("Creating new POS: {}", pos.name());
            return performUpsert(pos);
        } else {
            // Update existing POS
            log.info("Updating POS with ID: {}", pos.id());
            // POS ID must be set
            Objects.requireNonNull(pos.id());
            // POS must exist in the database before the update
            posDataService.getById(pos.id());
            return performUpsert(pos);
        }
    }

    @Override
    public @NonNull Pos importFromOsmNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Importing POS from OpenStreetMap node {}...", nodeId);

        // Fetch the OSM node data using the port
        OsmNode osmNode = osmDataService.fetchNode(nodeId);

        // Convert OSM node to POS domain object and upsert it
        Pos savedPos = upsert(convertOsmNodeToPos(osmNode));
        log.info("Successfully imported POS '{}' from OSM node {}", savedPos.name(), nodeId);

        return savedPos;
    }

    /**
     * Converts an OSM node to a POS domain object.
     * Maps OSM tags to POS fields with intelligent defaults and validation.
     */
    private @NonNull Pos convertOsmNodeToPos(@NonNull OsmNode osmNode) {
        // Debug: print all tags
        log.info("DEBUG: Converting OSM node {} with tags: {}", osmNode.nodeId(), osmNode.tags());
        log.info("DEBUG: Name tag: {}", osmNode.getTag("name"));

        // Extract basic information from OSM tags
        String name = osmNode.getTag("name");
        String amenity = osmNode.getTag("amenity");
        String cuisine = osmNode.getTag("cuisine");
        String description = osmNode.getTag("description", osmNode.getTag("note", ""));

        // Address components
        String street = osmNode.getTag("addr:street");
        String houseNumber = osmNode.getTag("addr:housenumber");
        String city = osmNode.getTag("addr:city", "Heidelberg"); // Default to Heidelberg
        Integer postalCode = parsePostalCode(osmNode.getTag("addr:postcode"));

        // Validate required fields
        if (name == null || name.trim().isEmpty()) {
            throw new OsmNodeMissingFieldsException(osmNode.nodeId(), "name");
        }

        if (street == null || street.trim().isEmpty()) {
            throw new OsmNodeMissingFieldsException(osmNode.nodeId(), "addr:street");
        }

        if (houseNumber == null || houseNumber.trim().isEmpty()) {
            throw new OsmNodeMissingFieldsException(osmNode.nodeId(), "addr:housenumber");
        }

        if (postalCode == null) {
            throw new OsmNodeMissingFieldsException(osmNode.nodeId(), "addr:postcode");
        }

        // Determine POS type from amenity
        PosType posType = determinePosType(amenity, cuisine);

        // Determine campus from postal code or address
        CampusType campus = determineCampus(postalCode, street, city);

        // Build description if not provided
        if (description.trim().isEmpty()) {
            description = buildDefaultDescription(posType, cuisine);
        }

        return Pos.builder()
                .name(name.trim())
                .description(description.trim())
                .type(posType)
                .campus(campus)
                .street(street.trim())
                .houseNumber(houseNumber.trim())
                .postalCode(postalCode)
                .city(city.trim())
                .build();
    }

    /**
     * Determines the POS type from OSM amenity and cuisine tags.
     */
    private @NonNull PosType determinePosType(String amenity, String cuisine) {
        if (amenity == null) {
            return PosType.CAFE; // Default to cafe
        }

        return switch (amenity.toLowerCase()) {
            case "cafe", "coffee_shop" -> PosType.CAFE;
            case "restaurant", "fast_food", "bar", "pub" -> PosType.CAFETERIA; // Map to cafeteria as closest match
            case "bakery" -> PosType.BAKERY;
            default -> PosType.CAFE; // Default fallback
        };
    }

    /**
     * Determines the campus location based on postal code and address.
     * For Heidelberg University, this is a simplified mapping.
     */
    private @NonNull CampusType determineCampus(Integer postalCode, String street, String city) {
        if (!"Heidelberg".equalsIgnoreCase(city)) {
            return CampusType.ALTSTADT; // Default for non-Heidelberg locations
        }

        // Heidelberg postal code mappings (simplified)
        if (postalCode >= 69115 && postalCode <= 69121) {
            // Check for specific campus indicators in street names
            if (street != null) {
                String streetLower = street.toLowerCase();
                if (streetLower.contains("bergheim") || streetLower.contains("handschuhsheim")) {
                    return CampusType.BERGHEIM;
                }
                if (streetLower.contains("im neuenheimer feld") || streetLower.contains("neuenh")) {
                    return CampusType.INF; // Map INF campus for technical areas
                }
            }
            return CampusType.ALTSTADT; // Default for central Heidelberg
        }

        return CampusType.ALTSTADT; // Default fallback
    }

    /**
     * Parses postal code from string to integer.
     */
    private Integer parsePostalCode(String postcodeStr) {
        if (postcodeStr == null || postcodeStr.trim().isEmpty()) {
            return null;
        }

        try {
            // Extract numeric part (German postal codes are 5 digits)
            String numericPart = postcodeStr.replaceAll("\\D", "");
            if (numericPart.length() == 5) {
                return Integer.parseInt(numericPart);
            }
        } catch (NumberFormatException e) {
            log.warn("Failed to parse postal code: {}", postcodeStr);
        }

        return null;
    }

    /**
     * Builds a default description based on POS type and cuisine.
     */
    private @NonNull String buildDefaultDescription(@NonNull PosType posType, String cuisine) {
        String baseDescription = switch (posType) {
            case CAFE -> "A cozy cafe";
            case CAFETERIA -> "A cafeteria";
            case BAKERY -> "A bakery";
            case VENDING_MACHINE -> "A vending machine";
        };

        if (cuisine != null && !cuisine.trim().isEmpty()) {
            baseDescription += " serving " + cuisine.trim();
        }

        return baseDescription;
    }

    /**
     * Performs the actual upsert operation with consistent error handling and logging.
     * Database constraint enforces name uniqueness - data layer will throw DuplicatePosNameException if violated.
     * JPA lifecycle callbacks (@PrePersist/@PreUpdate) set timestamps automatically.
     *
     * @param pos the POS to upsert
     * @return the persisted POS with updated ID and timestamps
     * @throws DuplicatePosNameException if a POS with the same name already exists
     */
    private @NonNull Pos performUpsert(@NonNull Pos pos) throws DuplicatePosNameException {
        try {
            Pos upsertedPos = posDataService.upsert(pos);
            log.info("Successfully upserted POS with ID: {}", upsertedPos.id());
            return upsertedPos;
        } catch (DuplicatePosNameException e) {
            log.error("Error upserting POS '{}': {}", pos.name(), e.getMessage());
            throw e;
        }
    }
}
