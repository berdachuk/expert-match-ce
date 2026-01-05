Evaluate the following expert for the given requirements using a structured, step-by-step approach.

## Requirements
<query>

## Expert Information
<expertInfo>

## Evaluation Steps (Cascade Pattern)
You must follow these steps in sequence:

1. **Expert Summary**: Provide a brief summary of the expert's background and experience.
2. **Skill Match Analysis**: Analyze how well the expert's skills match the requirements.
   - Score must-have skills match (0-10)
   - Score nice-to-have skills match (0-10)
   - List missing skills
   - List strength skills
3. **Experience Assessment**: Assess the expert's relevant experience.
   - Count relevant projects
   - Estimate domain experience years
   - Check customer/industry match
   - Check seniority match
4. **Recommendation**: Provide final recommendation.
   - Recommendation type: STRONGLY_RECOMMEND, RECOMMEND, CONDITIONAL, or NOT_RECOMMEND
   - Confidence score (0-100)
   - Rationale for the recommendation

## Response Format
Respond with a valid JSON object matching this structure:
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
}

