package com.newuniverse.nusmp.shishou;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Why: the skill tree of Shishou (docs/items/shishou.md) is ONE table, so the tiers, the unlock levels, the mana costs, the cooldowns, the config switches and the lang keys cannot drift apart. Pure (no game
 * classes). Every skill is a Shishou-owned ability; the Tensura names are carried as names (the Japanese reading is in the lang tooltip), they never call the Tensura API.
 *
 * <p>A skill is available either by LEVEL (tier 1 from level 1, tier 2 from 21, tier 3 from 50) or, for the absorbed ones, because Skill Eater stole it from a monster (a bit in the record). The {@code bit}
 * is the stable position in the saved mask: entries are only ever appended and a bit is never reused (saves depend on it).</p>
 */
public enum ShishouSkill {
    // ---- tier 1 (level 1..20)
    TELEKINESIS(0, 1, 1, Kind.PASSIVE, 2, 20, "nennou", "Telekinesis"),
    THOUGHT_COMMUNICATION(1, 1, 1, Kind.PASSIVE, 0, 0, "nenka", "Thought Communication"),
    ULTRA_SPEED_REGENERATION(2, 1, 1, Kind.PASSIVE, 0, 0, "chousokuryou", "Ultra-Speed Regeneration"),
    STAT_CONCEALMENT(3, 1, 1, Kind.PASSIVE, 0, 0, "gisou", "Stat Concealment"),
    SKILL_EATER(4, 1, 1, Kind.PASSIVE, 0, 0, "hoshoku", "Skill Eater"),
    SKILL_SHARING(5, 1, 1, Kind.PASSIVE, 0, 0, "gaisou", "Skill Sharing"),
    WIELDER_REGISTRATION(6, 1, 1, Kind.PASSIVE, 0, 0, "keiyaku", "Wielder Registration"),
    APPRAISAL(7, 1, 1, Kind.PASSIVE, 0, 0, "kantei", "Appraisal"),
    MULTILAYER_BARRIER(8, 1, 10, Kind.TOGGLE, 0, 600, "taju_kekkai", "Multilayer Barrier"),
    // ---- tier 2 (level 21..49)
    STOMACH(9, 2, 21, Kind.ACTIVE, 4, 60, "ibukuro", "Stomach"),
    ELEMENTAL_SYNTHESIS(10, 2, 21, Kind.PASSIVE, 0, 0, "gouseisei", "Elemental Synthesis"),
    SWORD_ARTS_MASTERY(11, 2, 25, Kind.PASSIVE, 0, 0, "kenjutsu", "Sword Arts Mastery"),
    MAGIC_SENSE(12, 2, 25, Kind.TOGGLE, 10, 400, "magikansi", "Magic Sense"),
    BLACK_FLAME(13, 2, 30, Kind.PASSIVE, 0, 0, "kuroen", "Black Flame"),
    STEALTH(14, 2, 35, Kind.TOGGLE, 20, 400, "inpei", "Stealth and Camouflage"),
    VIBRATION_MAGIC(15, 2, 40, Kind.PASSIVE, 0, 0, "shindou", "Vibration Magic"),
    // ---- tier 3 (level 50+)
    SPATIAL_MOTION(16, 3, 50, Kind.ACTIVE, 15, 100, "kuukan_idou", "Spatial Motion"),
    MAKA(17, 3, 50, Kind.PASSIVE, 0, 0, "maka", "Demon Lord's Haki"),
    DIVINE_SWORD_RELEASE(18, 3, 50, Kind.ACTIVE, 100, 3600, "shinken_kaihou", "Divine Sword Release"),
    BLACK_LIGHTNING(19, 3, 50, Kind.ACTIVE, 40, 160, "kurokaminari", "Black Lightning"),
    SPATIAL_BLADE(20, 3, 50, Kind.PASSIVE, 15, 20, "kuukan_zandan", "Spatial Blade"),
    DOPPELGANGER(21, 3, 60, Kind.ACTIVE, 30, 400, "bunshin", "Doppelganger"),
    // ---- absorbed with Skill Eater (no unlock level: the bit in the record is the unlock)
    FIRE_MAGIC(22, 2, 0, Kind.ACTIVE, 12, 40, "kaen", "Fire Magic"),
    WIND_MAGIC(23, 2, 0, Kind.ACTIVE, 10, 60, "fuuma", "Wind Magic"),
    EARTH_MAGIC(24, 2, 0, Kind.ACTIVE, 14, 60, "tsuchi", "Earth Magic"),
    WATER_ICE_MAGIC(25, 2, 0, Kind.ACTIVE, 12, 60, "hyousui", "Water and Ice Magic"),
    DARKNESS_MAGIC(26, 2, 0, Kind.ACTIVE, 12, 80, "yami", "Darkness Magic"),
    HEALING_MAGIC(27, 2, 0, Kind.ACTIVE, 15, 100, "kaifuku", "Healing Magic"),
    PURIFICATION(28, 2, 0, Kind.ACTIVE, 10, 120, "joka", "Purification"),
    POISON_COAT(29, 2, 0, Kind.PASSIVE, 0, 0, "doku", "Poison Coat"),
    SPATIAL_MOTION_SHORT(30, 2, 0, Kind.ACTIVE, 10, 100, "kuukan_idou_short", "Spatial Motion (short)");

