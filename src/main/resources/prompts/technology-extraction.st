Extract all technologies with version numbers mentioned in the following query.

## Query
<query>

## Instructions
- Extract technologies that are mentioned with specific version numbers or version ranges
- Include technologies like: Java, Spring Boot, Python, Node.js, React, Angular, etc.
- Format: "Technology Version" (e.g., "Java 21", "Spring Boot 3.5", "Python 3.9+")
- Return as a JSON array of technology strings with versions
- If no technologies with versions are found, return an empty array
- Do not include technologies without version numbers (those should be extracted as skills)

## Response Format
Respond with a valid JSON array of technology strings with versions:
["Java 21", "Spring Boot 3.5", "Python 3.9+"]

Example responses:
- Query: "Looking for Java 21+ and Spring Boot 3.5"
  Response: ["Java 21+", "Spring Boot 3.5"]

- Query: "Need React 18 and TypeScript 5.0"
  Response: ["React 18", "TypeScript 5.0"]

- Query: "Python developer needed"
  Response: []

- Query: "Node.js 20+ and Angular 15"
  Response: ["Node.js 20+", "Angular 15"]

