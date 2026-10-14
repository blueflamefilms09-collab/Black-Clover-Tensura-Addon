package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;

import java.util.List;

/** Preview-only stand-in for the real WikiBooks (no new-attribute sigils on the Compatibility Codex). */
public final class WikiBooks {
    private WikiBooks() {}
    public static boolean isNewAttribute(MagicType magic) { return false; }
    public static List<MagicType> newAttributes() { return List.of(); }
}
