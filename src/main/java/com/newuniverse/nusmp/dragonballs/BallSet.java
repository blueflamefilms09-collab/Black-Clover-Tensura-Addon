package com.newuniverse.nusmp.dragonballs;

import java.util.List;
import java.util.Locale;

/**
 * Why: the owner asked for two sets of Dragon Balls. The first is the Earth set (summons Shenron). The second is the stronger set of the anime, the Super Dragon Balls of the Dragon of the Gods
 * (summons Super Shenron). Everything that differs between the two (names, colours, how many wishes the default allows, the words that call the dragon) lives in this one pure enum, so the rest of the
 * package asks the set and never branches on strings. Canon (English Dragon Ball wiki, "Earth Dragon Ball", "Super Dragon Ball", "Shenron", "Super Shenron"): both sets are SEVEN balls; the setting
 * {@code dragonBalls.count} allows 7 or 8 (docs/dragon_balls.md). Pure: no Minecraft type.
 */
public enum BallSet {
    /** The Earth Dragon Balls: orange, red stars, Shenron. Default phrases: the owner's Namekian words and the "come forth" lines (Japanese wiki: Ideyo Shenron). */
    EARTH("earth", "Dragon Ball", "Shenron", 0xF08A20, 1,
            List.of("takkaradoppo pakkoradoppo yaikanaan", "come forth shenron", "arise shenron", "ideyo shenron", "detekoi shenron",
                    "takkaraputo popporunga pupiritto paro", "takkarapto popporunga pupiritto paro", "eternal dragon by your name i summon you forth shenron")),
    /** The Super Dragon Balls (the World Dragon Balls of the Dragon God Zalama): violet and gold, Super Shenron. English wiki: "Come forth, Dragon of the Gods, and grant my wish, pretty peas!" */
    SUPER("super", "Super Dragon Ball", "Super Shenron", 0xB25CF0, 2,
            List.of("come forth dragon of the gods and grant my wish pretty peas", "come forth divine dragon and grant my wish peas and carrots", "come forth dragon of the gods", "come forth divine dragon"));

    private final String id, ballName, dragonName;
    private final int color, defaultWishes;
    private final List<String> defaultPhrases;

    BallSet(String id, String ballName, String dragonName, int color, int defaultWishes, List<String> defaultPhrases) {
        this.id = id;
        this.ballName = ballName;
        this.dragonName = dragonName;
        this.color = color;
        this.defaultWishes = defaultWishes;
        this.defaultPhrases = defaultPhrases;
    }

    public String id() { return id; }

    /** English display name of one ball of this set ("Dragon Ball"). */
    public String ballName() { return ballName; }

    public String dragonName() { return dragonName; }

    public int color() { return color; }

    /** Wishes the default setting grants (Earth 1, Super 2: the owner's brief; canon Super Shenron grants one). */
    public int defaultWishes() { return defaultWishes; }

    public List<String> defaultPhrases() { return defaultPhrases; }

    /** The id of the ball item of {@code star} (1..8): {@code dragon_ball_earth_3}. */
    public String itemId(int star) { return "dragon_ball_" + id + "_" + star; }

    public static BallSet byId(String s) {
        if (s == null) return null;
        String k = s.toLowerCase(Locale.ROOT);
        for (BallSet b : values()) if (b.id.equals(k)) return b;
        return null;
    }

    public static BallSet byOrdinal(int i) { return i >= 0 && i < values().length ? values()[i] : null; }
}
