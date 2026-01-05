Find the best matching employee names from the candidate list that match the query name, considering typos, spelling variations, and partial matches.

## Query Name
<queryName>

## Candidate Employee Names
<candidateNames>

## Instructions
- Compare the query name with each candidate name
- Consider typos, spelling variations (e.g., "Sergei" vs "Siarhei"), and partial matches
- Return a JSON array of matching employee names in order of best match first
- Only include names that are likely to be the same person (account for common typos and variations)
- If no good matches are found, return an empty array
- Return only the employee names (not IDs), exactly as they appear in the candidate list

## Response Format
Respond with a valid JSON array of matching employee names:
["John Doe", "Jane Smith"]

Example responses:
- Query: "Sergei Berdachuk", Candidates: ["Siarhei Berdachuk", "John Smith"]
  Response: ["Siarhei Berdachuk"]

- Query: "John", Candidates: ["John Doe", "John Smith", "Jane Doe"]
  Response: ["John Doe", "John Smith"]

- Query: "Berdachuck", Candidates: ["Siarhei Berdachuk", "John Smith"]
  Response: ["Siarhei Berdachuk"]

- Query: "John Smith", Candidates: ["John Doe", "Jane Smith"]
  Response: []

