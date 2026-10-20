package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.TimedModifiers;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * The Club of Rot and the Doom Hammer, from the "Magic Item Compendium I" sheet. Both are two-handed in feel (slow, heavy), both go
 * through the balance law, and both are reached from {@link ReferenceWeapons}.
 * <ul>
 *   <li><b>Club of Rot</b> (a gnarled bone club, grave linen, green ooze): every hit inflicts <i>Rot</i>, a stacking disease of up to
 *       five stacks (three on a boss): Poison, Weakness, a rotting maximum health (a transient attribute modifier that is refreshed
 *       every second and so fades on its own), and a little necrotic damage a second. Stacks fall away one by one every 5 s. A foe
 *       with three stacks spreads Rot to those within three blocks (to two stacks). Undead and golems cannot rot.
 *       Right-click: Putrid Slam, a green shockwave 5 blocks wide (two stacks, a hit) that withers the plants and crops under it, in
 *       particles only. Sneak + right-click: Plague Cloud, a cloud of spores for 6 s.</li>
 *   <li><b>Doom Hammer</b> (black iron, a glowing doom rune): every hit breaks armour (a stack of -8% armour, up to five, 8 s) and
 *       strikes again for an impact that ignores half the armour; a critical hit staggers and interrupts. Right-click: Doom Slam,
 *       a leap and a slam (dive from the air): a ring of cracks and a shockwave that hurls foes up and stuns them for a moment,
 *       bosses only staggered, the longer the fall the harder the landing. Sneak + right-click: Sentence of Doom, a mark that
 *       tolls three times and then detonates for heavy magic damage.</li>
 * </ul>
 */
final class RotDoomArts {
    private RotDoomArts() {}

    // ---------------------------------------------------------------- names, numbers, colours
    private static final ResourceLocation ROT_HP = ResourceLocation.fromNamespaceAndPath("nusmp", "rot_decay");
    private static final ResourceLocation BREAK_ARMOUR = ResourceLocation.fromNamespaceAndPath("nusmp", "doom_armour_break");
    private static final ResourceLocation BREAK_TOUGH = ResourceLocation.fromNamespaceAndPath("nusmp", "doom_toughness_break");
    private static final String ROT_N = "nusmp_rot_n", ROT_AT = "nusmp_rot_at", ROT_CHAIN = "nusmp_rot_chain";
    private static final String BRK_N = "nusmp_doom_brk_n", BRK_UNTIL = "nusmp_doom_brk_until";
    private static final String DOOM_AT = "nusmp_doom_at", DOOM_BY = "nusmp_doom_by", DOOM_BANK = "nusmp_doom_bank";

    static final int ROT_MAX = 5, ROT_STEP = 100, BREAK_MAX = 5, BREAK_TICKS = 160, DOOM_TICKS = 80;

    private static final int ROT_GREEN = 0xFF7BD148, OOZE = 0xFFA6E05A, BONE = 0xFFE8E0C0;
    private static final int EMBER = 0xFFFF6A30, DOOM_RED = 0xFFE0381C, DUST = 0xFFB8A890, IRON = 0xFFA0A6B4;

    static boolean isMine(Kind k) { return k == Kind.CLUB_OF_ROT || k == Kind.DOOM_HAMMER; }

