package com.berdachuk.expertmatch.llm.tools;

import com.berdachuk.expertmatch.core.security.HeaderBasedUserContext;
import com.berdachuk.expertmatch.employee.repository.EmployeeRepository;
import com.berdachuk.expertmatch.employee.service.ExpertEnrichmentService;
import com.berdachuk.expertmatch.llm.service.AnswerGenerationService;
import com.berdachuk.expertmatch.query.domain.QueryParser;
import com.berdachuk.expertmatch.query.domain.QueryRequest;
import com.berdachuk.expertmatch.query.domain.QueryResponse;
import com.berdachuk.expertmatch.query.service.ExpertContextHolder;
import com.berdachuk.expertmatch.query.service.QueryService;
import com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService;
import com.berdachuk.expertmatch.workexperience.repository.WorkExperienceRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ExpertMatchTools.
 */
@ExtendWith(MockitoExtension.class)
class ExpertMatchToolsTest {

    @Mock
    private QueryService queryService;

    @Mock
    private HybridRetrievalService retrievalService;

    @Mock
    private ExpertEnrichmentService enrichmentService;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private WorkExperienceRepository workExperienceRepository;

    @Mock
    private QueryParser queryParser;

    @Mock
    private HeaderBasedUserContext userContext;

    private ExpertMatchTools expertMatchTools;

    @BeforeEach
    void setUp() {
        ExpertContextHolder.clear(); // Ensure clean state
        expertMatchTools = new ExpertMatchTools(
                queryService,
                retrievalService,
                enrichmentService,
                employeeRepository,
                workExperienceRepository,
                queryParser,
                userContext
        );
    }

    @AfterEach
    void tearDown() {
        ExpertContextHolder.clear(); // Clean up after each test
    }

    @Test
    void testGetRetrievedExperts() {
        // Arrange
        List<AnswerGenerationService.ExpertContext> contexts = List.of(
                new AnswerGenerationService.ExpertContext(
                        "expert1",
                        "John Doe",
                        "john@example.com",
                        "A4",
                        List.of("Java", "Spring"),
                        List.of("Project1"),
                        Map.of("matchScore", 0.9, "relevanceScore", 0.85)
                ),
                new AnswerGenerationService.ExpertContext(
                        "expert2",
                        "Jane Smith",
                        "jane@example.com",
                        "A3",
                        List.of("Python", "Django"),
                        List.of("Project2"),
                        Map.of("matchScore", 0.8, "relevanceScore", 0.75)
                )
        );
        ExpertContextHolder.set(contexts);

        // Act
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = expertMatchTools.getRetrievedExperts();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("expert1", result.get(0).id());
        assertEquals("John Doe", result.get(0).name());
        assertEquals("expert2", result.get(1).id());
        assertEquals("Jane Smith", result.get(1).name());
    }

    @Test
    void testGetRetrievedExpertsEmpty() {
        // Arrange - no contexts set
        ExpertContextHolder.clear();

        // Act
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = expertMatchTools.getRetrievedExperts();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testExpertQuery() {
        // Arrange
        String query = "Find experts in Java and Spring Boot";
        String chatId = "507f1f77bcf86cd799439011";
        String userId = "user-123";

        QueryResponse expectedResponse = new QueryResponse(
                "Found 5 experts",
                List.of(),
                List.of(),
                List.of(),
                0.85,
                "query-id",
                chatId,
                "message-id",
                1234L,
                new com.berdachuk.expertmatch.query.domain.QueryResponse.MatchSummary(5, 2, 2, 1),
                null  // executionTrace
        );

        when(userContext.getUserIdOrAnonymous()).thenReturn(userId);
        when(queryService.processQuery(any(QueryRequest.class), eq(chatId), eq(userId)))
                .thenReturn(expectedResponse);

        // Act
        QueryResponse result = expertMatchTools.expertQuery(query, chatId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        verify(queryService).processQuery(any(QueryRequest.class), eq(chatId), eq(userId));
    }

    @Test
    void testExpertQueryPreventsRecursion() {
        // Arrange - simulate that we're already processing a query
        List<AnswerGenerationService.ExpertContext> contexts = List.of(
                new AnswerGenerationService.ExpertContext(
                        "expert1",
                        "John Doe",
                        null,
                        null,
                        List.of(),
                        List.of(),
                        Map.of()
                )
        );
        ExpertContextHolder.set(contexts);

        String query = "Find experts";
        String chatId = "chat-123";

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            expertMatchTools.expertQuery(query, chatId);
        });

        assertTrue(exception.getMessage().contains("Cannot call expertQuery tool while already processing a query"));
        verifyNoInteractions(queryService);
    }

