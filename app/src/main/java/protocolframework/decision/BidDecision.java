/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package protocolframework.decision;

/**
 * Represents the result of a bidding decision made by an AI or player.
 * This class encapsulates whether the player chooses to pass or make a bid,
 * along with the bid amount. It extends the base {@link Decision} class to
 * include validation and reasoning support for traceability and explainability.
 */
public class BidDecision extends Decision {

    /** Whether the player has decided to pass the bidding round. */
    private final boolean shouldPass;

    private final int bidAmount;
    private final String recommendedTrumpSuit;

    /**
     * Constructs a new BidDecision with specified pass status and bid amount.
     * Also identify the decision validity.
     *
     * @param shouldPass true if the player chooses to pass; false if bidding
     * @param bidAmount the bid value (ignored if shouldPass is true)
     */
    public BidDecision(boolean shouldPass, int bidAmount, String recommendedTrumpSuit) {
        this.shouldPass = shouldPass;
        this.bidAmount = bidAmount;
        this.isValid = bidAmount >= 0;
        this.recommendedTrumpSuit = recommendedTrumpSuit;
    }

    public boolean shouldPass() {
        return shouldPass;
    }

    public int getBidAmount() {
        return bidAmount;
    }

    public String getRecommendedTrumpSuit() {
        return recommendedTrumpSuit;
    }
}