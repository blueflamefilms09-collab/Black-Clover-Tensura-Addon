package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.entity.MirrorDoubleEntity;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Mirror Magic, rebuilt as a front-line attribute (0.42), after the Black Clover wiki (mirrors of every shape that reflect and
 * weaponise light, duplicates from the mirror world, instant travel between mirrors) and Tensura's clone skills (Body Double:
 * a tenth of your magic power for a clone that fights until the end and can trade places with you).
 *
 * <ul>
 *   <li><b>Power from your existence:</b> everything scales with {@link #power}: your EP (log scale), plus a little for your
 *       armour (physical and equipment points). 0.8 for a fresh player, about 1.3 at 1,000 EP, 1.6 at 100,000, 2.0 at 10 million.
 *       It sets how many mirrors and doubles you get, how many hits a mirror holds, ray damage and the size of the VFX.</li>
 *   <li><b>Mirror Array:</b> real ornate mirrors orbit you ({@link VfxShape#MIRROR_ARRAY}). Enemy shots that come within 3
 *       blocks are caught and sent back faster (each catch costs a mirror a crack). The shooter's magic is jammed
 *       ({@code tensura:silence}). A blow that would take 35% of your health, or kill you, shatters a mirror instead.</li>
 *   <li><b>Real Double:</b> tangible doubles ({@link MirrorDoubleEntity}) step out of the glass; each costs a tenth of your
 *       maximum magicules. They copy your effects, fight beside you and echo every spell you cast with a ray. Recast while
 *       sneaking to trade places with the nearest double.</li>
 * </ul>
 */
public final class MirrorWorks {
    public static final int VIOLET = 0xFFB89CFF;

    /** One player's orbiting mirrors. */
    private static final class Array { int count; int cracks; long until; }

    private static final Map<UUID, Array> ARRAYS = new HashMap<>();

    private MirrorWorks() {}

    // ---------------------------------------------------------------- power
    /** The caster's mirror power from EP (log), plus armour: 0.8 .. ~2.4. */
    public static float power(LivingEntity e) {
        double ep = 0;
        try { ep = EnergyHelper.getMaxEP(e); } catch (Throwable ignored) {}
        float p = 0.8f + (float) Math.max(0, Math.log10(Math.max(1, ep)) - 1.5) * 0.24f + e.getArmorValue() / 60f;
        return Mth.clamp(p, 0.8f, 2.4f);
    }

    /** 0, 1 or 2: the tier the power reaches (for counts). */
    public static int tier(float power) { return power >= 1.8f ? 2 : power >= 1.35f ? 1 : 0; }

    /** A Tensura status effect by name (tensura:silence, fragility, insanity...), if Tensura has it. */
    static Optional<Holder<MobEffect>> tensuraEffect(String name) {
        return BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.fromNamespaceAndPath("tensura", name)).map(h -> (Holder<MobEffect>) h);
    }

    static void tensuraEffect(LivingEntity t, String name, int ticks, int amp) {
        tensuraEffect(name).ifPresent(h -> t.addEffect(new MobEffectInstance(h, ticks, amp)));
    }

    static void mirror(ServerPlayer p, Vec3 at, Vec3 facing, int ticks, float size) {
        VfxSpawn.send(p.serverLevel(), VfxShape.MIRROR_FRAME, at, facing, VIOLET, ticks, size);
    }

    // ---------------------------------------------------------------- Mirror Array
    /** Mirror Array: 3-5 mirrors (by power) orbit you for 40 s, catching shots and shattering in place of heavy blows. */
    public static boolean mirrorArray(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float pw = power(p);
        Array a = new Array();
        a.count = 3 + tier(pw);
        a.until = p.level().getGameTime() + 800;
        ARRAYS.put(p.getUUID(), a);
        b.castCircle(p, 1.3f);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MIRROR_ARRAY, p, p.position().add(0, 1, 0), VIOLET, 800, a.count);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.4f, 1.2f);
        p.displayClientMessage(Component.literal(a.count + " mirrors turn round you.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        return true;
    }

    public static int mirrorsLeft(ServerPlayer p) {
        Array a = ARRAYS.get(p.getUUID());
        if (a == null || p.level().getGameTime() >= a.until) { ARRAYS.remove(p.getUUID()); return 0; }
        return a.count;
    }

    private static void loseMirror(ServerPlayer p, Array a, Vec3 where) {
        a.count--;
        a.cracks = 0;
        VfxSpawn.send(p.serverLevel(), VfxShape.MIRROR_SHATTER, where, where.add(0, 1, 0), VIOLET, 24, 1.1f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2f, 1.2f);
        if (a.count <= 0) {
            ARRAYS.remove(p.getUUID());
            p.displayClientMessage(Component.literal("Your last mirror breaks.").withStyle(ChatFormatting.GRAY), true);
        } else {
            VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MIRROR_ARRAY, p, p.position().add(0, 1, 0), VIOLET,
                    (int) (a.until - p.level().getGameTime()), a.count);
        }
    }

    /** Every tick: the array catches enemy shots near you and sends them back. */
    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || ARRAYS.isEmpty() || mirrorsLeft(p) == 0) return;
        Array a = ARRAYS.get(p.getUUID());
        for (Projectile pr : WikiSpells.enemyShots(p, p.getBoundingBox().inflate(3))) {
            Vec3 at = pr.position();
            if (pr.getOwner() instanceof LivingEntity shooter) {
                Vec3 back = shooter.getEyePosition().subtract(at).normalize().scale(Math.max(1.2, pr.getDeltaMovement().length() * 1.3));
                pr.setDeltaMovement(back);
                tensuraEffect(shooter, "silence", 60, 0);                               // reflected magic jams the caster's
            } else {
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.3));
            }
            pr.setOwner(p);
            pr.hurtMarked = true;
            mirror(p, at, at.add(pr.getDeltaMovement()), 10, 0.7f);
            if (++a.cracks >= 2 + tier(power(p))) { loseMirror(p, a, at); if (mirrorsLeft(p) == 0) return; }
        }
    }

    /** A heavy or lethal blow shatters a mirror instead (array), or a Full Reflection mirror turns it back. */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (p.getPersistentData().getLong("nusmp_full_reflect") > p.level().getGameTime() && e.getSource().getEntity() instanceof LivingEntity att && att != p) {
            float back = e.getAmount() * 0.6f;
            e.setAmount(e.getAmount() * 0.25f);
            att.hurt(p.damageSources().indirectMagic(p, p), back);
            mirror(p, p.getEyePosition().add(att.position().subtract(p.position()).normalize()), att.getEyePosition(), 8, 1.2f);
        }
        if (ARRAYS.isEmpty() || mirrorsLeft(p) == 0) return;
        float amount = e.getAmount();
        if (amount < p.getHealth() && amount < p.getMaxHealth() * 0.35f) return;
        e.setCanceled(true);
        loseMirror(p, ARRAYS.get(p.getUUID()), p.position().add(0, 1.2, 0));
        p.displayClientMessage(Component.literal("A mirror shatters in your place.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    // ---------------------------------------------------------------- Real Double
    public static List<MirrorDoubleEntity> doubles(ServerPlayer p) {
        List<MirrorDoubleEntity> out = new ArrayList<>();
        for (MirrorDoubleEntity d : p.serverLevel().getEntitiesOfClass(MirrorDoubleEntity.class, p.getBoundingBox().inflate(48)))
            if (d.isAlive() && p.getUUID().equals(d.getOwnerUUID())) out.add(d);
        return out;
    }

    /**
     * Real Double: 1-3 doubles (by power) step out of mirrors in front of you, each for a tenth of your maximum magicules, for
     * 60 s. Sneak-cast with doubles out: trade places with the nearest one (Body Double).
     */
    public static boolean realDouble(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        ServerLevel level = p.serverLevel();
        List<MirrorDoubleEntity> out = doubles(p);
        if (p.isShiftKeyDown() && !out.isEmpty()) {                                     // trade places
            MirrorDoubleEntity d = out.stream().min((x, y) -> Double.compare(x.distanceToSqr(p), y.distanceToSqr(p))).get();
            Vec3 me = p.position(), it = d.position();
            mirror(p, me.add(0, 1, 0), me.add(p.getLookAngle()).add(0, 1, 0), 14, 1.3f);
            mirror(p, it.add(0, 1, 0), it.add(d.getLookAngle()).add(0, 1, 0), 14, 1.3f);
            d.teleportTo(me.x, me.y, me.z);
            p.teleportTo(it.x, it.y, it.z);
            level.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.6f);
            return true;
        }
        float pw = power(p);
        int n = 1 + tier(pw) - out.size();
        if (n <= 0) { GrimoireBook.fail(p, "Your doubles are already out (" + out.size() + "). Sneak and cast to trade places."); return false; }
        var ex = TensuraStorages.getExistenceFrom(p);
        double each = EnergyHelper.getMaxMagicule(p) * 0.1;                             // Body Double: a tenth of your magic power
        if (ex != null) {
            // Magicule first; what the magicules can't cover is bridged from aura (a warrior's life-force can feed the glass too)
            n = Math.min(n, (int) Math.floor((ex.getMagicule() + ex.getAura()) / Math.max(1, each)));
            if (n <= 0) { GrimoireBook.fail(p, "You need a tenth of your magic power for each double."); return false; }
            double cost = each * n, fromMp = Math.min(cost, ex.getMagicule());
            ex.setMagicule(ex.getMagicule() - fromMp);
            if (cost > fromMp) ex.setAura(Math.max(0, ex.getAura() - (cost - fromMp)));
            ex.markDirty();
        }
        b.castCircle(p, 1.4f);
        Vec3 fwd = p.getLookAngle().multiply(1, 0, 1).normalize(), side = new Vec3(-fwd.z, 0, fwd.x);
        for (int k = 0; k < n; k++) {
            float off = n == 1 ? 0 : (k - (n - 1) / 2f) * 2.2f;
            Vec3 at = p.position().add(fwd.scale(2)).add(side.scale(off));
            mirror(p, at.add(0, 1.1, 0), at.add(0, 1.1, 0).add(fwd.scale(-1)), 30, 1.5f);
            int lifeTicks = 1200;
            SpellRuntime.later(level, 10, () -> {
                VfxSpawn.send(level, VfxShape.MIRROR_STEP, at, at.add(0, 2, 0), VIOLET, 20, 1.9f);
                MirrorDoubleEntity d = NUEntities.MIRROR_DOUBLE.get().create(level);
                if (d == null) return;
                d.moveTo(at.x, at.y, at.z, p.getYRot() + 180f, 0);
                d.setup(p, pw, lifeTicks);
                try { io.github.manasmods.tensura.entity.human.CloneEntity.copyEffects(p, d); } catch (Throwable ignored) {}
                level.addFreshEntity(d);
                for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16)))
                    if (m.getTarget() == p && level.getRandom().nextBoolean()) m.setTarget(d);   // half of them go for the double
            });
        }
        level.playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5f, 0.8f);
        return true;
    }

    /** Every spell you cast, your doubles cast with you: a ray each at your target. */
    public static void echo(ServerPlayer p) {
        List<MirrorDoubleEntity> out = doubles(p);
        if (out.isEmpty()) return;
        LivingEntity t = GrimoireBook.target(p, 32);
        for (MirrorDoubleEntity d : out) d.echo(t != null ? t : d.getTarget());
    }

    // ---------------------------------------------------------------- the other pages
    /** Reflect Refrain (0.42): a real mirror before you for 4 s that sends back 2-4 shots (by power) and jams their casters. */
    public static boolean reflectRefrain(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 0.7f);
        Vec3 pane = p.getEyePosition().add(p.getViewVector(1f).scale(1.6));
        float pw = power(p);
        mirror(p, pane, pane.add(p.getViewVector(1f)), 80, 1.1f * Math.min(1.6f, pw));
        int[] holds = {2 + tier(pw)};
        SpellRuntime.zone(p.serverLevel(), 80, 1, age -> {
            if (holds[0] <= 0) return;
            for (Projectile pr : WikiSpells.enemyShots(p, new net.minecraft.world.phys.AABB(pane, pane).inflate(1.8))) {
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.3));
                if (pr.getOwner() instanceof LivingEntity shooter) tensuraEffect(shooter, "silence", 60, 0);
                pr.setOwner(p);
                pr.hurtMarked = true;
                if (--holds[0] <= 0) {
                    VfxSpawn.send(p.serverLevel(), VfxShape.MIRROR_SHATTER, pane, pane.add(0, 1, 0), VIOLET, 24, 1f);
                    return;
                }
            }
        });
        return true;
    }

    /** Reflect Ray (0.42): a mirror opens and looses a volley of rays through it (3-5 by power), and your doubles fire too. */
    public static boolean reflectRay(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float pw = power(p);
        Vec3 look = p.getViewVector(1f), a = p.getEyePosition().add(look.scale(1.3));
        b.castCircle(p, 0.8f);
        mirror(p, a, a.add(look), 30, 0.9f);
        int shots = 3 + tier(pw);
        for (int k = 0; k < shots; k++) {
            SpellRuntime.later(p.serverLevel(), 2 + k * 4, () -> {
                Vec3 l = p.getViewVector(1f), from = p.getEyePosition().add(l.scale(1.3)), end = from.add(l.scale(24));
                b.vfx(p, VfxShape.MIRROR_RAY, from, end, 12, 0.9f);
                for (LivingEntity t : GrimoireBook.along(p, from, end, 1.0)) {
                    b.hurt(i, p, t, mode, 4.5f * pw);
                    var ex = TensuraStorages.getExistenceFrom(t);              // light that cuts to the spirit
                    if (ex != null && ex.getSpiritualHealth() > 2) { ex.setSpiritualHealth(ex.getSpiritualHealth() - 1); ex.markDirty(); }
                }
            });
        }
        return true;
    }

    /** Full Reflection (0.42): for 8 s a great mirror turns back every shot and returns 60% of every blow to whoever struck. */
    public static boolean fullReflection(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        b.castCircle(p, 1.2f);
        p.getPersistentData().putLong("nusmp_full_reflect", p.level().getGameTime() + 160);
        VfxSpawn.sendFollowing(p.serverLevel(), VfxShape.MIRROR_ARRAY, p, p.position().add(0, 1, 0), 0xFFE8DAFF, 160, 6f);
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 1));
        SpellRuntime.zone(p.serverLevel(), 160, 1, age -> {
            for (Projectile pr : WikiSpells.enemyShots(p, p.getBoundingBox().inflate(3.5))) {
                pr.setDeltaMovement(pr.getDeltaMovement().scale(-1.25));
                if (pr.getOwner() instanceof LivingEntity shooter) tensuraEffect(shooter, "silence", 80, 0);
                pr.setOwner(p);
                pr.hurtMarked = true;
            }
        });
        return true;
    }

    /** Mirror Step: step into a mirror and out of another where you look (up to 32 blocks), or into your nearest double's place. */
    public static boolean mirrorStep(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        Vec3 from = p.position(), to = GrimoireBook.aim(p, 32);
        if (!p.level().noCollision(p, p.getBoundingBox().move(to.subtract(from)))) to = to.add(0, 1, 0);
        if (!p.level().noCollision(p, p.getBoundingBox().move(to.subtract(from)))) { GrimoireBook.fail(p, "No room for a mirror there."); return false; }
        mirror(p, from.add(0, 1.1, 0), from.add(0, 1.1, 0).add(p.getLookAngle()), 16, 1.3f);
        mirror(p, to.add(0, 1.1, 0), to.add(0, 1.1, 0).subtract(p.getLookAngle()), 16, 1.3f);
        VfxSpawn.send(p.serverLevel(), VfxShape.MIRROR_STEP, to, to.add(0, 2, 0), VIOLET, 18, 1.9f);
        p.teleportTo(to.x, to.y, to.z);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5f, 1.8f);
        return true;
    }

    /** Mirrors Slash: blade-edged mirrors spin out in a wide arc before you: 9 x power damage, the cut glass leaves foes fragile. */
    public static boolean mirrorsSlash(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float pw = power(p);
        b.castCircle(p, 1f);
        Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
        for (int k = -2; k <= 2; k++) {
            float a = k * 0.4f;
            Vec3 d = new Vec3(look.x * Mth.cos(a) - look.z * Mth.sin(a), 0, look.x * Mth.sin(a) + look.z * Mth.cos(a));
            Vec3 at = p.getEyePosition().add(d.scale(2.5));
            mirror(p, at, at.add(d), 14, 0.8f);
            b.vfx(p, VfxShape.MIRROR_RAY, at, at.add(d.scale(5)), 10, 0.5f);
        }
        for (LivingEntity t : GrimoireBook.around(p, p.position(), 7)) {
            Vec3 to = t.position().subtract(p.position()).multiply(1, 0, 1).normalize();
            if (to.dot(look) < 0.4) continue;
            b.hurt(i, p, t, mode, 9f * pw);
            tensuraEffect(t, "fragility", 100, 0);
        }
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.2f, 1.4f);
        return true;
    }

    /** Mirrors Meteorite: a storm of mirrors falls on where you aim and bursts: 8 x power each, blinding glare and fragility. */
    public static boolean mirrorsMeteorite(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int mode) {
        float pw = power(p);
        Vec3 c = GrimoireBook.aim(p, 32);
        b.castCircle(p, 1.6f);
        ServerLevel level = p.serverLevel();
        int n = 6 + 2 * tier(pw);
        var r = p.getRandom();
        for (int k = 0; k < n; k++) {
            Vec3 hit = c.add((r.nextDouble() - 0.5) * 9, 0, (r.nextDouble() - 0.5) * 9);
            SpellRuntime.later(level, 4 + k * 4, () -> {
                mirror(p, hit.add(0, 9, 0), hit.add(0, 8, 0), 10, 1.4f);
                SpellRuntime.later(level, 8, () -> {
                    VfxSpawn.send(level, VfxShape.MIRROR_SHATTER, hit.add(0, 0.5, 0), hit.add(0, 1.5, 0), VIOLET, 24, 1.5f);
                    level.playSound(null, net.minecraft.core.BlockPos.containing(hit), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.3f, 0.8f);
                    for (LivingEntity t : GrimoireBook.around(p, hit, 2.5)) {
                        b.hurt(i, p, t, mode, 8f * pw);
                        t.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
                        tensuraEffect(t, "fragility", 120, 0);
                    }
                });
            });
        }
        return true;
    }
}
