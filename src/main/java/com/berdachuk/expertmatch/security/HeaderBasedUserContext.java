package com.berdachuk.expertmatch.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service to extract user information from HTTP headers populated by Spring Gateway.
 * Spring Gateway handles authentication and authorization, then populates user information
 * in headers that this service extracts.
 */
@Component
public class HeaderBasedUserContext {

    private static final String DEFAULT_USER_ID_HEADER = "X-User-Id";
    private static final String DEFAULT_USER_ROLES_HEADER = "X-User-Roles";
    private static final String DEFAULT_USER_EMAIL_HEADER = "X-User-Email";

    @Value("${expertmatch.security.headers.user-id:X-User-Id}")
    private String userIdHeader;

    @Value("${expertmatch.security.headers.user-roles:X-User-Roles}")
    private String userRolesHeader;

    @Value("${expertmatch.security.headers.user-email:X-User-Email}")
    private String userEmailHeader;

    /**
     * Get the current HTTP request.
     */
    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return attributes.getRequest();
    }

    /**
     * Extract user ID from X-User-Id header.
     * Returns null if header is missing.
     *
     * @return User ID or null if not present
     */
    public String getUserId() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader(userIdHeader);
    }

    /**
     * Extract user ID from X-User-Id header, returns anonymous user if not present.
     * Note: This method explicitly provides a default value, not a fallback.
     *
     * @return User ID or "anonymous-user" if not present
     */
    public String getUserIdOrAnonymous() {
        String userId = getUserId();
        return userId != null && !userId.isBlank() ? userId : "anonymous-user";
    }

    /**
     * Extract user roles from X-User-Roles header (comma-separated).
     * Returns empty list if header is missing.
     *
     * @return List of roles (may be empty)
     */
    public List<String> getUserRoles() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return Collections.emptyList();
        }

        String rolesHeader = request.getHeader(userRolesHeader);
        if (rolesHeader == null || rolesHeader.isBlank()) {
            return Collections.emptyList();
        }

        // Parse comma-separated roles
        List<String> roles = new ArrayList<>();
        String[] roleArray = rolesHeader.split(",");
        for (String role : roleArray) {
            String trimmedRole = role.trim();
            if (!trimmedRole.isEmpty()) {
                roles.add(trimmedRole);
            }
        }
        return roles;
    }

    /**
     * Check if user has a specific role.
     *
     * @param role Role to check (e.g., "ROLE_USER", "ROLE_ADMIN")
     * @return true if user has the role
     */
    public boolean hasRole(String role) {
        List<String> roles = getUserRoles();
        return roles.contains(role);
    }

    /**
     * Extract user email from X-User-Email header.
     * Returns null if header is missing.
     *
     * @return User email or null if not present
     */
    public String getUserEmail() {
        HttpServletRequest request = getRequest();
        if (request == null) {
            return null;
        }
        return request.getHeader(userEmailHeader);
    }

    /**
     * Check if user information is present in headers.
     * Useful for validation.
     *
     * @return true if at least user ID is present
     */
    public boolean isUserPresent() {
        return getUserId() != null && !getUserId().isBlank();
    }
}

