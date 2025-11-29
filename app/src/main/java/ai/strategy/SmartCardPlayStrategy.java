/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package ai.strategy;

import ch.aplu.jcardgame.Card;
import core.Rank;
import core.Suit;
import data.GameDataSnapshot;
import protocolframework.*;
import protocolframework.Request.DecisionRequest;
import protocolframework.Request.GeneralCardDecisionRequest;
import protocolframework.decision.GeneralCardDecision;

import java.util.*;
import java.util.stream.Collectors;

/**
 *This class implements Bayesian estimation (basic probability inference).
 * This strategy will attempt to obtain as high an expected score as possible,
 * which means that risks and probabilities are also included in the expected calculation.
 * Furthermore, considering the nature of the game, we have added heuristic rules
 * based on gameplay experience. The current strategy is definitely superior to random situations
 *
 * This is just a basic AI strategy for job presentation. Considering that
 * the current AI architecture is flexible enough and easy to expand,
 * it can be considered to use the Monte Carlo Tree Search (MCTS) strategy or
 * true Bayesian probability in the future,
 * and reduce the priority of this strategy as a backup strategy.
 *
 *
 * Simplified Bayesian Card Play Strategy with Heuristic Rule Refinement
 *
 * Uses basic Bayesian inference with minimal information:
 * - Own hand (known)
 * - Played cards (known)
 * - Unknown cards (assumed in opponent's hand with equal probability)
 *
 * Enhanced with strategic heuristic rules (in priority order):
 * 0.   PRIORITY: Loss Minimization - When following and certain to lose, choose absolute lowest value card
 * 1.   Conservative Trump Usage: Avoid using trump unless high win probability (>TRUMP_CONSERVATION_THRESHOLD)
 * 2.   Low-Value Discard Priority: When win probability is low (<LOW_VALUE_DISCARD_THRESHOLD), discard low-value non-trump cards
 */
public class SmartCardPlayStrategy implements DecisionStrategy<GeneralCardDecision> {

    // Strategic thresholds as named constants (no magic numbers)
    private static final double TRUMP_CONSERVATION_THRESHOLD = 0.70; // 70% win probability to use trump
    private static final double LOW_VALUE_DISCARD_THRESHOLD = 0.20;   // 20% win probability for low-value discard
    private static final double CERTAIN_LOSS_PROBABILITY = 0.0;       // 0% win probability = certain loss
    private static final double CERTAIN_WIN_PROBABILITY = 1.0;        // 100% win probability = certain win
    private static final double DEFAULT_WIN_PROBABILITY = 0.5;        // 50% default when no information

    // Pinochle deck constants
    private static final int TOTAL_CARDS_PER_SUIT = 6;               // 9, J, Q, K, 10, A per suit
    private static final int COPIES_PER_CARD = 2;                    // Each card appears twice in Pinochle
    private static final String[] SUITS = {"S", "H", "D", "C"};      // Spades, Hearts, Diamonds, Clubs
    private static final int[] RANK_VALUES = {0, 1, 2, 3, 4, 5};     // NINE=0, JACK=1, QUEEN=2, KING=3, TEN=4, ACE=5

    // String parsing constants
    private static final int SUIT_CHAR_INDEX = 1;                    // Last character is suit (e.g., "5S" -> "S")
    private static final int RANK_SUBSTRING_END = 1;                 // Rank is from start to second-last char

    // Strategy priority constants
    private static final int BAYESIAN_STRATEGY_PRIORITY = 1;         // Higher priority than basic smart strategy

    // All 48 cards in Pinochle deck (2 copies of 9-A in each suit)
    private static final List<String> ALL_CARDS = generateAllCards();

