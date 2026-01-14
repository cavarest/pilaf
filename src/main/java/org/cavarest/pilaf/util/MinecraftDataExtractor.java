package org.cavarest.pilaf.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class to extract and parse JSON/NBT data from Minecraft RCON responses.
 *
 * Minecraft RCON commands like `data get entity` return responses in the format:
 * "player has the following entity data: [100.5d, 64.0d, 100.5d]"
 *
 * This class:
 * 1. Extracts the JSON/NBT portion from the response
 * 2. Converts Minecraft NBT type suffixes (d, f, b, s, L) to valid JSON
 * 3. Returns both raw and parsed JSON representations
 */
public class MinecraftDataExtractor {

    private static final ObjectMapper mapper = new ObjectMapper();

    // Pattern to match data after common RCON response prefixes
    // Examples:
    //   "player has the following entity data: [...]"
    //   "Entity data: {...}"
    //   "Block data: {...}"
    private static final Pattern DATA_PATTERN = Pattern.compile(
        "(?:has the following (?:entity |block )?data:|data:)\\s*(.+)$",
        Pattern.CASE_INSENSITIVE
    );

    // Pattern to match NBT type suffixes (must not be part of a word)
    // d=double, f=float, b=byte, s=short, L=long
    // Matches: 100.5d, 0.5f, 1b, 100s, 1234567890L
    // But NOT: "minecraft:diamond" (colon followed by letter)
    // PRESERVES decimals for d/f suffixes by ensuring .0 stays as 0.0
    private static final Pattern NBT_SUFFIX_PATTERN = Pattern.compile(
        "([-+]?(?:\\d+\\.?\\d*|\\d*\\.?\\d+))([dDfFbBsSlL])(?=[,\\]\\}\\s]|$)"
    );

    // Pattern to preserve decimal points for float/double suffixes
    private static final Pattern DECIMAL_PRESERVE_PATTERN = Pattern.compile(
        "\\b(\\d+)\\.0+\\b"  // Matches whole numbers with decimal like 15.0, 20.0
    );

