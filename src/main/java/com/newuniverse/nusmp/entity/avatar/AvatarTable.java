package com.newuniverse.nusmp.entity.avatar;

import com.newuniverse.nusmp.entity.zogratis.MoveSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * The Devil Avatar's move table (pure, unit tested): every move's numbers in one place, as {@link MoveSpec}s the scheduler can read without
 * Minecraft. {@code DevilAvatarBoss} turns each entry into a {@link com.newuniverse.nusmp.entity.zogratis.Move} by adding its telegraph and
 * its code, looked up by id.
 * <p>
 * The Avatar is the floor boss of the Underworld Gate: a high devil that bends the gravity of the room (it pulls everyone up to the
 * ceiling, slams them down, flips the pull of a field) and throws fast curses (homing hex bolts that brand, a dread brand on the foe
 * farthest from it, a curse wave). Both variants, the Avatar of Lucifero (violet, gravity-leaning) and the Avatar of Megicula (crimson,
 * curse-leaning), have the same moves in the same order (the client reads move names by index); only the weights differ.
 * Abyssal Collapse is the only ultimate and needs 60% power.
 */
public final class AvatarTable {
    private AvatarTable() {}

    /** The two variants (the synched value of the entity). */
    public static final int LUCIFERO = 0, MEGICULA = 1;

    public static final String VOID_CLAWS = "void_claws", HEX_VOLLEY = "hex_volley", SHADOW_STEP = "shadow_step", CEILING_PULL = "ceiling_pull",
            FLOOR_CRUSH = "floor_crush", DREAD_BRAND = "dread_brand", INVERSION_FIELD = "inversion_field", CURSE_NOVA = "curse_nova",
            CALL_DISCIPLES = "call_disciples", ABYSSAL_COLLAPSE = "abyssal_collapse",
            /** 1.35.0: spokes of brimstone from the Avatar (two waves at the Gates), from 60% power. */
            BRIMSTONE_ERUPTION = "brimstone_eruption";

    /** Base damage of the moves (before the boss's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float CLAWS = 15f, BOLT = 3.5f, CRUSH = 11f, SHADOW = 12f, NOVA = 11f, ULT_CRUSH = 18f, ULT_NOVA = 12f, ERUPT = 13f;

    /** The Avatar's build (1.35.0: 900 health -> 1100, a little more armour and toughness; the damage still goes through the balance law and the 6%/s budget). */
    public static final double HEALTH = 1100, ATTACK = 15, ARMOR = 9, TOUGHNESS = 5;

    /** The ticks after the Gates open at which a wave of Dark Disciples steps out (1.35.0: a fourth wave, late in the fight). */
    public static final int[] GATE_WAVES = {80, 260, 460, 640};

    /** One row of the table. The record's accessors are the {@link MoveSpec} methods. */
    public record Spec(String id, String label, int minPower, int windUpTicks, int cooldownTicks, double minRange, double maxRange,
                       int weight, boolean ultimate, boolean crowd, int recovery) implements MoveSpec {
        /** The mana an ordinary move costs below 60% power (the same rule as the framework's Move): 6 + a sixteenth of its cooldown, at most 32; 60 for an ultimate. */
        @Override public double manaCost() { return ultimate ? 60 : Math.min(32, 6 + cooldownTicks / 16.0); }
    }

    /** Moves that bend gravity (the Lucifero Avatar favours them) and moves that curse (the Megicula Avatar favours them). */
    public static boolean isGravity(String id) { return CEILING_PULL.equals(id) || FLOOR_CRUSH.equals(id) || INVERSION_FIELD.equals(id); }

    public static boolean isCurse(String id) { return HEX_VOLLEY.equals(id) || DREAD_BRAND.equals(id) || CURSE_NOVA.equals(id); }

    /** The weight of a move for a variant: the base weight, 1.5 times as much for the move kind the variant leans on, 0.75 times for the other kind. */
    public static int weight(int variant, String id, int base) {
        double k = 1.0;
        if (isGravity(id)) k = variant == MEGICULA ? 0.75 : 1.5;
        else if (isCurse(id)) k = variant == MEGICULA ? 1.5 : 0.75;
        return Math.max(1, (int) Math.round(base * k));
    }

    private static Spec row(int variant, String id, String label, int minPower, int windUp, int cooldown, double minRange, double maxRange, int baseWeight,
                            boolean ultimate, boolean crowd, int recovery) {
        return new Spec(id, label, minPower, windUp, cooldown, minRange, maxRange, weight(variant, id, baseWeight), ultimate, crowd, recovery);
    }

    private static final List<Spec> LUCIFERO_SPECS = build(LUCIFERO), MEGICULA_SPECS = build(MEGICULA);

    private static List<Spec> build(int v) {
        List<Spec> out = new ArrayList<>();
        out.add(row(v, VOID_CLAWS, "Void Claws", 20, 14, 70, 0, 6.5, 6, false, false, 10));
        out.add(row(v, HEX_VOLLEY, "Hex Volley", 20, 18, 110, 5, 34, 6, false, false, 8));
        out.add(row(v, SHADOW_STEP, "Shadow Step", 20, 16, 150, 6, 26, 4, false, false, 10));
        out.add(row(v, CEILING_PULL, "Ceiling Pull", 20, 20, 300, 4, 30, 4, false, true, 14));
        out.add(row(v, FLOOR_CRUSH, "Floor Crush", 20, 24, 230, 0, 26, 4, false, true, 12));
        out.add(row(v, DREAD_BRAND, "Dread Brand", 20, 24, 320, 6, 36, 3, false, false, 10));
        out.add(row(v, INVERSION_FIELD, "Inversion Field", 40, 24, 360, 4, 30, 3, false, true, 12));
        out.add(row(v, CURSE_NOVA, "Curse Nova", 40, 22, 240, 0, 10, 3, false, true, 12));
        out.add(row(v, CALL_DISCIPLES, "Call the Disciples", 20, 30, 700, 0, 48, 2, false, false, 10));
        out.add(row(v, BRIMSTONE_ERUPTION, "Brimstone Eruption", 60, 24, 200, 0, 22, 3, false, true, 12));
        out.add(row(v, ABYSSAL_COLLAPSE, "Abyssal Collapse", 60, 56, 1300, 0, 40, 5, true, false, 60));
        return List.copyOf(out);
    }

    /** The table of a variant, in the order of the boss's move list (the client reads move names by index). */
    public static List<Spec> specs(int variant) { return variant == MEGICULA ? MEGICULA_SPECS : LUCIFERO_SPECS; }

    /** The table of the Lucifero Avatar. */
    public static List<Spec> specs() { return LUCIFERO_SPECS; }

    public static Spec byId(String id) { return byId(LUCIFERO, id); }

    public static Spec byId(int variant, String id) {
        for (Spec s : specs(variant)) if (s.id().equals(id)) return s;
        return null;
    }

    /** The variant a value stands for (anything but Megicula's is Lucifero's). */
    public static int variantOf(int raw) { return raw == MEGICULA ? MEGICULA : LUCIFERO; }

    /** The display name: "Avatar of Lucifero" or "Avatar of Megicula". */
    public static String nameOf(int variant) { return variant == MEGICULA ? "Avatar of Megicula" : "Avatar of Lucifero"; }
}
