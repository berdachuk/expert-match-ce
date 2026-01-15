SELECT id, name
FROM expertmatch.employee
WHERE LOWER(name) LIKE LOWER(:namePattern)
   OR LOWER(name) LIKE LOWER(:firstPattern)
   OR LOWER(name) LIKE LOWER(:lastPattern)
LIMIT 100
