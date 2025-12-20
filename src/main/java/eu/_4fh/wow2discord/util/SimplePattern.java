package eu._4fh.wow2discord.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SimplePattern {
    private static final Pattern splitPattern = Pattern.compile("([^*+?]*)([*+?]?)");

    private final Pattern pattern;

    public SimplePattern(String glob) {
        Matcher matcher = splitPattern.matcher(glob);
        StringBuilder regex = new StringBuilder();
        while (matcher.find()) {
            String textPart = matcher.group(1);
            String patternPart = matcher.group(2);
            if (!textPart.isEmpty()) {
                regex.append(Pattern.quote(textPart));
            }
            if (patternPart.equals("?")) {
                regex.append(".");
            } else if (!patternPart.isEmpty()) {
                regex.append(".").append(patternPart);
            }
        }

        pattern = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    public boolean doesMatch(String str) {
        return pattern.matcher(str).matches();
    }
}
