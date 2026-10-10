package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.ZagredAttacks;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.52: every Black Clover sword gets what the Genesis Demon-Slayer has (its conceptual blow on a full swing, its sneak-use
 * second technique):
 * <ul>
 *   <li><b>Full swing</b> ({@link #fullSwing}): a hit with the attack bar at 90% or more adds the weapon's signature strike on
 *       top of the normal blow (anti-magic bleed, darkness, an armour-piercing thrust, a cleave, a holy mend, a frost burst).</li>
 *   <li><b>Sneak + right-click</b> ({@link #tryAlt}): a second, heavier technique on its own cooldown (separate from the
 *       right-click technique, which is unchanged).</li>
 * </ul>
 * Damage goes through the balance law, so it scales with the wielder like the other techniques. The Demon-Slayer keeps its own.
 */
public final class WeaponArts {
    private WeaponArts() {}

    static final String K_ALT = "nusmp_weapon_alt_";
    private static final Map<UUID, Float> CHARGE = new HashMap<>();

    public record Alt(String name, String desc, int cooldown) {}

    /** The second technique of a weapon, or null (the Demon-Slayer has its own Black Meteorite). */
    public static Alt alt(Kind k) {
        return switch (k) {
            case DEMON_SLASHER_KATANA -> new Alt("Black Dash", "Dash up to 10 blocks through everything in the way: it is cut, stripped of three buffs and bled of magic.", 600);
            case MIASMA_KATANA -> new Alt("Dark Cloaked Eclipse", "Opens a field of darkness 7 blocks wide for 5 s where you aim: foes in it are blinded, slowed, hurt and have their skills jammed.", 900);
            case SPELL_FORGED_RAPIER -> new Alt("Spatial Barrage", "Blinks to up to five foes in turn and runs each one through.", 700);
            case SEVERING_GREATSWORD -> new Alt("Severing Quake", "Slams the ground: a shockwave ring races out 8 blocks, throwing foes up and slowing them.", 1200);
            case DEMON_DWELLER -> new Alt("Black Hurricane Cut", "Spins through three anti-magic sweeps that drag foes in and strip a buff each time.", 800);
            case DEMON_DESTROYER -> new Alt("Causality Collapse", "For 3 s a sphere 7 blocks wide erases spells and barriers, strips buffs and jams skills of foes, and cleanses allies.", 1200);
            case LICHT_DWELLER -> new Alt("Eon Burst", "A burst of light: allies within 9 blocks are mended and quickened, foes are burned and lit up.", 1000);
            case LICHT_DESTROYER -> new Alt("Light Verdict", "A beam 25 blocks long: everything in it loses every buff and takes a heavy blow.", 1000);
            case RIMEHEART -> new Alt("Absolute Zero", "Freezes everything within 9 blocks solid, then shatters it a second later.", 1400);
            case LAST_WORD -> new Alt("Long Sentence", "Writes a long sentence of eight glyph letters that detonates in reading order.", 1000);
            case ELSDOCIA -> new Alt("Legacy Gate", "With Key Magic summoned, opens a long spatial rift that releases stored legacy power and drains magic from foes.", 1200);
            default -> null;
        };
    }

    // ---------------------------------------------------------------- full swing
    /** Remembers how charged the swing was (called on the left click, before the hit lands). */
    public static void swing(Player p) {
        if (!p.level().isClientSide) CHARGE.put(p.getUUID(), p.getAttackStrengthScale(0.5f));
    }

    /** On a hit: if it was a full swing, the weapon's signature strike lands too. */
    public static void fullSwing(ServerPlayer p, LivingEntity t, Kind k) {
        Float c = CHARGE.remove(p.getUUID());
        if (c == null || c < 0.9f || k == Kind.DEMON_SLAYER) return;
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter(), look = p.getViewVector(1f);
        switch (k) {
            case DEMON_SLASHER_KATANA -> {
                hit(p, t, 5);
                MagicWeaponItem.stripOne(t);
                Nullification.bleed(t, 0.03);
                VfxSpawn.send(sl, VfxShape.ANTI_MAGIC_SLASH, mid.subtract(look.scale(1.2)), mid.add(look.scale(1.2)), 0xFF2A0A30, 10, 1.0f);
            }
            case MIASMA_KATANA -> {
                hit(p, t, 5);
                t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                VfxSpawn.send(sl, VfxShape.WIND_SLASH, mid.subtract(look.scale(1.2)), mid.add(look.scale(1.2)), 0xFF1A0F2A, 10, 1.0f);
            }
            case SPELL_FORGED_RAPIER -> {
                t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, 6, 0.6));         // magic goes through armour
                VfxSpawn.send(sl, VfxShape.THREAD_LINE, p.getEyePosition(), mid, 0xFFE0E8FF, 8, 0.8f);
            }
            case SEVERING_GREATSWORD -> {
                for (LivingEntity o : GrimoireBook.around(p, t.position(), 3)) {
                    hit(p, o, 5);
                    Vec3 away = o.position().subtract(p.position()).multiply(1, 0, 1);
                    if (away.lengthSqr() > 1e-4) o.knockback(0.7, -away.x, -away.z);
                }
                VfxSpawn.send(sl, VfxShape.WEAPON_CONSTRUCTS, t.position(), t.position().add(0, 1, 0), 0xFFD8E2FF, 14, 0.8f);
            }
            case DEMON_DWELLER -> {
                hit(p, t, 5);
                MagicWeaponItem.stripOne(t);
                t.knockback(0.8, p.getX() - t.getX(), p.getZ() - t.getZ());
            }
            case DEMON_DESTROYER -> {
                hit(p, t, 4);
                Nullification.interfere(t, 3, 0.5f, 0.2f);
            }
            case LICHT_DWELLER -> {
                hit(p, t, 5);
                BalanceLaw.heal(p, 2f);
            }
            case LICHT_DESTROYER -> {
                hit(p, t, 4);
                for (int n = 0; n < 2; n++) MagicWeaponItem.stripOne(t);
                for (LivingEntity o : GrimoireBook.around(p, t.position(), 2.5)) if (o != t) hit(p, o, 3);
            }
            case RIMEHEART -> {
                for (LivingEntity o : GrimoireBook.around(p, t.position(), 2.5)) {
                    hit(p, o, 3);
                    o.setTicksFrozen(Math.min(o.getTicksRequiredToFreeze() + 100, o.getTicksFrozen() + 80));
                }
                VfxSpawn.send(sl, VfxShape.WATER_BURST, mid, t.position(), 0xFF8FE9FF, 12, 0.6f);
            }
            default -> { }
        }
    }

    // ---------------------------------------------------------------- the second technique
    /** Sneak + right-click. Always handles the click (returns true) so the normal technique does not also fire. */
    public static boolean tryAlt(ServerPlayer p, Kind k) { return tryAlt(p, k, ItemStack.EMPTY); }

    /** 0.99: passes the actual held stack so offhand alt-techs spend the right sword's reserve. */
    public static boolean tryAlt(ServerPlayer p, Kind k, ItemStack stack) {
        Alt a = alt(k);
        if (a == null) return false;
        long now = p.level().getGameTime(), ready = p.getPersistentData().getLong(K_ALT + k.name());
        if (now < ready) { GrimoireBook.fail(p, a.name() + " is gathering again (" + (ready - now + 19) / 20 + "s)."); return true; }
        if (!cast(p, k, stack)) return true;
        int cd = (int) Math.round(a.cooldown() * NUGameRules.spellCooldown(p.level()));
        p.getPersistentData().putLong(K_ALT + k.name(), now + (p.isCreative() ? cd / 4 : cd));
        p.displayClientMessage(Component.literal(a.name() + "!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
        return true;
    }

    private static boolean cast(ServerPlayer p, Kind k, ItemStack stack) {
        ServerLevel sl = p.serverLevel();
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f);
        switch (k) {
            case DEMON_SLASHER_KATANA -> {
                Vec3 from = p.position(), dir = look.multiply(1, 0, 1);
                if (dir.lengthSqr() < 1e-4) return false;
                dir = dir.normalize();
                Vec3 dest = null;
                for (double d = 10; d >= 2; d -= 0.5) {
                    Vec3 c = from.add(dir.scale(d));
                    if (sl.noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
                }
                if (dest == null) { GrimoireBook.fail(p, "No room to dash."); return false; }
                MagicWeaponItem.eraseProjectiles(p, new AABB(from, dest).inflate(2));
                for (LivingEntity t : GrimoireBook.along(p, from.add(0, 1, 0), dest.add(0, 1, 0), 1.6)) {
                    hit(p, t, 10);
                    for (int n = 0; n < 3; n++) MagicWeaponItem.stripOne(t);
                    Nullification.bleed(t, 0.05);
                }
                VfxSpawn.send(sl, VfxShape.ANTI_MAGIC_SLASH, from.add(0, 1, 0), dest.add(0, 1, 0), 0xFF2A0A30, 14, 1.8f);
                p.teleportTo(dest.x, dest.y, dest.z);
                p.fallDistance = 0;
                sl.playSound(null, dest.x, dest.y, dest.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.5f, 0.6f);
                return true;
            }
            case MIASMA_KATANA -> {
                Vec3 c = p.pick(20, 1f, false).getLocation();
                VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, c, c.add(0, 1.5, 0), 0xFF5B3A8A, 100, 3f);
                sl.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.2f, 0.5f);
                SpellRuntime.zone(sl, 100, 10, age -> {
                    for (LivingEntity t : GrimoireBook.around(p, c, 7)) {
                        t.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
                        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
                        hit(p, t, 2);
                        Nullification.jamAll(t, 2, 0.2f);
                    }
                });
                return true;
            }
            case SPELL_FORGED_RAPIER -> {
                List<LivingEntity> ts = new ArrayList<>(GrimoireBook.around(p, p.position(), 12));
                ts.sort(Comparator.comparingDouble(t -> t.distanceToSqr(p)));
                if (ts.isEmpty()) { GrimoireBook.fail(p, "No one to thrust at."); return false; }
                for (int n = 0; n < Math.min(5, ts.size()); n++) {
                    LivingEntity t = ts.get(n);
                    SpellRuntime.later(sl, n * 5, () -> {
                        if (!t.isAlive() || !p.isAlive()) return;
                        Vec3 from = p.position();
                        Vec3 behind = t.position().subtract(t.getViewVector(1f).multiply(1, 0, 1).normalize().scale(1.5));
                        if (sl.noCollision(p, p.getBoundingBox().move(behind.subtract(from)))) p.teleportTo(behind.x, behind.y, behind.z);
                        hit(p, t, 7);
                        VfxSpawn.send(sl, VfxShape.THREAD_LINE, from.add(0, 1, 0), t.getBoundingBox().getCenter(), 0xFFE0E8FF, 10, 1f);
                        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, from, from.add(0, 1, 0), 0xFFB088FF, 10, 0.5f);
                        sl.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, 1.4f);
                    });
                }
                return true;
            }
            case SEVERING_GREATSWORD -> {
                Vec3 c = p.position();
                Set<UUID> struck = new HashSet<>();
                VfxSpawn.send(sl, VfxShape.WEAPON_CONSTRUCTS, c, c.add(0, 1, 0), 0xFFD8E2FF, 40, 1.6f);
                sl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.6f, 0.5f);
                for (int r = 1; r <= 4; r++) {
                    int ring = r;
                    SpellRuntime.later(sl, ring * 3, () -> {
                        for (LivingEntity t : GrimoireBook.around(p, c, ring * 2 + 1.2)) {
                            if (t.distanceToSqr(c) < (ring * 2 - 1.2) * (ring * 2 - 1.2) || !struck.add(t.getUUID())) continue;
                            hit(p, t, 12);
                            t.setDeltaMovement(t.getDeltaMovement().add(0, 0.7, 0));
                            t.hurtMarked = true;
                            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
                        }
                        double a0 = ring * 1.3;
                        for (int s = 0; s < 4; s++) {
                            double a = a0 + s * Math.PI / 2;
                            Vec3 at = c.add(Math.cos(a) * ring * 2, 0, Math.sin(a) * ring * 2);
                            VfxSpawn.send(sl, VfxShape.LIGHTNING_SPEAR, at.add(0, 8, 0), at, 0xFFD8E2FF, 6, 0.6f);
                        }
                    });
                }
                return true;
            }
            case DEMON_DWELLER -> {
                for (int n = 0; n < 3; n++) {
                    int pulse = n;
                    SpellRuntime.later(sl, n * 6, () -> {
                        if (!p.isAlive()) return;
                        for (LivingEntity t : GrimoireBook.around(p, p.position(), 5.5)) {
                            hit(p, t, 6);
                            MagicWeaponItem.stripOne(t);
                            Vec3 in = p.position().subtract(t.position()).multiply(1, 0, 1);
                            if (in.lengthSqr() > 1e-4) t.setDeltaMovement(t.getDeltaMovement().add(in.normalize().scale(0.4)));
                            t.hurtMarked = true;
                        }
                        for (int s = 0; s < 4; s++) {
                            double a = pulse * 0.8 + s * Math.PI / 2;
                            Vec3 o = p.position().add(0, 1, 0);
                            VfxSpawn.send(sl, VfxShape.ANTI_MAGIC_SLASH, o.add(Math.cos(a) * 5, 0, Math.sin(a) * 5), o.add(Math.cos(a + 1.2) * 5, 0, Math.sin(a + 1.2) * 5), 0xFF2A0A30, 8, 1.2f);
                        }
                        sl.playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 0.7f);
                    });
                }
                return true;
            }
            case DEMON_DESTROYER -> {
                Vec3 c = p.position();
                Nullification.shatterBarriers(sl, c, 7, p);
                VfxSpawn.sendFollowing(sl, VfxShape.ANTI_MAGIC_SLASH, p, c.add(0, 1, 0).add(look), 0xFF2A0A30, 60, 2.0f);
                SpellRuntime.zone(sl, 60, 10, age -> {
                    MagicWeaponItem.eraseProjectiles(p, new AABB(c, c).inflate(7));
                    for (LivingEntity t : GrimoireBook.around(p, c, 7)) {
                        for (int n = 0; n < 2; n++) MagicWeaponItem.stripOne(t);
                        Nullification.interfere(t, 2, 0.6f, 0.2f);
                    }
                    for (Player a : sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(7), x -> x == p || x.isAlliedTo(p)))
                        for (MobEffectInstance e : new ArrayList<>(a.getActiveEffects())) if (!e.getEffect().value().isBeneficial()) a.removeEffect(e.getEffect());
                });
                return true;
            }
            case LICHT_DWELLER -> {
                Vec3 c = p.position();
                for (Player a : sl.getEntitiesOfClass(Player.class, new AABB(c, c).inflate(9), x -> x == p || x.isAlliedTo(p))) {
                    BalanceLaw.heal(a, 6f);
                    a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 1));
                }
                for (LivingEntity t : GrimoireBook.around(p, c, 9)) {
                    hit(p, t, 10);
                    t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                }
                VfxSpawn.send(sl, VfxShape.WIND_SLASH, c.add(0, 1, -9), c.add(0, 1, 9), 0xFFF4F8FF, 18, 2.2f);
                VfxSpawn.send(sl, VfxShape.WIND_SLASH, c.add(-9, 1, 0), c.add(9, 1, 0), 0xFFF4F8FF, 18, 2.2f);
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2f, 1.2f);
                return true;
            }
            case LICHT_DESTROYER -> {
                Vec3 end = eye.add(look.scale(25));
                MagicWeaponItem.eraseProjectiles(p, new AABB(eye, end).inflate(2));
                for (LivingEntity t : GrimoireBook.along(p, eye, end, 2.0)) {
                    hit(p, t, 16);
                    for (var e : new ArrayList<>(t.getActiveEffects())) if (e.getEffect().value().isBeneficial()) t.removeEffect(e.getEffect());
                    t.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                }
                VfxSpawn.send(sl, VfxShape.WIND_SLASH, eye, end, 0xFFFFF4C8, 20, 2.6f);
                sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 2f, 0.8f);
                return true;
            }
            case RIMEHEART -> {
                Vec3 c = p.position();
                List<LivingEntity> ts = GrimoireBook.around(p, c, 9);
                for (LivingEntity t : ts) {
                    hit(p, t, 10);
                    t.setTicksFrozen(t.getTicksRequiredToFreeze() + 200);
                    t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 3));
                }
                VfxSpawn.send(sl, VfxShape.WATER_BURST, c.add(0, 0.6, 0), c, 0xFF8FE9FF, 24, 2.4f);
                VfxSpawn.send(sl, VfxShape.WATER_RING, c.add(0, 0.1, 0), c.add(0, 1, 0), 0xFF3FE9FF, 22, 2.4f);
                SpellRuntime.later(sl, 20, () -> {
                    for (LivingEntity t : ts) {
                        if (!t.isAlive()) continue;
                        hit(p, t, 8);
                        Vec3 away = t.position().subtract(c).multiply(1, 0, 1);
                        if (away.lengthSqr() > 1e-4) t.knockback(0.8, -away.x, -away.z);
                        VfxSpawn.send(sl, VfxShape.WATER_BURST, t.getBoundingBox().getCenter(), t.position(), 0xFFFFFFFF, 10, 0.6f);
                    }
                    sl.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 2f, 0.6f);
                });
                return true;
            }
            case LAST_WORD -> {
                List<Vec3> pts = ZagredAttacks.plan(p, 8);
                ZagredAttacks.telegraph(p, pts);
                ZagredAttacks.redact(p, 2.4f, pts, 14);
                return true;
            }
            case ELSDOCIA -> {                                                          // 0.99: the Key Magic gate
                if (!com.newuniverse.nusmp.book.GrimoireSummon.isFloating(p, com.newuniverse.nusmp.blackclover.MagicType.KEY)) {
                    GrimoireBook.fail(p, "Summon your Key Magic grimoire to shape Elsdocia's gate.");
                    return false;
                }
                // use the stack the alt was actually invoked with (MagicWeaponItem.use passes the held hand's);
                // fall back to the main/offhand Elsdocia for the legacy no-stack caller
                ItemStack sword = stack.getItem() instanceof MagicWeaponItem w && w.kind == Kind.ELSDOCIA ? stack : ItemStack.EMPTY;
                if (sword.isEmpty()) {
                    if (p.getMainHandItem().getItem() instanceof MagicWeaponItem main && main.kind == Kind.ELSDOCIA) sword = p.getMainHandItem();
                    else if (p.getOffhandItem().getItem() instanceof MagicWeaponItem off && off.kind == Kind.ELSDOCIA) sword = p.getOffhandItem();
                }
                if (sword.isEmpty()) { GrimoireBook.fail(p, "Hold Elsdocia to open its Key Magic gate."); return false; }
                var data = sword.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                int charge = data == null ? 0 : Math.max(0, Math.min(MagicWeaponItem.ELSDOCIA_MAX, data.copyTag().getInt("ElsdociaCharge")));
                if (charge < 100) { GrimoireBook.fail(p, "Elsdocia needs at least 100 stored magic to open a gate."); return false; }
                int spent = Math.max(100, charge / 2);
                net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, sword,
                        tag -> tag.putInt("ElsdociaCharge", charge - spent));
                Vec3 end = eye.add(look.scale(32));
                MagicWeaponItem.eraseProjectiles(p, new AABB(eye, end).inflate(2));
                for (LivingEntity target : GrimoireBook.along(p, eye, end, 2.2)) {
                    hit(p, target, 12 + spent * 0.025f);
                    var existence = TensuraStorages.getExistenceFrom(target);
                    if (existence != null) {
                        double max = EnergyHelper.getMaxMagicule(target);
                        if (max > 0) {
                            double drain = Math.min(existence.getMagicule(), max * 0.04);
                            existence.setMagicule(existence.getMagicule() - drain);
                            existence.markDirty();
                        }
                    }
                }
                VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, eye, end, 0xFFFFD66E, 28, 1.6f + spent / 900f);
                return true;
            }
            default -> { return false; }
        }
    }

    private static void hit(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p)) return;
        t.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, t, raw, 0.6));
    }
}
