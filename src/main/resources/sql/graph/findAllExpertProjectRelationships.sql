SELECT we.employee_id, we.project_id, we.project_name, we.role, we.start_date, we.end_date
FROM expertmatch.work_experience we
WHERE we.project_name IS NOT NULL