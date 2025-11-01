package scraper;

import model.Player;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;

public class PlayerScraper {

    /**
     * Parses a Transfermarkt team page and extracts a list of players.
     *
     * @param html the URL of the team roster page for a specific season
     *             (e.g., "https://www.transfermarkt.com/.../saison_id=2022")
     * @return a list of {@link Player} objects representing players in the squad
     * @throws IOException if a connection or parsing error occurs while fetching the page
     */
    public static List<Player> parsePlayers(String html) throws IOException {
        List<Player> players = new ArrayList<>();
        Document doc = Jsoup.connect(html).get();

        Elements rows = doc.select("table.items > tbody > tr");

        for (Element row : rows) {
            try {
                // Player number (parsed safely)
                String numberText = safeText(row.select("td.zentriert.rueckennummer div.rn_nummer"));
                int number = parseIntSafe(numberText);

                // Player name and position
                String name = safeText(row.select("td.posrela td.hauptlink a"));
                String position = safeText(row.select("td.posrela tr:last-child td"));

                // Date of birth and age
                String dobAndAge = safeText(row.select("td:eq(2)")); // Example: "04/11/2002 (20)"
                String dateOfBirth = dobAndAge.contains("(")
                        ? dobAndAge.substring(0, dobAndAge.indexOf("(")).trim()
                        : dobAndAge;
                int age = parseIntSafe(dobAndAge.replaceAll(".*\\((\\d+)\\).*", "$1"));

                // Nationality
                String nationality = safeAttr(row.select("td:eq(3) img"), "alt");

                // Current club
                String currentClub = safeAttr(row.select("td:eq(4) a"), "title");

                // Height, foot, joined date, previous club
                String height = safeText(row.select("td:eq(5)"));
                String foot = safeText(row.select("td:eq(6)"));
                String joined = safeText(row.select("td:eq(7)"));
                String signedFrom = safeAttr(row.select("td:eq(8) a"), "title");

                // Market value
                String marketValue = safeText(row.select("td:eq(9)"));

                Player player = new Player(
                        number, name, position, dateOfBirth, age,
                        nationality, currentClub, height, foot, joined, signedFrom, marketValue
                );
                players.add(player);

            } catch (Exception e) {
                System.err.println("Error parsing table row: " + e.getMessage());
            }
        }

        return players;
    }

    /**
     * Safely extracts and cleans text content from a Jsoup element selection.
     * If the element is missing, empty, or contains "-", returns "N/A".
     *
     * @param elements the Jsoup Elements selection
     * @return cleaned text value or "N/A" if not available
     */
    private static String safeText(Elements elements) {
        if (elements == null || elements.isEmpty()) return "N/A";
        String text = elements.text().trim();
        return (text.isEmpty() || text.equals("-")) ? "N/A" : text;
    }

    /**
     * Safely extracts an attribute value (e.g., "alt", "title") from an HTML element.
     * Returns "N/A" if the attribute or element is missing.
     *
     * @param elements the Jsoup Elements selection
     * @param attr     the attribute name to extract
     * @return the attribute value or "N/A" if not found
     */
    private static String safeAttr(Elements elements, String attr) {
        if (elements == null || elements.isEmpty()) return "N/A";
        String val = elements.attr(attr).trim();
        return (val.isEmpty() || val.equals("-")) ? "N/A" : val;
    }

    /**
     * Safely converts a string to an integer.
     * Removes non-digit characters before parsing.
     * Returns -1 if conversion fails or value is not numeric.
     *
     * @param value the input string (e.g., "20", "(25)", or "-")
     * @return the parsed integer value, or -1 if invalid
     */
    private static int parseIntSafe(String value) {
        try {
            if (value == null || value.equals("N/A") || value.equals("-") || value.isEmpty()) return -1;
            return Integer.parseInt(value.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return -1;
        }
    }
}