    @Test
    void testExpertQueryWithoutChatId() {
        // Arrange
        String query = "Find experts in Java";
        String userId = "user-123";

        QueryResponse expectedResponse = new QueryResponse(
                "Found 3 experts",
                List.of(),
                List.of(),
                List.of(),
                0.80,
                "query-id",
                null,
                "message-id",
                1000L,
                new com.berdachuk.expertmatch.query.domain.QueryResponse.MatchSummary(3, 1, 1, 1),
                null  // executionTrace
        );

        when(userContext.getUserIdOrAnonymous()).thenReturn(userId);
        when(queryService.processQuery(any(QueryRequest.class), isNull(), eq(userId)))
                .thenReturn(expectedResponse);

        // Act
        QueryResponse result = expertMatchTools.expertQuery(query, null);

        // Assert
        assertNotNull(result);
        verify(queryService).processQuery(any(QueryRequest.class), isNull(), eq(userId));
    }

    @Test
    void testFindExperts() {
        // Arrange
        List<String> skills = List.of("Java", "Spring Boot");
        String seniority = "A4";
        List<String> technologies = List.of("AWS", "Docker");
        String domain = "FinTech";

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery = new com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery(
                "Skills: Java, Spring Boot. Technologies: AWS, Docker. Seniority: A4. Domain: FinTech.",
                skills,
                List.of(seniority),
                null,
                "expert_search",
                technologies
        );

        com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult retrievalResult =
                new com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult(
                        List.of("emp1", "emp2"),
                        Map.of("emp1", 0.9, "emp2", 0.8)
                );

        com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch expert1 = createMockExpert("emp1", "John Doe");
        com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch expert2 = createMockExpert("emp2", "Jane Smith");

        when(queryParser.parse(anyString())).thenReturn(parsedQuery);
        when(retrievalService.retrieve(any(QueryRequest.class), eq(parsedQuery)))
                .thenReturn(retrievalResult);
        when(enrichmentService.enrichExperts(eq(retrievalResult), eq(parsedQuery)))
                .thenReturn(List.of(expert1, expert2));

        // Act
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = expertMatchTools.findExperts(
                skills, seniority, technologies, domain
        );

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(queryParser).parse(anyString());
        verify(retrievalService).retrieve(any(QueryRequest.class), eq(parsedQuery));
        verify(enrichmentService).enrichExperts(eq(retrievalResult), eq(parsedQuery));
    }

    @Test
    void testFindExpertsWithEmptyCriteria() {
        // Act
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = expertMatchTools.findExperts(
                List.of(), null, List.of(), null
        );

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(queryParser, retrievalService, enrichmentService);
    }

    @Test
    void testGetExpertProfileById() {
        // Arrange
        String expertId = "8760000000000420950";
        com.berdachuk.expertmatch.employee.domain.Employee employee = new com.berdachuk.expertmatch.employee.domain.Employee(
                expertId,
                "John Doe",
                "john.doe@example.com",
                "A4",
                "C1",
                "Available"
        );

        com.berdachuk.expertmatch.workexperience.domain.WorkExperience workExp = new com.berdachuk.expertmatch.workexperience.domain.WorkExperience(
                "exp1",
                expertId,
                "proj1",
                "cust1",
                "Project Alpha",
                "Customer A",
                "Finance",
                "Senior Developer",
                java.time.Instant.now().minusSeconds(86400 * 365),
                java.time.Instant.now(),
                "Built microservices",
                "Developed APIs",
                List.of("Java", "Spring Boot")
        );

        when(employeeRepository.findById(expertId)).thenReturn(Optional.of(employee));
        when(workExperienceRepository.findByEmployeeId(expertId)).thenReturn(List.of(workExp));

        // Act
        com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch result = expertMatchTools.getExpertProfile(expertId, null);

        // Assert
        assertNotNull(result);
        assertEquals(expertId, result.id());
        assertEquals("John Doe", result.name());
        assertEquals("john.doe@example.com", result.email());
        verify(employeeRepository).findById(expertId);
        verify(workExperienceRepository).findByEmployeeId(expertId);
    }

    @Test
    void testGetExpertProfileNotFound() {
        // Arrange
        String expertId = "nonexistent";
        when(employeeRepository.findById(expertId)).thenReturn(Optional.empty());

        // Act
        com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch result = expertMatchTools.getExpertProfile(expertId, null);

        // Assert
        assertNull(result);
        verify(employeeRepository).findById(expertId);
        verifyNoInteractions(workExperienceRepository);
    }

