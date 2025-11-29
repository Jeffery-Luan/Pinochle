/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package ai.strategy;

import ch.aplu.jcardgame.Card;
import protocolframework.*;
import protocolframework.Request.DecisionRequest;
import protocolframework.Request.GeneralCardDecisionRequest;
import protocolframework.decision.*;

import java.util.*;


/**
 * Essentially, there are multiple random strategies used at the end of the chain,
 * and I gave them the lowest priority. Since the random strategy is very simple,
 * I use a class reason polymorphism to avoid an explosion in the number of classes
 *Random fallback strategy - Implement the common DecisionStrategy interface
 *Using the most common Decision type, it can handle all types of decision requests
 *As the last link in the chain of responsibility, ensure that decisions can always be made
 */
public class RandomFallbackStrategy implements DecisionStrategy<Decision> {
    private static final int MAX_HAND_SIZE = 12;
    private final Random random = new Random();

    @Override
    @SuppressWarnings("unchecked")
    public Decision decide(DecisionRequest<Decision> request) {
        return switch (request.getType()) {
            case BID -> createRandomBidDecision(request);
            case CARD_PLAY, REVEALED_CARD_SELECTION, CARD_DISCARD -> createRandomCardDecision(request);
            default -> throw new UnsupportedOperationException("Unsupported decision type: " + request.getType());
        };
    }

    @Override
    public DecisionType getSupportedType() {
        return null;
    }

    @Override
    public boolean canHandle(DecisionRequest<Decision> request) {
        // Random strategies can handle all known types of decisions
        return request.getType() == DecisionType.BID ||
                request.getType() == DecisionType.CARD_PLAY ||
                request.getType() == DecisionType.REVEALED_CARD_SELECTION ||
                request.getType() == DecisionType.CARD_DISCARD;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getPriority() {
        return -1;
    }


    private BidDecision createRandomBidDecision(DecisionRequest<Decision> request) {

        String[] suits = {"S", "H", "D", "C"};
        String randomTrumpSuit = suits[random.nextInt(suits.length)];

        if (random.nextDouble() < 0.3) {
            return new BidDecision(true, 0, randomTrumpSuit);
        } else {
            int bidAmount = (random.nextInt(3) + 1) * 10; // 10, 20, or 30
            return new BidDecision(false, bidAmount, randomTrumpSuit);
        }
    }


    private GeneralCardDecision createRandomCardDecision(DecisionRequest<Decision> request) {
        DecisionRequest<?> rawRequest = request;
        GeneralCardDecisionRequest cardRequest = (GeneralCardDecisionRequest) rawRequest;

        List<Card> availableCards = cardRequest.getAvailableCards();

        if (availableCards.isEmpty()) {
            return new GeneralCardDecision(Collections.emptyList());
        }

        // Select strategies based on the type of decision
        switch (request.getType()) {
            case CARD_PLAY:
                return createRandomPlayDecision(availableCards);
            case REVEALED_CARD_SELECTION:
                return createRandomRevealDecision(availableCards);
            case CARD_DISCARD:
                return createRandomDiscardDecision(availableCards);
            default:
                return createRandomPlayDecision(availableCards);
        }
    }


    private GeneralCardDecision createRandomPlayDecision(List<Card> availableCards) {
        int selectedIndex = random.nextInt(availableCards.size());
        return new GeneralCardDecision(Arrays.asList(selectedIndex));
    }


    private GeneralCardDecision createRandomRevealDecision(List<Card> availableCards) {
        if (availableCards.size() <= 1) {
            return new GeneralCardDecision(Arrays.asList(0));
        }
        int selectedIndex = random.nextInt(Math.min(2, availableCards.size()));
        return new GeneralCardDecision(Arrays.asList(selectedIndex));
    }


    // Randomly select the cards to discard
    private GeneralCardDecision createRandomDiscardDecision(List<Card> availableCards) {
        if (availableCards.size() <= MAX_HAND_SIZE) {
            return new GeneralCardDecision(Collections.emptyList());
        }

        int cardsToDiscard = availableCards.size() - MAX_HAND_SIZE;
        List<Integer> discardIndices = new ArrayList<>();
        Set<Integer> selected = new HashSet<>();

        while (discardIndices.size() < cardsToDiscard && selected.size() < availableCards.size()) {
            int randomIndex = random.nextInt(availableCards.size());
            if (!selected.contains(randomIndex)) {
                selected.add(randomIndex);
                discardIndices.add(randomIndex);
            }
        }
        return new GeneralCardDecision(discardIndices);
    }
}