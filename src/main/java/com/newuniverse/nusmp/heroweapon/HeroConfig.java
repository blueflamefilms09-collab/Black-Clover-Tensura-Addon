package com.newuniverse.nusmp.heroweapon;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the core of the hero weapons has switches and numbers the owner and a server operator will want to change without a rebuild (turn the whole system off,
 * make the Holy Weapons shareable, make the experience rule stricter or off, soften the shock), and they must be one file, {@code nusmp-heroweapon-server.toml},
 * section {@code [heroweapon]}, owned by the core. The forms, curse and gem systems have their own files, so four builders never edit one spec. Every value is
 * a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every
 * reader goes through {@link #get}, which answers the default while the spec is not loaded yet (a self-test or an early event must not crash on a missing
 * config), the way {@code DndConfig} does.
 */
public final class HeroConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED, HOLY_UNIQUE, VASSAL_UNIQUE, HOLY_REJECTS_OTHERS, REPEL_XP, ADOPT_STACKS, SHOW_MESSAGES;
    public static final ModConfigSpec.BooleanValue CASTING_RELICS_ALLOWED, CONSUMABLES_FIRST;
    public static final ModConfigSpec.IntValue MAX_BONDS, REPEL_RADIUS, MAX_LEVEL, SHOCK_SLOW_TICKS, SKILL_GLOBAL_LOCK_TICKS, MIN_COOLDOWN_TICKS, KEY_SPACING_TICKS,
            MAX_KILL_EXP, REPEL_NOTICE_SECONDS;
    public static final ModConfigSpec.BooleanValue BUFF_ENABLED;
    public static final ModConfigSpec.DoubleValue BUFF_DAMAGE, BUFF_HOLY_EXTRA, BUFF_COOLDOWN, BUFF_COST, BUFF_TAKEN, BUFF_SHIELD_TAKEN, BUFF_REFLECT, BUFF_REFLECT_CAP;
    public static final ModConfigSpec.DoubleValue WEAPON_EXP_MULTIPLIER, SHOCK_DAMAGE, KILL_EXP_SHARE, BOSS_KILL_EXP_FACTOR;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("heroweapon");
        ENABLED = b.comment("Master switch. Off: nobody can be bound to a hero weapon, summoned weapons are put away, no hero technique, restriction or experience rule runs.")
                .define("enabled", true);
        MAX_BONDS = b.comment("How many hero weapons one player may be bound to. A bond holds ONE weapon, so 1 is the only useful value; 0 closes the doors (nobody new can be bound, existing bonds stay).")
                .defineInRange("maxBonds", 1, 0, 1);
        HOLY_UNIQUE = b.comment("One holder per Holy Weapon (Shield, Sword, Spear, Bow): while a player holds one, nobody else can be bound to it. Kept in the world file nusmp_hero_registry.dat.")
                .define("holyUnique", true);
        VASSAL_UNIQUE = b.comment("The same for the Seven Star and Vassal Weapons (off by default: several people may hold a Hammer or a Fan).")
                .define("vassalUnique", false);
        HOLY_REJECTS_OTHERS = b.comment("Restriction one: a Holy Weapon's wielder who fights with (or fires) a weapon of another class is shocked instead.")
                .define("holyRejectsOthers", true);
        CASTING_RELICS_ALLOWED = b.comment("3.48.1: relics that are not fought with (tomes, wands, staffs, orbs) are never rejected by restriction one, so a Holy Weapon's wielder can still use and learn from them. Off: they count as weapons again (the old behaviour).")
                .define("castingRelicsAllowed", true);
        CONSUMABLES_FIRST = b.comment("3.63.0: a hero weapon in the main hand lets the use key go to a food (when hungry) or a potion in the OFF hand, so consumables can be used while holding the Shield, Bow, Sword or Spear. Sneak + use is still the weapon's own gesture. Off: the weapon takes every click (the old behaviour).")
                .define("consumablesFirst", true);
        REPEL_XP = b.comment("Restriction three: two Holy wielders within repelRadius of each other gain no experience (orbs, kill experience, the weapon's own experience).")
                .define("repelXp", true);
        REPEL_RADIUS = b.comment("Blocks within which two Holy wielders of the same dimension repel each other's experience.")
                .defineInRange("repelRadius", 48, 1, 512);
        REPEL_NOTICE_SECONDS = b.comment("Seconds between two 'The Legendary Weapons repel each other' lines for one player.")
                .defineInRange("repelNoticeSeconds", 60, 1, 3600);
        ADOPT_STACKS = b.comment("A hero weapon taken from the creative tab or given with /give is adopted by the first survival player who holds it (bound to them if they are free, else it dissolves). Off: such stacks always dissolve in survival.")
                .define("adoptStacks", true);
        WEAPON_EXP_MULTIPLIER = b.comment("Multiplier on every point of weapon experience (blows, kills, techniques, mastery).")
                .defineInRange("weaponExpMultiplier", 1.0, 0.0, 100.0);
        MAX_LEVEL = b.comment("The highest level a weapon can GAIN by experience (the curve itself ends at 100; /multiverse hero level can still set any level).")
                .defineInRange("maxLevel", 100, 1, 100);
        KILL_EXP_SHARE = b.comment("Weapon experience for a kill, as a share of the victim's maximum health.")
                .defineInRange("killExpShare", 0.25, 0.0, 10.0);
        BOSS_KILL_EXP_FACTOR = b.comment("Factor on the kill experience of a boss.")
                .defineInRange("bossKillExpFactor", 5.0, 1.0, 100.0);
        MAX_KILL_EXP = b.comment("The most weapon experience one kill can give (so a boss with a huge health pool does not take a weapon up in one blow).")
                .defineInRange("maxKillExp", 500, 1, 1000000);
        SHOCK_DAMAGE = b.comment("Magic damage of the shock a Holy wielder takes for fighting with another class of weapon (4 = two hearts). Never lethal: it leaves at least half a heart.")
                .defineInRange("shockDamage", 4.0, 0.0, 20.0);
        SHOCK_SLOW_TICKS = b.comment("Ticks of Slowness after the shock (20 = one second).")
                .defineInRange("shockSlowTicks", 20, 0, 600);
        SKILL_GLOBAL_LOCK_TICKS = b.comment("Ticks between two techniques of the same wielder, whichever they are (40 = two seconds).")
                .defineInRange("skillGlobalLockTicks", 40, 0, 600);
        MIN_COOLDOWN_TICKS = b.comment("The shortest cooldown any technique can have after the level's shortening (the owner's balance floor).")
                .defineInRange("minCooldownTicks", 20, 1, 600);
        KEY_SPACING_TICKS = b.comment("The server answers a press of the same hero key at most this often (a held-down key or a modified client cannot flood it).")
                .defineInRange("keySpacingTicks", 4, 0, 40);
        SHOW_MESSAGES = b.comment("Action-bar lines for the weapon (summoned, put away, refused, level up). Off keeps the screen clear for streamers.")
                .define("showMessages", true);
        b.push("buff");
        BUFF_ENABLED = b.comment("3.55.0 master switch of the hero weapon buffs below. Off: every weapon behaves exactly as before 3.55.0.").define("enabled", true);
        BUFF_DAMAGE = b.comment("Factor on EVERY point of damage a bound wielder deals while their hero weapon is out (blows, techniques, arrows). Bosses still obey the boss damage cap.")
                .defineInRange("damageFactor", 4.0, 1.0, 100.0);
        BUFF_HOLY_EXTRA = b.comment("Extra factor on top of damageFactor for the four Holy Weapons (Shield, Sword, Spear, Bow).").defineInRange("holyExtraFactor", 1.5, 1.0, 100.0);
        BUFF_COOLDOWN = b.comment("Factor on every technique cooldown (0.5 = half as long); the minCooldownTicks floor still holds.").defineInRange("cooldownFactor", 0.5, 0.05, 1.0);
        BUFF_COST = b.comment("Factor on every technique's magicule cost (0.5 = half).").defineInRange("costFactor", 0.5, 0.0, 1.0);
        BUFF_TAKEN = b.comment("Factor on all damage a bound wielder takes (0.6 = 40 percent less).").defineInRange("damageTakenFactor", 0.6, 0.0, 1.0);
        BUFF_SHIELD_TAKEN = b.comment("The same for the wielder of the Legendary Shield (Naofumi): the supreme defence.").defineInRange("shieldDamageTakenFactor", 0.2, 0.0, 1.0);
        BUFF_REFLECT = b.comment("Share of the damage the Legendary Shield's wielder takes that is thrown back at the attacker (the Counter). 0 = off.").defineInRange("shieldReflectShare", 0.75, 0.0, 10.0);
        BUFF_REFLECT_CAP = b.comment("The most one reflected blow can deal.").defineInRange("shieldReflectCap", 60.0, 0.0, 10000.0);
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private HeroConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try {
            return v.get();
        } catch (IllegalStateException notLoaded) {
            return v.getDefault();
        }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static int maxBonds() { return get(MAX_BONDS); }

    public static boolean holyUnique() { return get(HOLY_UNIQUE); }

    public static boolean vassalUnique() { return get(VASSAL_UNIQUE); }

    public static boolean holyRejectsOthers() { return get(HOLY_REJECTS_OTHERS); }
    public static boolean castingRelicsAllowed() { return get(CASTING_RELICS_ALLOWED); }
    public static boolean consumablesFirst() { return get(CONSUMABLES_FIRST); }

    public static boolean repelXp() { return get(REPEL_XP); }

    public static int repelRadius() { return get(REPEL_RADIUS); }

    public static int repelNoticeSeconds() { return get(REPEL_NOTICE_SECONDS); }

    public static boolean adoptStacks() { return get(ADOPT_STACKS); }

    public static double expMultiplier() { return get(WEAPON_EXP_MULTIPLIER); }

    public static int maxLevel() { return get(MAX_LEVEL); }

    public static double killExpShare() { return get(KILL_EXP_SHARE); }

    public static double bossKillExpFactor() { return get(BOSS_KILL_EXP_FACTOR); }

    public static int maxKillExp() { return get(MAX_KILL_EXP); }

    public static float shockDamage() { return get(SHOCK_DAMAGE).floatValue(); }

    public static int shockSlowTicks() { return get(SHOCK_SLOW_TICKS); }

    public static int skillGlobalLockTicks() { return get(SKILL_GLOBAL_LOCK_TICKS); }

    public static int minCooldownTicks() { return get(MIN_COOLDOWN_TICKS); }

    public static int keySpacingTicks() { return get(KEY_SPACING_TICKS); }

    public static boolean showMessages() { return get(SHOW_MESSAGES); }
}
