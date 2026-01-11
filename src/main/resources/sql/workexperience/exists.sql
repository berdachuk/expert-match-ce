SELECT id FROM expertmatch.work_experience
WHERE employee_id = :employeeId 
  AND project_name = :projectName 
  AND start_date = :startDate
LIMIT 1
