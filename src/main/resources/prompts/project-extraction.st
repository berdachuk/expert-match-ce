Extract all project entities (project names, project references) from the following query.

## Query
<query>

## Instructions
- Extract all project names mentioned in the query
- Include project names, project codes, or any project references
- Return as a JSON array of project objects with type, name, and id fields
- Use the project name as both name and id (normalized to lowercase, spaces replaced with hyphens)
- If no projects are found, return an empty array

## Response Format
Respond with a valid JSON array of project objects:
[
  {"type": "project", "name": "Project Alpha", "id": "project-alpha"},
  {"type": "project", "name": "Banking System", "id": "banking-system"}
]

Example responses:
- Query: "Need expert for Project Alpha"
  Response: [{"type": "project", "name": "Project Alpha", "id": "project-alpha"}]

- Query: "Looking for someone who worked on Banking System or Payment Gateway"
  Response: [{"type": "project", "name": "Banking System", "id": "banking-system"}, {"type": "project", "name": "Payment Gateway", "id": "payment-gateway"}]

- Query: "No specific project mentioned"
  Response: []

