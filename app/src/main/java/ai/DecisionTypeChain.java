/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package ai;

import ai.strategy.DecisionStrategy;
import ai.strategy.RandomFallbackStrategy;
import protocolframework.*;
import protocolframework.Request.DecisionRequest;
import protocolframework.decision.Decision;
import java.util.*;

/**
 * Decision type chain with automatic fallback strategy
 * Each chain has its own fallback mechanism to ensure reliable decision-making
 *
 *This is one of the layers of the two-dimensional responsibility chain,
 * which will select and sort strategies based on the priority of the strategies.
 *  It will be assembled into a two-dimensional responsibility chain,
 * or a responsibility chain forest, in the general AI
 */
public class DecisionTypeChain {
    private final DecisionType supportedType;
    private final List<DecisionStrategy> strategies = new ArrayList<>();
    private final RandomFallbackStrategy fallbackStrategy;

    public DecisionTypeChain(DecisionType type) {
        this.supportedType = type;
        this.fallbackStrategy = new RandomFallbackStrategy();
    }


     //Add strategy and automatically sort by priority
     //Ensure that the RandomFallbackStrategy is always at the very bottom of
     // the chain
    public void addStrategy(DecisionStrategy strategy) {
        strategies.add(strategy);
        // Sort by priority: higher priority first, RandomFallbackStrategy always last
        strategies.sort((a, b) -> {
            if (a instanceof RandomFallbackStrategy) return 1;
            if (b instanceof RandomFallbackStrategy) return -1;
            return Integer.compare(b.getPriority(), a.getPriority());
        });
    }

    @SuppressWarnings("unchecked")
    public <T extends Decision> T handleRequest(DecisionRequest<T> request) {
        // First phase: try all business strategies
        for (DecisionStrategy strategy : strategies) {
            if (strategy.canHandle((DecisionRequest<Decision>) request)) {
                try {
                    T decision = (T) strategy.decide((DecisionRequest<Decision>) request);
                    if (decision != null && decision.isValid()) {
                        return decision;
                    }
                } catch (Exception e) {
                    System.err.println("Strategy failed: " + strategy.getClass().getSimpleName() + " - " + e.getMessage());
                    // Continue to next strategy
                }
            }
        }

        // Second phase: use the RandomFallbackStrategy at the bottom of the chain as a fallback
        try {
            T decision = (T) fallbackStrategy.decide((DecisionRequest<Decision>) request);
            if (decision != null && decision.isValid()) {
                return decision;
            }
        } catch (Exception e) {
            System.err.println("Fallback strategy failed: " + e.getMessage());
        }

        // If even fallback strategy fails, throw exception
        throw new IllegalStateException("All strategies failed including fallback for type: " + supportedType);
    }

    // Check whether it can be handled
    public boolean canHandle(DecisionType type) {
        return supportedType == type;
    }

    // Get all strategies including fallback
    public List<DecisionStrategy> getStrategies() {
        List<DecisionStrategy> allStrategies = new ArrayList<>(strategies);
        allStrategies.add(fallbackStrategy);
        return allStrategies;
    }

    // Reset all strategies in the chain
    public void reset() {
        strategies.forEach(DecisionStrategy::reset);
        fallbackStrategy.reset();
    }
}