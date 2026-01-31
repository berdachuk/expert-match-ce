SELECT DISTINCT
    we.project_id,
    COALESCE(we.customer_id, 'CUSTOMER_' || we.customer_name) as customer_id
FROM expertmatch.work_experience we
WHERE we.customer_name IS NOT NULL AND we.project_id IS NOT NULL