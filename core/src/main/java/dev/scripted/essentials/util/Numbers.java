package dev.scripted.essentials.util;

import java.util.OptionalDouble;
import java.util.OptionalInt;

/** Lenient parsing for command arguments. */
public final class Numbers {

    private Numbers() {
    }

    public static OptionalInt parseInt(String raw) {
        try {
            return OptionalInt.of(Integer.parseInt(raw.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            return OptionalInt.empty();
        }
    }

    public static OptionalDouble parseDouble(String raw) {
        try {
            return OptionalDouble.of(Double.parseDouble(raw.trim()));
        } catch (NumberFormatException | NullPointerException e) {
            return OptionalDouble.empty();
        }
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Parses a duration such as {@code 30s}, {@code 10m}, {@code 2h} or {@code 7d} into seconds.
     * A bare number is read as seconds.
     */
    public static OptionalInt parseDuration(String raw) {
        if (raw == null || raw.isBlank()) {
            return OptionalInt.empty();
        }
        String value = raw.trim().toLowerCase(java.util.Locale.ROOT);
        char unit = value.charAt(value.length() - 1);
        int multiplier = switch (unit) {
            case 's' -> 1;
            case 'm' -> 60;
            case 'h' -> 3600;
            case 'd' -> 86400;
            default -> 0;
        };
        if (multiplier == 0) {
            return parseInt(value);
        }
        OptionalInt amount = parseInt(value.substring(0, value.length() - 1));
        return amount.isPresent() ? OptionalInt.of(amount.getAsInt() * multiplier) : OptionalInt.empty();
    }

    /** Formats a second count as {@code 1d 2h 3m 4s}, dropping empty leading units. */
    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "0s";
        }
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        StringBuilder out = new StringBuilder();
        if (days > 0) {
            out.append(days).append("d ");
        }
        if (hours > 0) {
            out.append(hours).append("h ");
        }
        if (minutes > 0) {
            out.append(minutes).append("m ");
        }
        if (secs > 0) {
            out.append(secs).append('s');
        }
        return out.toString().trim();
    }
}
