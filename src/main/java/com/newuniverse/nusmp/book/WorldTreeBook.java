package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxPayload;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * World Tree Magic (0.45, new; William Vangeance). After the wiki: gigantic world trees whose branches and roots bind, catch
 * attacks in mid-air, and absorb the mana of those nearby.
 * <ul>
 *   <li><b>Terrain:</b> roots and trees are real blocks (mangrove roots, oak, flowering azalea) grown into open space only and
 *       taken away again after their time (SpellRuntime.tempBlock): the battlefield changes, nothing is griefed.</li>
 *   <li><b>Energy Drain:</b> bound foes lose magicules (Tensura) and what is drained flows to you and your allies
 *       ({@link EnergyBridge#drain}).</li>
 *   <li><b>Catching spells:</b> inside Budding of Yggdrasil, enemy shots are caught by the roots.</li>
 * </ul>
 * Pages: Mistilteinn Seed, Root Bind, Magic Tree Descent, Budding of Yggdrasil.
 */
public class WorldTreeBook extends GrimoireBook {
    static final int EMERALD = 0xFF3CE08A, BARK = 0xFF7A5232;

    private final List<BookPage> pages = List.of(
            BookPage.starter("mistilteinn_seed", "Mistilteinn Seed", WorldTreeBook::seed),
            BookPage.mid("root_bind", "Root Bind", WorldTreeBook::rootBind),
            BookPage.zone("magic_tree_descent", "Magic Tree Descent", WorldTreeBook::treeDescent).withCooldown(900),
            BookPage.signature("budding_of_yggdrasil", "Budding of Yggdrasil", WorldTreeBook::yggdrasil).withCooldown(1200));

    public WorldTreeBook() { super(MagicType.WORLD_TREE, 0xFF3CE08A); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.EARTH_ELEMENTAL; }

    static void roots(ServerPlayer p, Vec3 at, float radius, int count, int ticks) {
        long seed = (p.getRandom().nextLong() & ~15L) | Math.min(15, count);
        VfxSpawn.send(p.serverLevel(), new VfxPayload(VfxShape.TREE_ROOTS.ordinal(), at, at.add(0, 1, 0), EMERALD, ticks, radius, -1, seed));
    }

    /** Holds a foe fast for 'ticks' (roots round its legs) and drains its magicules to the caster. */
    static void bind(ServerPlayer p, LivingEntity t, int ticks, double drainFrac) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 6, false, true));
        t.setDeltaMovement(0, Math.min(0, t.getDeltaMovement().y), 0);
        t.hurtMarked = true;
        double got = EnergyBridge.drain(t, p, drainFrac);
        if (got > 0) VfxSpawn.send(p.serverLevel(), VfxShape.TREE_DRAIN, t.getBoundingBox().getCenter(), p.getBoundingBox().getCenter(), EMERALD, 16, 0.5f);
    }

    /** Grows a short root sculpture of real blocks at 'base' (temporary). */
    static void growRoots(ServerLevel level, BlockPos base, int len, int ticks, net.minecraft.util.RandomSource rnd) {
        BlockState root = Blocks.MANGROVE_ROOTS.defaultBlockState();
        BlockPos at = base;
        int dx = rnd.nextInt(3) - 1, dz = rnd.nextInt(3) - 1;
        for (int k = 0; k < len; k++) {
            at = at.offset(k % 2 == 0 ? dx : 0, 1, k % 2 == 1 ? dz : 0);
            if (!SpellRuntime.tempBlock(level, at, root, ticks)) break;
        }
    }

    /** Mistilteinn Seed: a seed of the world tree; where it strikes, roots burst out and bind, drinking the foe's mana. */
    static boolean seed(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 eye = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.TREE_DRAIN, eye, eye.add(dir.scale(20)), 10, 0.3f);
        SpellRuntime.bolt(p, eye, dir.scale(1.4 + 0.4 * EnergyBridge.scale(p)), 0.5, 16, false, null,
                (bolt, t) -> {
                    b.hurt(i, p, t, mode, 7f * EnergyBridge.scale(p));
                    roots(p, t.position(), 1.6f, 5, 40);
                    bind(p, t, 40, 0.03);
                },
                (bolt, at) -> roots(p, at, 1.2f, 4, 30));
        return true;
    }

    /** Root Bind: roots tear up under the foe you look at, hold it, and drag it toward you. */
    static boolean rootBind(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        if (!GrimoireBook.control(p)) return false;
        LivingEntity t = target(p, 24);
        if (t == null) { fail(p, "No one to bind."); return false; }
        b.castCircle(p, 0.9f);
        roots(p, t.position(), 2f, 7, 60);
        growRoots(p.serverLevel(), t.blockPosition().below(), 2, 100, p.getRandom());
        SpellRuntime.zone(p.serverLevel(), 50, 10, age -> {
            if (!t.isAlive()) return;
            bind(p, t, 12, 0.015);
            b.hurt(i, p, t, mode, 2f * EnergyBridge.scale(p));
        });
        SpellRuntime.later(p.serverLevel(), 50, () -> {
            if (!t.isAlive()) return;
            Vec3 pull = p.position().subtract(t.position()).normalize().scale(1.2);
            t.setDeltaMovement(pull.x, 0.4, pull.z);
            t.hurtMarked = true;
        });
        return true;
    }

    /**
     * Magic Tree Descent: a world tree comes down out of a hole in the sky and roots where you aim. For 20 s its roots bind foes
     * around it and drain them, and what it drinks flows to you and your allies, who also mend in its shade.
     */
    static boolean treeDescent(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        Vec3 at = aim(p, 24);
        float r = 8 * GrimoireBook.size(i, p) * (0.9f + 0.1f * EnergyBridge.power(p));
        float h = 9 + 3 * EnergyBridge.scale(p);
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.TREE_CANOPY, at, at.add(0, h, 0), 400, h);
        roots(p, at, r, 12, 400);
        // the trunk and a crown of real blocks, gone again when the spell ends
        BlockPos base = BlockPos.containing(at);
        int trunk = (int) (h * 0.6f);
        for (int y = 0; y < trunk; y++)
            for (int x = 0; x <= 1; x++) for (int z = 0; z <= 1; z++)
                SpellRuntime.tempBlock(level, base.offset(x, y, z), Blocks.OAK_LOG.defaultBlockState(), 400);
        for (int x = -3; x <= 4; x++) for (int z = -3; z <= 4; z++) for (int y = 0; y < 3; y++)
            if ((x - 0.5) * (x - 0.5) + (z - 0.5) * (z - 0.5) + (y - 1) * (y - 1) * 3 < 14)
                SpellRuntime.tempBlock(level, base.offset(x, trunk + y, z), Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState()
                        .setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true), 400);
        for (int k = 0; k < 6; k++) {
            float a = Mth.TWO_PI * k / 6;
            growRoots(level, base.offset(Math.round(Mth.cos(a) * 3), -1, Math.round(Mth.sin(a) * 3)), 3, 400, p.getRandom());
        }
        SpellRuntime.zone(level, 400, 20, age -> {
            double total = 0;
            for (LivingEntity t : around(p, at, r)) {
                bind(p, t, 20, 0);
                total += EnergyBridge.drain(t, null, 0.02);
                b.hurt(i, p, t, mode, 2f);
            }
            List<Player> friends = level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(at, at).inflate(r),
                    x -> x == p || x.isAlliedTo(p));
            for (Player f : friends) {
                f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 30, 0));
                if (total > 0) {                                                      // share what the tree drank
                    var ex = io.github.manasmods.tensura.storage.TensuraStorages.getExistenceFrom(f);
                    if (ex != null) {
                        ex.setMagicule(Math.min(io.github.manasmods.tensura.util.EnergyHelper.getMaxMagicule(f), ex.getMagicule() + total / friends.size()));
                        ex.markDirty();
                    }
                    VfxSpawn.send(level, VfxShape.TREE_DRAIN, at.add(0, h * 0.5, 0), f.getBoundingBox().getCenter(), EMERALD, 18, 0.4f);
                }
            }
        });
        level.playSound(null, base, SoundEvents.ROOTED_DIRT_PLACE, SoundSource.PLAYERS, 2f, 0.5f);
        return true;
    }

    /**
     * Budding of Yggdrasil: twelve great roots burst up in a ring around you. Every foe inside is bound and drained for 5 s,
     * and enemy shots that enter are caught by the roots in mid-air.
     */
    static boolean yggdrasil(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        float r = 12 * GrimoireBook.size(i, p) * (0.9f + 0.1f * EnergyBridge.power(p));
        Vec3 c = p.position();
        b.castCircle(p, 2f);
        roots(p, c, r, 12, 100);
        for (int k = 0; k < 12; k++) {
            float a = Mth.TWO_PI * k / 12;
            growRoots(level, BlockPos.containing(c.add(Mth.cos(a) * r * 0.8, -1, Mth.sin(a) * r * 0.8)), 4, 140, p.getRandom());
        }
        SpellRuntime.zone(level, 100, 5, age -> {
            for (Projectile pr : level.getEntitiesOfClass(Projectile.class, new net.minecraft.world.phys.AABB(c, c).inflate(r),
                    x -> x.getOwner() != p && !(x.getOwner() instanceof LivingEntity o && o.isAlliedTo(p)))) {
                b.vfx(p, VfxShape.TREE_ROOTS, pr.position(), pr.position(), 20, 0.6f);
                pr.discard();
            }
            if (age % 20 != 0) return;
            for (LivingEntity t : around(p, c, r)) {
                bind(p, t, 22, 0.03);
                b.hurt(i, p, t, mode, 5f * EnergyBridge.scale(p));
            }
        });
        level.playSound(null, p.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.PLAYERS, 2f, 0.4f);
        return true;
    }
}
