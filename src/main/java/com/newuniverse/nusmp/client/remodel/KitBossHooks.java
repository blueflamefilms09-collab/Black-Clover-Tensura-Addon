package com.newuniverse.nusmp.client.remodel;

import com.newuniverse.nusmp.client.zogratis.ZogratisGlow;
import com.newuniverse.nusmp.client.zogratis.ZogratisPose;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathBoss;
import com.newuniverse.nusmp.entity.avatar.DevilAvatarBoss;
import com.newuniverse.nusmp.entity.zogratis.LuciusBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.relics.order.GodOfOrder;
import com.newuniverse.nusmp.relics.order.OrderPlan;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Why: the few things a framework boss does with its look that are its own, as {@link KitBossRenderer.Hooks}, one small class each, carried over from the old renderers so no boss loses a
 * behaviour in the remodel: Lucius floats in his divine form and glows white-blue while Julius holds the body; the Devil Avatar wears the Megicula skin when it is the Avatar of Megicula;
 * the Goliath rises out of the floor (its fists break through first); the God of Order steps out of its column and plays the visit's clips (read, beam, bow) from the synced phase.
 */
final class KitBossHooks {
    private KitBossHooks() {}

    /** Lucius's Julius tint of the glow: the Wizard King's white-blue. */
    static final int JULIUS_RGB = 0xCFE6FF;

    /** The divine Lucius floats (ramping up over the transformation); while Julius holds the body the glow is the Wizard King's white-blue. */
    static final KitBossRenderer.Hooks<LuciusBoss> LUCIUS = new KitBossRenderer.Hooks<>() {
        @Override
        public float lift(LuciusBoss e, boolean transforming, float phase, int form) {
            return e.hover() * ZogratisGlow.hoverRamp(transforming, phase, form == ZogratisBoss.FORM_DIVINE);
        }

        @Override
        public int glowColor(LuciusBoss e, float level) { return e.julius() ? ZogratisGlow.scaled(JULIUS_RGB, level) : 0; }
    };

    /** The Avatar of Megicula is crimson (the second form's skin is named {@code megicula_form2}); the Avatar of Lucifero is the plain violet one. */
    static final KitBossRenderer.Hooks<DevilAvatarBoss> AVATAR = new KitBossRenderer.Hooks<>() {
        @Override
        public String skin(DevilAvatarBoss e, boolean second) { return e.megicula() ? (second ? "megicula_form2" : "megicula") : null; }
    };

    /** The Goliath rises out of the floor: the model is sunk by the eased rise value and plays its spawn clip at that time, so the fists break through first. */
    static final KitBossRenderer.Hooks<GoliathBoss> GOLIATH = new KitBossRenderer.Hooks<>() {
        @Override
        public float rise(GoliathBoss e, float partial) { return e.rise(partial); }
    };

    /**
     * The God of Order's visit: it steps out of its column (grows to full size in 14 ticks, eased), and from the synced phase of the visit plays {@code read} (arms open while it reads its
     * verdict), {@code beam} (the staff levelled at the holder) or {@code bow} (when it leaves). The clips are made in tools/arc/char_god_of_order.py.
     */
    static final class Order implements KitBossRenderer.Hooks<GodOfOrder> {
        private static final float APPEAR_TICKS = 14f;
        /** When each body entered its current phase (client age), to pace the one-shot bow. */
        private final Map<Entity, float[]> phaseStart = new WeakHashMap<>();

        @Override
        public float appear(GodOfOrder e, float partial) {
            float t = Mth.clamp((e.tickCount + partial) / APPEAR_TICKS, 0.02f, 1f);
            return t * t * (3f - 2f * t);
        }

        @Override
        public int clip(GodOfOrder e, ZogratisPose clock, ZogratisPose.Frame f, ZogratisPose.Clips clips) {
            OrderPlan.Phase visit = e.orderPhase();
            float[] since = phaseStart.computeIfAbsent(e, k -> new float[]{-1f, f.age});
            if (since[0] != visit.ordinal()) {
                since[0] = visit.ordinal();
                since[1] = f.age;
            }
            if (visit == OrderPlan.Phase.READING && clips.length("read") > 0f) {
                clock.clip = "read";
                clock.seconds = (f.age - since[1]) / 20f;
                return 1;
            }
            if (visit == OrderPlan.Phase.BEAM && clips.length("beam") > 0f) {
                clock.clip = "beam";
                clock.seconds = (f.age - since[1]) / 20f;
                return 1;
            }
            if (visit == OrderPlan.Phase.BOW && clips.length("bow") > 0f && e.targetId() < 0) {
                clock.clip = "bow";
                clock.seconds = Math.min(clips.length("bow"), (f.age - since[1]) / 20f);
                return 2;
            }
            return 0;
        }
    }
}
