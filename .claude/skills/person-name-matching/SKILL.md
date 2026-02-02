---
name: person-name-matching
description: Match person names to expert profiles using exact, partial, and fuzzy matching strategies with LLM fallback
---

# Person Name Matching

This skill guides you on how to match person names to expert profiles in ExpertMatch, handling variations, typos, and
partial matches.

## Overview

Person name matching in ExpertMatch uses a multi-strategy approach:

1. **Exact Match** - Direct name lookup
2. **Partial Match** - Substring matching
3. **Fuzzy Match** - Similarity-based matching
4. **LLM-Assisted Match** - Use LLM to interpret name variations

## When to Use This Skill

Use this skill when:

- User provides a person's name to find their profile
- User asks "Who is [Name]?" or "Find [Name]"
- User references an expert by name in conversation
- You need to resolve name variations or typos

## Available Tools

### Primary Tool: `getExpertProfile`

Use `getExpertProfile` to look up an expert by name:

```java
getExpertProfile(expertId:null, expertName:"John Doe")
```

**Note**: The current implementation may require expert ID for reliable lookup. For name-based search, consider using
`expertQuery` with the name.

### Alternative: Use `expertQuery`

For name-based searches, `expertQuery` can be more effective:

```java
expertQuery(query:"Find expert named John Doe", chatId:chatId)
```

This leverages the hybrid retrieval system which may find experts by name through various search strategies.

## Matching Strategies

### Strategy 1: Exact Match

**When to use:**

- User provides full name exactly as stored
- High confidence in name spelling

**Approach:**

1. Try `getExpertProfile` with exact name
2. If not found, try `expertQuery` with exact name

**Example:**

```
User: "Find John Doe"
→ getExpertProfile(expertName: "John Doe")
→ If null: expertQuery("Find expert named John Doe")
```

### Strategy 2: Partial Match

**When to use:**

- User provides first name only
- User provides last name only
- User provides partial name

**Approach:**

1. Use `expertQuery` with partial name
2. Filter results by name similarity
3. Present matches with confidence indicators

**Example:**

```
User: "Find John"
→ expertQuery("Find expert named John")
→ Filter results where name contains "John"
→ Present: "Found multiple experts named John: John Doe, John Smith..."
```

### Strategy 3: Fuzzy Match

**When to use:**

- Name may have typos
- Name variations (e.g., "Mike" vs "Michael")
- Different spellings (e.g., "Smith" vs "Smyth")

**Approach:**

1. Use `expertQuery` with the provided name
2. Vector search may find similar names semantically
3. Present matches with similarity scores
4. Ask for clarification if multiple matches

**Example:**

```
User: "Find Micheal Smith" (typo: "Micheal" instead of "Michael")
→ expertQuery("Find expert named Micheal Smith")
→ Results may include "Michael Smith" due to semantic similarity
→ Present: "Did you mean Michael Smith? Found 1 expert..."
```

### Strategy 4: LLM-Assisted Match

**When to use:**

- Ambiguous name matches
- Multiple experts with same name
- Need to disambiguate based on context

**Approach:**

1. Retrieve all potential matches
2. Use LLM to analyze context (previous conversation, user intent)
3. Select most likely match based on context
4. Present match with explanation

**Example:**

```
User: "Find the Java expert named John"
→ Previous context: "I need a Java developer"
→ expertQuery("Find expert named John with Java experience")
→ LLM analyzes: Match "John Doe" who has Java experience
→ Present: "Found John Doe (Java expert) matching your requirements"
```

## Handling Name Variations

### Common Variations

1. **First Name Only**
    - "John" → Search for experts with first name "John"
    - Present all matches or ask for last name

2. **Last Name Only**
    - "Smith" → Search for experts with last name "Smith"
    - Present all matches or ask for first name

3. **Nicknames**
    - "Mike" → May match "Michael"
    - "Bob" → May match "Robert"
    - Use fuzzy matching or LLM interpretation

4. **Name Order**
    - "Doe, John" vs "John Doe"
    - Handle both formats

