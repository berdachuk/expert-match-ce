Extract all seniority levels mentioned in the following query.

## Query
<query>

## Instructions
- Extract all seniority levels mentioned in the query
- Seniority levels follow the format: A1-A5, B1-B3, C1-C2
  - **Hierarchy**: B levels are higher than A levels, C levels are higher than B levels
  - A levels: A1 (Junior), A2 (Middle), A3 (Senior), A4 (Lead), A5 (Principal)
  - B levels: B1 (Junior Manager), B2 (Middle Manager), B3 (Senior Manager) - **Higher than A levels**
  - C levels: C1 (Director), C2 (VP/Executive) - **Higher than B levels**
- Return seniority levels as a JSON array of strings in uppercase format (e.g., "A4", "B2")
- If no seniority levels are found, return an empty array

## Response Format
Respond with a valid JSON array of seniority level strings:
["A4", "A5", "B1"]

Example responses:
- Query: "Need A4 or A5 level expert"
  Response: ["A4", "A5"]

- Query: "Looking for senior developer"
  Response: ["A3"]

- Query: "Need a team lead"
  Response: ["A4"]

- Query: "No specific seniority mentioned"
  Response: []

