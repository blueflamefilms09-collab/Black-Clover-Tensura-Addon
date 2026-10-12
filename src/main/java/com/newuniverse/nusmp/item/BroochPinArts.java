package com.newuniverse.nusmp.item;

import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.GrimoireBook;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.item.MagicWeaponItem.Kind;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 1.3.0: the Brooch Pin (the dagger that folds into a ruby brooch, from the "Magic Item Compendium I" sheet). Reached from
 * {@link ReferenceWeapons} and {@link MagicWeaponItem}; damage goes through the balance law, and the pin's blows are magic damage so armour
 * does not turn them.
 * <ul>
 *   <li><b>Very fast attacks</b>: 3.2 a second, and the foe's hit pause is cleared after every blow so that each stab lands in full.</li>
 *   <li><b>Backstab</b> (the passive): a blow on a foe from behind, or one that is not targeting you, is a critical: the pin strikes again
 *       through the armour (about as hard as the blow itself).</li>
 *   <li><b>Piercing Pin</b> (right-click): a quick lunge of up to 7 blocks. Every foe on the line is run through (armour does not stop it)
 *       and slowed; the first is struck from behind as you land past it.</li>
 *   <li><b>Ruby Ward</b> (sneak + right-click): the brooch shatters into a ruby barrier. For 6 s it absorbs up to 40% of your health in
 *       damage (at least 14); when it breaks, or fades, the shards burst out and cut the foes within 4 blocks.</li>
 * </ul>
 */
public final class BroochPinArts {
    private BroochPinArts() {}

    static final int RUBY = 0xFFFF3358, RUBY_L = 0xFFFF7A8C, GOLD = 0xFFFFD66E, STEEL = 0xFFE6EEFF;
    static final float CRIT_RAW = 4.5f;
    static final int DASH = 7, WARD_TICKS = 120;
    private static final String K_UNTIL = "nusmp_ruby_ward_until", K_POOL = "nusmp_ruby_ward_pool";
    private static final DustParticleOptions RUBY_DUST = new DustParticleOptions(new Vector3f(0.86f, 0.09f, 0.2f), 1.3f);
    private static final DustParticleOptions RUBY_DUST_L = new DustParticleOptions(new Vector3f(1.0f, 0.45f, 0.5f), 0.9f);

    static final WeaponArts.Alt ALT = new WeaponArts.Alt("Ruby Ward",
            "The brooch shatters into a ruby barrier round you. For 6 s it absorbs damage (40% of your health, at least 14); when it breaks or fades, the shards burst out and cut every foe within 4 blocks.", 600);

    static boolean isMine(Kind k) { return k == Kind.BROOCH_PIN; }

