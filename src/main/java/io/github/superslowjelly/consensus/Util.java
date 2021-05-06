package io.github.superslowjelly.consensus;

import java.time.Duration;
import java.util.regex.Pattern;

public class Util {

    private final static Pattern DURATION_TO_STRING_PATTERN = Pattern.compile("[PT]");

    public static String durationToString(Duration duration) {
        return DURATION_TO_STRING_PATTERN.matcher(duration.toString()).replaceAll("");
    }

}
