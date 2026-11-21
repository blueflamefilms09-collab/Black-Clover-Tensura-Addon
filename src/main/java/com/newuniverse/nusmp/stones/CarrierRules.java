package com.newuniverse.nusmp.stones;

/**
 * Why: every number of Phase B of the Infinity Stones that can be decided without a world lives here so it can be tested without Minecraft: the socket mask of a gauntlet or scepter, the comics feedback
 * circuit (Mind > Power > Space > Time > Reality > Soul > Mind: a stone whose predecessor is also socketed gets +25% effect and -15% cooldown), the surge of socketing (inverse to the wearer's strength, never
 * lethal), the combos, the Snap (modes, who may be dusted, boss and target caps, the cooldown, the toll), the telekinesis and Mind Link caps, the mastery bonus period and the Scepter's numbers. Pure; no
 * Minecraft classes. The numbers are design values (docs/infinity_stones_phase_b.md); none of them can kill a player or take a boss below 90% of its health.
 */
public final class CarrierRules {
    private CarrierRules() {}

    /** The comics feedback circuit in order; the stone before a stone in this list is its predecessor (Mind follows Soul). */
    public static final InfinityStone[] CIRCUIT = {InfinityStone.MIND, InfinityStone.POWER, InfinityStone.SPACE, InfinityStone.TIME, InfinityStone.REALITY, InfinityStone.SOUL};
    public static final int ALL = 63;
    public static final double EFFECT_BOOST = 1.25, COOLDOWN_BOOST = 0.85;

    // ---------------------------------------------------------------- the socket mask (bit i = InfinityStone ordinal i)
    public static int bit(InfinityStone s) { return s == null ? 0 : 1 << s.ordinal(); }

    public static boolean has(int mask, InfinityStone s) { return s != null && (mask & bit(s)) != 0; }

    public static int with(int mask, InfinityStone s) { return (mask | bit(s)) & ALL; }

    public static int without(int mask, InfinityStone s) { return mask & ~bit(s) & ALL; }

    public static int count(int mask) { return Integer.bitCount(mask & ALL); }

    public static boolean full(int mask) { return (mask & ALL) == ALL; }

    // ---------------------------------------------------------------- the circuit
    public static InfinityStone predecessor(InfinityStone s) {
        for (int i = 0; i < CIRCUIT.length; i++) if (CIRCUIT[i] == s) return CIRCUIT[(i + CIRCUIT.length - 1) % CIRCUIT.length];
        return null;
    }

    /** Does this stone work better because the stone before it in the circuit is socketed beside it? */
    public static boolean boosted(int mask, InfinityStone s) {
        return has(mask, s) && has(mask, predecessor(s));
    }

    /** +25% effect for a boosted stone (1.0 otherwise or when the circuit is off). */
    public static double effectFactor(int mask, InfinityStone s, boolean circuitOn) {
        return circuitOn && boosted(mask, s) ? EFFECT_BOOST : 1.0;
    }

    /** -15% cooldown for a boosted stone (1.0 otherwise or when the circuit is off). */
    public static double cooldownFactor(int mask, InfinityStone s, boolean circuitOn) {
        return circuitOn && boosted(mask, s) ? COOLDOWN_BOOST : 1.0;
    }

    /** How many stones of a mask are boosted (the status line shows it). */
    public static int boostedCount(int mask) {
        int n = 0;
        for (InfinityStone s : InfinityStone.values()) if (boosted(mask, s)) n++;
        return n;
    }

    // ---------------------------------------------------------------- socketing
    /**
     * The surge that socketing a stone sends through the wearer, as a share of maximum health: 10% on the Infinity Gauntlet and twice that on the Nano Gauntlet (it has less shielding), less the stronger the
     * wearer is (canon: the surge is inverse to the wielder's strength; at ten times the gate it is a fifth of that), and never more than 30%.
     */
    public static double surgeFraction(double baseEp, double gate, boolean nano) {
        double ratio = baseEp > 0 && gate > 0 ? Math.max(0.2, Math.min(1.0, gate / baseEp)) : 1.0;
        return Math.min(0.30, 0.10 * ratio * (nano ? 2.0 : 1.0));
    }

