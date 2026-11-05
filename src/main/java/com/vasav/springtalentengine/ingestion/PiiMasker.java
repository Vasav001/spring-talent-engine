package com.vasav.springtalentengine.ingestion;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class PiiMasker {

    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern PHONE_CANDIDATE = Pattern.compile("\\+?\\(?\\d[\\d ().-]{7,}\\d");
    private static final int MIN_PHONE_DIGITS = 10;

    private PiiMasker() {
    }

    static String mask(String text) {
        String withoutEmails = EMAIL.matcher(text).replaceAll("[EMAIL]");
        return PHONE_CANDIDATE.matcher(withoutEmails).replaceAll(match -> digitCount(match.group()) >= MIN_PHONE_DIGITS
                ? "[PHONE]"
                : Matcher.quoteReplacement(match.group()));
    }

    private static long digitCount(String value) {
        return value.chars().filter(Character::isDigit).count();
    }
}
