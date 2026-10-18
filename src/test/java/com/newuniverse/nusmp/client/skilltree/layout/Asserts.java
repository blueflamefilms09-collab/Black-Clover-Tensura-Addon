package com.newuniverse.nusmp.client.skilltree.layout;

import java.util.Arrays;

/**
 * Why: the polyline and router tests compare whole coordinate arrays, and a failure that says "arrays differ" without printing them wastes the one minute a person has to find the
 * wrong coordinate. This prints both arrays; it also does not depend on which JUnit overloads a given test runner offers.
 */
final class Asserts {
    private Asserts() {}

    static void ints(int[] expected, int[] actual) { ints(expected, actual, null); }

    static void ints(int[] expected, int[] actual, String message) {
        if (!Arrays.equals(expected, actual))
            throw new AssertionError((message == null ? "" : message + ": ") + "expected " + Arrays.toString(expected) + " but was " + Arrays.toString(actual));
    }
}
