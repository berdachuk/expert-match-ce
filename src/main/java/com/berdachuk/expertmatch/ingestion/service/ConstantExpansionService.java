package com.berdachuk.expertmatch.ingestion.service;

import java.util.List;
import java.util.Map;


/**
 * Service interface for constantexpansion operations.
 */
public interface ConstantExpansionService {
    /**
     * Expands a list of technologies using LLM to include related technologies.
     *
     * @param existingTechnologies List of existing technology names
     * @return Expanded list of technologies including related ones
     */
    List<String> expandTechnologies(List<String> existingTechnologies);

    /**
     * Expands a list of tools using LLM to include related tools.
     *
     * @param existingTools List of existing tool names
     * @return Expanded list of tools including related ones
     */
    List<String> expandTools(List<String> existingTools);

    /**
     * Expands a list of project types using LLM to include related project types.
     *
     * @param existingProjectTypes List of existing project type names
     * @return Expanded list of project types including related ones
     */
    List<String> expandProjectTypes(List<String> existingProjectTypes);

    /**
     * Expands a list of team names using LLM to include related team names.
     *
     * @param existingTeamNames List of existing team names
     * @return Expanded list of team names including related ones
     */
    List<String> expandTeamNames(List<String> existingTeamNames);

    /**
     * Expands technology categories using LLM.
     *
     * @param technologies       List of technology names
     * @param existingCategories Map of existing technology to category mappings
     * @return Expanded map of technology to category mappings
     */
    Map<String, String> expandTechnologyCategories(List<String> technologies,
                                                   Map<String, String> existingCategories);

    /**
     * Expands technology synonyms using LLM.
     *
     * @param technologies     List of technology names
     * @param existingSynonyms Map of existing technology to synonyms array mappings
     * @return Expanded map of technology to synonyms array mappings
     */
    Map<String, String[]> expandTechnologySynonyms(List<String> technologies,
                                                   Map<String, String[]> existingSynonyms);
}
