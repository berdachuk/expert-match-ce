INSERT INTO expertmatch.employee (id, name, email, seniority, language_english, availability_status)
VALUES (:id, :name, :email, :seniority, :languageEnglish, :availabilityStatus)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    email = EXCLUDED.email,
    seniority = EXCLUDED.seniority,
    language_english = EXCLUDED.language_english,
    availability_status = EXCLUDED.availability_status
