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
import java.util.List;

/**
 * Mucus Magic (1.93.0 rebuild). Canon (the wiki page's description, found by search: "the user generates and controls mucus, which can smother people and entrap them; the mucus also absorbs
 * physical attacks and absorbs the magic power of those caught in it"): the spells smother, entrap, absorb blows and drain magicules. The five generic pages keep their ids and order
 * (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard); five pages are appended.
 * <pre>
 *   attribute_bolt    Mucus Magic: Mucus Shot         a blob of mucus: bursts on the first foe, slows, takes a little magicule
 *   attribute_cut     Mucus Magic: Mucus Tendril      a tendril (prop MUCUS_1, variant 1) lashes along a line: cuts, slows, drags toward the caster
 *   attribute_field   Mucus Magic: Sticky Field       a field of mucus: foes inside are slowed hard and their magicules are absorbed for 8 s
 *   attribute_domain  Mucus Magic: Engulfing Tide     a tide of mucus engulfs everything round the caster: damage, slowed, silenced, drained
 *   attribute_guard   Mucus Magic: Mucus Armor        the Gelplate armour (Aura.MUCUS): blows are absorbed (a third less), feed the wearer, and drain whoever strikes
 *   mucus_construct   Mucus Magic: Mucus Construct    a mound of mucus (prop MUCUS_1, variant 0) swallows projectiles and engulfs foes for 15 s
 *   smother           Mucus Magic: Smother            mucus closes over one foe (control, shared 12 s lock): slowed, silenced, drained
 *   absorption_wall   Mucus Magic: Absorption Wall    a wall of mucus: dissolves what is shot at it and drains the casters beyond it
 *   sticky_trail      Mucus Magic: Sticky Trail       the caster leaves a trail of mucus for 6 s that slows whoever steps in it
 *   mucus_burst       Mucus Magic: Bubble Burst       bubbles of mucus drift to the aim point and burst one after another
 * </pre>
 */
public final class MucusArts {
    private MucusArts() {}

    private static final BlockState GEL = Blocks.SLIME_BLOCK.defaultBlockState();

