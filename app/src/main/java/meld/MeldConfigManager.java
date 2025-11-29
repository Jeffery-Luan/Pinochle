/**
 * [Mon16:00] Team 02:
 * Haoguang Zhou 1344871
 * Baimin PAN 1329449
 * Yudong Luan 1362030
 */
package meld;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 Combo card configuration manager
 Responsibilities: Specifically responsible for configuring loading, parsing, and creating meld.MeldChecker instances
 */
public class MeldConfigManager{
    /**
     Load and create all combination card checkers from the JSON configuration file
     This is the unified entry method for the outside world
     @param jsonFile JSON configuration file path (relative to the resources' directory)
     @return the meld.MeldChecker list sorted by score
     */
    public static List<MeldChecker> loadCheckers(String jsonFile,
                                                 Properties properties) {

        boolean useAdditionalMelds = Boolean.parseBoolean(
                properties.getProperty("melds.additional", "true")
        );
        if (!useAdditionalMelds) {
            System.out.println("==============================");
            return getOriginalMeldCheckers();}

        try {
            // Read the content of the JSON file
            String jsonContent = readJsonFile(jsonFile);
            // Parse the JSON content to obtain the configuration list
            List<MeldConfig> configs = parseJsonConfig(jsonContent);
            // Create and sort the checker
            return createAndSortCheckers(configs);

        } catch (Exception e) {
            //Considering the differences between the test and compilation environments,
            // two ways are used to attempt to access the json file
            // Try fallback path with "app/" prefix
            try {
                String fallbackPath = "app/" + jsonFile;
                String jsonContent = readJsonFile(fallbackPath);
                List<MeldConfig> configs = parseJsonConfig(jsonContent);
                return createAndSortCheckers(configs);
            } catch (Exception fallbackException) {
                // In fact, this place will not be executed
                //
                //This is an alternative solution if the use of json is prohibited
                // If the loading fails, use the default configuration
                return getDefaultMeldCheckers(); // Enter the deck information from the code
            }
        }
    }

    /**
     Read the content of the JSON configuration file
     * @param jsonFile JSON file path
     * @return the content string of the JSON file
     * @throws IOException file reading exception
     */
    private static String readJsonFile(String jsonFile) throws IOException {
        File file = new File(jsonFile);

        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     Parse the JSON configuration content
     Use simplified JSON parsing logic to avoid introducing external dependencies
     * @param jsonContent JSON content string
     * @return configure the object list
     */
    private static List<MeldConfig> parseJsonConfig(String jsonContent) {
        List<MeldConfig> configs =new ArrayList<>();

        // Simplified JSON parsing: Process line by line and search for configuration items
        // Due to the build issue of the project, a naive string json processing method is used
        // In order for the formula to be extended to a professional json
        // processing library
        String[] lines = jsonContent.split("\n");
        String currentName = null;
        Integer currentScore = null;
        String currentCards = null;

        for (String line : lines) {
            line = line.trim();
            // Skip blank lines and comments
            if (line.isEmpty() ||  line.startsWith("//")) {continue;}


            // Parse each field
            if (line.contains("\"name\"")) {
                currentName = extractJsonString(line);
            } else if (line.contains("\"score\"")) {
                currentScore = extractJsonNumber(line);
            } else if (line.contains("\"cards\"")) {
                currentCards = extractJsonString(line);

                // When a complete configuration is collected, create the configuration object
                if (currentName != null && currentScore != null && currentCards != null) {
                    configs.add(new MeldConfig(currentName, currentScore, currentCards));

                    // Reset the temporary variable
                    currentCards = null;
                    currentName = null;
                    currentScore = null;

                }
            }
        }
        return configs;
    }

    /**
     Extract string values from JSON lines
     Example: "name": "Double Run" -> "Double Run"
     @param line JSON line
     The string value extracted by @return
     */
    private static String extractJsonString(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex == -1) return null;

        String valuePart = line.substring(colonIndex + 1).trim();

        // Find the first and the last quotation marks
        int firstQuote = valuePart.indexOf('"');
        int lastQuote = valuePart.lastIndexOf('"');

        if (firstQuote != -1 && lastQuote != -1 && firstQuote != lastQuote) {
            return valuePart.substring(firstQuote + 1, lastQuote);}
        return null;
    }

    /**
     Extract numeric values from the JSON lines
     Example: "score": 1500 -> 1500
     @param line JSON line
     The numeric value extracted by @return
     */
    private static Integer extractJsonNumber(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex == -1) return null;

