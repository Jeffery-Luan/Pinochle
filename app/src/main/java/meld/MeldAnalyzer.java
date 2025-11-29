/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package meld;

import ch.aplu.jcardgame.Card;
import java.util.*;

/**
 Combo card optimization algorithm engine
 Responsibility: Specifically implement the algorithm logic for finding the optimal combination
 */
public class MeldAnalyzer {
    private final List<MeldChecker> checkers;

    /**
     * Constructor: Initialized through the configuration file
     * @param jsonFile JSON configuration file path
     */
    public MeldAnalyzer(String jsonFile, Properties properties) {
        this.checkers = MeldConfigManager.loadCheckers(jsonFile, properties);
    }

    /**
     * Constructor: Initialize directly using the inspector list
     * @param checkers List of pre-configured checkers
     */
    public MeldAnalyzer(List<MeldChecker> checkers){this.checkers = new ArrayList<>(checkers);}

    /**
     Calculate the optimal combination score of the given cards
     This is the main interface method for the outside
     * @param cards player hand cards
     @return the total score of the optimal combination
     */
    public int calculateBestScore(List<Card> cards, String trumpSuit) {
        return findOptimalCombination(new ArrayList<>(cards),trumpSuit,0);
    }

    /**
     The core implementation of the backtracking algorithm: Recursive search for the optimal combination
     * @param availableCards Currently available cards
     * @param currentScore The score has been obtained currently
     * @return the maximum score that can be obtained from the current state
     */
    private int findOptimalCombination(List<Card> availableCards, String trumpSuit, int currentScore) {

        int bestScore =currentScore; // Baseline situation: The current score is a possible result

        // Try each combination card checker
        for (MeldChecker checker : checkers) {
            if (checker.canFormMeld(availableCards, trumpSuit)) {
                // Select this combination of cards
                List<Card> remainingCards =
                        checker.removeUsedCards(availableCards, trumpSuit);
                // Recursively search for the optimal combination of the remaining cards
                int totalScore = findOptimalCombination(remainingCards, trumpSuit,
                        currentScore+checker.getScore());
                // Update the optimal score
                if (totalScore > bestScore) {bestScore = totalScore;}
            }
        }
        return bestScore;
    }

    /**
     Obtain all available combination card types
     @return Combo card checker list
     */
    public List<MeldChecker> getAvailableMelds() {
        return new ArrayList<>(checkers);
    }
}