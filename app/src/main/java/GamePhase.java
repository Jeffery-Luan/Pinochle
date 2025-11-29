/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */

/**
It is used to define the complete life cycle stages of the Pinochle game and
provide the enumeration definitions required for precise execution timing control
of the game mode system

The detailed stage division makes the granularity of game stages easier to control
*/
public enum GamePhase {
    INITIALIZATION("initialization"),// Game initialization stage
    DEALING("dealing"),// The licensing stage
    BIDDING("bidding"),// Bidding stage
    POST_BIDDING("post_bidding"),//Post-Bid stage
    TRUMP_SELECTION("trump_selection"),// Ace selection stage
    PRE_MELDING("pre_melding"),// Pre-combination stage
    MELDING("melding"),//  combination stage
    PRE_TRICK_TAKING("pre_trick_taking"),//Tricks the stage before playing cards
    TRICK_TAKING("trick_taking"),// Skill card playing stage
    SCORING("scoring"),// Scoring stage
    GAME_END("game_end");// Game end stage

    private final String phaseName;

    GamePhase(String phaseName) {
        this.phaseName = phaseName;
    }

    public String getPhaseName() {
        return phaseName;
    }
}