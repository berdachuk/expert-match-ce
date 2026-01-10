package com.berdachuk.expertmatch.retrieval;

import com.berdachuk.expertmatch.retrieval.domain.GapAnalysis;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for GapAnalysis.
 */
class GapAnalysisTest {

    @Test
    void testGapAnalysisWithGaps() {
        GapAnalysis analysis = new GapAnalysis(
                List.of("Missing cloud experience", "No database expertise"),
                List.of("Unclear seniority requirement"),
                List.of("Missing project context"),
                true
        );

        assertTrue(analysis.hasSignificantGaps());
        assertEquals(2, analysis.identifiedGaps().size());
        assertEquals(1, analysis.ambiguities().size());
        assertTrue(analysis.needsExpansion());
    }

    @Test
    void testGapAnalysisNoExpansion() {
        GapAnalysis analysis = GapAnalysis.noExpansionNeeded();

        assertFalse(analysis.hasSignificantGaps());
        assertTrue(analysis.identifiedGaps().isEmpty());
        assertTrue(analysis.ambiguities().isEmpty());
        assertTrue(analysis.missingInformation().isEmpty());
        assertFalse(analysis.needsExpansion());
    }

    @Test
    void testGapAnalysisNeedsExpansionButNoGaps() {
        GapAnalysis analysis = new GapAnalysis(
                List.of(),
                List.of(),
                List.of(),
                true
        );

        // If needsExpansion is true but no gaps, hasSignificantGaps should be false
        assertFalse(analysis.hasSignificantGaps());
    }

    @Test
    void testGapAnalysisWithGapsButNoExpansion() {
        GapAnalysis analysis = new GapAnalysis(
                List.of("Minor gap"),
                List.of(),
                List.of(),
                false
        );

        // If needsExpansion is false, hasSignificantGaps should be false
        assertFalse(analysis.hasSignificantGaps());
    }
}

