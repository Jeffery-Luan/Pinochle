/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
import ch.aplu.jcardgame.*;
import ch.aplu.jgamegrid.*;
import core.Rank;
import core.Suit;
import data.GameDataSnapshot;
import protocolframework.DecisionType;
import protocolframework.decision.GeneralCardDecision;

import java.util.*;

/**
First, remove the configuration file and test code.
There are only over 200 lines of code in this class

To ensure the absolute correctness of the configuration file (for testing),
there is a lot of code that is only used for the configuration file later and can be removed

This is our cruel game mode class. It is only responsible for business logic and
 does not participate in specific service-level tasks

For example, tasks such as specifically showing cards, removing cards, and
adding cards are not business logic. They are provided as services through agents
instead of being handled by the business logic of the game

Is this the reason why the code of my class is very concise
 */
public class CutThroatGameMode implements GameMode {
    private static final int PLAYER_COUNT = 2;
    private static final int MAX_HAND_SIZE = 12;

    private static final int CENTER_X = 350;
    private static final int CENTER_Y = 350;

    private GameModeServiceProxy serviceProxy;
    private final Set<String> configDistributedCards = new HashSet<>();

    private final Map<String, Integer> revealedCardOwners = new HashMap<>();

    @Override
    public String getModeName() {
        return "cutthroat";
    }

    @Override
    public void initialize(GameModeServiceProxy serviceProxy) {
        this.serviceProxy = serviceProxy;
    }

    @Override
    public void execute(GamePhase phase, GameDataSnapshot snapshot, int playerIndex) {
        switch (phase) {
            case POST_BIDDING:
                executeRevealAndDistribute(snapshot);
                break;
            case PRE_MELDING:
                executeFinalCardSelection(snapshot);
                break;
        }
    }

    @Override
    public void cleanup() {
        configDistributedCards.clear();
        revealedCardOwners.clear();
        serviceProxy.updateGameStatus("");
        serviceProxy.forceRefreshAllHands();
    }

    @Override
    public boolean isApplicable(GamePhase phase) {
        return phase == GamePhase.POST_BIDDING || phase == GamePhase.PRE_MELDING;
    }

    public void setServiceProxy(GameModeServiceProxy serviceProxy) {
        this.serviceProxy = serviceProxy;
    }


    // Carry out the card-flipping selection and distribution process
    private void executeRevealAndDistribute(GameDataSnapshot snapshot) {
        int bidWinnerIndex = snapshot.getBidWinnerIndex();
        int dealerIndex = 1 - bidWinnerIndex;

        serviceProxy.updateGameStatus("Dealer (Player " + dealerIndex + ") flipping top two cards...");
        List<Card> revealedCards = drawTwoCardsFromPack();

        // Display the card flip
        serviceProxy.displayCards(revealedCards, new Location(CENTER_X, CENTER_Y));

        if (isAutoMode() && hasExtraCardsConfig()) {
            distributeCardsByConfig(revealedCards);
        } else {
            distributeCardsByChoice(revealedCards, bidWinnerIndex);
        }

        // Distribute the remaining pile of cards
        distributeRemainingPack(bidWinnerIndex);

        serviceProxy.updateGameStatus("Cut-throat distribution phase completed!");
    }


    // Draw two cards from the deck
    private List<Card> drawTwoCardsFromPack() {
        List<Card> drawnCards = new ArrayList<>();

        Card firstCard = serviceProxy.drawFromPack();
        if (firstCard != null) drawnCards.add(firstCard);

        Card secondCard = serviceProxy.drawFromPack();
        if (secondCard != null) drawnCards.add(secondCard);

        return drawnCards;
    }

