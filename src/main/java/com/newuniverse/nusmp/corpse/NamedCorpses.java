package com.newuniverse.nusmp.corpse;

/**
 * Why: Jimmy, Alfredo and Carl are Rades's named Soul Corpse puppets. The canon gives their names and little else, so their roles
 * here are INVENTED (see docs/exclusive/rs.md) and are kept in one table of numbers: entity attributes, the renderer's scale and the
 * tests all read the same row, and a balance change is one edit. No game types: the entity class turns a row into attributes.
 */
public enum NamedCorpses {
    /** Frontline brawler: fast, light, hits often. */
    JIMMY("corpse_jimmy", "Jimmy", Role.BRAWLER, 70, 7.0, 0.36, 2, 0.10, 0.62f, 1.95f, 0.0),
    /** Heavy bruiser: slow, a wall of health and armour, hits very hard. */
    ALFREDO("corpse_alfredo", "Alfredo", Role.BRUISER, 260, 12.0, 0.20, 10, 0.90, 1.0f, 2.55f, 0.0),
    /** Ranged support: keeps its distance, throws bone shards and Soul Corpse bolts. It never heals anyone. */
    CARL("corpse_carl", "Carl", Role.CASTER, 56, 3.0, 0.26, 0, 0.0, 0.58f, 1.9f, 4.5);

    public enum Role { BRAWLER, BRUISER, CASTER }

    /** Seconds between two volleys of bone shards, and between two Soul Corpse bolts, for the caster role. */
    public static final int SHARD_EVERY_TICKS = 40, BOLT_EVERY_TICKS = 160;
    /** Shards per volley and the range a caster keeps / attacks from. */
    public static final int SHARDS_PER_VOLLEY = 3;
    public static final double CASTER_RANGE = 16.0, CASTER_KEEP_AWAY = 7.0;
    /** Bolt damage in shard damages (one bolt is a few shards at once). */
    public static final double BOLT_FACTOR = 2.2;

    public final String id, display;
    public final Role role;
    public final double health, attack, speed, armor, knockbackResistance;
    public final float width, height;
    /** Damage of one bone shard (0 for the melee roles). */
    public final double shardDamage;

    NamedCorpses(String id, String display, Role role, double health, double attack, double speed, double armor, double knockbackResistance,
                 float width, float height, double shardDamage) {
        this.id = id;
        this.display = display;
        this.role = role;
        this.health = health;
        this.attack = attack;
        this.speed = speed;
        this.armor = armor;
        this.knockbackResistance = knockbackResistance;
        this.width = width;
        this.height = height;
        this.shardDamage = shardDamage;
    }

    public boolean ranged() { return role == Role.CASTER; }

    /** The named corpse with this registry path, or null. */
    public static NamedCorpses byId(String id) {
        for (NamedCorpses n : values()) if (n.id.equals(id)) return n;
        return null;
    }
}
