package qupath.ext.omero;

import org.junit.jupiter.api.Assertions;

import java.util.Collection;

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
     * @param expectedCollection  the expected values
     * @param actualCollection  the actual values
     * @param <T>  the type of the elements of the collection
     */
    public static <T> void assertCollectionsEqualsWithoutOrder(Collection<? extends T> expectedCollection, Collection<? extends T> actualCollection) {
        Assertions.assertEquals(expectedCollection.size(), actualCollection.size());
        Assertions.assertTrue(expectedCollection.containsAll(actualCollection));
        Assertions.assertTrue(actualCollection.containsAll(expectedCollection));
    }
}
