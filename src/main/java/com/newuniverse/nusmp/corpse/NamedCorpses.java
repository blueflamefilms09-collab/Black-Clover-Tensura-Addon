package com.newuniverse.nusmp.corpse;

/**
 * Why: Rades Spirito's numbered undead are his own Soul Corpses: No.4 Jimmy, No.3 David, No.2 Alfredo, No.1 Carl and No.0 Michael Caesal. The canon gives their names, numbers and looks and little
 * else, so their roles here are INVENTED (see docs/exclusive/rs.md and docs/exclusive/rades_undead.md) and are kept in one table of numbers: entity attributes, the renderer's scale and the tests
 * all read the same row, and a balance change is one edit. No game types: the entity class turns a row into attributes. The first three rows are the 1.43.0 puppets with their stats and registry
 * ids unchanged (only their look and page names changed); David and Michael Caesal are appended after them.
 */
public enum NamedCorpses {
    /** No.4, frontline brawler: fast, light, hits often. */
    JIMMY("corpse_jimmy", "Jimmy", Role.BRAWLER, 70, 7.0, 0.36, 2, 0.10, 0.62f, 1.95f, 0.0, 4, Signature.NONE, 1.0),
    /** No.2, a guard: slow, a wall of health and armour, hits hard, and every 20 seconds shields the caster and the caster's side with a soul barrier. */
    ALFREDO("corpse_alfredo", "Alfredo", Role.BRUISER, 260, 12.0, 0.20, 10, 0.90, 1.0f, 2.55f, 0.0, 2, Signature.SOUL_BARRIER, 1.0),
    /** No.1, ranged support: keeps its distance, throws bone shards and Soul Corpse bolts. It never heals anyone. */
    CARL("corpse_carl", "Carl", Role.CASTER, 56, 3.0, 0.26, 0, 0.0, 0.58f, 1.9f, 4.5, 1, Signature.NONE, 1.0),
    /** No.3, the huge stitched bruiser: the slowest and the heaviest, a wall of health, and a ground slam every 8 seconds; it stays only 135 seconds. */
    DAVID("corpse_david", "David", Role.BRUISER, 380, 15.0, 0.17, 12, 1.0, 1.5f, 3.4f, 0.0, 3, Signature.GROUND_SLAM, 0.75),
    /** No.0, the elite: the strongest blow and the fastest feet, a soul barrage every 10 seconds; one at a time, it stays only 90 seconds. */
    MICHAEL_CAESAL("corpse_michael_caesal", "Michael Caesal", Role.ELITE, 210, 18.0, 0.38, 6, 0.60, 0.62f, 2.05f, 0.0, 0, Signature.SOUL_BARRAGE, 0.5);

    /** Appended after the three old roles. */
    public enum Role { BRAWLER, BRUISER, CASTER, ELITE }

    /** The signature move of a numbered undead (besides its blows): what it does is in {@link NamedCorpseEntity}, the numbers are the constants below. */
    public enum Signature { NONE, SOUL_BARRIER, GROUND_SLAM, SOUL_BARRAGE }

    /** Seconds between two volleys of bone shards, and between two Soul Corpse bolts, for the caster role. */
    public static final int SHARD_EVERY_TICKS = 40, BOLT_EVERY_TICKS = 160;
    /** Shards per volley and the range a caster keeps / attacks from. */
    public static final int SHARDS_PER_VOLLEY = 3;
    public static final double CASTER_RANGE = 16.0, CASTER_KEEP_AWAY = 7.0;
    /** Bolt damage in shard damages (one bolt is a few shards at once). */
    public static final double BOLT_FACTOR = 2.2;

    /** Most numbered undead one caster has at once (the oldest is dismissed first); the three old puppets still fit together. */
    public static final int MAX_AT_ONCE = 3;
    /** Soul barrier (Alfredo): every 20 s, within 12 blocks of him, the caster, the pact's people, the caster's other bodies and himself get Absorption II for 8 s; at most 6 at once. Absorption is a shield, not a heal. */
    public static final int BARRIER_EVERY_TICKS = 400, BARRIER_TICKS = 160, BARRIER_AMPLIFIER = 1, BARRIER_MAX_TARGETS = 6;
    public static final double BARRIER_RANGE = 12.0;
    /** Ground slam (David): every 8 s a foe is within 4 blocks, after a wind-up: everything hostile within 3.5 blocks takes 0.8 of his blow, is thrown back and slowed 2 s; at most 8 victims. */
    public static final int SLAM_EVERY_TICKS = 160, SLAM_MAX_TARGETS = 8, SLAM_SLOW_TICKS = 40;
    public static final double SLAM_RADIUS = 3.5, SLAM_TRIGGER = 4.0, SLAM_FACTOR = 0.8, SLAM_KNOCKBACK = 1.4;
    /** Soul barrage (Michael Caesal): every 10 s a foe is in sight within 18 blocks, after a wind-up: six soul bolts, one every 2 ticks, 5 each (30 in all, one target, magic damage). */
    public static final int BARRAGE_EVERY_TICKS = 200, BARRAGE_BOLTS = 6, BARRAGE_GAP_TICKS = 2;
    public static final double BARRAGE_RANGE = 18.0, BARRAGE_DAMAGE = 5.0;
    /** The first signature move waits this long after the summon, so none opens with it. */
    public static final int SIGNATURE_OPENING_TICKS = 60;
    /** The page cooldowns in ticks of the two new pages: David 30 s, the elite 3 minutes (the old pages keep the mid-tier 8 s). */
    public static final int DAVID_PAGE_COOLDOWN = 600, ELITE_PAGE_COOLDOWN = 3600;

    public final String id, display;
    public final Role role;
    public final double health, attack, speed, armor, knockbackResistance;
    public final float width, height;
    /** Damage of one bone shard (0 for the melee roles). */
    public final double shardDamage;
    /** The number in the series: No.4 Jimmy, No.3 David, No.2 Alfredo, No.1 Carl, No.0 Michael Caesal. */
    public final int number;
    public final Signature signature;
    /** Share of the configured named life ({@code corpse.namedLifeSeconds}) this one stays: 1.0 for the old three, less for the heavy and the elite. */
    public final double lifeFactor;

    NamedCorpses(String id, String display, Role role, double health, double attack, double speed, double armor, double knockbackResistance,
                 float width, float height, double shardDamage, int number, Signature signature, double lifeFactor) {
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
        this.number = number;
        this.signature = signature;
        this.lifeFactor = lifeFactor;
    }

    public boolean ranged() { return role == Role.CASTER; }

    /** The elite: at most one at a time per caster, and only while none stands (see {@link SoulCorpseSpells#named}). */
    public boolean elite() { return role == Role.ELITE; }

    /** "No.4 - Jimmy": how the page and the body are named. */
    public String title() { return "No." + number + " - " + display; }

    /** The life of this one in ticks given the configured named life. */
    public int lifeTicks(int namedLifeTicks) { return Math.max(20, (int) Math.round(namedLifeTicks * lifeFactor)); }

    /** The named corpse with this registry path, or null. */
    public static NamedCorpses byId(String id) {
        for (NamedCorpses n : values()) if (n.id.equals(id)) return n;
        return null;
    }
}
