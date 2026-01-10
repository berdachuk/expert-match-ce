package com.berdachuk.expertmatch.chat.domain;

import java.time.Instant;

/**
 * Chat entity record.
 */
public record Chat(
        String id,
        String userId,
        String name,
        boolean isDefault,
        Instant createdAt,
        Instant updatedAt,
        Instant lastActivityAt,
        int messageCount
) {
}
