package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.49 Game Magic (texture: tools/gen_game_textures.py). GAME_BOARD: a round board of glowing squares unrolls over the ground to
 * radius power, its grid pulsing gold, a slow rune ring turning on its rim, and pieces of light (motes) hopping square to square.
 * A short one (a sprung trap square) is a single bright flash of the board.
 */
public class GameLayer extends AbstractVfxLayer {
    static final ResourceLocation BOARD = VfxTextures.byName("game_board"), RING = VfxTextures.RUNE_RING, GLOW = VfxTextures.GLOW;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.GAME_BOARD); }
    @Override public int defaultDuration(VfxShape s) { return 200; }
    @Override public int defaultColor(VfxShape s) { return 0xFFE8C04A; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(0.5f, inst.power);
        Vector3f c = ctx.rel(inst.from(ctx));
        float open = VfxAnim.easeOutCubic(Mth.clamp(age / 16f, 0, 1)), fade = life(inst, age, 0, Math.min(30, inst.duration / 2f));
        float pulse = 0.65f + 0.35f * Mth.sin(age * 0.12f);
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.04f, 0));
        float r = R * open;
        buf.plane(BOARD, VfxBlend.ALPHA, ground, r, VfxVertexBuffer.withAlpha(0xFF101018, 0.35f * fade));
        buf.plane(BOARD, VfxBlend.ADD, ground.lift(0.01f), r, VfxVertexBuffer.withAlpha(inst.color, 0.75f * pulse * fade));
        if (inst.duration <= 40) return;
        buf.ring(RING, VfxBlend.ADD, ground.lift(0.02f).spin(age * 0.01f), r * 0.94f, r * 1.04f, ctx.seg(32, 16), 8, age * 0.005f,
                VfxVertexBuffer.withAlpha(inst.color, 0.6f * fade));
        float cell = R * 2 / 12;
        for (int k = 0; k < 8; k++) {                                            // pieces of light hopping square to square
            float hop = (age * 0.05f + hash(inst.seed, k, 1)) % 1f;
            int step = (int) (age * 0.05f + hash(inst.seed, k, 1));
            float gx = (Math.floorMod(Math.round(hash(inst.seed, k, 2) * 12) + step, 12) - 5.5f) * cell, gz = (Math.round(hash(inst.seed, k, 3) * 11) - 5.5f) * cell;
            if (gx * gx + gz * gz > r * r * 0.8f) continue;
            Vector3f p = new Vector3f(c).add(gx, 0.25f + Mth.sin(hop * Mth.PI) * 0.8f, gz);
            buf.billboard(ctx, GLOW, VfxBlend.ADD, p, 0.35f, 0, VfxVertexBuffer.withAlpha(k % 3 == 0 ? 0xFFFF6A6A : inst.color, fade));
        }
    }
}
