/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package data;

import ch.aplu.jcardgame.*;
import java.util.*;

/**
* This is a simplified version of the strategy snapshot, which is used by the
* AI module to obtain the state of the current system moment in order to make decisions
*
* This is the data packet of data communication
*/
public class GameDataSnapshot {
    private final Hand[] hands;
    private final List<Card> allPlayedCards;
    private final String trumpSuit;
    private final int currentBid;
    private final int[] scores;
    private final Hand playingArea;
    private final int packSize;
    private final int bidWinnerIndex;

    public GameDataSnapshot(Hand[] hands, List<Card> allPlayedCards,
                            String trumpSuit, int currentBid, int[] scores,
                            Hand playingArea,
                            int packSize,
                            int bidWinnerIndex) {
        this.hands = Arrays.copyOf(hands, hands.length);
        this.allPlayedCards = new ArrayList<>(allPlayedCards != null ? allPlayedCards : new ArrayList<>());
        this.trumpSuit = trumpSuit;
        this.currentBid = currentBid;
        this.scores = Arrays.copyOf(scores, scores.length);
        this.playingArea = playingArea;
        this.packSize = packSize;
        this.bidWinnerIndex = bidWinnerIndex;
    }

    public int getPackSize() { return packSize; }
    public int getBidWinnerIndex() { return bidWinnerIndex; }

    public List<Card> getPlayerHand(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= hands.length) {
            return new ArrayList<>();
        }
        return hands[playerIndex].getCardList();
    }

    public List<Card> getAllPlayedCards() {
        return new ArrayList<>(allPlayedCards);
    }

    public String getTrumpSuit() {
        return trumpSuit != null ? trumpSuit : "";
    }

    public int getCurrentBid() {
        return currentBid;
    }

    public int getPlayerScore(int playerIndex) {
        if (playerIndex < 0 || playerIndex >= scores.length) {
            return 0;
        }
        return scores[playerIndex];
    }

    public List<Card> getCurrentTrick() {
        return playingArea != null ? playingArea.getCardList() : new ArrayList<>();
    }

    public Hand[] getHands() {
        return Arrays.copyOf(hands, hands.length);
    }

    public int[] getScores() {
        return Arrays.copyOf(scores, scores.length);
    }

    public Hand getPlayingArea() {
        return playingArea;
    }
}