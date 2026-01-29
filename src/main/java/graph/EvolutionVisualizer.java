package graph;

import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.view.Viewer;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/*
 * EvolutionVisualizer provides animated and interactive visualization
 * of how the football co-play graph evolves over seasons.
 */
public class EvolutionVisualizer {

    private final EvolutionGraphBuilder dataBuilder;
    private Graph displayGraph;
    private JLabel statusLabel;
    private JSlider seasonSlider;
    private JCheckBox cumulativeMode;
    private JCheckBox highlightChanges;
    private JComboBox<String> teamFilter;
    private JSpinner minEdgeWeight;
    private JSpinner maxNodes;

    private int currentSeason;
    private final List<Integer> seasons;
    private volatile boolean isAnimating = false;

    // Filter settings
    private String selectedTeam = "All Teams";
    private int minWeight = 1;
    private int maxNodeCount = 100;

    // Colors
    private static final String NODE_COLOR_NORMAL = "#4F46E5";
    private static final String NODE_COLOR_NEW = "#22C55E";
    private static final String NODE_COLOR_LEAVING = "#EF4444";
    private static final String EDGE_COLOR_NORMAL = "#9CA3AF";
    private static final String EDGE_COLOR_NEW = "#86FACE";

    /**
     * Creates a new evolution visualizer.
     *
     * @param builder the data builder with loaded temporal data
     */
    public EvolutionVisualizer(EvolutionGraphBuilder builder) {
        this.dataBuilder = builder;
        this.seasons = new ArrayList<>(builder.getAllSeasons());

        if (!seasons.isEmpty()) {
            this.currentSeason = seasons.getFirst();
        }
    }

    //Launches the interactive visualization with controls.
    public void show() {
        System.setProperty("org.graphstream.ui", "swing");

        // Create the graph
        displayGraph = new SingleGraph("Football Team Evolution");
        displayGraph.setAttribute("ui.quality");
        displayGraph.setAttribute("ui.antialias");
        applyDefaultStyle();

        // Create viewer
        Viewer viewer = displayGraph.display();
        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.HIDE_ONLY);

        // Create control panel
        createControlPanel();

