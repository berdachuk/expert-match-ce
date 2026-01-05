# Project Types Constant Expansion Prompt
# Purpose: Generate additional project type names similar to provided examples
# Input: existingProjectTypes (JSON array) - current project types list
# Output: JSON array of additional project type names

Generate additional project type names for software development projects that are similar in style and relevance to the following examples.

## Existing Project Types
<existingProjectTypes>

## Instructions
- Generate 15-25 additional project type names
- Include various types of software projects (web, mobile, enterprise, data, etc.)
- Maintain similar naming style (descriptive, professional)
- Do not duplicate existing project types
- Return as a JSON array of strings
- Focus on common project types in enterprise software development

## Response Format
Return a valid JSON array:
["Project Type1", "Project Type2", "Project Type3", ...]

## Examples
Existing: ["Web Application", "Microservices", "ETL Pipeline", "Mobile App"]
Additional: ["REST API", "GraphQL API", "Single Page Application", "Progressive Web App", "Desktop Application", "Cloud Native Application", "Serverless Application", "Data Warehouse", "Business Intelligence", "Machine Learning Platform", "IoT Solution", "Blockchain Application", "Real-time Analytics", "Content Management System", "E-commerce Platform", ...]

