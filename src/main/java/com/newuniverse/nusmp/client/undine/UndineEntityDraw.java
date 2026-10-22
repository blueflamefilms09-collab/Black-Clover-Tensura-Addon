package com.newuniverse.nusmp.client.undine;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.client.prop.UndineDraw;
import com.newuniverse.nusmp.entity.ElementalSpiritLordEntity;
import com.newuniverse.nusmp.perf.PerfSettings;
import com.newuniverse.nusmp.undine.UndineConfig;
import com.newuniverse.nusmp.undine.UndineLod;
import com.newuniverse.nusmp.undine.UndineFace;
import com.newuniverse.nusmp.undine.UndinePose;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the Spirit Lord of Water in her own body (the exclusive {@code ElementalSpiritLordEntity} of kind UNDINE) is drawn as the new Undine, the tall woman of water, while {@code magic.undine} is on;
 * with it off the renderer keeps the old mermaid. She is always the full form (arms spread, standing on the ring of waves, 3.2 blocks tall); the entity's own cast clip plays the cast pose. Same cost rules as
 * the prop: the level of detail follows the distance, the frame quad budget is shared with every other Undine and the owner's own close one is never dropped.
 */
public final class UndineEntityDraw {
    private UndineEntityDraw() {}

    private static final UndineLod.QuadBudget BUDGET = new UndineLod.QuadBudget();

    /** True when the Undine was drawn (the pose is already at the entity's feet and turned with its body); false when the old look should be drawn. */
    public static boolean draw(ElementalSpiritLordEntity e, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        if (!UndineConfig.enabled()) return false;
        Minecraft mc = Minecraft.getInstance();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double dx = e.getX() - cam.x, dy = e.getY() + 1.5 - cam.y, dz = e.getZ() - cam.z, dsq = dx * dx + dy * dy + dz * dz;
        int wanted = UndineLod.level(true, dsq, PerfSettings.vfxQuality().detail);
        if (wanted == UndineLod.CULL) return true;                                           // too far to draw: nothing, and not the old look either
        boolean mine = e.getOwner() != null && e.getOwner() == mc.player;
        int lod = UndineLod.fit(true, wanted, BUDGET, UndineLod.frameKey(e.level().getGameTime(), partial), UndineLod.ownerPriority(mine, dsq));
        if (lod == UndineLod.CULL) return true;
        float age = e.tickCount + partial, castAge = e.tickCount - e.clientCastTick + partial;
        boolean casting = castAge >= 0 && castAge < UndinePose.CAST.ticks();
        String clip = casting ? UndinePose.CAST.clip(true) : UndinePose.NONE.clip(true);
        float secs = casting ? castAge / 20f : age / 20f;
        UndineDraw.undine(pose, buffers, true, lod, clip, secs, age, 1f, light, casting ? UndineFace.CALM.id() : UndineFace.CALM.id());
        return true;
    }
}
