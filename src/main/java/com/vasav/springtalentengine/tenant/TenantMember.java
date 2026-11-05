package com.vasav.springtalentengine.tenant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tenant_members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TenantMember {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID tenantId;

    private String email;

    @Enumerated(EnumType.STRING)
    private MemberRole role;

    private Instant createdAt;

    TenantMember(UUID tenantId, String email, MemberRole role) {
        this.tenantId = tenantId;
        this.email = email;
        this.role = role;
        this.createdAt = Instant.now();
    }

    void changeRole(MemberRole role) {
        this.role = role;
    }
}
