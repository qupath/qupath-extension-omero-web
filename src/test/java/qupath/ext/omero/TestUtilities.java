package qupath.ext.omero;

import org.junit.jupiter.api.Assertions;

import java.util.List;

/**
 * Adds some utility functions for testing.
 */
public class TestUtilities {

    private TestUtilities() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Assert that two lists are equal without taking the order
     * of elements into account.
     * This function doesn't work if some duplicates are present in one
     * of the list.
     *
     * @param expectedList  the expected values
     * @param actualList  the actual values
     * @param <T>  the type of the elements of the list
     */
    public static <T> void assertListEqualsWithoutOrder(List<? extends T> expectedList, List<? extends T> actualList) {
        Assertions.assertEquals(expectedList.size(), actualList.size());
        Assertions.assertTrue(expectedList.containsAll(actualList));
        Assertions.assertTrue(actualList.containsAll(expectedList));
    }
}