    // ---------------------------------------------------------------- damage helpers
    /** A blow of the weapon itself: melee damage through the balance law, the hurt timer reset so it lands on top of the swing. */
    private static void strike(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p) || !t.isAlive()) return;
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), BalanceLaw.damage(p, t, raw, 0.6));
    }

    /** Necrotic and doom damage: magic damage that armour does not turn, and that does not shove the foe about. */
    private static void magic(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p) || !t.isAlive()) return;
        Vec3 v = t.getDeltaMovement();
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, raw, 0.6));
        t.invulnerableTime = 0;
        t.setDeltaMovement(v);
    }

    /** A crushing blow that ignores part of the armour (EnergyBridge.armourBypass); players still take at most the pvp cap. */
    private static void crush(ServerPlayer p, LivingEntity t, float raw, float bypass) {
        if (t == p || t.isAlliedTo(p) || !t.isAlive()) return;
        DamageSource src = p.damageSources().playerAttack(p);
        float amount = BalanceLaw.damage(p, t, raw, 0.6);
        amount *= EnergyBridge.armourBypass(t, src, amount, bypass);
        if (t instanceof Player) amount = BalanceLaw.cap(t, amount);
        t.invulnerableTime = 0;
        t.hurt(src, amount);
    }

    private static boolean critical(ServerPlayer p) { return p.fallDistance > 0 && !p.onGround() && !p.isInWater(); }

    private static void push(LivingEntity t, Vec3 from, double strength, double lift) {
        Vec3 away = t.position().subtract(from).multiply(1, 0, 1);
        if (away.lengthSqr() > 1e-4) t.knockback(strength, -away.x, -away.z);
        if (lift > 0) t.setDeltaMovement(t.getDeltaMovement().add(0, lift, 0));
        t.hurtMarked = true;
    }

    // ================================================================ Club of Rot: the Rot
    /** Living flesh rots; the dead and golems do not. */
    private static boolean canRot(LivingEntity t) {
        return !t.getType().is(EntityTypeTags.UNDEAD) && t.getType() != EntityType.IRON_GOLEM && t.getType() != EntityType.SNOW_GOLEM;
    }

    /** The stacks of Rot a foe carries now: they fall off one by one, every {@link #ROT_STEP} ticks after the last infection. */
    static int rotStacks(LivingEntity t) {
        CompoundTag d = t.getPersistentData();
        int n = d.getInt(ROT_N);
        if (n <= 0) return 0;
        long age = Math.max(0L, t.level().getGameTime() - d.getLong(ROT_AT));
        return Mth.clamp(n - (int) (age / ROT_STEP), 0, ROT_MAX);
    }

    /** Gives a foe Rot stacks, up to {@code cap}. Returns the stacks it now carries (0 if it cannot rot). */
    static int rot(ServerPlayer p, LivingEntity t, int add, int cap) {
        if (t == p || t.isAlliedTo(p) || !t.isAlive() || !canRot(t)) return 0;
        ServerLevel sl = p.serverLevel();
        CompoundTag d = t.getPersistentData();
        int old = rotStacks(t);
        int max = Math.min(cap, BalanceLaw.isBoss(t) ? 3 : ROT_MAX);
        int n = Math.max(old, Math.min(max, old + add));
        long now = sl.getGameTime();
        d.putInt(ROT_N, n);
        d.putLong(ROT_AT, now);
        fever(t, n);
        if (old == 0) {                                                       // a fresh infection starts its pulse
            d.putLong(ROT_CHAIN, now);
            SpellRuntime.later(sl, 20, () -> pulse(p, t, now));
        }
        return n;
    }

    /** What n stacks do to a foe right now: Poison, Weakness, a slow at five, and the rotting maximum health (a transient, short modifier refreshed by the pulse). */
    private static void fever(LivingEntity t, int n) {
        t.addEffect(new MobEffectInstance(MobEffects.POISON, 60, n >= 3 ? 1 : 0));
        if (n >= 2) t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, n >= 4 ? 1 : 0));
        if (n >= 5) t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 0));
        double frac = Math.min(BalanceLaw.isBoss(t) ? 0.08 : 0.20, 0.04 * n);
        TimedModifiers.apply(t, Attributes.MAX_HEALTH, ROT_HP, -frac, 50, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        if (t.getHealth() > t.getMaxHealth()) t.setHealth(t.getMaxHealth());
    }

    /** Once a second while a foe rots: the fever is renewed, the rot eats a little, and a carrier of three stacks spreads it. */
    private static void pulse(ServerPlayer p, LivingEntity t, long chain) {
        if (!t.isAlive() || t.isRemoved() || !(t.level() instanceof ServerLevel sl)) return;
        if (t.getPersistentData().getLong(ROT_CHAIN) != chain) return;               // a newer infection owns the pulse
        int n = rotStacks(t);
        if (n <= 0) return;
        fever(t, n);
        Vec3 mid = t.getBoundingBox().getCenter();
        Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, mid.x, mid.y, mid.z, 3 + n * 2, t.getBbWidth() * 0.4, t.getBbHeight() * 0.35, t.getBbWidth() * 0.4, 0.01);
        if (p.isAlive() && !p.isRemoved() && p.level() == t.level()) {
            magic(p, t, 0.3f * n);
            if (n >= 3) {
                for (LivingEntity o : GrimoireBook.around(p, t.position(), 3.2)) {
                    if (o == t || !canRot(o)) continue;
                    if (rot(p, o, 1, 2) > 0)
                        VfxSpawn.send(sl, VfxShape.THREAD_LINE, mid, o.getBoundingBox().getCenter(), ROT_GREEN, 8, 0.8f);
                }
            }
        }
        SpellRuntime.later(sl, 20, () -> pulse(p, t, chain));
    }

    /** Spore dust and a dull wet sound on a rotting wound. */
    private static void festerFx(ServerLevel sl, LivingEntity t, int n) {
        Vec3 mid = t.getBoundingBox().getCenter();
        Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, mid.x, mid.y, mid.z, 8 + n * 2, 0.3, 0.4, 0.3, 0.02);
        Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, mid.x, mid.y - 0.2, mid.z, 6, 0.25, 0.5, 0.25, 0.01);
        sl.playSound(null, t.blockPosition(), SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 0.8f, 0.55f + 0.05f * n);
    }

    // ---------------------------------------------------------------- Club of Rot: Putrid Slam
    private static boolean putridSlam(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        for (LivingEntity t : GrimoireBook.around(p, c, 5.0)) {
            strike(p, t, 7f);
            rot(p, t, 2, ROT_MAX);
            if (!BalanceLaw.isBoss(t)) push(t, c, 0.5, 0.15);
            festerFx(sl, t, 2);
        }
        VfxSpawn.send(sl, VfxShape.FUNGUS_FX3, c, c, ROT_GREEN, 28, 1.0f);
        VfxSpawn.send(sl, VfxShape.WIND_RING, c.add(0, 0.15, 0), c.add(0, 1, 0), ROT_GREEN, 22, 2.2f);
        for (int i = 0; i < 4; i++) {                                                     // the mist rolls out in four directions
            Vec3 dir = new Vec3(i == 0 ? 4.5 : i == 1 ? -4.5 : 0, 0.5, i == 2 ? 4.5 : i == 3 ? -4.5 : 0);
            VfxSpawn.send(sl, VfxShape.POISON_BREATH, c.add(0, 0.5, 0), c.add(dir), OOZE, 24, 1.0f);
        }
        witherPlants(sl, c, 5);
        sl.playSound(null, p.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.6f, 0.6f);
        sl.playSound(null, p.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 1.4f, 0.5f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CURE, SoundSource.PLAYERS, 0.8f, 0.5f);
        return true;
    }

    /** Plants, crops and leaves under the wave wither: ash and dead-bush crumbs, ring after ring as the wave passes. Particles only; no block changes. */
    private static void witherPlants(ServerLevel sl, Vec3 c, int radius) {
        BlockPos base = BlockPos.containing(c);
        for (int ring = 0; ring <= radius; ring++) {
            final int r = ring;
            SpellRuntime.later(sl, r * 2, () -> {
                int shown = 0;
                for (int dx = -radius; dx <= radius && shown < 16; dx++) {
                    for (int dz = -radius; dz <= radius && shown < 16; dz++) {
                        int d2 = dx * dx + dz * dz;
                        if (d2 < r * r || d2 >= (r + 1) * (r + 1)) continue;
                        for (int dy = 2; dy >= -2; dy--) {
                            BlockPos pos = base.offset(dx, dy, dz);
                            BlockState s = sl.getBlockState(pos);
                            if (s.getBlock() instanceof BushBlock || s.is(BlockTags.LEAVES)) {
                                Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, 6, 0.25, 0.25, 0.25, 0.05);
                                Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 5, 0.3, 0.3, 0.3, 0.02);
                                shown++;
                                break;
                            }
                        }
                    }
                }
            });
        }
    }

    // ---------------------------------------------------------------- Club of Rot: Plague Cloud
    private static boolean plagueCloud(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        Vec3 aim = p.pick(14, 1f, false).getLocation();
        BlockPos bp = BlockPos.containing(aim);
        for (int i = 0; i < 12 && sl.getBlockState(bp).isAir() && sl.getBlockState(bp.below()).isAir(); i++) bp = bp.below();
        final Vec3 c = new Vec3(aim.x, bp.getY(), aim.z);
        VfxSpawn.send(sl, VfxShape.FUNGUS_FX2, c, c, ROT_GREEN, 120, 4.5f);
        sl.playSound(null, bp, SoundEvents.SPLASH_POTION_BREAK, SoundSource.PLAYERS, 1.3f, 0.6f);
        sl.playSound(null, bp, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.PLAYERS, 1.2f, 0.5f);
        SpellRuntime.zone(sl, 120, 10, age -> {
            for (LivingEntity t : GrimoireBook.around(p, c, 4.5)) {
                if (rot(p, t, 1, 3) > 0) magic(p, t, 0.8f);
            }
            Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, c.x, c.y + 1.0, c.z, 14, 2.2, 0.8, 2.2, 0.01);
            Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.LIGHT, c.x, c.y + 1.4, c.z, 6, 2.4, 0.9, 2.4, 0.0);
            if (age % 20 == 0) sl.playSound(null, c.x, c.y, c.z, SoundEvents.SLIME_SQUISH_SMALL, SoundSource.PLAYERS, 1.0f, 0.5f);
        });
        return true;
    }

    // ================================================================ Doom Hammer: armour breaking
    static int broken(LivingEntity t) {
        CompoundTag d = t.getPersistentData();
        int n = d.getInt(BRK_N);
        return n > 0 && t.level().getGameTime() < d.getLong(BRK_UNTIL) ? Mth.clamp(n, 0, BREAK_MAX) : 0;
    }

    /** Adds stacks of broken armour (-8% armour and -6% toughness each, up to five, three on a boss) for 8 s. Returns the stacks it now carries. */
    private static int breakArmour(LivingEntity t, int add) {
        CompoundTag d = t.getPersistentData();
        int n = Math.min(BalanceLaw.isBoss(t) ? 3 : BREAK_MAX, broken(t) + add);
        d.putInt(BRK_N, n);
        d.putLong(BRK_UNTIL, t.level().getGameTime() + BREAK_TICKS);
        TimedModifiers.apply(t, Attributes.ARMOR, BREAK_ARMOUR, -0.08 * n, BREAK_TICKS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        TimedModifiers.apply(t, Attributes.ARMOR_TOUGHNESS, BREAK_TOUGH, -0.06 * n, BREAK_TICKS, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        return n;
    }

    /** A staggered foe loses its footing for a moment, drops what it was drawing and eating, and a mob stops in its tracks. */
    private static void stagger(LivingEntity t, int ticks, int amp) {
        t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, amp));
        if (t.isUsingItem()) t.stopUsingItem();
        if (t instanceof Mob m) m.getNavigation().stop();
    }

    // ---------------------------------------------------------------- Doom Hammer: Doom Slam
    private static boolean doomSlam(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        Vec3 look = p.getViewVector(1f);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        flat = flat.lengthSqr() < 1e-4 ? Vec3.ZERO : flat.normalize();
        if (p.onGround()) p.setDeltaMovement(flat.x * 0.85, 0.95, flat.z * 0.85);       // leap
        else p.setDeltaMovement(flat.x * 0.25, -1.7, flat.z * 0.25);                      // dive
        p.hurtMarked = true;
        p.fallDistance = 0;
        Vec3 at = p.position();
        VfxSpawn.send(sl, VfxShape.WIND_RING, at, at.add(0, 1, 0), DUST, 14, 1.3f);
        VfxSpawn.send(sl, VfxShape.IRON_FX1, at.add(0, 1, 0), at.add(flat.scale(3)).add(0, 3, 0), EMBER, 12, 0.7f);
        sl.playSound(null, p.blockPosition(), SoundEvents.MACE_SMASH_AIR, SoundSource.PLAYERS, 1.2f, 0.6f);
        sl.playSound(null, p.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.PLAYERS, 1.0f, 0.5f);
        leapStep(p, 0, new double[]{p.getY()}, new boolean[]{false});
        return true;
    }

    /** Watches the leap each tick: no fall damage on the way, and the slam on landing (or after 4 s, or if the leap is blocked). */
    private static void leapStep(ServerPlayer p, int age, double[] peak, boolean[] airborne) {
        if (!p.isAlive() || p.isRemoved()) return;
        peak[0] = Math.max(peak[0], p.getY());
        if (!p.onGround()) airborne[0] = true;
        boolean landed = airborne[0] && (p.onGround() || p.isInWater() || p.isInLava() || p.getAbilities().flying);
        if (landed || (!airborne[0] && age >= 12) || age >= 80) {
            slam(p, peak[0] - p.getY());
            return;
        }
        if (age < 40) p.fallDistance = 0;
        ServerLevel sl = p.serverLevel();
        if (age % 2 == 0 && airborne[0]) {                                               // an ember trail behind the hammer
            Vec3 at = p.position();
            Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.LIGHT, at.x, at.y + 0.2, at.z, 3, 0.2, 0.1, 0.2, 0.01);
            Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, at.x, at.y + 0.2, at.z, 2, 0.2, 0.1, 0.2, 0.01);
        }
        SpellRuntime.later(sl, 1, () -> leapStep(p, age + 1, peak, airborne));
    }

    private static void slam(ServerPlayer p, double drop) {
        ServerLevel sl = p.serverLevel();
        p.fallDistance = 0;
        Vec3 c = p.position();
        float scale = 1f + (float) Mth.clamp(drop * 0.07, 0.0, 1.0);
        List<LivingEntity> foes = GrimoireBook.around(p, c, 6.5);
        boolean stun = !foes.isEmpty() && BalanceLaw.beginControl(p);                   // one hard control at a time, like every other weapon
        for (LivingEntity t : foes) {
            boolean boss = BalanceLaw.isBoss(t);
            crush(p, t, 12f * scale, 0.5f);
            breakArmour(t, 2);
            double d = t.position().subtract(c).multiply(1, 0, 1).length();
            if (boss) push(t, c, 0.3, 0);
            else push(t, c, 1.3 - 0.6 * Math.min(1.0, d / 7.0), 0.85 - 0.4 * Math.min(1.0, d / 7.0));
            if (stun) {
                stagger(t, BalanceLaw.controlTicks(t, 30), 6);
                t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1));
            } else {
                stagger(t, 20, 2);
            }
            Vec3 mid = t.getBoundingBox().getCenter();
            VfxSpawn.send(sl, VfxShape.FIRE_BURST, t.position(), t.position(), EMBER, 10, 0.5f);
            Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.LIGHT, mid.x, mid.y, mid.z, 14, 0.4, 0.5, 0.4, 0.3);
        }
        // the shockwave and its ring of cracks
        VfxSpawn.send(sl, VfxShape.IRON_FX3, c, c, EMBER, 30, 1.3f + 0.3f * (scale - 1f));
        VfxSpawn.send(sl, VfxShape.WIND_RING, c.add(0, 0.15, 0), c.add(0, 1, 0), DUST, 20, 2.6f);
        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, c.add(0, 0.05, 0), c.add(0, 1, 0), DOOM_RED, 26, 2.4f);
        double a0 = p.getRandom().nextDouble() * Math.PI;
        for (int i = 0; i < 8; i++) {
            double a = a0 + i * Math.PI / 4;
            Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
            VfxSpawn.send(sl, VfxShape.EARTH_FISSURE, c.add(dir.scale(1.2)), c.add(dir.scale(6.0)), 0xFFFF7A3A, 30, 0.9f);
        }
        BlockState ground = sl.getBlockState(BlockPos.containing(c).below());
        if (!ground.isAir()) {
            Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, c.x, c.y + 0.2, c.z, 60, 2.6, 0.2, 2.6, 0.25);
        }
        Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.LIGHT, c.x, c.y + 0.4, c.z, 3, 1.2, 0.1, 1.2, 0.0);
        sl.playSound(null, p.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.8f, 0.7f);
        sl.playSound(null, p.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 1.6f, 0.5f);
        sl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.5f, 0.6f);
    }

    // ---------------------------------------------------------------- Doom Hammer: Sentence of Doom
    private static boolean sentence(ServerPlayer p) {
        LivingEntity t = GrimoireBook.target(p, 24);
        if (t == null || t == p || t.isAlliedTo(p) || !t.isAlive()) { GrimoireBook.fail(p, "The sentence needs a foe in your sights."); return false; }
        ServerLevel sl = p.serverLevel();
        CompoundTag d = t.getPersistentData();
        long at = sl.getGameTime() + DOOM_TICKS;
        d.putLong(DOOM_AT, at);
        d.putUUID(DOOM_BY, p.getUUID());
        d.putFloat(DOOM_BANK, 0f);
        VfxSpawn.sendFollowing(sl, VfxShape.DEVIL_CIRCLE, t, t.position().add(0, 1, 0), DOOM_RED, DOOM_TICKS, 1.4f);
        VfxSpawn.send(sl, VfxShape.CURSE_FX1, p.getEyePosition(), t.getBoundingBox().getCenter(), DOOM_RED, 14, 1.0f);
        sl.playSound(null, t.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.PLAYERS, 1.2f, 0.5f);
        final Vec3[] where = {t.position()};
        for (int k = 1; k <= 3; k++) {
            final int toll = k;
            SpellRuntime.later(sl, k * 20, () -> {
                if (t.getPersistentData().getLong(DOOM_AT) != at) return;
                if (t.isAlive()) where[0] = t.position();
                Vec3 w = where[0];
                sl.playSound(null, w.x, w.y, w.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0f, 0.5f);
                Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, w.x, w.y + 0.2, w.z, 14, 0.4, 0.9, 0.4, 0.03);
                Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, w.x, w.y + 1.6, w.z, 6, 0.3, 0.2, 0.3, 0.01);
                if (t instanceof ServerPlayer tp)
                    tp.displayClientMessage(Component.literal("Sentenced: " + (4 - toll) + "...").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            });
        }
        SpellRuntime.later(sl, DOOM_TICKS, () -> detonate(p, t, at, where));
        return true;
    }

    /** The sentence is carried out: heavy magic damage on the foe (more under 35% health, more for every blow landed meanwhile), half that on those near it. */
    private static void detonate(ServerPlayer p, LivingEntity t, long at, Vec3[] where) {
        CompoundTag d = t.getPersistentData();
        if (d.getLong(DOOM_AT) != at) return;                                          // overwritten by a newer sentence
        d.putLong(DOOM_AT, 0L);
        ServerLevel sl = p.serverLevel();
        float raw = 24f + Mth.clamp(d.getFloat(DOOM_BANK), 0f, 12f);
        boolean alive = t.isAlive();
        if (alive) where[0] = t.position();
        if (alive && t.getHealth() < t.getMaxHealth() * 0.35f) raw *= 1.35f;
        Vec3 c = where[0].add(0, alive ? t.getBbHeight() / 2 : 0.8, 0);
        if (alive) magic(p, t, raw);
        for (LivingEntity o : GrimoireBook.around(p, c, 3.5)) {
            if (o == t) continue;
            magic(p, o, raw * 0.5f);
            if (!BalanceLaw.isBoss(o)) push(o, c, 0.7, 0.35);
        }
        VfxSpawn.send(sl, VfxShape.CURSE_FX3, c, c, DOOM_RED, 28, 1.4f);
        VfxSpawn.send(sl, VfxShape.DEVIL_CIRCLE, where[0].add(0, 0.05, 0), where[0].add(0, 1, 0), DOOM_RED, 20, 1.6f);
        Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.DARK, c.x, c.y, c.z, 30, 0.6, 0.9, 0.6, 0.12);
        Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.LIGHT, c.x, c.y, c.z, 2, 0.4, 0.4, 0.4, 0.0);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.6f, 0.55f);
        sl.playSound(null, c.x, c.y, c.z, SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.2f, 0.4f);
        if (p.isAlive()) p.displayClientMessage(Component.literal("The sentence is carried out.").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
    }

    // ================================================================ the entry points from ReferenceWeapons
    static boolean technique(ServerPlayer p, Kind k, ItemStack stack) {
        return switch (k) {
            case CLUB_OF_ROT -> putridSlam(p);
            case DOOM_HAMMER -> doomSlam(p);
            default -> false;
        };
    }

    static WeaponArts.Alt alt(Kind k) {
        return switch (k) {
            case CLUB_OF_ROT -> new WeaponArts.Alt("Plague Cloud", "A cloud of spores 9 blocks wide settles where you aim for 6 s: everything in it gains Rot (up to 3 stacks), is poisoned and rots from within.", 1000);
            case DOOM_HAMMER -> new WeaponArts.Alt("Sentence of Doom", "Marks the foe in your sights. After 4 s, and three tolls of the bell, the sentence is carried out: heavy magic damage that armour cannot turn, more on a foe under 35% health, and every blow you land meanwhile adds to it.", 900);
            default -> null;
        };
    }

    static boolean cast(ServerPlayer p, Kind k, ItemStack stack) {
        return switch (k) {
            case CLUB_OF_ROT -> plagueCloud(p);
            case DOOM_HAMMER -> sentence(p);
            default -> false;
        };
    }

    /** The passive of every blow. */
    static void onHit(ServerPlayer p, ItemStack stack, LivingEntity t, Kind k) {
        if (!t.isAlive()) return;
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter();
        switch (k) {
            case CLUB_OF_ROT -> {
                int n = rot(p, t, critical(p) ? 2 : 1, ROT_MAX);
                if (n <= 0) return;                                                    // the dead and golems cannot rot
                magic(p, t, 2.5f);                                                     // necrotic: armour does not turn it
                festerFx(sl, t, n);
            }
            case DOOM_HAMMER -> {
                boolean crit = critical(p);
                int n = breakArmour(t, crit ? 2 : 1);
                crush(p, t, 4f + 0.6f * n, 0.5f);                                      // the sundering impact ignores half the armour
                stagger(t, crit ? 30 : 12, crit ? 2 : 1);
                CompoundTag d = t.getPersistentData();
                if (d.getLong(DOOM_AT) > sl.getGameTime() && d.hasUUID(DOOM_BY) && d.getUUID(DOOM_BY).equals(p.getUUID()))
                    d.putFloat(DOOM_BANK, Math.min(12f, d.getFloat(DOOM_BANK) + 2f));  // every blow adds to the sentence
                Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.LIGHT, mid.x, mid.y, mid.z, 8 + 2 * n, 0.3, 0.4, 0.3, 0.25);
                Fx.spray(sl, FxThemes.ITEM_ROTDOOM, FxTone.LIGHT, mid.x, mid.y, mid.z, 4, 0.2, 0.3, 0.2, 0.02);
                sl.playSound(null, t.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.7f, 0.6f + 0.05f * n);
                if (crit) VfxSpawn.send(sl, VfxShape.FIRE_BURST, t.position(), t.position(), EMBER, 10, 0.6f);
            }
            default -> { }
        }
    }

    /** The signature strike of a full swing. */
    static void fullSwing(ServerPlayer p, LivingEntity t, Kind k) {
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter();
        switch (k) {
            case CLUB_OF_ROT -> {                                                      // Festering Blow: the growths burst
                magic(p, t, 4f);
                rot(p, t, 1, ROT_MAX);
                for (LivingEntity o : GrimoireBook.around(p, t.position(), 2.5)) {
                    if (o != t) rot(p, o, 1, 2);
                }
                VfxSpawn.send(sl, VfxShape.FUNGUS_FX3, mid, t.position(), ROT_GREEN, 22, 0.55f);
                VfxSpawn.send(sl, VfxShape.BONE_FX3, mid, t.position(), BONE, 16, 0.45f);
                sl.playSound(null, t.blockPosition(), SoundEvents.SLIME_BLOCK_BREAK, SoundSource.PLAYERS, 1.1f, 0.6f);
            }
            case DOOM_HAMMER -> {                                                      // Shatterblow: a shock round the foe
                for (LivingEntity o : GrimoireBook.around(p, t.position(), 3.0)) {
                    crush(p, o, o == t ? 5f : 3.5f, 0.5f);
                    breakArmour(o, 1);
                    if (!BalanceLaw.isBoss(o) && o != t) push(o, t.position(), 0.6, 0.4);
                }
                VfxSpawn.send(sl, VfxShape.EARTH_FISSURE, t.position().add(-1.8, 0, 0), t.position().add(1.8, 0, 0), 0xFFFF7A3A, 20, 0.7f);
                VfxSpawn.send(sl, VfxShape.IRON_FX3, t.position(), t.position(), EMBER, 18, 0.6f);
                sl.playSound(null, t.blockPosition(), SoundEvents.MACE_SMASH_GROUND, SoundSource.PLAYERS, 1.2f, 0.8f);
            }
            default -> { }
        }
    }

    // ================================================================ tooltip
    static void tooltip(ItemStack stack, Kind k, List<Component> tip) {
        if (k == Kind.CLUB_OF_ROT) {
            tip.add(Component.literal("Rot: every hit adds a stack (two on a critical hit, up to five): Poison, Weakness and a rotting maximum health (-4% a stack, -8% at most on a boss) that fades one stack every 5 s. A foe with 3 stacks spreads Rot to those within 3 blocks. Undead and golems cannot rot.").withStyle(ChatFormatting.DARK_GREEN));
            tip.add(Component.literal("  Every hit also deals necrotic damage that armour does not turn").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("A gnarled femur wrapped in grave linen, studded with growths that weep green ooze. What it strikes does not heal clean.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        } else if (k == Kind.DOOM_HAMMER) {
            tip.add(Component.literal("Sundering: every hit breaks armour (-8% a stack, up to five, 8 s) and strikes again for an impact that ignores half the armour. A critical hit staggers the foe and interrupts what it is doing.").withStyle(ChatFormatting.RED));
            tip.add(Component.literal("  The bell tolls once a second during the sentence; a landed blow adds 2 to its damage (up to 12)").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("A baleful warhammer: its haft is carved from the coffin of the dead, and the rune on its face is a verdict.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
    }
}
