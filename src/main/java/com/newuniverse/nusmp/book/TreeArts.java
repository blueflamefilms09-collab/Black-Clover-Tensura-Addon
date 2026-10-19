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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tree Magic (1.93.0 rebuild): the book of heartwood, bark and living trunks. The five generic pages keep their ids and order (attribute_bolt, attribute_cut, attribute_field,
 * attribute_domain, attribute_guard, so every unlock and mastery bit stays); they are remade as Tree spells, and five pages are appended.
 * <pre>
 *   attribute_bolt    Tree Magic: Splinter Shot      a sharpened shaft of wood, pierces, slows
 *   attribute_cut     Tree Magic: Root Lance         lances of root burst out of the ground along a line, one after another (prop TREE_1, variant 1)
 *   attribute_field   Tree Magic: Grove              a grove grows at the aim point: foes are held and cut, friends mend
 *   attribute_domain  Tree Magic: Great Tree Rising  a great trunk rises under everything round the aim point: heavy damage, thrown up, held by roots
 *   attribute_guard   Tree Magic: Bark Armor         the Heartwood armour (Aura.TREE) grows over the caster, blows are softened and answered with thorns that root
 *   heartwood_guardian  Tree Magic: Heartwood Guardian  a treant fights for the caster for 20 s (prop TREE_1, variant 0)
 *   seedling_volley   Tree Magic: Seedling Volley    five seeds that sprout into rooting thorns where they land
 *   timber_wall       Tree Magic: Timber Wall        a wall of oak trunks, foes in front are thrown back
 *   verdant_canopy    Tree Magic: Verdant Canopy     a canopy over the caster: friends mend and regenerate, foes beneath are slowed
 *   heartwood_fist    Tree Magic: Heartwood Charge   a charge with a fist of heartwood: hits everything on the way, throws it back
 * </pre>
 * Canon: the wiki page of Tree Magic could not be reached (the sandbox has no network), so the spells are built from what the magic is (creating and controlling trees and wood, the
 * "World Tree" family of the grimoire list) and named in the canon idiom; see docs/attributes/tree_rebuild.md.
 */
public final class TreeArts {
    private TreeArts() {}

    private static final BlockState LOG = Blocks.OAK_LOG.defaultBlockState();

