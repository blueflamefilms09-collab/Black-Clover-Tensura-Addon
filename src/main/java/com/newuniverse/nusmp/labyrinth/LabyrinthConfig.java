package com.newuniverse.nusmp.labyrinth;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Labyrinth, {@code world/serverconfig/nusmp-labyrinth-server.toml}, section {@code [labyrinth]}. Only plain
 * {@code define} / {@code defineInRange} are used, never {@code defineInList}, because a list-validated value throws on an unknown entry while the
 * server is still loading. Every reader goes through {@link #get}, which answers the default while the spec is not loaded yet (world-gen and block
 * registration run before SERVER configs are read). Until 1.58.0 the file also held {@code engine}, the switch that sent Maws and Gates to the
 * Spire or to the Living Dungeon's caves; both were removed and the key with them (an old file that still has it is simply not read).
 */
public final class LabyrinthConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue UNBREAKABLE, RELICS_EXCLUSIVE, VOID_RESCUE, BOSS_ROSTER_ENABLED, GATEWAY_UNBREAKABLE;
    public static final ModConfigSpec.IntValue TRIAL_RESET_SECONDS;
    public static final ModConfigSpec.DoubleValue WARDEN_HEALTH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("labyrinth");
        UNBREAKABLE = b.comment("Every block of the Labyrinth dimension is unbreakable by any means (mining, creative too, explosions, pistons, mob griefing): the Dungeon Guard holds the whole dimension so nobody digs round a sealed door. false = the dungeon can be mined.")
                .define("unbreakable", true);
        RELICS_EXCLUSIVE = b.comment("Relics of Many Worlds are found ONLY in the Labyrinth (vault chests, Relic Pedestals, boss drops). false = the 1.24 behaviour: relics may also roll in ordinary Overworld chests.")
                .define("relicsExclusive", true);
        VOID_RESCUE = b.comment("A player who falls into the void of the dungeon is caught and put back on the arrival pad with 4 damage instead of dying. false = the void kills as usual.")
                .define("voidRescue", true);
        TRIAL_RESET_SECONDS = b.comment("Seconds after a Sealed Archway's trial is abandoned (every player left its box) before its barriers fall and it can be fought again.")
                .defineInRange("trialResetSeconds", 60, 10, 600);
        WARDEN_HEALTH = b.comment("Max health of the Floor Warden mini-boss that drops the Shadow Key.")
                .defineInRange("wardenHealth", 200.0, 20.0, 2000.0);
        BOSS_ROSTER_ENABLED = b.comment("The Peak Altar rolls its floor boss from the roster written into the structure (the Zogratis siblings, a Floor Warden). false = the altar always calls a Floor Warden.")
                .define("bossRosterEnabled", true);
        GATEWAY_UNBREAKABLE = b.comment("1.55.0: the Mega-Dungeon's Gateway ruin in the Overworld cannot be mined (survival or adventure) or blown up; a creative-mode operator can still edit it. false = the ruin is an ordinary structure.")
                .define("gatewayUnbreakable", true);
        b.pop();
        SPEC = b.build();
    }

    private LabyrinthConfig() {}

    /** The value, or its default while the server config is not loaded yet (registration, world-gen threads, the client's own guesses). */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
