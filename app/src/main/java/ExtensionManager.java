/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
import data.GameDataSnapshot;

import java.util.*;

/**
 If the main system is regarded as a large-scale PC game,
 this extension manager is similar to game DLCS/workshops and plugins

Be responsible for managing the life cycle of various game modes

ExtensionManager is a plug-in game extension management system,
responsible for: adding game modes to the mode library,
starting specific modes and allocating permissions, scheduling mode execution
at each stage of the game, and releasing mode resources at the end of the game

ExtensionManager assigns different system access permissions based on the mode type
and broadcasts game phase events to all relevant modes

Inspired by STEAM's paid DLCS and community mods, developing a new mode only
requires implementing interfaces
 */
public class ExtensionManager {
    private final Map<String, GameMode> registeredModes = new HashMap<>();
    private final List<GameMode> activeModes = new ArrayList<>();

    // Register a new mode and add it to the current game
    // It's like installing the DLC to the game directory,
    // but it hasn't been enabled yet
    public void registerMode(GameMode mode) {
        if (mode != null && mode.getModeName() != null) {
            registeredModes.put(mode.getModeName(), mode);
        }
    }

    // Activate this game mode and assign an agent to it
    // Allocate corresponding permissions based on the mode name and initialize the mode
    public void activateMode(String modeName, Pinochle pinochleSystem) {
        GameMode mode = registeredModes.get(modeName);
        if (mode != null && !activeModes.contains(mode)) {

            mode.initialize(
                    new GameModeServiceProxy(pinochleSystem,
                            determinePermissions(modeName)
                    ));
            activeModes.add(mode);

            activeModes.sort((a, b) -> Integer.compare(b.getPriority(), a.getPriority()));
        }
    }

    // Notify the game mode, observer
    // Function: Broadcast game phase events to all activated game modes
    public void notifyPhase(GamePhase phase, GameDataSnapshot snapshot, int playerIndex) {
        for (GameMode mode : activeModes) {
            if (mode.isActive() && mode.isApplicable(phase)) {
                try {
                    mode.execute(phase, snapshot, playerIndex);
                } catch (Exception e) {
                    handleModeException(mode, e);
                }
            }
        }
    }

    public boolean hasActiveModes() {
        return !activeModes.isEmpty();
    }

    // Used for cleaning the game mode
    public void cleanup() {
        for (GameMode mode : activeModes) {
            mode.cleanup();
        }
        activeModes.clear();
    }

    // This place is to determine whether our new model has sufficient authority
    // to do some proxy matters. It is also the place where agents are set up
    //It's somewhat similar to the feeling of different STEAM DLCS and workshops
    private Set<String> determinePermissions(String modeName) {
        Set<String> permissions = new HashSet<>();

        switch (modeName.toLowerCase()) {
            case "cutthroat":
                // Data access permission
                permissions.add("READ_GAME_DATA");// Get a game snapshot
                permissions.add("READ_PACK");// View the deck information
                permissions.add("READ_CONFIG");// Read the configuration file
                permissions.add("READ_HANDS");// Read the information of the hand cards
                permissions.add("READ_BID_INFO");// Read the bidding information
                permissions.add("READ_TRUMP");//Read the ace card information
                permissions.add("READ_DECK");//Read the card deck information

                // Status modification permission
                permissions.add("MODIFY_PACK");//Modify the deck
                permissions.add("MODIFY_HANDS");//Modify the hand cards (increase/decrease)
                permissions.add("MODIFY_SELECTION");//Modify the selected status

                //Interface control authority
                permissions.add("CONTROL_TIMING");//Control time delay
                permissions.add("CONTROL_DISPLAY");//Control display effect

                //AI and user interaction permissions
                permissions.add("USE_AI_SERVICE");//Use AI decision-making services
                permissions.add("HANDLE_USER_INPUT");//Process user input

                //Game rules and permissions
                permissions.add("USE_GAME_RULES");//Verify using game rules

                break;

            default:
                // Minimum permissions for other modes
                permissions.add("READ_GAME_DATA");
                permissions.add("READ_HANDS");
                permissions.add("READ_CONFIG");
                permissions.add("CONTROL_TIMING");
                permissions.add("CONTROL_DISPLAY");
                break;
        }

        return permissions;
    }

    private void handleModeException(GameMode mode, Exception e) {
        System.err.println("Error in game mode " + mode.getModeName() + ": " + e.getMessage());
        //More complex error recovery logic can be implemented, which is generally used for insufficient permissions
    }
}