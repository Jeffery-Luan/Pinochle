/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package protocolframework.Request;
import ch.aplu.jcardgame.Card;
import data.GameDataSnapshot;
import protocolframework.DecisionType;
import protocolframework.decision.GeneralCardDecision;
import java.util.ArrayList;
import java.util.List;

/**
 This is the data request protocol of the universal data exchange interface for card decision-making

 Because card selection is essentially mapping from card set A to card set B,
 the selection includes choosing one of two (although this is a naive implementation),
 discarding cards, playing cards, etc., all of which can be satisfied by this interface
 */
public class GeneralCardDecisionRequest extends DecisionRequest<GeneralCardDecision> {
    private final List<Card> availableCards;

    public GeneralCardDecisionRequest(GameDataSnapshot snapshot,
                                      DecisionType type, int playerIndex,
                                      List<Card> availableCards) {
        super(snapshot, type, playerIndex);
        this.availableCards = new ArrayList<>(availableCards);
    }

    public List<Card> getAvailableCards() {
        return new ArrayList<>(availableCards);
    }
}