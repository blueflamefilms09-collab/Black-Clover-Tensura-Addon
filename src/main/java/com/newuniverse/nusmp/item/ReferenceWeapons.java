package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 0.100: the seven weapons of the "Custom weapon i want added" reference sheets. Their right-click technique, sneak-click second
 * technique and full-swing strike live here and are reached from {@link MagicWeaponItem} / {@link WeaponArts} through their
 * {@code default} branches, so the older weapons are untouched. Damage goes through the balance law like every other weapon.
 * <ul>
 *   <li>Quickling's Ivy (rapier): Ensnaring Ivy roots and poisons; Thorn Flurry.</li>
 *   <li>Magebane Longsword: Dispel Surge strips buffs and jams casters; Spellbreak Ward.</li>
 *   <li>Prism Blade: Blinding Flash; Chromatic Strike (a seven-colour beam, each colour its own effect).</li>
 *   <li>Thundercleave (greatsword): Thunder Arc; Thunderstorm. Hits arc to a second foe.</li>
 *   <li>Bloodbound Katana: hits store Blood Charges (max 10); Bloodflare spends them; Crimson Reaping.</li>
 *   <li>Permafrost Sabre + Cyclone Rapier (the Boreal Blades): each strong alone, and Fusion when one is in each hand.</li>
 * </ul>
 */
final class ReferenceWeapons {
    private ReferenceWeapons() {}

    static final int BLOOD_MAX = 10;

    static boolean isReference(Kind k) {
        return switch (k) {
            case QUICKLINGS_IVY, MAGEBANE_LONGSWORD, PRISM_BLADE, THUNDERCLEAVE, BLOODBOUND_KATANA, PERMAFROST_SABRE, CYCLONE_RAPIER -> true;
            case BOLT_THROWER, CROSSBOW_OF_SWIFT_DEATH -> true;                                          // the two crossbows (BoltArts)
            default -> false;
        };
    }

