package com.newuniverse.nusmp.relics.djinn;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config {@code nusmp-belial-server.toml} (registered from the mod constructor): the settings of the canon Belial. SERVER settings, because they change
 * gameplay (the weapon, the acts, the Extreme Magic, the look the server tells the clients about).
 * <ul>
 *   <li>{@code belial.belialCanon} (default true): Belial is the white spirit-severing scythe Djinn of the owner's notes. false = the old fire greatsword
 *       Belial ("Lord of Hollow Thrones") exactly as before.</li>
 *   <li>{@code belial.exile} (default true): the second act banishes up to three targets for a few seconds. false = it only severs.</li>
 * </ul>
 */
public final class BelialConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CANON, EXILE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("belial");
        CANON = b.comment("Belial is the white scythe Djinn of the owner's notes: his scythe (Belior Goldorezza) severs the spirit and ignores armour and every nullification, and his Extreme Magic is Belior Zauto.",
                "false = the old Belial (Lord of Hollow Thrones, a fire greatsword, Belial Inanis) exactly as before. Takes effect for the next Equip and the next cast.").define("belialCanon", true);
        EXILE = b.comment("The scythe's second act, Exile, banishes up to three targets for three to six seconds (a short safe state in place, never a real teleport between dimensions; bosses are never exiled).",
                "false = Exile only severs.").define("exile", true);
        b.pop();
        SPEC = b.build();
    }

    private BelialConfig() {}

    /** The canon switch; false while the config is not loaded (so a plain unit test sees the old Belial). */
    public static boolean canon() {
        try { return CANON.get(); } catch (RuntimeException notLoaded) { return false; }
    }

    public static boolean exile() {
        try { return EXILE.get(); } catch (RuntimeException notLoaded) { return true; }
    }
}