5. **Titles and Suffixes**
    - "Dr. John Doe" → Extract "John Doe"
    - "John Doe Jr." → Match "John Doe"
    - "John Doe, PhD" → Extract "John Doe"

### Name Parsing

When user provides a name, parse it:

1. **Extract components:**
    - First name
    - Last name
    - Middle name/initial
    - Titles (Dr., Mr., Ms., etc.)
    - Suffixes (Jr., Sr., III, etc.)

2. **Normalize:**
    - Remove titles and suffixes
    - Handle common abbreviations
    - Standardize capitalization

3. **Search:**
    - Try full name first
    - Try first + last name
    - Try last name only if multiple matches

## Best Practices

### 1. Always Verify Matches

- Present the matched expert's full profile
- Show key details (skills, projects) to confirm it's the right person
- Ask for confirmation if uncertain

### 2. Handle Multiple Matches

- List all matches with distinguishing details
- Ask user to clarify which expert they meant
- Use context to suggest the most likely match

### 3. Provide Helpful Error Messages

- If no match found: "No expert found named '[Name]'. Did you mean...?"
- Suggest similar names if available
- Offer to search by skills/technologies instead

### 4. Use Conversation Context

- Remember previously mentioned experts
- Use chat history to resolve ambiguous names
- Build on previous expert references

### 5. Progressive Disclosure

- Start with exact match
- Fall back to partial/fuzzy match if needed
- Use LLM assistance for complex cases

## Response Templates

### Template 1: Exact Match Found

```
Found expert **[Name]**:

**Profile:**
- ID: [Expert ID]
- Seniority: [Level]
- Skills: [Skills]
- Technologies: [Technologies]

**Recent Projects:**
- [Project 1]: [Role]
- [Project 2]: [Role]

[Link to profile if available]
```

### Template 2: Multiple Matches

```
Found [N] experts named "[Name]":

1. **[Full Name 1]** (ID: [ID])
   - Seniority: [Level]
   - Key Skills: [Skills]
   - [Distinguishing detail]

2. **[Full Name 2]** (ID: [ID])
   - Seniority: [Level]
   - Key Skills: [Skills]
   - [Distinguishing detail]

Which expert did you mean? You can also provide more details (e.g., "John Doe who works with Java").
```

### Template 3: No Match Found

```
No expert found named "[Name]".

**Suggestions:**
- Check the spelling
- Try searching by skills or technologies instead
- Did you mean: [Similar names if available]

**Alternative Search:**
I can help you find experts by:
- Skills or technologies
- Projects they worked on
- Domain expertise
```

### Template 4: Ambiguous Match (LLM-Assisted)

```
Found **[Name]** matching your query:

Based on our conversation about [context], I believe you're looking for:

**[Full Name]** (ID: [ID])
- [Relevant details matching context]

Is this the correct expert?
```

## Integration with Other Skills

### With Expert Matching

Combine name matching with expert search:

```
User: "Find John who knows Java"
→ expertQuery("Find expert named John with Java experience")
→ Present: Expert profile with Java-related projects highlighted
```

### With Project Discovery

Find experts by name and their projects:

```
User: "Who is John Doe and what projects did he work on?"
→ getExpertProfile(expertName: "John Doe")
→ getProjectExperts with expert's projects
→ Present: Profile + project details
```

## Common Scenarios

### Scenario 1: Full Name Provided

```
User: "Find John Doe"
→ Strategy: Exact match
→ Result: Single expert or multiple matches
```

### Scenario 2: First Name Only

```
User: "Find John"
→ Strategy: Partial match
→ Result: List all experts named John
```

### Scenario 3: Name with Typo

```
User: "Find Micheal Smith"
→ Strategy: Fuzzy match
→ Result: "Did you mean Michael Smith?"
```

### Scenario 4: Name with Context

```
User: "Find the Java expert named John"
→ Strategy: LLM-assisted match
→ Result: John Doe (Java expert) based on context
```

## References

- See `references/name-matching.st` for the name matching prompt template
- See `references/fuzzy-matching.md` for fuzzy matching algorithms
- See `references/name-variations.md` for common name variations
