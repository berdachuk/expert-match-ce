Extract all domain/industry entities mentioned in the following query.

## Query
<query>

## Instructions
- Extract all domain/industry entities (e.g., banking, healthcare, retail, finance, insurance, automotive, etc.)
- Include both explicit mentions and implied domains based on context
- Return as a JSON array of domain objects with type, name, and id fields
- Use lowercase for domain names (e.g., "banking", "healthcare")
- If no domains are found, return an empty array

## Response Format
Respond with a valid JSON array of domain objects:
[
  {"type": "domain", "name": "banking", "id": "banking"},
  {"type": "domain", "name": "healthcare", "id": "healthcare"}
]

Example responses:
- Query: "Need expert for banking application"
  Response: [{"type": "domain", "name": "banking", "id": "banking"}]

- Query: "Looking for healthcare domain specialist"
  Response: [{"type": "domain", "name": "healthcare", "id": "healthcare"}]

- Query: "Finance and insurance project"
  Response: [{"type": "domain", "name": "finance", "id": "finance"}, {"type": "domain", "name": "insurance", "id": "insurance"}]

- Query: "No specific domain mentioned"
  Response: []

