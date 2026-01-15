INSERT INTO expertmatch.technology (id, name, normalized_name, category, synonyms)
VALUES (:id, :name, :normalizedName, :category, :synonyms)
ON CONFLICT (name) DO UPDATE SET
    normalized_name = EXCLUDED.normalized_name,
    category = EXCLUDED.category,
    synonyms = EXCLUDED.synonyms
