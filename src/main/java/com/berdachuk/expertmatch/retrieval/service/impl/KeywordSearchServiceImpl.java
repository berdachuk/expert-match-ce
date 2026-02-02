package com.berdachuk.expertmatch.retrieval.service.impl;

import com.berdachuk.expertmatch.retrieval.repository.KeywordSearchRepository;
import com.berdachuk.expertmatch.retrieval.service.KeywordSearchService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for keyword/full-text search using PostgreSQL.
 */
@Slf4j
@Service
public class KeywordSearchServiceImpl implements KeywordSearchService {

    private final KeywordSearchRepository repository;

    public KeywordSearchServiceImpl(KeywordSearchRepository repository) {
        this.repository = repository;
    }

    /**
     * Searches work experience by keywords.
     */
    @Override
    public List<String> searchByKeywords(List<String> keywords, int maxResults) {
        // Validate input parameters
        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("Keywords cannot be null or empty");
        }
        if (maxResults < 1) {
            throw new IllegalArgumentException("Max results must be at least 1, got: " + maxResults);
        }

        // Build full-text search query
        // Use plainto_tsquery instead of to_tsquery for better handling of user input
        // plainto_tsquery automatically handles multi-word terms, special characters, and AND logic
        String searchTerms = String.join(" ", keywords);

        try {
            return repository.searchByKeywords(searchTerms, maxResults);
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) and other SQL errors gracefully
            // This can happen if a previous query in the transaction failed
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                // Transaction is aborted - return empty results to allow graceful degradation
                log.warn("Keyword search failed due to aborted transaction - returning empty results to allow graceful degradation. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return List.of();
            }
            // Re-throw other SQL errors
            log.error("SQL error during keyword search", e);
            throw e;
        } catch (Exception e) {
            // Handle any other exceptions (e.g., invalid search terms, connection issues)
            log.warn("Keyword search failed - returning empty results to allow graceful degradation. Error: {}",
                    e.getMessage());
            log.debug("Keyword search error details", e);
            return List.of();
        }
    }

    /**
     * Searches by exact technology match.
     */
    @Override
    public List<String> searchByTechnologies(List<String> technologies, int maxResults) {
        // Validate input parameters
        if (technologies == null || technologies.isEmpty()) {
            throw new IllegalArgumentException("Technologies cannot be null or empty");
        }
        if (maxResults < 1) {
            throw new IllegalArgumentException("Max results must be at least 1, got: " + maxResults);
        }

        try {
            return repository.searchByTechnologies(technologies.toArray(new String[0]), maxResults);
        } catch (org.springframework.jdbc.UncategorizedSQLException e) {
            // Handle transaction aborted errors (25P02) and other SQL errors gracefully
            java.sql.SQLException sqlException = e.getSQLException();
            String sqlState = sqlException != null ? sqlException.getSQLState() : null;
            if ("25P02".equals(sqlState)) {
                log.warn("Technology search failed due to aborted transaction - returning empty results to allow graceful degradation. Error: {}",
                        e.getMessage());
                log.debug("Transaction aborted error details", e);
                return List.of();
            }
            log.error("SQL error during technology search", e);
            throw e;
        } catch (Exception e) {
            log.warn("Technology search failed - returning empty results to allow graceful degradation. Error: {}",
                    e.getMessage());
            log.debug("Technology search error details", e);
            return List.of();
        }
    }
}
