# Team Names Constant Expansion Prompt
# Purpose: Generate additional team name patterns similar to provided examples
# Input: existingTeamNames (JSON array) - current team names list
# Output: JSON array of additional team name patterns

Generate additional team name patterns for software development teams that follow the same pattern as the provided examples.

## Existing Team Names
<existingTeamNames>

## Instructions
- Generate 15-25 additional team name patterns
- Follow the pattern: "{Domain/Technology} Team" or "{Function} Team"
- Maintain similar naming style and structure
- Do not duplicate existing team names
- Return as a JSON array of strings
- Focus on common team structures in enterprise software development

## Response Format
Return a valid JSON array:
["Team Name1", "Team Name2", "Team Name3", ...]

## Examples
Existing: ["Backend Team", "Frontend Team", "DevOps Team", "QA Team"]
Additional: ["Mobile Team", "Cloud Team", "Security Team", "Performance Team", "Integration Team", "Platform Team", "Data Team", "AI/ML Team", "Infrastructure Team", "Support Team", "Architecture Team", "Product Team", "Scrum Team", "Agile Team", ...]

