package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;

/**
 * 1.93.0 Why: one dispatch from {@link WikiBooks#attribute} to the rebuilt books of the wiki magics that were rebuilt after the anime (Smoke, Snow, Wing, Vortex, Air),
 * so WikiBooks gets a single new line. Ash has its own class since 0.34 (AshBook, rebuilt in place). The Air Magic book itself is built here.
 */
public final class AirBook {
    private AirBook() {}

    /** The rebuilt book of this magic, or null when the generic wiki book is still the one. */
    public static GrimoireBook rebuilt(MagicType magic) {
        return switch (magic) {
            case SMOKE -> SmokeBook.create();
            default -> null;
        };
    }
}
