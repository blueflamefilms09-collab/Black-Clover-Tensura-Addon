package com.newuniverse.nusmp.stones;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: whether the Infinity Stone slot exists, what EP it asks for, how long a holder may be away and what happens to a stone at death change gameplay, so they are SERVER settings
 * ({@code nusmp-stones-server.toml}, registered by {@link StoneBoot}). {@code enabled = false} restores the Wave 30 stones exactly as they were (carried, no gate, no slot acts, no passives, no
 * container rule, no away rule).
 */
public final class StoneConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, PASSIVES, CONTAINER_RULE, WHISPERS, EP_SCALE;
    public static final ModConfigSpec.DoubleValue EP_GATE;
    public static final ModConfigSpec.IntValue AWAY_DAYS;
    public static final ModConfigSpec.ConfigValue<String> ON_DEATH;
    // ---- Phase B (docs/infinity_stones_phase_b.md)
    public static final ModConfigSpec.BooleanValue CARRIERS, CARRIER_SURGE, SYNERGY, COMBOS, SNAP, SNAP_ERASE, SNAP_TOLL, SCEPTER, TELEKINESIS, MIND_LINK, MASTERY_BONUS, ALTAR, ALTAR_REQUIRED;
    public static final ModConfigSpec.IntValue SNAP_COOLDOWN_DAYS, MAX_LIFTS, MAX_LINKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("infinityStones");
        ENABLED = b.comment("The Infinity Stone slot: a stone worn in it (needs base max EP at the gate) gains two slot abilities and passives. false = the Wave 30 stones exactly as before.").define("enabled", true);
        EP_GATE = b.comment("Base maximum EP (not the Akashic grade's +20%) needed to wear a stone in the infinity_stone slot. Below it the stone's slot powers pause (the stone is never destroyed).").defineInRange("epGate", StoneRules.GATE_EP, 1.0e6, 1.0e18);
        PASSIVES = b.comment("The slot passives of the worn stone (speed, health, mental shield, ...). false = only the slot abilities.").define("passives", true);
        EP_SCALE = b.comment("Slot abilities and passives grow with 1 + log10(EP / gate), at most x2. false = always x1.").define("scaleWithEp", true);
        AWAY_DAYS = b.comment("A stone held by a player who has been offline this many real days is declared lost to the Order's ledger and returns to the world. 0 = never.").defineInRange("awayDays", 7, 0, 365);
        ON_DEATH = b.comment("What a stone worn in the slot does when its bearer dies: \"keep\" (it is given back at the respawn) or \"drop\" (it falls like the rest of the loot).").define("onDeath", "keep", o -> o instanceof String s && (s.equals("keep") || s.equals("drop")));
        CONTAINER_RULE = b.comment("Stones cannot be stored in chests, barrels or ender chests: they return to the player's pack when the container closes. false = the Wave 30 behaviour (they can).").define("noContainers", true);
        WHISPERS = b.comment("Rare whispers in the action bar from the worn stone.").define("whispers", true);
        b.pop();
        b.push("infinityStonesB");
        CARRIERS = b.comment("The Infinity and Nano Gauntlet carriers: stones are socketed into the gauntlet and cast through it. false = the Wave 30 gauntlet exactly as before (carried stones, old acts).").define("carriers", true);
        CARRIER_SURGE = b.comment("Socketing a stone sends a surge through the wearer (10% of max health on the Infinity Gauntlet, twice that on the Nano Gauntlet, less the stronger the wearer; it can never kill).").define("socketSurge", true);
        SYNERGY = b.comment("The comics feedback circuit Mind > Power > Space > Time > Reality > Soul > Mind: a socketed stone whose predecessor is also socketed gets +25% effect and -15% cooldown.").define("synergyCircuit", true);
        COMBOS = b.comment("Combo casts of two or three socketed stones (Soul+Time+Reality: Mend; Space+Power: Rift).").define("combos", true);
        SNAP = b.comment("The reworked Snap of a gauntlet with all six stones socketed (Restore, Purge and Grant modes). The Wave 30 Snap is not affected.").define("snap", true);
        SNAP_ERASE = b.comment("Allow the Snap's Erase mode (dusts weak hostile mobs near the wearer: up to 40 mobs, no player, no pet, no named mob, a boss only loses a tenth of its health). OFF by default: it removes mobs.").define("snapErase", false);
        SNAP_TOLL = b.comment("The Snap's toll on the wearer (Weakness and Mining Fatigue; on the Nano Gauntlet a Burned Arm of one hour). false = no toll.").define("snapToll", true);
        SNAP_COOLDOWN_DAYS = b.comment("In-game days before the same player can Snap again (one game day is 20 real minutes).").defineInRange("snapCooldownDays", 7, 1, 365);
        SCEPTER = b.comment("Loki's Scepter (a weapon that holds the Mind Stone): energy blast, barrier, and the Mind Stone's telekinesis and Mind Link. No mind control. false = the item still exists but its acts refuse.").define("scepter", true);
        TELEKINESIS = b.comment("Mind Stone telekinesis: lift one mob for three seconds and hurl it. Never a player or a boss.").define("telekinesis", true);
        MAX_LIFTS = b.comment("Most things lifted by telekinesis on the whole server at once.").defineInRange("maxLifts", 24, 1, 200);
        MIND_LINK = b.comment("Mind Link: for 30 seconds the bearer's good effects are shared with up to four allied players or own pets nearby.").define("mindLink", true);
        MAX_LINKS = b.comment("Most Mind Links open on the whole server at once.").defineInRange("maxLinks", 24, 1, 200);
        MASTERY_BONUS = b.comment("The Mind Stone's mastery bonus: one extra mastery point on one learned skill every 80 seconds while a Mind Stone works for the player (slot, gauntlet or scepter).").define("masteryBonus", true);
        ALTAR = b.comment("The Vormir altar block: right-click it to be offered the Soul Stone trial (the same server-checked offer as /multiverse stones offer).").define("altar", true);
        ALTAR_REQUIRED = b.comment("The Soul Stone offer works only within six blocks of a Vormir altar. false (default) = the command works anywhere, as in Phase A.").define("altarRequired", false);
        b.pop();
        SPEC = b.build();
    }

    private StoneConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static double epGate() { return get(EP_GATE); }

    public static boolean passives() { return get(PASSIVES); }

    public static boolean epScale() { return get(EP_SCALE); }

    public static int awayDays() { return get(AWAY_DAYS); }

    public static boolean keepOnDeath() { return !"drop".equals(get(ON_DEATH)); }

    public static boolean containerRule() { return get(CONTAINER_RULE); }

    public static boolean whispers() { return get(WHISPERS); }

    public static boolean carriers() { return enabled() && get(CARRIERS); }

    public static boolean carrierSurge() { return get(CARRIER_SURGE); }

    public static boolean synergy() { return get(SYNERGY); }

    public static boolean combos() { return get(COMBOS); }

    public static boolean snap() { return carriers() && get(SNAP); }

    public static boolean snapErase() { return get(SNAP_ERASE); }

    public static boolean snapToll() { return get(SNAP_TOLL); }

    public static int snapCooldownDays() { return get(SNAP_COOLDOWN_DAYS); }

    public static boolean scepter() { return enabled() && get(SCEPTER); }

    public static boolean telekinesis() { return get(TELEKINESIS); }

    public static int maxLifts() { return get(MAX_LIFTS); }

    public static boolean mindLink() { return get(MIND_LINK); }

    public static int maxLinks() { return get(MAX_LINKS); }

    public static boolean masteryBonus() { return get(MASTERY_BONUS); }

    public static boolean altar() { return enabled() && get(ALTAR); }

    public static boolean altarRequired() { return get(ALTAR_REQUIRED); }
}
