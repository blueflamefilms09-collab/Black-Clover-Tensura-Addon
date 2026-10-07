package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import com.newuniverse.nusmp.prop.PropKind;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * 0.54 Key Magic: how the two props look (both are GeoLite models, see tools/gen_key_geo.py).
 * KEY_1, the Gate of Keys (param & 3 = element: 0 fire, 1 ice, 2 lightning, 3 wind):
 *   1. a rune sigil on the ground and a counter-turning ring: additive (RenderType.eyes), full bright, flat, 0.04 over the ground so nothing z-fights;
 *   2. the gate model rising out of the ground, cutout (entityCutoutNoCull) with its glow map additive on top (GeoDraw's own second pass);
 *   3. the light of the opened doorway: one upright additive quad (key_door) 0.03 behind the door plane, drawn on both sides;
 *   4. a glint at the keyhole that flashes with every bolt: additive camera-facing sprite.
 * KEY_2, a Great Key (param bits 0-1 slot, 8-15 stab tick, set by KeyProps):
 *   1. the key model, cutout + glow, pitched at its foe while it stabs;
 *   2. a glint on the bow's gem (additive) and, during the thrust, a streak ribbon behind the tip and a glint on the tip (additive).
 * Nothing here allocates per frame beyond the vertex data; every texture exists (particle/key_*.png from gen_key_textures.py, entity/key_* from gen_key_geo.py).
 */
public final class KeyPropPainter {
    private KeyPropPainter() {}

    private static final GeoSpec GATE = GeoSpec.of("key", "gate"), KEY = GeoSpec.of("key", "great_key");
    private static final int FB = PropDraw.FULL_BRIGHT, GOLD = 0xFFD070, VIOLET = 0xB080FF, WHITE = 0xFFFFFF;
    /** Element glow colours: fire, ice, lightning, wind. */
    private static final int[] ELEMENT = {0xFF8A3C, 0x8FE8FF, 0xFFF070, 0xB8FFD0};
    /** The gate's timeline, the same numbers as prop.KeyProps (ticks): doors open at 10, a bolt every 10 from 16, closing for the last 14. */
    private static final int GATE_OPEN_AT = 10, GATE_FIRE_EVERY = 10, GATE_CLOSE = 14;
    private static ResourceLocation sigil, ring, door, glint, streak;

    /** Called once on the client by PropPainterRegistry. */
    public static void register() {
        sigil = particle("key_sigil");
        ring = particle("key_ring");
        door = particle("key_door");
        glint = particle("key_glint");
        streak = particle("key_streak");
        PropPainters.register(PropKind.KEY_1, KeyPropPainter::gate);
        PropPainters.register(PropKind.KEY_2, KeyPropPainter::key);
    }

    private static ResourceLocation particle(String n) { return AuraRender.tex("particle/" + n); }

    // ================================================================ the gate
    private static void gate(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife()), s = e.scale();
        float k = KeyDraw.smooth(life / 10f) * Mth.clamp((max - life) / 8f, 0f, 1f);
        if (k <= 0.01f) return;
        int el = Mth.clamp(e.param() & 3, 0, 3);
        int hue = KeyDraw.mix(GOLD, ELEMENT[el], 0.45f);
        // which clip: idle until the doors open, open, hold open (looping), close in the last 14 ticks
        float openAt = GATE_OPEN_AT, closeAt = max - GATE_CLOSE;
        String clip;
        float clock, open;
        if (life < openAt) { clip = "idle"; clock = age / 20f; open = 0f; }
        else if (life < closeAt) {
            float since = (life - openAt) / 20f;
            if (since < 0.9f) { clip = "open"; clock = since; open = KeyDraw.smooth(since / 0.9f); }
            else { clip = "open_hold"; clock = age / 20f; open = 1f; }
        } else { clip = "close"; clock = (life - closeAt) / 20f; open = 1f - KeyDraw.smooth(clock / 0.65f); }
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.3f);
        float shots = life >= openAt + 6f && life <= closeAt ? (life - openAt - 6f) % GATE_FIRE_EVERY : 99f;
        float flash = Mth.clamp(1f - shots / 4f, 0f, 1f);

        KeyDraw.camera(pose);
        // 1. the ground sigil and its counter ring: additive, flat
        pose.pushPose();
        pose.translate(0f, 0.04f, 0f);
        KeyDraw.flat(pose, buf.getBuffer(AuraRender.additive(sigil)), 2.3f * s * (0.7f + 0.3f * k), age * 1.1f, KeyDraw.argb(0.85f * k, hue), FB);
        pose.translate(0f, 0.01f, 0f);
        KeyDraw.flat(pose, buf.getBuffer(AuraRender.additive(ring)), 1.5f * s * (0.7f + 0.3f * k), -age * 2.2f, KeyDraw.argb(0.6f * k * pulse, VIOLET), FB);
        pose.popPose();

        // 2. the gate itself, rising from the ground
        float rise = KeyDraw.smooth(life / 12f) * KeyDraw.smooth((max - life) / 10f);
        int glow = KeyDraw.argb(Mth.clamp(k * (0.75f + 0.25f * pulse + 0.35f * flash), 0f, 1f), KeyDraw.mix(WHITE, ELEMENT[el], 0.3f));
        pose.pushPose();
        pose.translate(0f, -(1f - rise) * 3.6f * s, 0f);
        pose.scale(s, s, s);
        GeoDraw.paint(pose, buf, GATE, clip, clock, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, glow);
        pose.popPose();

        // 3. the light in the doorway (between the doors and the void), 4. the glint at the keyhole
        if (open > 0.02f) {
            KeyDraw.upright(pose, buf.getBuffer(AuraRender.additive(door)), 0f, 2.05f * s, -0.03f * s, 0.9f * s, 1.75f * s,
                    KeyDraw.argb(Mth.clamp(open * k * (0.45f * pulse + 0.4f * flash), 0f, 1f), hue), FB);
            VertexConsumer gv = buf.getBuffer(AuraRender.additive(glint));
            KeyDraw.glow(pose, gv, 0f, 1.95f * s, 0.2f * s, (0.55f + 0.9f * flash) * s, age * 0.05f, KeyDraw.argb(Mth.clamp(open * k * (0.5f + 0.5f * flash), 0f, 1f), hue), FB);
        }
    }

    // ================================================================ the great key
    private static void key(MagicPropEntity e, float partial, float age, PoseStack pose, MultiBufferSource buf, int light) {
        float life = e.life() + partial, max = Math.max(1, e.maxLife()), s = e.scale();
        float k = KeyDraw.smooth(life / 8f) * Mth.clamp((max - life) / 8f, 0f, 1f);
        if (k <= 0.01f) return;
        int slot = e.param() & 3, ks = (e.param() >> 8) & 255;
        Entity foe = e.target();
        boolean stab = ks > 0;
        float reach = 34f / 16f * s;                                                    // centre to tip of the model, in blocks
        pose.pushPose();
        if (foe != null) {                                                               // pitch the tip at the foe (the yaw is the entity's)
            Vec3 d = new Vec3(foe.getX() - e.getX(), foe.getY() + foe.getBbHeight() * 0.5 - e.getY(), foe.getZ() - e.getZ());
            double flat = Math.sqrt(d.x * d.x + d.z * d.z);
            pose.mulPose(Axis.XP.rotationDegrees(-(float) Math.toDegrees(Mth.atan2(d.y, Math.max(0.01, flat)))));
        } else {
            pose.mulPose(Axis.XP.rotationDegrees(-8f));
        }
        KeyDraw.camera(pose);
        String clip = stab ? "thrust" : "idle";
        float clock = stab ? (ks - 1 + partial) / 20f : age / 20f + slot * 1.3f;
        float pulse = 0.75f + 0.25f * Mth.sin(age * 0.35f + slot);
        // 1. the model (the glow map is its second, additive pass)
        pose.pushPose();
        pose.translate(0f, -0.75f * s * k, 0f);                                          // the model's centre sits 12 px over its origin
        pose.scale(s * k, s * k, s * k);
        GeoDraw.paint(pose, buf, KEY, clip, clock, GeoDraw.Space.PROP, GeoDraw.Layer.CUTOUT, light, 0xFFFFFFFF, KeyDraw.argb(Mth.clamp(k * pulse, 0f, 1f), 0xFFE9B0));
        pose.popPose();
        // 2. a glint on the bow's gem, and the thrust's streak and tip glint
        KeyDraw.glow(pose, buf.getBuffer(AuraRender.additive(glint)), 0f, 0f, -(23f / 16f) * s * k, 0.34f * s * pulse, age * 0.04f, KeyDraw.argb(0.55f * k, VIOLET), FB);
        if (stab && ks >= 6 && ks <= 13) {
            float q = ks <= 11 ? 1f : 1f - (ks - 11) / 3f;
            KeyDraw.ribbon(pose, buf.getBuffer(AuraRender.additive(streak)), 0f, 0f, -2.6f * s, 0f, 0f, reach, 0.28f * s, 0f, 1f, KeyDraw.argb(0.8f * q * k, GOLD), FB);
            KeyDraw.glow(pose, buf.getBuffer(AuraRender.additive(glint)), 0f, 0f, reach, 0.6f * s * q, age * 0.2f, KeyDraw.argb(0.9f * q * k, 0xFFF4C8), FB);
        }
        pose.popPose();
    }
}
