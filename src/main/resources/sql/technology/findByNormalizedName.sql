SELECT id, name, normalized_name, category, synonyms
FROM expertmatch.technology
WHERE LOWER(normalized_name) = LOWER(:normalizedName)
