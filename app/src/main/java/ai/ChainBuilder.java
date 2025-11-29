/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package ai;

import ai.strategy.DecisionStrategy;
import ai.strategy.RandomFallbackStrategy;
import protocolframework.DecisionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Supplier;

/*
 * The constructor of a single decision chain
 *
 * If the two-dimensional responsibility chain is imagined as a series of grids,
 * then this is one of the layers of the grid.

Each decision type may have multiple other strategies, such as different bidding
* strategies, card-playing strategies, and even intelligent card-choosing strategies. This chain enables the program to adapt to changes
 */
public class ChainBuilder {
    private final DecisionType decisionType;
    private final List<DecisionStrategy> strategies = new ArrayList<>();

    public ChainBuilder(DecisionType type) {
        this.decisionType = type;
    }

    // The condition addition strategy is not currently used, but it is very useful in complex builds
    public ChainBuilder addStrategyIf(boolean condition, Supplier<DecisionStrategy> strategySupplier) {
        if (condition) {
            addStrategy(strategySupplier.get());
        }
        return this;
    }


    // This is the strategy pattern frequently used in this project.
    // It will construct the chain of responsibility based on the configuration file
    public ChainBuilder addStrategyIfEnabled(Properties config, String configKey, Supplier<DecisionStrategy> strategySupplier) {
        if (Boolean.parseBoolean(config.getProperty(configKey, "false"))) {
            try {
                DecisionStrategy strategy = strategySupplier.get();
                addStrategy(strategy);
            } catch (Exception e) {
                System.err.println("failed to load strategy for " + configKey + ": " + e.getMessage());
            }
        }
        return this;
    }

    // Add the strategy directly
    public ChainBuilder addStrategy(DecisionStrategy strategy) {
        strategies.add(strategy);
        return this;
    }


    //Construct the final decision chain
    //Automatically add RandomFallbackStrategy as a fallback
    public DecisionTypeChain build() {
        DecisionTypeChain chain = new DecisionTypeChain(decisionType);

        // // Sort by priority strategy
        strategies.sort((a, b) -> {
            // RandomFallbackStrategy Always at the end
            if (a instanceof RandomFallbackStrategy) return 1;
            if (b instanceof RandomFallbackStrategy) return -1;
            // Other strategies are sorted by priority (higher priority comes first)
            return Integer.compare(b.getPriority(), a.getPriority());
        });

        // Add all business strategies
        for (DecisionStrategy strategy : strategies) {
            chain.addStrategy(strategy);
        }

        // Make sure there is a fallback strategy
        boolean hasFallback = strategies.stream()
                .anyMatch(s -> s instanceof RandomFallbackStrategy);
        if (!hasFallback) {
            chain.addStrategy(new RandomFallbackStrategy());
        }

        return chain;
    }
}

