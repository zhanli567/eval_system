package com.agentnexus.backend.common.security;

import java.util.Set;

public record CurrentUser(String userId, String displayName, Set<String> memberSpaceId) {
}
