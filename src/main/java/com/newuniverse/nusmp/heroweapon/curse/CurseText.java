package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.Sin;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: every word the curse says to a player (the whispers of the weapon, the price of the Embrace, the warnings of the Overrun, the lines of the HUD) is a language
 * key with an English text, and the game code must be able to show the English text even before a language file carrying the key has been merged (the way the
 * System window does with {@code translatableWithFallback}). This class is that table, with no Minecraft type in it, so the whisper bank and the key list can be
 * tested (every sin has its lines, every key is unique, the text is plain ASCII) and the language fragment that goes into {@code en_us.json} is written from the
 * same table and cannot drift from the code. The whispers are the weapon speaking to a despairing wielder in the voice of the sin; they are original lines
 * written for this mod, not quotes of the series.
 */
public final class CurseText {
    private CurseText() {}

    private static final Map<String, String> EN = new LinkedHashMap<>();

    // ---- the offer and the Embrace
    public static final String OFFER_BAR = put("hero.curse.offer.bar", "%s whispers: \"%s\"");
    public static final String OFFER_CHAT = put("hero.curse.offer.chat",
            "%s offers you the Curse Series of %s. The price: %s hearts of maximum health for good, and corruption while the curse form is worn; at 1000 corruption the weapon takes your hands. "
                    + "To accept, hold Sneak + %s for 3 seconds, press Embrace in the Weapon Book, or type /multiverse hero curse embrace. If you do nothing it fades in %s seconds.");
    public static final String OFFER_KEY3 = put("hero.curse.offer.key3", "The third hero key accepts it as well: hold Sneak and that key for 3 seconds.");
    public static final String LAPSE = put("hero.curse.lapse", "The whisper fades.");
    public static final String DECLINE = put("hero.curse.decline", "You turn the whisper away. It will return if the despair does.");
    public static final String EMBRACE_TITLE = put("hero.curse.embrace.title", "The Curse of %s");
    public static final String EMBRACE_SUBTITLE = put("hero.curse.embrace.subtitle", "%s hearts of maximum health are given for good");
    public static final String EMBRACE_CHAT = put("hero.curse.embrace.chat", "You embrace the %s Series. %s is yours to wear. The weapon takes its price: %s hearts of maximum health.");
    public static final String EMBRACE_CHAT_NO_FORM = put("hero.curse.embrace.chat_no_form", "You embrace the %s Series. The weapon takes its price: %s hearts of maximum health.");
    public static final String FORCED_BASE = put("hero.curse.forced_base", "That curse form is not yours to wear. The weapon returns to its base form.");

    // ---- refusals
    public static final String REFUSE_NO_WEAPON = put("hero.curse.refuse.no_weapon", "You have no hero weapon.");
    public static final String REFUSE_NO_OFFER = put("hero.curse.refuse.no_offer", "The weapon has nothing to offer you.");
    public static final String REFUSE_LOCKED = put("hero.curse.refuse.locked", "The weapon holds you; nothing can be changed now.");
    public static final String REFUSE_OFF = put("hero.curse.refuse.off", "The Curse Series is switched off on this server.");
    public static final String REFUSE_ALREADY = put("hero.curse.refuse.already", "You have embraced that series already.");

    // ---- the Overrun
    public static final String OVERRUN_WARN = put("hero.curse.overrun.warn", "The weapon strains against you (%s of 1000). Put the curse form away, or it will take your hands.");
    public static final String OVERRUN_TITLE = put("hero.curse.overrun.title", "OVERRUN");
    public static final String OVERRUN_SUBTITLE = put("hero.curse.overrun.subtitle", "The weapon has taken your hands");
    public static final String OVERRUN_END = put("hero.curse.overrun.end", "The weapon lets go. Your arms are heavy for a minute.");
    public static final String OVERRUN_REFUSE = put("hero.curse.overrun.refuse", "The weapon does not listen to you.");

    // ---- cleansing
    public static final String PURIFY_DONE = put("hero.curse.purify.done", "The charm burns the corruption away.");
    public static final String PURIFY_REFUND = put("hero.curse.purify.refund", "It gives back %s hearts of your maximum health.");
    public static final String PURIFY_NOTHING = put("hero.curse.purify.nothing", "The charm finds nothing to cleanse.");
    public static final String PURIFY_LOCKED = put("hero.curse.purify.locked", "The charm cannot reach you while the weapon holds your hands.");
    public static final String PURIFY_NO_WEAPON = put("hero.curse.purify.no_weapon", "The charm answers only to the wielder of a hero weapon.");
    public static final String REST = put("hero.curse.rest", "Rest quiets the curse: %s corruption fades.");

    // ---- the sins
    public static final String ENVY_TAKE = put("hero.curse.envy.take", "Envy takes your %s for %s seconds.");
    public static final String ENVY_REFUSE = put("hero.curse.envy.refuse", "Envy has taken %s from you.");
    public static final String ENVY_STRIP = put("hero.curse.envy.strip", "Envy devours your %s.");
    public static final String SLOTH_REFUSE = put("hero.curse.sloth.refuse", "Sloth weighs your hand down.");

