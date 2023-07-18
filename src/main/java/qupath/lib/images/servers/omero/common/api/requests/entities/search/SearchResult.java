package qupath.lib.images.servers.omero.common.api.requests.entities.search;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>The result of a search query.</p>
 * <p>This class can create usable results from an HTML search query response.</p>
 */
public class SearchResult {
    private static final Logger logger = LoggerFactory.getLogger(SearchResult.class);
    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
    private static final Pattern patternRow = Pattern.compile("<tr id=\"(.+?)-(.+?)\".+?</tr>", Pattern.DOTALL | Pattern.MULTILINE);
    private static final Pattern patternDesc = Pattern.compile("<td class=\"desc\"><a>(.+?)</a></td>");
    private static final Pattern patternDate = Pattern.compile("<td class=\"date\">(.+?)</td>");
    private static final Pattern patternGroup = Pattern.compile("<td class=\"group\">(.+?)</td>");
    private static final Pattern patternLink = Pattern.compile("<td><a href=\"(.+?)\"");
    private static final Pattern[] patterns = new Pattern[] { patternDesc, patternDate, patternDate, patternGroup, patternLink };
    private final String type;
    private final int id;
    private final String name;
    private final Date dateAcquired;
    private final Date dateImported;
    private final String group;
    private final String link;

    private SearchResult(String[] values, URI serverURI) throws ParseException {
        this.type = values[0];
        this.id = Integer.parseInt(values[1]);
        this.name = values[2];
        this.dateAcquired = dateFormat.parse(values[3]);
        this.dateImported = dateFormat.parse(values[4]);
        this.group = values[5];
        this.link = serverURI + values[6];
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
        Matcher rowMatcher = patternRow.matcher(htmlResponse);

        while (rowMatcher.find()) {
            try {
                String[] values = new String[7];
                String row = rowMatcher.group(0);
                values[0] = rowMatcher.group(1);
                values[1] = rowMatcher.group(2);
                String value = "";

                int nValue = 2;
                for (var pattern: patterns) {
                    Matcher matcher = pattern.matcher(row);
                    if (matcher.find()) {
                        value = matcher.group(1);
                        row = row.substring(matcher.end());
                    }
                    values[nValue++] = value;
                }

                searchResults.add(new SearchResult(values, serverURI));
            } catch (Exception e) {
                logger.error("Could not parse search result.", e);
            }
        }

        return searchResults;
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
}
