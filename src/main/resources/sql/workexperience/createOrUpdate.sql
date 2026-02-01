INSERT INTO expertmatch.work_experience
    (id, employee_id, project_id, customer_id, project_name, project_summary, role, start_date, end_date,
     technologies, responsibilities, customer_name, industry, metadata)
VALUES (:id, :employeeId, :projectId, :customerId, :projectName, :projectSummary, :role, :startDate, :endDate,
        :technologies, :responsibilities, :customerName, :industry, :metadata::jsonb)
ON CONFLICT (id) DO UPDATE SET
    employee_id = EXCLUDED.employee_id,
    project_id = EXCLUDED.project_id,
    customer_id = EXCLUDED.customer_id,
    project_name = EXCLUDED.project_name,
    project_summary = EXCLUDED.project_summary,
    role = EXCLUDED.role,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    technologies = EXCLUDED.technologies,
    responsibilities = EXCLUDED.responsibilities,
    customer_name = EXCLUDED.customer_name,
    industry = EXCLUDED.industry,
    metadata = EXCLUDED.metadata
