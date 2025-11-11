package de.seuhd.campuscoffee.data.impl;

import de.seuhd.campuscoffee.domain.exceptions.OsmNodeNotFoundException;
import de.seuhd.campuscoffee.domain.model.OsmNode;
import de.seuhd.campuscoffee.domain.ports.OsmDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OSM import service that fetches real data from OpenStreetMap API.
 */
@Service
@Slf4j
@RequiredArgsConstructor
class OsmDataServiceImpl implements OsmDataService {

    private final RestTemplate restTemplate;

    private static final String OSM_API_BASE_URL = "https://api.openstreetmap.org/api/0.6/node/";

    @Override
    public @NonNull OsmNode fetchNode(@NonNull Long nodeId) throws OsmNodeNotFoundException {
        log.info("Fetching OSM node {} from OpenStreetMap API", nodeId);

        try {
            String url = OSM_API_BASE_URL + nodeId;
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                System.out.println("DEBUG: Non-2xx response: " + response.getStatusCode());
                throw new OsmNodeNotFoundException(nodeId);
            }

            String xmlResponse = response.getBody();
            System.out.println("DEBUG: Response body length: " + (xmlResponse != null ? xmlResponse.length() : "null"));
            if (xmlResponse == null || xmlResponse.trim().isEmpty()) {
                System.out.println("DEBUG: Empty response body");
                throw new OsmNodeNotFoundException(nodeId);
            }

            System.out.println("DEBUG: Raw XML response for node " + nodeId + ":\n" + xmlResponse.substring(0, Math.min(500, xmlResponse.length())));

            // Parse XML response to extract tags
            Map<String, String> tags = parseOsmXmlResponse(xmlResponse);

            return OsmNode.builder()
                    .nodeId(nodeId)
                    .tags(tags)
                    .build();

        } catch (RestClientException e) {
            log.error("Failed to fetch OSM node {}: {}", nodeId, e.getMessage());
            throw new OsmNodeNotFoundException(nodeId);
        } catch (Exception e) {
            log.error("Error processing OSM node {}: {}", nodeId, e.getMessage());
            throw new OsmNodeNotFoundException(nodeId);
        }
    }

    /**
     * Parses the OSM XML response to extract tags as key-value pairs.
     * This is a simple XML parser that extracts tag elements from the OSM node.
     */
    private @NonNull Map<String, String> parseOsmXmlResponse(@NonNull String xmlResponse) {
        Map<String, String> tags = new HashMap<>();

        try {
            // Simple parsing for <tag k="key" v="value"/> elements
            int tagStart = 0;
            while ((tagStart = xmlResponse.indexOf("<tag k=\"", tagStart)) != -1) {
                int kStart = tagStart + 9; // <tag k="
                int kEnd = xmlResponse.indexOf("\"", kStart);
                if (kEnd == -1) break;

                String key = xmlResponse.substring(kStart, kEnd);

                int vStart = xmlResponse.indexOf("v=\"", kEnd) + 3;
                if (vStart == -1) break;

                int vEnd = xmlResponse.indexOf("\"", vStart);
                if (vEnd == -1) break;

                String value = xmlResponse.substring(vStart, vEnd);
                tags.put(key, value);

                tagStart = vEnd + 1;
            }

        } catch (Exception e) {
            // Ignore parsing errors
        }

        return tags;
    }
}