    private static void hit(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p)) return;
        t.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, t, raw, 0.6));
    }

    private static boolean fusion(ServerPlayer p) {
        ItemStack a = p.getMainHandItem(), b = p.getOffhandItem();
        if (!(a.getItem() instanceof MagicWeaponItem x) || !(b.getItem() instanceof MagicWeaponItem y)) return false;
        return (x.kind == Kind.PERMAFROST_SABRE && y.kind == Kind.CYCLONE_RAPIER) || (x.kind == Kind.CYCLONE_RAPIER && y.kind == Kind.PERMAFROST_SABRE);
    }

    static int blood(ItemStack s) {
        CustomData d = s.get(DataComponents.CUSTOM_DATA);
        return d == null ? 0 : Mth.clamp(d.copyTag().getInt("BloodCharge"), 0, BLOOD_MAX);
    }

    static void setBlood(ItemStack s, int n) {
        int v = Mth.clamp(n, 0, BLOOD_MAX);
        CustomData.update(DataComponents.CUSTOM_DATA, s, tag -> tag.putInt("BloodCharge", v));
    }

    // ---------------------------------------------------------------- right-click technique
    static boolean technique(ServerPlayer p, Kind k, ItemStack stack) {
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        switch (k) {
            case QUICKLINGS_IVY -> {
                Vec3 end = eye.add(look.scale(12));
                List<LivingEntity> caught = GrimoireBook.along(p, eye, end, 1.4);
                if (caught.isEmpty()) { GrimoireBook.fail(p, "The ivy finds nothing to seize."); return false; }
                for (LivingEntity t : caught) {
                    hit(p, t, 5);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 6));         // rooted
                    t.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1));
                    VfxSpawn.send(sl, VfxShape.TREE_ROOTS, t.position(), t.position(), 0xFF6CE05A, 40, 1.6f);
                }
                VfxSpawn.send(sl, VfxShape.THREAD_LINE, eye, end, 0xFF8CFF78, 12, 1.2f);
            }
            case MAGEBANE_LONGSWORD -> {
                MagicWeaponItem.eraseProjectiles(p, p.getBoundingBox().inflate(6));
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 6)) {
                    if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.2) continue;
                    hit(p, t, 8);
                    MagicWeaponItem.stripOne(t);
                    Nullification.interfere(t, 4, 0.5f, 0.2f);
                }
                p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 0));
                VfxSpawn.send(sl, VfxShape.ANTI_MAGIC_SLASH, eye, eye.add(look.scale(6)), 0xFF2FB6AE, 16, 1.6f);
            }
            case PRISM_BLADE -> {
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 10)) {
                    hit(p, t, 7);
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
                    t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                }
                VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, p.position().add(0, 1.4, 0), p.position().add(0, 1.4, 0), 0xFFFFFFFF, 24, 3.2f);
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2f, 1.3f);
            }
            case THUNDERCLEAVE -> {
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 8)) {
                    if (t.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.15) continue;
                    hit(p, t, 14);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));                 // deafened by the thunder
                    Vec3 away = t.position().subtract(p.position()).multiply(1, 0, 1);
                    if (away.lengthSqr() > 1e-4) t.knockback(1.4, -away.x, -away.z);
                }
                MagicWeaponItem.eraseProjectiles(p, p.getBoundingBox().inflate(7));
                VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, eye, eye.add(look.scale(8)), 0xFF7FE6FF, 14, 1.8f);
                VfxSpawn.send(sl, VfxShape.WIND_SLASH, eye, eye.add(look.scale(8)), 0xFFB0F0FF, 14, 2.4f);
                sl.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.4f, 1.1f);
            }
            case BLOODBOUND_KATANA -> {
                int n = blood(stack);
                if (n <= 0) { GrimoireBook.fail(p, "The katana has drunk no blood yet."); return false; }
                setBlood(stack, 0);
                Vec3 end = eye.add(look.scale(14));
                MagicWeaponItem.eraseProjectiles(p, new AABB(eye, end).inflate(1.4));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 1.5)) hit(p, t, 6 + 2.2f * n);
                BalanceLaw.heal(p, 0.6f * n);
                VfxSpawn.send(sl, VfxShape.WIND_SLASH, eye, end, 0xFFD0183A, 18, 1.6f + n * 0.12f);
                VfxSpawn.send(sl, VfxShape.BLOOD_FX1, p.position().add(0, 1, 0), end, 0xFFD0183A, 20, 1f);
            }
            case PERMAFROST_SABRE -> {
                boolean fused = fusion(p);
                double r = fused ? 8 : 5;
                for (LivingEntity t : GrimoireBook.around(p, p.position(), r)) {
                    hit(p, t, fused ? 13 : 8);
                    t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 100, t.getTicksFrozen() + 140));
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, fused ? 3 : 1));
                    if (fused) t.setDeltaMovement(t.getDeltaMovement().add(0, 0.7, 0));
                }
                VfxSpawn.send(sl, VfxShape.WATER_RING, p.position().add(0, 0.1, 0), p.position().add(0, 1, 0), 0xFFAEEBFF, 22, (float) r * 0.45f);
                VfxSpawn.send(sl, VfxShape.WATER_BURST, p.position().add(0, 0.6, 0), p.position(), 0xFFAEEBFF, 20, 1.4f);
                if (fused) VfxSpawn.send(sl, VfxShape.WIND_TORNADO, p.position(), p.position(), 0xFFAEEBFF, 40, 3f);
            }
            case CYCLONE_RAPIER -> {
                boolean fused = fusion(p);
                Vec3 from = p.position(), dir = look.multiply(1, 0, 1);
                if (dir.lengthSqr() < 1e-4) return false;
                dir = dir.normalize();
                Vec3 dest = null;
                for (double d = fused ? 8 : 5; d >= 1; d -= 0.5) {
                    Vec3 c = from.add(dir.scale(d));
                    if (sl.noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
                }
                if (dest == null) { GrimoireBook.fail(p, "No room to spin through."); return false; }
                for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), fused ? 2.4 : 1.4)) {
                    hit(p, t, fused ? 12 : 8);
                    t.setDeltaMovement(t.getDeltaMovement().add(0, 0.65, 0));
                    t.hurtMarked = true;
                }
                p.teleportTo(dest.x, dest.y, dest.z);
                p.fallDistance = 0;
                p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 60, 1));
                VfxSpawn.send(sl, VfxShape.WIND_GALE, from.add(0, 1, 0), dest.add(0, 1, 0), 0xFF78F5E8, 14, 1.6f);
                VfxSpawn.send(sl, VfxShape.WIND_RING, from, from.add(0, 1, 0), 0xFF78F5E8, 16, 1.4f);
            }
            case BOLT_THROWER, CROSSBOW_OF_SWIFT_DEATH -> { return BoltArts.technique(p, k, stack); }
            default -> { return false; }
        }
        return true;
    }

    // ---------------------------------------------------------------- the second technique
    static WeaponArts.Alt alt(Kind k) {
        return switch (k) {
            case QUICKLINGS_IVY -> new WeaponArts.Alt("Thorn Flurry", "Three lightning thrusts into the nearest foe, each leaving a thorn of poison.", 700);
            case MAGEBANE_LONGSWORD -> new WeaponArts.Alt("Spellbreak Ward", "For 4 s a ward 5 blocks wide erases spells, jams the skills of foes and hardens allies.", 1000);
            case PRISM_BLADE -> new WeaponArts.Alt("Chromatic Strike", "A seven-colour beam 18 blocks long: every colour lands its own blow and its own affliction.", 1100);
            case THUNDERCLEAVE -> new WeaponArts.Alt("Thunderstorm", "Three lightning strikes crash down where you aim, each stunning the foes beneath it.", 1200);
            case BLOODBOUND_KATANA -> new WeaponArts.Alt("Crimson Reaping", "Spends 3 Blood Charges: a ring of blood cuts everything within 6 blocks and bleeds its magic away.", 800);
            case PERMAFROST_SABRE -> new WeaponArts.Alt("Permafrost Sweep", "A frozen crescent cut 10 blocks long: foes are chilled and slowed. With the Cyclone Rapier in the other hand it becomes a glacial gale.", 800);
            case CYCLONE_RAPIER -> new WeaponArts.Alt("Gale Lance", "A cone of cyclone wind 10 blocks long hurls foes back. With the Permafrost Sabre in the other hand the gale carries frost.", 800);
            case BOLT_THROWER, CROSSBOW_OF_SWIFT_DEATH -> BoltArts.alt(k);
            default -> null;
        };
    }

    /** The held stack of this weapon (main hand first, then off hand), for weapons that keep state in it. */
    static ItemStack held(ServerPlayer p, Kind k) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()})
            if (s.getItem() instanceof MagicWeaponItem w && w.kind == k) return s;
        return ItemStack.EMPTY;
    }

    static boolean cast(ServerPlayer p, Kind k) {
        ItemStack stack = held(p, k);
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        switch (k) {
            case QUICKLINGS_IVY -> {
                List<LivingEntity> ts = new ArrayList<>(GrimoireBook.around(p, p.position(), 8));
                ts.sort(Comparator.comparingDouble(t -> t.distanceToSqr(p)));
                if (ts.isEmpty()) { GrimoireBook.fail(p, "No one to thrust at."); return false; }
                LivingEntity t = ts.get(0);
                for (int i = 0; i < 3; i++) {
                    SpellRuntime.later(sl, i * 5, () -> {
                        if (!t.isAlive()) return;
                        hit(p, t, 4);
                        t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
                        VfxSpawn.send(sl, VfxShape.THREAD_LINE, p.getEyePosition(), t.getBoundingBox().getCenter(), 0xFF8CFF78, 6, 1f);
                    });
                }
            }
            case MAGEBANE_LONGSWORD -> {
                Vec3 c = p.position();
                VfxSpawn.send(sl, VfxShape.NIHILITY_ZONE, c, c, 0xFF2FB6AE, 80, 5f);
                SpellRuntime.zone(sl, 80, 10, age -> {
                    MagicWeaponItem.eraseProjectiles(p, new AABB(c, c).inflate(5));
                    for (LivingEntity t : GrimoireBook.around(p, c, 5)) {
                        MagicWeaponItem.stripOne(t);
                        Nullification.jamAll(t, 2, 0.2f);
                    }
                    p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 30, 1));
                });
            }
            case PRISM_BLADE -> {
                Vec3 end = eye.add(look.scale(18));
                int[] colors = {0xFFFF3030, 0xFFFF9A20, 0xFFFFE830, 0xFF50E050, 0xFF38C8FF, 0xFF4060FF, 0xFFB050FF};
                var targets = GrimoireBook.along(p, eye, end, 1.6);
                for (int i = 0; i < 7; i++) {
                    Vec3 off = new Vec3(0, (i - 3) * 0.12, 0);
                    VfxSpawn.send(sl, VfxShape.LIGHT_BLADE, eye.add(off), end.add(off), colors[i], 14, 1.0f);
                }
                for (LivingEntity t : targets) {
                    hit(p, t, 14);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                    t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                    t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                    t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 120, 0));
                    t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
                }
            }
            case THUNDERCLEAVE -> {
                Vec3 c = p.pick(24, 1f, false).getLocation();
                var rnd = p.getRandom();
                for (int i = 0; i < 3; i++) {
                    Vec3 at = c.add(rnd.nextGaussian() * 2.0, 0, rnd.nextGaussian() * 2.0);
                    SpellRuntime.later(sl, 6 + i * 10, () -> {
                        VfxSpawn.send(sl, VfxShape.LIGHTNING_GOD, at, at, 0xFF7FE6FF, 20, 3f);
                        sl.playSound(null, at.x, at.y, at.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 1.6f, 1f);
                        for (LivingEntity t : GrimoireBook.around(p, at, 3)) {
                            hit(p, t, 11);
                            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 3));
                        }
                    });
                }
            }
            case BLOODBOUND_KATANA -> {
                int n = blood(stack);
                if (n < 3) { GrimoireBook.fail(p, "Reaping needs 3 Blood Charges."); return false; }
                setBlood(stack, n - 3);
                for (LivingEntity t : GrimoireBook.around(p, p.position(), 6)) {
                    hit(p, t, 10);
                    Nullification.bleed(t, 0.04);
                    t.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
                }
                VfxSpawn.send(sl, VfxShape.SLASH_SCYTHE, p.position().add(0, 1, 0), p.position().add(look), 0xFFD0183A, 20, 6f);
            }
            case PERMAFROST_SABRE -> {
                boolean fused = fusion(p);
                Vec3 end = eye.add(look.scale(10));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, fused ? 3 : 1.8)) {
                    hit(p, t, fused ? 14 : 10);
                    t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 100, t.getTicksFrozen() + 160));
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2));
                    if (fused) t.setDeltaMovement(t.getDeltaMovement().add(look.x * 0.8, 0.5, look.z * 0.8));
                }
                VfxSpawn.send(sl, VfxShape.WIND_SLASH, eye, end, 0xFFAEEBFF, 16, fused ? 3f : 2f);
            }
            case CYCLONE_RAPIER -> {
                boolean fused = fusion(p);
                Vec3 end = eye.add(look.scale(10));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 2.6)) {
                    hit(p, t, fused ? 11 : 7);
                    t.setDeltaMovement(t.getDeltaMovement().add(look.x * 1.6, 0.4, look.z * 1.6));
                    t.hurtMarked = true;
                    if (fused) t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 100, t.getTicksFrozen() + 100));
                }
                VfxSpawn.send(sl, VfxShape.WIND_GALE, eye, end, 0xFF78F5E8, 18, 2f);
            }
            case BOLT_THROWER, CROSSBOW_OF_SWIFT_DEATH -> { return BoltArts.cast(p, k, stack); }
            default -> { return false; }
        }
        return true;
    }

    // ---------------------------------------------------------------- on hit / full swing
    static void onHit(ServerPlayer p, ItemStack stack, LivingEntity t, Kind k) {
        ServerLevel sl = p.serverLevel();
        switch (k) {
            case QUICKLINGS_IVY -> {
                if (p.fallDistance > 0 && !p.onGround() && !p.isInWater()) {              // a critical hit: the ivy ensnares for free
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 6));
                    VfxSpawn.send(sl, VfxShape.TREE_ROOTS, t.position(), t.position(), 0xFF6CE05A, 30, 1.2f);
                }
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
            }
            case MAGEBANE_LONGSWORD -> {
                MagicWeaponItem.stripOne(t);
                Nullification.interfere(t, 2, 0.35f, 0.15f);
            }
            case PRISM_BLADE -> t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0));
            case THUNDERCLEAVE -> {
                for (LivingEntity o : GrimoireBook.around(p, t.position(), 4)) {
                    if (o == t) continue;
                    hit(p, o, 3);
                    VfxSpawn.send(sl, VfxShape.FX_LIGHTNING_ARC, t.getBoundingBox().getCenter(), o.getBoundingBox().getCenter(), 0xFF7FE6FF, 8, 0.8f);
                    break;
                }
            }
            case BLOODBOUND_KATANA -> {
                int n = blood(stack);
                boolean crit = p.fallDistance > 0 && !p.onGround() && !p.isInWater();
                setBlood(stack, n + (crit ? 2 : 1));
                if (n > 0) t.hurt(p.damageSources().indirectMagic(p, p), n * 0.8f);              // each stored charge bites
                VfxSpawn.send(sl, VfxShape.BLOOD_FX1, t.getBoundingBox().getCenter(), t.position(), 0xFFD0183A, 10, 0.7f);
            }
            case PERMAFROST_SABRE -> t.setTicksFrozen(Math.min(t.getTicksRequiredToFreeze() + 100, t.getTicksFrozen() + (fusion(p) ? 90 : 55)));
            case CYCLONE_RAPIER -> {
                if (fusion(p)) t.setDeltaMovement(t.getDeltaMovement().add(0, 0.35, 0));
            }
            case BOLT_THROWER, CROSSBOW_OF_SWIFT_DEATH -> BoltArts.onHit(p, stack, t, k);
            default -> { }
        }
    }

    static void fullSwing(ServerPlayer p, LivingEntity t, Kind k) {
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter(), look = p.getViewVector(1f);
        switch (k) {
            case QUICKLINGS_IVY -> {
                t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, 4, 0.6));           // a thorn through the armour
                t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 1));
            }
            case MAGEBANE_LONGSWORD -> { hit(p, t, 4); Nullification.interfere(t, 3, 0.5f, 0.2f); }
            case PRISM_BLADE -> {
                hit(p, t, 4);
                t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mid, mid, 0xFFFFFFFF, 10, 1.4f);
            }
            case THUNDERCLEAVE -> {
                hit(p, t, 6);
                t.knockback(0.9, p.getX() - t.getX(), p.getZ() - t.getZ());
                VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, mid.add(0, 6, 0), mid, 0xFF7FE6FF, 8, 1f);
            }
            case BLOODBOUND_KATANA -> {
                hit(p, t, 4);
                Nullification.bleed(t, 0.02);
                VfxSpawn.send(sl, VfxShape.WIND_SLASH, mid.subtract(look.scale(1.2)), mid.add(look.scale(1.2)), 0xFFD0183A, 10, 1f);
            }
            case PERMAFROST_SABRE -> {
                for (LivingEntity o : GrimoireBook.around(p, t.position(), 2.5)) {
                    hit(p, o, 3);
                    o.setTicksFrozen(Math.min(o.getTicksRequiredToFreeze() + 100, o.getTicksFrozen() + 80));
                }
                VfxSpawn.send(sl, VfxShape.WATER_BURST, mid, t.position(), 0xFFAEEBFF, 12, 0.6f);
            }
            case CYCLONE_RAPIER -> {
                hit(p, t, 4);
                t.setDeltaMovement(t.getDeltaMovement().add(look.x * 0.6, 0.4, look.z * 0.6));
                t.hurtMarked = true;
            }
            case BOLT_THROWER, CROSSBOW_OF_SWIFT_DEATH -> BoltArts.fullSwing(p, t, k);
            default -> { }
        }
    }
}