    // Select-driven card flipping distribution
    private void distributeCardsByChoice(List<Card> revealedCards, int bidWinnerIndex) {
        serviceProxy.updateGameStatus("Cut-throat mode: Bid winner (Player " + bidWinnerIndex + ") choose one card");

        // Get a choice
        int selectedIndex = getPlayerChoice(revealedCards, bidWinnerIndex);

        if (selectedIndex < 0 || selectedIndex >= revealedCards.size()) {
            selectedIndex = 0;
        }

        Card selectedCard = revealedCards.get(selectedIndex);
        Card remainingCard = revealedCards.get(1 - selectedIndex);
        int dealerIndex = 1 - bidWinnerIndex;

        // Release and distribute the cards
        selectedCard.removeFromHand(false);
        remainingCard.removeFromHand(false);

        serviceProxy.addCardToHand(selectedCard, bidWinnerIndex);
        serviceProxy.addCardToHand(remainingCard, dealerIndex);

        serviceProxy.forceRefreshAllHands();

        String winnerName = bidWinnerIndex == serviceProxy.getComputerPlayerIndex() ? "Computer" : "Human";
        String dealerName = dealerIndex == serviceProxy.getComputerPlayerIndex() ? "Computer" : "Human";
        serviceProxy.updateGameStatus(winnerName + " received the selected card, " + dealerName + " received the remaining card");
    }

    // Obtain player choices
    private int getPlayerChoice(List<Card> revealedCards, int bidWinnerIndex) {
        if (isAutoMode()) {
            return getAutoModeChoice(bidWinnerIndex);
        }

        if (bidWinnerIndex == serviceProxy.getComputerPlayerIndex()) {
            return getAIChoice(revealedCards);
        } else {
            return getHumanChoice(revealedCards);
        }
    }

    // AI selection logic
    private int getAIChoice(List<Card> revealedCards) {
        serviceProxy.updateGameStatus("Computer is analyzing the revealed cards...");

        GeneralCardDecision decision = serviceProxy.requestAIDecision(
                DecisionType.REVEALED_CARD_SELECTION, revealedCards);

        List<Integer> indices = decision.getSelectedIndices();
        int selectedIndex = indices.isEmpty() ? 0 : indices.get(0);

        if (selectedIndex >= 0 && selectedIndex < revealedCards.size()) {
            Card selectedCard = revealedCards.get(selectedIndex);
            Rank rank = (Rank) selectedCard.getRank();
            Suit suit = (Suit) selectedCard.getSuit();
            serviceProxy.updateGameStatus("Computer selected: " + rank.getCardLog() + suit.getSuitShortHand());
        }

        return selectedIndex;
    }


    // Human player choice
    private int getHumanChoice(List<Card> revealedCards) {
        serviceProxy.updateGameStatus("Double-click one of the revealed cards to select it");
        return serviceProxy.waitForUserCardSelection(revealedCards);
    }


    // Automatic mode selection
    private int getAutoModeChoice(int bidWinnerIndex) {
        String choiceKey = "players." + bidWinnerIndex + ".cutthroat_choice";
        String choice = serviceProxy.getConfigProperty(choiceKey, "0");

        try {
            int selectedIndex = Integer.parseInt(choice);
            return Math.max(0, Math.min(1, selectedIndex));
        } catch (NumberFormatException e) {
            return 0;
        }
    }


    // Distribute the remaining pile of cards
    private void distributeRemainingPack(int bidWinnerIndex) {
        if (isAutoMode()) {
            distributeByConfiguration(bidWinnerIndex);
        } else {
            distributeAlternately(bidWinnerIndex);
        }

        serviceProxy.forceRefreshAllHands();
    }



    // Distribute the remaining piles of cards alternately
    private void distributeAlternately(int startPlayerIndex) {
        int currentPlayer = startPlayerIndex;
        GameDataSnapshot snapshot = serviceProxy.getSnapshot();

        while (snapshot.getPackSize() > 0) {
            Card card = serviceProxy.drawFromPack();
            if (card != null) {
                serviceProxy.addCardToHand(card, currentPlayer);
                currentPlayer = (currentPlayer + 1) % PLAYER_COUNT;
            }
            snapshot = serviceProxy.getSnapshot();
        }
    }

    // Execute the final hand card selection
    private void executeFinalCardSelection(GameDataSnapshot snapshot) {
        serviceProxy.updateGameStatus("FINAL CARD SELECTION PHASE");
        serviceProxy.updateGameStatus("Each player must now select their best 12 cards from their 24 cards...");

        for (int playerIndex = 0; playerIndex < PLAYER_COUNT; playerIndex++) {
            String playerName = playerIndex == serviceProxy.getComputerPlayerIndex() ? "Computer" : "Human";
            serviceProxy.updateGameStatus("Player " + playerIndex + " (" + playerName + ") is selecting final cards...");

            selectFinalCardsForPlayer(playerIndex);
        }

        serviceProxy.updateGameStatus("CUT-THROAT MODE COMPLETED");
    }

