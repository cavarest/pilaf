package org.cavarest.pilaf.model;

import org.cavarest.pilaf.backend.PilafBackend;
import java.util.Map;
import java.util.HashMap;

public class Assertion {
    public enum AssertionType {
        // Original 4 types (kept for backward compatibility)
        ENTITY_HEALTH,
        ENTITY_EXISTS,
        PLAYER_INVENTORY,
        PLUGIN_COMMAND,

        // New assertion types for comprehensive testing
        ASSERT_ENTITY_MISSING,
        ASSERT_PLAYER_HAS_ITEM,
        ASSERT_RESPONSE_CONTAINS,
        ASSERT_JSON_EQUALS,
        ASSERT_LOG_CONTAINS,
        ASSERT_CONDITION
    }

    public enum Condition { EQUALS, NOT_EQUALS, LESS_THAN, GREATER_THAN, LESS_THAN_OR_EQUALS, GREATER_THAN_OR_EQUALS }

    private AssertionType type;
    private String entity, player, item, slot, plugin, command;
    private String source, contains, expectedJson, condition;
    private Condition conditionType;
    private Double value;
    private Boolean expected;
    private String variableName;  // For ASSERT_CONDITION

    public Assertion() {}
    public Assertion(AssertionType type) { this.type = type; }

    public AssertionType getType() { return type; }
    public void setType(AssertionType type) { this.type = type; }
    public String getEntity() { return entity; }
    public void setEntity(String entity) { this.entity = entity; }
    public String getPlayer() { return player; }
    public void setPlayer(String player) { this.player = player; }
    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }
    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }
    public String getPlugin() { return plugin; }
    public void setPlugin(String plugin) { this.plugin = plugin; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public Condition getConditionType() { return conditionType; }
    public void setConditionType(Condition conditionType) { this.conditionType = conditionType; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    public Boolean getExpected() { return expected; }
    public void setExpected(Boolean expected) { this.expected = expected; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getContains() { return contains; }
    public void setContains(String contains) { this.contains = contains; }
    public String getExpectedJson() { return expectedJson; }
    public void setExpectedJson(String expectedJson) { this.expectedJson = expectedJson; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public String getVariableName() { return variableName; }
    public void setVariableName(String variableName) { this.variableName = variableName; }

    /**
     * Evaluate the assertion against the backend.
     * Returns a result map with: passed (boolean), message (String), details (String)
     */
    public Map<String, Object> evaluate(PilafBackend backend) {
        Map<String, Object> result = new HashMap<>();
        result.put("passed", false);
        result.put("message", "Unknown assertion type: " + type);
        result.put("details", "");

        switch (type) {
            case ENTITY_HEALTH:
                double actualHealth = backend.getEntityHealth(entity);
                boolean healthMatch = compareValues(actualHealth, value, conditionType);
                result.put("passed", healthMatch);
                result.put("message", String.format("Entity '%s' health %s %.1f (actual: %.1f)",
                    entity, conditionType, value, actualHealth));
                result.put("details", String.valueOf(actualHealth));
                break;

            case ENTITY_EXISTS:
                boolean exists = backend.entityExists(entity);
                boolean existsMatch = (expected != null && exists == expected) || (expected == null && exists);
                result.put("passed", existsMatch);
                result.put("message", String.format("Entity '%s' exists=%s", entity, exists));
                result.put("details", String.valueOf(exists));
                break;

            case PLAYER_INVENTORY:
                boolean hasItem = backend.playerInventoryContains(player, item, slot);
                boolean itemMatch = (expected != null && hasItem == expected) || (expected == null && hasItem);
                result.put("passed", itemMatch);
                result.put("message", String.format("Player '%s' has item '%s'=%s", player, item, hasItem));
                result.put("details", String.valueOf(hasItem));
                break;

            case PLUGIN_COMMAND:
                boolean cmdReceived = backend.pluginReceivedCommand(plugin, command, player);
                result.put("passed", cmdReceived);
                result.put("message", String.format("Plugin '%s' received command '%s' from %s: %s",
                    plugin, command, player, cmdReceived));
                result.put("details", String.valueOf(cmdReceived));
                break;

            case ASSERT_ENTITY_MISSING:
                boolean isMissing = !backend.entityExists(entity);
                result.put("passed", isMissing);
                result.put("message", String.format("Entity '%s' missing=%s", entity, isMissing));
                result.put("details", String.valueOf(isMissing));
                break;

            case ASSERT_PLAYER_HAS_ITEM:
                boolean playerHasItem = backend.playerInventoryContains(player, item, null);
                result.put("passed", playerHasItem);
                result.put("message", String.format("Player '%s' has item '%s': %s", player, item, playerHasItem));
                result.put("details", String.valueOf(playerHasItem));
                break;

            case ASSERT_RESPONSE_CONTAINS:
                // source should be a reference to previous step output
                String response = resolveVariable(source);
                boolean containsText = response != null && response.contains(contains);
                result.put("passed", containsText);
                result.put("message", String.format("Response contains '%s': %s", contains, containsText));
                result.put("details", containsText ? response : "");
                break;

            case ASSERT_JSON_EQUALS:
                // JSON comparison using string comparison
                // For more advanced comparison, zjsonpatch could be used
                String actualJson = resolveVariable(source);
                boolean jsonEqual;
                if (actualJson != null && expectedJson != null) {
                    // Simple comparison - normalize both JSON strings
                    String normalizedActual = normalizeJson(actualJson);
                    String normalizedExpected = normalizeJson(expectedJson);
                    jsonEqual = normalizedActual.equals(normalizedExpected);
                } else {
                    jsonEqual = false;
                }
                result.put("passed", jsonEqual);
                result.put("message", String.format("JSON equals: %s", jsonEqual));
                result.put("details", actualJson != null ? actualJson : "null");
                break;

            case ASSERT_LOG_CONTAINS:
                // Server log assertion
                String serverLog = backend.getServerLog();
                boolean logContains = serverLog != null && serverLog.contains(contains);
                result.put("passed", logContains);
                result.put("message", String.format("Log contains '%s': %s", contains, logContains));
                result.put("details", serverLog != null ? serverLog : "");
                break;

            case ASSERT_CONDITION:
                // Evaluate a custom condition expression
                // Supports: variable comparison, arithmetic expressions
                Object conditionResult = evaluateCondition(condition, variableName);
                result.put("passed", Boolean.TRUE.equals(conditionResult));
                result.put("message", String.format("Condition '%s': %s", condition, conditionResult));
                result.put("details", String.valueOf(conditionResult));
                break;

            default:
                result.put("passed", false);
                result.put("message", "Unimplemented assertion type: " + type);
        }

        return result;
    }

    private boolean compareValues(double actual, Double expectedVal, Condition cond) {
        if (expectedVal == null || cond == null) return false;
        switch (cond) {
            case EQUALS: return Math.abs(actual - expectedVal) < 0.001;
            case NOT_EQUALS: return Math.abs(actual - expectedVal) >= 0.001;
            case LESS_THAN: return actual < expectedVal;
            case GREATER_THAN: return actual > expectedVal;
            case LESS_THAN_OR_EQUALS: return actual <= expectedVal;
            case GREATER_THAN_OR_EQUALS: return actual >= expectedVal;
            default: return false;
        }
    }

    private String resolveVariable(String ref) {
        // In a full implementation, this would resolve step output references
        // For now, return the raw value if it's not a reference
        if (ref != null && ref.startsWith("${{")) {
            // Would resolve from step outputs in real implementation
            return null;
        }
        return ref;
    }

    /**
     * Normalize JSON string for comparison.
     * Removes whitespace and sorts keys for consistent comparison.
     */
    private String normalizeJson(String json) {
        if (json == null) return "";
        // Simple normalization - remove whitespace
        return json.replaceAll("\\s+", "").trim();
    }

    /**
     * Evaluate a condition expression.
     * Supports:
     * - Variable comparison: "${variable} > 10"
     * - String equality: "${variable} == 'expected'"
     * - Boolean: "${variable}"
     */
    private Object evaluateCondition(String condition, String variableName) {
        if (condition == null) return false;

        try {
            // Handle variable reference
            if (condition.startsWith("${") && variableName != null) {
                String varValue = resolveVariable(variableName);
                if (varValue == null) return false;

                // Check if it's a boolean check
                if (condition.equals("${" + variableName + "}")) {
                    return !varValue.isEmpty() && !varValue.equals("null") && !varValue.equals("false");
                }

                // Handle comparison operators
                if (condition.contains("==")) {
                    String[] parts = condition.split("==");
                    String expected = parts.length > 1 ? parts[1].trim().replace("'", "") : "";
                    return varValue.trim().equals(expected);
                }
                if (condition.contains(">")) {
                    String[] parts = condition.split(">");
                    double left = Double.parseDouble(varValue);
                    double right = Double.parseDouble(parts[1].trim());
                    return left > right;
                }
                if (condition.contains("<")) {
                    String[] parts = condition.split("<");
                    double left = Double.parseDouble(varValue);
                    double right = Double.parseDouble(parts[1].trim());
                    return left < right;
                }
                if (condition.contains(">=")) {
                    String[] parts = condition.split(">=");
                    double left = Double.parseDouble(varValue);
                    double right = Double.parseDouble(parts[1].trim());
                    return left >= right;
                }
                if (condition.contains("<=")) {
                    String[] parts = condition.split("<=");
                    double left = Double.parseDouble(varValue);
                    double right = Double.parseDouble(parts[1].trim());
                    return left <= right;
                }
                if (condition.contains("!=")) {
                    String[] parts = condition.split("!=");
                    String expected = parts.length > 1 ? parts[1].trim().replace("'", "") : "";
                    return !varValue.trim().equals(expected);
                }
            }

            // Direct boolean expression
            if (condition.equals("true")) return true;
            if (condition.equals("false")) return false;

            // Arithmetic evaluation
            return evaluateArithmetic(condition);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Simple arithmetic expression evaluator.
     */
    private Object evaluateArithmetic(String expr) {
        try {
            // Remove whitespace
            expr = expr.replaceAll("\\s+", "");

            // Handle simple addition
            if (expr.contains("+")) {
                String[] parts = expr.split("\\+");
                double sum = 0;
                for (String part : parts) {
                    sum += Double.parseDouble(part);
                }
                return sum;
            }

            // Handle simple subtraction
            if (expr.contains("-")) {
                String[] parts = expr.split("-");
                if (parts.length == 2) {
                    return Double.parseDouble(parts[0]) - Double.parseDouble(parts[1]);
                }
            }

            // Handle simple multiplication
            if (expr.contains("*")) {
                String[] parts = expr.split("\\*");
                double product = 1;
                for (String part : parts) {
                    product *= Double.parseDouble(part);
                }
                return product;
            }

            // Handle simple division
            if (expr.contains("/")) {
                String[] parts = expr.split("/");
                if (parts.length == 2) {
                    double divisor = Double.parseDouble(parts[1]);
                    if (divisor != 0) {
                        return Double.parseDouble(parts[0]) / divisor;
                    }
                }
            }

            // Return as string if not a simple arithmetic expression
            return expr;
        } catch (Exception e) {
            return expr;
        }
    }
}
