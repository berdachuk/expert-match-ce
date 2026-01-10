SELECT DISTINCT we.employee_id
FROM expertmatch.work_experience we
WHERE to_tsvector('english', 
    COALESCE(we.project_summary, '') || ' ' || 
    COALESCE(we.responsibilities, '') || ' ' ||
    array_to_string(we.technologies, ' ')
    ) @@ plainto_tsquery('english', :searchTerms)
LIMIT :maxResults
