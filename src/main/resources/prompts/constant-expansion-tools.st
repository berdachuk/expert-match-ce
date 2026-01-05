# Development Tools Constant Expansion Prompt
# Purpose: Generate additional development tool names similar to provided examples
# Input: existingTools (JSON array) - current tools list
# Output: JSON array of additional tool names

Generate additional development tool names that are similar in style and relevance to the following examples.

## Existing Tools
<existingTools>

## Instructions
- Generate 20-30 additional tool names
- Include IDEs, version control, CI/CD, testing, monitoring, API tools
- Maintain similar naming style (product names, not generic terms)
- Do not duplicate existing tools
- Return as a JSON array of strings
- Focus on commonly used tools in enterprise software development

## Response Format
Return a valid JSON array:
["Tool1", "Tool2", "Tool3", ...]

## Examples
Existing: ["IntelliJ IDEA", "VS Code", "Git", "Jira", "Jenkins"]
Additional: ["PyCharm", "WebStorm", "Eclipse", "NetBeans", "Sublime Text", "Atom", "GitHub", "GitLab", "Bitbucket", "Bamboo", "TeamCity", "CircleCI", "Travis CI", "Selenium", "JUnit", "TestNG", "Postman", "SoapUI", "New Relic", "Datadog", "Splunk", ...]

