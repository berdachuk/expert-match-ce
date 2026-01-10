SELECT id, name, email, seniority, language_english, availability_status
FROM expertmatch.employee
WHERE id = :id
