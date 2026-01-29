package graph;

import lombok.Getter;
import model.Player;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/*
 * EvolutionGraphBuilder extends the basic GraphBuilder with temporal capabilities.
 * It tracks which players played together in which seasons, enabling:
 * - Snapshot graphs for specific seasons
 * - Cumulative graphs up to a certain season
 * - Analysis of graph evolution over time
 */
@Getter
public class EvolutionGraphBuilder {

    // Player registry (player name -> Player object with most recent data)
    private final Map<String, Player> players = new HashMap<>();

    // Team-season data: "TeamName_Season" -> Set of player names
    private final Map<String, Set<String>> teamSeasonRosters = new TreeMap<>();

    // Edge data with temporal info: "PlayerA-PlayerB" -> Map<Season, Count>
    // This tracks in which seasons two players played together
    private final Map<String, Map<Integer, Integer>> edgeSeasonData = new HashMap<>();

    // All seasons in the dataset (sorted)
    private final TreeSet<Integer> allSeasons = new TreeSet<>();

    // All teams in the dataset
    private final Set<String> allTeams = new HashSet<>();

    /**
     * @param folderPath path to folder containing team CSV files
     */
    public void loadFromFolder(String folderPath) {
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

        System.out.println("Loading " + files.length + " team-season files for evolution analysis...\n");

        for (File file : files) {
            processTeamFile(file);
        }

        System.out.println("Data loaded successfully!");
        System.out.println("Teams: " + allTeams.size());
        System.out.println("Seasons: " + allSeasons.first() + " - " + allSeasons.last());
        System.out.println("Total players: " + players.size());
    }

    /*
     * Processes one team-season CSV file.
     * Extracts team name and season from filename (e.g., "Bayern_Munich_2020.csv")
     */
    private void processTeamFile(File csvFile) {
        String fileName = csvFile.getName().replace(".csv", "");

        // Extract season
        Pattern pattern = Pattern.compile("(.+)_(\\d{4})$");
        Matcher matcher = pattern.matcher(fileName);

        if (!matcher.matches()) {
            System.err.println("Cannot parse filename: " + fileName);
            return;
        }

        String teamName = matcher.group(1).replace("_", " ");
        int season = Integer.parseInt(matcher.group(2));

        allTeams.add(teamName);
        allSeasons.add(season);

        Set<String> playerNames = new HashSet<>();
        String teamSeasonKey = teamName + "_" + season;

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = parseCSVLine(line);
                if (parts.length < 2) continue;

                String name = parts[1].trim();
                if (name.isEmpty() || name.equals("N/A")) continue;

                playerNames.add(name);

                // Store player info
                if (parts.length >= 12) {
                    Player player = new Player(
                            parseIntSafe(parts[0]),
                            name,
                            parts.length > 2 ? parts[2] : "N/A",
                            parts.length > 3 ? parts[3] : "N/A",
                            parts.length > 4 ? parseIntSafe(parts[4]) : -1,
                            parts.length > 5 ? parts[5] : "N/A",
                            parts.length > 6 ? parts[6] : "N/A",
                            parts.length > 7 ? parts[7] : "N/A",
                            parts.length > 8 ? parts[8] : "N/A",
                            parts.length > 9 ? parts[9] : "N/A",
                            parts.length > 10 ? parts[10] : "N/A",
                            parts.length > 11 ? parts[11] : "N/A"
                    );
                    players.put(name, player);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading " + csvFile.getName() + ": " + e.getMessage());
            return;
        }

        teamSeasonRosters.put(teamSeasonKey, playerNames);

        // Create edges between all teammates in this season
        List<String> playerList = new ArrayList<>(playerNames);
        for (int i = 0; i < playerList.size(); i++) {
            for (int j = i + 1; j < playerList.size(); j++) {
                addTemporalEdge(playerList.get(i), playerList.get(j), season);
            }
        }
    }

    // Adds or updates an edge with temporal information
    private void addTemporalEdge(String playerA, String playerB, int season) {
        String edgeKey = createEdgeKey(playerA, playerB);

        edgeSeasonData.computeIfAbsent(edgeKey, k -> new HashMap<>())
                .merge(season, 1, Integer::sum);
    }

    private String createEdgeKey(String playerA, String playerB) {
        return playerA.compareTo(playerB) < 0
                ? playerA + "|||" + playerB
                : playerB + "|||" + playerA;
    }

    private String[] parseEdgeKey(String edgeKey) {
        return edgeKey.split("\\|\\|\\|");
    }

    /*
     * Returns a snapshot graph for a specific season only.
     * Nodes: players active in that season
     * Edges: co-play relationships formed in that season
     */
    /**
     * @param season the season year
     * @return adjacency map for that season
     */
    public Map<String, Map<String, Integer>> getSeasonSnapshot(int season) {
        Map<String, Map<String, Integer>> snapshot = new HashMap<>();

        for (Map.Entry<String, Map<Integer, Integer>> entry : edgeSeasonData.entrySet()) {
            Map<Integer, Integer> seasonCounts = entry.getValue();

            if (seasonCounts.containsKey(season)) {
                String[] players = parseEdgeKey(entry.getKey());
                int weight = seasonCounts.get(season);

                snapshot.computeIfAbsent(players[0], k -> new HashMap<>())
                        .put(players[1], weight);
                snapshot.computeIfAbsent(players[1], k -> new HashMap<>())
                        .put(players[0], weight);
            }
        }

        return snapshot;
    }

