package com.newuniverse.nusmp.relics.wave;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Wave 8: the small server-side helpers the blades and axes share (held id, aiming, paralysis, the soul counter, a safe landing). */
final class Wave8Util {
    private Wave8Util() {}

    /** The def id of the relic in the main hand, or "". */
    static String heldId(ServerPlayer p) {
        RelicDef d = Relics.defOf(p.getMainHandItem());
        return d == null ? "" : d.id();
    }

    static void tell(ServerPlayer p, String text) { p.displayClientMessage(Component.literal(text), true); }

    // ---------------------------------------------------------------- aiming
    /** The nearest foe within {@code range} that lies inside the cone of the look vector (cosine {@code cosHalf}); null if none. */
    static LivingEntity aimed(ServerPlayer p, double range, double cosHalf) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (LivingEntity t : Fx.foesAround(p, range)) {                                           // nearest first
            Vec3 d = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(eye);
            if (Wave8Rules.inCone(d.x, d.y, d.z, look.x, look.y, look.z, range, cosHalf)) return t;
        }
        return null;
    }

    // ---------------------------------------------------------------- control
    /** Hard control, as the balance law allows it: Tensura's paralysis where it exists plus a complete slow; the duration is already capped by the caller. */
    static void paralyse(LivingEntity t, int ticks) {
        if (ticks <= 0) return;
        Fx.addEffect(t, "tensura:paralysis", ticks, 0);
        Fx.addEffect(t, "minecraft:slowness", ticks, 6);
        Fx.addEffect(t, "minecraft:weakness", ticks, 1);
        if (t instanceof Mob m) m.getNavigation().stop();
    }

    static void chill(LivingEntity t, int ticks) {
        t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 40, t.getTicksFrozen() + ticks));
        Fx.addEffect(t, "tensura:frost", ticks, 0);
        Fx.addEffect(t, "minecraft:slowness", Math.min(ticks, 80), 0);
    }

    // ---------------------------------------------------------------- the soul counter (survives death: the player's persisted data)
    private static final String ROOT = "nusmp_w8", SOULS = "souls", DAY = "day", USED = "used";

    private static CompoundTag root(Player p, boolean create) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, Tag.TAG_COMPOUND)) {
            if (!create) return null;
            pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        CompoundTag persisted = pd.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.contains(ROOT, Tag.TAG_COMPOUND)) {
            if (!create) return null;
            persisted.put(ROOT, new CompoundTag());
        }
        return persisted.getCompound(ROOT);
    }

    static int souls(Player p) {
        CompoundTag r = root(p, false);
        return r == null ? 0 : r.getInt(SOULS);
    }

    static void setSouls(Player p, int n) { root(p, true).putInt(SOULS, Math.max(0, Math.min(Wave8Rules.MAX_SOULS, n))); }

    static int chargesLeft(ServerPlayer p) {
        CompoundTag r = root(p, false);
        long today = Wave8Rules.dayOf(p.level().getGameTime());
        return Wave8Rules.chargesLeft(souls(p), r == null ? -1 : r.getLong(DAY), r == null ? 0 : r.getInt(USED), today);
    }

    static void useCharge(ServerPlayer p) {
        CompoundTag r = root(p, true);
        long today = Wave8Rules.dayOf(p.level().getGameTime());
        int used = Wave8Rules.usedAfter(r.getLong(DAY), r.getInt(USED), today);
        r.putLong(DAY, today);
        r.putInt(USED, used);
    }

    // ---------------------------------------------------------------- landing
    /** A spot with room to stand and ground within three blocks below, no lava; same test as the relics' own blink. Null if none. */
    static Vec3 landing(ServerLevel level, ServerPlayer p, double x, double y, double z) {
        for (int dy : new int[]{0, 1, -1, 2}) {
            double yy = y + dy;
            AABB box = p.getBoundingBox().move(x - p.getX(), yy - p.getY(), z - p.getZ());
            if (!level.noCollision(p, box)) continue;
            BlockPos feet = BlockPos.containing(x, yy, z);
            boolean ground = false;
            for (int i = 1; i <= 3 && !ground; i++) {
                BlockState below = level.getBlockState(feet.below(i));
                if (below.is(net.minecraft.world.level.block.Blocks.LAVA) || below.is(net.minecraft.world.level.block.Blocks.FIRE)) return null;
                ground = !below.getCollisionShape(level, feet.below(i)).isEmpty();
            }
            if (ground && !level.getFluidState(feet).is(net.minecraft.tags.FluidTags.LAVA)) return new Vec3(x, yy, z);
        }
        return null;
    }

    /** The foes (nearest first) from {@code Fx.foesAround}, as a list that can be empty. */
    static List<LivingEntity> foes(ServerPlayer p, double r) { return Fx.foesAround(p, r); }
}
