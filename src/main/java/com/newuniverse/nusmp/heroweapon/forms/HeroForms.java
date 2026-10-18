package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import net.minecraft.server.level.ServerPlayer;

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

    // ---- the calls the CORE makes (the forms builder implements them for real; the stubs refuse or do nothing)

    /** Makes a learned form the active one; false when refused (unknown, not learned, a curse form without its series, the curse lock, the 2 s technique lock). Keeps the hand in step ({@link #refresh}). */
    public static boolean setActive(ServerPlayer player, String formId) { return false; }

    /** The hero key "cycle form": the next learned form after the active one (curse forms only when allowed); returns the new active form id, or null when there is nothing to change to. */
    public static String cycle(ServerPlayer player) { return null; }

    /**
     * Re-applies everything the active form puts on the wielder: the attribute modifiers while the weapon is summoned (and none when it is not), the permanent ability bonuses of finished forms
     * (always), and the {@code nusmp:hero_look} component of the summoned stack. The core calls it on summon, dismiss, login, respawn and after any change of form or bond.
     */
    public static void refresh(ServerPlayer player) {}

    /**
     * The sneak + use route of absorption: the stack in the OFF hand is absorbed into the gem when some form of the bonded weapon asks for the item (or the config lets the gem take anything).
     * Returns true when something was absorbed (the core then does not fire the sneak technique); false otherwise.
     */
    public static boolean absorbOffhand(ServerPlayer player) { return false; }
}
