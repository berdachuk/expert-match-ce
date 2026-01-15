INSERT INTO expertmatch.work_experience 
    (id, employee_id, project_id, project_name, project_summary, role, start_date, end_date,
     technologies, responsibilities, customer_name, industry, metadata)
    VALUES (:id, :employeeId, :projectId, :projectName, :projectSummary, :role, :startDate, :endDate,
            :technologies, :responsibilities, :customerName, :industry, :metadata::jsonb)
ON CONFLICT (id) DO NOTHING
