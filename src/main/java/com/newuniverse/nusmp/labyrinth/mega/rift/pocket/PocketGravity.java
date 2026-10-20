package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: the canon gravity of the ancient tombs ("rivers flowing on ceilings, areas where gravity is negated") needs the player to really fall up in some rooms and drift in others. The 1.21 player
 * has a {@code GRAVITY} attribute (0.08 normally, allowed from -1 to 1, synced to the client, which is the side that moves a player), so a zone is a TRANSIENT attribute modifier: -0.14 turns
 * 0.08 into -0.06 (a gentle rise to the ceiling, where the delver walks), +0.02 makes a room weightless. A transient modifier is never saved, so a relog, a death or a crash cannot leave anyone upside
 * down; the tomb removes it the moment the delver is outside every zone, outside the tomb, dead or logged out ({@link #clear}), and a second modifier on {@code SAFE_FALL_DISTANCE} forgives the drop
 * from the ceiling of a tall corridor. The arithmetic is {@link PocketRules#gravity}. The mobs are not touched: the rooms with a gravity rule are filled with fliers only.
 */
public final class PocketGravity {
    private PocketGravity() {}

    public static final ResourceLocation GRAVITY_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "pocket_gravity");
    public static final ResourceLocation CUSHION_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "pocket_cushion");

    /** Puts the pocket's gravity on a player: the zone's (null = none) in a pocket of the given base gravity, and the cushion for falls. */
    public static void apply(ServerPlayer p, PocketLayout.Kind zone, double base, boolean gravityOn) {
        AttributeInstance g = p.getAttribute(Attributes.GRAVITY);
        if (g != null) {
            double want = gravityOn ? PocketRules.gravityModifier(PocketRules.gravity(zone, base)) : 0.0;
            AttributeModifier cur = g.getModifier(GRAVITY_ID);
            if (Math.abs(want) < 1e-9) {
                if (cur != null) g.removeModifier(GRAVITY_ID);
            } else if (cur == null || Math.abs(cur.amount() - want) > 1e-9) {
                g.addOrUpdateTransientModifier(new AttributeModifier(GRAVITY_ID, want, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        AttributeInstance f = p.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        if (f != null && f.getModifier(CUSHION_ID) == null) f.addOrUpdateTransientModifier(new AttributeModifier(CUSHION_ID, PocketRules.SAFE_FALL, AttributeModifier.Operation.ADD_VALUE));
    }

    /** Takes both modifiers off. Safe to call for anybody at any time. */
    public static void clear(ServerPlayer p) {
        AttributeInstance g = p.getAttribute(Attributes.GRAVITY);
        if (g != null && g.getModifier(GRAVITY_ID) != null) g.removeModifier(GRAVITY_ID);
        AttributeInstance f = p.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        if (f != null && f.getModifier(CUSHION_ID) != null) f.removeModifier(CUSHION_ID);
    }

    /** Does this player carry one of the pocket's modifiers? */
    public static boolean marked(ServerPlayer p) {
        AttributeInstance g = p.getAttribute(Attributes.GRAVITY);
        if (g != null && g.getModifier(GRAVITY_ID) != null) return true;
        AttributeInstance f = p.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        return f != null && f.getModifier(CUSHION_ID) != null;
    }
}
