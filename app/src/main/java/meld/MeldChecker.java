/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package meld;

import ch.aplu.jcardgame.Card;
import core.Rank;
import core.Suit;

import java.util.*;

/**
 A single combination card processor
 Responsibility: Specifically handle all the logic of a single combination of cards
 */
public class MeldChecker {

    private static final int EXPECTED_PATTERN_PARTS = 3;

    private final String name;    // Combination card name
    private final int score;   // Combination card score
    private final String cardPattern;// Card mode string

    /**
     Constructor
     * @param name Combination card names, such as "Double Run"
     * @param score Combo card scores, such as 1500
     * @ param cardPattern card mode, such as "10: TRUMP: 2, J: TRUMP: 2, Q: TRUMP: 2 K: TRUMP: 2, A: TRUMP: 2"
     */
    public MeldChecker(String name,int score,String cardPattern) {

        this.score = score;
        this.name = name;

        this.cardPattern = cardPattern;
    }

    /**
     Check whether this combination of cards can be formed
     * @param cards Available card list
     * @return Can form a combo card
     */
    public boolean canFormMeld(List<Card> cards, String trumpSuit) {
        List<String> requiredCards = parseRequiredCards(trumpSuit);
        return hasAllRequiredCards(cards,
                requiredCards);
    }

    /**
     Remove the cards used to form the combination
     * @param cards Original card list
     * @return removes the remaining card list after using the card
     */
    public List<Card> removeUsedCards(List<Card> cards, String trumpSuit) {
        if (!canFormMeld(cards, trumpSuit)){
            return  new ArrayList<>(cards);
        }

        List<String> requiredCards = parseRequiredCards(trumpSuit);
        return removeCardsFromList(cards,requiredCards);
    }

    /**
     Parse the card pattern string and generate the required card list
     @return required card names list
     */
    private List<String> parseRequiredCards(String trumpSuit) {
        List<String> requiredCards = new ArrayList<>();

        // Divide the pattern string (cardPattern) of the combination cards into
        // multiple separate pattern units based on commas (,)
        String[] patterns = cardPattern.split( ",");

        for (String pattern : patterns) {
            String[] parts = pattern.trim().split(":");
            if (EXPECTED_PATTERN_PARTS  !=parts.length ) {continue;}

            String rank = parts[0].trim();
            String suit = parts[1].trim();
            int count;

            try {
                count = Integer.parseInt(parts[2].trim());
            } catch (NumberFormatException e) {
                System.err.println("Invalid count in pattern: " + pattern);
                continue;
            }

            // Handle special pattern marks
            String actualSuit = resolveSuit(suit, trumpSuit);

            // Generate the required card names
            for (int i =0;i< count;i++) {
                requiredCards.add(rank +actualSuit);}
        }

        // THE card names list
        return requiredCards;
    }

    /**
     It is used to convert to the actual suit through the card string
     * The pattern marking in the @param suit configuration
     * @param trumpSuit Current ace suit
     * @return the actual suit
     */
    private String resolveSuit(String suit, String trumpSuit) {
        if ("TRUMP".equals(suit)) {return trumpSuit;}
        return suit; // Other suits return directly, such as S, H, D, C
    }

    /**
     Check whether all the required cards are included in the card list
     * @param cards Available card list
     * @param requiredCards List of required card names
     * @return Does it include all the required cards
     */
    private boolean hasAllRequiredCards(List<Card> cards, List<String> requiredCards) {
        // Convert the Card object to a string representation
        List<String> availableCards = new ArrayList<>();
        for (Card card : cards) {
            availableCards.add(convertCardToString(card));
        }

        // Check whether each required card exists (repetition is allowed)
        List<String> tempRequired = new ArrayList<>(requiredCards);
        for (String available : availableCards) {
            tempRequired.remove(available); // Remove the found card (only remove one)
        }

        return tempRequired.isEmpty(); // If all the required cards have been found, the list should be empty
    }

    /**
     Remove the specified card from the card list
     * @param cards Original card list
     * @param cardsToRemove List of card names to be removed
     * @return the list of remaining cards after removing the specified card
     */
    private List<Card> removeCardsFromList(List<Card> cards, List<String> cardsToRemove) {
        List<Card> result = new ArrayList<>();
        List<String> tempCardsToRemove = new ArrayList<>(cardsToRemove);

        for (Card card : cards) {
            String cardName = convertCardToString(card);
            if (tempCardsToRemove.contains(cardName)) {
                tempCardsToRemove.remove(cardName); // Remove a matching card
            } else {result.add(card); } // Keep unused cards
        }
        return result;
    }

    /**
     Convert the Card object to a string representation
     Reuse the logic in Pinochle in the format: rank + suit
     @param card Card object
     @return Card string representation, such as "AH", "KS", "10D"
     */
    private String convertCardToString(Card card) {
        Suit suit = (Suit) card.getSuit();
        Rank rank = (Rank) card.getRank();
        return rank.getRankCardValue() + suit.getSuitShortHand();
    }

    // Public access method
    // Getter and Setter
    public String getName() {
        return name;
    }

    public int getScore() {
        return score;
    }

    public String getCardPattern() {
        return cardPattern;
    }
}