    // ---------------------------------------------------------------- the blow
    /** An armour-piercing blow: magic damage through the balance law, the foe's hit pause cleared first so quick stabs all land. */
    private static void pierce(ServerPlayer p, LivingEntity t, float raw) {
        if (t == p || t.isAlliedTo(p) || !t.isAlive()) return;
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().indirectMagic(p, p), BalanceLaw.damage(p, t, raw, 0.6));
    }

    /** True if the wielder stands in the foe's back arc (about 140 degrees behind it). */
    private static boolean behind(ServerPlayer p, LivingEntity t) {
        Vec3 face = t.getViewVector(1f).multiply(1, 0, 1);
        Vec3 to = p.position().subtract(t.position()).multiply(1, 0, 1);
        if (face.lengthSqr() < 1e-4 || to.lengthSqr() < 1e-4) return false;
        return face.normalize().dot(to.normalize()) < -0.3;
    }

    /** True if the foe is asleep, or hunting something else (or nothing at all). */
    private static boolean distracted(ServerPlayer p, LivingEntity t) {
        if (t.isSleeping()) return true;
        return t instanceof Mob m && m.getTarget() != p;
    }

    static void onHit(ServerPlayer p, ItemStack stack, LivingEntity t) {
        ServerLevel sl = p.serverLevel();
        if (behind(p, t) || distracted(p, t)) {                                       // a critical: the pin strikes again, through the armour
            Vec3 mid = t.getBoundingBox().getCenter(), look = p.getViewVector(1f);
            pierce(p, t, CRIT_RAW);
            sl.sendParticles(ParticleTypes.CRIT, mid.x, mid.y, mid.z, 10, 0.25, 0.3, 0.25, 0.4);
            sl.sendParticles(RUBY_DUST, mid.x, mid.y, mid.z, 8, 0.25, 0.35, 0.25, 0.02);
            VfxSpawn.send(sl, VfxShape.WIND_SLASH, mid.subtract(look.scale(1.1)), mid.add(look.scale(1.1)), RUBY, 8, 0.8f);
            sl.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0f, 1.3f);
        }
        t.invulnerableTime = Math.min(t.invulnerableTime, 10);                         // very fast: the next stab, a moment later, lands in full
    }

    /** Full swing: the pin finds a joint in the armour, and drinks a little of the foe's magic. */
    static void fullSwing(ServerPlayer p, LivingEntity t) {
        ServerLevel sl = p.serverLevel();
        Vec3 mid = t.getBoundingBox().getCenter();
        pierce(p, t, 2f);
        t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0));
        Nullification.bleed(t, 0.015);
        VfxSpawn.send(sl, VfxShape.THREAD_LINE, p.getEyePosition().add(0, -0.4, 0), mid, STEEL, 6, 0.9f);
        t.invulnerableTime = Math.min(t.invulnerableTime, 10);
    }

    // ---------------------------------------------------------------- right-click: Piercing Pin
    static boolean technique(ServerPlayer p, ItemStack stack) {
        ServerLevel sl = p.serverLevel();
        Vec3 from = p.position();
        Vec3 flat = p.getViewVector(1f).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) { GrimoireBook.fail(p, "Look ahead to lunge."); return false; }
        final Vec3 dir = flat.normalize();
        Vec3 chest = from.add(0, 1, 0);
        List<LivingEntity> line = new ArrayList<>(GrimoireBook.along(p, chest, chest.add(dir.scale(DASH)), 1.3));
        line.sort(Comparator.comparingDouble(t -> t.position().subtract(from).dot(dir)));
        double want = line.isEmpty() ? DASH : Mth.clamp(line.get(0).position().subtract(from).dot(dir) + 1.9, 2.5, DASH);   // past the first foe
        Vec3 dest = null;
        for (double d = want; d >= 1; d -= 0.5) {
            Vec3 c = from.add(dir.scale(d));
            if (sl.noCollision(p, p.getBoundingBox().move(c.subtract(from)))) { dest = c; break; }
        }
        if (dest == null) { GrimoireBook.fail(p, "No room to lunge."); return false; }
        double dist = dest.subtract(from).length();
        boolean first = true;
        for (LivingEntity t : line) {
            if (t.position().subtract(from).dot(dir) > dist + 1.2) continue;
            Vec3 mid = t.getBoundingBox().getCenter();
            pierce(p, t, first ? 6f + CRIT_RAW : 6f);                                  // run through; the first from behind, as you land past it
            t.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BalanceLaw.isBoss(t) ? 10 : 40, 1));
            VfxSpawn.send(sl, VfxShape.WIND_SLASH, mid.subtract(dir.scale(1.3)).add(0, 0.2, 0), mid.add(dir.scale(1.3)), RUBY, 8, 1.0f);
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, mid, mid, RUBY_L, 8, 0.8f);
            sl.sendParticles(ParticleTypes.CRIT, mid.x, mid.y, mid.z, first ? 14 : 8, 0.3, 0.4, 0.3, 0.5);
            sl.playSound(null, t.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.2f, first ? 1.1f : 1.4f);
            first = false;
        }
        VfxSpawn.send(sl, VfxShape.CORUNDUM_FX1, from.add(0, 1.1, 0), dest.add(0, 1.1, 0), RUBY, 12, 0.8f);
        VfxSpawn.send(sl, VfxShape.THREAD_LINE, from.add(0, 1, 0), dest.add(0, 1, 0), STEEL, 10, 1.4f);
        VfxSpawn.send(sl, VfxShape.WIND_RING, from, from.add(0, 1, 0), RUBY_L, 12, 1.0f);
        p.teleportTo(dest.x, dest.y, dest.z);
        p.fallDistance = 0;
        p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 50, 1));
        VfxSpawn.send(sl, VfxShape.CORUNDUM_FX3, dest, dest.add(0, 0.6, 0), RUBY, 16, 0.7f);
        sl.sendParticles(RUBY_DUST, dest.x, dest.y + 1, dest.z, 14, 0.4, 0.5, 0.4, 0.04);
        sl.playSound(null, from.x, from.y, from.z, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.4f, 1.6f);
        sl.playSound(null, dest.x, dest.y, dest.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2f, 1.7f);
        return true;
    }

    // ---------------------------------------------------------------- sneak + right-click: Ruby Ward
    static boolean cast(ServerPlayer p) {
        ServerLevel sl = p.serverLevel();
        long now = sl.getGameTime();
        final long until = now + WARD_TICKS;
        CompoundTag d = p.getPersistentData();
        d.putLong(K_UNTIL, until);
        d.putFloat(K_POOL, Mth.clamp(p.getMaxHealth() * 0.4f, 14f, 800f));
        Vec3 c = p.position();
        VfxSpawn.sendFollowing(sl, VfxShape.CORUNDUM_FX2, p, c, RUBY, WARD_TICKS, 2.4f);
        VfxSpawn.send(sl, VfxShape.CORUNDUM_FX3, c, c.add(0, 1, 0), RUBY, 22, 1.1f);
        VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, c.add(0, 1.2, 0), c.add(0, 1.2, 0), RUBY_L, 14, 1.8f);
        sl.sendParticles(RUBY_DUST, c.x, c.y + 1, c.z, 24, 0.7, 0.8, 0.7, 0.03);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.6f, 0.8f);
        sl.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.2f, 1.7f);
        SpellRuntime.zone(sl, WARD_TICKS, 4, age -> {                                  // rubies orbiting the wearer while the ward holds
            if (!p.isAlive() || p.isRemoved() || p.getPersistentData().getLong(K_UNTIL) != until) return;
            double a = age * 0.45;
            for (int k = 0; k < 3; k++) {
                double aa = a + k * 2.094;
                sl.sendParticles(RUBY_DUST_L, p.getX() + Math.cos(aa) * 1.15, p.getY() + 0.35 + (age % 24) * 0.06, p.getZ() + Math.sin(aa) * 1.15, 1, 0, 0, 0, 0);
            }
        });
        SpellRuntime.later(sl, WARD_TICKS, () -> {                                      // it fades: the unspent barrier bursts anyway
            CompoundTag t = p.getPersistentData();
            if (t.getLong(K_UNTIL) == until && t.getFloat(K_POOL) > 0f) {
                t.putLong(K_UNTIL, 0);
                t.putFloat(K_POOL, 0f);
                shatter(p, false);
            }
        });
        return true;
    }

    /** The barrier bursts into shards: every foe within 4 blocks is cut (through armour) and shoved back. */
    private static void shatter(ServerPlayer p, boolean broke) {
        ServerLevel sl = p.serverLevel();
        Vec3 c = p.position();
        for (LivingEntity t : GrimoireBook.around(p, c, 4.0)) {
            pierce(p, t, broke ? 7f : 4f);
            Vec3 away = t.position().subtract(c).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4 && !BalanceLaw.isBoss(t)) t.knockback(broke ? 1.0 : 0.6, -away.x, -away.z);
        }
        VfxSpawn.send(sl, VfxShape.CORUNDUM_FX3, c, c.add(0, 1, 0), RUBY, 26, broke ? 1.5f : 1.1f);
        VfxSpawn.send(sl, VfxShape.BARRIER_FX3, c.add(0, 1, 0), c.add(0, 1, 0), RUBY_L, 18, 1.2f);
        sl.sendParticles(RUBY_DUST, c.x, c.y + 1, c.z, 40, 1.2, 0.9, 1.2, 0.06);
        sl.sendParticles(ParticleTypes.CRIT, c.x, c.y + 1, c.z, 20, 1.2, 0.9, 1.2, 0.5);
        sl.playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.8f, 0.7f);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.6f, 1.0f);
    }

    /** Registered on the game bus (NUSMP): while the ward holds, damage drains its pool before it reaches the wearer. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || e.getAmount() <= 0f) return;
        CompoundTag d = p.getPersistentData();
        if (p.level().getGameTime() >= d.getLong(K_UNTIL)) return;
        float pool = d.getFloat(K_POOL);
        if (pool <= 0f) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;                   // /kill and the void are not stopped
        float take = Math.min(pool, e.getAmount());
        pool -= take;
        e.setAmount(e.getAmount() - take);
        if (e.getAmount() <= 0.001f) e.setCanceled(true);
        d.putFloat(K_POOL, pool);
        ServerLevel sl = p.serverLevel();
        Vec3 at = p.position().add(0, 1.1, 0);
        VfxSpawn.send(sl, VfxShape.BARRIER_FX3, at, at.add(p.getViewVector(1f)), RUBY, 10, 0.6f);
        sl.sendParticles(RUBY_DUST, at.x, at.y, at.z, 6, 0.4, 0.5, 0.4, 0.02);
        sl.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.4f, 0.8f + sl.getRandom().nextFloat() * 0.4f);
        if (pool <= 0.01f) {                                                           // the ward is spent: it shatters
            d.putLong(K_UNTIL, 0);
            d.putFloat(K_POOL, 0f);
            shatter(p, true);
        }
    }

    // ---------------------------------------------------------------- tooltip
    static void tooltip(ItemStack stack, List<Component> tip) {
        tip.add(Component.literal("Backstab (passive)").withStyle(ChatFormatting.GOLD));
        tip.add(Component.literal("  Very fast attacks, and no pause between hits. A blow on a foe from behind, or one that is not targeting you, is a critical: the pin strikes again through the armour.").withStyle(ChatFormatting.YELLOW));
        tip.add(Component.literal("A dagger that folds into a ruby brooch, and passes for jewellery until it is drawn.").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
    }
}
