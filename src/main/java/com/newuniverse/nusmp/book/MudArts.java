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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Mud Magic (1.93.0 rebuild): the book of mud. The five generic pages keep their ids and order (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard); they are
 * remade as Mud spells, and five pages are appended.
 * <pre>
 *   attribute_bolt    Mud Magic: Mud Ball          a heavy glob: bursts on the first foe, slows
 *   attribute_cut     Mud Magic: Mud Wave          a wave of mud along a line: cuts everything on it, throws it back, slows
 *   attribute_field   Mud Magic: Mud Pit           the ground at the aim point turns to a pit: foes sink in it, are slowed hard, weakened and pulled down for 8 s
 *   attribute_domain  Mud Magic: Bog Domain        the ground round the aim point erupts: heavy damage, thrown back, mired for a long time
 *   attribute_guard   Mud Magic: Mud Armor         the Bogplate armour (Aura.MUD): blows are softened a third, whoever strikes is slowed
 *   mud_golem         Mud Magic: Mud Golem         a two-block golem of mud fights for the caster for 20 s (prop MUD_1, variant 0)
 *   mud_spout         Mud Magic: Mud Spout         spouts of mud burst under up to four foes (prop MUD_1, variant 1): damage, thrown up
 *   mud_wall          Mud Magic: Mud Wall          a wall of packed mud, foes in front are thrown back
 *   mud_shackle       Mud Magic: Mud Shackle       mud closes round one foe's legs (control, shared 12 s lock)
 *   mud_barrage       Mud Magic: Mud Barrage       for 3 s globs of mud rain on the aim point
 * </pre>
 * Canon: the wiki page of Mud Magic could not be reached (no network in the sandbox); built from what the magic is (docs/attributes/mud_rebuild.md).
 */
public final class MudArts {
    private MudArts() {}

    private static final BlockState MUD = Blocks.PACKED_MUD.defaultBlockState();

