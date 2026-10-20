package com.newuniverse.nusmp.relics.wave;


import com.newuniverse.nusmp.relics.ActCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.pair.PairHooks;
import com.newuniverse.nusmp.relics.pair.PairRules;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.phys.Vec3;
import com.newuniverse.nusmp.particle.fx.FxRelic;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.particle.fx.FxTone;

/**
 * The light, quick weapons of wave 12.
 * <ul>
 *   <li>White Whip of Welting: a snap with reach; a wet towel stings more and humiliates (weakness).</li>
 *   <li>Whirling Blades: every blow is struck by both swords (cold and hot in turn); the spin attack hits everything around you.</li>
 *   <li>Twin Hook Swords: Sweep the Leg hooks a two-legged or winged foe off its feet; the hooks hold you to a wall you push into.</li>
 * </ul>
 */
final class Wave12Light {
    private Wave12Light() {}

    static final String WHIP = "white_whip_of_welting", BLADES = "whirling_blades", HOOKS = "twin_hook_swords";
    static final String WHIP_FLAG = "w12_white_whip_of_welting", BLADES_FLAG = "w12_whirling_blades", HOOKS_FLAG = "w12_twin_hook_swords";

    // ================================================================ White Whip of Welting
    static boolean wet(ServerPlayer p) {
        return Wave12Data.hasPlayerData(p) && Wave12Data.player(p).getLong("wet_until") >= p.level().getGameTime();
    }

    /** Once a second while the whip is held: standing in water or rain wets the towel for a minute. */
    static void whipTick(ServerPlayer p) {
        if (p.isInWater() || p.level().isRainingAt(p.blockPosition())) Wave12Data.player(p).putLong("wet_until", p.level().getGameTime() + 1200);
    }

