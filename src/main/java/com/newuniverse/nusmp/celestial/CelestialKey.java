package com.newuniverse.nusmp.celestial;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: Celestial Spirit Magic is built on physical keys, and every rule about them (which spirit a key calls, what it costs, how long the spirit stays,
 * what its Star Dress gives) is a table. The table lives here, with no game class in it, so the rules and the tests can read it and so that the twelve
 * Gold Zodiac keys, the silver keys and the Spirit King's gate key are listed in ONE place. The order is part of the save format (the ledger stores a bit
 * per ordinal): entries are only ever appended at the end, never reordered or removed.
 * <p>
 * Numbers: {@code costPermille} is the share of the caster's maximum magicules one summon costs (per thousand, with {@link CelestialRules#COST_FLOOR} as the
 * lower bound so a small pool still pays something); {@code power} is the spirit's base damage in half hearts at 1,000 EP, scaled by the caster's EP through
 * the balance law; {@code reach} is the radius or length of its ability in blocks. The abilities follow the canon spirits of Fairy Tail; where the anime
 * only shows a spirit in a supporting role the nearest honest version is built and the {@code note} says so.
 */
public enum CelestialKey {
    // ---- the twelve Gold Zodiac keys (ordinals 0..11, the order of the owner's brief)
    AQUARIUS("aquarius", "Aquarius", "Aquarius the Water Bearer", Rank.GOLD, 0x3FA9FF, "Water Blast",
            "Aquarius empties her urn: a torrent of water runs 14 blocks down your line of sight, hurls foes back and soaks them.",
            60, 400, 100, 9f, 14, 0.6f, "Her sea also washes away fire: allies in the torrent are put out.",
            "Aquarius Form", 0.04, 0.0, 0.0, 0),
    TAURUS("taurus", "Taurus", "Taurus the Golden Bull", Rank.GOLD, 0xD9A441, "Axe Slam",
            "Taurus brings his great axe down: a shockwave 6 blocks wide that staggers everything it hits.",
            70, 360, 80, 12f, 6, 0.5f, "Pure strength; a boss is only staggered.",
            "Taurus Form", 0.0, 5.0, 3.0, 0),
    LEO("leo", "Leo", "Leo the Lion (Loki)", Rank.GOLD, 0xFFE27A, "Regulus Impact",
            "Loki gathers the light of the stars into a beam 18 blocks long that burns and blinds.",
            90, 600, 100, 16f, 18, 0.6f, "His ring Regulus is the focus; the beam is holy light.",
            "Leo Form", 0.03, 3.0, 1.0, 0),
    VIRGO("virgo", "Virgo", "Virgo the Maiden", Rank.GOLD, 0xF2A0C8, "Punishment Chains",
            "Virgo's chains lash up from the ground and bind every foe within 8 blocks.",
            50, 480, 120, 3f, 8, 0.35f, "Hard control: shortened on players and bosses by the balance law.",
            "Virgo Form", 0.02, 1.0, 4.0, 2),
    SAGITTARIUS("sagittarius", "Sagittarius", "Sagittarius the Archer", Rank.GOLD, 0xC98A4B, "Archery Volley",
            "Sagittarius looses seven arrows that each find a different foe within 20 blocks.",
            60, 420, 140, 5f, 20, 0.6f, "A volley that spreads across targets instead of stacking on one.",
            "Sagittarius Form", 0.05, 2.0, 0.0, 0),
    GEMINI("gemini", "Gemini", "Gemini the Twins", Rank.GOLD, 0x7FB5FF, "Twin Mirror",
            "Gemini and Mini step out as two decoys: foes turn on the twins, and when they vanish they burst.",
            55, 700, 200, 6f, 10, 0.6f, "Canon Gemini copies a person; the copy is built as a decoy pair.",
            "Gemini Form", 0.06, 1.0, 1.0, 0),
    CANCER("cancer", "Cancer", "Cancer the Crab", Rank.GOLD, 0xFF8A6B, "Crab Shears",
            "Cancer snips with his scissors: a flurry of cuts that bleed and shear armour from foes within 5 blocks.",
            55, 380, 80, 7f, 5, 0.6f, "The scissors are a hairdresser's; the armour shear is the nearest honest cut.",
            "Cancer Form", 0.03, 4.0, 0.0, 0),
    SCORPIO("scorpio", "Scorpio", "Scorpio the Scorpion", Rank.GOLD, 0xD4A257, "Sand Buster",
            "Scorpio whips up a sandstorm 9 blocks wide that scours and blinds for several seconds.",
            80, 520, 120, 8f, 9, 0.6f, "Sand Buster is his canon spell; it ticks while the storm stands.",
            "Scorpio Form", 0.03, 3.0, 2.0, 0),
    CAPRICORN("capricorn", "Capricorn", "Capricorn the Goat", Rank.GOLD, 0x9A7FD6, "Goat Charge",
            "Capricorn lowers his horns and charges 10 blocks, trampling and throwing aside whatever stands in the way.",
            65, 400, 60, 10f, 10, 0.5f, "Canon Capricorn is a melee butler; the charge is the nearest honest version.",
            "Capricorn Form", 0.06, 3.0, 2.0, 0),
    PISCES("pisces", "Pisces", "Pisces the Fish", Rank.GOLD, 0x66E0D4, "Twin Fish Tide",
            "Two fish circle you: one jets water at the nearest foe, the other mends you and your allies.",
            60, 450, 120, 5f, 10, 0.6f, "Pisces is a gentle spirit; the healing is capped by the balance law.",
            "Pisces Form", 0.04, 0.0, 1.0, 4),
    ARIES("aries", "Aries", "Aries the Ram", Rank.GOLD, 0xFFC8E4, "Wool Wall",
            "Aries swells her wool into a wall: you and your allies within 8 blocks gain absorption and are cushioned from knockback.",
            45, 600, 160, 0f, 8, 0.0f, "Pure defence: no damage, the wool is the shield.",
            "Aries Form", 0.0, 0.0, 5.0, 6),
    LIBRA("libra", "Libra", "Libra the Scales", Rank.GOLD, 0xB0B0FF, "Gravity Change",
            "Libra tips her scales: within 11 blocks foes are weighed down and slowed while you and your allies move light.",
            85, 700, 160, 4f, 11, 0.45f, "Gravity is her canon domain; foes are weighed, not crushed.",
            "Libra Form", 0.08, 1.0, 1.0, 0),

