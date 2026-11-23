package com.newuniverse.nusmp.franchise;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 3.88.0: {@code visual.franchiseTabs} in {@code config/nusmp-franchise-tabs-client.toml} (a CLIENT config: it only changes how the creative menu is laid out). On (the default): the creative menu has one extra tab per
 * franchise next to the older tabs. Off: the franchise tabs stay empty (and so are hidden) and only the older layout is shown. The dedicated server never registers this file; reading it there gives the default.
 */
public final class FranchiseTabsConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue TABS, VERSES, COMPACT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("visual");
        TABS = b.comment("Show one extra creative tab per anime franchise (Black Clover, Tensura, Berserk, ...). Off = only the older creative tabs.").define("franchiseTabs", true);
        VERSES = b.comment("4.96.0: also show one creative tab per verse (Dragon Ball, Shield Hero, DanMachi, ...), with a note item in a verse that has nothing yet. Off = only the seven franchise tabs of 3.88.0. Needs franchiseTabs.").define("verseTabs", true);
        COMPACT = b.comment("4.108.0: the compact creative menu (the default): only the seven franchise tabs (the verse items sit in Anime Worlds) plus Misc for anything no franchise owns. false = every tab of the older layouts again (the 7 type tabs, the 7 franchise tabs and the 28 verse tabs). Needs franchiseTabs on.").define("compactTabs", true);
        b.pop();
        SPEC = b.build();
    }

    private FranchiseTabsConfig() {}

    public static boolean verses() {
        try { return TABS.get() && VERSES.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    public static boolean enabled() {
        try { return TABS.get(); } catch (IllegalStateException notLoaded) { return true; }
    }

    /** 4.108.0: true = the compact menu (franchise tabs + Misc); false = the older, longer layouts. Always false while the franchise tabs are off, so the items never lose their only tabs. */
    public static boolean compact() {
        try { return TABS.get() && COMPACT.get(); } catch (IllegalStateException notLoaded) { return true; }
    }
}
