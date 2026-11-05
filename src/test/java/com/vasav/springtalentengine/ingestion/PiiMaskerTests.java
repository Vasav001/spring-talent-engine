package com.vasav.springtalentengine.ingestion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PiiMaskerTests {

    @Test
    void masksEmailsAndPhoneNumbers() {
        String masked = PiiMasker.mask("Reach me at jane.doe@mail.com or +91 98765 43210, (555) 123-4567.");

        assertThat(masked).isEqualTo("Reach me at [EMAIL] or [PHONE], [PHONE].");
    }

    @Test
    void keepsDatesAndShortNumbers() {
        String text = "Backend Engineer 2019 - 2023, team of 12, 3.5 years with Java 21";

        assertThat(PiiMasker.mask(text)).isEqualTo(text);
    }

    @Test
    void findsGithubUsername() {
        assertThat(GithubProfiles.username("Links: https://github.com/jane-doe/portfolio")).contains("jane-doe");
        assertThat(GithubProfiles.username("No links here")).isEmpty();
    }
}