    /*
     * Returns a cumulative graph up to and including the specified season.
     * Edge weights = total number of seasons played together up to this point.
     */
    /**
     * @param upToSeason include all seasons up to this year
     * @return cumulative adjacency map
     */
    public Map<String, Map<String, Integer>> getCumulativeGraph(int upToSeason) {
        Map<String, Map<String, Integer>> cumulative = new HashMap<>();

        for (Map.Entry<String, Map<Integer, Integer>> entry : edgeSeasonData.entrySet()) {
            String[] playerPair = parseEdgeKey(entry.getKey());

            int totalWeight = entry.getValue().entrySet().stream()
                    .filter(e -> e.getKey() <= upToSeason)
                    .mapToInt(Map.Entry::getValue)
                    .sum();

            if (totalWeight > 0) {
                cumulative.computeIfAbsent(playerPair[0], k -> new HashMap<>())
                        .put(playerPair[1], totalWeight);
                cumulative.computeIfAbsent(playerPair[1], k -> new HashMap<>())
                        .put(playerPair[0], totalWeight);
            }
        }

        return cumulative;
    }

    public Map<String, Map<String, Integer>> getFullGraph() {
        if (allSeasons.isEmpty()) {
            return new HashMap<>();
        }
        return getCumulativeGraph(allSeasons.last());
    }

    public Set<String> getPlayersInSeason(int season) {
        Set<String> activePlayers = new HashSet<>();

        for (Map.Entry<String, Set<String>> entry : teamSeasonRosters.entrySet()) {
            if (entry.getKey().endsWith("_" + season)) {
                activePlayers.addAll(entry.getValue());
            }
        }

        return activePlayers;
    }

    public Set<String> getNewPlayers(int season) {
        Set<String> currentPlayers = getPlayersInSeason(season);
        Set<String> previousPlayers = getPlayersInSeason(season - 1);

        return currentPlayers.stream()
                .filter(p -> !previousPlayers.contains(p))
                .collect(Collectors.toSet());
    }

    public Set<String> getDepartedPlayers(int season) {
        Set<String> currentPlayers = getPlayersInSeason(season);
        Set<String> nextPlayers = getPlayersInSeason(season + 1);

        return currentPlayers.stream()
                .filter(p -> !nextPlayers.contains(p))
                .collect(Collectors.toSet());
    }

    public Set<String> getPlayersForTeam(String teamName, int season) {
        String key = teamName + "_" + season;
        return teamSeasonRosters.getOrDefault(key, new HashSet<>());
    }

    // Generates statistics for each season showing graph evolution
    public List<SeasonStats> getEvolutionStats() {
        List<SeasonStats> stats = new ArrayList<>();

        for (int season : allSeasons) {
            Map<String, Map<String, Integer>> snapshot = getSeasonSnapshot(season);
            Map<String, Map<String, Integer>> cumulative = getCumulativeGraph(season);

            int nodesInSeason = snapshot.size();
            int edgesInSeason = countEdges(snapshot);
            int cumulativeNodes = cumulative.size();
            int cumulativeEdges = countEdges(cumulative);
            int newPlayers = getNewPlayers(season).size();
            int departedPlayers = getDepartedPlayers(season).size();

            stats.add(new SeasonStats(
                    season, nodesInSeason, edgesInSeason,
                    cumulativeNodes, cumulativeEdges,
                    newPlayers, departedPlayers
            ));
        }

        return stats;
    }

    private int countEdges(Map<String, Map<String, Integer>> adjMap) {
        Set<String> counted = new HashSet<>();
        int count = 0;

        for (String a : adjMap.keySet()) {
            for (String b : adjMap.get(a).keySet()) {
                String key = createEdgeKey(a, b);
                if (!counted.contains(key)) {
                    counted.add(key);
                    count++;
                }
            }
        }

        return count;
    }

    public String getMostConnectedPlayer(Map<String, Map<String, Integer>> graph) {
        return graph.entrySet().stream()
                .max(Comparator.comparingInt(e -> e.getValue().size()))
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    // Prints summary of the evolution
    public void printEvolutionSummary() {
        System.out.println("Graph evolution summary");

        if (allSeasons.isEmpty()) {
            System.out.println("No data loaded. Please check that CSV files exist in the data folder.");
            return;
        }

        System.out.printf("%-8s | %-12s | %-12s | %-14s | %-14s | %-8s | %-8s%n",
                "Season", "Nodes(year)", "Edges(year)", "Nodes(cumul)", "Edges(cumul)", "Joined", "Left");

        for (SeasonStats stats : getEvolutionStats()) {
            System.out.printf("%-8d | %-12d | %-12d | %-14d | %-14d | %-8d | %-8d%n",
                    stats.season, stats.nodesInSeason, stats.edgesInSeason,
                    stats.cumulativeNodes, stats.cumulativeEdges,
                    stats.newPlayers, stats.departedPlayers);
        }

        // Most connected player overall
        Map<String, Map<String, Integer>> fullGraph = getFullGraph();
        if (fullGraph != null && !fullGraph.isEmpty()) {
            String mostConnected = getMostConnectedPlayer(fullGraph);
            if (mostConnected != null) {
                int connections = fullGraph.get(mostConnected).size();
                System.out.println("Most connected player overall: " + mostConnected +
                        " (" + connections + " unique teammates)");
            }
        }
        System.out.println();
    }

    private String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());

        return result.toArray(new String[0]);
    }

    private int parseIntSafe(String value) {
        try {
            if (value == null || value.equals("N/A") || value.equals("-") || value.isEmpty()) {
                return -1;
            }
            return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
        } catch (Exception e) {
            return -1;
        }
    }

    // Data class for season statistics
        public record SeasonStats(int season, int nodesInSeason, int edgesInSeason, int cumulativeNodes,
                                  int cumulativeEdges, int newPlayers, int departedPlayers) {
    }

    // Test main
    public static void main(String[] args) {
        EvolutionGraphBuilder builder = new EvolutionGraphBuilder();
        builder.loadFromFolder("src/main/resources/teamsData");
        builder.printEvolutionSummary();
    }
}