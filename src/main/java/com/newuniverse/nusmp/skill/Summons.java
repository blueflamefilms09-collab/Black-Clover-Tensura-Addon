package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.NUConfig;
import com.newuniverse.nusmp.skill.rules.SummonRules;
import com.newuniverse.nusmp.skill.rules.SummonRules.Card;
import com.newuniverse.nusmp.skill.rules.SummonerRules;
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
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
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

    static final String UNTIL = "nusmp_summon_until", CARD = "nusmp_summon_card", RANK = "nusmp_summon_rank", COMMAND = "nusmp_summon_cmd";
    // the permanent boosts of Strengthening and of the command tiers (built once: a ResourceLocation from strings is not allowed per tick)
    private static final net.minecraft.resources.ResourceLocation STR_HP = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anime_multiverse", "summoner_strength_hp"),
            STR_ATK = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anime_multiverse", "summoner_strength_atk"),
            CMD_HP = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anime_multiverse", "summoner_command_hp"),
            CMD_ATK = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("anime_multiverse", "summoner_command_atk");
    /** How far from a player a summon is still looked after. */
    private static final double SCAN = 64.0;

    // ---------------------------------------------------------------- lookup
    /**
     * Every summon an owner has called, by UUID, so the cap counts them wherever they stand (another dimension, far away), not just the
     * ones near the owner. In memory only: after a restart the ledger is empty and fills again from the entities found near the owner.
     */
    private static final java.util.Map<UUID, List<UUID>> LEDGER = new java.util.HashMap<>();

    /** All living summons of this player: the ledger's (any distance, any loaded dimension) plus any found near them (oldest first). */
    static List<Mob> owned(ServerPlayer player) {
        UUID id = player.getUUID();
        java.util.Map<UUID, Mob> found = new java.util.LinkedHashMap<>();
        List<UUID> ids = LEDGER.computeIfAbsent(id, k -> new ArrayList<>());
        MinecraftServer server = player.getServer();
        if (server != null) {
            for (java.util.Iterator<UUID> it = ids.iterator(); it.hasNext(); ) {
                UUID mobId = it.next();
                net.minecraft.world.entity.Entity e = null;
                for (ServerLevel lvl : server.getAllLevels()) {
                    e = lvl.getEntity(mobId);
                    if (e != null) break;
                }
                if (e instanceof Mob m && m.isAlive() && ownedBy(m, id)) found.put(mobId, m); else it.remove();
            }
        }
        // anything of theirs near them that the ledger does not know yet (after a restart) is adopted
        for (Mob m : player.serverLevel().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(SCAN),
                m -> m.isAlive() && m.getTags().contains(SummonerSkill.SUMMON_TAG) && ownedBy(m, id))) {
            if (found.putIfAbsent(m.getUUID(), m) == null) ids.add(m.getUUID());
        }
        List<Mob> list = new ArrayList<>(found.values());
        list.sort(Comparator.comparingInt(Mob::getId));
        if (ids.isEmpty()) LEDGER.remove(id);
        return list;
    }

    static void clearLedger() { LEDGER.clear(); }

    private static void track(ServerPlayer owner, Mob mob) {
        LEDGER.computeIfAbsent(owner.getUUID(), k -> new ArrayList<>()).add(mob.getUUID());
    }

    static boolean ownedBy(Mob m, UUID owner) {
        var data = m.getPersistentData();
        return data.hasUUID(SummonerSkill.OWNER_KEY) && owner.equals(data.getUUID(SummonerSkill.OWNER_KEY));
    }

    // ---------------------------------------------------------------- creation
    /** Builds (does not add) one creature of the card, or null if its entity type cannot be made. */
    static Mob create(ServerPlayer owner, Card card, Tier tier) { return create(owner, card, tier, 0); }

    /** The same with a Synthesis rank (0 = a plain card): health and damage grow by {@link SummonerRules#rankFactor}. */
    static Mob create(ServerPlayer owner, Card card, Tier tier, int rank) {
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

        double factor = SummonerRules.rankFactor(rank);
        net.minecraft.network.chat.MutableComponent label = canonLabel(card, rank);
        mob.setCustomName((label != null ? label : rank > 0 ? name(card).append(" +" + rank) : name(card)).withStyle(color(card)));
        mob.setCustomNameVisible(false);
        setBase(mob, Attributes.MAX_HEALTH, SummonRules.health(card, tier) * factor);
        setBase(mob, Attributes.ATTACK_DAMAGE, SummonRules.damage(card, tier) * factor);
        double scale = SummonRules.scale(card, tier);
        if (scale != 1.0) setBase(mob, Attributes.SCALE, scale);
        mob.setHealth(mob.getMaxHealth());

        long now = level.getGameTime();
        mob.addTag(SummonerSkill.SUMMON_TAG);
        var data = mob.getPersistentData();
        data.putUUID(SummonerSkill.OWNER_KEY, owner.getUUID());
        data.putLong(UNTIL, now + SummonRules.lifetimeTicks(card, tier));
        data.putInt(CARD, card.ordinal());
        if (rank > 0) data.putInt(RANK, rank);
        mob.setPersistenceRequired();
        return mob;
    }

    private static void setBase(Mob mob, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, double value) {
        AttributeInstance inst = mob.getAttribute(attr);
        if (inst != null) inst.setBaseValue(value);
    }

    /** Canon names (docs/summoner_rework.md): a Beast Card is "Beast H" (a letter further per Synthesis), a Stone Card is Wally (Stone E), then Stone D, Steely (C), Mira (B); null = keep the old name. */
    static net.minecraft.network.chat.MutableComponent canonLabel(Card card, int rank) {
        if (!NUConfig.summonerCanon()) return null;
        return switch (card) {
            case BEAST -> Component.literal(SummonerRules.cardLabel(SummonerRules.Family.BEAST, rank, false));
            case STONE -> Component.literal(SummonerRules.cardLabel(SummonerRules.Family.STONE, rank, true));
            default -> null;
        };
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
                track(owner, mob);
                SkillFx.burst(level, mob.position().add(0, mob.getBbHeight() * 0.5, 0), card == Card.WRAITH, 8, 0.4);
            }
        }
        return made;
    }

    /** Strengthening: every summon of the owner within range gets strength and resistance for a while. At most the cap's worth of creatures. Returns how many. */
    static int strengthen(ServerPlayer owner, Tier tier) {
        int ticks = SummonerRules.strengthenSeconds(tier) * 20, amp = SummonerRules.strengthenAmplifier(tier), n = 0;
        double range2 = SummonerRules.STRENGTHEN_RANGE * SummonerRules.STRENGTHEN_RANGE;
        for (Mob m : owned(owner)) {
            if (m.level() != owner.level() || m.distanceToSqr(owner) > range2) continue;
            m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, ticks, amp, true, true));
            m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, ticks, amp, true, true));
            SkillFx.burst(owner.serverLevel(), m.position().add(0, m.getBbHeight() * 0.5, 0), false, 4, 0.3);
            n++;
        }
        return n;
    }

    // ---------------------------------------------------------------- canon: Strengthening that stays, and the command tiers
    private static void boost(Mob m, net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attr, net.minecraft.resources.ResourceLocation id, double share) {
        AttributeInstance inst = m.getAttribute(attr);
        if (inst == null) return;
        if (share <= 0) inst.removeModifier(id);
        else inst.addOrReplacePermanentModifier(new AttributeModifier(id, share, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private static void boostBoth(Mob m, net.minecraft.resources.ResourceLocation hp, net.minecraft.resources.ResourceLocation atk, double share) {
        float before = m.getMaxHealth();
        boost(m, Attributes.MAX_HEALTH, hp, share);
        boost(m, Attributes.ATTACK_DAMAGE, atk, share);
        float grown = m.getMaxHealth() - before;
        if (grown > 0) m.heal(grown);
    }

    /** Canon Strengthening: two stats of every summon within range grow for as long as the summon lives (a Synthesis makes a new, plain one, as in the wiki). At most the cap's worth. */
    static int strengthenCanon(ServerPlayer owner, int level) {
        double share = SummonerRules.strengthenBonus(level), range2 = SummonerRules.STRENGTHEN_RANGE * SummonerRules.STRENGTHEN_RANGE;
        int n = 0;
        for (Mob m : owned(owner)) {
            if (m.level() != owner.level() || m.distanceToSqr(owner) > range2) continue;
            boostBoth(m, STR_HP, STR_ATK, share);
            SkillFx.burst(owner.serverLevel(), m.position().add(0, m.getBbHeight() * 0.5, 0), false, 4, 0.3);
            n++;
        }
        return n;
    }

    static int commandOf(Mob m) { return Math.max(0, Math.min(SummonerRules.CMD_KING, m.getPersistentData().getInt(COMMAND))); }

    static void setCommand(Mob m, int cmd) {
        m.getPersistentData().putInt(COMMAND, cmd);
        boostBoth(m, CMD_HP, CMD_ATK, SummonerRules.commandBonus(cmd));
    }

    /** The highest command tier among the owner's summons (it shortens the wait of Awakening). */
    static int topCommand(ServerPlayer owner) {
        int top = 0;
        for (Mob m : owned(owner)) top = Math.max(top, commandOf(m));
        return top;
    }

    /**
     * Deputize on one summon: the next tier ({@link SummonerRules#nextCommand}); a new General turns the other summons of its card within range that have no tier into
     * Soldiers (the wiki's General does it for its kind). Returns the new tier, or -1 when refused.
     */
    static int deputize(ServerPlayer owner, Mob target, boolean kingnification) {
        int card = target.getPersistentData().getInt(CARD), generals = 0, kings = 0;
        List<Mob> kin = new ArrayList<>();
        for (Mob m : owned(owner)) {
            if (m.getPersistentData().getInt(CARD) != card) continue;
            kin.add(m);
            if (m != target) {
                int c = commandOf(m);
                if (c == SummonerRules.CMD_GENERAL) generals++;
                else if (c == SummonerRules.CMD_KING) kings++;
            }
        }
        int next = SummonerRules.nextCommand(commandOf(target), generals, kings, kingnification);
        if (next < 0) return -1;
        setCommand(target, next);
        if (next == SummonerRules.CMD_GENERAL || next == SummonerRules.CMD_KING) {
            double range2 = SummonerRules.STRENGTHEN_RANGE * SummonerRules.STRENGTHEN_RANGE;
            for (Mob m : kin) {
                if (m != target && commandOf(m) == SummonerRules.CMD_NONE && m.level() == owner.level() && m.distanceToSqr(owner) <= range2) setCommand(m, SummonerRules.CMD_SOLDIER);
            }
        }
        SkillFx.burst(owner.serverLevel(), target.position().add(0, target.getBbHeight() * 0.5, 0), next == SummonerRules.CMD_KING, 10, 0.5);
        return next;
    }

    static int rankOf(Mob m) { return Math.max(0, m.getPersistentData().getInt(RANK)); }

    /** Two summons of the same card and rank that Synthesis may combine (oldest first), or an empty list. */
    static List<Mob> pair(ServerPlayer owner, Tier tier) {
        java.util.Map<Integer, List<Mob>> groups = new java.util.TreeMap<>();
        for (Mob m : owned(owner)) {
            int rank = rankOf(m);
            if (m.level() != owner.level() || !SummonerRules.canSynthesize(rank, tier) || !m.getPersistentData().contains(CARD)) continue;
            groups.computeIfAbsent(m.getPersistentData().getInt(CARD) * 8 + rank, k -> new ArrayList<>()).add(m);
        }
        for (List<Mob> g : groups.values()) if (g.size() >= 2) return new ArrayList<>(g.subList(0, 2));
        return List.of();
    }

    /** Synthesis: the two creatures go and one of the next rank stands where the first was. Returns it, or null if it could not be made. */
    static Mob merge(ServerPlayer owner, List<Mob> two, Tier tier) {
        if (two.size() != 2) return null;
        Mob a = two.get(0);
        Card[] cards = Card.values();
        int ordinal = a.getPersistentData().getInt(CARD);
        if (ordinal < 0 || ordinal >= cards.length) return null;
        Mob made = create(owner, cards[ordinal], tier, rankOf(a) + 1);
        if (made == null) return null;
        made.moveTo(a.getX(), a.getY(), a.getZ(), a.getYRot(), 0.0F);
        for (Mob m : two) m.discard();
        if (!owner.serverLevel().addFreshEntity(made)) return null;
        track(owner, made);
        SkillFx.burst(owner.serverLevel(), made.position().add(0, made.getBbHeight() * 0.5, 0), true, 10, 0.5);
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
        SummonerSkill.reflect(player, mine);
        for (int i = 0, drop = SummonRules.excess(mine.size(), max); i < drop; i++) mine.remove(0).discard();   // oldest first

        // summons pick fights with monsters on their own, never with players (a PvP server must not see wolves hunting strangers);
        // they still defend their owner against a player through the ordinary tamed-animal AI
        List<LivingEntity> foes = new ArrayList<>(SkillTargets.foes(player, player.position(), SummonRules.ENGAGE_RANGE));
        foes.removeIf(f -> f instanceof net.minecraft.world.entity.player.Player);
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
