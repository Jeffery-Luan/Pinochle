/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package ai.strategy;

import protocolframework.*;
import protocolframework.Request.DecisionRequest;
import protocolframework.decision.Decision;

/*
This DecisionStrategy interface is the core interface of the AI strategy system
in our new architecture. It defines the standard behaviors that all AI decision strategies must follow

It is used to encapsulate different decision-making algorithms in different strategy classes,
and even the decision-making strategies can be dynamically switched at runtime
* */
public interface DecisionStrategy<T extends Decision> {

    // This is the core approach of the entire strategy interface, executing specific AI decision-making logic
    T decide(DecisionRequest<T> request);

    // Obtain the types of decisions supported by this strategy
    DecisionType getSupportedType();

    // Check whether specific requests can be processed
    default boolean canHandle(DecisionRequest<T> request) {
        return true;
    }

    // Reset the policy state
    default void reset() {}

    int getPriority();
}