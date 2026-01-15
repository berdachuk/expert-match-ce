SELECT id
FROM expertmatch.employee
WHERE LOWER(name) LIKE LOWER(:namePattern)
LIMIT :maxResults
