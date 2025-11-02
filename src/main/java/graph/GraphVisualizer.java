package graph;

import org.graphstream.graph.Graph;
import org.graphstream.graph.implementations.SingleGraph;
import java.util.Map;

/**
 * Utility class for visualizing a football co-play graph using GraphStream (Swing version).
 */
public class GraphVisualizer {

    /**
     * Displays a graph where vertices are player names and edges show co-play relationships.
     *
     * @param edges adjacency map from GraphBuilder: player -> (teammate -> sharedSeasons)
     */
    public static void showGraph(Map<String, Map<String, Integer>> edges) {
        System.setProperty("org.graphstream.ui", "swing");

        Graph graph = new SingleGraph("Football Co-Play Graph");

        // Add all nodes
        for (String player : edges.keySet()) {
            if (graph.getNode(player) == null) {
                graph.addNode(player).setAttribute("ui.label", player);
            }

            // Add edges to teammates
            for (Map.Entry<String, Integer> teammateEntry : edges.get(player).entrySet()) {
                String teammate = teammateEntry.getKey();
                int sharedSeasons = teammateEntry.getValue();

                if (graph.getNode(teammate) == null) {
                    graph.addNode(teammate).setAttribute("ui.label", teammate);
                }

                String edgeId = player + "-" + teammate;
                if (graph.getEdge(edgeId) == null && graph.getEdge(teammate + "-" + player) == null) {
                    graph.addEdge(edgeId, player, teammate)
                            .setAttribute("ui.label", String.valueOf(sharedSeasons));
                }
            }
        }

        // Style: make it readable
        graph.setAttribute("ui.stylesheet", """
            node {
                fill-color: #4F46E5;
                size: 12px;
                text-size: 14;
                text-alignment: above;
                text-color: white;
            }
            edge {
                fill-color: gray;
                text-size: 10;
            }
        """);

        graph.display();
    }
}
