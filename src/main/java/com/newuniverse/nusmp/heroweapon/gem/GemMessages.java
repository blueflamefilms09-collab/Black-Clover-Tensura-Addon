package com.newuniverse.nusmp.heroweapon.gem;

import com.newuniverse.nusmp.heroweapon.HeroKind;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: every sentence the acquisition system shows (item names, tooltips, the refusals, the countdown, the title that tells a player a weapon chose them)
 * has an English text, and that text must exist in two places that must not differ: as the fallback the game uses when the lang file lacks the key, and
 * as the entry in the lang file. This pure class is the single list. The game code asks {@link #en} for the fallback; the lang fragment handed to the lead is
 * written from {@link #all()}; and {@code GemMessagesTest} fails when a key is repeated, a text is empty or two texts disagree on how many {@code %s} they
 * take, and, once the lead has merged the fragment into {@code en_us.json}, when the merged file says something else. Placeholders are always {@code %s}
 * (the game's translation format knows no other kind).
 */
public final class GemMessages {
    private GemMessages() {}

    private static final Map<String, String> EN = new LinkedHashMap<>();

    private static String put(String key, String english) {
        EN.put(key, english);
        return key;
    }

    // ------------------------------------------------------------------ items
    public static final String ITEM_HOLY = put("item.nusmp." + GemCatalog.HOLY_ID, "Calling Gem");
    public static final String ITEM_FRAGMENT = put("item.nusmp." + GemCatalog.FRAGMENT_ID, "Gem Fragment");

    static {
        for (HeroKind k : GemCatalog.sealKinds()) put("item.nusmp." + GemCatalog.sealId(k), GemCatalog.sealName(k));
    }

    // ------------------------------------------------------------------ tooltips
    public static final String TIP_HOLY_KIND = put("tooltip.nusmp.hero.gem.holy.kind", "Holy Calling Gem");
    public static final String TIP_HOLY_LORE = put("tooltip.nusmp.hero.gem.holy.lore", "One of the four Legendary Weapons will choose you.");
    public static final String TIP_HOLY_RULE = put("tooltip.nusmp.hero.gem.holy.rule", "Only a Legendary Weapon nobody holds can answer. The gem is spent when one does.");
    public static final String TIP_SEAL_KIND = put("tooltip.nusmp.hero.gem.seal.kind", "Seal of a %s");
    public static final String TIP_SEAL_LORE = put("tooltip.nusmp.hero.gem.seal.lore", "The %s chooses those who prove worthy.");
    public static final String TIP_SEAL_HOLDER = put("tooltip.nusmp.hero.gem.seal.holder", "Before you it served %s.");
    public static final String TIP_SEAL_RULE = put("tooltip.nusmp.hero.gem.seal.rule", "The weapon binds to you at once. The seal is spent.");
    public static final String TIP_USE = put("tooltip.nusmp.hero.gem.use", "Hold right click for %s seconds, standing still in the open.");
    public static final String TIP_FRAGMENT = put("tooltip.nusmp.hero.gem.fragment", "A shard of a calling gem. Every Vassal Seal is made from one.");

    // ------------------------------------------------------------------ why a ritual cannot start
    public static final String CLOSED = put("hero.msg.gem.closed", "The gem is silent. This server has closed the Choosing (gem config: recipes = false).");
    public static final String CLOSED_CRAFT = put("hero.msg.gem.closed_craft", "The Choosing is closed on this server: this gem will not answer you.");
    public static final String BOUND = put("hero.msg.gem.bound", "You already carry the %s. Release it before you answer another call.");
    public static final String NONE_FREE = put("hero.msg.gem.none_free", "The legendary weapons have already chosen their heroes.");
    public static final String REFUSED_SEAL = put("hero.msg.gem.refused_seal", "The %s does not answer you: %s.");
    public static final String REFUSED_GEM = put("hero.msg.gem.refused_gem", "The gem does not answer you: %s.");
    public static final String RIDING = put("hero.msg.gem.riding", "Dismount first: the Choosing needs solid ground under you.");
    public static final String SUBMERGED = put("hero.msg.gem.submerged", "The gem will not wake under water.");
    public static final String AIRBORNE = put("hero.msg.gem.airborne", "Stand on firm ground. The gem cannot find you in the air.");
    public static final String MOVING = put("hero.msg.gem.moving", "Stand still. The gem cannot find you while you are moving.");
    public static final String CLOSED_ABOVE = put("hero.msg.gem.closed_above", "The gem needs open space above you: at least %s blocks of clear air.");

    // ------------------------------------------------------------------ the ritual
    public static final String START = put("hero.msg.gem.start", "The gem begins to glow. Hold still for %s seconds.");
    public static final String COUNT = put("hero.msg.gem.count", "%s...");
    public static final String INT_MOVED = put("hero.msg.gem.int_moved", "You moved: the circle breaks.");
    public static final String INT_HURT = put("hero.msg.gem.int_hurt", "You were struck: the light scatters.");
    public static final String INT_CLOSED = put("hero.msg.gem.int_closed", "Something covers the sky above you: the circle breaks.");
    public static final String INT_RELEASED = put("hero.msg.gem.int_released", "The gem's light settles again.");
    public static final String INT_LOST = put("hero.msg.gem.int_lost", "The gem is no longer in your hand: the circle fades.");
    public static final String TITLE = put("hero.msg.gem.title", "The %s has chosen you.");
    public static final String SUBTITLE_HOLDER = put("hero.msg.gem.subtitle_holder", "Before you it served %s.");
    public static final String SUBTITLE_PLAIN = put("hero.msg.gem.subtitle_plain", "Its gem wakes in your hand.");
    public static final String CHOSEN = put("hero.msg.gem.chosen", "The %s answers the call. It is bound to you until you release it.");

    // ------------------------------------------------------------------ commands
    public static final String GAVE = put("hero.msg.gem.give", "Gave %s a %s.");
    public static final String NO_SEAL = put("hero.msg.gem.no_seal", "The %s has no Seal. Only the Seven Star and the other Vassal Weapons have one: use 'give <player> holy' for the Holy Calling Gem.");
    public static final String UNKNOWN_WEAPON = put("hero.msg.gem.unknown_weapon", "There is no weapon called '%s'.");
    public static final String HOLDERS_HEAD = put("hero.msg.gem.holders_head", "Hero weapon holders (%s):");
    public static final String HOLDERS_NONE = put("hero.msg.gem.holders_none", "Nobody holds a hero weapon.");

    /** The English text of a key, or the key itself when it is not one of ours (so a typo shows up on screen instead of as an empty line). */
    public static String en(String key) {
        String t = EN.get(key);
        return t == null ? key : t;
    }

    /** Every key with its English text, in declaration order (the lang fragment is written from this). */
    public static Map<String, String> all() { return Collections.unmodifiableMap(EN); }

    /** The message key that tells a player why a ritual cannot start; a refused Seal and a refused Holy Gem read differently. */
    public static String denialKey(GemRules.Denial d, boolean sealed) {
        if (d == GemRules.Denial.GATE_REFUSED) return sealed ? REFUSED_SEAL : REFUSED_GEM;
        return "hero.msg.gem." + d.key();
    }

    /** The message key that tells a player why a ritual broke. */
    public static String interruptKey(GemRitual.Interrupt why) { return "hero.msg.gem.int_" + why.key(); }
}
