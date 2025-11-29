/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
import data.GameDataSnapshot;

/*
 This interface implements a plug-in architecture pattern and is used to solve
  the problems initially raised by the team. When adding new game modes,
  there is no need to modify the main class code or make other troublesome modifications
  or unelegant additions. Decouple different game modes and even support
  the simultaneous operation of multiple modes
 */
public interface GameMode {

    // Each pattern has a unique name identifier for registration and lookup
    // in the pattern manager
    String getModeName();

    // It might be necessary to create GUI components and initialize data structures
    void initialize(GameModeServiceProxy serviceProxy);

    /**
     * The game mode is the entry point for executing custom logic at a specific game stage
     * @param phase Let the mode know at which stage of the game it is called,
     *  and you can choose to execute the logic at a specific stage
     * @param snapshot The mode acquires all the game states needed for making decisions
     */
    void execute(GamePhase phase, GameDataSnapshot snapshot, int playerIndex);

    // Used for cleaning up GUI components created by patterns, temporary data, etc
    void cleanup();

    //Check whether the mode is applicable to a specific stage
    default boolean isApplicable(GamePhase phase) {
        return true;
    }

    //Obtain execution priority
    //The larger the number, the higher the priority
    default int getPriority() {
        return 0;
    }

    // Check whether the mode is active
    default boolean isActive() {
        return true;
    }

    void setServiceProxy(GameModeServiceProxy serviceProxy);
}