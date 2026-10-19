package com.newuniverse.nusmp.antimagic;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure rules of the Anti-Magic Aura economy (no Minecraft types, so they are unit tested). Since 1.94.0 Anti-Magic pays for everything
 * with Tensura Aura, exactly like the Slash, Light, Body and Earth families: a page costs a fraction of the caster's MAX Aura with a flat
 * floor (the same shape as {@code book.EnergyBridge.aura(player, frac, floor)}), a sustained state costs a fraction every second, and
 * Aura that runs out ends the state. The old Anti-Magic Power (AMP) bar no longer pays or gates anything; it stays only as an accumulated
 * stat on the grimoire (see {@link #ampIsOnlyAStat}).
 *
 * <h3>Cost table</h3>
 * Cheap utility 1 to 3 percent, strong 5 to 10 percent, ultimate more; Black Meteorite on the Demon-Slayer Sword (80 percent of the
 * Aura, already in {@code item.DemonSlayerSwordItem}) stays the most expensive thing Anti-Magic does.
 * <ul>
 *   <li>Black Slash 2 percent (floor 8), Bull Thrust 3 percent (12), Black Divider 4 percent (15)</li>
 *   <li>Infinite Slash 5 percent (20), Black Meteorite page 6 percent (25), Black Hurricane 6 percent (25)</li>
 *   <li>Black Asta (the timed form before the Lord awakens) 12 percent (60), Infinite Slash Equinox 15 percent (80)</li>
 *   <li>Black Form (the Lord's toggle): 3 percent (12) to start, then 1.5 percent (3) of the max Aura every second while it is held</li>
 *   <li>Demon-Slayer Nullification Field (held sword): 0.3 percent (1) every second, paid only while something is in its reach</li>
 * </ul>
 */
public final class AntiMagicCosts {
    private AntiMagicCosts() {}

    /** A price: {@code frac} of the max Aura, but at least {@code floor}. */
    public record Cost(double frac, double floor) {
        /** The amount in Aura points for a caster whose max Aura is {@code maxAura}. */
        public double amount(double maxAura) { return Math.max(floor, Math.max(0, maxAura) * frac); }
    }

    /** Page id to price, in the order of the book. Every page of the Anti-Magic grimoire that is not a universal page is here. */
    public static final Map<String, Cost> PAGES;
    static {
        Map<String, Cost> m = new LinkedHashMap<>();
        m.put("demon_slayer_slash", new Cost(0.02, 8));
        m.put("black_divider", new Cost(0.04, 15));
        m.put("black_meteorite", new Cost(0.06, 25));
        m.put("black_hurricane", new Cost(0.06, 25));
        m.put("black_form", new Cost(0.12, 60));          // Black Asta, before the Lord awakens (the Lord's Black Form is FORM_START / FORM_DRAIN)
        m.put("bull_thrust", new Cost(0.03, 12));
        m.put("infinite_slash", new Cost(0.05, 20));
        m.put("infinite_slash_equinox", new Cost(0.15, 80));
        PAGES = Collections.unmodifiableMap(m);
    }

    /** The Lord's Black Form: what switching it on costs and what every second of it costs. */
    public static final Cost FORM_START = new Cost(0.03, 12), FORM_DRAIN = new Cost(0.015, 3);

    /** The Demon-Slayer's Conceptual Nullification Field, per second, while there is something in it to nullify. */
    public static final Cost FIELD_DRAIN = new Cost(0.003, 1);

    /** The price of a page; a page that is not in the table (a universal page) costs nothing here. */
    public static Cost page(String pageId) { return PAGES.getOrDefault(pageId, new Cost(0, 0)); }

    /** "Not enough aura to ..." the failure message every Aura-paying book uses. */
    public static String notEnough(String what) { return "Not enough aura to " + what + "."; }

    // ------------------------------------------------------------------ pools (the maths of EnergyBridge.aura, without Minecraft)
    /**
     * Pays {@code frac}/{@code floor} from the Aura pool, else from the magicule pool (the same fallback as {@code EnergyBridge.aura}).
     * Returns {@code {aura, magicule}} after paying, or {@code null} when neither pool can pay.
     */
    public static double[] pay(double aura, double magicule, double maxAura, double maxMagicule, double frac, double floor) {
        double auraCost = Math.max(floor, Math.max(0, maxAura) * frac), mpCost = Math.max(floor, Math.max(0, maxMagicule) * frac);
        if (aura >= auraCost) return new double[]{aura - auraCost, magicule};
        if (magicule >= mpCost) return new double[]{aura, magicule - mpCost};
        return null;
    }

    /** How many whole seconds a drain of {@code drain} per second lasts from {@code aura} (no regeneration counted). */
    public static int secondsLeft(double aura, double maxAura, Cost drain) {
        double per = drain.amount(maxAura);
        return per <= 0 ? Integer.MAX_VALUE : (int) Math.floor(Math.max(0, aura) / per);
    }

    /** The share of the Aura pool left, 0..1 (what the action bar shows beside the form; Tensura's own Aura bar shows the same pool). */
    public static double auraShare(double aura, double maxAura) {
        return maxAura <= 0 ? 0 : Math.max(0, Math.min(1, aura / maxAura));
    }

    // ------------------------------------------------------------------ the AMP stat and the migration
    /** The cap of the stored AMP stat (the same as {@code AntiMagic.MAX_AMP}; repeated here so this class stays free of Minecraft types). */
    public static final int AMP_CAP = 1000;

    /** Current version of the stored Anti-Magic data (0 = before the Aura change, which stored nothing). */
    public static final int DATA_VERSION = 1;

    /** What the grimoire keeps: the Lord state, Black Form, the accumulated AMP and the data version. */
    public record Stored(boolean lord, boolean form, int amp, int version) {}

    /**
     * Brings stored Anti-Magic data to the Aura economy. Nothing is lost: a Lord stays a Lord (a legacy Lord skill counts as awakened),
     * the Black Form stays on only for a Lord, and the AMP is kept as it is (clamped to 0..{@link #AMP_CAP}); only the version
     * moves up. Running it twice changes nothing.
     */
    public static Stored migrate(Stored s, boolean legacyLordSkill) {
        boolean lord = s.lord() || legacyLordSkill;
        return new Stored(lord, lord && s.form(), Math.max(0, Math.min(AMP_CAP, s.amp())), Math.max(s.version(), DATA_VERSION));
    }

    /** True: since 1.94.0 AMP gates and scales nothing (the Lord is awakened by mastery, Black Form costs Aura, damage scales with EP). */
    public static boolean ampIsOnlyAStat() { return true; }
}
