package org.cavarest.pilaf.executor;

import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;
import org.cavarest.pilaf.util.MinecraftDataExtractor;
import org.cavarest.pilaf.util.ResponseValidator;

import java.util.EnumSet;
import java.util.Set;

/**
 * Executor for server-related actions.
 * Handles: SERVER_COMMAND, EXECUTE_RCON_COMMAND, EXECUTE_RCON_WITH_CAPTURE,
 *          EXECUTE_RCON_RAW, WAIT, CLEAR_ENTITIES, PLACE_BLOCK, GET_SERVER_INFO
 */
public class ServerActionExecutor extends AbstractActionExecutor {

    private static final Set<Action.ActionType> SUPPORTED_TYPES = EnumSet.of(
        Action.ActionType.SERVER_COMMAND,
        Action.ActionType.EXECUTE_RCON_COMMAND,
        Action.ActionType.EXECUTE_RCON_WITH_CAPTURE,
        Action.ActionType.EXECUTE_RCON_RAW,
        Action.ActionType.WAIT,
        Action.ActionType.CLEAR_ENTITIES,
        Action.ActionType.PLACE_BLOCK,
        Action.ActionType.GET_SERVER_INFO
    );

    @Override
    public String getName() {
        return "ServerExecutor";
    }

    @Override
    public Set<Action.ActionType> getSupportedTypes() {
        return SUPPORTED_TYPES;
    }

    @Override
    public ActionResult execute(Action action, PilafBackend backend, StateManager stateManager) {
        try {
            switch (action.getType()) {
                case SERVER_COMMAND:
                case EXECUTE_RCON_COMMAND:
                case EXECUTE_RCON_RAW:
                case EXECUTE_RCON_WITH_CAPTURE:
                    return executeRconCommand(action, backend, stateManager);

                case WAIT:
                    return executeWait(action);

                case CLEAR_ENTITIES:
                    return executeClearEntities(action, backend);

                case GET_SERVER_INFO:
                    return executeGetServerInfo(action, backend, stateManager);

                case PLACE_BLOCK:
                    return executePlaceBlock(action, backend);

                default:
                    return ActionResult.failure("Unsupported action type: " + action.getType());
            }
        } catch (Exception e) {
            return ActionResult.failure(e);
        }
    }

    private ActionResult executeRconCommand(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        String result = executeRcon(backend, action.getCommand());

        if (result == null) {
            result = "";
        }

        // Try to extract JSON/NBT data from RCON response
        MinecraftDataExtractor.ExtractionResult extraction = MinecraftDataExtractor.extract(result);

        // Log extraction result for debugging
        if (action.getStoreAs() != null) {
            if (extraction.hasJsonContent()) {
                log("  [EXTRACTION] Successfully extracted JSON for storage: " + action.getStoreAs());
            } else if (extraction.isSuccess()) {
                log("  [EXTRACTION] Extraction succeeded but no JSON content for: " + action.getStoreAs());
            } else {
                log("  [EXTRACTION] Extraction FAILED for " + action.getStoreAs() + ": " + extraction.getErrorMessage());
            }
        }

        // Store result if store_as is specified (even on validation failure for debugging)
        if (action.getStoreAs() != null) {
            if (extraction.hasJsonContent()) {
                // Store the parsed JSON object for better state comparisons
                stateManager.store(action.getStoreAs(), extraction.getJsonObject());
            } else {
                // Store raw response if no JSON content
                stateManager.store(action.getStoreAs(), result);
            }
        }

        // Validate response against action expectations
        ResponseValidator.ValidationResult validation = ResponseValidator.validate(result, action);

        // Return failure if validation failed
        if (!validation.isValid()) {
            return new ActionResult.Builder()
                .success(false)
                .response(result)
                .error(validation.getReason())
                .extractedJson(extraction.hasJsonContent() ? extraction.getJsonString() : null)
                .parsedData(extraction.hasJsonContent() ? extraction.getJsonObject() : null)
                .build();
        }

        // Return result with extracted JSON if available
        if (extraction.hasJsonContent()) {
            return ActionResult.successWithExtractedJson(
                result,
                extraction.getJsonString(),
                extraction.getJsonObject()
            );
        }

        return ActionResult.success(result);
    }

    private ActionResult executeWait(Action action) throws InterruptedException {
        long duration = action.getDuration() != null ? action.getDuration() : 1000L;
        Thread.sleep(duration);
        return ActionResult.success("Waited " + duration + "ms");
    }

    private ActionResult executeClearEntities(Action action, PilafBackend backend) throws Exception {
        String result;
        if (action.getEntityType() != null && !action.getEntityType().isEmpty() &&
            !"all".equalsIgnoreCase(action.getEntityType())) {
            // Kill specific entity type
            result = executeRcon(backend, "kill @e[type=" + action.getEntityType() + ",distance=..50]");
        } else {
            // Kill all entities except players
            result = executeRcon(backend, "kill @e[type=!player,distance=..50]");
        }
        return ActionResult.success(result != null ? result : "Entities cleared");
    }

    private ActionResult executeGetServerInfo(Action action, PilafBackend backend, StateManager stateManager) throws Exception {
        String result = executeRcon(backend, "list");

        if (action.getStoreAs() != null && result != null) {
            stateManager.store(action.getStoreAs(), result);
        }

        return ActionResult.success(result != null ? result : "");
    }

    private ActionResult executePlaceBlock(Action action, PilafBackend backend) throws Exception {
        String setblockCmd = "setblock";
        if (action.getPosition() != null) {
            setblockCmd += " " + action.getPosition();
        }
        // Use getItem() for block type since getBlockType() doesn't exist
        String blockType = action.getItem() != null ? action.getItem() : "stone";
        setblockCmd += " " + blockType;

        String result = executeRcon(backend, setblockCmd);
        return ActionResult.success("Block placed at " + action.getPosition());
    }

    private String executeRcon(PilafBackend backend, String command) throws Exception {
        if (isMineflayer(backend)) {
            return asMineflayer(backend).executeRconWithCapture(command);
        } else if (isRcon(backend)) {
            return asRcon(backend).executeRconWithCapture(command);
        }
        return null;
    }
}
