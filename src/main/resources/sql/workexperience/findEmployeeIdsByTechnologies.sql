SELECT DISTINCT employee_id
FROM expertmatch.work_experience
WHERE technologies && ARRAY[:technologies]::text[]
