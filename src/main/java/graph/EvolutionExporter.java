package graph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
 * Utility class for exporting graph evolution statistics and data.
 */
public class EvolutionExporter {

    /**
     * @param builder the evolution graph builder with loaded data
     * @param outputPath path for the output CSV file
     */
    public static void exportEvolutionStats(EvolutionGraphBuilder builder, String outputPath) {
        List<EvolutionGraphBuilder.SeasonStats> stats = builder.getEvolutionStats();

        try (FileWriter writer = new FileWriter(outputPath)) {
            // Header
            writer.append("Season,NodesInSeason,EdgesInSeason,CumulativeNodes,CumulativeEdges,NewPlayers,DepartedPlayers,AvgDegree\n");

            // Data rows
            for (EvolutionGraphBuilder.SeasonStats s : stats) {
                double avgDegree = s.nodesInSeason() > 0
                    ? (2.0 * s.edgesInSeason()) / s.nodesInSeason()
                    : 0;

                writer.append(String.format("%d,%d,%d,%d,%d,%d,%d,%.2f\n",
                    s.season(),
                    s.nodesInSeason(),
                    s.edgesInSeason(),
                    s.cumulativeNodes(),
                    s.cumulativeEdges(),
                    s.newPlayers(),
                    s.departedPlayers(),
                    avgDegree
                ));
            }

            System.out.println("Evolution statistics exported to: " + outputPath);

        } catch (IOException e) {
            System.err.println("Error exporting stats: " + e.getMessage());
        }
    }

    /**
     * @param builder the evolution graph builder
     * @param outputPath output file path
     * @param topN number of top players per season
     */
    public static void exportTopConnectedPlayers(EvolutionGraphBuilder builder, String outputPath, int topN) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.append("Season,Rank,PlayerName,Connections\n");

            for (int season : builder.getAllSeasons()) {
                Map<String, Map<String, Integer>> graph = builder.getSeasonSnapshot(season);
                
                // Sort players by connection count
                List<Map.Entry<String, Map<String, Integer>>> sorted = graph.entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                    .limit(topN)
                    .toList();

                int rank = 1;
                for (Map.Entry<String, Map<String, Integer>> entry : sorted) {
                    writer.append(String.format("%d,%d,\"%s\",%d\n",
                        season, rank++, entry.getKey(), entry.getValue().size()));
                }
            }

            System.out.println("Top connected players exported to: " + outputPath);

        } catch (IOException e) {
            System.err.println("Error exporting top players: " + e.getMessage());
        }
    }

    /**
     * @param builder the evolution graph builder
     * @param outputPath output file path
     */
    public static void exportRosterChanges(EvolutionGraphBuilder builder, String outputPath) {
        try (FileWriter writer = new FileWriter(outputPath)) {
            writer.append("Season,ChangeType,PlayerName\n");

            for (int season : builder.getAllSeasons()) {
                Set<String> newPlayers = builder.getNewPlayers(season);
                Set<String> departedPlayers = builder.getDepartedPlayers(season);

                for (String player : newPlayers) {
                    writer.append(String.format("%d,JOINED,\"%s\"\n", season, player));
                }
                for (String player : departedPlayers) {
                    writer.append(String.format("%d,LEFT,\"%s\"\n", season, player));
                }
            }

            System.out.println("Roster changes exported to: " + outputPath);

        } catch (IOException e) {
            System.err.println("Error exporting roster changes: " + e.getMessage());
        }
    }

    // Test main
    public static void main(String[] args) {
        EvolutionGraphBuilder builder = new EvolutionGraphBuilder();
        builder.loadFromFolder("src/main/resources/teamsData");

        // Export all statistics
        exportEvolutionStats(builder, "src/main/resources/output/evolution_stats.csv");
        exportTopConnectedPlayers(builder, "src/main/resources/output/top_players.csv", 5);
        exportRosterChanges(builder, "src/main/resources/output/roster_changes.csv");
    }
}