    public static List<BookPage> pages() {
        return List.of(
                BookPage.starter("attribute_bolt", "Tree Magic: Splinter Shot",
                        ElementBook.bolt(9f, 1.8, 0.55, 24, true, 0, VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_BURST,
                                ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1)))),
                BookPage.mid("attribute_cut", "Tree Magic: Root Lance", TreeArts::lances),
                BookPage.zone("attribute_field", "Tree Magic: Grove", TreeArts::grove),
                BookPage.signature("attribute_domain", "Tree Magic: Great Tree Rising", TreeArts::rising).withCooldown(900),
                BookPage.mid("attribute_guard", "Tree Magic: Bark Armor", TreeArts::barkArmor),
                // 1.93.0: appended
                BookPage.signature("heartwood_guardian", "Tree Magic: Heartwood Guardian", TreeArts::guardian).withCooldown(1200).withAnim("signature"),
                BookPage.mid("seedling_volley", "Tree Magic: Seedling Volley", TreeArts::seedlings),
                BookPage.mid("timber_wall", "Tree Magic: Timber Wall", TreeArts::timberWall),
                BookPage.zone("verdant_canopy", "Tree Magic: Verdant Canopy", TreeArts::canopy),
                BookPage.mid("heartwood_fist", "Tree Magic: Heartwood Charge",
                        ElementBook.dash(13f, 7, false, VfxShape.WIKI_MAGIC_BURST,
                                ElementBook.all(ElementBook.knock(1.1), ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1))))).withAnim("sweep"));
    }

    // ------------------------------------------------------------------ Root Lance
    static boolean lances(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        ServerLevel level = p.serverLevel();
        Vec3 dir = TreeKit.forward(p), from = p.position();
        int n = Math.min(7, 4 + Math.round(s * 2));
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.TREE_FX1, p.position().add(0, 0.4, 0), from.add(dir.scale(2.0 * n)), 14, 0.8f);
        for (int k = 0; k < n; k++) {
            final int kk = k;
            SpellRuntime.later(level, 2 + k * 2, () -> {
                if (!p.isAlive()) return;
                Vec3 at = from.add(dir.scale(2.0 * (kk + 1)));
                Vec3 ground = TreeKit.onGround(level, at);
                TreeKit.spawn(b, i, p, mode, PropKind.TREE_1, ground, 1, 30, 1.0f, (float) Math.toDegrees(Math.atan2(-dir.x, dir.z)));
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ Grove
    static boolean grove(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 5.5 * s;
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.TREE_FX2, c, c.add(0, 1, 0), 160, (float) r);
        SpellRuntime.zone(p.serverLevel(), 160, 20, age -> {
            for (LivingEntity t : TreeKit.foes(p, c, r)) {
                b.hurt(i, p, t, mode, 3f);
                TreeKit.root(t, 30, 2);
            }
            for (LivingEntity f : TreeKit.friends(p, c, r)) BalanceLaw.heal(f, f.getMaxHealth() * 0.025f);
        });
        return true;
    }

    // ------------------------------------------------------------------ Great Tree Rising
    static boolean rising(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 6 * s;
        Vec3 c = GrimoireBook.aim(p, 24);
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.TREE_FX2, c, c.add(0, 1, 0), 60, (float) r);
        SpellRuntime.later(level, 10, () -> {
            if (!p.isAlive()) return;
            b.vfx(p, VfxShape.TREE_FX3, c, c.add(0, 2, 0), 34, 1.5f);
            for (LivingEntity t : TreeKit.foes(p, c, r)) {
                b.hurt(i, p, t, mode, 15f);
                TreeKit.lift(t, 0.9);
                TreeKit.root(t, BalanceLaw.controlTicks(t, 70), 7);
            }
        });
        SpellRuntime.zone(level, 80, 10, age -> {
            if (age < 20) return;
            for (LivingEntity t : TreeKit.foes(p, c, r)) { TreeKit.root(t, 14, 5); if (age % 20 == 0) b.hurt(i, p, t, mode, 2f); }
        });
        return true;
    }

    // ------------------------------------------------------------------ Bark Armor
    static boolean barkArmor(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        TreeKit.armour(i, p, Aura.TREE, "TREE", 200, 0);
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + 200);
        tag.putFloat("EmpowerBonus", 4f);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.TREE_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.7f);
        return true;
    }

    // ------------------------------------------------------------------ Heartwood Guardian
    static boolean guardian(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = Math.min(1.4f, TreeKit.k(i, p));
        Vec3 at = p.position().add(TreeKit.forward(p).scale(2.5));
        b.castCircle(p, 1.3f);
        b.vfx(p, VfxShape.TREE_FX3, at, at.add(0, 2, 0), 30, 1.3f);
        TreeKit.spawn(b, i, p, mode, PropKind.TREE_1, TreeKit.onGround(p.serverLevel(), at), 0, 400, s, p.getYRot());
        return true;
    }

    // ------------------------------------------------------------------ Seedling Volley
    static boolean seedlings(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        Vec3 start = p.getEyePosition(), dir = p.getViewVector(1f), side = new Vec3(-dir.z, 0, dir.x).normalize();
        b.castCircle(p, 0.9f);
        for (int k = 0; k < 5; k++) {
            double off = (k - 2) * 0.12;
            Vec3 d = dir.add(side.scale(off)).add(0, Math.abs(off) * 0.4, 0).normalize();
            b.vfx(p, VfxShape.TREE_FX1, start, start.add(d.scale(14)), 14, 0.5f);
            SpellRuntime.bolt(p, start, d.scale(1.1), 0.45, 26, false, null, (bolt, t) -> { }, (bolt, at) -> {
                b.vfx(p, VfxShape.TREE_FX3, at, at.add(0, 1, 0), 22, 0.5f);
                for (LivingEntity t : TreeKit.foes(p, at, 2.2 * s)) {
                    b.hurt(i, p, t, mode, 5f);
                    TreeKit.root(t, 40, 2);
                }
            });
        }
        return true;
    }

    // ------------------------------------------------------------------ Timber Wall
    static boolean timberWall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = TreeKit.forward(p), side = new Vec3(-dir.z, 0, dir.x), base = p.position().add(dir.scale(2.5));
        ServerLevel level = p.serverLevel();
        int placed = 0;
        for (int w = -2; w <= 2; w++) for (int h = 0; h < 3; h++)
            if (TreeKit.put(level, BlockPos.containing(base.add(side.scale(w)).add(0, h, 0)), LOG, 200)) placed++;
        if (placed == 0) { GrimoireBook.fail(p, "No room for the trunks."); return false; }
        for (LivingEntity t : TreeKit.foes(p, base, 4.0)) {
            TreeKit.knock(t, p.position(), 0.9, 0.2);
            b.hurt(i, p, t, mode, 4f);
        }
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.TREE_FX2, base, base.add(0, 1, 0), 100, 2.6f);
        b.vfx(p, VfxShape.TREE_FX3, base, base.add(0, 1, 0), 24, 0.9f);
        return true;
    }

    // ------------------------------------------------------------------ Verdant Canopy
    static boolean canopy(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 7 * s;
        Vec3 c = p.position();
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.TREE_FX2, c, c.add(0, 1, 0), 200, (float) r);
        SpellRuntime.zone(p.serverLevel(), 200, 20, age -> {
            for (LivingEntity f : TreeKit.friends(p, c, r)) {
                BalanceLaw.heal(f, f.getMaxHealth() * 0.03f);
                f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0));
            }
            for (LivingEntity t : TreeKit.foes(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                if (age % 40 == 0) b.hurt(i, p, t, mode, 2f);
            }
        });
        return true;
    }
}
