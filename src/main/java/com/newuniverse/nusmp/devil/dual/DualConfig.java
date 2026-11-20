package com.newuniverse.nusmp.devil.dual;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the SERVER settings of Double Magic, One of One and the devil-magic immunities ({@code serverconfig/nusmp-devil-dual-server.toml}, docs/devil_double_magic.md). Every one of them changes gameplay, so
 * it is a server setting (rule 9), and every value is clamped again when read. Each new behaviour has a switch that restores the game as it was before: {@code ultimate.enabled = false} grants no skill (and takes
 * back none that was given), {@code oneOfOne.enabled = false} lets any number of players hold one devil, {@code immunity.*} = false lets Adaptability and law-domination act on devil magic again. The look of the
 * skill icon is a CLIENT setting ({@code client.devil.DevilIconClient}).
 */
public final class DualConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ULTIMATE, INTRINSIC, ONE, DUEL, ADAPT, LAW, COR_ON;
    private static final ModConfigSpec.DoubleValue COR_IDLE, COR_ASSIM, COR_UNION, COR_KILL, COR_DEATH, COR_CAP;
    private static final ModConfigSpec.IntValue COR_QUIET;
    private static final ModConfigSpec.IntValue GRACE_HOURS, ABSENT_DAYS, DUEL_SECONDS, DUEL_RANGE, RESERVE_HOURS;
    private static final ModConfigSpec.DoubleValue DAMAGE_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("devilDual");
        b.push("ultimate");
        ULTIMATE = b.comment("A devil contract gives the Ultimate skill Devil Magic (the devil's art, and the double art with the grimoire magic). false = no skill is granted and any that was granted is taken back at the next sync.")
                .define("enabled", true);
        DAMAGE_SCALE = b.comment("Multiplier on the raw damage of the arts (the balance law and its caps come after).").defineInRange("damageScale", 1.0, 0.0, 10.0);
        INTRINSIC = b.comment("A devil contract gives the intrinsic skill Devil Bond (assimilate / read the bond), shown with the other intrinsic skills. false = it is not granted and is taken back at the next sync.")
                .define("intrinsicEnabled", true);
        b.pop();
        b.push("corruption");
        COR_ON = b.comment("The price of a devil (a lost eye, half the health, ...) corrupts slowly: a creeping dark takes the screen the longer you go without killing and the more you overuse the devil. false = no corruption (the old small eye shade only).")
                .define("enabled", true);
        COR_IDLE = b.comment("Growth per minute (in thousandths of the screen) while you have not killed anything for quietSeconds.").defineInRange("idlePerMinute", 20.0, 0.0, 600.0);
        COR_ASSIM = b.comment("Extra growth per minute while the devil is let in (assimilating).").defineInRange("assimilatePerMinute", 40.0, 0.0, 600.0);
        COR_UNION = b.comment("Extra growth per minute inside the Devil Union.").defineInRange("unionPerMinute", 80.0, 0.0, 1200.0);
        COR_KILL = b.comment("Corruption (thousandths) taken away by each kill.").defineInRange("killRelief", 40.0, 0.0, 1000.0);
        COR_DEATH = b.comment("Share of the corruption kept at a death (0.5 = half of it is gone).").defineInRange("deathKeep", 0.5, 0.0, 1.0);
        COR_CAP = b.comment("The most the corruption can reach, in thousandths of the screen (850 = 85 percent). A clear window around the crosshair is always kept.").defineInRange("cap", 850.0, 0.0, 900.0);
        COR_QUIET = b.comment("Seconds after a kill during which the idle growth stops.").defineInRange("quietSeconds", 60, 0, 3600);
        b.pop();
        b.push("oneOfOne");
        ONE = b.comment("Each devil of the roster has only one contract holder in the world. A second player can not call a devil that is held, unless one of the conditions below frees it. false = the old behaviour (anybody may call any devil).")
                .define("enabled", true);
        GRACE_HOURS = b.comment("Death lapse: a holder who died and has not let the devil in again for this many in-game hours (1000 ticks each) loses the bond to the next caller. 0 = a death never lapses a bond.")
                .defineInRange("graceHours", 72, 0, 720);
        ABSENT_DAYS = b.comment("Absent lapse: a holder not seen on the server for this many real days loses the bond to the next caller. 0 = an absence never lapses a bond.")
                .defineInRange("absentDays", 30, 0, 365);
        DUEL = b.comment("The bound duel: /devil duel <holder> opens a duel; if the challenger kills the holder inside its window the holder loses the contract and the devil is reserved for the challenger. false = no duels.")
                .define("duelEnabled", true);
        DUEL_SECONDS = b.comment("How long a duel stays open, in seconds.").defineInRange("duelSeconds", 300, 30, 1800);
        DUEL_RANGE = b.comment("How close the two must stand to open a duel, in blocks.").defineInRange("duelRange", 32, 4, 128);
        RESERVE_HOURS = b.comment("After a won duel the devil is reserved for the victor this many in-game hours; nobody else may call it meanwhile.").defineInRange("reserveHours", 24, 1, 240);
        b.pop();
        b.push("immunity");
        ADAPT = b.comment("Adaptability never adapts to devil magic (the arts, and the blows of a host who has let the devil in). false = Adaptability treats devil blows like any other.").define("adaptability", true);
        LAW = b.comment("Law-domination style effects have no effect on devil magic (devil_magic damage tag, DevilMagic.lawDominationApplies). false = they act on it again.").define("lawDomination", true);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private DualConfig() {}

    private static boolean on(ModConfigSpec.BooleanValue v, boolean def) { try { return v.get(); } catch (RuntimeException notLoaded) { return def; } }

    private static int whole(ModConfigSpec.IntValue v, int def, int lo, int hi) {
        try { return Math.max(lo, Math.min(hi, v.get())); } catch (RuntimeException notLoaded) { return def; }
    }

    public static boolean ultimate() { return on(ULTIMATE, true); }
    public static double damageScale() {
        try { double d = DAMAGE_SCALE.get(); return Double.isNaN(d) ? 1.0 : Math.max(0.0, Math.min(10.0, d)); } catch (RuntimeException notLoaded) { return 1.0; }
    }
    public static boolean intrinsic() { return on(INTRINSIC, true); }

    private static float real(ModConfigSpec.DoubleValue v, float def) { try { return (float) v.get().doubleValue(); } catch (RuntimeException notLoaded) { return def; } }

    /** The corruption numbers (clamped again by {@link CorruptionRules}). */
    public static CorruptionRules.Params corruption() {
        return new CorruptionRules.Params(on(COR_ON, true), real(COR_IDLE, 20f), real(COR_ASSIM, 40f), real(COR_UNION, 80f), real(COR_KILL, 40f), real(COR_DEATH, 0.5f), real(COR_CAP, 850f),
                whole(COR_QUIET, 60, 0, 3600)).clamped();
    }

    public static boolean oneOfOne() { return on(ONE, true); }
    public static int graceHours() { return whole(GRACE_HOURS, 72, 0, 720); }
    public static int absentDays() { return whole(ABSENT_DAYS, 30, 0, 365); }
    public static boolean duelEnabled() { return on(DUEL, true); }
    public static int duelSeconds() { return whole(DUEL_SECONDS, 300, 30, 1800); }
    public static int duelRange() { return whole(DUEL_RANGE, 32, 4, 128); }
    public static int reserveHours() { return whole(RESERVE_HOURS, 24, 1, 240); }
    public static boolean immuneAdaptability() { return on(ADAPT, true); }
    public static boolean immuneLaw() { return on(LAW, true); }

    public static HolderRules.Params holderParams() {
        return new HolderRules.Params(oneOfOne(), HolderRules.graceTicks(graceHours()), HolderRules.absentMillis(absentDays()));
    }
}
