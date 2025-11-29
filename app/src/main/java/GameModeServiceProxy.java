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
 It is mainly responsible for providing service agents and complex internal systems
 It will provide a bunch of atomic operations for DLC or other mode plugins to use.
 And in combination with the permission mode, it prevents other modes from obtaining
 excessive permissions and provides fine-grained control methods

 Please note that its focus is not on data exchange but on the provision of services
 */
public class GameModeServiceProxy {
    private static final int SHOW_DELAY_MS = 1000;
    private static final int DEFAULT_DELAY_MS = 50;
    private static final int SHORT_DELAY_MS = 10;
    private static final int ROW_LAYOUT_WIDTH = 200;
    private static final int SELECTION_X = 350;
    private static final int SELECTION_Y = 350;
    private static final int UNSELECTED_INDEX = -1;

    private final Pinochle pinochleSystem;
    private final int authorizedPlayerIndex;
    private final Set<String> permissions;

    public GameModeServiceProxy(Pinochle pinochleSystem, Set<String> permissions) {
        this.pinochleSystem = pinochleSystem;
        this.authorizedPlayerIndex = pinochleSystem.getBidWinPlayerIndex();
        this.permissions = new HashSet<>(permissions);
    }

    // Game snapshots can prevent the main system from being modified when obtaining data
    public GameDataSnapshot getSnapshot() {
        validatePermission("READ_GAME_DATA");
        return pinochleSystem.createSnapshot();
    }

    public String getConfigProperty(String key, String defaultValue) {
        validatePermission("READ_CONFIG");
        return pinochleSystem.getProperties().getProperty(key, defaultValue);
    }


    // Draw a card from the deck (remove and return)
    public Card drawFromPack() {
        validatePermission("MODIFY_PACK");
        Hand pack = pinochleSystem.getPack();

        if (pack.isEmpty()) {
            return null;
        }

        Card card = pack.getCardList().get(0);
        card.removeFromHand(true);
        return card;
    }

    // Search and remove from the card pile based on the card name
    public Card findAndDrawCard(String cardName) {
        validatePermission("MODIFY_PACK");
        Hand pack = pinochleSystem.getPack();

        for (Card card : pack.getCardList()) {
            if (getCardName(card).equals(cardName)) {
                card.removeFromHand(true);
                return card;
            }
        }
        return null;
    }


    // Hand card operation service Atomic operation

    // Add cards to the designated player's hand
    public void addCardToHand(Card card, int playerIndex) {
        validatePermission("MODIFY_HANDS");
        Hand[] hands = pinochleSystem.getHands();

        if (card != null && playerIndex >= 0 && playerIndex < hands.length) {
            hands[playerIndex].insert(card, true);
            forceRefreshAllHands();
            performDelay(DEFAULT_DELAY_MS);
        }
    }

    // Remove the card from the hand of the designated player
    public void removeCardFromHand(Card card, int playerIndex) {
        validatePermission("MODIFY_HANDS");

        if (card != null) {
            card.removeFromHand(true);
            forceRefreshAllHands();
            performDelay(DEFAULT_DELAY_MS);
        }
    }

    // Proxy service for general AI module services
    // Use a unified protocol
    public GeneralCardDecision requestAIDecision(DecisionType decisionType, List<Card> availableCards) {
        validatePermission("USE_AI_SERVICE");
        return pinochleSystem.requestAIDecision(authorizedPlayerIndex, decisionType, availableCards);
    }

    // General game system status update
    public void updateGameStatus(String message) {
        validatePermission("CONTROL_DISPLAY");
        pinochleSystem.setStatus(message);
    }

    // General system latency service
    public void performDelay(int milliseconds) {
        validatePermission("CONTROL_TIMING");
        pinochleSystem.delay(milliseconds);
    }


    // Universal card display service
    public void displayCards(List<Card> cards, Location displayLocation) {
        validatePermission("CONTROL_DISPLAY");

        Hand tempHand = new Hand(pinochleSystem.getDeck());
        for (Card card : cards) {
            tempHand.insert(card, true);
        }

        RowLayout layout = new RowLayout(displayLocation, ROW_LAYOUT_WIDTH);
        tempHand.setView(pinochleSystem, layout);
        tempHand.draw();
        performDelay(SHOW_DELAY_MS);
    }



