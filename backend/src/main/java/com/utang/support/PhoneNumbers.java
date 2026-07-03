package com.utang.support;

/**
 * Normalizes phone numbers to E.164 format, defaulting to the Philippines (+63).
 * Twilio requires E.164 (e.g. {@code +639171234567}) for both delivery and lookup.
 */
public final class PhoneNumbers {

    private PhoneNumbers() {
    }

    /**
     * Converts a user-entered phone number to E.164. Accepts common Philippine formats
     * ({@code 09XXXXXXXXX}, {@code 639XXXXXXXXX}, {@code 9XXXXXXXXX}) as well as numbers
     * already in {@code +<country><number>} or {@code 00<country><number>} form.
     *
     * @throws IllegalArgumentException if the value is blank or not a recognizable number
     */
    public static String toE164(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("phoneNumber is required");
        }
        String digits = input.trim().replaceAll("[\\s()\\-]", "");

        if (digits.startsWith("+")) {
            String rest = digits.substring(1);
            requireDigits(rest, input);
            return "+" + rest;
        }
        if (digits.startsWith("00")) {
            String rest = digits.substring(2);
            requireDigits(rest, input);
            return "+" + rest;
        }
        if (digits.matches("09\\d{9}")) {       // 09XXXXXXXXX -> +639XXXXXXXXX
            return "+63" + digits.substring(1);
        }
        if (digits.matches("639\\d{9}")) {      // 639XXXXXXXXX -> +639XXXXXXXXX
            return "+" + digits;
        }
        if (digits.matches("9\\d{9}")) {        // 9XXXXXXXXX  -> +639XXXXXXXXX
            return "+63" + digits;
        }
        throw invalid(input);
    }

    private static void requireDigits(String value, String original) {
        if (!value.matches("\\d{8,15}")) {
            throw invalid(original);
        }
    }

    private static IllegalArgumentException invalid(String input) {
        return new IllegalArgumentException("Invalid phone number: " + input);
    }
}
