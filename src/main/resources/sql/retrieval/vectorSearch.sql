SELECT 
    we.id,
    we.employee_id,
    we.project_name,
    we.project_summary,
    we.role,
    we.technologies,
    1 - (we.embedding <=> :queryVector::vector) as similarity
FROM expertmatch.work_experience we
WHERE we.embedding IS NOT NULL
AND 1 - (we.embedding <=> :queryVector::vector) >= :threshold
ORDER BY we.embedding <=> :queryVector::vector
LIMIT :maxResults