    // ---------------------------------------------------------------- combos
    /** Soul + Time + Reality: a full heal in canon; here a bounded mend of allies (never more than 40% of maximum health, under the half-a-bar law). */
    public static boolean mendReady(int mask) { return has(mask, InfinityStone.SOUL) && has(mask, InfinityStone.TIME) && has(mask, InfinityStone.REALITY); }

    /** Space + Power: a rift of power along the look. */
    public static boolean riftReady(int mask) { return has(mask, InfinityStone.SPACE) && has(mask, InfinityStone.POWER); }

    public static final double MEND_FRACTION = 0.40;
    public static final int MEND_TARGETS = 12, RIFT_TARGETS = 8;
    public static final double RIFT_RANGE = 24.0, MEND_RADIUS = 12.0;

    // ---------------------------------------------------------------- the Snap
    /** The Snap's modes. Append only. Erase is off unless the server allows it. */
    public enum SnapMode {
        RESTORE("Restore", "Hulk's snap: allies near you are healed (at most half a bar) and cleansed."),
        ERASE("Erase", "Tony's snap: weak hostile mobs near you turn to dust; a boss loses a tenth of its health."),
        PURGE("Purge", "Every curse and harmful effect leaves you and your allies."),
        GRANT("Grant", "Your allies are empowered for five minutes (Resistance II, Strength I).");

        public final String title, text;

        SnapMode(String title, String text) {
            this.title = title;
            this.text = text;
        }
    }

    public static SnapMode snapMode(int ordinal) {
        SnapMode[] all = SnapMode.values();
        return all[Math.floorMod(ordinal, all.length)];
    }

    /** The mode after this one that is allowed (Erase is skipped when the server forbids it). */
    public static SnapMode nextMode(SnapMode now, boolean eraseAllowed) {
        SnapMode[] all = SnapMode.values();
        int i = now == null ? -1 : now.ordinal();
        for (int k = 1; k <= all.length; k++) {
            SnapMode m = all[(i + k) % all.length];
            if (m != SnapMode.ERASE || eraseAllowed) return m;
        }
        return SnapMode.RESTORE;
    }

    public static final int SNAP_TARGET_CAP = 40;
    public static final double SNAP_RADIUS = 48.0;
    public static final float DUST_MAX_HEALTH = 120f, HEAL_FRACTION = 0.5f, BOSS_FRACTION = 0.10f;
    public static final int GRANT_TICKS = 6000, TOLL_TICKS = 1200, BURNED_ARM_TICKS = 72000;
    /** Game ticks in one in-game day. */
    public static final long DAY = 24000L;

    /** What the Erase mode does to one kind of mob. */
    public enum Fate { NONE, DUST, HALF, TENTH }

    /**
     * The fate of a mob under Erase. Never a player, never a pet or named or relic-owned mob ({@code spared}). A boss loses a tenth of its maximum health (the mod's boss convention). A weak mob (at most
     * {@link #DUST_MAX_HEALTH} maximum health) is dusted; a stronger one loses half of its current health.
     */
    public static Fate fate(boolean boss, boolean player, boolean spared, float maxHealth) {
        if (player || spared) return Fate.NONE;
        if (boss) return Fate.TENTH;
        return maxHealth <= DUST_MAX_HEALTH ? Fate.DUST : Fate.HALF;
    }

    /** The damage of a fate (a dusted mob loses all it has). */
    public static float eraseDamage(Fate f, float maxHealth, float health) {
        return switch (f) {
            case DUST -> Math.max(0f, health);
            case HALF -> Math.max(0f, health * 0.5f);
            case TENTH -> Math.max(0f, maxHealth * BOSS_FRACTION);
            case NONE -> 0f;
        };
    }

    /** The reason a Snap may not start, or null: it needs all six stones socketed, the setting, the gate and a cooldown that has passed. */
    public static String snapRefusal(int mask, boolean settingOn, boolean gateOk, boolean restrained, long now, long readyAt) {
        if (!settingOn) return "The Snap is switched off on this server.";
        if (!full(mask)) return "The Snap needs all six stones socketed (" + count(mask) + " of 6).";
        if (!gateOk) return "The gauntlet burns: you are not strong enough to bear all six stones.";
        if (restrained) return "Your hand is held: you cannot snap.";
        if (now < readyAt) return "The universe has been touched enough: the Snap is ready in " + ((readyAt - now) / DAY + 1) + " in-game day(s).";
        return null;
    }

    public static long snapReadyAt(long now, int days) { return now + Math.max(1, days) * DAY; }

