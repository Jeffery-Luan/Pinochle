/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package ai;

import protocolframework.*;
import protocolframework.Request.DecisionRequest;
import protocolframework.decision.Decision;

/**
This is the core interface of the AI decision-making engine in our new architecture

Whether it is random AI, intelligent AI, or other types of AI that may be added in the future,
all must follow this interface

This is the unified standard for all AI implementations
*/
public interface GameAI {

    // This is a brain similar to AI. It receives a decision request and,
    // after thinking, returns a decision result
    // Randomness is also a kind of brain
    <T extends Decision> T makeDecision(DecisionRequest<T> request);

    // Reset the status of the AI
    default void reset() {}

    // Check whether the AI supports specific types of decisions
    default boolean supportsDecisionType(DecisionType type) {
        return true;
    }
}