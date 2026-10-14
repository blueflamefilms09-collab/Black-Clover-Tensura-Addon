package com.newuniverse.nusmp.dungeon.nexus.zone;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 1.35.0: what the Blood-Red Commander's moves look like (drawn by {@code CommanderBossLayer}): crimson banners rising round every danger,
 * crimson chains drawn along every line before it is cut, the vault light that falls when it roars or calls its soldiers, and a crimson
 * cut with a blood stain for every strike. Only sends effects; the old dust marks stay as the exact hit shapes. Nothing is placed or broken.
 */
final class MonarchFx {
    private MonarchFx() {}

    static final int CRIMSON = 0xFFD0102A, PALE = 0xFFFFE6C8;

    /** Banners rise round a danger of `radius` blocks at (x, y, z) for `ticks`. */
    static void banners(ServerLevel l, double x, double y, double z, double radius, int ticks) {
        VfxSpawn.send(l, VfxShape.CMD_BANNERS, new Vec3(x, y + 0.05, z), new Vec3(x, y + 1.0, z), CRIMSON, Math.max(12, ticks), (float) radius);
    }

    /** Crimson chains are drawn from a to b for `ticks` and cinch (the telegraph of a line). */
    static void chains(ServerLevel l, Vec3 a, Vec3 b, int ticks, float size) {
        VfxSpawn.send(l, VfxShape.CMD_CHAINS, a, b, CRIMSON, Math.max(10, ticks), size);
    }

    /** The vault light falls on (x, y, z). */
    static void vault(ServerLevel l, double x, double y, double z, double radius, int ticks) {
        VfxSpawn.send(l, VfxShape.CMD_VAULT_LIGHT, new Vec3(x, y + 0.05, z), new Vec3(x, y + 1.0, z), PALE, Math.max(20, ticks), (float) radius);
    }

    /** A crimson cut from a to b with a blood stain at b. */
    static void cut(ServerLevel l, Vec3 a, Vec3 b, float size, int ticks) {
        VfxSpawn.send(l, VfxShape.CMD_CRIMSON_STRIKE, a, b, CRIMSON, Math.max(10, ticks), size);
    }
}
