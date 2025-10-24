import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class FootBallTeamsGraphs {
    static final String outputFolderPath = "src/main/resources/teamsData";

    /**
     * Main entry point for the Football Teams Evolution project.
     * This class automatically collects player data from Transfermarkt
     * for multiple football teams and across multiple seasons,
     * using the {@link PlayerScraper} class.
     * Each dataset (team + season) is then exported to a CSV file.
     */
    public static void main(String[] args) {
        //Map: team name -> base URL (without the season parameter)
        Map<String, String> teams = Map.of(
                "Legia Warszawa", "https://www.transfermarkt.com/legia-warszawa/kader/verein/255/plus/1/galerie/0?saison_id=",
                "Bayern Munich", "https://www.transfermarkt.com/fc-bayern-munich/kader/verein/27/plus/1/galerie/0?saison_id=",
                "AC Milan", "https://www.transfermarkt.com/ac-milan/kader/verein/5/plus/1/galerie/0?saison_id=",
                "AS Roma", "https://www.transfermarkt.com/as-roma/kader/verein/12/plus/1/galerie/0?saison_id="
        );

        //Season range to scrape (e.g., from 1999 to 2025)
        int startSeason = 1999;
        int endSeason = 2025;

        // Result map: team_season -> list of players
        Map<String, List<Player>> teamsData = new HashMap<>();

        //Download player data for each team and season
        for (Map.Entry<String, String> entry : teams.entrySet()) {
            String teamName = entry.getKey();
            String baseUrl = entry.getValue();

            for (int season = startSeason; season <= endSeason; season++) {
                String fullUrl = baseUrl + season;
                String teamSeasonKey = teamName + " " + season;
                System.out.println("Downloading data for: " + teamSeasonKey);

                try {
                    // Fetch and parse player data from Transfermarkt
                    List<Player> players = PlayerScraper.parsePlayers(fullUrl);

                    if (players.isEmpty()) {
                        System.out.println("No data available for " + teamSeasonKey);
                        continue;
                    }

                    teamsData.put(teamSeasonKey, players);

                    // Save results to CSV file
                    String fileName = teamName.replace(" ", "_") + "_" + season + ".csv";
                    String fullPath = outputFolderPath + File.separator + fileName;
                    saveToCSV(players, fullPath);

                    System.out.println("Saved: " + fileName + " (" + players.size() + " players)\n");
                } catch (Exception e) {
                    System.err.println("Error while downloading data for " + teamSeasonKey + ": " + e.getMessage());
                }
            }
        }

        System.out.println("All data successfully downloaded and saved for all teams and seasons.");
    }

    /**
     * Saves a list of players to a CSV file.
     *
     * @param players  list of {@link Player} objects to save
     * @param fileName name of the output CSV file
     */
    private static void saveToCSV(List<Player> players, String fileName) {
        try (FileWriter writer = new FileWriter(fileName)) {
            // CSV header
            writer.append("Number,Name,Position,DateOfBirth,Age,Nationality,CurrentClub,Height,Foot,Joined,SignedFrom,MarketValue\n");

            // CSV rows
            for (Player p : players) {
                writer.append(p.getNumber() + ",")
                        .append(escapeCSV(p.getName()) + ",")
                        .append(escapeCSV(p.getPosition()) + ",")
                        .append(escapeCSV(p.getDateOfBirth()) + ",")
                        .append(p.getAge() + ",")
                        .append(escapeCSV(p.getNationality()) + ",")
                        .append(escapeCSV(p.getCurrentClub()) + ",")
                        .append(escapeCSV(p.getHeight()) + ",")
                        .append(escapeCSV(p.getFoot()) + ",")
                        .append(escapeCSV(p.getJoined()) + ",")
                        .append(escapeCSV(p.getSignedFrom()) + ",")
                        .append(escapeCSV(p.getMarketValue()))
                        .append("\n");
            }
        } catch (IOException e) {
            System.err.println("Error while saving data to CSV: " + e.getMessage());
        }
    }

    /**
     * Escapes text values for safe CSV writing.
     * Adds quotes around values containing commas or quotes.
     *
     * @param value the text to escape
     * @return properly escaped CSV-safe string
     */
    private static String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"")) {
            value = "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
