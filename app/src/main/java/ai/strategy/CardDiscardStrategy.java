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
import protocolframework.Request.DecisionRequest;
import protocolframework.Request.GeneralCardDecisionRequest;
import protocolframework.decision.GeneralCardDecision;

import java.util.*;
import protocolframework.*;

/**
 * This strategy handles card discard decisions in Cut-throat mode.
 * It determines which cards to discard to reduce hand size to 12 cards.
 * The logic was restructured and migrated from the original CutThroatStrategy.
 * The strategy brain for card discarding, which specifically handles the
 * excess card removal and implements the DecisionStrategy interface,
 * can be dynamically loaded into the AI system
 */
public class CardDiscardStrategy implements DecisionStrategy<GeneralCardDecision> {

    /**
     * Carry out card discard decision.
     * Determines which cards to discard from the current hand to reach 12 cards.
     *
     * @param request includes the current hand cards
     * @return a decision with indices of cards to discard
     */
    @Override
    public GeneralCardDecision decide(DecisionRequest<GeneralCardDecision> request) {
        GeneralCardDecisionRequest cardRequest = (GeneralCardDecisionRequest) request;

        List<Card> currentHand = cardRequest.getAvailableCards();
        String trumpSuit = cardRequest.getSnapshot().getTrumpSuit();

        if (currentHand.size() <= 12) {
            return new GeneralCardDecision(Collections.emptyList());
        }

        int cardsToDiscard = currentHand.size() - 12;
        List<Integer> discardIndices = selectCardsToDiscard(currentHand, trumpSuit, cardsToDiscard);

        return new GeneralCardDecision(discardIndices);
    }

    @Override
    public DecisionType getSupportedType() {
        return DecisionType.CARD_DISCARD;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    /**
     * Select cards to discard, keeping the best cards.
     *
     * @param allCards current hand cards
     * @param trumpSuit trump suit used for evaluation
     * @param discardCount number of cards to discard
     * @return the list of indices to be discarded
     */
    private List<Integer> selectCardsToDiscard(List<Card> allCards, String trumpSuit, int discardCount) {
        if (discardCount <= 0) {
            return Collections.emptyList();
        }

        Map<String, List<CardWithIndex>> suitGroups = groupCardsBySuit(allCards);

        List<String> nonTrumpSuits = new ArrayList<>();
        for (String suit : suitGroups.keySet()) {
            if (!suit.equals(trumpSuit)) {
                nonTrumpSuits.add(suit);
            }
        }

        //Sort by the number of cards of suit and color, and discard the
        // fewer ones first
        // If the quantity is the same, sort in reverse alphabetical order to ensure a deterministic result
        nonTrumpSuits.sort((a, b) -> {
            int sizeCompare = Integer.compare(suitGroups.get(a).size(), suitGroups.get(b).size());
            if (sizeCompare != 0) {
                return sizeCompare;
            }
           return b.compareTo(a);
        });

        List<Integer> discardIndices = new ArrayList<>();

        // Discard non-ace suits according to their priority
        for (String suit : nonTrumpSuits) {
            if (discardIndices.size() >= discardCount) break;

            List<CardWithIndex> suitCards = new ArrayList<>(suitGroups.get(suit));

            // Sort the cards in ascending order of rankCardValue within the suit,
            // and prepare to discard the cards with high rankCardValue
            suitCards.sort(Comparator.comparingInt(a -> ((Rank) a.card.getRank()).getRankCardValue()));

            // Discard starting from the entire suit (prioritize discarding cards with high rankCardValue)
            for (CardWithIndex cardWithIndex : suitCards) {
                if (discardIndices.size() >= discardCount) break;
                discardIndices.add(cardWithIndex.index);
            }
        }

        //If you still need to discard, discard from the ace suit
        if (discardIndices.size() < discardCount && suitGroups.containsKey(trumpSuit)) {
            List<CardWithIndex> trumpCards = new ArrayList<>(suitGroups.get(trumpSuit));

            // The ace cards are sorted in descending order of rankCardValue,
            // and the cards with high rankCardValue are discarded
            trumpCards.sort((a, b) -> Integer.compare(
                    ((Rank) b.card.getRank()).getRankCardValue(),
                    ((Rank) a.card.getRank()).getRankCardValue()
            ));

            for (CardWithIndex cardWithIndex : trumpCards) {
                if (discardIndices.size() >= discardCount) break;
                discardIndices.add(cardWithIndex.index);
            }
        }

        return discardIndices;
    }

    // Group the cards by suit while retaining the original index
    private Map<String, List<CardWithIndex>> groupCardsBySuit(List<Card> allCards) {
        Map<String, List<CardWithIndex>> suitGroups = new HashMap<>();

        for (int i = 0; i < allCards.size(); i++) {
            Card card = allCards.get(i);
            Suit suit = (Suit) card.getSuit();
            String suitName = suit.getSuitShortHand();

            CardWithIndex cardWithIndex = new CardWithIndex(card, i);
            suitGroups.computeIfAbsent(suitName, k -> new ArrayList<>()).add(cardWithIndex);
        }

        return suitGroups;
    }

    /**
         * Internal utility class that wraps a card with its original index and evaluated value.
         * Used to track card positions when sorting and selecting cards to discard.
         */
        private record CardWithIndex(Card card, int index) {
    }
}