package com.berdachuk.expertmatch.ingestion.service;

import com.berdachuk.expertmatch.ingestion.model.IngestionResult;

import java.io.IOException;


/**
 * Service interface for jsonprofileingestion operations.
 */
public interface JsonProfileIngestionService {
    /**
     * Ingests an employee profile from JSON content.
     *
     * @param jsonContent The JSON content containing the employee profile
     * @param sourceName  The name of the source (for logging/tracking)
     * @return Ingestion result containing processing statistics and any errors
     */
    IngestionResult ingestFromContent(String jsonContent, String sourceName);

    /**
     * Ingests an employee profile from a file resource.
     *
     * @param resourcePath The path to the resource file (classpath or filesystem)
     * @return Ingestion result containing processing statistics and any errors
     * @throws IOException if the file cannot be read
     */
    IngestionResult ingestFromFile(String resourcePath) throws IOException;

    /**
     * Ingests employee profiles from all JSON files in a directory.
     *
     * @param directoryPath The path to the directory containing JSON profile files
     * @return Ingestion result containing processing statistics and any errors
     */
    IngestionResult ingestFromDirectory(String directoryPath);
}
