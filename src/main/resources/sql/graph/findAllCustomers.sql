SELECT DISTINCT
    COALESCE(customer_id, 'CUSTOMER_' || customer_name) as customer_id,
    customer_name
FROM expertmatch.work_experience
WHERE customer_name IS NOT NULL