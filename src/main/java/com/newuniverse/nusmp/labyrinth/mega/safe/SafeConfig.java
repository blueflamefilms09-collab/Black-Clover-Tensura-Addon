package com.newuniverse.nusmp.labyrinth.mega.safe;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the Safe Zones of the Mega-Dungeon (rests, the end of easy regeneration, the mod-compat switch),
 * {@code world/serverconfig/nusmp-labyrinth-mega-safe-server.toml}, section {@code [safe]}. Its own file (the pattern of {@code DndConfig} and
 * {@code MegaConfig}) so no two builders edit one spec and a server owner who finds resting too generous, or the regeneration throttle too
 * harsh, changes one file. Only plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the
 * server is still loading), the defaults are the numbers of {@link RestRules}, and every reader goes through {@link #get}, which answers the
 * default while the spec is not loaded yet (the compat processor runs during world generation, before SERVER configs are read).
 */
public final class SafeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue REST_ENABLED, REST_AURA, PROTECT_BEDS, COMPAT_ENABLED;
    public static final ModConfigSpec.IntValue SHORT_REST_SECONDS, LONG_REST_SECONDS, SHORT_REST_USES, LONG_REST_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue SHORT_REST_HEALTH, SHORT_REST_MANA, PASSIVE_REGEN_SCALE, AMBUSH_CHANCE, AMBUSH_MAX_CHANCE, HOSTILE_RADIUS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("safe");
        REST_ENABLED = b.comment("Short and long rests at the marked seats, hearths and beds of the Landings and Safe Zones. false = nobody can rest (the beds still do not explode).")
                .define("restEnabled", true);
        SHORT_REST_SECONDS = b.comment("Seconds a short rest (a seat or a hearth) takes. The player must stay still, hurt by nothing, with no hostile mob close.")
                .defineInRange("shortRestSeconds", RestRules.DEFAULT_SHORT_SECONDS, 1, 120);
        LONG_REST_SECONDS = b.comment("Seconds a long rest (a bed) takes.")
                .defineInRange("longRestSeconds", RestRules.DEFAULT_LONG_SECONDS, 1, 600);
        SHORT_REST_USES = b.comment("Short rests a player gets between two long rests.")
                .defineInRange("shortRestUses", RestRules.DEFAULT_SHORT_USES, 1, 9);
        SHORT_REST_HEALTH = b.comment("Share of maximum health a short rest restores (a long rest restores everything).")
                .defineInRange("shortRestHealth", RestRules.DEFAULT_SHORT_HEALTH, 0.0, 1.0);
        SHORT_REST_MANA = b.comment("Share of maximum mana (magicule, and aura when restAura is on) a short rest restores (a long rest restores everything).")
                .defineInRange("shortRestMana", RestRules.DEFAULT_SHORT_MANA, 0.0, 1.0);
        REST_AURA = b.comment("A rest restores aura as well as magicule. false = magicule only.")
                .define("restAura", true);
        LONG_REST_COOLDOWN_TICKS = b.comment("Game ticks (20 = 1 second) that must pass between two long rests of one player.")
                .defineInRange("longRestCooldownTicks", RestRules.DEFAULT_LONG_COOLDOWN_TICKS, 0, 1_728_000);
        HOSTILE_RADIUS = b.comment("A hostile mob this many blocks away forbids a rest and breaks one in progress.")
                .defineInRange("hostileRadius", RestRules.DEFAULT_HOSTILE_RADIUS, 1.0, 64.0);
        AMBUSH_CHANCE = b.comment("Chance per level of sector threat that a long rest brings an ambush (announced to the Dungeon Master through RestAmbushListener; nothing spawns unless something listens).")
                .defineInRange("ambushChance", RestRules.DEFAULT_AMBUSH_PER_THREAT, 0.0, 1.0);
        AMBUSH_MAX_CHANCE = b.comment("The ceiling of that chance.")
                .defineInRange("ambushMaxChance", RestRules.DEFAULT_AMBUSH_CAP, 0.0, 1.0);
        PASSIVE_REGEN_SCALE = b.comment("Natural health regeneration and magicule / aura regeneration outside Safe Zones in the Mega-Dungeon are multiplied by this: 1 = vanilla, 0.25 = a quarter (default), 0 = none. Inside a Landing or Safe Zone regeneration is normal.")
                .defineInRange("passiveRegenScale", 0.25, 0.0, 1.0);
        PROTECT_BEDS = b.comment("Right-clicking a bed in the Mega-Dungeon never explodes it (the dimension has bed_works = false, where vanilla blows the bed up in the player's face). Beds that are rest anchors start a long rest, every other bed refuses politely.")
                .define("protectBeds", true);
        COMPAT_ENABLED = b.comment("The nusmp:mod_compat processor swaps marked vanilla furniture for Farmer's Delight and MrCrayfish's Furniture Mod: Refurbished blocks when those mods are loaded. false = always vanilla.")
                .define("compatEnabled", true);
        b.pop();
        SPEC = b.build();
    }

    private SafeConfig() {}

    /** The value, or its default while the server config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