    /** PASSIVE: always on once available (some pay mana per use); ACTIVE: cast with the cast key; TOGGLE: the cast key switches it on and off (or starts its timed window). */
    public enum Kind { PASSIVE, ACTIVE, TOGGLE }

    private final int bit, tier, unlockLevel, manaCost, cooldown;
    private final Kind kind;
    private final String jp, english;

    ShishouSkill(int bit, int tier, int unlockLevel, Kind kind, int manaCost, int cooldown, String jp, String english) {
        this.bit = bit;
        this.tier = tier;
        this.unlockLevel = unlockLevel;
        this.kind = kind;
        this.manaCost = manaCost;
        this.cooldown = cooldown;
        this.jp = jp;
        this.english = english;
    }

    public int bit() { return bit; }
    public int tier() { return tier; }
    /** The level that unlocks it; 0 = absorbed only. */
    public int unlockLevel() { return unlockLevel; }
    public Kind kind() { return kind; }
    public int manaCost() { return manaCost; }
    public int cooldown() { return cooldown; }
    public String english() { return english; }
    /** The romaji of the Japanese name; the kanji are in the lang text. */
    public String romaji() { return jp; }
    public boolean absorbed() { return unlockLevel == 0; }
    public boolean castable() { return kind != Kind.PASSIVE; }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public String langKey() { return "shishou.skill." + id(); }
    /** The key of the SERVER config switch of this skill (under [skills]). */
    public String configKey() { return id(); }

    /** The level tier of a level: 1 (1..20), 2 (21..49), 3 (50 and above). */
    public static int tierOf(int level) { return level >= 50 ? 3 : level >= 21 ? 2 : 1; }

    /** Is the skill usable at {@code level} with the absorbed bits {@code learned}? */
    public boolean available(int level, long learned) {
        if (absorbed()) return (learned >> bit & 1L) != 0L;
        return level >= unlockLevel;
    }

    public static ShishouSkill byId(String id) {
        if (id == null) return null;
        for (ShishouSkill s : values()) if (s.id().equals(id.toLowerCase(Locale.ROOT))) return s;
        return null;
    }

    public static ShishouSkill byBit(int bit) {
        for (ShishouSkill s : values()) if (s.bit == bit) return s;
        return null;
    }

    /** Every skill available at this level with these absorbed bits, in table order. */
    public static List<ShishouSkill> availableList(int level, long learned) {
        List<ShishouSkill> out = new ArrayList<>();
        for (ShishouSkill s : values()) if (s.available(level, learned)) out.add(s);
        return out;
    }

    /** The skills the cast key can cycle through (available and not passive), in table order. */
    public static List<ShishouSkill> castList(int level, long learned) {
        List<ShishouSkill> out = new ArrayList<>();
        for (ShishouSkill s : values()) if (s.castable() && s.available(level, learned)) out.add(s);
        return out;
    }

    /** The next castable skill after {@code current} (wraps; the first one when current is null or no longer available); null when there is none. */
    public static ShishouSkill nextCastable(ShishouSkill current, int level, long learned) {
        List<ShishouSkill> l = castList(level, learned);
        if (l.isEmpty()) return null;
        int i = current == null ? -1 : l.indexOf(current);
        return l.get((i + 1) % l.size());
    }

    /** The skills a level-up from {@code from} to {@code to} newly unlocks (by level only). */
    public static List<ShishouSkill> unlockedBetween(int from, int to) {
        List<ShishouSkill> out = new ArrayList<>();
        for (ShishouSkill s : values()) if (!s.absorbed() && s.unlockLevel > from && s.unlockLevel <= to) out.add(s);
        return out;
    }
}
