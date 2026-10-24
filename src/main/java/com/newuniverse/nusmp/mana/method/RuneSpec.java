package com.newuniverse.nusmp.mana.method;

/**
 * What a rune array says (pure): the element, the modifiers and whether it is True Magic. It is "the spell's data": it is written into a tag at the cast ({@link RuneSpecNbt}),
 * travels to the clients as one small number, and the flying bolt reads it back. {@link #pack()} / {@link #unpack(int)} make it one int.
 */
public record RuneSpec(RuneElement element, int mods, boolean trueMagic) {
    public RuneSpec {
        element = element == null ? RuneElement.FIRE : element;
        mods = RuneMods.sanitize(mods);
    }

    public boolean homing() { return RuneMods.has(mods, RuneMods.HOMING); }

    public boolean growth() { return RuneMods.has(mods, RuneMods.GROWTH); }

    /** bits 0-1 mods, bit 2 true magic, bits 3-5 element. */
    public int pack() { return mods | (trueMagic ? 4 : 0) | (element.ordinal() << 3); }

    public static RuneSpec unpack(int v) {
        return new RuneSpec(RuneElement.byOrdinal((v >> 3) & 7), v & 3, (v & 4) != 0);
    }
}