    // ---- the client
    public static final String HUD_CORRUPTION = put("hero.curse.hud.corruption", "Corruption");
    public static final String HUD_DESPAIR = put("hero.curse.hud.despair", "Despair");
    public static final String HUD_OVERRUN = put("hero.curse.hud.overrun", "OVERRUN");
    public static final String HUD_OFFER = put("hero.curse.hud.offer", "The weapon offers the Curse of %s");
    public static final String HUD_HOLD = put("hero.curse.hud.hold", "Hold Sneak + %s");
    public static final String KEY_EMBRACE = put("key.nusmp.hero_embrace", "Embrace the Curse (hold with Sneak)");

    // ---- the Purification Charm
    public static final String CHARM_NAME = put("item.nusmp.purification_charm", "Purification Charm");
    public static final String CHARM_TIP_1 = put("tooltip.nusmp.purification_charm.1", "Clears the corruption of a hero weapon's curse.");
    public static final String CHARM_TIP_2 = put("tooltip.nusmp.purification_charm.2", "The first use also gives back some of the hearts the curse took.");
    public static final String CHARM_TIP_3 = put("tooltip.nusmp.purification_charm.3", "A stand-in for the Bless Series.");

    /** Whisper lines per sin, four each; a sin appended to the enum later without lines of its own falls back to {@link #FALLBACK_WHISPERS}. */
    private static final Map<Sin, String[]> WHISPERS = new EnumMap<>(Sin.class);

    private static final String[] FALLBACK_WHISPERS = {
            "Take what I offer. You will not have to carry this alone.",
            "Is there anyone left who will stand with you?",
            "Let go of what you were. I will hold what you are.",
            "It does not have to hurt. Say yes."};

    static {
        WHISPERS.put(Sin.WRATH, new String[]{
                "They lied. They all lied. Let it burn.",
                "Rage is not weakness. Take it. I will carry you.",
                "Who stood by and watched? Remember their faces.",
                "Your hands are shaking. Let me hold them steady."});
        WHISPERS.put(Sin.PRIDE, new String[]{
                "Who are they to judge you? You alone know what is right.",
                "Take no hand. Take no pity. Justice needs no witnesses.",
                "They doubted you. Let them see how wrong they were.",
                "Stand tall. Lowering your eyes is how the weak begin."});
        WHISPERS.put(Sin.LUST, new String[]{
                "Everything you love is leaving. Hold on tighter.",
                "Take what is offered. Take what is not. They will stay.",
                "You are not alone. Not while I am in your hand.",
                "Let them come closer. Closer. Yes."});
        WHISPERS.put(Sin.ENVY, new String[]{
                "Look how easily it comes to them. Why not you?",
                "Everything they have could be yours. Take a copy.",
                "They smile with what should be yours.",
                "Why does it always go to someone else?"});
        WHISPERS.put(Sin.GREED, new String[]{
                "More. There is always more, and it should be yours.",
                "Hold it. Count it. Never let it leave your hand.",
                "A little more power costs only a little more of you.",
                "Why share what you can keep?"});
        WHISPERS.put(Sin.SLOTH, new String[]{
                "Rest. It does not matter. Nothing you do will matter.",
                "Why struggle? Lie down. I will hold the world for you.",
                "So heavy. Let your arms fall. Just a little.",
                "Tomorrow. Everything can wait until tomorrow."});
        WHISPERS.put(Sin.GLUTTONY, new String[]{
                "You are empty. Fill the hollow. Fill it with anything.",
                "Hunger is honest. Feed it.",
                "Strength is food. Everything strong is food.",
                "Eat. Eat. There is room for all of them."});
        for (Sin sin : Sin.values()) {
            String[] lines = lines(sin);
            for (int i = 0; i < lines.length; i++) put(whisperKey(sin, i), lines[i]);
        }
    }

    private static String[] lines(Sin sin) { return WHISPERS.getOrDefault(sin, FALLBACK_WHISPERS); }

    private static String put(String key, String english) {
        EN.put(key, english);
        return key;
    }

    /** The language key of whisper {@code n} (0-based) of a sin: {@code hero.curse.whisper.<sin>.<n+1>}. */
    public static String whisperKey(Sin sin, int n) { return "hero.curse.whisper." + sin.id() + "." + (n + 1); }

    /** How many whisper lines every sin has. */
    public static final int WHISPER_COUNT = 4;

    /** The whisper lines of a sin, in order (English). */
    public static List<String> whispers(Sin sin) { return List.of(lines(sin)); }

    /** A counter that goes up forever turned into a whisper index, wrapping around. */
    public static int whisperIndex(long n) { return (int) Math.floorMod(n, (long) WHISPER_COUNT); }

    /** The English text of a key, or the key itself when it is not one of ours. */
    public static String english(String key) { return EN.getOrDefault(key, key); }

    /** Every key and its English text, in the order they were declared (for the language fragment and the tests). */
    public static Map<String, String> all() { return Collections.unmodifiableMap(EN); }
}
