SELECT DISTINCT we.project_id, we.project_name, we.industry as domain
FROM expertmatch.work_experience we
WHERE we.industry IS NOT NULL AND we.project_name IS NOT NULL