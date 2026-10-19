package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.aura.Aura;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.prop.PropKind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Poison Plant Magic (1.93.0 rebuild): the book of toxic plants. Canon (the search summary of the wiki page: "create and control toxic plants; its vines can skewer targets and tunnel through the
 * ground, and its plants can release a dense cloud of gaseous poison"): the spells are those three things, a trap and the antidote. The five generic pages keep their ids and order
 * (attribute_bolt, attribute_cut, attribute_field, attribute_domain, attribute_guard); five pages are appended.
 * <pre>
 *   attribute_bolt    Poison Plant Magic: Venom Thorn           a hooked thorn: pierces, poisons
 *   attribute_cut     Poison Plant Magic: Burrowing Venom Vine  a vine tunnels along the ground and erupts under everything on a line: damage, thrown up, poisoned
 *   attribute_field   Poison Plant Magic: Miasma Bloom          a bloom (prop POISON_PLANT_1, variant 0) opens at the aim point and breathes a poison cloud for 9 s
 *   attribute_domain  Poison Plant Magic: Toxic Garden          three blooms and two trap maws round the caster and a heavy poison zone for 10 s
 *   attribute_guard   Poison Plant Magic: Venomflora Armor      the Venomflora armour (Aura.POISON_PLANT): blows softened, whoever strikes is poisoned
 *   trap_maw          Poison Plant Magic: Trap Maw              a carnivorous trap (prop POISON_PLANT_1, variant 1) that snaps on the first foe to come near, again every 2.5 s, for 12 s
 *   spore_burst       Poison Plant Magic: Spore Burst           a cone of spores: blind, sick and poisoned
 *   antivenom_graft   Poison Plant Magic: Antivenom Graft       grafts an antidote on the caster and friends: harmful effects are cleansed, a little health returns
 *   pitcher_swallow   Poison Plant Magic: Pitcher Swallow       a pitcher plant swallows one foe (control, shared 12 s lock): it is held, digested and drained
 *   thorn_volley      Poison Plant Magic: Toxic Thorn Volley    five homing venom thorns
 * </pre>
 */
public final class PoisonPlantArts {
    private PoisonPlantArts() {}

    public static List<BookPage> pages() {
        return List.of(
                BookPage.starter("attribute_bolt", "Poison Plant Magic: Venom Thorn",
                        ElementBook.bolt(9f, 1.8, 0.55, 24, true, 0, VfxShape.WIKI_MAGIC_CAST, VfxShape.WIKI_MAGIC_BURST, (t, p) -> TreeKit.wound(t, 80, 1, 0.004))),
                BookPage.mid("attribute_cut", "Poison Plant Magic: Burrowing Venom Vine", PoisonPlantArts::burrow).withAnim("thrust"),
                BookPage.zone("attribute_field", "Poison Plant Magic: Miasma Bloom", PoisonPlantArts::miasma),
                BookPage.signature("attribute_domain", "Poison Plant Magic: Toxic Garden", PoisonPlantArts::garden).withCooldown(900),
                BookPage.mid("attribute_guard", "Poison Plant Magic: Venomflora Armor", PoisonPlantArts::armor),
                // 1.93.0: appended
                BookPage.mid("trap_maw", "Poison Plant Magic: Trap Maw", PoisonPlantArts::maw),
                BookPage.mid("spore_burst", "Poison Plant Magic: Spore Burst", PoisonPlantArts::spores),
                BookPage.mid("antivenom_graft", "Poison Plant Magic: Antivenom Graft", PoisonPlantArts::antivenom).withCooldown(300),
                BookPage.mid("pitcher_swallow", "Poison Plant Magic: Pitcher Swallow", PoisonPlantArts::swallow),
                BookPage.mid("thorn_volley", "Poison Plant Magic: Toxic Thorn Volley",
                        ElementBook.volley(5, 5f, 1.4, true, VfxShape.POISON_PLANT_FX1, (t, p) -> TreeKit.wound(t, 60, 1, 0.003))));
    }

    // ------------------------------------------------------------------ Burrowing Venom Vine
    static boolean burrow(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        Vec3 dir = TreeKit.forward(p), a = p.position().add(0, 0.3, 0), end = a.add(dir.scale(12 * s));
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.POISON_PLANT_FX1, a, end, 14, 1.0f);
        for (LivingEntity t : GrimoireBook.along(p, a, end, 1.2)) {
            if (Math.abs(t.getY() - p.getY()) > 3) continue;
            b.hurt(i, p, t, mode, 9f);
            TreeKit.lift(t, 0.5);
            TreeKit.wound(t, 100, 1, 0.004);
            b.vfx(p, VfxShape.POISON_PLANT_FX3, t.position(), t.position().add(0, 1, 0), 20, 0.6f);
        }
        return true;
    }

    // ------------------------------------------------------------------ Miasma Bloom
    static boolean miasma(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 c = GrimoireBook.aim(p, 18);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.POISON_PLANT_FX1, p.getEyePosition(), c, 12, 0.9f);
        b.vfx(p, VfxShape.POISON_PLANT_FX2, c, c.add(0, 1, 0), 180, 4.5f * Math.min(1.3f, TreeKit.k(i, p)));
        TreeKit.spawn(b, i, p, mode, PropKind.POISON_PLANT_1, TreeKit.onGround(p.serverLevel(), c), 0, 180, Math.min(1.3f, TreeKit.k(i, p)), p.getYRot());
        return true;
    }

    // ------------------------------------------------------------------ Toxic Garden
    static boolean garden(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        double r = 8 * s;
        Vec3 c = p.position();
        ServerLevel level = p.serverLevel();
        b.castCircle(p, 1.5f);
        b.vfx(p, VfxShape.POISON_PLANT_FX2, c, c.add(0, 1, 0), 200, (float) r);
        b.vfx(p, VfxShape.POISON_PLANT_FX3, c, c.add(0, 2, 0), 34, 1.5f);
        for (int k = 0; k < 5; k++) {
            double a = Math.toRadians(k * 72 + p.getYRot());
            Vec3 at = c.add(Math.cos(a) * r * 0.6, 0, Math.sin(a) * r * 0.6);
            TreeKit.spawn(b, i, p, mode, PropKind.POISON_PLANT_1, TreeKit.onGround(level, at), k < 3 ? 0 : 1, 200, 1.0f, (float) Math.toDegrees(a));
        }
        SpellRuntime.zone(level, 200, 20, age -> {
            for (LivingEntity t : TreeKit.foes(p, c, r)) {
                b.hurt(i, p, t, mode, 3f);
                TreeKit.wound(t, 80, 1, 0.005);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            }
        });
        return true;
    }

    // ------------------------------------------------------------------ Venomflora Armor
    static boolean armor(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        TreeKit.armour(i, p, Aura.POISON_PLANT, "POISON_PLANT", 200, 0);
        var tag = i.getOrCreateTag();
        tag.putLong("EmpowerUntil", p.level().getGameTime() + 200);
        tag.putFloat("EmpowerBonus", 4f);
        b.castCircle(p, 1f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.POISON_PLANT_FX3, p, p.position().add(0, 1, 0), b.color, 26, 0.7f);
        return true;
    }

    // ------------------------------------------------------------------ Trap Maw
    static boolean maw(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 at = GrimoireBook.aim(p, 14);
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.POISON_PLANT_FX3, at, at.add(0, 1, 0), 22, 0.7f);
        TreeKit.spawn(b, i, p, mode, PropKind.POISON_PLANT_1, TreeKit.onGround(p.serverLevel(), at), 1, 240, 1.0f, p.getYRot());
        return true;
    }

    // ------------------------------------------------------------------ Spore Burst
    static boolean spores(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float s = TreeKit.k(i, p);
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        double r = 8 * s;
        b.castCircle(p, 0.8f);
        b.vfx(p, VfxShape.POISON_PLANT_FX1, eye, eye.add(look.scale(r)), 16, 1.2f);
        b.vfx(p, VfxShape.POISON_PLANT_FX3, eye.add(look.scale(r * 0.6)), eye.add(look.scale(r * 0.6)).add(0, 1, 0), 26, 1.1f);
        for (LivingEntity t : TreeKit.foes(p, p.position(), r)) {
            if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.5) continue;
            boolean boss = BalanceLaw.isBoss(t);
            b.hurt(i, p, t, mode, 4f);
            t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, boss ? 30 : 80, 0));
            t.addEffect(new MobEffectInstance(MobEffects.CONFUSION, boss ? 40 : 100, 0));
            TreeKit.wound(t, 80, 0, 0.003);
        }
        return true;
    }

    // ------------------------------------------------------------------ Antivenom Graft
    private static final List<Holder<MobEffect>> HARMFUL = List.of(MobEffects.POISON, MobEffects.WITHER, MobEffects.WEAKNESS, MobEffects.BLINDNESS, MobEffects.MOVEMENT_SLOWDOWN,
            MobEffects.CONFUSION, MobEffects.HUNGER, MobEffects.DIG_SLOWDOWN);

    static boolean antivenom(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        double r = 6 * TreeKit.k(i, p);
        b.castCircle(p, 1f);
        b.vfx(p, VfxShape.POISON_PLANT_FX2, p.position(), p.position().add(0, 1, 0), 40, (float) r * 0.6f);
        for (LivingEntity f : TreeKit.friends(p, p.position(), r)) {
            for (Holder<MobEffect> e : HARMFUL) f.removeEffect(e);
            BalanceLaw.heal(f, f.getMaxHealth() * 0.12f);
            f.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
        }
        return true;
    }

    // ------------------------------------------------------------------ Pitcher Swallow
    static boolean swallow(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        LivingEntity t = GrimoireBook.target(p, 12);
        if (t == null) { GrimoireBook.fail(p, "No target."); return false; }
        if (!GrimoireBook.control(p)) return false;
        int hold = BalanceLaw.controlTicks(t, 70);
        Vec3 maw = p.position().add(TreeKit.forward(p).scale(2.2));
        b.castCircle(p, 0.9f);
        b.vfx(p, VfxShape.POISON_PLANT_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), 12, 1.0f);
        b.vfx(p, VfxShape.POISON_PLANT_FX2, maw, maw.add(0, 1, 0), hold, 1.6f);
        SpellRuntime.zone(p.serverLevel(), hold, 5, age -> {
            if (!t.isAlive()) return;
            TreeKit.root(t, 10, 9);
            TreeKit.pull(t, maw, 0.35);
            if (age % 10 == 0) {
                b.hurt(i, p, t, mode, 2.5f);
                TreeKit.wound(t, 40, 1, 0.004);
                EnergyBridge.drain(t, p, 0.012);
            }
        });
        return true;
    }
}
