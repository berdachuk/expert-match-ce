SELECT DISTINCT we.employee_id
FROM expertmatch.work_experience we
WHERE we.technologies && ARRAY[:technologies]::text[]
LIMIT :maxResults
