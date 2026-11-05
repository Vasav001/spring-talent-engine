package com.vasav.springtalentengine.tenant;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public enum MemberRole {
    OWNER(EnumSet.allOf(Permission.class)),
    RECRUITER(EnumSet.of(Permission.JOB_WRITE, Permission.BATCH_UPLOAD, Permission.RESULT_OVERRIDE, Permission.RESULT_READ)),
    VIEWER(EnumSet.of(Permission.RESULT_READ));

    private final Set<Permission> permissions;

    MemberRole(Set<Permission> permissions) {
        this.permissions = Collections.unmodifiableSet(permissions);
    }

    public Set<Permission> permissions() {
        return permissions;
    }
}