    @Override
    public GeneralCardDecision decide(DecisionRequest<GeneralCardDecision> request) {
        GeneralCardDecisionRequest cardRequest = (GeneralCardDecisionRequest) request;
        GameDataSnapshot snapshot = request.getSnapshot();
        int playerIndex = request.getPlayerIndex();

        List<Card> validCards = cardRequest.getAvailableCards();
        if (validCards.isEmpty()) {
            return new GeneralCardDecision(Collections.emptyList());
        }

        // Get known information
        List<Card> myHand = snapshot.getPlayerHand(playerIndex);
        List<Card> playedCards = snapshot.getAllPlayedCards();
        List<Card> currentTrick = snapshot.getCurrentTrick();
        String trumpSuit = snapshot.getTrumpSuit();

        // Calculate opponent's possible cards using Bayesian inference
        List<String> unknownCards = calculateUnknownCards(myHand, playedCards, currentTrick);

        // Choose best card based on expected value
        Card bayesianChoice = chooseBestCard(validCards, unknownCards,
                currentTrick, trumpSuit, snapshot);

        // Apply heuristic rules to refine the decision
        Card finalChoice = applyHeuristicRules(bayesianChoice, validCards, unknownCards,
                currentTrick, trumpSuit, snapshot);

        int selectedIndex = validCards.indexOf(finalChoice);
        return new GeneralCardDecision(Arrays.asList(selectedIndex));
    }

    /**
     * Core Bayesian calculation: What cards might opponent have?
     * Returns List to preserve duplicate information for accurate probability calculation
     */
    private List<String> calculateUnknownCards(List<Card> myHand, List<Card> playedCards,
                                               List<Card> currentTrick) {
        List<String> knownCards = new ArrayList<>();

        // Add all known cards (preserving duplicates)
        myHand.forEach(card -> knownCards.add(getCardName(card)));
        playedCards.forEach(card -> knownCards.add(getCardName(card)));
        currentTrick.forEach(card -> knownCards.add(getCardName(card)));

        // Unknown cards = All cards - Known cards (accounting for duplicates)
        List<String> unknownCards = new ArrayList<>(ALL_CARDS);
        for (String knownCard : knownCards) {
            unknownCards.remove(knownCard); // Remove only one instance
        }

        return unknownCards; // Keep as List to preserve duplicate count information
    }

    /**
     * Choose best card based on expected value calculation
     * Uses functional programming style for cleaner, more elegant code
     */
    private Card chooseBestCard(List<Card> validCards, List<String> unknownCards,
                                List<Card> currentTrick, String trumpSuit,
                                GameDataSnapshot snapshot) {

        return validCards.stream()
                .max(Comparator.comparingDouble(card ->
                        calculateExpectedValue(card, unknownCards, currentTrick,
                                trumpSuit, snapshot)))
                .orElse(validCards.get(0));
    }



    // Calculate the expected value used for algorithmic decision-making
    // based on probability, that is, the true score after deducting risks
    private double calculateExpectedValue(Card myCard, List<String> unknownCards,
                                          List<Card> currentTrick,
                                          String trumpSuit, GameDataSnapshot snapshot) {

        if (currentTrick.isEmpty()) {
            // Leading the trick - estimate probability opponent can beat our card
            return calculateLeadingExpectedValue(myCard, unknownCards, trumpSuit);
        } else {
            // Following the trick - calculate if we can/should win
            return calculateFollowingExpectedValue(myCard, currentTrick.get(0), unknownCards, trumpSuit);
        }
    }


    // Now use enhanced probability calculation and repetitive awareness
    private double calculateLeadingExpectedValue(Card leadCard, List<String> unknownCards, String trumpSuit) {
        // Use enhanced probability calculation that accounts for duplicates
        double beatProbability = calculateBeatProbability(leadCard, unknownCards, trumpSuit);
        double winProbability = 1.0 - beatProbability;

        // Expected value calculation:
        // Win: Get both our card + opponent's average card value
        // Lose: Lose our card value
        double ourCardValue = getCardValue(leadCard);
        double avgOpponentCardValue = calculateAverageCardValue(unknownCards);

        double expectedWinValue = ourCardValue + avgOpponentCardValue;
        double expectedLoseValue = -ourCardValue;

        return winProbability * expectedWinValue + (1 - winProbability) * expectedLoseValue;
    }

