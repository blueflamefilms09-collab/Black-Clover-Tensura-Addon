package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import com.newuniverse.nusmp.mana.zone.ManaZoneManager;
import com.newuniverse.nusmp.mana.zone.TrueMagicRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * The five Mana Method pages, appended last to every grimoire (after the spirit control pages, so no mode number moves) and late-game like the Mana Zone: they are listed and
 * castable only when the Mana Zone gate is open ({@link ManaZoneManager#gateOpenFor}) and the server setting is on. No key is bound: they are ordinary grimoire pages, cast
 * through the grimoire's own flow (hold, release, cost, cooldown).
 *
 * <table>
 *   <caption>The pages</caption>
 *   <tr><th>id</th><th>name</th><th>tier</th><th>cost</th><th>cooldown</th><th>rune</th></tr>
 *   <tr><td>mana_method_fire</td><td>Flame Rune</td><td>mid, late game</td><td>12 % (floor 300)</td><td>8 s</td><td>grows</td></tr>
 *   <tr><td>mana_method_water</td><td>Water Rune</td><td>mid, late game</td><td>12 %</td><td>8 s</td><td>pursues</td></tr>
 *   <tr><td>mana_method_wind</td><td>Wind Rune</td><td>mid, late game</td><td>12 %</td><td>8 s</td><td>pursues</td></tr>
 *   <tr><td>mana_method_earth</td><td>Earth Rune</td><td>mid, late game</td><td>12 %</td><td>8 s</td><td>grows</td></tr>
 *   <tr><td>mana_method_true</td><td>True Magic Rune</td><td>zone, late game</td><td>20 % (floor 600)</td><td>20 s</td><td>grows (your natural element)</td></tr>
 * </table>
 * <p>3.5.0: two mobility pages are appended LAST after these five (same gate, same setting, plus {@code manaMethod.mobility}): <b>Air Step Rune</b> ({@code mana_method_air_step}, 8 %, floor 150,
 * 12 s) and <b>Dash Rune</b> ({@code mana_method_dash}, 6 %, floor 100, 3 s). Their ids hold the words "step" and "dash", which the Avatar's list denies, so an Avatar never casts them.
 * The rune pages are charged by HOLDING the cast key ({@link RuneHold}); a tap fires the basic spell, a full hold the massive one.
 *
 * Casting while sneaking writes BOTH runes (pursue and grow), at 85 % of the power and 4 more ticks of charge. The ids contain no attack word, so an Avatar never casts them
 * (AvatarSkillList), and the Mana Method pages are not family pages, so the "15 spells" pages15 machinery and the armour policy do not see them.
 */
public final class ManaMethodPages {
    private ManaMethodPages() {}

    public static final String FIRE_ID = "mana_method_fire", WATER_ID = "mana_method_water", WIND_ID = "mana_method_wind", EARTH_ID = "mana_method_earth", TRUE_ID = "mana_method_true";
    /** 3.5.0: the two mobility pages, appended after the five rune pages. */
    public static final String AIR_STEP_ID = "mana_method_air_step", DASH_ID = "mana_method_dash";
    /** 3.6.0: the two canon pages, appended LAST after the mobility pages: Gadjah's runes on the body and Rill's painted rune. */
    public static final String BODY_ID = "mana_method_body", BRUSH_ID = "mana_method_brush";
    private static final List<String> RUNE_IDS = List.of(FIRE_ID, WATER_ID, WIND_ID, EARTH_ID, TRUE_ID);
    private static final List<String> IDS_35 = List.of(FIRE_ID, WATER_ID, WIND_ID, EARTH_ID, TRUE_ID, AIR_STEP_ID, DASH_ID);
    /** 3.8.0: the Rune State page (TIMED access), appended LAST after the Brush Rune, so no mode number moves. */
    public static final String STATE_ID = "mana_method_state";
    private static final List<String> IDS_36 = List.of(FIRE_ID, WATER_ID, WIND_ID, EARTH_ID, TRUE_ID, AIR_STEP_ID, DASH_ID, BODY_ID, BRUSH_ID);
    private static final List<String> IDS = List.of(FIRE_ID, WATER_ID, WIND_ID, EARTH_ID, TRUE_ID, AIR_STEP_ID, DASH_ID, BODY_ID, BRUSH_ID, STATE_ID);

    public static List<String> ids() { return IDS; }

    /** The five rune pages (3.4.0), without the mobility pages. */
    public static List<String> runeIds() { return RUNE_IDS; }

    public static boolean isPage(String id) { return id != null && IDS.contains(id); }

    public static boolean isMobility(String id) { return AIR_STEP_ID.equals(id) || DASH_ID.equals(id); }

    /** 3.6.0: the two canon pages (Body Rune, Brush Rune). */
    public static boolean isCanon(String id) { return BODY_ID.equals(id) || BRUSH_ID.equals(id); }

    /**
     * How many of the last pages of a book are Mana Method pages: all ten (3.8.0), the nine of 3.6.0, the seven of 3.5.0, the five of 3.4.0, or none (an older list). The pinned "the last pages are the spirit
     * pages" checks use it to look past them.
     */
    public static int tailCount(List<BookPage> pages) {
        int n = pages.size();
        for (List<String> ids : List.of(IDS, IDS_36, IDS_35, RUNE_IDS)) {
            int k = ids.size();
            if (n < k) continue;
            boolean all = true;
            for (int i = 0; i < k && all; i++) all = pages.get(n - k + i).id().equals(ids.get(i));
            if (all) return k;
        }
        return 0;
    }

    /** The pages as the grimoire appends them (always present, so the mode numbers never depend on the setting; {@link #usable} hides them). */
    public static List<BookPage> pages() {
        return List.of(
                element(FIRE_ID, "Flame Rune", RuneElement.FIRE),
                element(WATER_ID, "Water Rune", RuneElement.WATER),
                element(WIND_ID, "Wind Rune", RuneElement.WIND),
                element(EARTH_ID, "Earth Rune", RuneElement.EARTH),
                new BookPage(TRUE_ID, "True Magic Rune", "Mana Method: True Magic", RuneRules.COST_PERCENT_TRUE, RuneRules.COST_FLOOR_TRUE, RuneRules.COOLDOWN_TRUE, ManaMethodPages::castTrue),
                new BookPage(AIR_STEP_ID, "Air Step Rune", "Mana Method: Air Step", MobilityRules.COST_PERCENT_AIR_STEP, MobilityRules.COST_FLOOR_AIR_STEP, MobilityRules.COOLDOWN_AIR_STEP, ManaMethodPages::castAirStep),
                new BookPage(DASH_ID, "Dash Rune", "Mana Method: Dash", MobilityRules.COST_PERCENT_DASH, MobilityRules.COST_FLOOR_DASH, MobilityRules.COOLDOWN_DASH, ManaMethodPages::castDash),
                new BookPage(BODY_ID, "Body Rune", "Mana Method: Body", BodyRuneRules.COST_PERCENT, BodyRuneRules.COST_FLOOR, BodyRuneRules.COOLDOWN, ManaMethodPages::castBody),
                new BookPage(BRUSH_ID, "Brush Rune", "Mana Method: Brush", BrushRules.COST_PERCENT, BrushRules.COST_FLOOR, BrushRules.COOLDOWN, ManaMethodPages::castBrush),
                new BookPage(STATE_ID, "Rune State", "Mana Method: Rune State", 0, 0, 0, ManaMethodPages::castState));
    }

    private static BookPage element(String id, String name, RuneElement e) {
        return new BookPage(id, name, "Mana Method: " + e.title, RuneRules.COST_PERCENT, RuneRules.COST_FLOOR, RuneRules.COOLDOWN,
                (b, i, p, m) -> castElement(b, i, p, m, e));
    }

    /** The 3.7.0 test: the setting is on and the late-game gate of the Mana Zone is open for this entity (kept: it is the MANA_ZONE_GATE answer). */
    public static boolean usable(LivingEntity e) { return ManaMethodConfig.enabled() && ManaZoneManager.gateOpenFor(e); }

    /** As {@link #usable(LivingEntity)}, and the mobility pages also need the setting {@code manaMethod.mobility} (the 3.7.0 test with its per-page settings). */
    public static boolean usable(LivingEntity e, String pageId) {
        return usable(e) && pageSettingsOn(pageId);
    }

    private static boolean pageSettingsOn(String pageId) {
        return (!isMobility(pageId) || ManaMethodConfig.mobility()) && (!BODY_ID.equals(pageId) || ManaMethodConfig.bodyRunes()) && (!BRUSH_ID.equals(pageId) || ManaMethodConfig.brushRunes());
    }

    /**
     * 3.8.0: the decision the grimoire asks (on the server when a page is cast and on the client when a menu lists it), for the page {@code pageId} of the grimoire {@code b} with the
     * skill instance {@code i} (both may be null for a menu with no instance: then only the modes that need no instance can say yes).
     * <ol>
     *   <li>the setting {@code manaMethod.enabled} and the page's own setting (mobility, body, brush) must be on;</li>
     *   <li>ACCESS ({@link RuneAccess#allowed}): PASSIVE = the mastery of THIS grimoire reaches {@code passiveMastery}; TIMED = the caster's rune state is ON (the Rune State page itself is
     *       usable only in TIMED); MANA_ZONE_GATE = the 3.7.0 late-game gate;</li>
     *   <li>ELEMENT ({@link RuneOwnElement#allowed}): with {@code ownElementOnly} the Flame / Water / Wind / Earth pages need a grimoire of that element (its magic or a parent of its fusion).</li>
     * </ol>
     * The mastery and the element are those of the grimoire the page belongs to, so a player with two grimoires gets the pages each grimoire earns; both are in the instance the client
     * already has, so no packet is needed for them.
     */
    public static boolean usable(GrimoireBook b, ManasSkillInstance i, LivingEntity e, String pageId) {
        if (!ManaMethodConfig.enabled() || !pageSettingsOn(pageId)) return false;
        RuneAccess.Mode mode = ManaMethodConfig.accessMode();
        boolean statePage = STATE_ID.equals(pageId);
        if (mode == RuneAccess.Mode.PASSIVE && i == null && !statePage) return false;                  // a menu with no instance cannot know a mastery
        double mastery = mode == RuneAccess.Mode.PASSIVE && b != null && i != null && !statePage ? b.masteryFrac(i) : 0.0;
        boolean stateOn = mode == RuneAccess.Mode.TIMED && !statePage && RuneState.on(e);
        boolean gate = RuneAccess.needsGate(mode) && !statePage && ManaZoneManager.gateOpenFor(e);
        if (!RuneAccess.allowed(mode, statePage, mastery, ManaMethodConfig.passiveMastery(), stateOn, gate)) return false;
        return elementOk(i, pageId);
    }

    /** Whether the grimoire of {@code i} may write the elemental rune page {@code pageId} (true for every other page, and for everything when {@code ownElementOnly} is off). */
    public static boolean elementOk(ManasSkillInstance i, String pageId) {
        if (RuneOwnElement.pageElement(pageId) == null || !ManaMethodConfig.ownElementOnly()) return true;
        if (i == null) return false;
        MagicType magic = GrimoirePages.magicOf(i);
        return RuneOwnElement.allowed(true, pageId, magic == null ? null : magic.name(), i.getOrCreateTag().getString("Fusion"));
    }

    // ---------------------------------------------------------------- the price
    /**
     * 3.6.0: the share of the page's normal price the cast that is about to happen pays ({@link RuneCost}): the charge of the release that is waiting for its cast, peeked WITHOUT using it up. 1
     * when the setting {@code manaMethod.costScalesWithCharge} is off, hold-to-charge is off, nothing is waiting (the 3.4.0 cast), or for the Air Step Rune (no charge to speak of).
     */
    public static double costFactor(ServerPlayer p, String pageId, int mode) {
        if (!isPage(pageId) || AIR_STEP_ID.equals(pageId) || !ManaMethodConfig.costScalesWithCharge() || !RuneHold.active()) return 1.0;
        Charge c = RuneHold.peek(p.getUUID(), mode, p.level().getGameTime());
        return c == null ? 1.0 : RuneCost.fraction(c, ManaMethodConfig.minCostFraction());
    }

    /** The share of the normal price a cast with {@code charge} has been charged (what {@link #costFactor} gave when the grimoire asked). */
    static double paidFraction(Charge charge) {
        return charge == null || !ManaMethodConfig.costScalesWithCharge() ? 1.0 : RuneCost.fraction(charge, ManaMethodConfig.minCostFraction());
    }

    /**
     * 3.6.0: gives back what a REFUSED Mana Method cast was charged. The grimoire takes the price before the page's cast runs (the page cannot know it will be refused); a cast the page
     * refuses (runes still forming, server full, cooldown, nothing to paint on, ...) must not cost. A creative player paid nothing.
     */
    static void refund(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Charge charge) {
        if (p.isCreative()) return;
        try {
            double cost = b.getMagiculeCost(p, i, mode) * paidFraction(charge);
            if (cost > 0) io.github.manasmods.tensura.util.EnergyHelper.gainMagicule(p, cost, io.github.manasmods.tensura.util.EnergyHelper.GainType.NORMAL);
        } catch (Throwable ignored) { /* no Tensura data: nothing was taken */ }
    }

    private static boolean paid(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, Charge charge, boolean ok) {
        if (!ok) refund(b, i, p, mode, charge);
        return ok;
    }

    /** The element a Body or Brush Rune is drawn in: the natural element of the grimoire, else wind (the neutral one). */
    private static RuneElement canonElement(ManasSkillInstance i) {
        MagicType magic = GrimoirePages.magicOf(i);
        RuneElement e = RuneElement.ofMagic(magic == null ? null : magic.name());
        return e == null ? RuneElement.WIND : e;
    }

    // ---------------------------------------------------------------- the casts
    private static boolean castElement(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, RuneElement e) {
        Charge ch = chargeOf(p, mode);
        if (!elementOk(i, b.page(mode) == null ? null : b.page(mode).id())) {                  // 3.8.0: the grimoire has already asked usable(); this is the cast's own second look
            GrimoireBook.fail(p, "A " + e.title + " Rune is not written in your magic.");
            return paid(b, i, p, mode, ch, false);
        }
        int mods = RuneCharge.modsFor(ch, e, p.isShiftKeyDown());                  // a basic (tapped) spell never writes the second rune
        return begin(b, i, p, mode, new RuneSpec(e, mods, false), ch);
    }

    /** The charge of this cast: what the hold measured (hold-to-charge on and a release waiting), otherwise the 3.4.0 spell. */
    private static Charge chargeOf(ServerPlayer p, int mode) {
        if (!RuneHold.active()) return Charge.LEGACY;
        Charge c = RuneHold.take(p.getUUID(), mode, p.level().getGameTime());
        return c == null ? Charge.LEGACY : c;
    }

    /**
     * The rune a page would write for this player right now, without any message or side effect (null for a page that writes no rune, or a True Magic page the caster cannot use): the
     * hold packet draws it while the key is down.
     */
    public static RuneSpec specFor(String pageId, ManasSkillInstance i, ServerPlayer p, Charge charge) {
        if (pageId == null) return null;
        RuneElement e = switch (pageId) {
            case FIRE_ID -> RuneElement.FIRE;
            case WATER_ID -> RuneElement.WATER;
            case WIND_ID -> RuneElement.WIND;
            case EARTH_ID -> RuneElement.EARTH;
            case TRUE_ID -> trueElement(i, p);
            default -> null;
        };
        if (e == null) return null;
        return new RuneSpec(e, RuneCharge.modsFor(charge, e, p.isShiftKeyDown()), TRUE_ID.equals(pageId));
    }

    /** The element of the True Magic page for this caster (the natural attribute of the grimoire), or null when it cannot be cast. */
    private static RuneElement trueElement(ManasSkillInstance i, ServerPlayer p) {
        MagicType magic = GrimoirePages.magicOf(i);
        RuneElement e = RuneElement.ofMagic(magic == null ? null : magic.name());
        if (e == null || !TrueMagicRules.isNatural(magic.name())) return null;
        var stage = ManaLaw.stage(p);
        return TrueMagicRules.controlEnough(stage.number(), stage.arcane() || stage.saint()) ? e : null;
    }

    private static boolean castAirStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!ManaMethodConfig.mobility()) { GrimoireBook.fail(p, "The mobility runes are switched off on this server."); return paid(b, i, p, mode, null, false); }
        RuneHold.take(p.getUUID(), mode, p.level().getGameTime());                  // the hold does not matter for the platform; the waiting release is used up
        return paid(b, i, p, mode, null, AirStep.start(p, ManaMethodConfig.airStepSeconds(), p.level().getGameTime()));
    }

    private static boolean castDash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Charge ch = chargeOf(p, mode);
        if (!ManaMethodConfig.mobility()) { GrimoireBook.fail(p, "The mobility runes are switched off on this server."); return paid(b, i, p, mode, ch, false); }
        return paid(b, i, p, mode, ch, Dash.cast(p, ch, p.level().getGameTime()));
    }

    private static boolean castTrue(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Charge ch = chargeOf(p, mode);
        MagicType magic = GrimoirePages.magicOf(i);
        RuneElement e = RuneElement.ofMagic(magic == null ? null : magic.name());
        if (e == null || !TrueMagicRules.isNatural(magic.name())) {
            GrimoireBook.fail(p, "True Magic follows the shapes of nature: fire, water, wind, earth, lightning and plants. Your magic is too abstract to be drawn as a rune array.");
            return paid(b, i, p, mode, ch, false);
        }
        var stage = ManaLaw.stage(p);
        if (!TrueMagicRules.controlEnough(stage.number(), stage.arcane() || stage.saint())) {
            GrimoireBook.fail(p, "True Magic needs the control of Stage 3 or higher to hold the runes.");
            return paid(b, i, p, mode, ch, false);
        }
        int mods = RuneCharge.modsFor(ch, e, p.isShiftKeyDown());
        boolean ok = begin(b, i, p, mode, new RuneSpec(e, mods, true), ch);
        if (ok) RuneManager.refundFuel(b, i, p, mode, ManaLaw.density(p.level(), p.blockPosition()), paidFraction(ch));
        return ok;
    }

    private static boolean begin(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, RuneSpec spec, Charge charge) {
        RuneManager.Begin r = RuneManager.begin(b, i, p, mode, spec, p.getServer().getTickCount(), charge);
        switch (r) {
            case OK -> {
                p.displayClientMessage(Component.translatableWithFallback("nusmp.mana_method.cast", "Mana Method: %s (%s)", spec.element().title + (spec.trueMagic() ? " True Magic" : " Rune"), RuneMods.describe(spec.mods()) + (charge.legacy() ? "" : ", " + charge.stage().name().toLowerCase(java.util.Locale.ROOT))).withStyle(ChatFormatting.LIGHT_PURPLE), true);
                return true;
            }
            case CASTER_FULL -> GrimoireBook.fail(p, "Your runes are still forming (" + ManaMethodConfig.perCaster() + " at most).");
            case SERVER_FULL -> GrimoireBook.fail(p, "The mana of this world is busy with other runes. Try again in a moment.");
            default -> GrimoireBook.fail(p, "The Mana Method is switched off on this server.");
        }
        refund(b, i, p, mode, charge);                                                      // 3.6.0: a refused cast gives its price back
        return false;
    }

    // ---------------------------------------------------------------- 3.8.0: the Rune State page
    private static boolean castState(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        RuneHold.take(p.getUUID(), mode, p.level().getGameTime());                          // there is nothing to charge; a waiting release is used up
        if (ManaMethodConfig.accessMode() != RuneAccess.Mode.TIMED) { GrimoireBook.fail(p, "The runes are not switched on and off on this server."); return false; }
        return RuneState.cast(p);
    }

    // ---------------------------------------------------------------- 3.6.0: the two canon pages
    private static boolean castBody(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Charge ch = chargeOf(p, mode);
        if (!ManaMethodConfig.bodyRunes()) { GrimoireBook.fail(p, "The Body Rune is switched off on this server."); return paid(b, i, p, mode, ch, false); }
        RuneElement e = canonElement(i);
        return paid(b, i, p, mode, ch, BodyRunes.start(p, new RuneSpec(e, e.defaultMods(), false), ch, p.level().getGameTime()));
    }

    private static boolean castBrush(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Charge ch = chargeOf(p, mode);
        if (!ManaMethodConfig.brushRunes()) { GrimoireBook.fail(p, "The Brush Rune is switched off on this server."); return paid(b, i, p, mode, ch, false); }
        RuneElement e = canonElement(i);
        RuneSpec spec = new RuneSpec(e, RuneCharge.modsFor(ch, e, p.isShiftKeyDown()), false);
        return paid(b, i, p, mode, ch, BrushRunes.paint(b, i, p, mode, spec, ch, p.level().getGameTime()));
    }
}
