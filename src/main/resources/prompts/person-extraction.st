Extract all person entities (expert names, people mentioned) from the following query.

## Query
<query>

## Instructions
- Extract all person names mentioned in the query
- Include expert names, team member names, or any person references
- Return as a JSON array of person objects with type, name, and id fields
- Use the person's name as both name and id (normalized to lowercase, spaces replaced with hyphens)
- If no persons are found, return an empty array

## Response Format
Respond with a valid JSON array of person objects:
[
  {"type": "person", "name": "John Doe", "id": "john-doe"},
  {"type": "person", "name": "Jane Smith", "id": "jane-smith"}
]

Example responses:
- Query: "Need expert like John Doe"
  Response: [{"type": "person", "name": "John Doe", "id": "john-doe"}]

- Query: "Looking for someone similar to Jane Smith or Bob Johnson"
  Response: [{"type": "person", "name": "Jane Smith", "id": "jane-smith"}, {"type": "person", "name": "Bob Johnson", "id": "bob-johnson"}]

- Query: "No specific person mentioned"
  Response: []

