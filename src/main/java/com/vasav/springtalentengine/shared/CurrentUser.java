package com.vasav.springtalentengine.shared;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static Optional<String> email() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.ofNullable(authentication.getName())
                .filter(name -> !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT));
    }

    public static boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(granted -> authority.equals(granted.getAuthority()));
    }

    public static String requireEmail() {
        return email().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token has no email claim"));
    }
}
