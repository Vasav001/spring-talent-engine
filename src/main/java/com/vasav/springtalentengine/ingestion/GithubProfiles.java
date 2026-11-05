package com.vasav.springtalentengine.ingestion;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GithubProfiles {

    private static final Pattern PROFILE = Pattern.compile("github\\.com/([A-Za-z0-9][A-Za-z0-9-]{0,38})", Pattern.CASE_INSENSITIVE);

    private GithubProfiles() {
    }

    static Optional<String> username(String text) {
        Matcher matcher = PROFILE.matcher(text);
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
