package org.cavarest.pilaf.executor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cavarest.pilaf.backend.MineflayerBackend;
import org.cavarest.pilaf.backend.PilafBackend;
import org.cavarest.pilaf.backend.RconBackend;
import org.cavarest.pilaf.model.Action;
import org.cavarest.pilaf.state.StateManager;

/**
 * Abstract base class for action executors.
 * Provides common utilities for JSON serialization, backend type checking, etc.
 */
public abstract class AbstractActionExecutor implements ActionExecutor {

    protected static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    /**
     * Serialize an object to JSON string safely.
     */
    protected String serializeToJson(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            return JSON_MAPPER.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return obj.toString();
        }
    }

    /**
     * Check if backend is Mineflayer type.
     */
    protected boolean isMineflayer(PilafBackend backend) {
        return backend instanceof MineflayerBackend;
    }

    /**
     * Check if backend is RCON type.
     */
    protected boolean isRcon(PilafBackend backend) {
        return backend instanceof RconBackend;
    }

    /**
     * Get backend as MineflayerBackend (unsafe cast - check with isMineflayer first).
     */
    protected MineflayerBackend asMineflayer(PilafBackend backend) {
        return (MineflayerBackend) backend;
    }

    /**
     * Get backend as RconBackend (unsafe cast - check with isRcon first).
     */
    protected RconBackend asRcon(PilafBackend backend) {
        return (RconBackend) backend;
    }

    /**
     * Create a successful result with optional state storage.
     */
    protected ActionResult successWithOptionalState(String response, Action action, Object value) {
        if (action.getStoreAs() != null) {
            return ActionResult.successWithState(response, action.getStoreAs(), value);
        }
        return ActionResult.success(response);
    }

    /**
     * Log a message (can be overridden for custom logging).
     */
    protected void log(String message) {
        System.out.println("[" + getName() + "] " + message);
    }
}