    public static List<BookPage> pages() {
        return List.of(
                BookPage.starter("attribute_bolt", "Mud Magic: Mud Ball",
                        ElementBook.bolt(9f, 1.6, 0.6, 26, false, 1.4, VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_BURST,
                                ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 2)))),
                BookPage.mid("attribute_cut", "Mud Magic: Mud Wave",
                        ElementBook.line(10, 9, 1.3, VfxShape.WIKI_MAGIC_CAST,
                                ElementBook.all(ElementBook.knock(1.1), ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)))))
                        .withAnim("sweep"),
                BookPage.zone("attribute_field", "Mud Magic: Mud Pit", MudArts::pit),
                BookPage.signature("attribute_domain", "Mud Magic: Bog Domain", MudArts::bog).withCooldown(900),
                BookPage.mid("attribute_guard", "Mud Magic: Mud Armor", MudArts::armor),
                // 1.93.0: appended
                BookPage.signature("mud_golem", "Mud Magic: Mud Golem", MudArts::golem).withCooldown(1200).withAnim("signature"),
                BookPage.mid("mud_spout", "Mud Magic: Mud Spout", MudArts::spouts),
                BookPage.mid("mud_wall", "Mud Magic: Mud Wall", MudArts::wall),
                BookPage.mid("mud_shackle", "Mud Magic: Mud Shackle", MudArts::shackle),
                BookPage.zone("mud_barrage", "Mud Magic: Mud Barrage", MudArts::barrage));
    }

    // ------------------------------------------------------------------ Mud Pit
    static boolean pit(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 5 * s;
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.MUD_FX1, p.getEyePosition(), c, 12, 0.9f);
        b.vfx(p, VfxShape.MUD_FX2, c, c.add(0, 1, 0), 160, (float) r);
        SpellRuntime.zone(p.serverLevel(), 160, 10, age -> {
            for (LivingEntity t : TreeKit.foes(p, c, r)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                Vec3 v = t.getDeltaMovement();
                if (v.y > 0 && !BalanceLaw.isBoss(t)) { t.setDeltaMovement(v.x, v.y * 0.3 - 0.05, v.z); t.hurtMarked = true; }
                if (age % 20 == 0) b.hurt(i, p, t, mode, 2f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Bog Domain
    static boolean bog(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 7 * s;
        Vec3 c = GrimoireBook.aim(p, 22);
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.MUD_FX2, c, c.add(0, 1, 0), 140, (float) r);
        SpellRuntime.later(p.serverLevel(), 10, () -> {
            if (!p.isAlive()) return;
            b.vfx(p, VfxShape.MUD_FX3, c, c.add(0, 2, 0), 34, 1.5f);
            for (LivingEntity t : TreeKit.foes(p, c, r)) {
                b.hurt(i, p, t, mode, 15f);
                TreeKit.knock(t, c, 1.3, 0.5);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 120, 3));
            }
        });
        SpellRuntime.zone(p.serverLevel(), 120, 20, age -> {
            for (LivingEntity t : TreeKit.foes(p, c, r)) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
        });
        return true;
    }

    // ------------------------------------------------------------------ Mud Armor
    static boolean armor(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        TreeKit.armour(i, p, Aura.MUD, "MUD", 200, 1);
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + 200);
        tag.putFloat("EmpowerBonus", 5f);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MUD_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.7f);
        return true;
    }

    // ------------------------------------------------------------------ Mud Golem
    static boolean golem(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = p.position().add(TreeKit.forward(p).scale(3.0));
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.MUD_FX3, at, at.add(0, 2, 0), 30, 1.3f);
        TreeKit.spawn(b, i, p, mode, PropKind.MUD_1, TreeKit.onGround(p.serverLevel(), at), 0, 400, Math.min(1.4f, TreeKit.k(i, p)), p.getYRot());
        return true;
    }

    // ------------------------------------------------------------------ Mud Spout
    static boolean spouts(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 aim = GrimoireBook.aim(p, 18);
        List<LivingEntity> prey = new ArrayList<>(TreeKit.foes(p, aim, 8));
        prey.sort(Comparator.comparingDouble(t -> t.distanceToSqr(aim)));
        b.castCircle(p, 0.9f);
        if (prey.isEmpty()) {
            b.vfx(p, VfxShape.MUD_FX3, aim, aim.add(0, 1, 0), 22, 0.8f);
            TreeKit.spawn(b, i, p, mode, PropKind.MUD_1, TreeKit.onGround(p.serverLevel(), aim), 1, 30, 1.0f, 0f);
            return true;
        }
        for (int k = 0; k < Math.min(4, prey.size()); k++) {
            LivingEntity t = prey.get(k);
            TreeKit.spawn(b, i, p, mode, PropKind.MUD_1, TreeKit.onGround(p.serverLevel(), t.position()), 1, 30, 1.0f, 0f);
        }
        return true;
    }

    // ------------------------------------------------------------------ Mud Wall
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = TreeKit.forward(p), side = new Vec3(-dir.z, 0, dir.x), base = p.position().add(dir.scale(2.5));
        ServerLevel level = p.serverLevel();
        int placed = 0;
        for (int w = -3; w <= 3; w++) for (int h = 0; h < 3; h++)
            if (TreeKit.put(level, BlockPos.containing(base.add(side.scale(w)).add(0, h, 0)), MUD, 160)) placed++;
        if (placed == 0) { GrimoireBook.fail(p, "No room for the mud."); return false; }
        for (LivingEntity t : TreeKit.foes(p, base, 4.5)) { TreeKit.knock(t, p.position(), 0.9, 0.2); b.hurt(i, p, t, mode, 4f); }
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.MUD_FX2, base, base.add(0, 1, 0), 100, 3f);
        b.vfx(p, VfxShape.MUD_FX3, base, base.add(0, 1, 0), 24, 1.0f);
        return true;
    }

    // ------------------------------------------------------------------ Mud Shackle
    static boolean shackle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No target."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int hold = BalanceLaw.controlTicks(t, 70);
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.MUD_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 12, 0.9f);
        b.vfx(p, VfxShape.MUD_FX2, t.position(), t.position().add(0, 1, 0), hold, 1.6f);
        b.hurt(i, p, t, mode, 4f);
        TreeKit.root(t, hold, 9);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, hold, 1));
        SpellRuntime.zone(p.serverLevel(), hold, 10, age -> { if (t.isAlive()) TreeKit.root(t, 12, 9); });
        return true;
    }

    // ------------------------------------------------------------------ Mud Barrage
    static boolean barrage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 7 * s;
        Vec3 c = GrimoireBook.aim(p, 18);
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.MUD_FX2, c, c.add(0, 1, 0), 70, (float) r);
        SpellRuntime.zone(p.serverLevel(), 60, 6, age -> {
            double a = p.getRandom().nextDouble() * Math.PI * 2, d = Math.sqrt(p.getRandom().nextDouble()) * r;
            Vec3 at = c.add(Math.cos(a) * d, 0, Math.sin(a) * d);
            b.vfx(p, VfxShape.MUD_FX3, at, at.add(0, 1, 0), 18, 0.5f);
            for (LivingEntity t : TreeKit.foes(p, at, 1.8)) {
                b.hurt(i, p, t, mode, 4f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
            }
        });
        return true;
    }
}
