package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Vine Magic (1.93.0 rebuild): the book of living vines, thorns and tendrils. The five generic pages keep their ids and order (attribute_bolt, attribute_cut, attribute_field, attribute_domain,
 * attribute_guard); they are remade as Vine spells, and five pages are appended.
 * <pre>
 *   attribute_bolt    Vine Magic: Thorn Dart         a thorn flung on a vine: pierces, pricks with poison
 *   attribute_cut     Vine Magic: Vine Whip          a whip of vine along a line: cuts everything on it and hauls it toward the caster
 *   attribute_field   Vine Magic: Thicket            a thicket at the aim point: foes are tangled, pricked and drained for 8 s
 *   attribute_domain  Vine Magic: Verdant Maelstrom  every vine round the aim point whirls inward: foes are dragged to the middle, cut and held
 *   attribute_guard   Vine Magic: Thorn Mantle       the Thornbine armour (Aura.VINE): blows are softened, whoever strikes is rooted and pricked
 *   vine_serpent      Vine Magic: Vine Serpent       a serpent of vine hunts the nearest foe for 15 s (prop VINE_1, variant 0)
 *   vine_wall         Vine Magic: Thicket Wall       a wall of living vines rises in front of the caster, thorns tear whoever leans on it (prop VINE_1, variant 2)
 *   vine_swing        Vine Magic: Vine Swing         a vine thrown at the aim point hauls the caster there; foes at the landing are caught
 *   strangling_bloom  Vine Magic: Strangling Bloom   vines bind one foe (control, shared 12 s lock), tighten and feed on its magicules
 *   lash_barrage      Vine Magic: Lash Barrage       for 3 s whips (prop VINE_1, variant 1) lash at up to three foes at a time
 * </pre>
 * Canon: the wiki page of Vine Magic could not be reached (no network in the sandbox); the spells are built from what the magic is and named in the canon idiom (docs/attributes/vine_rebuild.md).
 */
public final class VineArts {
    private VineArts() {}

    public static List<BookPage> pages() {
        return List.of(
                BookPage.starter("attribute_bolt", "Vine Magic: Thorn Dart",
                        ElementBook.bolt(9f, 1.8, 0.55, 24, true, 0, VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_BURST,
                                (t, p) -> TreeKit.wound(t, 60, 0, 0.003))),
                BookPage.mid("attribute_cut", "Vine Magic: Vine Whip", VineArts::whip).withAnim("sweep"),
                BookPage.zone("attribute_field", "Vine Magic: Thicket", VineArts::thicket),
                BookPage.signature("attribute_domain", "Vine Magic: Verdant Maelstrom", VineArts::maelstrom).withCooldown(900),
                BookPage.mid("attribute_guard", "Vine Magic: Thorn Mantle", VineArts::mantle),
                // 1.93.0: appended
                BookPage.signature("vine_serpent", "Vine Magic: Vine Serpent", VineArts::serpent).withCooldown(1000).withAnim("signature"),
                BookPage.mid("vine_wall", "Vine Magic: Thicket Wall", VineArts::wall),
                BookPage.mid("vine_swing", "Vine Magic: Vine Swing", VineArts::swing).withAnim("up"),
                BookPage.mid("strangling_bloom", "Vine Magic: Strangling Bloom", VineArts::strangle),
                BookPage.zone("lash_barrage", "Vine Magic: Lash Barrage", VineArts::barrage));
    }

    // ------------------------------------------------------------------ Vine Whip
    static boolean whip(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        Vec3 a = p.getEyePosition(), dir = p.getViewVector(1f), end = a.add(dir.scale(10 * s));
        List<LivingEntity> hit = GrimoireBook.along(p, a, end, 0.9);
        if (hit.size() > 8) hit = hit.subList(0, 8);
        b.castCircle(p, 0.6f);
        b.vfx(p, VfxShape.VINE_FX1, a, end, 12, 1.0f);
        for (LivingEntity t : hit) {
            b.hurt(i, p, t, mode, 8f);
            TreeKit.wound(t, 40, 0, 0.003);
            TreeKit.pull(t, p.position(), 0.55);
            b.vfx(p, VfxShape.VINE_FX3, t.position(), t.position().add(0, 1, 0), 18, 0.5f);
        }
        return true;
    }

