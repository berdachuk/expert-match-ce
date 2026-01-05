Extract language requirements and proficiency levels mentioned in the following query.

## Query
<query>

## Instructions
- Extract language requirements (e.g., English, Russian) and proficiency levels
- Proficiency levels follow the format: A1, A2, B1, B2, C1, C2 (Common European Framework)
- Return as a JSON object with language and proficiency fields
- If no language requirements are found, return null
- If language is mentioned but proficiency is not specified, set proficiency to null

## Response Format
Respond with a valid JSON object:
{
  "language": "English",
  "proficiency": "C1"
}

Or if no language requirements:
null

Example responses:
- Query: "Need expert with English C1+"
  Response: {"language": "English", "proficiency": "C1"}

- Query: "Russian B2 level required"
  Response: {"language": "Russian", "proficiency": "B2"}

- Query: "Must speak English"
  Response: {"language": "English", "proficiency": null}

- Query: "No language requirements"
  Response: null