    // Select the final hand card for the designated player
    private void selectFinalCardsForPlayer(int playerIndex) {
        GameDataSnapshot currentSnapshot = serviceProxy.getSnapshot();
        List<Card> playerCards = currentSnapshot.getPlayerHand(playerIndex);

        int currentSize = playerCards.size();
        int targetSize = MAX_HAND_SIZE;

        if (currentSize <= targetSize) {
            return;
        }

        if (isAutoMode()) {
            selectByConfiguration(playerIndex, playerCards);
        } else if (playerIndex == serviceProxy.getComputerPlayerIndex()) {
            selectByAI(playerIndex, playerCards);
        } else {
            selectByHuman(playerIndex, currentSize - targetSize);
        }
    }

    // Select the final hand card through AI
    private void selectByAI(int playerIndex, List<Card> playerCards) {
        GeneralCardDecision decision = serviceProxy.requestAIDecision(
                DecisionType.CARD_DISCARD, playerCards);

        List<Integer> discardIndices = decision.getSelectedIndices();
        discardIndices.sort(Collections.reverseOrder());

        for (int index : discardIndices) {
            if (index >= 0 && index < playerCards.size()) {
                serviceProxy.removeCardFromHand(playerCards.get(index), playerIndex);
            }
        }
    }

    // The final hand cards are selected by human players
    private void selectByHuman(int playerIndex, int discardCount) {
        GameDataSnapshot snapshot = serviceProxy.getSnapshot();
        Hand playerHand = snapshot.getHands()[playerIndex];
        serviceProxy.enableCardDiscardMode(playerHand, discardCount);
    }

    // Check whether it is in automatic mode
    private boolean isAutoMode() {
        return "true".equals(serviceProxy.getConfigProperty("isAuto", "false"));
    }

    // Get the card name
    private String getCardName(Card card) {
        Rank rank = (Rank) card.getRank();
        Suit suit = (Suit) card.getSuit();
        return rank.getCardLog() + suit.getSuitShortHand();
    }


//===================================================
    //===================================================
    //===================================================
    // JUST FOR CONFIG VERSION (can remove)

    //Read the original configuration (without filtering any cards)
    private Map<Integer, List<String>> readPlayerExtraCardsOriginal() {
        Map<Integer, List<String>> playerCards = new HashMap<>();

        for (int i = 0; i < PLAYER_COUNT; i++) {
            String extraCardsKey = "players." + i + ".extra_cards";
            String extraCards = serviceProxy.getConfigProperty(extraCardsKey, "");

            if (!extraCards.isEmpty()) {
                List<String> cardList = Arrays.asList(extraCards.split(","));
                List<String> cleanedCardList = new ArrayList<>();
                for (String card : cardList) {
                    cleanedCardList.add(card.trim());
                }
                playerCards.put(i, cleanedCardList);
            }
        }

        return playerCards;
    }


    // Select the final hand card through configuration
    private void selectByConfiguration(int playerIndex, List<Card> playerCards) {
        String finalCardsKey = "players." + playerIndex + ".final_cards";
        String finalCards = serviceProxy.getConfigProperty(finalCardsKey, "");

        if (finalCards.isEmpty()) {
            selectByAI(playerIndex, playerCards);
            return;
        }

        // Parse the cards to be retained in the configuration
        List<String> cardsToKeep = Arrays.asList(finalCards.split(","));
        List<String> keepList = new ArrayList<>();
        for (String cardName : cardsToKeep) {
            keepList.add(cardName.trim());
        }

        // Mark the cards to be kept
        boolean[] shouldKeep = new boolean[playerCards.size()];
        List<String> remainingKeepList = new ArrayList<>(keepList);

        for (int i = 0; i < playerCards.size(); i++) {
            Card card = playerCards.get(i);
            String cardName = getCardName(card);

            if (remainingKeepList.contains(cardName)) {
                shouldKeep[i] = true;
                remainingKeepList.remove(cardName);
            }
        }

        // Select the final hand card through AI
        List<Card> cardsToRemove = new ArrayList<>();
        for (int i = 0; i < playerCards.size(); i++) {
            if (!shouldKeep[i]) {
                cardsToRemove.add(playerCards.get(i));
            }
        }

        for (Card card : cardsToRemove) {
            serviceProxy.removeCardFromHand(card, playerIndex);
        }
    }


