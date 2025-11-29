/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package protocolframework.decision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 This is the common format returned by the data packets of the universal data
 exchange interface for card decision-making, just like the content returned by the HTTP protocol. Used for complex data exchange protocols

 This is Set B
 Because card selection is essentially mapping from card set A to card set B,
 the selection includes choosing one of two (although this is a naive implementation),
 discarding cards, playing cards, etc., all of which can be satisfied by this interface
 */
public class GeneralCardDecision extends Decision {
    private final List<Integer> selectedIndices;

    public GeneralCardDecision(List<Integer> selectedIndices) {
        this.selectedIndices = new ArrayList<>(selectedIndices != null ? selectedIndices : Collections.emptyList());
        this.isValid = !this.selectedIndices.isEmpty();
    }

    public List<Integer> getSelectedIndices() {
        return new ArrayList<>(selectedIndices);
    }
}
