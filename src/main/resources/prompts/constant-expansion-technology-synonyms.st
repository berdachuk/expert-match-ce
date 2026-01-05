# Technology Synonyms Expansion Prompt
# Purpose: Generate synonym mappings for additional technologies
# Input: existingTechnologies (JSON array), existingSynonyms (JSON object) - current mappings
# Output: JSON object with technology -> synonyms array mappings

Generate synonym mappings for additional technologies, following the same pattern as the provided examples.

## Existing Technologies and Synonyms
Technologies: <existingTechnologies>
Synonyms: <existingSynonyms>

## Instructions
- Generate synonym mappings for 30-50 additional technologies
- Include common alternative names, abbreviations, and variations
- Maintain similar synonym patterns (abbreviations, full names, variations)
- Return as a JSON object: {"Technology": ["Synonym1", "Synonym2"], ...}
- Focus on commonly used alternative names in the industry

## Response Format
Return a valid JSON object:
{"Technology1": ["Synonym1", "Synonym2"], "Technology2": ["Synonym3"], ...}

## Examples
Existing: {"Java": ["Java Programming", "Java Language"], "Node.js": ["Node", "NodeJS"]}
Additional: {"C#": ["C Sharp", "CSharp"], ".NET": ["DotNet", "Dot NET"], "MongoDB": ["Mongo"], "PostgreSQL": ["Postgres", "Postgre"], "JavaScript": ["JS"], "TypeScript": ["TS"], ...}

