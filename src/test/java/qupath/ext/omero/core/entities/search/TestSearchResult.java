package qupath.ext.omero.core.entities.search;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
public class TestSearchResult {

    private static final DateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss Z");

    @Test
    void Check_Number_Of_Results_When_Empty() {
        List<SearchResult> searchResults = SearchResult.createFromHTMLResponse("", URI.create(""));

        int numberOfResults = searchResults.size();

        Assertions.assertEquals(0, numberOfResults);
    }

    @Test
    void Check_Number_Of_Results() {
        List<SearchResult> searchResults = createSearchResults();

        int numberOfResults = searchResults.size();

        Assertions.assertEquals(3, numberOfResults);
    }

    @Test
    void Check_Results_Types() {
        List<SearchResult> searchResults = createSearchResults();

        List<String> resultsTypes = searchResults.stream().map(SearchResult::getType).toList();

        for (String resultType: resultsTypes) {
            Assertions.assertEquals("image", resultType);
        }
    }

    @Test
    void Check_Results_Ids() {
        List<SearchResult> searchResults = createSearchResults();

        List<Integer> resultsTypes = searchResults.stream().map(SearchResult::getId).toList();

        Assertions.assertArrayEquals(new Integer[] {12546, 12547, 12548}, resultsTypes.toArray());
    }

    @Test
    void Check_Results_Names() {
        List<SearchResult> searchResults = createSearchResults();

        List<String> resultsNames = searchResults.stream().map(SearchResult::getName).toList();

        Assertions.assertArrayEquals(new String[] {
                "CMU-1.tiff [0]",
                "CMU-1.tiff [macro image]",
                "CMU-1.tiff [label image]"
        }, resultsNames.toArray());
    }

    @Test
    void Check_Results_Acquired_Dates() throws ParseException {
        List<SearchResult> searchResults = createSearchResults();

        List<Date> resultsAcquiredDates = searchResults.stream().map(SearchResult::getDateAcquired).toList();

        Assertions.assertArrayEquals(new Date[] {
                dateFormat.parse("Tue, 29 Dec 2009 09:59:15 +0000"),
                dateFormat.parse("Tue, 29 Dec 2009 09:59:15 +0000"),
                dateFormat.parse("Tue, 29 Dec 2009 09:59:15 +0000")
        }, resultsAcquiredDates.toArray());
    }

    @Test
    void Check_Results_Imported_Dates() throws ParseException {
        List<SearchResult> searchResults = createSearchResults();

        List<Date> resultsImportedDates = searchResults.stream().map(SearchResult::getDateImported).toList();

        Assertions.assertArrayEquals(new Date[]{
                dateFormat.parse("Mon, 09 Oct 2023 15:36:27 +0100"),
                dateFormat.parse("Mon, 09 Oct 2023 15:36:27 +0100"),
                dateFormat.parse("Mon, 09 Oct 2023 15:36:27 +0100")
        }, resultsImportedDates.toArray());
    }

    @Test
    void Check_Results_Groups() {
        List<SearchResult> searchResults = createSearchResults();

        List<String> resultsGroups = searchResults.stream().map(SearchResult::getGroupName).toList();

        for (String resultGroup: resultsGroups) {
            Assertions.assertEquals("MVM", resultGroup);
        }
    }

    @Test
    void Check_Results_Links() {
        List<SearchResult> searchResults = createSearchResults();

        List<String> resultsLinks = searchResults.stream().map(SearchResult::getLink).toList();

        Assertions.assertArrayEquals(new String[]{
                "https://omero-czi-cpw.mvm.ed.ac.uk/webclient/?show=image-12546",
                "https://omero-czi-cpw.mvm.ed.ac.uk/webclient/?show=image-12547",
                "https://omero-czi-cpw.mvm.ed.ac.uk/webclient/?show=image-12548"
        }, resultsLinks.toArray());
    }

    private List<SearchResult> createSearchResults() {
        return SearchResult.createFromHTMLResponse("""
                <table id="dataTable" class="tablesorter" data-result-count="3" >
                    <thead>
                        <tr>
                            <th class="table_images">Type</th>
                            <th class="table_desc">Name</th>
                            <th class="table_date">Acquired</th>
                            <th class="table_date">Imported</th>
                            <th>Group</th>
                            <th>Link</th>
                        </tr>
                    </thead>
                    <tbody>
                        <!-- NB: E.g. "#project-123 td.desc a" etc is used to update names if edited in right-panel 'editinplace.js' -->
                        <tr id="image-12546" class="canEdit canAnnotate canLink canDelete isOwned canChgrp">
                            <td class="image" id="image_icon-12546">
                                <img class="search_thumb" id="12546" alt="image" title="CMU-1.tiff [0]"
                                    src="/static/webgateway/img/spinner.gif" />
                            </td>
                            <td class="desc"><a>CMU-1.tiff [0]</a></td>
                            <td class="date" data-isodate='Tue, 29 Dec 2009 09:59:15 +0000'></td>
                            <td class="date" data-isodate='Mon, 09 Oct 2023 15:36:27 +0100'></td>
                            <td class="group">MVM</td>
                            <td><a href="/webclient/?show=image-12546" title="Show in hierarchy view">Browse</a></td>
                        </tr>
                        <tr id="image-12547" class="canEdit canAnnotate canLink canDelete isOwned canChgrp">
                            <td class="image" id="image_icon-12547">
                                <img class="search_thumb" id="12547" alt="image" title="CMU-1.tiff [macro image]"
                                    src="/static/webgateway/img/spinner.gif" />
                            </td>
                            <td class="desc"><a>CMU-1.tiff [macro image]</a></td>
                            <td class="date" data-isodate='Tue, 29 Dec 2009 09:59:15 +0000'></td>
                            <td class="date" data-isodate='Mon, 09 Oct 2023 15:36:27 +0100'></td>
                            <td class="group">MVM</td>
                            <td><a href="/webclient/?show=image-12547" title="Show in hierarchy view">Browse</a></td>
                        </tr>
                        <tr id="image-12548" class="canEdit canAnnotate canLink canDelete isOwned canChgrp">
                            <td class="image" id="image_icon-12548">
                                <img class="search_thumb" id="12548" alt="image" title="CMU-1.tiff [label image]"
                                    src="/static/webgateway/img/spinner.gif" />
                            </td>
                            <td class="desc"><a>CMU-1.tiff [label image]</a></td>
                            <td class="date" data-isodate='Tue, 29 Dec 2009 09:59:15 +0000'></td>
                            <td class="date" data-isodate='Mon, 09 Oct 2023 15:36:27 +0100'></td>
                            <td class="group">MVM</td>
                            <td><a href="/webclient/?show=image-12548" title="Show in hierarchy view">Browse</a></td>
                        </tr>
                    </tbody>
                </table>
                """, URI.create("https://omero-czi-cpw.mvm.ed.ac.uk"));
    }
}
