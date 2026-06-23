package com.evalsystem.common.context;

import java.util.Set;

public record CurrentUser(String userId, String displayName, Set<String> memberSpaceId) {
}
