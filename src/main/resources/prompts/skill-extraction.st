Extract all technical skills, technologies, and programming languages mentioned in the following query.

## Query
<query>

## Instructions
- Extract all technical skills, technologies, frameworks, libraries, and programming languages
- Include both explicit mentions and implied technologies
- Return skills as a JSON array of strings
- Use standard naming conventions (e.g., "Spring Boot" not "springboot", "Node.js" not "nodejs")
- If no skills are found, return an empty array

## Response Format
Respond with a valid JSON array of skill strings:
["skill1", "skill2", "skill3"]

Example responses:
- Query: "I need a Java developer with Spring Boot experience"
  Response: ["Java", "Spring Boot"]

- Query: "Looking for someone who knows React and TypeScript"
  Response: ["React", "TypeScript"]

- Query: "Need expert in AWS, Kubernetes, and Docker"
  Response: ["AWS", "Kubernetes", "Docker"]

