package com.vasav.springtalentengine.tenant;

import com.vasav.springtalentengine.TestcontainersConfiguration;
import com.vasav.springtalentengine.shared.TenantContext;
import com.vasav.springtalentengine.tenant.TenantService.Membership;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@ActiveProfiles("test")
class TenantIsolationTests {

    @Autowired
    TenantService tenantService;

    @Autowired
    TenantMemberRepository memberRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearContext() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void appConnectsAsNonOwnerRole() {
        assertThat(jdbcTemplate.queryForObject("SELECT current_user", String.class)).isEqualTo("talent_app");
    }

    @Test
    void tenantSeesOnlyItsOwnMembers() {
        UUID tenantA = createTenant("alice@a.test", "Tenant A");
        createTenant("bob@b.test", "Tenant B");

        actAs("alice@a.test", tenantA);

        assertThat(memberRepository.findAll()).extracting(TenantMember::getTenantId).containsOnly(tenantA);
    }

    @Test
    void cannotInsertIntoAnotherTenant() {
        UUID tenantA = createTenant("erin@e.test", "Tenant E");
        UUID tenantB = createTenant("frank@f.test", "Tenant F");

        actAs("erin@e.test", tenantA);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO tenant_members (id, tenant_id, email, role, created_at) VALUES (gen_random_uuid(), ?, ?, 'OWNER', now())",
                tenantB, "erin@e.test"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("row-level security");
    }

    @Test
    void userSeesOwnMembershipsWithoutSelectingTenant() {
        UUID first = createTenant("carol@c.test", "Carol One");
        UUID second = createTenant("carol@c.test", "Carol Two");
        createTenant("dave@d.test", "Dave");

        actAs("carol@c.test", null);

        assertThat(tenantService.membershipsOf("carol@c.test"))
                .extracting(Membership::tenantId)
                .containsExactlyInAnyOrder(first, second);
    }

    private UUID createTenant(String ownerEmail, String name) {
        actAs(ownerEmail, null);
        UUID tenantId = tenantService.createTenant(name, ownerEmail).tenantId();
        clearContext();
        return tenantId;
    }

    private void actAs(String email, UUID tenantId) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(email, null, List.of()));
        if (tenantId != null) {
            TenantContext.set(tenantId);
        }
    }
}
