package com.newuniverse.nusmp.skill;

/**
 * Why (3.42.0): the pure rules of {@link ResetGuard}, with no game classes, so they are unit-tested: which skills are ours, which call stacks are "a Tensura Reset Scroll", and when a removal is refused.
 */
public final class ResetRules {
    /** Tensura's scroll class: the frames that make a skill removal "a reset". */
    public static final String SCROLL_CLASS = "io.github.manasmods.tensura.item.misc.ResetScrollItem";
    /** Every skill this mod registers lives in this namespace. */
    public static final String NAMESPACE = "anime_multiverse";

    private ResetRules() {}

    /** Is a skill of this namespace one of ours? */
    public static boolean isOurs(String namespace) { return NAMESPACE.equals(namespace); }

    /** Is this class name Tensura's Reset Scroll, or a class nested in it? */
    public static boolean isScrollClass(String className) { return className != null && (className.equals(SCROLL_CLASS) || className.startsWith(SCROLL_CLASS + "$")); }

    /** Does any of these frames (class names) belong to the Reset Scroll? */
    public static boolean anyScrollFrame(Iterable<String> classNames) {
        for (String c : classNames) if (isScrollClass(c)) return true;
        return false;
    }

    /** The decision: refuse a removal when the guard is on, the skill is ours and the removal comes out of a Reset Scroll. Everything else (our own removals, the admin command, other mods) passes. */
    public static boolean refuses(boolean guardOn, String skillNamespace, boolean fromScroll) { return guardOn && isOurs(skillNamespace) && fromScroll; }

    // ---------------------------------------------------------------- 4.67.0: soul-tied skills
    /**
     * Owner request (4.67.0): a Reset Scroll takes this mod's skills away like any other, except the ones tied to the soul: the grimoires (every book, every page, the per-magic grimoire skills, the retired merged
     * books), the Spirit Lord skills that grow out of a grimoire, the Forbidden Magic a devil contract writes and the devil contract's own bond. Those stay and are only switched off. Path-based on purpose: a new
     * book or page is covered by its id without a list to keep up. {@code book_maker} is a different skill (the Ultimate, not a grimoire) and is not soul-tied.
     */
    public static boolean soulTied(String path) {
        if (path == null || path.isEmpty() || path.equals("book_maker")) return false;
        return path.startsWith("grimoire_") || path.startsWith("book_") || path.startsWith("page_") || path.startsWith("merged_")
                || path.equals("forbidden_magic") || path.equals("devil_bond") || path.equals("devil_magic")
                || path.equals("anti_magic_spirit_lord") || path.equals("spirit_lord");
    }

    /**
     * The 4.67.0 decision. {@code soulOnly} true (the new default): only a soul-tied skill of ours is refused (kept, then switched off); every other skill of ours is reset by the scroll like any Tensura skill.
     * {@code soulOnly} false: the 3.42.0 behaviour, every skill of ours is refused.
     */
    public static boolean refuses(boolean guardOn, boolean soulOnly, String skillNamespace, String skillPath, boolean fromScroll) {
        return refuses(guardOn, skillNamespace, fromScroll) && (!soulOnly || soulTied(skillPath));
    }
}