    static void snap(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave5Aim.pickFoe(p, 6.0);
        ServerLevel l = p.serverLevel();
        Wave5Aim.sound(l, p.position(), SoundEvents.PLAYER_ATTACK_CRIT, 0.9f, 1.6f);
        p.swing(InteractionHand.MAIN_HAND, true);
        if (t == null) {
            Wave5Aim.msg(p, "The towel cracks on empty air.", ChatFormatting.GRAY);
            return;
        }
        Wave5Aim.beam(l, Wave5Aim.muzzle(p), t.getBoundingBox().getCenter(), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_12, FxTone.LIGHT), 10);
        Wave0Util.extra(p, t, 4.0f, "minecraft:player_attack");                        // 1d6 bludgeoning, wound tight
        if (wet(p)) humiliate(p, t);
    }

    /** The wet towel's sting (the Wisdom save of the card is not rolled: a stinging, shaming lash, 1d4 of mind damage). */
    static void humiliate(ServerPlayer p, LivingEntity t) {
        Wave0Util.extra(p, t, 2.0f, "minecraft:magic");
        Fx.addEffect(t, "minecraft:weakness", 80, 0);
        Wave5Aim.puff(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.8, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_12, FxTone.LIGHT), 10, 0.3);
    }

    static void soak(ActCtx c) {
        ServerPlayer p = c.player();
        if (!waterNear(p)) {
            Wave5Aim.msg(p, "No water to soak it in (stand in water, in rain, or beside a pool).", ChatFormatting.GRAY);
            return;
        }
        Wave12Data.player(p).putLong("wet_until", p.level().getGameTime() + 2400);
        Wave5Aim.sound(p.serverLevel(), p.position(), SoundEvents.PLAYER_SPLASH, 0.6f, 1.2f);
        Wave5Aim.msg(p, "The towel is soaked for two minutes.", ChatFormatting.AQUA);
    }

    /** In water, in rain, or within three blocks of a water block. */
    static boolean waterNear(ServerPlayer p) {
        if (p.isInWater() || p.level().isRainingAt(p.blockPosition())) return true;
        BlockPos c = p.blockPosition();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (p.level().getFluidState(c.offset(dx, dy, dz)).is(FluidTags.WATER)) return true;
                }
            }
        }
        return false;
    }

    // ================================================================ Whirling Blades
    static void spin(ActCtx c) {
        ServerPlayer p = c.player();
        ServerLevel l = p.serverLevel();
        p.swing(InteractionHand.MAIN_HAND, true);
        Wave5Aim.sound(l, p.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0f, 1.3f);
        FxRelic.ring(l, p.position(), 3.0, 0x3FE0D0);
        FxRelic.ring(l, p.position(), 2.2, 0xE8503A);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            com.newuniverse.nusmp.particle.fx.Fx.spray(l, FxThemes.RELIC_WAVE_12, FxTone.LIGHT, p.getX() + Math.cos(a) * 2.2, p.getY() + 1.0, p.getZ() + Math.sin(a) * 2.2, 1, 0, 0, 0, 0);
        }
        int hit = 0;
        for (LivingEntity t : Fx.foesAround(p, 3.5)) {
            Wave0Util.extra(p, t, 6.0f * PairRules.factor(PairHooks.active(p)), "minecraft:player_attack");      // 1d6 + 1d6 (+ the extra 1d6) of piercing, spun; one blade alone spins weaker
            t.knockback(0.4, p.getX() - t.getX(), p.getZ() - t.getZ());
            hit++;
        }
        Wave5Aim.msg(p, hit > 0 ? "Whirling Blades: " + hit + " struck." : "Whirling Blades spin and find no one.", ChatFormatting.AQUA);
    }

    /**
     * The main-hand blade's own bite after a blow: the right blade is the hot one (a flame), the left the cold one (a chill). With the pair in hand the
     * off-hand blade strikes its own blow too ({@link #bladeOffHand}), so a swing carries both, as the card says.
     */
    static void bladeBlow(ServerPlayer p, LivingEntity t) {
        RelicDef d = Relics.defOf(p.getMainHandItem());
        bite(p, t, d != null && d.isPairLeft());
    }

    /** The off-hand blade's blow: its own bite. */
    static void bladeOffHand(ServerPlayer p, LivingEntity t, RelicDef off) { bite(p, t, off.isPairLeft()); }

    private static void bite(ServerPlayer p, LivingEntity t, boolean cold) {
        if (cold) {
            Fx.addEffect(t, "minecraft:slowness", 30, 0);
            Fx.addEffect(t, "tensura:frost", 40, 0);
            Wave5Aim.puff(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.6, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_12, FxTone.LIGHT), 6, 0.3);
        } else {
            t.setRemainingFireTicks(Math.max(t.getRemainingFireTicks(), 40));
            Wave5Aim.puff(p.serverLevel(), t.position().add(0, t.getBbHeight() * 0.6, 0), com.newuniverse.nusmp.particle.fx.Fx.options(FxThemes.RELIC_WAVE_12, FxTone.LIGHT), 5, 0.25);
        }
    }

    // ================================================================ Twin Hook Swords
    static void sweepLeg(ActCtx c) {
        ServerPlayer p = c.player();
        LivingEntity t = Wave5Aim.pickFoe(p, 4.5);
        p.swing(InteractionHand.MAIN_HAND, true);
        if (t == null) {
            Wave5Aim.msg(p, "No foe within reach of the hooks.", ChatFormatting.GRAY);
            return;
        }
        boolean flies = t instanceof net.minecraft.world.entity.FlyingMob || t instanceof net.minecraft.world.entity.animal.FlyingAnimal
                || (t instanceof Mob m && m.getNavigation() instanceof FlyingPathNavigation);
        boolean boss = t.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES);
        boolean prone = Wave12Rules.hookable(t.getBbWidth(), t.getBbHeight(), flies) && Wave12Rules.hookHolds(t.getMaxHealth(), boss);
        Wave0Util.extra(p, t, (prone ? 3.5f : 2.5f) * PairRules.factor(PairHooks.active(p)), "minecraft:player_attack");        // 1d6 on a hold, 1d4 when it slips; one hook alone bites less
        if (!prone) {
            Wave5Aim.msg(p, "The hook slips.", ChatFormatting.GRAY);
            return;
        }
        Vec3 pull = p.position().subtract(t.position());
        Vec3 flat = new Vec3(pull.x, 0, pull.z);
        flat = flat.lengthSqr() < 1.0e-4 ? Vec3.ZERO : flat.normalize().scale(0.45);
        t.setDeltaMovement(flat.x, -0.4, flat.z);
        t.hurtMarked = true;
        Fx.addEffect(t, "minecraft:slowness", 30, 5);                                  // prone: it cannot move for a moment
        Fx.addEffect(t, "minecraft:weakness", 40, 0);
        Wave5Aim.sound(p.serverLevel(), t.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.0f);
        Wave5Aim.msg(p, flies ? "Hooked out of the air." : "Swept off its feet.", ChatFormatting.GREEN);
    }

    /** The off-hand hook's blow: the foe is caught for a moment. */
    static void hookOffHand(ServerPlayer p, LivingEntity t, RelicDef off) {
        Fx.addEffect(t, "minecraft:slowness", 20, 1);
    }

    /** Every tick, only while pushed against a wall off the ground: the hooks bite and hold (a slow climb; sneak to hang still). */
    static void climb(ServerPlayer p) {
        CompoundTag d = Wave12Data.player(p);
        int n = d.getInt("climb");
        if (n >= Wave12Rules.CLIMB_MAX_TICKS || p.isInWater() || p.isFallFlying()) return;
        Vec3 v = p.getDeltaMovement();
        double y = p.isShiftKeyDown() ? 0.0 : Math.max(v.y, 0.16);
        p.setDeltaMovement(v.x, y, v.z);
        p.hurtMarked = true;
        p.fallDistance = 0;
        d.putInt("climb", n + 1);
    }

    /** The climb counter resets when the wielder stands on the ground again. */
    static void landed(ServerPlayer p) {
        if (!Wave12Data.hasPlayerData(p)) return;
        CompoundTag d = Wave12Data.player(p);
        if (d.getInt("climb") != 0) d.putInt("climb", 0);
    }
}
