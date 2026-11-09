package com.vasav.springtalentengine.tenant;

import com.vasav.springtalentengine.shared.TenantContext;
import com.vasav.springtalentengine.shared.TenantSession;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantService {

    private final TenantRepository tenantRepository;
    private final TenantMemberRepository memberRepository;
    private final TenantSession tenantSession;
    private final ApplicationEventPublisher events;

    public Optional<MemberRole> findRole(UUID tenantId, String email) {
        return memberRepository.findByTenantIdAndEmail(tenantId, email).map(TenantMember::getRole);
    }

    List<Membership> membershipsOf(String email) {
        List<TenantMember> memberships = memberRepository.findByEmail(email);
        Map<UUID, Tenant> tenants = tenantRepository.findAllById(memberships.stream().map(TenantMember::getTenantId).toList())
                .stream()
                .collect(Collectors.toMap(Tenant::getId, Function.identity()));
        return memberships.stream()
                .map(member -> new Membership(member.getTenantId(), tenants.get(member.getTenantId()).getName(), member.getRole()))
                .toList();
    }

    @Transactional
    Membership createTenant(String name, String ownerEmail) {
        Tenant tenant = tenantRepository.save(new Tenant(name));
        tenantSession.bindCurrentTransaction(tenant.getId());
        memberRepository.save(new TenantMember(tenant.getId(), ownerEmail, MemberRole.OWNER));
        events.publishEvent(new TenantCreated(tenant.getId()));
        return new Membership(tenant.getId(), tenant.getName(), MemberRole.OWNER);
    }

    Page<TenantMember> members(Pageable pageable) {
        return memberRepository.findByTenantId(TenantContext.require(), pageable);
    }

    @Transactional
    TenantMember addMember(String email, MemberRole role) {
        UUID tenantId = TenantContext.require();
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        if (memberRepository.existsByTenantIdAndEmail(tenantId, normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already a member of this tenant");
        }
        return memberRepository.save(new TenantMember(tenantId, normalizedEmail, role));
    }

    @Transactional
    TenantMember changeRole(UUID memberId, MemberRole role, String actingEmail) {
        TenantMember member = memberRepository.findById(memberId)
                .filter(found -> found.getTenantId().equals(TenantContext.require()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Member not found"));
        if (member.getEmail().equals(actingEmail)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot change your own role");
        }
        member.changeRole(role);
        return member;
    }

    record Membership(UUID tenantId, String tenantName, MemberRole role) {
    }
}
