/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package protocolframework;

/*
This is an enumeration class for game-stage decisions,
used to define and identify all supported types of AI decisions in the system.

It is the type registry and routing identifier of the entire decision framework.
* */

public enum DecisionType {
    BID("bidding"),
    CARD_PLAY("card_play"),
    REVEALED_CARD_SELECTION("revealed_card_selection"),
    CARD_DISCARD("card_discard"),
    TRUMP_SELECTION("trump_selection"),
    CARD_CHOOSE("card_choose");

    private final String typeName; //name for type

    // Private constructor (default for enumeration),
    // ensuring that it can only be created through predefined enumeration values
    DecisionType(String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }
}