    // Pattern to match UUID in format [I;...] which needs special handling
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "\\[I;\\s*([^\\]]+)\\]"
    );

    // Pattern to match unquoted string keys in NBT
    private static final Pattern UNQUOTED_KEY_PATTERN = Pattern.compile(
        "([{,]\\s*)([a-zA-Z_][a-zA-Z0-9_]*)\\s*:"
    );

    /**
     * Result of extracting JSON from an RCON response.
     */
    public static class ExtractionResult {
        private final String rawResponse;
        private final String extractedNbt;
        private final String jsonString;
        private final Object jsonObject;
        private final boolean success;
        private final String errorMessage;

        private ExtractionResult(String rawResponse, String extractedNbt,
                                 String jsonString, Object jsonObject,
                                 boolean success, String errorMessage) {
            this.rawResponse = rawResponse;
            this.extractedNbt = extractedNbt;
            this.jsonString = jsonString;
            this.jsonObject = jsonObject;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static ExtractionResult success(String rawResponse, String extractedNbt,
                                               String jsonString, Object jsonObject) {
            return new ExtractionResult(rawResponse, extractedNbt, jsonString, jsonObject, true, null);
        }

        public static ExtractionResult failure(String rawResponse, String message) {
            return new ExtractionResult(rawResponse, null, null, null, false, message);
        }

        public static ExtractionResult noJsonContent(String rawResponse) {
            return new ExtractionResult(rawResponse, null, null, null, true, null);
        }

        public String getRawResponse() { return rawResponse; }
        public String getExtractedNbt() { return extractedNbt; }
        public String getJsonString() { return jsonString; }
        public Object getJsonObject() { return jsonObject; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        public boolean hasJsonContent() { return jsonString != null; }
    }

    /**
     * Extract and parse JSON from a Minecraft RCON response.
     *
     * @param rconResponse The raw RCON response string
     * @return ExtractionResult containing raw, NBT, and parsed JSON
     */
    public static ExtractionResult extract(String rconResponse) {
        if (rconResponse == null || rconResponse.isEmpty()) {
            return ExtractionResult.noJsonContent(rconResponse);
        }

        // Try to find JSON/NBT content in the response
        String nbtContent = extractNbtContent(rconResponse);

        if (nbtContent == null) {
            // No structured data found - just return the raw response
            return ExtractionResult.noJsonContent(rconResponse);
        }

        // Convert NBT to valid JSON (preserves decimals for floats/doubles)
        String jsonString = convertNbtToJson(nbtContent);

        // Try to parse the JSON using JsonNode to preserve numeric types
        try {
            com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(jsonString);
            // Convert JsonNode to Object while preserving numeric types
            Object jsonObject = mapper.treeToValue(jsonNode, Object.class);
            return ExtractionResult.success(rconResponse, nbtContent, jsonString, jsonObject);
        } catch (JsonProcessingException e) {
            // JSON parsing failed - try to fix common NBT issues and retry
            String fixedJson = fixCommonNbtIssues(jsonString);
            try {
                com.fasterxml.jackson.databind.JsonNode jsonNode = mapper.readTree(fixedJson);
                Object jsonObject = mapper.treeToValue(jsonNode, Object.class);
                System.err.println("[NBT EXTRACTION] Fixed NBT parsing issue, retry succeeded");
                return ExtractionResult.success(rconResponse, nbtContent, fixedJson, jsonObject);
            } catch (JsonProcessingException e2) {
                // Still failed - return with error details
                String errorMsg = "Failed to parse JSON: " + e.getMessage();
                if (e.getMessage().equals(e2.getMessage())) {
                    // Same error, don't duplicate
                } else {
                    errorMsg += " (retry also failed: " + e2.getMessage() + ")";
                }
                errorMsg += ". NBT length: " + nbtContent.length() + ", JSON length: " + jsonString.length();
                return ExtractionResult.failure(rconResponse, errorMsg);
            }
        }
    }

    /**
     * Attempts to fix common NBT-to-JSON conversion issues.
     * Handles cases like:
     * - Truncated arrays
     * - Unmatched brackets
     * - Missing commas between array elements
     */
    private static String fixCommonNbtIssues(String json) {
        String fixed = json;

        // Fix missing commas between array elements: "} {" => "}, {"
        fixed = fixed.replaceAll("\\}\\s+\\{", "}, {");

        // Fix missing commas between object properties: "} " => "}, "
        // Be careful not to add commas before closing brackets
        fixed = fixed.replaceAll("\"\\s+([a-zA-Z_])", "\", $1");

        // Try to fix truncated arrays by adding closing brackets
        int openBrackets = countChar(fixed, '[');
        int closeBrackets = countChar(fixed, ']');
        while (openBrackets > closeBrackets) {
            fixed += "]";
            closeBrackets++;
        }

        int openBraces = countChar(fixed, '{');
        int closeBraces = countChar(fixed, '}');
        while (openBraces > closeBraces) {
            fixed += "}";
            closeBraces++;
        }

        return fixed;
    }

    /**
     * Counts occurrences of a character in a string.
     */
    private static int countChar(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    /**
     * Extract NBT content from an RCON response.
     *
     * @param response The RCON response
     * @return The NBT content string, or null if not found
     */
    private static String extractNbtContent(String response) {
        // First try the common pattern "has the following entity data: ..."
        Matcher matcher = DATA_PATTERN.matcher(response);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // If response starts with [ or {, it's likely already JSON/NBT
        String trimmed = response.trim();
        if (trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return trimmed;
        }

        // Look for standalone array or object anywhere in the response
        // This handles cases like "Value: [1, 2, 3]" or "Result: {...}"
        int bracketStart = findJsonStart(response);
        if (bracketStart >= 0) {
            return response.substring(bracketStart).trim();
        }

        return null;
    }

    /**
     * Find the start of JSON/NBT content in a string.
     *
     * @param str The string to search
     * @return The index where JSON starts, or -1 if not found
     */
    private static int findJsonStart(String str) {
        int bracketIdx = str.indexOf('[');
        int braceIdx = str.indexOf('{');

        if (bracketIdx < 0 && braceIdx < 0) {
            return -1;
        }
        if (bracketIdx < 0) {
            return braceIdx;
        }
        if (braceIdx < 0) {
            return bracketIdx;
        }
        return Math.min(bracketIdx, braceIdx);
    }

    /**
     * Convert Minecraft NBT format to valid JSON.
     *
     * NBT differences from JSON:
     * - Type suffixes: 100.5d, 0.5f, 1b, 100s, 1234567890L
     * - Unquoted keys: {Pos: [...]} instead of {"Pos": [...]}
     * - UUID arrays: [I; 1, 2, 3, 4]
     *
     * @param nbt The NBT string
     * @return Valid JSON string
     */
    public static String convertNbtToJson(String nbt) {
        if (nbt == null) {
            return null;
        }

        String result = nbt;

        // 1. Handle UUID arrays [I; ...] -> regular arrays
        result = UUID_PATTERN.matcher(result).replaceAll("[$1]");

        // 2. Remove NBT type suffixes (d, f, b, s, L)
        result = NBT_SUFFIX_PATTERN.matcher(result).replaceAll("$1");

        // 3. Quote unquoted keys
        result = quoteUnquotedKeys(result);

        return result;
    }

    /**
     * Quote unquoted keys in NBT format.
     * Converts {Pos: [...]} to {"Pos": [...]}
     *
     * @param nbt The NBT string
     * @return String with quoted keys
     */
    private static String quoteUnquotedKeys(String nbt) {
        Matcher matcher = UNQUOTED_KEY_PATTERN.matcher(nbt);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String prefix = matcher.group(1);  // "{" or ","
            String key = matcher.group(2);     // The key name
            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + "\"" + key + "\":"));
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * Check if a string contains JSON/NBT data that can be extracted.
     *
     * @param str The string to check
     * @return true if the string likely contains extractable data
     */
    public static boolean containsExtractableData(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }

        // Check for common patterns
        return DATA_PATTERN.matcher(str).find()
            || str.trim().startsWith("[")
            || str.trim().startsWith("{")
            || str.contains(": [")
            || str.contains(": {");
    }

    /**
     * Pretty-print a JSON string.
     *
     * @param json The JSON string
     * @return Pretty-printed JSON, or original string if parsing fails
     */
    public static String prettyPrint(String json) {
        if (json == null) {
            return null;
        }
        try {
            Object obj = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return json;
        }
    }
}
