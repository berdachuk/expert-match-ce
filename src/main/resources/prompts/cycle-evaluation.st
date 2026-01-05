Evaluate multiple experts for the given requirements. You must evaluate EACH expert using the same structured approach.

## Requirements
<query>

## Experts to Evaluate
<expertsSection>
## Evaluation Steps (Cycle Pattern)
For EACH expert, you must follow these steps in sequence:

1. **Expert Summary**: Brief summary of the expert's background
2. **Skill Match Analysis**: Analyze skill match (must-have: 0-10, nice-to-have: 0-10, missing skills, strength skills)
3. **Experience Assessment**: Assess experience (relevant projects count, domain years, customer/industry match, seniority match)
4. **Recommendation**: Final recommendation (type: STRONGLY_RECOMMEND|RECOMMEND|CONDITIONAL|NOT_RECOMMEND, confidence: 0-100, rationale)

## Response Format
Respond with a valid JSON array containing <expertCount> evaluation objects:
[
  {
    "expertSummary": "string",
    "skillMatchAnalysis": {
      "mustHaveMatchScore": 0-10,
      "niceToHaveMatchScore": 0-10,
      "missingSkills": ["string"],
      "strengthSkills": ["string"]
    },
    "experienceAssessment": {
      "relevantProjectsCount": number,
      "domainExperienceYears": number,
      "customerIndustryMatch": boolean,
      "seniorityMatch": boolean
    },
    "recommendation": {
      "recommendationType": "STRONGLY_RECOMMEND|RECOMMEND|CONDITIONAL|NOT_RECOMMEND",
      "confidenceScore": 0-100,
      "rationale": "string"
    }
  },
  // ... repeat for each expert
]

IMPORTANT: You must provide exactly <expertCount> evaluation objects in the array, one for each expert.

