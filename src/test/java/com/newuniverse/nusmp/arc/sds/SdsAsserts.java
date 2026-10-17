package com.newuniverse.nusmp.arc.sds;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Why: the unit tests of the four Sins compare short arrays of numbers (a direction, a point on a dive) all the time, and the quick JUnit shim the team runs them on
 * between builds has no double-array assertion while the real JUnit has. One tiny helper that uses only the assertions both have keeps every test compiling in both.
 */
public final class SdsAsserts {
    private SdsAsserts() {}

    /** Same length, and every element within {@code eps} of the expected one. */
    public static void assertNear(double[] expected, double[] actual, double eps) {
        assertEquals(expected.length, actual.length, "same length");
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], actual[i], eps, "element " + i);
    }
}