    /**
     * Expected value when following a trick
     * FIXED: Correctly calculates trump 9 value in expected value calculations
     */
    private double calculateFollowingExpectedValue(Card myCard, Card leadCard,
                                                   List<String> unknownCards, String trumpSuit) {

        boolean canWin = canCardWinTrick(myCard, leadCard, trumpSuit);

        if (canWin) {
            // We can win - calculate total trick value (both cards)
            double totalTrickValue = getCardValue(leadCard, trumpSuit) + getCardValue(myCard, trumpSuit);
            return totalTrickValue; // We gain both cards' values
        } else {
            // We can't win - we lose our card, opponent gains both
            double ourCardValue = getCardValue(myCard, trumpSuit);
            return -ourCardValue; // We lose our card value
        }
    }

    /**
     * Check if our card can win against the lead card
     */
    private boolean canCardWinTrick(Card myCard, Card leadCard, String trumpSuit) {
        String mySuit = getSuit(myCard);
        String leadSuit = getSuit(leadCard);
        int myRank = getRank(myCard);
        int leadRank = getRank(leadCard);

        // Same suit and higher rank
        if (mySuit.equals(leadSuit) && myRank > leadRank) return true;

        // Trump beats non-trump
        if (!leadSuit.equals(trumpSuit) && mySuit.equals(trumpSuit)) return true;

        return false;
    }

    // Helper methods
    private static List<String> generateAllCards() {
        List<String> cards = new ArrayList<>();

        for (String suit : SUITS) {
            for (int rank : RANK_VALUES) {
                for (int copy = 0; copy < COPIES_PER_CARD; copy++) {
                    cards.add(rank + suit); // e.g., "5S" for Ace of Spades
                }
            }
        }
        return cards;
    }

    private String getCardName(Card card) {
        return getRank(card) + getSuit(card);
    }

    private String getSuit(Card card) {
        return ((Suit) card.getSuit()).getSuitShortHand();
    }

    private int getRank(Card card) {
        return ((Rank) card.getRank()).getRankCardValue();
    }

    /**
     * Get card value with explicit trump suit (preferred method)
     * Correctly handles trump 9 special case (10 points instead of 0)
     */
    private int getCardValue(Card card, String trumpSuit) {
        Rank rank = (Rank) card.getRank();

        // Special case: Trump 9 has different value (10 points instead of 0)
        if (rank == Rank.NINE && isTrump(card, trumpSuit)) {
            return Rank.NINE_TRUMP; // 10 points for trump 9
        }

        return rank.getScoreValue(); // Standard score for all other cards
    }

    /**
     * Legacy method for backward compatibility - use getCardValue(Card, String) when possible
     * Note: This method cannot correctly calculate trump 9 value without trump suit context
     */
    private int getCardValue(Card card) {
        return ((Rank) card.getRank()).getScoreValue();
    }

    /**
     * Calculate average card value for unknown cards (for expected value calculation)
     * Uses existing Rank enum for accurate score calculation, no hardcoded values
     */
    private double calculateAverageCardValue(List<String> unknownCards) {
        if (unknownCards.isEmpty()) return CERTAIN_LOSS_PROBABILITY;

        double totalValue = unknownCards.stream()
                .mapToDouble(cardName -> {
                    int rankValue = Integer.parseInt(cardName.substring(0,
                            cardName.length() - RANK_SUBSTRING_END));
                    return getRankByValue(rankValue).getScoreValue();
                })
                .sum();

        return totalValue / unknownCards.size();
    }

    /**
     * Calculate probability that opponent has a specific card type
     * Accounts for the fact that each card type can have 0, 1, or 2 copies
     */
    private double calculateCardTypeProbability(List<String> unknownCards,
                                                String cardType) {
        if (unknownCards.isEmpty()) return CERTAIN_LOSS_PROBABILITY;

        long cardCount = unknownCards.stream()
                .mapToLong(card -> card.equals(cardType) ? 1 : 0)
                .sum();

        return (double) cardCount / unknownCards.size();
    }

