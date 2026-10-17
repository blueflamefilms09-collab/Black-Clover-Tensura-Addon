package com.newuniverse.nusmp.avatar.evolution;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarConfig;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.avatar.appearance.AppearanceData;
import com.newuniverse.nusmp.avatar.appearance.DynamicAppearanceEngine;
import com.newuniverse.nusmp.avatar.party.AvatarPartyManager;
import com.newuniverse.nusmp.avatar.race.AvatarRace;
import com.newuniverse.nusmp.avatar.race.AvatarRaces;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import dev.architectury.event.EventResult;
import io.github.manasmods.tensura.event.TensuraEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import org.slf4j.Logger;

/**
 * Why: the owner wants Avatars to be "independent adventurers mirroring player progression": as their EP grows they go through a Harvest Festival and evolve into the
 * next form of their race. Be plain about what this can and cannot do. Tensura's own evolution tree (its screen and packets) runs on players through the race system
 * and cannot be applied to a mob, so an Avatar's evolution follows the chain its race data declares ({@code evolves_to_race_id} in the Avatar race JSON). What it hooks
 * from Tensura is the Harvest Festival: when Tensura puts an Avatar into one (it does this for the subordinates of a player who enters one) the Avatar evolves
 * when that festival pays its reward ({@code HARVEST_FESTIVAL_REWARD_EVENT}); and when an Avatar's EP reaches {@link EvolutionRules#threshold} of the next race's base EP it
 * goes through a festival of its own, which is simulated (it stands still and shining for {@code avatar.festivalSeconds}, hard to hurt, then changes), because Tensura's
 * festival code was not written for a mob namer and nobody could check it for one. Either way the change itself is the one naming uses ({@link #evolve}): race,
 * look, scores and attributes, and the EP never drops below what the Avatar had. EP grows by kills ({@link #onKill}) and by naming. Server side.
 */
public final class AvatarEvolution {
    private static final Logger LOG = LogUtils.getLogger();

    private AvatarEvolution() {}

    /** The state in the Avatar's persistent data: Left (festival ticks), To (race id), Native (waiting for Tensura's reward), Last (game time of the last evolution), Next (next look). */
    static final String STATE = "nusmp_av_evo";
    /** A festival Tensura started is waited for at most this long (half an hour); then the Avatar forgets it. */
    private static final long NATIVE_WAIT_TICKS = 36_000L;

    // ---------------------------------------------------------------- the change
    /** The next race of this Avatar's race chain, or null at the end of it (or when its race is unknown). */
    public static AvatarRace nextRace(AvatarEntity a) {
        AvatarRace race = a.race();
        return race != null && race.evolves() ? AvatarRaces.get(race.evolvesTo()) : null;
    }

    /**
     * Turns the Avatar into the next race: race id, look (scaled by the ratio of the two races' default sizes), scores-derived attributes and EP pools. {@code epMultiplier}
     * is the naming surge (1.0 for a plain evolution). The Avatar's EP never ends up smaller than it was before, so a strong Avatar is not set back by a weak race's base.
     */
    public static void evolve(AvatarEntity a, AvatarRace now, AvatarRace next, double epMultiplier) {
        double epBefore = TensuraBridge.baseMaxEp(a);
        AppearanceData look = a.appearance();
        float scale = now != null && now.defaultScale() > 0 ? look.scale() * next.defaultScale() / now.defaultScale() : next.defaultScale();
        AppearanceData grown = new AppearanceData(look.skinTone(), look.hairId(), look.outfitId(), look.hornId(), look.wingId(), scale, look.auraRgb(), look.eyeRgb());
        a.getEntityData().set(AvatarEntity.RACE, next.id());
        a.setAppearance(DynamicAppearanceEngine.sanitize(grown, next));
        a.applyRace(next, epMultiplier);
        double restore = EvolutionRules.keepFactor(epBefore, TensuraBridge.baseMaxEp(a));
        if (restore > 1.0) TensuraBridge.scaleEnergy(a, restore);
        a.getPersistentData().remove(STATE);
        CompoundTag keep = new CompoundTag();
        keep.putLong("Last", a.level().getGameTime());
        a.getPersistentData().put(STATE, keep);
    }

