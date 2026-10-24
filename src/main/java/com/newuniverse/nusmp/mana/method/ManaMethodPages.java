package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.mana.zone.ManaZoneManager;
import com.newuniverse.nusmp.mana.zone.TrueMagicRules;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
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
 *   <tr><td>mana_method_fire</td><td>Flame Rune</td><td>mid, late game</td><td>10 % (floor 300)</td><td>8 s</td><td>grows</td></tr>
 *   <tr><td>mana_method_water</td><td>Water Rune</td><td>mid, late game</td><td>10 %</td><td>8 s</td><td>pursues</td></tr>
 *   <tr><td>mana_method_wind</td><td>Wind Rune</td><td>mid, late game</td><td>10 %</td><td>8 s</td><td>pursues</td></tr>
 *   <tr><td>mana_method_earth</td><td>Earth Rune</td><td>mid, late game</td><td>10 %</td><td>8 s</td><td>grows</td></tr>
 *   <tr><td>mana_method_true</td><td>True Magic Rune</td><td>zone, late game</td><td>16 % (floor 700)</td><td>20 s</td><td>grows (your natural element)</td></tr>
 * </table>
 * Casting while sneaking writes BOTH runes (pursue and grow), at 85 % of the power and 4 more ticks of charge. The ids contain no attack word, so an Avatar never casts them
 * (AvatarSkillList), and the Mana Method pages are not family pages, so the "15 spells" pages15 machinery and the armour policy do not see them.
 */
public final class ManaMethodPages {
    private ManaMethodPages() {}

    public static final String FIRE_ID = "mana_method_fire", WATER_ID = "mana_method_water", WIND_ID = "mana_method_wind", EARTH_ID = "mana_method_earth", TRUE_ID = "mana_method_true";
    private static final List<String> IDS = List.of(FIRE_ID, WATER_ID, WIND_ID, EARTH_ID, TRUE_ID);

    public static List<String> ids() { return IDS; }

    public static boolean isPage(String id) { return id != null && IDS.contains(id); }

    /** How many of the last pages of a book are Mana Method pages: all five, or none (an older list). The pinned "the last pages are the spirit pages" checks use it to look past them. */
    public static int tailCount(List<BookPage> pages) {
        int n = pages.size(), k = IDS.size();
        if (n < k) return 0;
        for (int i = 0; i < k; i++) if (!pages.get(n - k + i).id().equals(IDS.get(i))) return 0;
        return k;
    }

    /** The pages as the grimoire appends them (always present, so the mode numbers never depend on the setting; {@link #usable} hides them). */
    public static List<BookPage> pages() {
        return List.of(
                element(FIRE_ID, "Flame Rune", RuneElement.FIRE),
                element(WATER_ID, "Water Rune", RuneElement.WATER),
                element(WIND_ID, "Wind Rune", RuneElement.WIND),
                element(EARTH_ID, "Earth Rune", RuneElement.EARTH),
                new BookPage(TRUE_ID, "True Magic Rune", "Mana Method: True Magic", RuneRules.COST_PERCENT_TRUE, RuneRules.COST_FLOOR_TRUE, RuneRules.COOLDOWN_TRUE, ManaMethodPages::castTrue));
    }

    private static BookPage element(String id, String name, RuneElement e) {
        return new BookPage(id, name, "Mana Method: " + e.title, RuneRules.COST_PERCENT, RuneRules.COST_FLOOR, RuneRules.COOLDOWN,
                (b, i, p, m) -> castElement(b, i, p, m, e));
    }

    /** Usable = the setting is on and the late-game gate of the Mana Zone is open for this entity. */
    public static boolean usable(LivingEntity e) { return ManaMethodConfig.enabled() && ManaZoneManager.gateOpenFor(e); }

    // ---------------------------------------------------------------- the casts
    private static boolean castElement(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, RuneElement e) {
        int mods = p.isShiftKeyDown() ? RuneMods.ALL : e.defaultMods();
        return begin(b, i, p, mode, new RuneSpec(e, mods, false));
    }

    private static boolean castTrue(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        MagicType magic = GrimoirePages.magicOf(i);
        RuneElement e = RuneElement.ofMagic(magic == null ? null : magic.name());
        if (e == null || !TrueMagicRules.isNatural(magic.name())) {
            GrimoireBook.fail(p, "True Magic follows the shapes of nature: fire, water, wind, earth, lightning and plants. Your magic is too abstract to be drawn as a rune array.");
            return false;
        }
        var stage = ManaLaw.stage(p);
        if (!TrueMagicRules.controlEnough(stage.number(), stage.arcane() || stage.saint())) {
            GrimoireBook.fail(p, "True Magic needs the control of Stage 3 or higher to hold the runes.");
            return false;
        }
        int mods = p.isShiftKeyDown() ? RuneMods.ALL : e.defaultMods();
        boolean ok = begin(b, i, p, mode, new RuneSpec(e, mods, true));
        if (ok) RuneManager.refundFuel(b, i, p, mode, ManaLaw.density(p.level(), p.blockPosition()));
        return ok;
    }

    private static boolean begin(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode, RuneSpec spec) {
        RuneManager.Begin r = RuneManager.begin(b, i, p, mode, spec, p.getServer().getTickCount());
        switch (r) {
            case OK -> { return true; }
            case CASTER_FULL -> GrimoireBook.fail(p, "Your runes are still forming (" + ManaMethodConfig.perCaster() + " at most).");
            case SERVER_FULL -> GrimoireBook.fail(p, "The mana of this world is busy with other runes. Try again in a moment.");
            default -> GrimoireBook.fail(p, "The Mana Method is switched off on this server.");
        }
        return false;
    }
}