    // ------------------------------------------------------------------ Thicket
    static boolean thicket(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 5 * s;
        Vec3 c = GrimoireBook.aim(p, 20);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.VINE_FX1, p.getEyePosition(), c, 12, 0.9f);
        b.vfx(p, VfxShape.VINE_FX2, c, c.add(0, 1, 0), 160, (float) r);
        SpellRuntime.zone(p.serverLevel(), 160, 20, age -> {
            for (LivingEntity t : TreeKit.foes(p, c, r)) {
                b.hurt(i, p, t, mode, 2.5f);
                TreeKit.root(t, 40, 4);
                TreeKit.wound(t, 40, 0, 0.003);
                EnergyBridge.drain(t, p, 0.004);
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Verdant Maelstrom
    static boolean maelstrom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 8 * s;
        Vec3 c = GrimoireBook.aim(p, 22);
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 1.4f);
        b.vfx(p, VfxShape.VINE_FX2, c, c.add(0, 1, 0), 60, (float) r);
        SpellRuntime.later(level, 8, () -> {
            if (!p.isAlive()) return;
            b.vfx(p, VfxShape.VINE_FX3, c, c.add(0, 2, 0), 34, 1.5f);
        });
        SpellRuntime.zone(level, 60, 10, age -> {
            for (LivingEntity t : TreeKit.foes(p, c, r)) {
                TreeKit.pull(t, c, 0.5);
                TreeKit.root(t, 16, 3);
                if (age % 20 == 0) { b.hurt(i, p, t, mode, 6f); TreeKit.wound(t, 40, 0, 0.004); }
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Thorn Mantle
    static boolean mantle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        TreeKit.armour(i, p, Aura.VINE, "VINE", 200, 0);
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + 200);
        tag.putFloat("EmpowerBonus", 4f);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.VINE_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.7f);
        return true;
    }

    // ------------------------------------------------------------------ Vine Serpent
    static boolean serpent(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = p.position().add(TreeKit.forward(p).scale(2.0));
        b.castCircle(p, 1.2f);
        b.vfx(p, VfxShape.VINE_FX3, at, at.add(0, 1, 0), 26, 1.0f);
        TreeKit.spawn(b, i, p, mode, PropKind.VINE_1, TreeKit.onGround(p.serverLevel(), at), 0, 300, Math.min(1.4f, TreeKit.k(i, p)), p.getYRot());
        return true;
    }

    // ------------------------------------------------------------------ Thicket Wall
    static boolean wall(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 dir = TreeKit.forward(p), at = p.position().add(dir.scale(3.0));
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.VINE_FX3, at, at.add(0, 1, 0), 24, 0.9f);
        TreeKit.spawn(b, i, p, mode, PropKind.VINE_1, TreeKit.onGround(p.serverLevel(), at), 2, 200, 1.0f, p.getYRot());
        return true;
    }

    // ------------------------------------------------------------------ Vine Swing
    static boolean swing(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), to = GrimoireBook.aim(p, 22);
        Vec3 d = to.subtract(from);
        double dist = d.length();
        if (dist < 3) { GrimoireBook.fail(p, "Too close to throw a vine."); return false; }
        Vec3 flat = new Vec3(d.x, 0, d.z).normalize();
        double sp = Math.min(1.9, 0.5 + dist * 0.09);
        p.setDeltaMovement(flat.x * sp, 0.55 + Math.min(0.3, d.y * 0.02), flat.z * sp);
        p.hurtMarked = true;
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 50, 0));
        b.castCircle(p, 0.7f);
        b.vfx(p, VfxShape.VINE_FX1, from.add(0, 1, 0), to, 12, 1.0f);
        SpellRuntime.later(p.serverLevel(), 16, () -> {
            if (!p.isAlive()) return;
            Vec3 at = p.position();
            b.vfx(p, VfxShape.VINE_FX3, at, at.add(0, 1, 0), 22, 1.0f);
            for (LivingEntity t : TreeKit.foes(p, at, 3.2)) { b.hurt(i, p, t, mode, 5f); TreeKit.root(t, 40, 3); }
        });
        return true;
    }

    // ------------------------------------------------------------------ Strangling Bloom
    static boolean strangle(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 16);
        if (t == null) { GrimoireBook.fail(p, "No target."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int hold = BalanceLaw.controlTicks(t, 70);
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.VINE_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 12, 1.0f);
        b.vfx(p, VfxShape.VINE_FX2, t.position(), t.position().add(0, 1, 0), hold, 1.8f);
        TreeKit.root(t, hold, 9);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, hold, 1));
        b.hurt(i, p, t, mode, 5f);
        SpellRuntime.zone(p.serverLevel(), hold, 10, age -> {
            if (!t.isAlive()) return;
            TreeKit.root(t, 12, 9);
            if (age % 20 == 0) { b.hurt(i, p, t, mode, 2.5f); EnergyBridge.drain(t, p, 0.015); Nullification.bleed(t, 0.004); }
        });
        return true;
    }

    // ------------------------------------------------------------------ Lash Barrage
    static boolean barrage(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        b.castCircle(p, 1.1f);
        b.vfx(p, VfxShape.VINE_FX2, p.position(), p.position().add(0, 1, 0), 70, (float) (10 * s) * 0.5f);
        SpellRuntime.zone(p.serverLevel(), 60, 10, age -> {
            List<LivingEntity> prey = new ArrayList<>(TreeKit.foes(p, p.position(), 10 * s));
            prey.sort(Comparator.comparingDouble(t -> t.distanceToSqr(p)));
            for (int k = 0; k < Math.min(3, prey.size()); k++) {
                LivingEntity t = prey.get(k);
                TreeKit.spawn(b, i, p, mode, PropKind.VINE_1, t.position(), 1, 14, 1.0f, p.getYRot());
                b.hurt(i, p, t, mode, 4f);
                TreeKit.root(t, 20, 3);
            }
        });
        return true;
    }
}