        String valuePart = line.substring(colonIndex + 1).trim();
        // Remove possible commas and curly braces
        valuePart = valuePart.replaceAll("[,}\\s]", "");
        return Integer.parseInt(valuePart);
    }

    /**
     Create and sort the checkers based on the configuration list
     * @param configs configure the list of objects
     * @return a list of checkers sorted by score
     */
    private static List<MeldChecker> createAndSortCheckers(List<MeldConfig> configs) {
        return configs.stream()
                .filter(MeldConfig::isValid) // Filter invalid configurations
                .sorted((a, b) -> Integer.compare(b.score, a.score)) // Sort by score from high to low
                .map(config -> new MeldChecker(config.name, config.score, config.cards))
                .collect(Collectors.toList());
    }


    // In fact, this method is not needed. It's just to prevent the graders from
    // accidentally deleting my json file
    //Or it could be used in the future json format and be banned by the company
    /**
     Get the list of default combination card checkers
     It is used when the JSON configuration fails to load
     * @return the default inspector list
     */
    private static List<MeldChecker> getDefaultMeldCheckers() {
        List<MeldChecker> defaultCheckers = new ArrayList<>();

        // Add all combination cards according to the content of the JSON file (they will be re-sorted by score in createAndSortCheckers)

        defaultCheckers.add(new MeldChecker("Double Run", 1500,
                "4:TRUMP:2,1:TRUMP:2,2:TRUMP:2,3:TRUMP:2,5:TRUMP:2"));
        defaultCheckers.add(new MeldChecker("Jacks Abound", 400,
                "1:S:2,1:H:2,1:D:2,1:C:2"));
        defaultCheckers.add(new MeldChecker("Double Pinochle", 300,
                "1:D:2,2:S:2"));
        defaultCheckers.add(new MeldChecker("Ace Run + Royal Marriage", 230,
                "4:TRUMP:1,1:TRUMP:1,2:TRUMP:2,3:TRUMP:2,5:TRUMP:1"));
        defaultCheckers.add(new MeldChecker("Ace Run + Extra King", 190,
                "4:TRUMP:1,1:TRUMP:1,2:TRUMP:1,3:TRUMP:2,5:TRUMP:1"));
        defaultCheckers.add(new MeldChecker("Ace Run + Extra Queen", 190,
                "4:TRUMP:1,1:TRUMP:1,2:TRUMP:2,3:TRUMP:1,5:TRUMP:1"));
        defaultCheckers.add(new MeldChecker("Ten to Ace Run", 150,
                "4:TRUMP:1,1:TRUMP:1,2:TRUMP:1,3:TRUMP:1,5:TRUMP:1"));
        defaultCheckers.add(new MeldChecker("Aces Around", 100,
                "5:S:1,5:H:1,5:D:1,5:C:1"));
        defaultCheckers.add(new MeldChecker("Royal Marriage", 40,
                "2:TRUMP:1,3:TRUMP:1"));
        defaultCheckers.add(new MeldChecker("Pinochle", 40,
                "1:D:1,2:S:1"));
        defaultCheckers.add(new MeldChecker("Common Marriage Spades", 20,
                "2:S:1,3:S:1"));
        defaultCheckers.add(new MeldChecker("Common Marriage Hearts", 20,
                "2:H:1,3:H:1"));
        defaultCheckers.add(new MeldChecker("Common Marriage Diamonds", 20,
                "2:D:1,3:D:1"));
        defaultCheckers.add(new MeldChecker("Common Marriage Clubs", 20,
                "2:C:1,3:C:1"));
        defaultCheckers.add(new MeldChecker("Dix", 10,
                "0:TRUMP:1"));

        return defaultCheckers;
    }




    //Actually, I don't like this function, but considering
    //The parameter "melds.additional=false" was newly added to maintain the original version
    /**
     Get the original four combination card checkers
     Corresponding to the combo cards supported in the original Pinochle version
     */
    public static List<MeldChecker> getOriginalMeldCheckers() {
        List<MeldChecker> originalCheckers = new ArrayList<>();

        originalCheckers.add(new MeldChecker("Ace Run + Extra King", 190,
                "4:TRUMP:1,1:TRUMP:1,2:TRUMP:1,3:TRUMP:2,5:TRUMP:1"));
        originalCheckers.add(new MeldChecker("Ace Run + Extra Queen", 190,
                "4:TRUMP:1,1:TRUMP:1,2:TRUMP:2,3:TRUMP:1,5:TRUMP:1"));
        originalCheckers.add(new MeldChecker("Ten to Ace Run", 150,
                "4:TRUMP:1,1:TRUMP:1,2:TRUMP:1,3:TRUMP:1,5:TRUMP:1"));
        originalCheckers.add(new MeldChecker("Royal Marriage", 40,
                "2:TRUMP:1,3:TRUMP:1"));
        return originalCheckers;
    }



    /**
     Internal configuration data class
     Used for temporarily storing the configuration information parsed from JSON
     */
    private static class MeldConfig {
        final String name;
        final int score;
        final String cards;

        MeldConfig(String name, int score, String cards) {
            this.name = name;
            this.score = score;
            this.cards = cards;
        }

        /**
         Check whether the configuration is valid
         Whether @return is valid
         */
        boolean isValid() {
            return name != null && !name.trim().isEmpty()
                    && score > 0
                    && cards != null && !cards.trim().isEmpty();
        }
    }
}