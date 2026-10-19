package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Plant Magic (1.93.0 rebuild): the spells of {@link PlantBook}. Canon: Plant Magic is the magic of Mimosa Vermillion (healing flowers, the flowering vines of the Golden Dawn's healer) and of
 * the fern-and-frond plant users of the wiki; the repo's wiki dump has no page for it and the sandbox has no network, so the named spells of the first release stay (Magic Flower Guidepost, the
 * Dream World of a Hundred Flowers, with the two borrowed captain spells) and the new ones are built from the canon look: flowers, petals, healing and entangling green.
 * <pre>
 *   guidepost         Plant Magic: Magic Flower Guidepost         (kept, remade) flowers bloom along a path to the one you look at (or yourself) and mend them: 20 percent of the maximum health
 *   hundred_flowers   Plant Magic: Dream World of a Hundred Flowers  (kept, remade) a 6-block dream world for 8 s: friends heal, foes are tangled and cut
 *   yggdrasil, briar_prison                                       (kept untouched: the borrowed captain spells, see PlantBook)
 *   seed_bloom        Plant Magic: Seed Bloom           a seed that blossoms where it lands: foes are hurt and slowed, friends in the blast heal
 *   healing_bloom     Plant Magic: Healing Bloom        a great bloom (prop PLANT_1, variant 1) grows at the aim point and heals everyone near it for 8 s
 *   flower_sprite     Plant Magic: Flower Sprite        a flower sprite familiar (prop PLANT_1, variant 0) circles the caster for 30 s: it mends the most hurt friend and shoots petals at foes
 *   ivy_barrier       Plant Magic: Ivy Barrier          a wall of flowering leaves that dissolves everything shot at it
 *   blossom_dress     Plant Magic: Blossom Dress        the Blossom armour (Aura.PLANT): blows are softened, each one mends the wearer a little, regeneration
 *   petal_storm       Plant Magic: Petal Storm          for 8 s a storm of petals circles the caster: foes are cut and slowed, friends mend
 * </pre>
 */
public final class PlantArts {
    private PlantArts() {}

    private static final BlockState IVY = Blocks.FLOWERING_AZALEA_LEAVES.defaultBlockState();

    // ------------------------------------------------------------------ Magic Flower Guidepost
    /** A heal beam on the player you look at (or yourself): 20% max health, healing decay applies. */
    static boolean guidepost(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        LivingEntity heal = t instanceof Player ? t : p;
        BalanceLaw.heal(heal, heal.getMaxHealth() * 0.2f);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.PLANT_FX1, p.getEyePosition(), heal.getBoundingBox().getCenter(), 14, 0.8f);
        b.vfx(p, VfxShape.PLANT_FX3, heal.position(), heal.position().add(0, 1, 0), 24, 0.6f);
        return true;
    }

    // ------------------------------------------------------------------ Dream World of a Hundred Flowers
    /** A 6-block field for 8 s: you and nearby players heal, enemies are slowed and cut by thorns. */
    static boolean dreamWorld(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = p.position();
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.PLANT_FX2, c, c.add(0, 1, 0), 160, 6f);
        SpellRuntime.zone(p.serverLevel(), 160, 20, age -> {
            for (Player ally : p.serverLevel().getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(c, c).inflate(6))) {
                if (ally == p || ally.isAlliedTo(p)) BalanceLaw.heal(ally, 2f);
            }
            for (LivingEntity t : GrimoireBook.around(p, c, 6)) {
                if (t instanceof Player pl && pl.isAlliedTo(p)) continue;
                b.hurt(i, p, t, mode, 2f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Seed Bloom
    static boolean seedBloom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f);
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.PLANT_FX1, start, start.add(dir.scale(1.7 * 24)), 24, 0.9f);
        SpellRuntime.bolt(p, start, dir.scale(1.7), 0.5, 24, false, null, (bolt, t) -> { }, (bolt, at) -> {
            b.vfx(p, VfxShape.PLANT_FX3, at, at.add(0, 1, 0), 22, 0.7f);
            for (LivingEntity t : TreeKit.foes(p, at, 2.4 * s)) {
                b.hurt(i, p, t, mode, 7f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
            }
            for (LivingEntity f : TreeKit.friends(p, at, 2.4 * s)) BalanceLaw.heal(f, 3f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Healing Bloom
    static boolean healingBloom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 16);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.PLANT_FX1, p.getEyePosition(), c, 12, 0.8f);
        b.vfx(p, VfxShape.PLANT_FX2, c, c.add(0, 1, 0), 170, 5f * Math.min(1.3f, TreeKit.k(i, p)));
        TreeKit.spawn(b, i, p, mode, PropKind.PLANT_1, TreeKit.onGround(p.serverLevel(), c), 1, 170, Math.min(1.3f, TreeKit.k(i, p)), p.getYRot());
        return true;
    }

    // ------------------------------------------------------------------ Flower Sprite
    static boolean sprite(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = p.position().add(TreeKit.forward(p).scale(1.2)).add(0, 1.6, 0);
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.PLANT_FX3, at, at.add(0, 1, 0), 26, 0.9f);
        TreeKit.spawn(b, i, p, mode, PropKind.PLANT_1, at, 0, 600, Math.min(1.3f, TreeKit.k(i, p)), p.getYRot());
        return true;
    }

    // ------------------------------------------------------------------ Ivy Barrier
    static boolean ivy(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = TreeKit.forward(p), side = new Vec3(-dir.z, 0, dir.x), base = p.position().add(dir.scale(2.5));
        ServerLevel level = p.serverLevel();
        int placed = 0;
        for (int w = -2; w <= 2; w++) for (int h = 0; h < 3; h++)
            if (TreeKit.put(level, BlockPos.containing(base.add(side.scale(w)).add(0, h, 0)), IVY, 240)) placed++;
        if (placed == 0) { GrimoireBook.fail(p, "No room for the ivy."); return false; }
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.PLANT_FX2, base, base.add(0, 1, 0), 120, 3f);
        SpellRuntime.zone(level, 240, 5, age -> SpellRuntime.dissolveBolts(level, base.add(0, 1, 0), 3.2, p));
        return true;
    }

    // ------------------------------------------------------------------ Blossom Dress
    static boolean dress(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        TreeKit.armour(i, p, Aura.PLANT, "PLANT", 200, -1);
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 0));
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.PLANT_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.7f);
        return true;
    }

    // ------------------------------------------------------------------ Petal Storm
    static boolean storm(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 5 * s;
        b.castCircle(p, 1.2f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.PLANT_FX2, p, p.position(), b.color, 160, (float) r);
        SpellRuntime.zone(p.serverLevel(), 160, 10, age -> {
            Vec3 c = p.position();
            for (LivingEntity t : TreeKit.foes(p, c, r)) {
                b.hurt(i, p, t, mode, 2.5f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 24, 1));
            }
            if (age % 20 == 0) for (LivingEntity f : TreeKit.friends(p, c, r)) BalanceLaw.heal(f, 1.5f);
        });
        return true;
    }
}
