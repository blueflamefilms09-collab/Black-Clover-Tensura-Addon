package com.newuniverse.nusmp.heroweapon.curse;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.Sin;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner's rule is that a weapon offers the sin that is its own: the Holy Weapons the sins canon ties to them (the Shield Wrath, the Sword Greed and
 * Gluttony, the Spear Lust, the Bow Pride), and a Vassal or Seven Star weapon only the series its builder gave it by writing curse forms into its forms file
 * (the Mirror Envy, the Chain Sloth, and any further one a builder justified). An offer of a sin with no curse form behind it would take the wielder's hearts and
 * give nothing, so a sin is a candidate only when the weapon's forms really hold a curse form of it. This is that list, in the order the weapon prefers (canon
 * sins first, in the enum's order, then the file's other sins in file order), and the choice of the form the Embrace teaches (the lowest tier of that sin, the
 * first of equals). Pure: it reads the {@link HeroForm} records, which have no game types.
 */
public final class CurseCandidates {
    private CurseCandidates() {}

    /** The sins this weapon can offer, in the order it prefers; empty when its forms hold no curse form (then the weapon never whispers). */
    public static List<Sin> of(HeroKind kind, List<HeroForm> forms) {
        List<Sin> out = new ArrayList<>();
        if (kind == null || forms == null) return out;
        for (Sin sin : kind.sins()) if (hasForm(forms, sin) && !out.contains(sin)) out.add(sin);
        for (HeroForm f : forms) if (f != null && f.curse() && f.sin() != null && !out.contains(f.sin())) out.add(f.sin());
        return out;
    }

    /** True when the list holds a curse form of this sin. */
    public static boolean hasForm(List<HeroForm> forms, Sin sin) { return firstForm(forms, sin) != null; }

    /** The curse form the Embrace of this sin teaches: the lowest tier among the curse forms of the sin, the earlier one when tiers are equal; null when none. */
    public static HeroForm firstForm(List<HeroForm> forms, Sin sin) {
        if (forms == null || sin == null) return null;
        HeroForm best = null;
        for (HeroForm f : forms) {
            if (f == null || !f.curse() || f.sin() != sin) continue;
            if (best == null || f.tier() < best.tier()) best = f;
        }
        return best;
    }

    /** The sin of the curse form with this id in the list, or null (not a curse form, or unknown). */
    public static Sin sinOfForm(List<HeroForm> forms, String formId) {
        if (forms == null || formId == null) return null;
        for (HeroForm f : forms) if (f != null && f.curse() && f.id().equals(formId)) return f.sin();
        return null;
    }
}
