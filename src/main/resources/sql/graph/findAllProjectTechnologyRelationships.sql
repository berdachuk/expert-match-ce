SELECT DISTINCT we.project_id, we.project_name, unnest(we.technologies) as technology
FROM expertmatch.work_experience we
WHERE we.technologies IS NOT NULL AND we.project_name IS NOT NULL