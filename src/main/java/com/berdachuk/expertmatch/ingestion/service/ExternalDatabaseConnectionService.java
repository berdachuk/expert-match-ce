package com.berdachuk.expertmatch.ingestion.service;

/**
 * Service for verifying external database connection.
 */
public interface ExternalDatabaseConnectionService {

    /**
     * Verifies connection to external database.
     *
     * @return true if connection is successful, false otherwise
     */
    boolean verifyConnection();

    /**
     * Gets connection information (without sensitive data).
     *
     * @return connection info string
     */
    String getConnectionInfo();
}
