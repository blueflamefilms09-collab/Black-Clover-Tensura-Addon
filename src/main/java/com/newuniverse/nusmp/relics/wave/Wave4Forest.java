package com.newuniverse.nusmp.relics.wave;

import java.util.Locale;

/**
 * Wave 4: the six spirit blessings of the Amulet of the Forest as numbers. Touch one figurine and the spirit walks with you for a
 * game day (24000 ticks); one spirit at a time. Each blessing is a few attribute modifiers, a few ambient effects and, where Tensura has the
 * matching gift, a lent Tensura skill (cat: Danger Sense, beetle: Physical Attack and Poison Resistance, raccoon: Sage, owl: Magic Sense): the glue
 * ({@code Wave4Handlers}) maps the attribute names below to the game's attributes and applies them once a second while the amulet
 * is worn and attuned. The D&D "advantage on checks" of each card became the nearest thing the game has. Pure Java.
 */
enum Wave4Forest {
    // Bear: Strength and one size larger.
    BEAR("bear", "Bear", new Mod[]{new Mod("scale", 0.2, Mod.MULT_BASE), new Mod("attack_damage", 3.0, Mod.ADD),
            new Mod("knockback_resistance", 0.3, Mod.ADD), new Mod("max_health", 4.0, Mod.ADD)}, new String[]{}, new String[]{}),
    // Cat: Dexterity and ten feet more speed.
    CAT("cat", "Cat", new Mod[]{new Mod("movement_speed", 0.25, Mod.MULT_BASE), new Mod("safe_fall_distance", 4.0, Mod.ADD),
            new Mod("jump_strength", 0.15, Mod.ADD)}, new String[]{}, new String[]{"tensura:danger_sense"}),
    // Beetle: Constitution, and healing that is always the most it can be (see healMultiplier).
    BEETLE("beetle", "Beetle", new Mod[]{new Mod("armor", 4.0, Mod.ADD), new Mod("armor_toughness", 2.0, Mod.ADD),
            new Mod("max_health", 4.0, Mod.ADD)}, new String[]{}, new String[]{"tensura:physical_attack_resistance", "tensura:poison_resistance"}),
    // Raccoon: Intelligence, and no illusion takes (blindness, darkness, nausea are shaken off by the glue).
    RACCOON("raccoon", "Raccoon", new Mod[]{new Mod("luck", 2.0, Mod.ADD)}, new String[]{}, new String[]{"tensura:sage"}),
    // Owl: Wisdom, seeing in the dark, and the mind cannot be charmed (the glue strips charm effects).
    OWL("owl", "Owl", new Mod[]{}, new String[]{"minecraft:night_vision"}, new String[]{"tensura:magic_sense"}),
    // Peacock: Charisma (villagers favour you) and the Charm Person of the card (an act).
    PEACOCK("peacock", "Peacock", new Mod[]{}, new String[]{"minecraft:hero_of_the_village"}, new String[]{});

    /** One attribute modifier: the attribute's short name, the amount and the operation. */
    record Mod(String attr, double amount, int op) {
        static final int ADD = 0, MULT_BASE = 1, MULT_TOTAL = 2;
    }

    /** A blessing lasts one game day. */
    static final int DURATION = 24000;
    /** Beetle: every heal is 50 percent larger. */
    static final float BEETLE_HEAL = 1.5f;

    final String id, label;
    final Mod[] mods;
    final String[] effects;
    /** Tensura skills lent while the spirit blesses you (ids the skill registry does not know are skipped at run time). */
    final String[] skills;

    Wave4Forest(String id, String label, Mod[] mods, String[] effects, String[] skills) {
        this.id = id; this.label = label; this.mods = mods; this.effects = effects; this.skills = skills;
    }

    static Wave4Forest byId(String id) {
        if (id == null) return null;
        String k = id.toLowerCase(Locale.ROOT);
        for (Wave4Forest s : values()) if (s.id.equals(k)) return s;
        return null;
    }

    static boolean active(long until, long now) { return until > now; }

    static float healMultiplier(Wave4Forest s) { return s == BEETLE ? BEETLE_HEAL : 1f; }

    /** Minutes of blessing left, rounded up, for the chat line. */
    static int minutesLeft(long until, long now) { return (int) Math.max(0, (until - now + 1199) / 1200); }
}
