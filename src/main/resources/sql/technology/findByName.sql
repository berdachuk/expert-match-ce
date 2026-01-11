SELECT id, name, normalized_name, category, synonyms
FROM expertmatch.technology
WHERE name = :name
