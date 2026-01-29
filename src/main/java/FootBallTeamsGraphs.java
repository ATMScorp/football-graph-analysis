import graph.EvolutionGraphBuilder;
import graph.EvolutionVisualizer;
import graph.GraphBuilder;
import model.Player;
import scraper.PlayerScraper;
import graph.GraphVisualizer;

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
        // Configuration
        boolean downloadNewData = false;
        boolean showEvolution = true;
        boolean showStaticGraph = true;

        // Teams to analyze with their Transfermarkt URLs
        Map<String, String> teams = Map.of(
                "Legia Warszawa", "https://www.transfermarkt.com/legia-warszawa/kader/verein/255/plus/1/galerie/0?saison_id=",
                "Bayern Munich", "https://www.transfermarkt.com/fc-bayern-munich/kader/verein/27/plus/1/galerie/0?saison_id=",
                "AC Milan", "https://www.transfermarkt.com/ac-milan/kader/verein/5/plus/1/galerie/0?saison_id=",
                "AS Roma", "https://www.transfermarkt.com/as-roma/kader/verein/12/plus/1/galerie/0?saison_id="
        );

        //Season range to scrape (e.g., from 1999 to 2025)
        int startSeason = 1999;
        int endSeason = 2025;

        // Download data if needed
        if (downloadNewData) {
            System.out.println("Downloading player data from Transfermarkt\n");
            downloadAllTeamData(teams, startSeason, endSeason);
        } else {
            System.out.println("Using existing data (set downloadNewData=true to refresh)\n");
        }

        // Build and analyze the evolution graph
        System.out.println("Building temporal co-play graph\n");

        EvolutionGraphBuilder evolutionBuilder = new EvolutionGraphBuilder();
        evolutionBuilder.loadFromFolder(outputFolderPath);

        // Print evolution statistics
        evolutionBuilder.printEvolutionSummary();

        // Check if we have data
        if (evolutionBuilder.getAllSeasons().isEmpty()) {
            System.out.println("ERROR: No data found!");
            System.out.println("Please set downloadNewData = true to download data from Transfermarkt,");
            System.out.println("or make sure CSV files exist in: " + outputFolderPath);
            return;
        }

        // Visualize
        if (showEvolution) {
            System.out.println("Launching interactive evolution visualizer:\n");
            System.out.println("Controls:");
            System.out.println("  - Use slider or Previous/Next buttons to navigate seasons");
            System.out.println("  - Click 'Animate' for automatic playback");
            System.out.println("  - Toggle 'Cumulative' for cumulative vs snapshot view");
            System.out.println("  - Toggle 'Highlight Changes' to see new/departing players\n");
            System.out.println("Color coding:");
            System.out.println("  - Blue: Regular players");
            System.out.println("  - Green: Players who joined this season");
            System.out.println("  - Red: Players who will leave after this season\n");

            EvolutionVisualizer.visualize(evolutionBuilder);
        }

        if (showStaticGraph) {
            System.out.println("Showing static full graph\n");
            GraphBuilder staticBuilder = new GraphBuilder();
            staticBuilder.loadAndBuildFromFolder(outputFolderPath);
            staticBuilder.printSummary();
            GraphVisualizer.showGraph(staticBuilder.getEdges());
        }
    }

    //Download player data for each team and season
    private static void downloadAllTeamData(Map<String, String> teams, int startSeason, int endSeason) {
        // Ensure output directory exists
        new File(outputFolderPath).mkdirs();

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

                    String fileName = teamName.replace(" ", "_") + "_" + season + ".csv";
                    String fullPath = outputFolderPath + File.separator + fileName;
                    saveToCSV(players, fullPath);

                    System.out.println("Saved: " + fileName + " (" + players.size() + " players)\n");
                } catch (Exception e) {
                    System.err.println("Error while downloading data for " + teamSeasonKey + ": " + e.getMessage());
                }
            }
        }

        System.out.println("\nData download complete.\n");
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
