Extract all organization entities (companies, customers, clients) from the following query.

## Query
<query>

## Instructions
- Extract all organization names mentioned in the query
- Include company names, customer names, client names, or any organization references
- Return as a JSON array of organization objects with type, name, and id fields
- Use the organization's name as both name and id (normalized to lowercase, spaces replaced with hyphens)
- If no organizations are found, return an empty array

## Response Format
Respond with a valid JSON array of organization objects:
[
  {"type": "organization", "name": "Acme Corp", "id": "acme-corp"},
  {"type": "organization", "name": "Tech Solutions Inc", "id": "tech-solutions-inc"}
]

Example responses:
- Query: "Need expert for Acme Corp project"
  Response: [{"type": "organization", "name": "Acme Corp", "id": "acme-corp"}]

- Query: "Looking for someone with experience at Microsoft or Google"
  Response: [{"type": "organization", "name": "Microsoft", "id": "microsoft"}, {"type": "organization", "name": "Google", "id": "google"}]

- Query: "No specific organization mentioned"
  Response: []

