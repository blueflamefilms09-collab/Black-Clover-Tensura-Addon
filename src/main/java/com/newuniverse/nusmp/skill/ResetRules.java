package com.newuniverse.nusmp.skill;

/**
 * Why (3.42.0): the pure rules of {@link ResetGuard}, with no game classes, so they are unit-tested: which skills are ours, which call stacks are "a Tensura Reset Scroll", and when a removal is refused.
 */
public final class ResetRules {
    /** Tensura's scroll class: the frames that make a skill removal "a reset". */
    public static final String SCROLL_CLASS = "io.github.manasmods.tensura.item.misc.ResetScrollItem";
    /** Every skill this mod registers lives in this namespace. */
    public static final String NAMESPACE = "nusmp";

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
}
