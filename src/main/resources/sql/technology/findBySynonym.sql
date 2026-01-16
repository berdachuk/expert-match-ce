SELECT id, name, normalized_name, category, synonyms
FROM expertmatch.technology
WHERE EXISTS (
    SELECT 1
    FROM unnest(synonyms) AS synonym
    WHERE LOWER(synonym) = LOWER(:synonym)
)
