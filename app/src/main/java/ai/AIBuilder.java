/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package ai;

import ai.strategy.*;
import protocolframework.DecisionType;
import meld.MeldAnalyzer;
import java.util.*;

/**
 AI constructor
 Ensure that a complete chain of responsibility for all Decision types can be constructed

 It is responsible for assembling responsibility chains at different levels.
 By assembling multiple responsibility chains, a two-dimensional responsibility chain is achieved.
 Then, through the two-dimensional responsibility chain, a universal intelligent AI is constructed

 The builder pattern is adopted for complex builds

 It can automatically assemble the chain of responsibility based on the configuration file
 and decide whether the parameters should be enabled according to the configuration
 */
public class AIBuilder {
    private final Map<DecisionType, ChainBuilder> chainBuilders = new HashMap<>();
    private final Properties config;
    private final MeldAnalyzer meldAnalyzer;

    public AIBuilder(Properties config, MeldAnalyzer meldAnalyzer) {
        this.config = config;
        this.meldAnalyzer = meldAnalyzer;
        initializeAllChainBuilders();
    }

    // Initialize the chain constructors of all DecisionTypes
    // Make sure not to miss any type of decision
    private void initializeAllChainBuilders() {
        for (DecisionType type : DecisionType.values()) {
            chainBuilders.put(type, new ChainBuilder(type));
        }
    }


    // Configure the bidding strategy chain
    public AIBuilder configureBidding(ChainConfigurator configurator) {
        ChainBuilder bidChain = chainBuilders.get(DecisionType.BID);
        configurator.configure(bidChain, config, meldAnalyzer);
        return this;
    }

    // Configure the card-playing strategy chain
    public AIBuilder configureCardPlay(ChainConfigurator configurator) {
        ChainBuilder cardPlayChain = chainBuilders.get(DecisionType.CARD_PLAY);
        configurator.configure(cardPlayChain, config, meldAnalyzer);
        return this;
    }

    // Configure the strategy chain for card flipping selection
    public AIBuilder configureRevealedCardSelection(ChainConfigurator configurator) {
        ChainBuilder revealChain = chainBuilders.get(DecisionType.REVEALED_CARD_SELECTION);
        configurator.configure(revealChain, config, meldAnalyzer);
        return this;
    }


    // Configure the card discarding policy chain
    public AIBuilder configureCardDiscard(ChainConfigurator configurator) {
        ChainBuilder discardChain = chainBuilders.get(DecisionType.CARD_DISCARD);
        configurator.configure(discardChain, config, meldAnalyzer);
        return this;
    }

    // Configure the ace choice strategy chain
    public AIBuilder configureTrumpSelection(ChainConfigurator configurator) {
        ChainBuilder trumpChain = chainBuilders.get(DecisionType.TRUMP_SELECTION);
        configurator.configure(trumpChain, config, meldAnalyzer);
        return this;
    }


    // Configure the card selection strategy chain
    public AIBuilder configureCardChoose(ChainConfigurator configurator) {
        ChainBuilder chooseChain = chainBuilders.get(DecisionType.CARD_CHOOSE);
        configurator.configure(chooseChain, config, meldAnalyzer);
        return this;
    }


    // General configuration method
    public AIBuilder configure(DecisionType type, ChainConfigurator configurator) {
        ChainBuilder chain = chainBuilders.get(type);
        if (chain != null) {
            configurator.configure(chain, config, meldAnalyzer);
        }
        return this;
    }

    // Access the chain constructor directly
    public ChainBuilder getChainBuilder(DecisionType type) {
        return chainBuilders.get(type);
    }


    // Standard configuration based on configuration files - Constructing
    // universal AI
    public AIBuilder configureFromProperties() {
        return this
                .configureBidding(this::configureBiddingFromProperties)
                .configureCardPlay(this::configureCardPlayFromProperties)
                .configureRevealedCardSelection(this::configureRevealedCardFromProperties)
                .configureCardDiscard(this::configureCardDiscardFromProperties)
                .configureTrumpSelection(this::configureTrumpSelectionFromProperties)
                .configureCardChoose(this::configureCardChooseFromProperties);
    }


     //Construct the final AI
     //Ensure that all DecisionTypes have corresponding chains
    public GameAI build() {
        Map<DecisionType, DecisionTypeChain> chains = new HashMap<>();

        // Construct a chain for each DecisionType
        for (Map.Entry<DecisionType, ChainBuilder> entry : chainBuilders.entrySet()) {
            DecisionTypeChain chain = entry.getValue().build();
            chains.put(entry.getKey(), chain);
        }

        // Verify that all decision types have chains
        validateAllDecisionTypesHaveChains(chains);

        return new ConfigurableGameAI(chains);
    }

    // Essentially, other bidding strategies can continue to be added
    private void configureBiddingFromProperties(ChainBuilder chain, Properties config, MeldAnalyzer analyzer) {
        chain.addStrategyIfEnabled(config, "players.0.smartbids", () -> new SmartBiddingStrategy(analyzer));
        // more
    }

    // Essentially, other card-playing strategies can continue to be added
    private void configureCardPlayFromProperties(ChainBuilder chain, Properties config, MeldAnalyzer analyzer) {
        chain.addStrategyIfEnabled(config, "mode.smarttrick", SmartCardPlayStrategy::new);
    }   // more

    // as it is very simple, there is currently no intelligent card-flipping
    // strategy, only a fallback strategy
    private void configureRevealedCardFromProperties(ChainBuilder chain, Properties config, MeldAnalyzer analyzer) {
        // chain.addStrategyIfEnabled(config, "mode.smartreveal", () -> new SmartRevealedCardStrategy());
        // more
    }

    // Essentially, other mode.cutthroat strategies can continue to be added
    private void configureCardDiscardFromProperties(ChainBuilder chain, Properties config, MeldAnalyzer analyzer) {
        chain.addStrategyIfEnabled(config, "mode.cutthroat", CardDiscardStrategy::new);
        // more
    }

    // In the future, it is possible to add a fallback strategy that currently does not have a dedicated ace choice strategy
    private void configureTrumpSelectionFromProperties(ChainBuilder chain, Properties config, MeldAnalyzer analyzer) {
        // chain.addStrategyIfEnabled(config, "mode.smarttrump", () -> new SmartTrumpSelectionStrategy());
        // ADD
    }

    // At present, there is no specific card selection strategy, only a fallback strategy
    private void configureCardChooseFromProperties(ChainBuilder chain, Properties config, MeldAnalyzer analyzer) {
        // chain.addStrategyIfEnabled(config, "mode.smartchoose", () -> new SmartCardChooseStrategy());
        //MORE
    }


    // Verify that all types of decisions have corresponding chains
    private void validateAllDecisionTypesHaveChains(Map<DecisionType, DecisionTypeChain> chains) {
        for (DecisionType type : DecisionType.values()) {
            if (!chains.containsKey(type)) {
                throw new IllegalStateException("NO!, missing chain for " +
                        "DecisionType: " + type);
            }
        }
    }

    @FunctionalInterface
    public interface ChainConfigurator {
        void configure(ChainBuilder chain, Properties config, MeldAnalyzer meldAnalyzer);
    }
}