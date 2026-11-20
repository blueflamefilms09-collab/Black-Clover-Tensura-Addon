package com.newuniverse.nusmp.skill.rules;

import com.newuniverse.nusmp.heroweapon.HeroClass;
import com.newuniverse.nusmp.heroweapon.HeroKind;

import java.util.ArrayList;
import java.util.List;

/**
 * Weapon skills (docs/weapon_skills.md): the Vassal weapons, the Djinn and the Beast Wills as normal skills of the skill list and the skill wheel, so a player needs none of the separate keys. This file is the pure table
 * (no Minecraft class): which skills exist, the modes of each wheel and the ONE server action each mode stands for ({@link Act}). The skill class ({@code skill.WeaponSkill}) turns an {@link Act} into the call the old key's
 * packet makes; its switch over {@link Act} has no default, so a mode without an action does not compile. The old keys and packets are untouched.
 */
public final class WeaponSkillRules {
    private WeaponSkillRules() {}

    /** The three skill families. */
    public enum Kind { HERO, DJINN, BEAST }

    /**
     * The server action of one wheel mode, named after the key it replaces. HERO: the three technique keys (Z, X, C: a press), summon / put away (B), change form (N). DJINN: go to a phase (J raises one step; here each
     * phase is a mode), step down (K), dismiss (Shift + J), draw Rukh (H). BEAST: acquire form ([), integrate form (]), ability 1 (;), ability 2 (\), swap the will (=, and Shift + = backwards).
     */
    public enum Act {
        H_TECH_1, H_TECH_2, H_TECH_3, H_TOGGLE, H_CYCLE,
        D_LESSER, D_HALF, D_FULL, D_EXTREME, D_DOWN, D_OFF, D_RUKH,
        B_ACQUIRE, B_INTEGRATE, B_ABILITY_1, B_ABILITY_2, B_SWAP_NEXT, B_SWAP_PREV
    }

    /** One wheel mode: its id (unique over all three tables; the lang key is {@code anime_multiverse.skill.mode.weapon.<id>}), the English name, the one action it runs and what it does. */
    public record Mode(String id, String name, Act act, String blurb) {}

    /** The wheel of every Vassal weapon skill (the same five modes; what a technique IS depends on the form the weapon is in). */
    public static final List<Mode> HERO = List.of(
            new Mode("hero_tech_1", "Technique 1", Act.H_TECH_1, "the first technique of the active form (the Z key)"),
            new Mode("hero_tech_2", "Technique 2", Act.H_TECH_2, "the second technique of the active form (the X key)"),
            new Mode("hero_tech_3", "Technique 3", Act.H_TECH_3, "the third technique of the active form (the C key)"),
            new Mode("hero_toggle", "Summon / Put Away", Act.H_TOGGLE, "calls the weapon into your hand or puts it away (the B key)"),
            new Mode("hero_cycle", "Change Form", Act.H_CYCLE, "changes to the next learned form (the N key)"));

    /** The wheel of the Djinn skill: the phases of the Djinn Equip, then the Rukh. */
    public static final List<Mode> DJINN = List.of(
            new Mode("djinn_lesser", "Lesser Summon", Act.D_LESSER, "calls the Djinn as a small manifestation (phase 1)"),
            new Mode("djinn_half", "Half Equip", Act.D_HALF, "the Djinn's weapon and its element in your blows (phase 2)"),
            new Mode("djinn_full", "Full Equip", Act.D_FULL, "the Djinn's Maso, the full body of it (phase 3); while an Extreme Magic charges it lets go of it"),
            new Mode("djinn_extreme", "Extreme Magic", Act.D_EXTREME, "from a Full Equip, the Djinn's own Extreme Magic (phase 4)"),
            new Mode("djinn_down", "Step Down", Act.D_DOWN, "lowers the Djinn one phase (the K key)"),
            new Mode("djinn_off", "Dismiss", Act.D_OFF, "sends the Djinn back at once (Shift + the J key)"),
            new Mode("djinn_rukh", "Draw Rukh", Act.D_RUKH, "stand still with the vessel on you and draw the Rukh of the place into your magicules; use again to stop (the H key)"));

    /** The wheel of the Beast Will skill. */
    public static final List<Mode> BEAST = List.of(
            new Mode("beast_acquire", "Acquire Form", Act.B_ACQUIRE, "starts or ends the Acquire form of the active will (the [ key)"),
            new Mode("beast_integrate", "Integrate Form", Act.B_INTEGRATE, "starts or ends the Integrate form of the active will (the ] key)"),
            new Mode("beast_ability_1", "Ability 1", Act.B_ABILITY_1, "the first ability of the form you are in (the ; key)"),
            new Mode("beast_ability_2", "Ability 2", Act.B_ABILITY_2, "the second ability of the form you are in (the \\ key)"),
            new Mode("beast_swap", "Swap Will", Act.B_SWAP_NEXT, "makes the next bound will the active one (the = key)"),
            new Mode("beast_swap_back", "Swap Will Back", Act.B_SWAP_PREV, "makes the previous bound will the active one (Shift + the = key)"));

    /** The id prefix of every weapon skill: {@code ws_vassal_fan}, {@code ws_djinn}, {@code ws_beast_will}. */
    public static final String PREFIX = "ws_";
    public static final String DJINN_ID = PREFIX + "djinn", BEAST_ID = PREFIX + "beast_will";

    /** The wheel of a family. */
    public static List<Mode> modes(Kind kind) {
        return switch (kind) {
            case HERO -> HERO;
            case DJINN -> DJINN;
            case BEAST -> BEAST;
        };
    }

    /** The weapons that get a skill: the ten Vassal Weapons (carriage, fan, scythe, katana, book, mirror, instrument, harpoon, sewing set, chain). The Seven Star and the four Holy weapons are not in this release. */
    public static boolean coversHero(HeroKind kind) { return kind != null && kind.heroClass() == HeroClass.VASSAL; }

    /** The covered weapons in catalogue order. */
    public static List<HeroKind> heroKinds() {
        List<HeroKind> out = new ArrayList<>();
        for (HeroKind k : HeroKind.values()) if (coversHero(k)) out.add(k);
        return out;
    }

    /** The registry path of a weapon's skill ({@code ws_vassal_fan}). */
    public static String heroId(HeroKind kind) { return PREFIX + kind.itemId(); }

    /** Every skill id, in registration order: the ten weapons, the Djinn, the Beast Will. */
    public static List<String> allIds() {
        List<String> out = new ArrayList<>();
        for (HeroKind k : heroKinds()) out.add(heroId(k));
        out.add(DJINN_ID);
        out.add(BEAST_ID);
        return out;
    }

    /** The English name of a weapon's skill. */
    public static String heroName(HeroKind kind) { return kind.display() + " Arts"; }

    /** Why a press does or does not go through. */
    public enum Verdict { OK, SWITCHED_OFF, NOT_OWNED }

    /** The check of every press, first the server setting, then the bond: the skill works only for the owner of the weapon, the Djinn or the will. */
    public static Verdict verdict(boolean settingOn, boolean owned) {
        if (!settingOn) return Verdict.SWITCHED_OFF;
        return owned ? Verdict.OK : Verdict.NOT_OWNED;
    }

    /** At most one cast per tick and player: true when {@code now} is a later tick than the last cast (a clock that went back counts as due). */
    public static boolean due(long now, long lastCast) { return now != lastCast; }

    /** Worst case of this feature, for the doc and the self-test: one server action per player per tick, and at most this many skills a player holds (ten weapons cannot all be bound: one bond, one Djinn, one will family). */
    public static final int MAX_HELD_SKILLS = 3;
}
