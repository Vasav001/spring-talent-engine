package com.vasav.springtalentengine.tenant;

import com.vasav.springtalentengine.shared.CurrentUser;
import com.vasav.springtalentengine.tenant.TenantService.Membership;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class TenantController {

    private final TenantService tenantService;

    @GetMapping("/me")
    public MeResponse me() {
        String email = CurrentUser.requireEmail();
        return new MeResponse(email, tenantService.membershipsOf(email));
    }

    @PostMapping("/tenants")
    public ResponseEntity<Membership> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        Membership membership = tenantService.createTenant(request.name(), CurrentUser.requireEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(membership);
    }

    @GetMapping("/tenants/members")
    @PreAuthorize("hasAuthority('MEMBER_MANAGE')")
    public Page<MemberResponse> members(@PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return tenantService.members(pageable).map(MemberResponse::from);
    }

    @PostMapping("/tenants/members")
    @PreAuthorize("hasAuthority('MEMBER_MANAGE')")
    public ResponseEntity<MemberResponse> addMember(@Valid @RequestBody AddMemberRequest request) {
        TenantMember member = tenantService.addMember(request.email(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(MemberResponse.from(member));
    }

    @PutMapping("/tenants/members/{memberId}/role")
    @PreAuthorize("hasAuthority('MEMBER_MANAGE')")
    public MemberResponse changeRole(@PathVariable UUID memberId, @Valid @RequestBody ChangeRoleRequest request) {
        return MemberResponse.from(tenantService.changeRole(memberId, request.role(), CurrentUser.requireEmail()));
    }

    record MeResponse(String email, List<Membership> tenants) {
    }

    record CreateTenantRequest(@NotBlank @Size(max = 100) String name) {
    }

    record AddMemberRequest(@NotBlank @Email String email, @NotNull MemberRole role) {
    }

    record ChangeRoleRequest(@NotNull MemberRole role) {
    }

    record MemberResponse(UUID id, String email, MemberRole role, Instant createdAt) {

        static MemberResponse from(TenantMember member) {
            return new MemberResponse(member.getId(), member.getEmail(), member.getRole(), member.getCreatedAt());
        }
    }
}
