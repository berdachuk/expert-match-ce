package com.berdachuk.expertmatch.graph.service.impl;

import com.berdachuk.expertmatch.graph.service.GraphService;
import com.berdachuk.expertmatch.graph.service.GraphVisualizationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Implementation of GraphVisualizationService using GraphService Cypher execution.
 * Formats Apache AGE graph data for Cytoscape.js visualization.
 */
@Slf4j
@Service
public class GraphVisualizationServiceImpl implements GraphVisualizationService {

    private static final List<String> VERTEX_LABELS = List.of("Expert", "Project", "Technology", "Domain", "Customer");
    /**
     * Edge types must match labels used in GraphBuilderServiceImpl (PARTICIPATED_IN, WORKED_FOR, USES, IN_DOMAIN, FOR_CUSTOMER).
     */
    private static final List<String> EDGE_TYPES = List.of("PARTICIPATED_IN", "WORKED_FOR", "USES", "IN_DOMAIN", "FOR_CUSTOMER");

    private final GraphService graphService;

    public GraphVisualizationServiceImpl(GraphService graphService) {
        this.graphService = graphService;
    }

    private static long parseCount(Object val) {
        if (val == null) {
            return 0;
        }
        if (val instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(val.toString().replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getGraphStatistics() {
        Map<String, Object> stats = new HashMap<>();

        if (!graphService.graphExists()) {
            stats.put("exists", false);
            stats.put("totalVertices", 0);
            stats.put("totalEdges", 0);
            stats.put("vertexCounts", Map.of());
            stats.put("edgeCounts", Map.of());
            return stats;
        }

        stats.put("exists", true);

        try {
            Map<String, Long> vertexCounts = new TreeMap<>();
            long totalVertices = 0;
            for (String label : VERTEX_LABELS) {
                long count = countVerticesByLabel(label);
                if (count > 0) {
                    vertexCounts.put(label, count);
                    totalVertices += count;
                }
            }

            Map<String, Long> edgeCounts = new TreeMap<>();
            long totalEdges = 0;
            for (String edgeType : EDGE_TYPES) {
                long count = countEdgesByType(edgeType);
                if (count > 0) {
                    edgeCounts.put(edgeType, count);
                    totalEdges += count;
                }
            }
            long totalEdgesFromDb = countAllEdges();
            if (totalEdgesFromDb > totalEdges) {
                totalEdges = totalEdgesFromDb;
            }

            stats.put("totalVertices", totalVertices);
            stats.put("totalEdges", totalEdges);
            stats.put("vertexCounts", vertexCounts);
            stats.put("edgeCounts", edgeCounts);

        } catch (Exception e) {
            log.error("Error getting graph statistics", e);
            stats.put("error", "Failed to retrieve statistics: " + e.getMessage());
        }

        return stats;
    }

    private long countVerticesByLabel(String label) {
        try {
            String cypher = "MATCH (n:" + label + ") RETURN count(n)";
            List<Map<String, Object>> rows = graphService.executeCypher(cypher, Map.of());
            if (rows.isEmpty()) {
                return 0;
            }
            Object val = rows.get(0).get("result");
            return parseCount(val);
        } catch (Exception e) {
            log.debug("Count vertices for {} failed: {}", label, e.getMessage());
            return 0;
        }
    }

    private long countEdgesByType(String edgeType) {
        try {
            String cypher = "MATCH ()-[r:" + edgeType + "]->() RETURN count(r)";
            List<Map<String, Object>> rows = graphService.executeCypher(cypher, Map.of());
            if (rows.isEmpty()) {
                return 0;
            }
            Object val = rows.get(0).get("result");
            return parseCount(val);
        } catch (Exception e) {
            log.debug("Count edges for {} failed: {}", edgeType, e.getMessage());
            return 0;
        }
    }

    private long countAllEdges() {
        try {
            String cypher = "MATCH ()-[r]->() RETURN count(r)";
            List<Map<String, Object>> rows = graphService.executeCypher(cypher, Map.of());
            if (rows.isEmpty()) {
                return 0;
            }
            Object val = rows.get(0).get("result");
            return parseCount(val);
        } catch (Exception e) {
            log.debug("Count all edges failed: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> getGraphData(int limit, int offset, String vertexType, int clusterLevel) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();

        if (!graphService.graphExists()) {
            result.put("nodes", nodes);
            result.put("edges", edges);
            result.put("total", 0);
            return result;
        }

        try {
            int fetchLimit = offset > 0 ? limit + offset : limit;
            String effectiveVertexType = (vertexType != null && !vertexType.isBlank() && VERTEX_LABELS.contains(vertexType))
                    ? vertexType : null;
            String vertexCypher = effectiveVertexType != null
                    ? "MATCH (v:" + effectiveVertexType + ") RETURN v LIMIT " + fetchLimit
                    : "MATCH (v) RETURN v LIMIT " + fetchLimit;

            List<Map<String, Object>> vertexResults = graphService.executeCypher(vertexCypher, Map.of());

            if (offset > 0 && vertexResults.size() > offset) {
                vertexResults = vertexResults.subList(offset, vertexResults.size());
            } else if (offset > 0 && vertexResults.size() <= offset) {
                vertexResults = List.of();
            }

            Set<String> nodeIds = new HashSet<>();
            int nodeIndex = 0;

            for (Map<String, Object> row : vertexResults) {
                Object vertexObj = row.get("result");
                if (vertexObj == null) {
                    vertexObj = row.get("c0");
                }
                if (vertexObj != null) {
                    Map<String, Object> nodeData = extractVertexData(vertexObj, nodeIndex++);
                    if (nodeData != null) {
                        String nodeId = (String) nodeData.get("id");
                        if (nodeId != null) {
                            nodeIds.add(nodeId);
                            nodes.add(Map.of("data", nodeData));
                        }
                    }
                }
            }

            if (!nodeIds.isEmpty() && limit > 0) {
                int edgeLimit = Math.min(limit * 10, 10000);
                List<Map<String, Object>> edgeResults = new ArrayList<>();
                if (effectiveVertexType != null) {
                    String outCypher = "MATCH (a:" + effectiveVertexType + ")-[r]->(b) RETURN a, r, b LIMIT " + edgeLimit;
                    String inCypher = "MATCH (a)-[r]->(b:" + effectiveVertexType + ") RETURN a, r, b LIMIT " + edgeLimit;
                    edgeResults.addAll(graphService.executeCypher(outCypher, Map.of()));
                    edgeResults.addAll(graphService.executeCypher(inCypher, Map.of()));
                } else {
                    String edgeCypher = "MATCH (a)-[r]->(b) RETURN a, r, b LIMIT " + edgeLimit;
                    edgeResults.addAll(graphService.executeCypher(edgeCypher, Map.of()));
                }

                int edgeIndex = 0;
                for (Map<String, Object> row : edgeResults) {
                    Object sourceObj = row.get("c0");
                    Object edgeObj = row.get("c1");
                    Object targetObj = row.get("c2");

                    if (sourceObj == null || edgeObj == null || targetObj == null) {
                        edgeIndex++;
                        continue;
                    }

                    String sourceId = extractIdFromVertex(sourceObj.toString());
                    String targetId = extractIdFromVertex(targetObj.toString());
                    String type = extractEdgeType(edgeObj.toString());

                    if (sourceId == null || targetId == null) {
                        edgeIndex++;
                        continue;
                    }

                    boolean sourceInSet = nodeIds.contains(sourceId);
                    boolean targetInSet = nodeIds.contains(targetId);

                    if (!sourceInSet && sourceObj != null) {
                        Map<String, Object> sourceNodeData = extractVertexData(sourceObj, nodes.size());
                        if (sourceNodeData != null && sourceNodeData.get("id") != null) {
                            nodeIds.add(sourceId);
                            nodes.add(Map.of("data", sourceNodeData));
                            sourceInSet = true;
                        }
                    }

                    if (!targetInSet && targetObj != null) {
                        Map<String, Object> targetNodeData = extractVertexData(targetObj, nodes.size());
                        if (targetNodeData != null && targetNodeData.get("id") != null) {
                            nodeIds.add(targetId);
                            nodes.add(Map.of("data", targetNodeData));
                            targetInSet = true;
                        }
                    }

                    if (sourceInSet || targetInSet) {
                        Map<String, Object> edgeData = new HashMap<>();
                        edgeData.put("id", "e" + edgeIndex);
                        edgeData.put("source", sourceId);
                        edgeData.put("target", targetId);
                        if (type != null) {
                            edgeData.put("type", type);
                        }
                        edges.add(Map.of("data", edgeData));
                    }
                    edgeIndex++;
                }
            }

            result.put("nodes", nodes);
            result.put("edges", edges);
            result.put("total", nodes.size());

        } catch (Exception e) {
            log.error("Error getting graph data", e);
            result.put("error", "Failed to retrieve graph data: " + e.getMessage());
            result.put("nodes", nodes);
            result.put("edges", edges);
        }

        return result;
    }

    private Map<String, Object> extractVertexData(Object vertexObj, int index) {
        try {
            String vertexStr = vertexObj.toString();
            String id = extractIdFromVertex(vertexStr);
            String label = extractLabelFromVertex(vertexObj);
            String name = extractPropertyValue(vertexStr, "name");

            if (id == null) {
                id = "node_" + index;
            }

            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("id", id);
            if (label != null) {
                nodeData.put("label", label);
                nodeData.put("type", label);
            }
            if (name != null) {
                nodeData.put("name", name);
            }
            String email = extractPropertyValue(vertexStr, "email");
            if (email != null) {
                nodeData.put("email", email);
            }
            String seniority = extractPropertyValue(vertexStr, "seniority");
            if (seniority != null) {
                nodeData.put("seniority", seniority);
            }
            String projectType = extractPropertyValue(vertexStr, "projectType");
            if (projectType != null) {
                nodeData.put("projectType", projectType);
            }
            return nodeData;
        } catch (Exception e) {
            log.warn("Error extracting vertex data: {}", e.getMessage());
            return null;
        }
    }

    private String extractIdFromVertex(String vertexStr) {
        String id = extractPropertyValue(vertexStr, "id");
        if (id != null) {
            return id;
        }
        int topLevelIdStart = vertexStr.indexOf("\"id\":");
        if (topLevelIdStart >= 0) {
            int valueStart = vertexStr.indexOf(":", topLevelIdStart) + 1;
            while (valueStart < vertexStr.length() && Character.isWhitespace(vertexStr.charAt(valueStart))) {
                valueStart++;
            }
            int valueEnd = valueStart;
            while (valueEnd < vertexStr.length() && Character.isDigit(vertexStr.charAt(valueEnd))) {
                valueEnd++;
            }
            if (valueEnd > valueStart) {
                return vertexStr.substring(valueStart, valueEnd);
            }
        }
        return null;
    }

    private String extractLabelFromVertex(Object vertexObj) {
        try {
            String vertexStr = vertexObj.toString();
            int labelStart = vertexStr.indexOf("\"label\"");
            if (labelStart >= 0) {
                int valueStart = vertexStr.indexOf("\"", labelStart + 7) + 1;
                int valueEnd = vertexStr.indexOf("\"", valueStart);
                if (valueEnd > valueStart) {
                    return vertexStr.substring(valueStart, valueEnd);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractPropertyValue(String vertexStr, String propertyName) {
        try {
            int propsStart = vertexStr.indexOf("\"properties\"");
            if (propsStart < 0) {
                return null;
            }
            int propsBraceStart = vertexStr.indexOf("{", propsStart);
            if (propsBraceStart < 0) {
                return null;
            }
            int propsBraceEnd = vertexStr.indexOf("}", propsBraceStart);
            if (propsBraceEnd < 0) {
                return null;
            }
            String keyPattern = "\"" + propertyName + "\":";
            int keyStart = vertexStr.indexOf(keyPattern, propsBraceStart);
            if (keyStart < 0 || keyStart >= propsBraceEnd) {
                return null;
            }
            int colonPos = keyStart + keyPattern.length();
            int valueStart = colonPos;
            while (valueStart < propsBraceEnd && Character.isWhitespace(vertexStr.charAt(valueStart))) {
                valueStart++;
            }
            if (valueStart < propsBraceEnd && vertexStr.charAt(valueStart) == '"') {
                valueStart++;
                int valueEnd = vertexStr.indexOf("\"", valueStart);
                if (valueEnd > valueStart && valueEnd <= propsBraceEnd) {
                    return vertexStr.substring(valueStart, valueEnd);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String extractEdgeType(Object edgeObj) {
        try {
            String edgeStr = edgeObj.toString();
            int labelStart = edgeStr.indexOf("\"label\"");
            if (labelStart >= 0) {
                int valueStart = edgeStr.indexOf("\"", labelStart + 7) + 1;
                int valueEnd = edgeStr.indexOf("\"", valueStart);
                if (valueEnd > valueStart) {
                    return edgeStr.substring(valueStart, valueEnd);
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
