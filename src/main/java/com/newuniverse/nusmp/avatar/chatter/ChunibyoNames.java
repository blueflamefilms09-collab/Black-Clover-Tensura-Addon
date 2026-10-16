package com.newuniverse.nusmp.avatar.chatter;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: a Chunibyo Avatar never says "Exploding Fireball", it says "CRIMSON JUDGEMENT OF THE FALLEN SUN". One pure table maps the real page and
 * skill names to over-dramatic ones for the SKILL_CAST shout; a name that is not in the table still gets a stable (hash chosen, never random)
 * flourish so the same spell always has the same grand title.
 */
public final class ChunibyoNames {
    private ChunibyoNames() {}

    private static final Map<String, String> TABLE = Map.ofEntries(
            Map.entry("exploding fireball", "Crimson Judgement of the Fallen Sun"),
            Map.entry("sol linea", "Spear of the Dying Star"),
            Map.entry("calderos", "Cauldron of the Nine Hells"),
            Map.entry("waterball", "Tear of the Drowned Moon"),
            Map.entry("frost ball", "Sphere of Eternal Winter"),
            Map.entry("boulder shot", "Meteor of the Shattered Earth"),
            Map.entry("slash wave", "Crescent of the Severed Horizon"),
            Map.entry("water blade", "Edge of the Weeping Abyss"),
            Map.entry("voice cannon", "Roar of the Primordial Void"),
            Map.entry("mana strike", "Awakened Fist of the Pure Mana"),
            Map.entry("mana zone", "Forbidden Domain of Boundless Mana"),
            Map.entry("thunder lance", "Spear of the Wrathful Heavens"),
            Map.entry("iron strike", "Fang of the Black Iron Dragon"),
            Map.entry("hex shards", "Shards of the Cursed Kaleidoscope"),
            Map.entry("light shaft of divine punishment", "Radiant Verdict of the Seventh Heaven"));

    private static final List<String> OPEN = List.of("Crimson", "Abyssal", "Eternal", "Forbidden", "Twilight", "Astral", "Fallen", "Void-Born");
    private static final List<String> CLOSE = List.of("of the Fallen Sun", "of the Nine Hells", "of the Shattered Moon", "of Endless Dusk",
            "of the Dragon's Last Breath", "of the Sealed Heaven", "of the Hundred Nights", "of the Dark Flame");

    /** The grand title for a spell or skill name. Null or blank gives a title for "the Technique That Has No Name". */
    public static String rename(String real) {
        if (real == null || real.isBlank()) return "THE TECHNIQUE THAT HAS NO NAME";
        String key = real.strip().toLowerCase(Locale.ROOT);
        String hit = TABLE.get(key);
        if (hit != null) return hit.toUpperCase(Locale.ROOT);
        int h = key.hashCode() & 0x7fffffff;
        return (OPEN.get(h % OPEN.size()) + " " + real.strip() + " " + CLOSE.get((h / 8) % CLOSE.size())).toUpperCase(Locale.ROOT);
    }
}
