package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;

import java.util.List;

/**
 * Why: every system needs the forms of a weapon and the form a bond is in, and none of them should know where the forms come from. This is the door; the
 * forms builder replaces the whole class with the real one (the datapack loader of {@code data/nusmp/hero/forms/<itemId>.json}, the learned and available
 * forms). Until then it answers with a synthetic base form for every weapon so the rest compiles and a bond has a form. The contract of the real class: the
 * lists are never empty (the base form is first), lookups never return null for a bound bond, and nothing here throws.
 */
public final class HeroForms {
    private HeroForms() {}

    /** All forms of a weapon, the base form first, then by tier. Never empty. */
    public static List<HeroForm> of(HeroKind kind) { return List.of(HeroForm.base(kind)); }

    /** The form with this id, or null. */
    public static HeroForm get(HeroKind kind, String id) {
        for (HeroForm f : of(kind)) if (f.id().equals(id)) return f;
        return null;
    }

    /** The bond's active form; the base form when the id is unknown. Never null for a bound bond. */
    public static HeroForm active(HeroBond bond) {
        HeroForm f = get(bond.kind(), bond.form());
        return f != null ? f : of(bond.kind()).get(0);
    }
}
