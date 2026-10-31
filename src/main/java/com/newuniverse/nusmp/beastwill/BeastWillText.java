package com.newuniverse.nusmp.beastwill;

import net.minecraft.network.chat.Component;

/** Why: the one place a will's name is turned into text (3.45.0): the lang key {@code nusmp.beastwill.<id>.name} when the pack has it, else the name written in the data file. */
public final class BeastWillText {
    private BeastWillText() {}

    public static Component name(BeastWillDef d) { return Component.translatableWithFallback(d.langKey("name"), d.name()); }

    public static Component name(String id) {
        BeastWillDef d = BeastWillCatalog.get(id);
        return d == null ? Component.translatableWithFallback("nusmp.beastwill." + id + ".name", id) : name(d);
    }
}
