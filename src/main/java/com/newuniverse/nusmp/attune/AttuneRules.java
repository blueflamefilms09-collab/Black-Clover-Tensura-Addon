package com.newuniverse.nusmp.attune;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: the owner wants a player to hold several powers at once (a Beast Will, a Djinn, a Devil, a Spirit) through meditation, with elements that clash rejecting each other ("a fire Beast Will
 * and then an ice Djinn reject each other"). Every rule of that lives here, with no Minecraft type, so it is unit tested ({@code AttuneRulesTest}) and the client screen colours the pairs with the very
 * same table the server enforces. The table is in docs/attunement.md. Elements are a bit mask over {@link Element}; no bit set means neutral (fits anything, resonates with nothing).
 */
public final class AttuneRules {
    private AttuneRules() {}

    // ------------------------------------------------------------------ systems
    public static final int BEAST = 0, DJINN = 1, DEVIL = 2, SPIRIT = 3, SYSTEMS = 4;
    private static final String[] SYSTEM_KEYS = {"beast", "djinn", "devil", "spirit"};

    public static String systemKey(int system) { return system >= 0 && system < SYSTEMS ? SYSTEM_KEYS[system] : "none"; }

    /** The most slots any setting may give (one power per system, four systems). */
    public static final int MAX_SLOTS = SYSTEMS;
    /** The most powers one player can be offered on the screen (and the most ids a confirm may carry is {@link #MAX_SLOTS}). */
    public static final int MAX_OWNED = 24;
    public static final int MAX_ID_LENGTH = 64;
    /** The resonance bonus never passes this, whatever the setting per pair. */
    public static final double MAX_RESONANCE = 0.30;
    /** The most health a rejection's backlash takes; it always leaves at least one health. */
    public static final float BACKLASH_MAX = 4.0f;

    public static int slots(int configured) { return Math.max(1, Math.min(MAX_SLOTS, configured)); }

    // ------------------------------------------------------------------ elements
    public enum Element { FIRE, WATER, ICE, WIND, EARTH, LIGHTNING, LIGHT, DARK, LIFE, DEATH, SPACE }

    public static int bit(Element e) { return 1 << e.ordinal(); }

    /** Opposites: a power of one side rejects a power of the other. */
    private static final Element[][] REJECT = {
            {Element.FIRE, Element.WATER}, {Element.FIRE, Element.ICE}, {Element.LIGHT, Element.DARK}, {Element.LIFE, Element.DEATH}, {Element.WIND, Element.EARTH}};
    /** Allies: a pair resonates (same element always resonates too). */
    private static final Element[][] ALLY = {
            {Element.FIRE, Element.WIND}, {Element.FIRE, Element.LIGHTNING}, {Element.FIRE, Element.EARTH}, {Element.WATER, Element.ICE}, {Element.ICE, Element.WIND},
            {Element.WIND, Element.LIGHTNING}, {Element.EARTH, Element.LIFE}, {Element.LIGHT, Element.LIFE}, {Element.DARK, Element.DEATH}, {Element.DARK, Element.SPACE}};

    private static boolean listed(Element[][] table, Element a, Element b) {
        for (Element[] p : table) if ((p[0] == a && p[1] == b) || (p[0] == b && p[1] == a)) return true;
        return false;
    }

    public static boolean opposed(Element a, Element b) { return listed(REJECT, a, b); }

    public static boolean allied(Element a, Element b) { return a == b || listed(ALLY, a, b); }

    /**
     * The elements a label carries, read from the words of the label: a Beast Will's affinity ("Storm Fire", "Deep Sea"), a Djinn's magic ("HEAT"), a Devil's attribute ("ANTI_MAGIC"), a Spirit's grimoire
     * magic ("FLAME"). One table for the four systems, so a new will, djinn or devil gets its element without a new rule. "lightning" is read before "light".
     */
    public static int elementsOf(String label) {
        if (label == null) return 0;
        String s = label.toLowerCase(Locale.ROOT);
        int m = 0;
        if (has(s, "lightning", "thunder")) { m |= bit(Element.LIGHTNING); s = s.replace("lightning", " "); }
        if (has(s, "fire", "flame", "heat", "magma", "explosion", "scorch")) m |= bit(Element.FIRE);
        if (has(s, "water", "ocean", "sea", "flood", "strait", "tide", "abyss")) m |= bit(Element.WATER);
        if (has(s, "ice", "frost", "snow", "cold", "freez")) m |= bit(Element.ICE);
        if (has(s, "wind", "storm", "gale", "sky")) m |= bit(Element.WIND);
        if (has(s, "earth", "stone", "rock", "sand", "labyrinth")) m |= bit(Element.EARTH);
        if (has(s, "light", "holy", "judgement", "radian")) m |= bit(Element.LIGHT);
        if (has(s, "dark", "shadow", "chaos", "dread", "curse", "anti_magic", "antimagic", "abyss")) m |= bit(Element.DARK);
        if (has(s, "life", "plant", "wood", "heal")) m |= bit(Element.LIFE);
        if (has(s, "death", "decay", "rot", "venom", "poison", "soul", "ghost", "spectre", "underworld")) m |= bit(Element.DEATH);
        if (has(s, "space", "spatial", "gravity", "time", "void")) m |= bit(Element.SPACE);
        return m;
    }

    private static boolean has(String s, String... words) {
        for (String w : words) if (s.contains(w)) return true;
        return false;
    }

    public static List<Element> elements(int mask) {
        List<Element> out = new ArrayList<>(2);
        for (Element e : Element.values()) if ((mask & bit(e)) != 0) out.add(e);
        return out;
    }

    // ------------------------------------------------------------------ pairs of powers
    public enum Pair { NEUTRAL, RESONATE, REJECT }

    /** How two powers (element masks) stand: any opposed pair of their elements rejects, else any allied pair resonates, else neutral. Symmetric. */
    public static Pair pair(int a, int b) {
        boolean ally = false;
        for (Element x : Element.values()) {
            if ((a & bit(x)) == 0) continue;
            for (Element y : Element.values()) {
                if ((b & bit(y)) == 0) continue;
                if (opposed(x, y)) return Pair.REJECT;
                if (allied(x, y)) ally = true;
            }
        }
        return ally ? Pair.RESONATE : Pair.NEUTRAL;
    }

    /** One power as the rules see it: its save id, its system, its elements and its strength (who wins a clash). */
    public record Power(String id, int system, int elements, int rank) {}

    /** Why a chosen set is refused (null = it may be attuned). */
    public record Refusal(String reason, int first, int second) {}

    /**
     * May this set be attuned? {@code owned} are the ids the server found on the player this moment. Refusals in order: too many for the slots, an id the player does not own, an id twice, two of one system
     * (each system has one power out at a time), then the first rejecting pair (indices into {@code chosen}).
     */
    public static Refusal check(List<Power> chosen, Set<String> owned, int slots) {
        if (chosen.size() > slots(slots)) return new Refusal("too_many", -1, -1);
        Set<String> seen = new HashSet<>();
        boolean[] system = new boolean[SYSTEMS];
        for (int i = 0; i < chosen.size(); i++) {
            Power p = chosen.get(i);
            if (p == null || p.id() == null || !owned.contains(p.id())) return new Refusal("not_owned", i, -1);
            if (!seen.add(p.id())) return new Refusal("duplicate", i, -1);
            if (p.system() < 0 || p.system() >= SYSTEMS) return new Refusal("not_owned", i, -1);
            if (system[p.system()]) return new Refusal("same_system", i, -1);
            system[p.system()] = true;
        }
        for (int i = 0; i < chosen.size(); i++)
            for (int j = i + 1; j < chosen.size(); j++)
                if (pair(chosen.get(i).elements(), chosen.get(j).elements()) == Pair.REJECT) return new Refusal("reject", i, j);
        return null;
    }

    /**
     * Which of the powers that are out together are suppressed: the stronger is kept, the weaker of any rejecting pair is suppressed (equal strength: the one earlier in {@code ordered} wins, so the
     * caller lists attuned powers first, in slot order). Greedy over strength, so the result never keeps a rejecting pair. Returns the suppressed indices.
     */
    public static Set<Integer> suppressed(List<Power> ordered) {
        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) order.add(i);
        order.sort((x, y) -> ordered.get(y).rank() != ordered.get(x).rank() ? Integer.compare(ordered.get(y).rank(), ordered.get(x).rank()) : Integer.compare(x, y));
        List<Integer> kept = new ArrayList<>();
        Set<Integer> out = new HashSet<>();
        for (int i : order) {
            boolean clash = false;
            for (int k : kept) if (pair(ordered.get(i).elements(), ordered.get(k).elements()) == Pair.REJECT) { clash = true; break; }
            if (clash) out.add(i);
            else kept.add(i);
        }
        return out;
    }

    /** How many pairs of these powers resonate. */
    public static int resonatingPairs(List<Power> powers) {
        int n = 0;
        for (int i = 0; i < powers.size(); i++)
            for (int j = i + 1; j < powers.size(); j++)
                if (pair(powers.get(i).elements(), powers.get(j).elements()) == Pair.RESONATE) n++;
        return n;
    }

    /** The resonance multiplier: 1 + {@code perPair} per resonating pair, at most 1 + {@link #MAX_RESONANCE}. */
    public static double resonance(int pairs, double perPair) {
        double per = Double.isNaN(perPair) ? 0.0 : Math.max(0.0, perPair);
        return 1.0 + Math.min(MAX_RESONANCE, Math.max(0, pairs) * per);
    }

    // ------------------------------------------------------------------ damage
    /**
     * The extra factor this hook puts on a blow so the blow ends at {@code want} times its base when another hook already put {@code applied} on it (the 3.74.0 power raise). Never a product of the two:
     * {@code want} below {@code applied} gives a factor under 1 (a suppressed power loses the raise it had).
     */
    public static double extra(double want, double applied) {
        double w = clean(want), a = clean(applied);
        return w / a;
    }

    private static double clean(double f) { return Double.isNaN(f) || f < 1.0 ? 1.0 : Math.min(f, 1000.0); }

    /** The blow after the extra factor. Raised: held at {@code pvpShare} of a player's maximum health (never below what it was). Lowered: simply scaled. */
    public static float dealt(float amount, double extra, boolean targetIsPlayer, float targetMaxHealth, double pvpShare) {
        if (!(amount > 0f) || Double.isNaN(extra) || extra == 1.0) return amount;
        if (extra < 1.0) return (float) (amount * Math.max(0.0, extra));
        double raised = amount * extra;
        if (targetIsPlayer) raised = Math.max(amount, Math.min(raised, Math.max(0.0, targetMaxHealth) * Math.max(0.0, Math.min(1.0, pvpShare))));
        return (float) Math.min(raised, Float.MAX_VALUE / 4);
    }

    /** The backlash of a rejected attunement: at most {@link #BACKLASH_MAX}, never so much that the player falls under one health (never lethal). */
    public static float backlash(float health) {
        if (!(health > 1.0f)) return 0f;
        return Math.min(BACKLASH_MAX, health - 1.0f);
    }

    /** Ticks left before the attunement may change again (0 = it may). */
    public static long cooldownLeft(long lastChange, long now, int cooldownTicks) {
        if (lastChange <= 0) return 0;
        long ready = lastChange + Math.max(0, cooldownTicks);
        return now >= ready ? 0 : Math.min(ready - now, Math.max(0, cooldownTicks));
    }

    /** Meditation means standing still: the confirm is only taken within {@code maxMove} blocks of where the screen was opened and within {@code maxTicks} of it. */
    public static boolean stillMeditating(double dx, double dy, double dz, double maxMove, long openedAt, long now, int maxTicks) {
        if (now < openedAt || now - openedAt > maxTicks) return false;
        return dx * dx + dy * dy + dz * dz <= maxMove * maxMove;
    }

    /** A save id the server could have made: "system:name", lower case, bounded. */
    public static boolean validId(String id) {
        return id != null && id.length() <= MAX_ID_LENGTH && id.matches("(beast|djinn|devil|spirit):[a-z0-9_]{1,56}");
    }

    public static int systemOf(String id) {
        if (id == null) return -1;
        for (int i = 0; i < SYSTEMS; i++) if (id.startsWith(SYSTEM_KEYS[i] + ":")) return i;
        return -1;
    }
}