        // Show initial state
        updateGraphForSeason(currentSeason);
    }

    private void createControlPanel() {
        JFrame controlFrame = new JFrame("Evolution Controls");
        controlFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        controlFrame.setSize(700, 280);
        controlFrame.setLayout(new BorderLayout(10, 10));

        // Top: Status label
        statusLabel = new JLabel("Season: " + currentSeason, SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        controlFrame.add(statusLabel, BorderLayout.NORTH);

        // Center panel with slider and filters
        JPanel centerPanel = new JPanel(new GridLayout(3, 1, 5, 5));

        // Row 1 Season slider
        JPanel sliderPanel = new JPanel(new BorderLayout());
        seasonSlider = new JSlider(0, seasons.size() - 1, 0);
        seasonSlider.setMajorTickSpacing(5);
        seasonSlider.setMinorTickSpacing(1);
        seasonSlider.setPaintTicks(true);
        seasonSlider.setPaintLabels(true);

        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        for (int i = 0; i < seasons.size(); i += 5) {
            labelTable.put(i, new JLabel(String.valueOf(seasons.get(i))));
        }
        seasonSlider.setLabelTable(labelTable);

        seasonSlider.addChangeListener(e -> {
            if (!seasonSlider.getValueIsAdjusting()) {
                currentSeason = seasons.get(seasonSlider.getValue());
                updateGraphForSeason(currentSeason);
            }
        });

        sliderPanel.add(new JLabel(" Season: "), BorderLayout.WEST);
        sliderPanel.add(seasonSlider, BorderLayout.CENTER);
        centerPanel.add(sliderPanel);

        // Row 2 Filters
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // Team filter
        filterPanel.add(new JLabel("Team:"));
        String[] teamOptions = new String[dataBuilder.getAllTeams().size() + 1];
        teamOptions[0] = "All Teams";
        int i = 1;
        for (String team : dataBuilder.getAllTeams()) {
            teamOptions[i++] = team;
        }
        teamFilter = new JComboBox<>(teamOptions);
        teamFilter.addActionListener(e -> {
            selectedTeam = (String) teamFilter.getSelectedItem();
            updateGraphForSeason(currentSeason);
        });
        filterPanel.add(teamFilter);

        // Min edge weight filter
        filterPanel.add(new JLabel("  Min seasons together:"));
        minEdgeWeight = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        minEdgeWeight.addChangeListener(e -> {
            minWeight = (Integer) minEdgeWeight.getValue();
            updateGraphForSeason(currentSeason);
        });
        filterPanel.add(minEdgeWeight);

        // Max nodes filter
        filterPanel.add(new JLabel("  Max players:"));
        maxNodes = new JSpinner(new SpinnerNumberModel(100, 10, 500, 10));
        maxNodes.addChangeListener(e -> {
            maxNodeCount = (Integer) maxNodes.getValue();
            updateGraphForSeason(currentSeason);
        });
        filterPanel.add(maxNodes);

        centerPanel.add(filterPanel);

        // Row 3 Checkboxes
        JPanel checkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cumulativeMode = new JCheckBox("Cumulative", true);
        cumulativeMode.addActionListener(e -> updateGraphForSeason(currentSeason));

        highlightChanges = new JCheckBox("Highlight Changes", true);
        highlightChanges.addActionListener(e -> updateGraphForSeason(currentSeason));

        checkPanel.add(cumulativeMode);
        checkPanel.add(highlightChanges);
        centerPanel.add(checkPanel);

        controlFrame.add(centerPanel, BorderLayout.CENTER);

        // Bottom: Navigation buttons
        JPanel buttonPanel = getJPanel();

        controlFrame.add(buttonPanel, BorderLayout.SOUTH);

        controlFrame.setVisible(true);
    }

    private JPanel getJPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout());

        JButton prevBtn = new JButton("<- Previous");
        prevBtn.addActionListener(e -> navigateSeason(-1));

        JButton nextBtn = new JButton("Next ->");
        nextBtn.addActionListener(e -> navigateSeason(1));

        JButton animateBtn = new JButton("> Animate");
        animateBtn.addActionListener(e -> toggleAnimation(animateBtn));

        buttonPanel.add(prevBtn);
        buttonPanel.add(nextBtn);
        buttonPanel.add(animateBtn);
        return buttonPanel;
    }

    private void navigateSeason(int direction) {
        int currentIndex = seasons.indexOf(currentSeason);
        int newIndex = currentIndex + direction;

        if (newIndex >= 0 && newIndex < seasons.size()) {
            currentSeason = seasons.get(newIndex);
            seasonSlider.setValue(newIndex);
            updateGraphForSeason(currentSeason);
        }
    }

    private void toggleAnimation(JButton button) {
        if (isAnimating) {
            isAnimating = false;
            button.setText("> Animate");
        } else {
            isAnimating = true;
            button.setText("II Stop");

            new Thread(() -> {
                int startIndex = seasons.indexOf(currentSeason);

                for (int i = startIndex; i < seasons.size() && isAnimating; i++) {
                    final int seasonIndex = i;
                    SwingUtilities.invokeLater(() -> {
                        currentSeason = seasons.get(seasonIndex);
                        seasonSlider.setValue(seasonIndex);
                        updateGraphForSeason(currentSeason);
                    });
                }

                isAnimating = false;
                SwingUtilities.invokeLater(() -> button.setText("II Animate"));
            }).start();
        }
    }

    // Updates the graph display for a specific season.
    private void updateGraphForSeason(int season) {
        // Get filtered graph data
        Map<String, Map<String, Integer>> graphData = getFilteredGraph(season);

        // Get change information for highlighting
        Set<String> newPlayers = dataBuilder.getNewPlayers(season);
        Set<String> departingPlayers = dataBuilder.getDepartedPlayers(season);

        // Clear existing graph
        displayGraph.clear();
        applyDefaultStyle();

        // Track edges from previous season for highlighting
        Set<String> previousEdges = new HashSet<>();
        if (highlightChanges.isSelected() && season > seasons.getFirst()) {
            Map<String, Map<String, Integer>> prevGraph = getFilteredGraph(season - 1);
            for (String a : prevGraph.keySet()) {
                for (String b : prevGraph.get(a).keySet()) {
                    previousEdges.add(createEdgeId(a, b));
                }
            }
        }

        // Add nodes
        for (String player : graphData.keySet()) {
            Node node = displayGraph.addNode(player);
            node.setAttribute("ui.label", shortenName(player));

            // Color based on status
            if (highlightChanges.isSelected()) {
                if (newPlayers.contains(player)) {
                    node.setAttribute("ui.style", "fill-color: " + NODE_COLOR_NEW + "; size: 15px;");
                } else if (departingPlayers.contains(player)) {
                    node.setAttribute("ui.style", "fill-color: " + NODE_COLOR_LEAVING + "; size: 15px;");
                }
            }
        }

        // Add edges
        for (String playerA : graphData.keySet()) {
            for (Map.Entry<String, Integer> edge : graphData.get(playerA).entrySet()) {
                String playerB = edge.getKey();
                int weight = edge.getValue();

                String edgeId = createEdgeId(playerA, playerB);
                if (displayGraph.getEdge(edgeId) == null) {
                    Edge e = displayGraph.addEdge(edgeId, playerA, playerB);

                    // Show weight on edge
                    if (weight > 1) {
                        e.setAttribute("ui.label", String.valueOf(weight));
                    }

                    // Highlight new edges
                    if (highlightChanges.isSelected() && !previousEdges.contains(edgeId)) {
                        e.setAttribute("ui.style", "fill-color: " + EDGE_COLOR_NEW + "; size: 2px;");
                    }
                }
            }
        }

        // Update status
        int nodeCount = displayGraph.getNodeCount();
        int edgeCount = displayGraph.getEdgeCount();
        String mode = cumulativeMode.isSelected() ? "Cumulative" : "Snapshot";
        String teamInfo = selectedTeam.equals("All Teams") ? "All" : selectedTeam;

        statusLabel.setText(String.format("Season %d | %s | %s | Nodes: %d | Edges: %d",
                season, mode, teamInfo, nodeCount, edgeCount));
    }

    private Map<String, Map<String, Integer>> getFilteredGraph(int season) {
        // Get base graph
        Map<String, Map<String, Integer>> graphData;
        if (cumulativeMode.isSelected()) {
            graphData = dataBuilder.getCumulativeGraph(season);
        } else {
            graphData = dataBuilder.getSeasonSnapshot(season);
        }

        // Filter by team if selected
        if (!selectedTeam.equals("All Teams")) {
            Set<String> teamPlayers = dataBuilder.getPlayersForTeam(selectedTeam, season);
            graphData = filterByPlayers(graphData, teamPlayers);
        }

        // Filter by minimum edge weight
        if (minWeight > 1) {
            graphData = filterByMinWeight(graphData, minWeight);
        }

        // Limit number of nodes (keep most connected)
        if (graphData.size() > maxNodeCount) {
            graphData = limitToTopNodes(graphData, maxNodeCount);
        }

        return graphData;
    }

    private Map<String, Map<String, Integer>> filterByPlayers(
            Map<String, Map<String, Integer>> graph, Set<String> players) {
        Map<String, Map<String, Integer>> filtered = new HashMap<>();

        for (String player : players) {
            if (graph.containsKey(player)) {
                Map<String, Integer> edges = new HashMap<>();
                for (Map.Entry<String, Integer> edge : graph.get(player).entrySet()) {
                    if (players.contains(edge.getKey())) {
                        edges.put(edge.getKey(), edge.getValue());
                    }
                }
                if (!edges.isEmpty()) {
                    filtered.put(player, edges);
                }
            }
        }

        return filtered;
    }

    private Map<String, Map<String, Integer>> filterByMinWeight(
            Map<String, Map<String, Integer>> graph, int minW) {
        Map<String, Map<String, Integer>> filtered = new HashMap<>();

        for (Map.Entry<String, Map<String, Integer>> entry : graph.entrySet()) {
            Map<String, Integer> edges = new HashMap<>();
            for (Map.Entry<String, Integer> edge : entry.getValue().entrySet()) {
                if (edge.getValue() >= minW) {
                    edges.put(edge.getKey(), edge.getValue());
                }
            }
            if (!edges.isEmpty()) {
                filtered.put(entry.getKey(), edges);
            }
        }

        return filtered;
    }

    private Map<String, Map<String, Integer>> limitToTopNodes(
            Map<String, Map<String, Integer>> graph, int maxN) {
        // Sort players by number of connections
        List<String> sortedPlayers = graph.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(maxN)
                .map(Map.Entry::getKey)
                .toList();

        Set<String> topPlayers = new HashSet<>(sortedPlayers);
        return filterByPlayers(graph, topPlayers);
    }

    private String createEdgeId(String a, String b) {
        return a.compareTo(b) < 0 ? a + "-" + b : b + "-" + a;
    }

    private String shortenName(String fullName) {
        String[] parts = fullName.split(" ");
        if (parts.length >= 2) {
            return parts[0].charAt(0) + ". " + parts[parts.length - 1];
        }
        return fullName.length() > 15 ? fullName.substring(0, 12) + "..." : fullName;
    }

    private void applyDefaultStyle() {
        displayGraph.setAttribute("ui.stylesheet", """
            graph {
                padding: 50px;
            }
            node {
                fill-color: %s;
                size: 10px;
                text-size: 11;
                text-alignment: above;
                text-color: #1F2937;
                text-background-mode: rounded-box;
                text-background-color: #F3F4F6;
                text-padding: 2px;
            }
            edge {
                fill-color: %s;
                size: 1px;
                text-size: 9;
                text-color: #6B7280;
            }
        """.formatted(NODE_COLOR_NORMAL, EDGE_COLOR_NORMAL));
    }

    public static void visualize(EvolutionGraphBuilder builder) {
        EvolutionVisualizer visualizer = new EvolutionVisualizer(builder);
        visualizer.show();
    }


    // Test main
    public static void main(String[] args) {
        EvolutionGraphBuilder builder = new EvolutionGraphBuilder();
        builder.loadFromFolder("src/main/resources/teamsData");
        builder.printEvolutionSummary();

        EvolutionVisualizer.visualize(builder);
    }
}