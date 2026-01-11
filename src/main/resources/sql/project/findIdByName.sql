SELECT id FROM expertmatch.project
WHERE LOWER(name) LIKE :namePattern
LIMIT 1
