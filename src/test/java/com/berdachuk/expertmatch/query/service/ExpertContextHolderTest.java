package com.berdachuk.expertmatch.query.service;

import com.berdachuk.expertmatch.core.service.ExpertContextHolder;
import com.berdachuk.expertmatch.llm.service.AnswerGenerationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExpertContextHolderTest {

    @AfterEach
    void tearDown() {
        ExpertContextHolder.clear();
    }

    @Test
    void testSetAndGet() {
        List<AnswerGenerationService.ExpertContext> contexts = List.of(
                new AnswerGenerationService.ExpertContext(
                        "expert1",
                        "John Doe",
                        "john@example.com",
                        "A4",
                        List.of("Java", "Spring"),
                        List.of("Project1"),
                        Map.of("matchScore", 0.9)
                )
        );

        ExpertContextHolder.set(contexts);
        List<AnswerGenerationService.ExpertContext> retrieved = ExpertContextHolder.get();

        assertThat(retrieved).isNotNull();
        assertThat(retrieved).hasSize(1);
        assertThat(retrieved.get(0).expertId()).isEqualTo("expert1");
        assertThat(retrieved.get(0).name()).isEqualTo("John Doe");
    }

    @Test
    void testClear() {
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
        assertThat(ExpertContextHolder.get()).isNotNull();

        ExpertContextHolder.clear();
        assertThat(ExpertContextHolder.get()).isNull();
    }

    @Test
    void testHasContexts() {
        assertThat(ExpertContextHolder.hasContexts()).isFalse();

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
        assertThat(ExpertContextHolder.hasContexts()).isTrue();

        ExpertContextHolder.clear();
        assertThat(ExpertContextHolder.hasContexts()).isFalse();
    }

    @Test
    void testHasContextsWithEmptyList() {
        ExpertContextHolder.set(List.of());
        assertThat(ExpertContextHolder.hasContexts()).isFalse();
    }

    @Test
    void testThreadLocalIsolation() throws InterruptedException {
        List<AnswerGenerationService.ExpertContext> contexts1 = List.of(
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

        ExpertContextHolder.set(contexts1);

        Thread thread = new Thread(() -> {
            List<AnswerGenerationService.ExpertContext> contexts2 = List.of(
                    new AnswerGenerationService.ExpertContext(
                            "expert2",
                            "Jane Smith",
                            null,
                            null,
                            List.of(),
                            List.of(),
                            Map.of()
                    )
            );
            ExpertContextHolder.set(contexts2);
            assertThat(ExpertContextHolder.get()).hasSize(1);
            assertThat(ExpertContextHolder.get().get(0).expertId()).isEqualTo("expert2");
        });

        thread.start();
        thread.join();

        // Original thread should still have its own context
        assertThat(ExpertContextHolder.get()).hasSize(1);
        assertThat(ExpertContextHolder.get().get(0).expertId()).isEqualTo("expert1");
    }
}
