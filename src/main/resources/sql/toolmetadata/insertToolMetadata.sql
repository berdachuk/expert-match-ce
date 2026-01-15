INSERT INTO expertmatch.tool_metadata 
    (id, tool_name, description, tool_class, method_name, parameters, embedding, embedding_dimension, created_at, updated_at)
VALUES 
    (:id, :toolName, :description, :toolClass, :methodName, :parameters::jsonb, :embedding::vector, :embeddingDimension, :now, :now)
ON CONFLICT (tool_name) 
DO UPDATE SET
    description = EXCLUDED.description,
    parameters = EXCLUDED.parameters,
    embedding = EXCLUDED.embedding,
    embedding_dimension = EXCLUDED.embedding_dimension,
    updated_at = EXCLUDED.updated_at
