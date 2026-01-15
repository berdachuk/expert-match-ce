SELECT id, employee_id, project_id, customer_id, project_name, customer_name, industry,
       role, start_date, end_date, project_summary, responsibilities, technologies
FROM expertmatch.work_experience
WHERE embedding IS NULL
