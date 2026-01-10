SELECT 
    tool_name,
    description,
    tool_class,
    method_name,
    parameters,
    1 - (embedding <=> :queryVector::vector) as similarity
FROM expertmatch.tool_metadata
WHERE embedding IS NOT NULL
AND 1 - (embedding <=> :queryVector::vector) >= 0.5
ORDER BY embedding <=> :queryVector::vector
LIMIT :maxResults
