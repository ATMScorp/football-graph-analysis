package graph;

import java.io.*;
import java.util.*;

import lombok.Getter;
import model.Player;

/**
 * GraphBuilder constructs a co-play graph of football players based on data files.
 * Each vertex represents a player, and edges represent shared team-seasons.
 * Edge weights indicate how many seasons two players played together.
 * <p>
 * This version is adapted for the project "Evolution of Football Teams".
 * Data format comes from FootBallTeamsGraphs.java, which saves team rosters as CSV files.
 */
@Getter
public class GraphBuilder {

    /**
     * -- GETTER --
     *  Returns all players in the graph.
     */
    // Player registry (player name -> Player object)
    private final Map<String, Player> players = new HashMap<>();

    /**
     * -- GETTER --
     *  Returns adjacency map (edges).
     */
    // Weighted adjacency map: player -> (teammate -> number of shared seasons)
    private final Map<String, Map<String, Integer>> edges = new HashMap<>();

    /**
     * Loads multiple team-season CSV files and builds the co-play graph.
     *
     * @param folderPath the directory containing CSV files from FootBallTeamsGraphs
     */
    public void loadAndBuildFromFolder(String folderPath) {
        File folder = new File(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            System.err.println("Invalid folder path: " + folderPath);
            return;
        }

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".csv"));
        if (files == null || files.length == 0) {
            System.err.println("No CSV files found in " + folderPath);
            return;
        }

        System.out.println("Building co-play graph from " + files.length + " team-season files...\n");

        for (File file : files) {
            processTeamFile(file);
        }

        System.out.println("Graph built successfully!");
        System.out.println("Players: " + players.size());
        System.out.println("Edges: " + countEdges());
    }

    /**
     * Processes one team CSV (one team in one season) and adds co-play relationships.
     *
     * @param csvFile the team-season CSV file
     */
    private void processTeamFile(File csvFile) {
        List<String> playerNames = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 2) continue;

                String name = parts[1].trim(); // column "Name"
                if (name.isEmpty()) continue;

                playerNames.add(name);

                // create basic Player if not present
                players.putIfAbsent(name, new Player(
                        -1, name, "N/A", "N/A", -1,
                        "N/A", "N/A", "N/A", "N/A", "N/A", "N/A", "N/A"));
            }
        } catch (IOException e) {
            System.err.println("Error reading " + csvFile.getName() + ": " + e.getMessage());
            return;
        }

        // connect all players from this team-season as teammates
        for (int i = 0; i < playerNames.size(); i++) {
            for (int j = i + 1; j < playerNames.size(); j++) {
                addEdge(playerNames.get(i), playerNames.get(j));
                addEdge(playerNames.get(j), playerNames.get(i));
            }
        }
    }

    /**
     * Adds or increments a co-play edge between two players.
     */
    private void addEdge(String playerA, String playerB) {
        edges.computeIfAbsent(playerA, k -> new HashMap<>())
                .merge(playerB, 1, Integer::sum);
    }

    /**
     * Counts total unique undirected edges.
     */
    private int countEdges() {
        Set<String> uniqueEdges = new HashSet<>();
        for (String a : edges.keySet()) {
            for (String b : edges.get(a).keySet()) {
                String edgeKey = a.compareTo(b) < 0 ? a + "-" + b : b + "-" + a;
                uniqueEdges.add(edgeKey);
            }
        }
        return uniqueEdges.size();
    }

    /**
     * Prints a summary of the graph.
     */
    public void printSummary() {
        System.out.println("\n=== GRAPH SUMMARY ===");
        System.out.println("Players (vertices): " + players.size());
        System.out.println("Unique edges: " + countEdges());

        String mostConnected = null;
        int maxConnections = 0;
        for (Map.Entry<String, Map<String, Integer>> entry : edges.entrySet()) {
            int connections = entry.getValue().size();
            if (connections > maxConnections) {
                maxConnections = connections;
                mostConnected = entry.getKey();
            }
        }

        if (mostConnected != null) {
            System.out.println("Most connected player: " + mostConnected +
                    " (" + maxConnections + " teammates)");
        }
        System.out.println("======================\n");
    }

    /**
     * Main for testing the builder independently.
     */
    public static void main(String[] args) {
        GraphBuilder builder = new GraphBuilder();
        builder.loadAndBuildFromFolder("src/main/resources/teamsData");
        builder.printSummary();
    }
}
