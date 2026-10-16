package com.newuniverse.nusmp.avatar.ai;

import com.newuniverse.nusmp.avatar.AvatarAiConfig;
import com.newuniverse.nusmp.avatar.AvatarChatterEngine;
import com.newuniverse.nusmp.avatar.AvatarEntity;
import com.newuniverse.nusmp.avatar.AvatarPersonality;
import com.newuniverse.nusmp.avatar.ChatEvent;
import com.newuniverse.nusmp.avatar.FtbTeamHelper;
import com.newuniverse.nusmp.avatar.yandere.YandereMath;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Yandere's jealousy, made fair. Anyone who walks within {@code yandereJealousRadius} (5) blocks of the creator is first WARNED ("Step
 * away from {creator}.", a 20 tick grace with no contact), then body-blocked (the Avatar plants itself between them and the creator), and only
 * attacked if they act hostile toward the creator or stay three seconds ({@link YandereMath#phase}). Never the creator, their FTB team, other
 * Avatars of the same creator, the creator's pets, or any boss. Players are never taken below 1 health while {@code yandereLethal} is off (see
 * {@code AvatarAiEvents}). Only runs for an Avatar with YANDERE as primary or secondary personality; checked live, never at install.
 */
public final class YandereJealousyTargetGoal extends Goal {
    private final AvatarEntity avatar;
    private final Map<UUID, Integer> near = new HashMap<>();
    private LivingEntity focus;
    private ServerPlayer creator;

    public YandereJealousyTargetGoal(AvatarEntity avatar) {
        this.avatar = avatar;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (avatar.level().isClientSide || !AvatarAiConfig.yandereJealousy() || !avatar.has(AvatarPersonality.YANDERE) || avatar.frenzied() || !AvatarGates.loyal(avatar)) return false;
        if (avatar.tickCount % 5 != 0) return false;
        creator = avatar.creatorPlayer();
        if (creator == null || creator.level() != avatar.level() || avatar.distanceToSqr(creator) > 32 * 32) { near.clear(); return false; }
        scan();
        focus = pickFocus();
        return focus != null;
    }

    @Override
    public boolean canContinueToUse() {
        return focus != null && focus.isAlive() && creator != null && creator.isAlive() && avatar.has(AvatarPersonality.YANDERE) && !avatar.frenzied()
                && focus.distanceToSqr(creator) <= sq(AvatarAiConfig.jealousRadius() + 2);
    }

    @Override public boolean requiresUpdateEveryTick() { return true; }

    @Override public void start() { if (focus != null && ticksOf(focus) <= 5) warn(focus); }

    @Override
    public void tick() {
        if (focus == null || creator == null) return;
        if (avatar.tickCount % 5 == 0) scan();
        YandereMath.Phase ph = YandereMath.phase(ticksOf(focus), hostileActing(focus));
        avatar.getLookControl().setLookAt(focus, 30f, 30f);
        if (ph == YandereMath.Phase.WARN || ph == YandereMath.Phase.NONE) return;
        Vec3 away = focus.position().subtract(creator.position()).multiply(1, 0, 1);
        Vec3 spot = creator.position().add(away.lengthSqr() < 1e-4 ? Vec3.ZERO : away.normalize().scale(1.6));
        if (avatar.distanceToSqr(spot) > 1.0) avatar.getNavigation().moveTo(spot.x, spot.y, spot.z, 1.3);
        if (ph == YandereMath.Phase.ATTACK && avatar.getTarget() != focus) avatar.setTarget(focus);
    }

    @Override
    public void stop() {
        if (focus != null && avatar.getTarget() == focus) avatar.setTarget(null);
        focus = null;
        avatar.getNavigation().stop();
    }

    // ---------------------------------------------------------------- who is jealous-worthy
    private void scan() {
        double r = AvatarAiConfig.jealousRadius();
        List<LivingEntity> in = avatar.level().getEntitiesOfClass(LivingEntity.class, creator.getBoundingBox().inflate(r), e -> intruder(e) && e.distanceToSqr(creator) <= r * r);
        Iterator<Map.Entry<UUID, Integer>> it = near.entrySet().iterator();
        while (it.hasNext()) {
            UUID id = it.next().getKey();
            if (in.stream().noneMatch(e -> e.getUUID().equals(id))) it.remove();
        }
        for (LivingEntity e : in) near.merge(e.getUUID(), 5, Integer::sum);
    }

    private LivingEntity pickFocus() {
        LivingEntity best = null;
        int bestTicks = 0;
        for (LivingEntity e : avatar.level().getEntitiesOfClass(LivingEntity.class, creator.getBoundingBox().inflate(AvatarAiConfig.jealousRadius()), this::intruder)) {
            int t = ticksOf(e);
            if (t > bestTicks) { bestTicks = t; best = e; }
        }
        return best;
    }

    private int ticksOf(LivingEntity e) { return near.getOrDefault(e.getUUID(), 0); }

    private void warn(LivingEntity e) { AvatarChatterEngine.say(avatar, ChatEvent.JEALOUS_WARN, e.getDisplayName().getString()); }

    /** The intruder did something hostile toward the creator: it targets them, or it hit them lately. */
    private boolean hostileActing(LivingEntity e) {
        if (e instanceof Mob m && m.getTarget() == creator) return true;
        return e.getLastHurtMob() == creator && e.tickCount - e.getLastHurtMobTimestamp() < 100;
    }

    /** May this entity make the Avatar jealous? Everything except the creator and the creator's side. */
    private boolean intruder(LivingEntity e) {
        if (e == avatar || e == creator || !e.isAlive() || e.isSpectator() || e instanceof ArmorStand || !e.isAttackable()) return false;
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        if (e.getUUID().equals(avatar.creatorUuid())) return false;
        if (e instanceof AvatarEntity other && avatar.creatorUuid() != null && avatar.creatorUuid().equals(other.creatorUuid())) return false;
        if (e instanceof OwnableEntity o && avatar.creatorUuid() != null && avatar.creatorUuid().equals(o.getOwnerUUID())) return false;
        if (BossIdentity.isBoss(e)) return false;
        return !FtbTeamHelper.isSquadmate(avatar, e);
    }

    private static double sq(double v) { return v * v; }
}
