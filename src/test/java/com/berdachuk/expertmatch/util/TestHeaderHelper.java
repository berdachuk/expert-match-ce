package com.berdachuk.expertmatch.util;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

/**
 * Utility class for setting HTTP headers in test requests.
 * Headers are populated by Spring Gateway in production.
 * <p>
 * Usage in tests:
 * MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post("/api/v1/query");
 * TestHeaderHelper.setUserHeaders(request, "user-123", List.of("ROLE_USER"));
 */
public class TestHeaderHelper {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    /**
     * Set user headers on a MockHttpServletRequestBuilder.
     *
     * @param requestBuilder The request builder to add headers to
     * @param userId         User ID (required)
     * @param roles          List of roles (optional, can be null or empty)
     * @return The request builder for method chaining
     */
    public static MockHttpServletRequestBuilder setUserHeaders(
            MockHttpServletRequestBuilder requestBuilder,
            String userId,
            List<String> roles) {
        return setUserHeaders(requestBuilder, userId, roles, null);
    }

    /**
     * Set user headers on a MockHttpServletRequestBuilder.
     *
     * @param requestBuilder The request builder to add headers to
     * @param userId         User ID (required)
     * @param roles          List of roles (optional, can be null or empty)
     * @param email          User email (optional, can be null)
     * @return The request builder for method chaining
     */
    public static MockHttpServletRequestBuilder setUserHeaders(
            MockHttpServletRequestBuilder requestBuilder,
            String userId,
            List<String> roles,
            String email) {
        if (userId != null && !userId.isBlank()) {
            requestBuilder.header(USER_ID_HEADER, userId);
        }

        if (roles != null && !roles.isEmpty()) {
            // Join roles with comma
            String rolesHeader = String.join(",", roles);
            requestBuilder.header(USER_ROLES_HEADER, rolesHeader);
        }

        if (email != null && !email.isBlank()) {
            requestBuilder.header(USER_EMAIL_HEADER, email);
        }

        return requestBuilder;
    }

    /**
     * Set user headers with a single role.
     *
     * @param requestBuilder The request builder to add headers to
     * @param userId         User ID (required)
     * @param role           Single role (optional, can be null)
     * @return The request builder for method chaining
     */
    public static MockHttpServletRequestBuilder setUserHeaders(
            MockHttpServletRequestBuilder requestBuilder,
            String userId,
            String role) {
        List<String> roles = role != null ? List.of(role) : null;
        return setUserHeaders(requestBuilder, userId, roles, null);
    }
}

