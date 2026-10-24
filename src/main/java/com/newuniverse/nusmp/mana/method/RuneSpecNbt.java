package com.newuniverse.nusmp.mana.method;

import net.minecraft.nbt.CompoundTag;

/**
 * The rune's modifiers as the spell's NBT data: the construct carries this tag from the cast to the launch to the impact, and the bolt reads the spec back from it. Reading never
 * throws: a missing or foreign tag gives a plain Flame rune.
 */
public final class RuneSpecNbt {
    private RuneSpecNbt() {}

    public static final String ELEMENT = "RuneElement", MODS = "RuneMods", TRUE = "RuneTrue", CHARGE = "RuneCharge";

    public static CompoundTag write(RuneSpec s) {
        CompoundTag t = new CompoundTag();
        t.putByte(ELEMENT, (byte) s.element().ordinal());
        t.putByte(MODS, (byte) s.mods());
        t.putBoolean(TRUE, s.trueMagic());
        t.putInt(CHARGE, RuneRules.chargeTicks(s));
        return t;
    }

    public static RuneSpec read(CompoundTag t) {
        if (t == null) return new RuneSpec(RuneElement.FIRE, RuneMods.NONE, false);
        return new RuneSpec(RuneElement.byOrdinal(t.getByte(ELEMENT)), t.getByte(MODS), t.getBoolean(TRUE));
    }
}
