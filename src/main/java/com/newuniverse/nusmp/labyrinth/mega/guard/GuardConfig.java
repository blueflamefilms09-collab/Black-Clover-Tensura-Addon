package com.newuniverse.nusmp.labyrinth.mega.guard;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Labyrinth Guardian, its altar and the Mana Core, {@code world/serverconfig/nusmp-labyrinth-mega-guard-server.toml},
 * section {@code [guard]}. Its own spec (the {@code DndConfig} pattern) so the guard builder never edits another builder's file and a
 * server owner who finds the sector bosses too hard or too soft retunes ONE small file. Only plain {@code define} / {@code defineInRange}
 * (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes through {@link #get},
 * which answers the default while the spec is not loaded yet (block and entity registration run before SERVER configs are read). The
 * defaults repeat the constants of {@link GuardianScaling}, which is what the tests pin.
 */
public final class GuardConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue GUARDIAN_BASE_HEALTH, THREAT_HEALTH_SCALE, THREAT_DAMAGE_SCALE, GUARDIAN_SCALE;
    public static final ModConfigSpec.IntValue APPROACH_RADIUS, REFORGE_MINUTES, ABANDON_SECONDS;
    public static final ModConfigSpec.BooleanValue TRAITS, LEASH, DND, EXECUTION;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("guard");
        GUARDIAN_BASE_HEALTH = b.comment("Max health of the Labyrinth Guardian of sector 0 (threat 1). Later sectors get more: health * (1 + threatHealthScale * (threat - 1)). The vanilla max-health attribute stops at 1024: what the formula asks for beyond that is paid as damage the Guardian divides away, so a late Guardian still takes longer to kill.")
                .defineInRange("guardianBaseHealth", GuardianScaling.BASE_HEALTH, 20.0, 5000.0);
        THREAT_HEALTH_SCALE = b.comment("Extra health per level of threat, as a share of the base (0.35 = +35 % per threat).")
                .defineInRange("threatHealthScale", GuardianScaling.HEALTH_PER_THREAT, 0.0, 5.0);
        THREAT_DAMAGE_SCALE = b.comment("Extra melee damage per level of threat, as a share of the base 14 (0.12 = +12 % per threat). Armour and size grow by fixed, gentler steps.")
                .defineInRange("threatDamageScale", GuardianScaling.DAMAGE_PER_THREAT, 0.0, 2.0);
        GUARDIAN_SCALE = b.comment("Body size of the Labyrinth Guardian: the SCALE attribute, so its hitbox and model grow together (1.0 is the size of a Floor Warden, 0.5 half of it, 4.0 the most the halls hold). The Guardian is the colossal boss that gates every tenth floor, so it defaults to 1.8; a later sector's Guardian grows up to a quarter more.")
                .defineInRange("guardianScale", GuardianScaling.BASE_SCALE, GuardianScaling.MIN_BASE_SCALE, GuardianScaling.MAX_SCALE);
        APPROACH_RADIUS = b.comment("A survival player this close (blocks) to the Guardian Altar wakes the Guardian. The fight counts as abandoned when the party stays more than this plus 16 blocks away.")
                .defineInRange("approachRadius", 16, 6, 48);
        REFORGE_MINUTES = b.comment("Minutes after a Mana Core was forged before a player can ask the spent altar for another one (only while the sector's door is still sealed and no core of that sector can be found). The anti soft-lock for a core lost to lava, the void or a log-out.")
                .defineInRange("reforgeMinutes", 5, 0, 120);
        TRAITS = b.comment("The theme trait of the Guardian (gravity wells, a blood shield, blinks, a fire trail, a frost aura, roots, chain lightning, darkness pulses, falling stars). false = a plain Floor Warden that only scales with threat.")
                .define("traits", true);
        LEASH = b.comment("The Guardian cannot be led out of its hall: a Guardian that strays more than a few blocks beyond the walls is carried back to its altar.")
                .define("leash", true);
        DND = b.comment("The D&D layer of the Decennial bosses (1.65.0): legendary resistances, legendary actions, lair actions, a mythic second life, the anti-magic aura, magic absorption and frightful presence, from the profile of the boss's floor (data/<ns>/mega/dnd_bosses). false = the Guardian is as it was in 1.64.")
                .define("dnd", true);
        EXECUTION = b.comment("The Word of Ending (Power Word Kill) of the deepest Decennial bosses: a player already under 35 % health is named, marked for three seconds and killed unless they heal above the threshold, break sight or the boss is hurt hard enough. false = the word never lands (the mark and the message still appear).")
                .define("execution", true);
        ABANDON_SECONDS = b.comment("Seconds without a survival player near the altar before an unfinished fight is abandoned (the Guardian is removed and the altar waits again).")
                .defineInRange("abandonSeconds", 120, 20, 1800);
        b.pop();
        SPEC = b.build();
    }

    private GuardConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
