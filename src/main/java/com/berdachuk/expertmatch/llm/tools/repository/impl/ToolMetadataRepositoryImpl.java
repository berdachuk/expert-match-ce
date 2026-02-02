package com.berdachuk.expertmatch.llm.tools.repository.impl;

import com.berdachuk.expertmatch.core.repository.sql.InjectSql;
import com.berdachuk.expertmatch.llm.tools.repository.ToolMetadataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

/**
 * Repository implementation for tool metadata operations.
 */
@Slf4j
@Repository
public class ToolMetadataRepositoryImpl implements ToolMetadataRepository {

    private final NamedParameterJdbcTemplate namedJdbcTemplate;

    @InjectSql("/sql/toolmetadata/deleteByToolClass.sql")
    private String deleteByToolClassSql;

    @InjectSql("/sql/toolmetadata/insertToolMetadata.sql")
    private String insertToolMetadataSql;

    public ToolMetadataRepositoryImpl(NamedParameterJdbcTemplate namedJdbcTemplate) {
        this.namedJdbcTemplate = namedJdbcTemplate;
    }

    @Override
    public void insertToolMetadata(
            String id,
            String toolName,
            String description,
            String toolClass,
            String methodName,
            String parametersJson,
            String embedding,
            int embeddingDimension,
            java.sql.Timestamp now
    ) {
        Map<String, Object> params = Map.of(
                "id", id,
                "toolName", toolName,
                "description", description,
                "toolClass", toolClass,
                "methodName", methodName,
                "parameters", parametersJson,
                "embedding", embedding,
                "embeddingDimension", embeddingDimension,
                "now", now
        );
        namedJdbcTemplate.update(insertToolMetadataSql, params);
    }

    @Override
    public int deleteByToolClass(String toolClass) {
        Map<String, Object> params = Map.of("toolClass", toolClass);
        return namedJdbcTemplate.update(deleteByToolClassSql, params);
    }
}