    /** The toll of a Snap in ticks of Weakness and Mining Fatigue on the wearer; the Nano Gauntlet adds a Burned Arm that lasts an hour. */
    public static int tollTicks() { return TOLL_TICKS; }

    // ---------------------------------------------------------------- telekinesis
    public static final int LIFT_TICKS = 60;
    public static final double LIFT_REACH = 24.0, LIFT_HOLD_DISTANCE = 3.5, THROW_SPEED = 1.7;
    public static final float THROW_DAMAGE = 10f;

    /** A thing may be lifted: a living mob that is not a boss and not a player (a player is never moved by force, canon aside). */
    public static boolean liftable(boolean player, boolean boss, boolean alive) { return alive && !player && !boss; }

    /** The most lifts the server holds at once (the table of the world); a new one is refused at the cap. */
    public static boolean liftRoom(int open, int cap) { return open < cap; }

    // ---------------------------------------------------------------- Mind Link
    public static final int LINK_ALLIES = 4, LINK_TICKS = 600, LINK_PULSE = 40, LINK_EFFECT_TICKS = 100, LINK_MAX_AMP = 2, LINK_EFFECTS_PER_PULSE = 6;
    public static final double LINK_RADIUS = 24.0, LINK_SHARE_RADIUS = 48.0;

    /** A good effect may be shared when its amplifier is small; its length on the ally is short (renewed every pulse) and never longer than the source. */
    public static int linkAmplifier(int amp) { return Math.max(0, Math.min(LINK_MAX_AMP, amp)); }

    public static int linkTicks(int sourceTicksLeft) { return Math.max(1, Math.min(LINK_EFFECT_TICKS, sourceTicksLeft)); }

    // ---------------------------------------------------------------- mastery bonus
    public static final long MASTERY_PERIOD = 1600L;

    /** One extra mastery point every 80 seconds (the stand-in for +25% mastery gain; Tensura's gain events are not hooked). */
    public static boolean masteryDue(long now, long lastAward) { return now - lastAward >= MASTERY_PERIOD; }

    // ---------------------------------------------------------------- Loki's Scepter (no mind control anywhere)
    public static final float BLAST_DAMAGE = 16f;
    public static final double BLAST_RANGE = 24.0;
    public static final float BARRIER_ABSORB = 8f;
    public static final int BARRIER_TICKS = 200;
    /** The anger aura of the scepter (canon: the stone stirs anger): the bearer's own friendly fire is not raised; the aura is only a flavour line. Documented as not built. */
    public static final String SCEPTER_ID = "lokis_scepter";
    public static final String NANO_ID = "the_nano_gauntlet";
    public static final String INFINITY_ID = "the_infinity_gauntlet";

    public static boolean isGauntlet(String relicId) { return INFINITY_ID.equals(relicId) || NANO_ID.equals(relicId); }

    public static boolean isNano(String relicId) { return NANO_ID.equals(relicId); }

    /** Which stones a carrier may hold: a gauntlet holds all six, the scepter only the Mind Stone. */
    public static int allowedMask(String relicId) {
        if (isGauntlet(relicId)) return ALL;
        if (SCEPTER_ID.equals(relicId)) return bit(InfinityStone.MIND);
        return 0;
    }

    // ---------------------------------------------------------------- carrier acts (keys)
    /** The key of the stone's n-th act cast through a carrier: cs_&lt;stone&gt;_&lt;n&gt;. */
    public static String carrierKey(InfinityStone s, int n) { return "cs_" + s.id() + "_" + n; }

    public static boolean isCarrierAct(String key) { return key != null && key.startsWith("cs_"); }

    public static InfinityStone carrierStone(String key) {
        if (!isCarrierAct(key)) return null;
        for (InfinityStone s : InfinityStone.values()) if (key.startsWith("cs_" + s.id() + "_")) return s;
        return null;
    }

    /** How many slot acts a stone has (Mind has four since Phase B: Telekinesis and Mind Link were added). */
    public static int slotActs(InfinityStone s) { return s == InfinityStone.MIND ? 4 : 2; }

    // ---------------------------------------------------------------- the Vormir altar
    /** What the altar may be asked for (the same list as the command: a skill or a named pet; never a player, never EP). */
    public static boolean altarOffer(String kind) { return StoneRules.offeringAllowed(kind); }
}
