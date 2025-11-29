/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package protocolframework.Request;

import data.GameDataSnapshot;
import protocolframework.decision.Decision;
import protocolframework.DecisionType;

/*
 * It is used to encapsulate all the input information needed when the AI makes decisions.
 * It is the standardized request interface of the AI decision-making system.
 *
 */
public abstract class DecisionRequest<T extends Decision> {
    protected final GameDataSnapshot
            snapshot; // FOR Game status/snapshot information
    protected final DecisionType type; // Tell the AI system what type of decision request this is
    protected final int playerIndex;

    protected DecisionRequest(GameDataSnapshot snapshot, DecisionType type,
                              int playerIndex) {
        this.snapshot = snapshot;
        this.type = type;
        this.playerIndex = playerIndex;
    }

    public GameDataSnapshot getSnapshot() {
        return snapshot;
    }

    public DecisionType getType() {
        return type;
    }

    public int getPlayerIndex() {
        return playerIndex;
    }
}