    //Wait for the user to select one card from multiple cards and return the index of the selected card
    public int waitForUserCardSelection(List<Card> availableCards) {

        final int[] selectedIndex = {UNSELECTED_INDEX};
        final boolean[] selectionDone = {false};

        Hand selectionHand = new Hand(pinochleSystem.getDeck());
        for (Card card : availableCards) {
            selectionHand.insert(card, true);
        }

        RowLayout layout = new RowLayout(new Location(SELECTION_X, SELECTION_Y), ROW_LAYOUT_WIDTH);
        selectionHand.setView(pinochleSystem, layout);
        selectionHand.draw();

        CardListener selectionListener = new CardAdapter() {

            @Override
            public void leftDoubleClicked(Card card) {

                for (int i = 0; i < availableCards.size(); i++) {
                    if (availableCards.get(i).equals(card)) {
                        selectedIndex[0] = i;
                        selectionDone[0] = true;
                        break;
                    }
                }
            }
        };

        selectionHand.addCardListener(selectionListener);
        selectionHand.setTouchEnabled(true);

        while (!selectionDone[0]) {
            pinochleSystem.delay(DEFAULT_DELAY_MS);
        }

        selectionHand.setTouchEnabled(false);
        return selectedIndex[0];
    }

    // The classic operation of card games allows players to remove their cards
    public void enableCardDiscardMode(Hand playerHand, int discardCount) {
        validatePermission("HANDLE_USER_INPUT");

        final int[] remainingDiscards = {discardCount};
        final boolean[] discardComplete = {false};

        CardListener discardListener = new CardAdapter() {
            @Override
            public void leftDoubleClicked(Card card) {
                if (remainingDiscards[0] > 0) {
                    playerHand.setTouchEnabled(false);
                    card.removeFromHand(false);
                    remainingDiscards[0]--;
                    playerHand.draw();

                    performDelay(SHORT_DELAY_MS);
                    if (remainingDiscards[0] > 0) {
                        updateGameStatus("Please discard " + remainingDiscards[0] + " more cards.");
                        playerHand.setTouchEnabled(true);
                    } else {
                        updateGameStatus("Card selection completed!");
                        discardComplete[0] = true;
                        restoreNormalGameListener(playerHand);
                    }
                }
            }
        };

        playerHand.addCardListener(discardListener);
        playerHand.setTouchEnabled(true);
        updateGameStatus("Double-click " + discardCount + " cards to DISCARD them.");

        while (!discardComplete[0]) {
            pinochleSystem.delay(SHORT_DELAY_MS);
        }

        playerHand.draw();
        // Clear the state and let the main process take over
        updateGameStatus("");
        restoreNormalGameListener(playerHand);
    }


     //Restore the normal game listener and create a game listener that is
    // exactly the same as the main class
     //In fact,
    //this is because the main class is too bad. Essentially, this logic can
    // be modularized
    private void restoreNormalGameListener(Hand playerHand) {

        CardListener normalGameListener = new CardAdapter() {
            public void leftDoubleClicked(Card card) {
                GameDataSnapshot snapshot = pinochleSystem.createSnapshot();
                if (!pinochleSystem.checkValidTrick(card,
                        playerHand.getCardList(),
                        snapshot.getCurrentTrick())) {
                    pinochleSystem.setStatus(
                            "Card is not valid. Player needs to choose higher card of the same suit or trump suit");
                    return;
                }

                pinochleSystem.setSelected(card);
                playerHand.setTouchEnabled(false);
            }
        };

        // Add normal game listeners
        playerHand.addCardListener(normalGameListener);
        playerHand.setTouchEnabled(true);
    }


    // Obtain the name of the card from the configuration
    private String getCardName(Card card) {
        Rank rank = (Rank) card.getRank();
        Suit suit = (Suit) card.getSuit();
        return rank.getCardLog() + suit.getSuitShortHand();
    }

    // Assign different permissions to different modes
    private void validatePermission(String permission) {
        if (!permissions.contains(permission)) {
            String availablePermissions = String.join(", ", permissions);
            throw new SecurityException(
                    String.format("Access denied: Missing permission '%s'. Available permissions: [%s]",
                            permission, availablePermissions)
            );
        }
    }

    // Force refresh the display of the hand cards of the all player
    //Although it's called "forced", in reality, it's just a complete refresh
    public void forceRefreshAllHands() {
        validatePermission("CONTROL_DISPLAY");

        Hand[] hands = pinochleSystem.getHands();

        for (Hand hand : hands) {
            hand.draw();
        }
        pinochleSystem.refresh();
    }


    public int getComputerPlayerIndex() {
        return pinochleSystem.getCOMPUTER_PLAYER_INDEX();
    }
}