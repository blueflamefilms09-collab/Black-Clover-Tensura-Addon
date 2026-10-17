package com.newuniverse.nusmp.avatar.yandere;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.FtbTeamHelper;
import com.newuniverse.nusmp.avatar.ai.AvatarGates;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: the Yandere frenzy. When the creator is hurt, a Yandere Avatar within 48 blocks goes berserk for a short while: red eyes (the synced
 * IS_FRENZIED flag the renderer reads), +30% speed, skill cooldowns x0.1 and magicule costs waived (both read by the cast code), at the price of
 * draining ITS OWN maximum health per cast (never below 1, given back when the frenzy ends). It refreshes while the creator keeps being hurt and always ends in a short Slowness
 * exhaustion, so it is never permanent. The state lives in two ints in the Avatar's persistent data (see {@link YandereFrenzyState}) so a reload
 * cannot leave the flag stuck. Works only for Avatars with YANDERE as primary or secondary personality. Server side only.
 */
public final class YandereFrenzyHandler {
    private YandereFrenzyHandler() {}

    private static final String T_FRENZY = "nusmp_av_frenzy", T_EXHAUST = "nusmp_av_exhaust";
    private static final ResourceLocation SPEED_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_frenzy_speed");
    /** The temporary drain on the Avatar's maximum health that its frenzy casts cause (one transient modifier, replaced on every cast, removed when the frenzy ends). */
    private static final ResourceLocation DRAIN_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_frenzy_drain");

    /** The creator was damaged: start or refresh this Avatar's frenzy, and let it go for the attacker. Never throws on a client. */
    public static void onCreatorHurt(AvatarEntity avatar, DamageSource source, float amount) {
        if (avatar == null || avatar.level().isClientSide || !avatar.isAlive() || !avatar.has(AvatarPersonality.YANDERE) || amount <= 0 || !AvatarGates.loyal(avatar)) return;
        YandereFrenzyState st = load(avatar);
        boolean wasIdle = st.phase() != YandereFrenzyState.Phase.FRENZY;
        st.trigger(AvatarAiConfig.frenzyTicks());
        save(avatar, st);
        if (wasIdle) begin(avatar);
        Entity culprit = source == null ? null : source.getEntity();
        if (culprit instanceof LivingEntity l && l != avatar && validFrenzyTarget(avatar, l)) avatar.setTarget(l);
    }

    /** Every tick while the Avatar lives (called by its watcher goal): counts the frenzy down and ends it cleanly. */
    public static void tick(AvatarEntity avatar) {
        if (avatar == null || avatar.level().isClientSide) return;
        YandereFrenzyState st = load(avatar);
        if (st.phase() == YandereFrenzyState.Phase.IDLE) {
            if (avatar.frenzied()) end(avatar, false);                                  // a stale flag after a reload
            return;
        }
        if (!avatar.has(AvatarPersonality.YANDERE) || !AvatarGates.loyal(avatar)) { save(avatar, new YandereFrenzyState(0, 0, 0)); if (avatar.frenzied()) end(avatar, false); return; }
        YandereFrenzyState.Change c = st.tick();
        save(avatar, st);
        if (st.phase() == YandereFrenzyState.Phase.FRENZY) {
            if (!avatar.frenzied()) begin(avatar);
            if (avatar.tickCount % 20 == 0 && avatar.level() instanceof ServerLevel sl) {                // the mod's own aura effect, renewed every second (it runs 26 ticks)
                VfxSpawn.sendFollowing(sl, VfxShape.SPIRIT_AURA, avatar, avatar.position(), 0xFFB0102A, 26, 1.0f);
            }
        }
        if (c == YandereFrenzyState.Change.ENTERED_EXHAUSTION) end(avatar, true);
        else if (c == YandereFrenzyState.Change.ENDED) end(avatar, false);
    }

    /**
     * One frenzy cast has happened. The brief's price is the Avatar's own MAXIMUM health: the maximum drops by {@code frenzyHealthBurnPerCast} for as long as the frenzy
     * lasts (a transient modifier, so a reload or the end of the frenzy gives it back; the health it lost stays lost until it heals), and never so far that the maximum
     * falls below 1 ({@link YandereMath#drainStep}). Before the rebuild it burned the current health instead; that is kept as the fallback when the Avatar has no
     * maximum-health attribute to drain.
     */
    public static void burnForCast(AvatarEntity avatar) {
        if (avatar == null || avatar.level().isClientSide || !avatar.frenzied()) return;
        AttributeInstance max = avatar.getAttribute(Attributes.MAX_HEALTH);
        if (max == null) {
            float b = YandereMath.burn(avatar.getHealth(), AvatarAiConfig.frenzyBurn());
            if (b > 0f) avatar.setHealth(avatar.getHealth() - b);
            return;
        }
        AttributeModifier old = max.getModifier(DRAIN_ID);
        double drained = old == null ? 0.0 : -old.amount();
        double withoutDrain = max.getValue() + drained;
        double step = YandereMath.drainStep(withoutDrain, drained, AvatarAiConfig.frenzyBurn());
        if (step <= 0.0) return;
        max.removeModifier(DRAIN_ID);
        max.addTransientModifier(new AttributeModifier(DRAIN_ID, -(drained + step), AttributeModifier.Operation.ADD_VALUE));
        if (avatar.getHealth() > avatar.getMaxHealth()) avatar.setHealth(avatar.getMaxHealth());
    }

    private static void begin(AvatarEntity avatar) {
        avatar.setFrenzied(true);
        AttributeInstance speed = avatar.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.addOrUpdateTransientModifier(new AttributeModifier(SPEED_ID, AvatarAiConfig.frenzySpeedBonus(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        AvatarChatterEngine.say(avatar, ChatEvent.FRENZY);
    }

    private static void end(AvatarEntity avatar, boolean exhausted) {
        avatar.setFrenzied(false);
        AttributeInstance speed = avatar.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.removeModifier(SPEED_ID);
        AttributeInstance max = avatar.getAttribute(Attributes.MAX_HEALTH);
        if (max != null) max.removeModifier(DRAIN_ID);                                       // the maximum comes back
        if (exhausted && AvatarAiConfig.exhaustionTicks() > 0)
            avatar.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, AvatarAiConfig.exhaustionTicks(), 1, false, true));
    }

    /** The frenzy goes for whoever hurt the creator, unless that is the creator, a team mate, or another Avatar / pet of the creator's side. */
    static boolean validFrenzyTarget(AvatarEntity avatar, LivingEntity l) {
        if (!l.isAlive() || l.getUUID().equals(avatar.creatorUuid())) return false;
        if (l instanceof AvatarEntity other && avatar.creatorUuid() != null && avatar.creatorUuid().equals(other.creatorUuid())) return false;
        return !FtbTeamHelper.isSquadmate(avatar, l);
    }

    private static YandereFrenzyState load(AvatarEntity avatar) {
        CompoundTag d = avatar.getPersistentData();
        return new YandereFrenzyState(d.getInt(T_FRENZY), d.getInt(T_EXHAUST), AvatarAiConfig.exhaustionTicks());
    }

    private static void save(AvatarEntity avatar, YandereFrenzyState st) {
        CompoundTag d = avatar.getPersistentData();
        d.putInt(T_FRENZY, st.frenzyLeft());
        d.putInt(T_EXHAUST, st.exhaustLeft());
    }
}