    // ---- silver keys (ordinals 12..15): lesser spirits, weaker and cheaper, collected like the gold ones but not needed for the Gate
    HOROLOGIUM("horologium", "Horologium", "Horologium the Clock", Rank.SILVER, 0xC8B28A, "Shelter of Hours",
            "Horologium opens as a great clock: the clock's shelter lets you and allies within 6 blocks recover while foes cannot reach in.",
            30, 900, 160, 0f, 6, 0.0f, "Canon Horologium is a shelter; here it is a zone of regeneration and resistance (it does not touch Time Magic).",
            null, 0, 0, 0, 0),
    CRUX("crux", "Crux", "Crux the Southern Cross", Rank.SILVER, 0xAFE8FF, "Guiding Cross",
            "Crux shines overhead and reveals every foe within 24 blocks; allies see by it and move a little faster.",
            25, 500, 80, 0f, 24, 0.0f, "Canon Crux is a guide of the stars.",
            null, 0, 0, 0, 0),
    LYRA("lyra", "Lyra", "Lyra the Harp", Rank.SILVER, 0xFFD6F5, "Lullaby",
            "Lyra plays: foes within 10 blocks grow drowsy and slow, allies are soothed and regenerate.",
            30, 600, 120, 0f, 10, 0.3f, "Canon Lyra sings; sleep is only a deep slow on the weak, bosses barely notice.",
            null, 0, 0, 0, 0),
    NIKORA("nikora", "Nikora", "Nikora (Canis Minor)", Rank.SILVER, 0xFFFFFF, "Nikora Charge",
            "Nikora, the little dog spirit, bounds at the nearest foe and bumps it; it also warms your heart a little.",
            15, 300, 80, 2f, 8, 0.5f, "Canon Nikora is the weakest spirit and a comic one; it is built as such.",
            null, 0, 0, 0, 0),

    // ---- the gate key (ordinal 16): not a spirit, the key that opens the Celestial Gate
    SPIRIT_KING_GATE("spirit_king_gate", "Gate Key", "The Celestial Spirit King", Rank.GATE, 0xFFF3B0, "Celestial Gate",
            "Opens the Gate of the Celestial Spirit World to a player who carries all twelve Gold Zodiac keys.",
            0, 0, 0, 0f, 0, 0f, "Given when the twelfth gold key is found.",
            null, 0, 0, 0, 0);

    /** Gold keys call the zodiac spirits and count toward the Gate, silver keys call lesser ones, the gate key is the key of the Spirit King. */
    public enum Rank { GOLD, SILVER, GATE }

    /** A bit mask with a bit per gold key: all twelve set. */
    public static final int GOLD_MASK = 0xFFF;

    public final String id, display, spirit, ability, abilityText, note, dressName;
    public final Rank rank;
    public final int rgb, costPermille, cooldown, stay, reach, dressHealth;
    public final float power, bossFactor;
    public final double dressSpeed, dressAttack, dressArmor;

    CelestialKey(String id, String display, String spirit, Rank rank, int rgb, String ability, String abilityText, int costPermille, int cooldown, int stay,
                 float power, int reach, float bossFactor, String note, String dressName, double dressSpeed, double dressAttack, double dressArmor, int dressHealth) {
        this.id = id; this.display = display; this.spirit = spirit; this.rank = rank; this.rgb = rgb; this.ability = ability; this.abilityText = abilityText;
        this.costPermille = costPermille; this.cooldown = cooldown; this.stay = stay; this.power = power; this.reach = reach; this.bossFactor = bossFactor;
        this.note = note; this.dressName = dressName; this.dressSpeed = dressSpeed; this.dressAttack = dressAttack; this.dressArmor = dressArmor; this.dressHealth = dressHealth;
    }

    /** The registry id of the key's item: {@code aquarius_key} ... {@code spirit_king_gate_key}. */
    public String itemId() { return id + "_key"; }

    public boolean isGold() { return rank == Rank.GOLD; }

    /** True for a key that calls a spirit (gold and silver). */
    public boolean summons() { return rank != Rank.GATE; }

    /** True when the key has a Star Dress (the gold keys only). */
    public boolean hasStarDress() { return dressName != null; }

    public int bit() { return 1 << ordinal(); }

    /** The key with this id ({@code leo} or {@code leo_key}), or null. */
    public static CelestialKey byId(String id) {
        if (id == null) return null;
        for (CelestialKey k : values()) if (k.id.equals(id) || k.itemId().equals(id)) return k;
        return null;
    }

    public static CelestialKey byOrdinal(int i) {
        CelestialKey[] all = values();
        return i < 0 || i >= all.length ? null : all[i];
    }

    /** The twelve Gold Zodiac keys in the owner's order. */
    public static List<CelestialKey> gold() { return ofRank(Rank.GOLD); }

    public static List<CelestialKey> silver() { return ofRank(Rank.SILVER); }

    private static List<CelestialKey> ofRank(Rank r) {
        List<CelestialKey> out = new ArrayList<>();
        for (CelestialKey k : values()) if (k.rank == r) out.add(k);
        return List.copyOf(out);
    }
}