    // ---------------------------------------------------------------- the once-a-second check
    /** Looks at one Avatar: runs its festival, or starts one when it is strong enough for the next form. Called once a second from the Avatar's slow tick. */
    public static void tick(AvatarEntity a) {
        if (a.level().isClientSide || !(a.level() instanceof ServerLevel sl) || !AvatarConfig.enabled() || !AvatarAiConfig.evolutionEnabled() || a.isBetrayed()) return;
        long now = sl.getGameTime();
        CompoundTag st = a.getPersistentData().getCompound(STATE);
        if (st.contains("Left")) {
            if (st.getBoolean("Native")) {
                if (now - st.getLong("At") > NATIVE_WAIT_TICKS) a.getPersistentData().remove(STATE);                  // Tensura never paid out: forget it
                return;
            }
            int left = Math.max(0, st.getInt("Left") - 20);
            st.putInt("Left", left);
            a.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 9, false, false));
            a.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, false, false));
            a.setTarget(null);
            if (left == 0) finish(a, st.getString("To"));
            return;
        }
        if (now < st.getLong("Next")) return;
        st.putLong("Next", now + AvatarAiConfig.evolutionCheckTicks());
        a.getPersistentData().put(STATE, st);
        AvatarRace next = nextRace(a);
        if (next == null) return;
        double ep = TensuraBridge.maxEp(a);
        double need = EvolutionRules.threshold(next.baseEp(), AvatarAiConfig.evolutionEpFactor());
        if (!EvolutionRules.due(ep, need, false, now, st.getLong("Last"), AvatarAiConfig.evolutionCooldownTicks())) return;
        begin(a, next);
    }

    private static void begin(AvatarEntity a, AvatarRace next) {
        if (!(a.level() instanceof ServerLevel sl)) return;
        CompoundTag st = a.getPersistentData().getCompound(STATE);
        st.putInt("Left", Math.max(20, AvatarAiConfig.festivalTicks()));
        st.putString("To", next.id());
        st.putBoolean("Native", false);
        a.getPersistentData().put(STATE, st);
        a.getNavigation().stop();
        Vec3 at = a.position();
        VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, at.add(0, 0.08, 0), at.add(0, 1.08, 0), 0xFFFFD86B, Math.min(200, AvatarAiConfig.festivalTicks()), 2.6f);
        VfxSpawn.sendFollowing(sl, VfxShape.SPIRIT_AURA, a, at, 0xFFFFD86B, Math.min(200, AvatarAiConfig.festivalTicks()), 1.2f);
        ServerPlayer creator = a.creatorPlayer();
        if (creator != null && creator.level() == sl) creator.sendSystemMessage(Component.literal(a.avatarName() + " is going through a Harvest Festival.").withStyle(ChatFormatting.GOLD));
    }

    private static void finish(AvatarEntity a, String toRaceId) {
        AvatarRace next = AvatarRaces.get(toRaceId);
        AvatarRace now = a.race();
        a.getPersistentData().remove(STATE);
        if (next == null) return;
        evolve(a, now, next, 1.0);
        if (a.level() instanceof ServerLevel sl) {
            Vec3 at = a.position();
            VfxSpawn.send(sl, VfxShape.SPIRIT_AURA, at, at.add(0, 1.0, 0), 0xFFFFD86B, 60, 1.6f);
            ServerPlayer creator = a.creatorPlayer();
            if (creator != null && creator.level() == sl) creator.sendSystemMessage(Component.literal(a.avatarName() + " has evolved into a " + next.displayName() + "!").withStyle(ChatFormatting.GOLD));
        }
        LOG.debug("[nusmp] Avatar {} evolved into {}", a.getUUID(), next.id());
    }

    // ---------------------------------------------------------------- Tensura's own festival
    /** Registers the two Harvest Festival hooks on Tensura's entity events. Safe to call once at common setup; logs and gives up if the events are not as expected. */
    public static void registerTensuraHooks() {
        try {
            TensuraEntityEvents.ENTER_HARVEST_FESTIVAL_EVENT.register((entity, a, b) -> {
                try { onNativeEnter(entity); } catch (RuntimeException soft) { LOG.debug("[nusmp] Avatar festival hook: {}", soft.toString()); }
                return EventResult.pass();
            });
            TensuraEntityEvents.HARVEST_FESTIVAL_REWARD_EVENT.register(entity -> {
                try { onNativeReward(entity); } catch (RuntimeException soft) { LOG.debug("[nusmp] Avatar festival reward hook: {}", soft.toString()); }
                return EventResult.pass();
            });
        } catch (Throwable t) {
            LOG.warn("[nusmp] Could not hook Tensura's Harvest Festival events ({}); Avatars still evolve on their own EP", t.toString());
        }
    }

    /** Tensura put an Avatar into a Harvest Festival (for example because its creator entered one): the Avatar waits for the reward and then changes. */
    static void onNativeEnter(LivingEntity entity) {
        if (!(entity instanceof AvatarEntity a) || a.level().isClientSide || !AvatarConfig.enabled() || !AvatarAiConfig.evolutionEnabled() || a.isBetrayed()) return;
        AvatarRace next = nextRace(a);
        if (next == null) return;
        CompoundTag st = a.getPersistentData().getCompound(STATE);
        if (st.contains("Left")) return;                                                          // its own festival is running already
        st.putInt("Left", 1);
        st.putString("To", next.id());
        st.putBoolean("Native", true);
        st.putLong("At", a.level().getGameTime());
        a.getPersistentData().put(STATE, st);
    }

    /** Tensura's festival paid its reward: an Avatar that was waiting for it evolves now. */
    static void onNativeReward(LivingEntity entity) {
        if (!(entity instanceof AvatarEntity a) || a.level().isClientSide) return;
        CompoundTag st = a.getPersistentData().getCompound(STATE);
        if (!st.getBoolean("Native") || !st.contains("Left")) return;
        finish(a, st.getString("To"));
    }

    // ---------------------------------------------------------------- growth by kills
    /** An Avatar that kills something strong enough gains a share of its EP (see {@link EvolutionRules#killGain}). */
    public static void onKill(LivingDeathEvent e) {
        try {
            Entity killer = e.getSource().getEntity();
            if (!(killer instanceof AvatarEntity a) || a.level().isClientSide || !AvatarConfig.enabled() || !AvatarAiConfig.evolutionEnabled()) return;
            LivingEntity victim = e.getEntity();
            if (victim == a || victim instanceof Player || victim instanceof AvatarEntity || victim instanceof AbstractVillager || AvatarPartyManager.isMember(victim)) return;
            if (victim instanceof OwnableEntity pet && pet.getOwnerUUID() != null) return;
            double gain = EvolutionRules.killGain(TensuraBridge.maxEp(victim), TensuraBridge.maxEp(a), AvatarAiConfig.killEpShare());
            if (gain > 0.0) TensuraBridge.addBaseEp(a, gain);
        } catch (RuntimeException soft) { /* never break a death */ }
    }
}