    // Distribute the remaining deck as configured
    // only skip the cards allocated to yourself
    private void distributeByConfiguration(int bidWinnerIndex) {

        Map<Integer, List<String>> playerExtraCards = readPlayerExtraCardsOriginal();

        if (playerExtraCards.isEmpty()) {
            distributeAlternately(bidWinnerIndex);
            return;
        }

        int totalDistributed = 0; //DEBUG
        for (Map.Entry<Integer, List<String>> entry : playerExtraCards.entrySet()) {
            int playerIndex = entry.getKey();
            List<String> cardNames = entry.getValue();

            for (String cardName : cardNames) {
                String trimmedCardName = cardName.trim();

                if (revealedCardOwners.containsKey(trimmedCardName) &&
                        revealedCardOwners.get(trimmedCardName).equals(playerIndex)) {
                    continue;
                }

                Card card = serviceProxy.findAndDrawCard(trimmedCardName);
                serviceProxy.addCardToHand(card, playerIndex);
                totalDistributed++;

            }
        }
        distributeAlternately(bidWinnerIndex);
    }

    // Look for the card ownership in the configuration
    private Integer findCardOwnerInConfig(String cardName, Map<Integer, List<String>> playerExtraCards) {
        for (Map.Entry<Integer, List<String>> entry : playerExtraCards.entrySet()) {
            int playerIndex = entry.getKey();
            List<String> cardNames = entry.getValue();

            for (String configCard : cardNames) {
                if (configCard.trim().equals(cardName)) {
                    return playerIndex;
                }
            }
        }
        return null;
    }



    // Check if there are any additional card configurations
    private boolean hasExtraCardsConfig() {
        for (int i = 0; i < PLAYER_COUNT; i++) {
            String extraCardsKey = "players." + i + ".extra_cards";
            String extraCards = serviceProxy.getConfigProperty(extraCardsKey, "");
            if (!extraCards.isEmpty()) {
                return true;
            }
        }
        return false;
    }


    //Configure the driver to distribute the two cards flipped out
    //Record the specific ownership of each card
    private void distributeCardsByConfig(List<Card> revealedCards) {
        serviceProxy.updateGameStatus("Distributing revealed cards by configuration...");

        Card firstCard = revealedCards.get(0);
        Card secondCard = revealedCards.get(1);

        String firstCardName = getCardName(firstCard);
        String secondCardName = getCardName(secondCard);

        Map<Integer, List<String>> playerExtraCards = readPlayerExtraCardsOriginal();

        Integer firstCardOwner = findCardOwnerInConfig(firstCardName, playerExtraCards);
        Integer secondCardOwner = findCardOwnerInConfig(secondCardName, playerExtraCards);

        firstCard.removeFromHand(false);
        secondCard.removeFromHand(false);

        // Distribute as configured and record the attribution
        if (firstCardOwner != null) {
            serviceProxy.addCardToHand(firstCard, firstCardOwner);
            revealedCardOwners.put(firstCardName, firstCardOwner);
        } else {
            serviceProxy.addCardToHand(firstCard, 0);
            revealedCardOwners.put(firstCardName, 0);
        }

        if (secondCardOwner != null) {
            serviceProxy.addCardToHand(secondCard, secondCardOwner);
            revealedCardOwners.put(secondCardName, secondCardOwner);
        } else {
            serviceProxy.addCardToHand(secondCard, 1);
            revealedCardOwners.put(secondCardName, 1);
        }

        configDistributedCards.add(firstCardName);
        configDistributedCards.add(secondCardName);

        serviceProxy.forceRefreshAllHands();

    }

}