    /**
     * Enhanced probability calculation for cards that can beat our lead
     * More precise than simple counting due to preserved duplicate information
     */
    private double calculateBeatProbability(Card leadCard,
                                            List<String> unknownCards,
                                            String trumpSuit) {
        String leadSuit = getSuit(leadCard);
        int leadRank = getRank(leadCard);

        if (unknownCards.isEmpty()) return CERTAIN_LOSS_PROBABILITY;

        long beatableCardCount = unknownCards.stream()
                .mapToLong(cardName -> {
                    String suit = cardName.substring(cardName.length() - RANK_SUBSTRING_END);
                    int rank = Integer.parseInt(cardName.substring(0, cardName.length() - RANK_SUBSTRING_END));

                    // Same suit and higher rank, or trump card (if lead is not trump)
                    if (suit.equals(leadSuit) && rank> leadRank) return 1;
                    if (!leadSuit.equals(trumpSuit) && suit.equals(trumpSuit)) return 1;
                    return 0;
                })
                .sum();

        return (double) beatableCardCount /unknownCards.size();
    }


    // pply heuristic rules to refine the Bayesian decision
    // These rules add strategic considerations beyond pure mathematical expectation
    //Loss minimization has highest priority when following and certain to lose
    private Card applyHeuristicRules(Card bayesianChoice, List<Card> validCards,
                                     List<String> unknownCards, List<Card> currentTrick,
                                     String trumpSuit, GameDataSnapshot snapshot) {

        // PRIORITY RULE: If following and certain to lose, minimize loss immediately
        if (!currentTrick.isEmpty()) {
            Card leadCard = currentTrick.get(0);
            boolean canWinTrick = validCards.stream()
                    .anyMatch(card -> canCardWinTrick(card, leadCard, trumpSuit));

            if (!canWinTrick) {
                // Certain loss - override all other considerations
                return findAbsoluteLowestValueCard(validCards, trumpSuit);
            }
        }

        // Rule 1: Conservative trump usage - avoid using trump unless high win probability (>TRUMP_CONSERVATION_THRESHOLD)
        Card trumpRefinedChoice = applyTrumpConservationRule(bayesianChoice, validCards,
                unknownCards, currentTrick, trumpSuit);

        // Rule 2: Early discard of low-value non-trump when win probability is low (<LOW_VALUE_DISCARD_THRESHOLD)
        Card lowValueRefinedChoice = applyLowValueDiscardRule(trumpRefinedChoice, validCards,
                unknownCards, currentTrick, trumpSuit);

        return lowValueRefinedChoice;
    }


    //  Conservative Trump Usage
    //Avoid using trump cards unless win probability is high
    private Card applyTrumpConservationRule(Card currentChoice, List<Card> validCards,
                                            List<String> unknownCards, List<Card> currentTrick,
                                            String trumpSuit) {

        if (!isTrump(currentChoice, trumpSuit)) {
            return currentChoice;}  // Not trump, no need to apply this rule

        // Calculate win probability for this trump card
        double winProbability;
        if (currentTrick.isEmpty()) {
            winProbability = CERTAIN_WIN_PROBABILITY-calculateBeatProbability(currentChoice, unknownCards, trumpSuit);
        } else {
            winProbability = canCardWinTrick(currentChoice, currentTrick.get(0), trumpSuit) ?
                    CERTAIN_WIN_PROBABILITY:CERTAIN_LOSS_PROBABILITY;
        }

        // // If the probability of winning is very low, try to find a good non-ace substitute
        if (winProbability < TRUMP_CONSERVATION_THRESHOLD) {
            Card bestNonTrump = findBestNonTrump(validCards,unknownCards
                    ,currentTrick,trumpSuit);
            //Card bestNonTrump = findBestNonTrump(validCards,unknownCards,currentTrick);
            if (bestNonTrump != null) {
                return bestNonTrump;
            }
        }

        return currentChoice; // Keep trump if no good alternative or high win probability
    }

