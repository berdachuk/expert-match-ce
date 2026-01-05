Extract all technology entities (technologies, frameworks, tools) from the following query.

## Query
<query>

## Instructions
- Extract all technology entities mentioned in the query
- Include technologies, frameworks, libraries, tools, platforms
- Return as a JSON array of technology objects with type, name, and id fields
- Use the technology name as both name and id (normalized to lowercase, spaces replaced with hyphens)
- If no technologies are found, return an empty array
- Note: This is for entity extraction (different from QueryParser.extractTechnologies which extracts with versions)

## Response Format
Respond with a valid JSON array of technology objects:
[
  {"type": "technology", "name": "Java", "id": "java"},
  {"type": "technology", "name": "Spring Boot", "id": "spring-boot"}
]

Example responses:
- Query: "Need expert in Java and Spring Boot"
  Response: [{"type": "technology", "name": "Java", "id": "java"}, {"type": "technology", "name": "Spring Boot", "id": "spring-boot"}]

- Query: "Looking for React developer"
  Response: [{"type": "technology", "name": "React", "id": "react"}]

- Query: "No specific technologies mentioned"
  Response: []

