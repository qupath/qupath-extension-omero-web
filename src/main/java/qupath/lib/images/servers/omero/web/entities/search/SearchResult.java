package qupath.lib.images.servers.omero.web.entities.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>The result of a search query.</p>
 * <p>This class can create usable results from an HTML search query response.</p>
 */
public class SearchResult {

    private static final Logger logger = LoggerFactory.getLogger(SearchResult.class);
    private static final DateFormat OMERO_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z");
    private static final Pattern ROW_PATTERN = Pattern.compile("<tr id=\"(.+?)-(.+?)\".+?</tr>", Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("<td class=\"desc\"><a>(.+?)</a></td>");
    private static final Pattern DATE_PATTERN = Pattern.compile("<td class=\"date\" data-isodate='(.+?)'></td>");
    private static final Pattern GROUP_PATTERN = Pattern.compile("<td class=\"group\">(.+?)</td>");
    private static final Pattern LINK_PATTERN = Pattern.compile("<td><a href=\"(.+?)\"");
    private final String type;
    private final int id;
    private final String name;
    private final Date dateAcquired;
    private final Date dateImported;
    private final String group;
    private final String link;

    private SearchResult(
            String type,
            int id,
            String name,
            Date dateAcquired,
            Date dateImported,
            String group,
            String link
    ) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.dateAcquired = dateAcquired;
        this.dateImported = dateImported;
        this.group = group;
        this.link = link;
    }

    /**
     * Creates a list of results from an HTML search query response.
     *
     * @param htmlResponse  the HTML search query response
     * @param serverURI  the URI of the server
     * @return a list of search results, or an empty list if an error occurred or no result was found
     */
    public static List<SearchResult> createFromHTMLResponse(String htmlResponse, URI serverURI) {
        List<SearchResult> searchResults = new ArrayList<>();
        Matcher rowMatcher = ROW_PATTERN.matcher(htmlResponse);

        while (rowMatcher.find()) {
            try {
                String row = rowMatcher.group(0);

                searchResults.add(new SearchResult(
                        rowMatcher.group(1),
                        Integer.parseInt(rowMatcher.group(2)),
                        findPatternInText(DESCRIPTION_PATTERN, row).orElse("-"),
                        OMERO_DATE_FORMAT.parse(findPatternInText(DATE_PATTERN, row).orElse("")),
                        OMERO_DATE_FORMAT.parse(findPatternInText(DATE_PATTERN, row, 1).orElse("")),
                        findPatternInText(GROUP_PATTERN, row).orElse("-"),
                        serverURI + findPatternInText(LINK_PATTERN, row).orElse("")
                ));
            } catch (Exception e) {
                logger.warn("Could not parse search result.", e);
            }
        }

        return searchResults;
    }

    @Override
    public String toString() {
        return String.format("Search result: %s of ID %d and name %s", type, id, name);
    }

    /**
     * @return the type (e.g. image, dataset) of the result
     */
    public String getType() {
        return type;
    }

    /**
     * @return the ID of the result
     */
    public int getId() {
        return id;
    }

    /**
     * @return the name of the result
     */
    public String getName() {
        return name;
    }

    /**
     * @return the date the result was acquired
     */
    public Date getDateAcquired() {
        return dateAcquired;
    }

    /**
     * @return the date the result was imported
     */
    public Date getDateImported() {
        return dateImported;
    }

    /**
     * @return the group of the result
     */
    public String getGroup() {
        return group;
    }

    /**
     * @return a link to open the result in a web browser
     */
    public String getLink() {
        return link;
    }

    private static Optional<String> findPatternInText(Pattern pattern, String text) {
        return findPatternInText(pattern, text, 0);
    }

    private static Optional<String> findPatternInText(Pattern pattern, String text, int occurrenceIndex) {
        Matcher matcher = pattern.matcher(text);

        for (int i=0; i<occurrenceIndex; ++i) {
            if (!matcher.find()) {
                return Optional.empty();
            }
        }

        if (matcher.find()) {
            return Optional.of(matcher.group(1));
        } else {
            return Optional.empty();
        }
    }
}