    @Test
    void testMatchProjectRequirements() {
        // Arrange
        Map<String, Object> requirements = Map.of(
                "skills", List.of("Java", "Spring Boot"),
                "technical_stack", List.of("AWS", "Docker"),
                "seniority", "A4",
                "responsibilities", List.of("API Development", "Code Review")
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery = new com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery(
                "Technologies: Java, Spring Boot, AWS, Docker. Seniority: A4. Responsibilities: API Development, Code Review.",
                List.of("Java", "Spring Boot"),
                List.of("A4"),
                null,
                "team_formation",
                List.of("AWS", "Docker")
        );

        com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult retrievalResult =
                new com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult(
                        List.of("emp1", "emp2", "emp3"),
                        Map.of("emp1", 0.95, "emp2", 0.85, "emp3", 0.75)
                );

        com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch expert1 = createMockExpert("emp1", "Expert 1");
        com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch expert2 = createMockExpert("emp2", "Expert 2");
        com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch expert3 = createMockExpert("emp3", "Expert 3");

        when(queryParser.parse(anyString())).thenReturn(parsedQuery);
        when(retrievalService.retrieve(any(QueryRequest.class), eq(parsedQuery)))
                .thenReturn(retrievalResult);
        when(enrichmentService.enrichExperts(eq(retrievalResult), eq(parsedQuery)))
                .thenReturn(List.of(expert1, expert2, expert3));

        // Act
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = expertMatchTools.matchProjectRequirements(requirements);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.size());
        verify(queryParser).parse(anyString());
        verify(retrievalService).retrieve(any(QueryRequest.class), eq(parsedQuery));
        verify(enrichmentService).enrichExperts(eq(retrievalResult), eq(parsedQuery));
    }

    @Test
    void testMatchProjectRequirementsEmpty() {
        // Arrange
        Map<String, Object> requirements = Map.of();

        // Act
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = expertMatchTools.matchProjectRequirements(requirements);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(queryParser, retrievalService, enrichmentService);
    }

    @Test
    void testGetProjectExperts() {
        // Arrange
        String projectName = "Project Alpha";
        String expertId1 = "emp1";
        String expertId2 = "emp2";

        com.berdachuk.expertmatch.workexperience.domain.WorkExperience we1 = new com.berdachuk.expertmatch.workexperience.domain.WorkExperience(
                "exp1", expertId1, "proj1", "cust1", projectName, "Customer A",
                "Finance", "Developer", java.time.Instant.now().minusSeconds(86400 * 180),
                java.time.Instant.now(), "Summary", "Responsibilities", List.of("Java")
        );
        com.berdachuk.expertmatch.workexperience.domain.WorkExperience we2 = new com.berdachuk.expertmatch.workexperience.domain.WorkExperience(
                "exp2", expertId2, "proj1", "cust1", projectName, "Customer A",
                "Finance", "Lead", java.time.Instant.now().minusSeconds(86400 * 365),
                java.time.Instant.now(), "Summary", "Responsibilities", List.of("Java", "Spring")
        );

        com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery parsedQuery = new com.berdachuk.expertmatch.query.domain.QueryParser.ParsedQuery(
                "Experts who worked on project: " + projectName,
                List.of(),
                List.of(),
                null,
                "expert_search",
                List.of()
        );

        com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult retrievalResult =
                new com.berdachuk.expertmatch.retrieval.service.HybridRetrievalService.RetrievalResult(
                        List.of(expertId1, expertId2),
                        Map.of(expertId1, 1.0, expertId2, 1.0)
                );

        com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch expert1 = createMockExpert(expertId1, "John Doe");
        com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch expert2 = createMockExpert(expertId2, "Jane Smith");

        when(queryParser.parse(anyString())).thenReturn(parsedQuery);
        when(retrievalService.retrieve(any(QueryRequest.class), eq(parsedQuery)))
                .thenReturn(retrievalResult);
        when(enrichmentService.enrichExperts(eq(retrievalResult), eq(parsedQuery)))
                .thenReturn(List.of(expert1, expert2));
        when(workExperienceRepository.findByEmployeeIds(List.of(expertId1, expertId2)))
                .thenReturn(Map.of(
                        expertId1, List.of(we1),
                        expertId2, List.of(we2)
                ));

        // Act
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = expertMatchTools.getProjectExperts(null, projectName);

        // Assert
        assertNotNull(result);
        // Should filter to only experts who worked on the project
        assertTrue(result.size() <= 2);
        verify(queryParser).parse(anyString());
        verify(retrievalService).retrieve(any(QueryRequest.class), eq(parsedQuery));
    }

    @Test
    void testGetProjectExpertsEmptyCriteria() {
        // Act
        List<com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch> result = expertMatchTools.getProjectExperts(null, null);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(queryParser, retrievalService);
    }

    // Helper method to create mock expert
    private com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch createMockExpert(String id, String name) {
        return new com.berdachuk.expertmatch.query.domain.QueryResponse.ExpertMatch(
                id,
                name,
                name.toLowerCase().replace(" ", ".") + "@example.com",
                "A4",
                new com.berdachuk.expertmatch.query.domain.QueryResponse.LanguageProficiency("C1"),
                null,
                null,
                List.of(),
                new com.berdachuk.expertmatch.query.domain.QueryResponse.Experience(false, false, false, false, false),
                0.9,
                "Available"
        );
    }
}

