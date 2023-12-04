package qupath.ext.omero.core.entities.search;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.omero.core.entities.repositoryentities.RepositoryEntity;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.*;
import qupath.ext.omero.core.entities.repositoryentities.serverentities.image.Image;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>The result of a search query.</p>
 * <p>
 *     This class can create usable results from an HTML search query response
 *     (usually from {@code https://omero-server/webclient/load_searching/form}).
 * </p>
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

    /**
     * Creates a new search result corresponding to an OMERO object.
     * You should rather use {@link #createFromHTMLResponse(String, URI)}
     * to create search results.
     *
     * @param type  the type of the object (e.g. dataset, image)
     * @param id  the id of the object
     * @param name  the name of the object
     * @param group  the group name whose object belongs to
     * @param link  a URL linking to the object
     * @param dateAcquired  the date this object was acquired
     * @param dateImported  the date this object was imported
     */
    public SearchResult(
            String type,
            int id,
            String name,
            String group,
            String link,
            @Nullable Date dateAcquired,
            @Nullable Date dateImported
    ) {
        this.type = type;
        this.id = id;
        this.name = name;
        this.dateAcquired = dateAcquired;
        this.dateImported = dateImported;
        this.group = group;
        this.link = link;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof SearchResult searchResult))
            return false;
        return searchResult.id == id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
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

                Date acquiredDate = null;
                try {
                    acquiredDate = OMERO_DATE_FORMAT.parse(findPatternInText(DATE_PATTERN, row).orElse(""));
                } catch (ParseException e) {
                    logger.info("Could not parse acquired date.", e);
                }

                Date importedDate = null;
                try {
                    importedDate = OMERO_DATE_FORMAT.parse(findPatternInText(DATE_PATTERN, row, 1).orElse(""));
                } catch (ParseException e) {
                    logger.info("Could not parse imported date.", e);
                }

                searchResults.add(new SearchResult(
                        rowMatcher.group(1),
                        Integer.parseInt(rowMatcher.group(2)),
                        findPatternInText(DESCRIPTION_PATTERN, row).orElse("-"),
                        findPatternInText(GROUP_PATTERN, row).orElse("-"),
                        serverURI + findPatternInText(LINK_PATTERN, row).orElse(""),
                        acquiredDate,
                        importedDate
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
     * @return the class of the type (e.g. image, dataset) of the result, or an empty Optional if not found
     */
    public Optional<Class<? extends RepositoryEntity>> getType() {
        if (type.equalsIgnoreCase("image")) {
            return Optional.of(Image.class);
        } else if (type.equalsIgnoreCase("dataset")) {
            return Optional.of(Dataset.class);
        } else if (type.equalsIgnoreCase("project")) {
            return Optional.of(Project.class);
        } else if (type.equalsIgnoreCase("screen")) {
            return Optional.of(Screen.class);
        } else if (type.equalsIgnoreCase("plate")) {
            return Optional.of(Plate.class);
        } else if (type.equalsIgnoreCase("well")) {
            return Optional.of(Well.class);
        } else {
            return Optional.empty();
        }
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
     * @return the date the result was acquired, or an empty Optional
     * if the date couldn't be retrieved
     */
    public Optional<Date> getDateAcquired() {
        return Optional.ofNullable(dateAcquired);
    }

    /**
     * @return the date the result was imported, or an empty Optional
     * if the date couldn't be retrieved
     */
    public Optional<Date> getDateImported() {
        return Optional.ofNullable(dateImported);
    }

    /**
     * @return the group name of the result
     */
    public String getGroupName() {
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
