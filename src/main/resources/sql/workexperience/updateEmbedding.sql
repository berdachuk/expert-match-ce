UPDATE expertmatch.work_experience
    SET embedding = :embedding::vector,
        embedding_dimension = :dimension
WHERE id = :id
