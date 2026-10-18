package com.newuniverse.nusmp.elite.items;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner and a server operator want to switch parts of the item subsystem without a rebuild, in ONE file, {@code nusmp-elite-items-server.toml}: the master switch, the crate-key loot
 * injection (the owner's explicit ask: a master switch for it, plus a scale so an operator can make keys rarer or commoner), the health potion cooldown, the Potential Catalyst's limits, the
 * Nightingale set bonus and the weapon ability scale. Every reader goes through {@link #get}, which answers the default while the spec is not loaded yet (a self-test or an early event must not
 * crash on a missing config), the way the hero weapons' config does. Plain {@code define} / {@code defineInRange} only (a list-validated value throws while the server is still loading).
 */
public final class EliteItemsConfig {
    private EliteItemsConfig() {}

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED, CRATE_KEY_INJECTION, CRATES_ENABLED, NIGHTINGALE_BONUS, PLUSH_COUNTER, ABILITIES_ENABLED, HEALTH_POTION_SHARED_COOLDOWN;
    public static final ModConfigSpec.IntValue HEALTH_POTION_COOLDOWN, CATALYST_MAX_USES, CATALYST_SP, CATALYST_HEALTH_HALF_HEARTS, CRATE_ANIMATION_TICKS;
    public static final ModConfigSpec.DoubleValue CRATE_KEY_SCALE, ABILITY_DAMAGE_SCALE, ABILITY_COOLDOWN_SCALE, SQUEAK_VOLUME;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("items");
        ENABLED = b.comment("Master switch of the whole elite item subsystem's behaviour (abilities, set bonus, crates, catalyst, potions, plush counter). Off: the items still exist and can be held, they just do nothing.")
                .define("enabled", true);
        b.push("crates");
        CRATES_ENABLED = b.comment("Crates open with their key. Off: a crate only tells the player it is sealed.")
                .define("enabled", true);
        CRATE_KEY_INJECTION = b.comment("MASTER SWITCH of the crate-key loot injection: the Global Loot Modifier that adds crate keys to dungeon chests (data/nusmp/loot_modifiers/elite_crate_keys.json). Off: no key is ever added to a chest.")
                .define("keyInjection", true);
        CRATE_KEY_SCALE = b.comment("Multiplier on every chance in the key loot modifier's data file (0 = none, 2 = twice as often).")
                .defineInRange("keyChanceScale", 1.0, 0.0, 100.0);
        CRATE_ANIMATION_TICKS = b.comment("How long the opening animation (lid up, light, rukh) lasts, in ticks. The rewards are given at once, the animation is only for show.")
                .defineInRange("animationTicks", 36, 4, 200);
        b.pop();
        b.push("healthPotions");
        HEALTH_POTION_COOLDOWN = b.comment("Ticks before the next Health Potion of any size can be drunk (20 = one second). The shared cooldown stops a stack of potions from being a free instant heal.")
                .defineInRange("cooldownTicks", 60, 0, 1200);
        HEALTH_POTION_SHARED_COOLDOWN = b.comment("true: the three sizes share one cooldown. false: each size has its own.")
                .define("sharedCooldown", true);
        b.pop();
        b.push("potentialCatalyst");
        CATALYST_MAX_USES = b.comment("How many Potential Catalysts one player can use in total (each is permanent; the limit keeps the maximum health bonus finite).")
                .defineInRange("maxUses", 10, 0, 1000);
        CATALYST_SP = b.comment("Skill Points one Potential Catalyst gives (through the skill tree's one door).")
                .defineInRange("skillPoints", 5, 0, 1000);
        CATALYST_HEALTH_HALF_HEARTS = b.comment("Permanent maximum health one Potential Catalyst gives, in half hearts (2 = one heart).")
                .defineInRange("healthHalfHearts", 2, 0, 20);
        b.pop();
        b.push("nightingale");
        NIGHTINGALE_BONUS = b.comment("The four-piece Nightingale set bonus (Nightsong): speed, night sight and a lullaby while sneaking. Off: the pieces are plain armour.")
                .define("setBonus", true);
        b.pop();
        b.push("weapons");
        ABILITIES_ENABLED = b.comment("The abilities of the six elite weapons (right-click technique, sneak-right-click second technique, full-swing strike, Runic rune slots). Off: they are plain swords.")
                .define("abilities", true);
        ABILITY_DAMAGE_SCALE = b.comment("Multiplier on every damage number in data/nusmp/elite/items/abilities/*.json.")
                .defineInRange("damageScale", 1.0, 0.0, 20.0);
        ABILITY_COOLDOWN_SCALE = b.comment("Multiplier on every cooldown in the ability files (0.5 = half as long).")
                .defineInRange("cooldownScale", 1.0, 0.0, 20.0);
        b.pop();
        b.push("plushies");
        PLUSH_COUNTER = b.comment("Count the distinct plushies each player has held (the collection counter) and fire PlushCollectedEvent for the records system.")
                .define("collectionCounter", true);
        SQUEAK_VOLUME = b.comment("Volume of the plushie squeak.")
                .defineInRange("squeakVolume", 0.8, 0.0, 4.0);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    /** A value, or its default while the spec is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try {
            return v.get();
        } catch (IllegalStateException | NullPointerException ex) {
            return v.getDefault();
        }
    }

    /** The master switch. */
    public static boolean enabled() { return get(ENABLED); }
}