    public static List<BookPage> pages() {
        return List.of(
                BookPage.starter("attribute_bolt", "Mucus Magic: Mucus Shot",
                        ElementBook.bolt(9f, 1.7, 0.6, 26, false, 1.3, VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_BURST,
                                ElementBook.all(ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1)), (t, p) -> EnergyBridge.drain(t, p, 0.006)))),
                BookPage.mid("attribute_cut", "Mucus Magic: Mucus Tendril", MucusArts::tendril).withAnim("sweep"),
                BookPage.zone("attribute_field", "Mucus Magic: Sticky Field", MucusArts::sticky),
                BookPage.signature("attribute_domain", "Mucus Magic: Engulfing Tide", MucusArts::tide).withCooldown(900),
                BookPage.mid("attribute_guard", "Mucus Magic: Mucus Armor", MucusArts::armor),
                // 1.93.0: appended
                BookPage.signature("mucus_construct", "Mucus Magic: Mucus Construct", MucusArts::construct).withCooldown(1000).withAnim("signature"),
                BookPage.mid("smother", "Mucus Magic: Smother", MucusArts::smother),
                BookPage.mid("absorption_wall", "Mucus Magic: Absorption Wall", MucusArts::wall),
                BookPage.mid("sticky_trail", "Mucus Magic: Sticky Trail", MucusArts::trail),
                BookPage.zone("mucus_burst", "Mucus Magic: Bubble Burst", MucusArts::bubbles));
    }

    // ------------------------------------------------------------------ Mucus Tendril
    static boolean tendril(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        Vec3 a = p.getEyePosition(), dir = p.getViewVector(1f), end = a.add(dir.scale(9 * s));
        List<LivingEntity> hit = GrimoireBook.along(p, a, end, 0.9);
        if (hit.size() > 8) hit = hit.subList(0, 8);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.MUCUS_FX1, a, end, 12, 1.0f);
        TreeKit.spawn(b, i, p, mode, PropKind.MUCUS_1, TreeKit.onGround(p.serverLevel(), end), 1, 20, 1.0f, p.getYRot());
        for (LivingEntity t : hit) {
            b.hurt(i, p, t, mode, 8f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
            TreeKit.pull(t, p.position(), 0.45);
            b.vfx(p, VfxShape.MUCUS_FX3, t.position(), t.position().add(0, 1, 0), 18, 0.5f);
        }
        return true;
    }

    // ------------------------------------------------------------------ Sticky Field
    static boolean sticky(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 5 * s;
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.MUCUS_FX1, p.getEyePosition(), c, 12, 0.9f);
        b.vfx(p, VfxShape.MUCUS_FX2, c, c.add(0, 1, 0), 160, (float) r);
        SpellRuntime.zone(p.serverLevel(), 160, 20, age -> {
            for (LivingEntity t : TreeKit.foes(p, c, r)) {
                b.hurt(i, p, t, mode, 2f);
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                EnergyBridge.drain(t, p, 0.008);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Engulfing Tide
    static boolean tide(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 7 * s;
        Vec3 c = p.position();
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.MUCUS_FX3, c, c.add(0, 2, 0), 34, 1.6f);
        b.vfx(p, VfxShape.MUCUS_FX2, c, c.add(0, 1, 0), 60, (float) r * 0.7f);
        for (LivingEntity t : TreeKit.foes(p, c, r)) {
            boolean boss = BalanceLaw.isBoss(t);
            b.hurt(i, p, t, mode, 13f);
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, boss ? 50 : 100, 3));
            EnergyBridge.effect(t, "silence", boss ? 30 : 60, 0);
            EnergyBridge.drain(t, p, 0.03);
        }
        return true;
    }

    // ------------------------------------------------------------------ Mucus Armor
    static boolean armor(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        TreeKit.armour(i, p, Aura.MUCUS, "MUCUS", 200, 0);
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + 200);
        tag.putFloat("EmpowerBonus", 3f);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MUCUS_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.7f);
        return true;
    }

    // ------------------------------------------------------------------ Mucus Construct
    static boolean construct(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = p.position().add(TreeKit.forward(p).scale(2.5));
        b.castCircle(p, 1.3f);
        b.vfx(p, VfxShape.MUCUS_FX3, at, at.add(0, 2, 0), 30, 1.2f);
        TreeKit.spawn(b, i, p, mode, PropKind.MUCUS_1, TreeKit.onGround(p.serverLevel(), at), 0, 300, Math.min(1.4f, TreeKit.k(i, p)), p.getYRot());
        return true;
    }

    // ------------------------------------------------------------------ Smother
    static boolean smother(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 14);
        if (t == null) { GrimoireBook.fail(p, "No target."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int hold = BalanceLaw.controlTicks(t, 70);
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.MUCUS_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 12, 0.9f);
        b.vfx(p, VfxShape.MUCUS_FX3, t.position(), t.position().add(0, 1, 0), hold, 1.2f);
        TreeKit.root(t, hold, 9);
        EnergyBridge.effect(t, "silence", hold, 0);
        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Math.min(hold, 60), 0));
        b.hurt(i, p, t, mode, 4f);
        SpellRuntime.zone(p.serverLevel(), hold, 10, age -> {
            if (!t.isAlive()) return;
            TreeKit.root(t, 12, 9);
            if (age % 20 == 0) { b.hurt(i, p, t, mode, 2f); EnergyBridge.drain(t, p, 0.012); }
        });
        return true;
    }

    // ------------------------------------------------------------------ Absorption Wall
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = TreeKit.forward(p), side = new Vec3(-dir.z, 0, dir.x), base = p.position().add(dir.scale(2.5));
        ServerLevel level = p.serverLevel();
        int placed = 0;
        for (int w = -2; w <= 2; w++) for (int h = 0; h < 3; h++)
            if (TreeKit.put(level, BlockPos.containing(base.add(side.scale(w)).add(0, h, 0)), GEL, 160)) placed++;
        if (placed == 0) { GrimoireBook.fail(p, "No room for the mucus."); return false; }
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.MUCUS_FX2, base, base.add(0, 1, 0), 100, 3f);
        SpellRuntime.zone(level, 160, 5, age -> {
            SpellRuntime.dissolveBolts(level, base.add(0, 1, 0), 3.2, p);
            if (age % 20 == 0) for (LivingEntity t : TreeKit.foes(p, base, 4.0)) EnergyBridge.drain(t, p, 0.006);
        });
        return true;
    }

    // ------------------------------------------------------------------ Sticky Trail
    static boolean trail(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        List<Vec3> puddles = new ArrayList<>();
        b.castCircle(p, 0.8f);
        SpellRuntime.zone(p.serverLevel(), 120, 10, age -> {
            if (puddles.size() < 12 && p.isAlive()) {
                Vec3 at = p.position();
                puddles.add(at);
                b.vfx(p, VfxShape.MUCUS_FX2, at, at.add(0, 1, 0), 70, 1.5f);
            }
            for (Vec3 at : puddles) for (LivingEntity t : TreeKit.foes(p, at, 2.0)) {
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3));
                if (age % 30 == 0) b.hurt(i, p, t, mode, 2f);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Bubble Burst
    static boolean bubbles(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        Vec3 c = GrimoireBook.aim(p, 18);
        double r = 5 * s;
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.MUCUS_FX2, c, c.add(0, 1, 0), 70, (float) r);
        ServerLevel level = p.serverLevel();
        for (int k = 0; k < 6; k++) {
            double a = p.getRandom().nextDouble() * Math.PI * 2, d = Math.sqrt(p.getRandom().nextDouble()) * r;
            Vec3 at = c.add(Math.cos(a) * d, 0, Math.sin(a) * d);
            SpellRuntime.later(level, 10 + k * 8, () -> {
                if (!p.isAlive()) return;
                b.vfx(p, VfxShape.MUCUS_FX3, at, at.add(0, 1, 0), 22, 0.7f);
                for (LivingEntity t : TreeKit.foes(p, at, 2.5)) {
                    b.hurt(i, p, t, mode, 6f);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                }
            });
        }
        return true;
    }
}