    // Rule 2: Early Discard of Low-Value Non-Trump
    private Card applyLowValueDiscardRule(Card currentChoice,List<Card> validCards,
                                          List<String> unknownCards,List<Card> currentTrick,
                                          String trumpSuit){


        double winProbability; //Calculate the winning probability of the current selection



        if (currentTrick.isEmpty()) {
            winProbability = CERTAIN_WIN_PROBABILITY -calculateBeatProbability(currentChoice, unknownCards, trumpSuit);
        } else {
            winProbability = canCardWinTrick(currentChoice,currentTrick.get(0), trumpSuit) ?
                    CERTAIN_WIN_PROBABILITY:CERTAIN_LOSS_PROBABILITY;
        }

        // If win probability is very low, prioritize discarding low-value non-trump
        if (winProbability<LOW_VALUE_DISCARD_THRESHOLD) {
            Card lowestValueNonTrump = findLowestValueNonTrump(validCards, trumpSuit);
            if (lowestValueNonTrump !=null) {
                return lowestValueNonTrump;
            }
        }

        return currentChoice;
    }

    /**
     * Find the best non-trump card for strategic conservation of trump cards
     */
    private Card findBestNonTrump(List<Card> validCards, List<String> unknownCards,
                                  List<Card> currentTrick, String trumpSuit) {

        List<Card> nonTrumpCards = validCards.stream()
                .filter(card -> !isTrump(card, trumpSuit))
                .collect(Collectors.toList());

        if (nonTrumpCards.isEmpty()) {
            return null;
        }

        // Among non-trump cards, choose the one with best expected value
        return nonTrumpCards.stream()
                .max(Comparator.comparingDouble(card ->{
                    if (currentTrick.isEmpty()){
                        return calculateLeadingExpectedValue(card,unknownCards, trumpSuit);
                    } else {
                        return calculateFollowingExpectedValue(card,
                                currentTrick.get(0), unknownCards, trumpSuit);
                    }
                }))
                .orElse(nonTrumpCards.get(0));
    }

    /**
     * Find the lowest value non-trump card for strategic discarding
     * FIXED: Now correctly calculates card values including trump 9 special case
     */
    private Card findLowestValueNonTrump(List<Card> validCards,String trumpSuit) {
        return validCards.stream()
                .filter(card -> !isTrump(card, trumpSuit))
                .min(Comparator.comparingInt(card -> getCardValue(card,trumpSuit)))
                .orElse(null);
    }

    /**
     * Find the absolute lowest value card, strongly preferring non-trump over trump
     * This is critical for loss minimization - never waste trump when certain to lose
     * FIXED: Now correctly calculates trump 9 value (10 points, not 0)
     */
    private Card findAbsoluteLowestValueCard(List<Card> validCards,
                                             String trumpSuit) {
        // First priority: lowest value non-trump card
        Card lowestNonTrump = validCards.stream()
                .filter(card -> !isTrump(card,
                        trumpSuit))
                .min(Comparator.comparingInt(card ->  getCardValue(card,
                        trumpSuit)))
                .orElse(null);

        if (lowestNonTrump != null) {
            return lowestNonTrump; // Always prefer non-trump for losses
        }

        // Only if we ONLY have trump cards, choose the lowest value trump
        // IMPORTANT: This now correctly values trump 9 as 10 points, not 0
        return validCards.stream()
                .filter(card->isTrump(card,trumpSuit))
                .min(Comparator.comparingInt(card -> getCardValue(card,trumpSuit)))
                .orElse(validCards.get(0)); // Ultimate fallback
    }

    /**
     * Check if a card is a trump card
     */
    private boolean isTrump(Card card, String trumpSuit){
        return getSuit(card).equals(trumpSuit);
    }

    /**
     * Get Rank enum by its rankCardValue for proper score calculation
     * Uses existing Rank enum instead of hardcoded values for maintainability
     */
    private Rank getRankByValue(int rankCardValue) {
        for (Rank rank : Rank.values()){
            if (rankCardValue == rank.getRankCardValue() ){
                return rank;
            }
        }
        return Rank.NINE; // Default fallback
    }

    @Override
    public DecisionType getSupportedType(){
        return DecisionType.CARD_PLAY;
    }

    @Override
    public int getPriority(){
        return BAYESIAN_STRATEGY_PRIORITY; // Higher priority than basic smart strategy
    }

    @Override
    public void reset(){
        // No state to reset in this simplified approach
    }
}