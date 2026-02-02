SELECT DISTINCT unnest(technologies) as technology
FROM expertmatch.work_experience
WHERE technologies IS NOT NULL