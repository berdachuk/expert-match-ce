INSERT INTO expertmatch.project 
(id, name, summary, link, project_type, technologies)
VALUES (:id, :name, :summary, :link, :projectType, :technologies)
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    summary = EXCLUDED.summary,
    link = EXCLUDED.link,
    project_type = EXCLUDED.project_type,
    technologies = EXCLUDED.technologies
