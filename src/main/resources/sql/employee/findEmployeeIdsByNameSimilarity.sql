SELECT id
FROM expertmatch.employee
WHERE similarity(LOWER(name), LOWER(:name)) >= :threshold
   OR word_similarity(LOWER(:name), LOWER(name)) >= :threshold
ORDER BY GREATEST(
    similarity(LOWER(name), LOWER(:name)),
    word_similarity(LOWER(:name), LOWER(name))
) DESC
LIMIT :maxResults
