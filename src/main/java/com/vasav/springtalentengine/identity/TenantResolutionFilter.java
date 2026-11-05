package com.vasav.springtalentengine.identity;

import com.vasav.springtalentengine.shared.CurrentUser;
import com.vasav.springtalentengine.shared.TenantContext;
import com.vasav.springtalentengine.tenant.MemberRole;
import com.vasav.springtalentengine.tenant.TenantService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
class TenantResolutionFilter extends OncePerRequestFilter {

    static final String TENANT_HEADER = "X-Tenant-Id";

    private final TenantService tenantService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(TENANT_HEADER);
        if (header == null || !(SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken authentication)) {
            chain.doFilter(request, response);
            return;
        }

        Optional<UUID> tenantId = parse(header);
        if (tenantId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid X-Tenant-Id header");
            return;
        }

        Optional<String> email = CurrentUser.email();
        Optional<MemberRole> role = email.flatMap(value -> tenantService.findRole(tenantId.get(), value));
        if (role.isEmpty()) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Not a member of this tenant");
            return;
        }

        TenantContext.set(tenantId.get());
        SecurityContextHolder.getContext().setAuthentication(withPermissions(authentication, role.get()));
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private Optional<UUID> parse(String header) {
        try {
            return Optional.of(UUID.fromString(header));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    private JwtAuthenticationToken withPermissions(JwtAuthenticationToken authentication, MemberRole role) {
        List<GrantedAuthority> authorities = new ArrayList<>(authentication.getAuthorities());
        role.permissions().forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.name())));
        return new JwtAuthenticationToken(authentication.getToken(), authorities, authentication.getName());
    }
}
