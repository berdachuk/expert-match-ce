# Technology Categories Expansion Prompt
# Purpose: Generate category mappings for additional technologies
# Input: existingTechnologies (JSON array), existingCategories (JSON object) - current mappings
# Output: JSON object with technology -> category mappings

Generate category mappings for additional technologies, following the same categorization pattern as the provided examples.

## Existing Technologies and Categories
Technologies: <existingTechnologies>
Categories: <existingCategories>

## Instructions
- Generate category mappings for 30-50 additional technologies
- Use categories like: "Programming Language", "Framework", "Database", "Cloud Platform", "Containerization", "Orchestration", "Messaging", "Frontend Framework", "Runtime", etc.
- Maintain consistent categorization logic
- Return as a JSON object: {"Technology": "Category", ...}
- Focus on accurate categorization based on technology type

## Response Format
Return a valid JSON object:
{"Technology1": "Category1", "Technology2": "Category2", ...}

## Examples
Existing: {"Java": "Programming Language", "Spring Boot": "Framework", "PostgreSQL": "Database"}
Additional: {"C#": "Programming Language", ".NET": "Framework", "MongoDB": "Database", "Docker": "Containerization", "Kubernetes": "Orchestration", "Kafka": "Messaging", "React": "Frontend Framework", "Node.js": "Runtime", ...}

