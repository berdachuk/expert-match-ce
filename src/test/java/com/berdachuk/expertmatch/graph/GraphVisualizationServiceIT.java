package com.berdachuk.expertmatch.graph;

import com.berdachuk.expertmatch.graph.service.GraphService;
import com.berdachuk.expertmatch.graph.service.GraphVisualizationService;
import com.berdachuk.expertmatch.integration.BaseIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for GraphVisualizationService.
 * Uses Testcontainers PostgreSQL with Apache AGE.
 */
class GraphVisualizationServiceIT extends BaseIntegrationTest {

    @Autowired
    private GraphVisualizationService graphVisualizationService;

    @Autowired
    private GraphService graphService;

    @Autowired
    private NamedParameterJdbcTemplate namedJdbcTemplate;

    @BeforeEach
    void setUp() {
        ensureGraphExists();
        clearGraph();
    }

    private void ensureGraphExists() {
        if (!graphService.graphExists()) {
            graphService.createGraph();
        }
    }

    private void clearGraph() {
        try {
            namedJdbcTemplate.getJdbcTemplate().execute("SET search_path = ag_catalog, \"$user\", public, expertmatch;");
            graphService.executeCypher("MATCH (n) DETACH DELETE n", new HashMap<>());
        } catch (Exception e) {
            // Graph might be empty or query might fail
        } finally {
            try {
                namedJdbcTemplate.getJdbcTemplate().execute("SET search_path = expertmatch, public;");
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    void getGraphStatisticsWhenGraphEmptyReturnsEmptyCounts() {
        Map<String, Object> stats = graphVisualizationService.getGraphStatistics();

        assertNotNull(stats);
        assertTrue(stats.containsKey("exists"));
        assertTrue(stats.containsKey("totalVertices"));
        assertTrue(stats.containsKey("totalEdges"));
        assertTrue(stats.containsKey("vertexCounts"));
        assertTrue(stats.containsKey("edgeCounts"));
        assertTrue((Boolean) stats.get("exists"));
        assertEquals(0L, stats.get("totalVertices"));
        assertEquals(0L, stats.get("totalEdges"));
        assertTrue(((Map<?, ?>) stats.get("vertexCounts")).isEmpty());
        assertTrue(((Map<?, ?>) stats.get("edgeCounts")).isEmpty());
    }

    @Test
    void getGraphStatisticsWhenGraphHasDataReturnsCounts() {
        graphService.executeCypher(
                "CREATE (e:Expert {id: $eid, name: $ename}) RETURN e",
                Map.of("eid", "viz-expert-1", "ename", "Viz Expert 1"));
        graphService.executeCypher(
                "CREATE (p:Project {id: $pid, name: $pname}) RETURN p",
                Map.of("pid", "viz-project-1", "pname", "Viz Project 1"));
        graphService.executeCypher(
                "MATCH (e:Expert {id: $eid}), (p:Project {id: $pid}) CREATE (e)-[:PARTICIPATED_IN]->(p) RETURN e",
                Map.of("eid", "viz-expert-1", "pid", "viz-project-1"));

        Map<String, Object> stats = graphVisualizationService.getGraphStatistics();

        assertNotNull(stats);
        assertTrue((Boolean) stats.get("exists"));
        assertTrue((Long) stats.get("totalVertices") >= 2);
        assertTrue((Long) stats.get("totalEdges") >= 1);
        Map<String, Long> vertexCounts = (Map<String, Long>) stats.get("vertexCounts");
        Map<String, Long> edgeCounts = (Map<String, Long>) stats.get("edgeCounts");
        assertTrue(vertexCounts.containsKey("Expert"));
        assertTrue(vertexCounts.containsKey("Project"));
        assertTrue(edgeCounts.containsKey("PARTICIPATED_IN"));
    }

    @Test
    void getGraphDataWhenGraphEmptyReturnsEmptyNodesAndEdges() {
        Map<String, Object> data = graphVisualizationService.getGraphData(100, 0, null, 0);

        assertNotNull(data);
        assertTrue(data.containsKey("nodes"));
        assertTrue(data.containsKey("edges"));
        assertTrue(data.containsKey("total"));
        List<?> nodes = (List<?>) data.get("nodes");
        List<?> edges = (List<?>) data.get("edges");
        assertNotNull(nodes);
        assertNotNull(edges);
        assertTrue(nodes.isEmpty());
        assertTrue(edges.isEmpty());
        assertEquals(0, data.get("total"));
    }

    @Test
    void getGraphDataWhenGraphHasVerticesReturnsCytoscapeFormat() {
        graphService.executeCypher(
                "CREATE (e:Expert {id: $eid, name: $ename}) RETURN e",
                Map.of("eid", "viz-e2", "ename", "Expert Two"));
        graphService.executeCypher(
                "CREATE (p:Project {id: $pid, name: $pname}) RETURN p",
                Map.of("pid", "viz-p2", "pname", "Project Two"));
        graphService.executeCypher(
                "MATCH (e:Expert {id: $eid}), (p:Project {id: $pid}) CREATE (e)-[:PARTICIPATED_IN]->(p) RETURN e",
                Map.of("eid", "viz-e2", "pid", "viz-p2"));

        Map<String, Object> data = graphVisualizationService.getGraphData(100, 0, null, 0);

        assertNotNull(data);
        List<?> nodes = (List<?>) data.get("nodes");
        List<?> edges = (List<?>) data.get("edges");
        assertNotNull(nodes);
        assertNotNull(edges);
        assertFalse(nodes.isEmpty());
        assertFalse(edges.isEmpty());

        Map<?, ?> firstNode = (Map<?, ?>) nodes.get(0);
        assertTrue(firstNode.containsKey("data"));
        Map<?, ?> nodeData = (Map<?, ?>) firstNode.get("data");
        assertTrue(nodeData.containsKey("id"));
        assertTrue(nodeData.containsKey("label") || nodeData.containsKey("type"));

        Map<?, ?> firstEdge = (Map<?, ?>) edges.get(0);
        assertTrue(firstEdge.containsKey("data"));
        Map<?, ?> edgeData = (Map<?, ?>) firstEdge.get("data");
        assertTrue(edgeData.containsKey("source"));
        assertTrue(edgeData.containsKey("target"));
    }

    @Test
    void getGraphDataWithVertexTypeFilterReturnsFilteredNodes() {
        graphService.executeCypher(
                "CREATE (e:Expert {id: $eid, name: $ename}) RETURN e",
                Map.of("eid", "viz-expert-only", "ename", "Expert Only"));

        Map<String, Object> data = graphVisualizationService.getGraphData(100, 0, "Expert", 0);

        assertNotNull(data);
        List<?> nodes = (List<?>) data.get("nodes");
        assertNotNull(nodes);
        assertFalse(nodes.isEmpty());
        for (Object n : nodes) {
            Map<?, ?> nodeData = (Map<?, ?>) ((Map<?, ?>) n).get("data");
            assertEquals("Expert", nodeData.get("type"));
        }
    }
}
