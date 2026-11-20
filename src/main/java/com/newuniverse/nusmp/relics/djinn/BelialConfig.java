package com.newuniverse.nusmp.relics.djinn;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server config {@code nusmp-belial-server.toml} (registered from the mod constructor): the settings of the canon Belial. SERVER settings, because they change
 * gameplay (the weapon, the acts, the Extreme Magic, the look the server tells the clients about).
 * <ul>
 *   <li>{@code belial.belialCanon} (default true): Belial is the white spirit-severing scythe Djinn of the owner's notes. false = the old fire greatsword
 *       Belial ("Lord of Hollow Thrones") exactly as before.</li>
 *   <li>{@code belial.exile} (default true): the second act banishes up to three targets for a few seconds. false = it only severs.</li>
 *   <li>{@code belial.zakera} (default true): the third act, Belior Zakera (memory and sense illusion). false = the act does nothing.</li>
 *   <li>{@code belial.senses} (default true): Belior Zauto also devours the five senses of its victims for a few seconds. false = it only severs.</li>
 *   <li>{@code belial.dualDjinn} (default true): a canon-Belial owner who is also in the household of Zagan may cast Zagan's first act from Belial's weapon.</li>
 *   <li>{@code belial.deadWeight} (default true): Belior Goldoreza leaves the struck foe numb for a while (Slowness I, Weakness I and no normal healing; never a boss).</li>
 *   <li>{@code belial.boneGrip} (default true): in a Full Equip the skeletal dragon grips the foe the bearer strikes (once every 12 s, for at most 2 s).</li>
 * </ul>
 */
public final class BelialConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue CANON, EXILE, ZAKERA, SENSES, DUAL, GRIP, DEAD_WEIGHT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("belial");
        CANON = b.comment("Belial is the white scythe Djinn of the owner's notes: his scythe (Belior Goldorezza) severs the spirit and ignores armour and every nullification, and his Extreme Magic is Belior Zauto.",
                "false = the old Belial (Lord of Hollow Thrones, a fire greatsword, Belial Inanis) exactly as before. Takes effect for the next Equip and the next cast.").define("belialCanon", true);
        EXILE = b.comment("The scythe's second act, Exile, banishes up to three targets for three to six seconds (a short safe state in place, never a real teleport between dimensions; bosses are never exiled).",
                "false = Exile only severs.").define("exile", true);
        ZAKERA = b.comment("The scythe's third act, Belior Zakera (sprint + right-click): foes in a short cone lose their memory of the fight for a few seconds (mobs forget their target, players get distorted senses).",
                "false = the act does nothing.").define("zakera", true);
        SENSES = b.comment("Belior Zauto (the Extreme Magic) also devours the five senses of everything it severs, for a few seconds only (the permanent loss of the story is a design cap here).",
                "false = it only severs.").define("senses", true);
        DUAL = b.comment("Dual Djinn: a canon-Belial owner who is in the household of Zagan can cast Zagan's first act from Belial's weapon (look straight down + right-click) without changing Equips.",
                "false = Zagan cannot be shared with a Djinn owner, as before.").define("dualDjinn", true);
        GRIP = b.comment("In a Full Equip, the skeletal dragon grips the foe the bearer just struck and holds it for up to 2 seconds (one victim, every 12 seconds, never a boss).",
                "false = no grip.").define("boneGrip", true);
        DEAD_WEIGHT = b.comment("The scythe's first act, Belior Goldoreza, leaves no wound: the struck foe goes numb and limp for 8 to 20 seconds (Slowness I, Weakness I, and normal healing is blocked). Never a boss.",
                "false = it only severs.").define("deadWeight", true);
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

    public static boolean zakera() {
        try { return ZAKERA.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static boolean senses() {
        try { return SENSES.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static boolean dual() {
        try { return DUAL.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static boolean grip() {
        try { return GRIP.get(); } catch (RuntimeException notLoaded) { return true; }
    }

    public static boolean deadWeight() {
        try { return DEAD_WEIGHT.get(); } catch (RuntimeException notLoaded) { return true; }
    }
}
