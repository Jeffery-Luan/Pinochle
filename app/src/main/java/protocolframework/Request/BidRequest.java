/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package protocolframework.Request;

import data.GameDataSnapshot;
import protocolframework.decision.BidDecision;
import protocolframework.DecisionType;

/**
 This BidRequest class is a dedicated request class for AI bidding decisions

 This is a specific decision request class, specifically used to request bidding
 decisions from AI. It encapsulates all the contextual information that
 AI needs to make intelligent bidding decisions.
 */
public class BidRequest extends DecisionRequest<BidDecision> {
    private final int currentBid;  // current highest bid
    private final boolean isFirstBid;  // is or is not the first person bid


    public BidRequest(GameDataSnapshot snapshot, int playerIndex, int currentBid, boolean isFirstBid) {
        super(snapshot, DecisionType.BID, playerIndex);
        this.currentBid = currentBid;
        this.isFirstBid = isFirstBid;
    }

    // getters
    public int getCurrentBid() {
        return currentBid;
    }

    public boolean isFirstBid() {
        return isFirstBid;
    }
}