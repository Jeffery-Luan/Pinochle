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
import java.util.*;

/**
 Configurable game AI - two-dimensional Chain of responsibility mode
 The first dimension: Routing by decision type
 The second dimension: The strategy chain within each decision type (with a fallback strategy)

 We abstracted the entire agent and decision-making mechanism in the project and
 constructed a universal intelligent AI, just like the computers we usually use to play games.
 They are not responsible for a specific strategy but have their own style of strategy
 for different decisions in a game

 Its core module is a two-dimensional responsibility chain, similar to an intelligent decision-making center

 It uses the universal request decision protocol designed by us
The packaged decision information can be returned through the request protocol
 */
public class ConfigurableGameAI implements GameAI {
    private final Map<DecisionType, DecisionTypeChain> decisionChains = new HashMap<>();

    public ConfigurableGameAI(Map<DecisionType, DecisionTypeChain> chains) {
        this.decisionChains.putAll(chains);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Decision> T makeDecision(DecisionRequest<T> request) {
        DecisionType requestType = request.getType();

        DecisionTypeChain chain = decisionChains.get(requestType);
        if (chain != null && chain.canHandle(requestType)) {
            T decision = chain.handleRequest(request);
            if (decision != null) {
                return decision;
            }
        }
        //return null;
        throw new UnsupportedOperationException("No chain registered for type: " + requestType);
    }

    @Override
    public boolean supportsDecisionType(DecisionType type) {
        return decisionChains.containsKey(type);
    }

    @Override
    public void reset() {
        decisionChains.values().forEach(DecisionTypeChain::reset);
    }
}