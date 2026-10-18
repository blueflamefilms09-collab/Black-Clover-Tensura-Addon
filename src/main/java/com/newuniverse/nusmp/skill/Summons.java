package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.skill.rules.SummonRules;
import com.newuniverse.nusmp.skill.rules.SummonRules.Card;
import com.newuniverse.nusmp.skill.rules.Tier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Why: Summoner and Lord of Summons call creatures that must obey four safety rules: never more than the cap, never lost track of (every
 * summon carries its owner, its card and the time it fades, in its own persistent data, so a restart cannot orphan one), cleaned up when
 * its owner dies or leaves or its time is up, and never able to be hurt or commanded by anyone but its owner (all skill area effects skip
 * summons through {@link SkillTargets#isFoe}). The creatures are plain vanilla ones (wolf, iron golem, vex) with boosted attributes, so no
 * new entity type or save format is needed; the tags {@code nusmp_summon} and {@code nusmp_owner} are the ones the first release used.
 */
final class Summons {
    private Summons() {}

    static final String UNTIL = "nusmp_summon_until", CARD = "nusmp_summon_card";
    /** How far from a player a summon is still looked after. */
    private static final double SCAN = 96.0;

    // ---------------------------------------------------------------- lookup
    /** All living summons of this player within {@link #SCAN} blocks (oldest first: lowest entity id). */
    static List<Mob> owned(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        UUID id = player.getUUID();
        List<Mob> found = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SCAN),
                m -> m.isAlive() && m.getTags().contains(SummonerSkill.SUMMON_TAG) && ownedBy(m, id));
        found.sort(Comparator.comparingInt(Mob::getId));
        return found;
    }

    static boolean ownedBy(Mob m, UUID owner) {
        var data = m.getPersistentData();
        return data.hasUUID(SummonerSkill.OWNER_KEY) && owner.equals(data.getUUID(SummonerSkill.OWNER_KEY));
    }

    // ---------------------------------------------------------------- creation
    /** Builds (does not add) one creature of the card, or null if its entity type cannot be made. */
    static Mob create(ServerPlayer owner, Card card, Tier tier) {
        ServerLevel level = owner.serverLevel();
        Mob mob = switch (card) {
            case BEAST, LEGION -> {
                Wolf wolf = EntityType.WOLF.create(level);
                if (wolf != null) wolf.tame(owner);
                yield wolf;
            }
            case STONE -> {
                IronGolem golem = EntityType.IRON_GOLEM.create(level);
                if (golem != null) golem.setPlayerCreated(true);   // a player's golem never attacks players
                yield golem;
            }
            case WRAITH -> {
                Vex vex = EntityType.VEX.create(level);
                if (vex != null) vex.setLimitedLife(SummonRules.lifetimeTicks(card, tier));
                yield vex;
            }
        };
        if (mob == null) return null;

        mob.setCustomName(name(card).withStyle(color(card)));
        mob.setCustomNameVisible(false);
        setBase(mob, Attributes.MAX_HEALTH, SummonRules.health(card, tier));
        setBase(mob, Attributes.ATTACK_DAMAGE, SummonRules.damage(card, tier));
        double scale = SummonRules.scale(card, tier);
        if (scale != 1.0) setBase(mob, Attributes.SCALE, scale);
        mob.setHealth(mob.getMaxHealth());

        long now = level.getGameTime();
        mob.addTag(SummonerSkill.SUMMON_TAG);
        var data = mob.getPersistentData();
        data.putUUID(SummonerSkill.OWNER_KEY, owner.getUUID());
        data.putLong(UNTIL, now + SummonRules.lifetimeTicks(card, tier));
        data.putInt(CARD, card.ordinal());
        mob.setPersistenceRequired();
        return mob;
    }

    private static void setBase(Mob mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    static net.minecraft.network.chat.MutableComponent name(Card card) {
        return switch (card) {
            case BEAST -> SkillMsg.t("summoner.name.beast", "Beast Card");
            case STONE -> SkillMsg.t("summoner.name.stone", "Stone Card");
            case LEGION -> SkillMsg.t("summoner.name.legion", "Legion Card");
            case WRAITH -> SkillMsg.t("summoner.name.wraith", "Wraith Card");
        };
    }

    private static ChatFormatting color(Card card) {
        return switch (card) {
            case BEAST -> ChatFormatting.GOLD;
            case STONE -> ChatFormatting.GRAY;
            case LEGION -> ChatFormatting.RED;
            case WRAITH -> ChatFormatting.LIGHT_PURPLE;
        };
    }

    /** Adds {@code count} creatures of the card around the player; returns how many really appeared. */
    static int spawn(ServerPlayer owner, Card card, Tier tier, int count) {
        ServerLevel level = owner.serverLevel();
        int made = 0;
        Vec3 look = owner.getViewVector(1.0F);
        Vec3 flat = new Vec3(look.x, 0, look.z);
        Vec3 fwd = flat.lengthSqr() < 1.0e-4 ? new Vec3(0, 0, 1) : flat.normalize();
        for (int i = 0; i < count; i++) {
            Mob mob = create(owner, card, tier);
            if (mob == null) continue;
            // a fan in front of the caster, 2.2 blocks out
            double spread = count == 1 ? 0.0 : (i - (count - 1) / 2.0) * 0.55;
            double cos = Math.cos(spread), sin = Math.sin(spread);
            Vec3 dir = new Vec3(fwd.x * cos - fwd.z * sin, 0, fwd.x * sin + fwd.z * cos);
            Vec3 spot = owner.position().add(dir.scale(2.2));
            mob.moveTo(spot.x, owner.getY(), spot.z, owner.getYRot() + 180.0F, 0.0F);
            if (!level.noCollision(mob)) mob.moveTo(owner.getX(), owner.getY(), owner.getZ(), owner.getYRot(), 0.0F);
            if (level.addFreshEntity(mob)) {
                made++;
                SkillFx.burst(level, mob.position().add(0, mob.getBbHeight() * 0.5, 0), card == Card.WRAITH, 8, 0.4);
            }
        }
        return made;
    }

    /** Discards every summon of the owner (Recall, death, logout). Returns how many were removed. */
    static int recall(ServerPlayer owner) {
        int removed = 0;
        for (Mob m : owned(owner)) {
            SkillFx.burst(owner.serverLevel(), m.position().add(0, m.getBbHeight() * 0.5, 0), false, 6, 0.3);
            m.discard();
            removed++;
        }
        return removed;
    }

    // ---------------------------------------------------------------- upkeep (called from the owner's tick, once a second)
    /** Expires, trims and steers the summons near this player. */
    static void tick(ServerPlayer player, MinecraftServer server, int max) {
        ServerLevel level = player.serverLevel();
        long now = level.getGameTime();
        List<Mob> near = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SCAN),
                m -> m.isAlive() && m.getTags().contains(SummonerSkill.SUMMON_TAG));
        if (near.isEmpty()) return;

        List<Mob> mine = new ArrayList<>();
        for (Mob m : near) {
            var data = m.getPersistentData();
            UUID ownerId = data.hasUUID(SummonerSkill.OWNER_KEY) ? data.getUUID(SummonerSkill.OWNER_KEY) : null;
            ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
            boolean ownerOk = owner != null && owner.isAlive();
            if (SummonRules.shouldVanish(now, data.getLong(UNTIL), owner != null, ownerOk)) {
                SkillFx.burst(level, m.position().add(0, m.getBbHeight() * 0.5, 0), true, 6, 0.3);
                m.discard();
                continue;
            }
            if (ownerId.equals(player.getUUID())) mine.add(m);
        }
        if (mine.isEmpty()) return;

        mine.sort(Comparator.comparingInt(Mob::getId));
        for (int i = 0, drop = SummonRules.excess(mine.size(), max); i < drop; i++) mine.remove(0).discard();   // oldest first

        List<LivingEntity> foes = SkillTargets.foes(player, player.position(), SummonRules.ENGAGE_RANGE);
        for (Mob m : mine) steer(m, player, foes);
    }

    private static void steer(Mob m, ServerPlayer owner, List<LivingEntity> foes) {
        try {
            LivingEntity target = m.getTarget();
            if (target != null && (!target.isAlive() || target == owner || SkillTargets.isOwnedBy(target, owner.getUUID())
                    || !SkillTargets.isAimable(owner, target))) {
                m.setTarget(null);
                target = null;
            }
            if (target == null && !foes.isEmpty()) {
                LivingEntity best = null;
                double bestD = Double.MAX_VALUE;
                for (LivingEntity f : foes) {
                    double d = f.distanceToSqr(m);
                    if (d < bestD) { bestD = d; best = f; }
                }
                m.setTarget(best);
                target = best;
            }
            double dist = Math.sqrt(m.distanceToSqr(owner));
            int leash = SummonRules.leashAction(dist);
            if (leash == 2) {
                m.teleportTo(owner.getX() + (m.getRandom().nextDouble() - 0.5) * 3, owner.getY(), owner.getZ() + (m.getRandom().nextDouble() - 0.5) * 3);
                m.getNavigation().stop();
            } else if (leash == 1 && target == null) {
                m.getNavigation().moveTo(owner, 1.2);
            }
        } catch (RuntimeException ignored) {
            // a creature of an unexpected shape keeps its own mind
        }
    }

    static Component countMessage(int removed) {
        return SkillMsg.t("summoner.recalled", "Recalled %s summon(s).", removed).withStyle(ChatFormatting.AQUA);
    }
}
