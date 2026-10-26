package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.perf.PerfProbe;
import com.newuniverse.nusmp.skilltree.data.RouteEvolution;
import com.newuniverse.nusmp.skilltree.data.RouteRegistry;
import com.newuniverse.nusmp.skilltree.data.SkillNode;
import com.newuniverse.nusmp.skilltree.data.SkillRoute;
import com.newuniverse.nusmp.skilltree.unique.UniqueBooks;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.Tags;

import java.util.ArrayList;
import java.util.List;

/**
 * Why (3.26.0, the Evolution skills): the game-facing half of the Evolution feature. A line may carry an {@code evolution} requirement (kill so many elites, take so much fire, survive so many near-lethal
 * blows) and one Ultimate art that stays locked until it is met AND the skill is mastered; its passives may carry an {@code evolved} block that takes over once it is. This class counts. Each hook below is
 * called from one event of {@link LineEvents} (a kill, a blow dealt, a blow taken, an art cast, an ally buffed) with the facts of that event, finds the player's OWNED, SWITCHED-ON skills whose requirement is
 * that kind (the same ownership and switch rules the passive gate uses: {@link UniqueBooks#ownedLines}, {@code on()}), asks the pure rules ({@link EvolutionRules}: what counts, the clamp, the whole points of
 * damage) and writes the counter into the player's OWN persistent data ({@link LineState}, key {@code evo.<skill id>}), never a static map, so each player on a server has their own progress and it survives
 * a relog. There is no scan: the hooks run only when the event happens (a player's own blow, a blow they took, a cast), cost one skill-list read plus a few integer operations, and a player who owns no
 * evolution skill pays one list read and returns. Progress is shown to the player by mirroring it into the skill instance's own tag, which ManasCore already syncs to its owner: {@link #beat} does it at most
 * every {@link EvolutionRules#MIRROR_TICKS} ticks (and at once when the skill evolves), so a counter that moves every hit is a packet every two seconds at worst and no packet of ours. The whole class does
 * nothing when the server setting {@code evolutionSkills} is off. Server thread only.
 */
public final class Evolution {
    private Evolution() {}

    private static final PerfProbe.Section PROBE = PerfProbe.section("s.line.evolution");

    /** An owned, switched-on skill that counts something right now. */
    private record Tracked(UniqueBooks.Owned owned, SkillRoute route, RouteEvolution evo) {}

    // ================================================================== the state
    /** The player's counter for this skill (0 when it never counted). */
    public static int progress(LivingEntity holder, String skillId) { return LineState.evolution(holder, skillId); }

    /** True when the counter of this line's requirement has reached its count (ignores the server setting; the feature switch is {@link #evolvedNow}). */
    public static boolean isEvolved(LivingEntity holder, SkillRoute route) {
        return route != null && route.hasEvolution() && EvolutionRules.evolved(progress(holder, route.id()), route.evolution().count());
    }

    /** Evolved AND the feature is on: what the passives and the Ultimate gate read. */
    public static boolean evolvedNow(LivingEntity holder, SkillRoute route) { return EvolutionConfig.enabled() && isEvolved(holder, route); }

    /** Adds {@code delta} to a counter, clamped to {@code 0..count}; returns the new value. The one place a counter is written. */
    public static int bump(LivingEntity holder, String skillId, int count, long delta) {
        int cur = LineState.evolution(holder, skillId);
        int next = EvolutionRules.add(cur, delta, count);
        if (next != cur) LineState.setEvolution(holder, skillId, next);
        return next;
    }

    /** The route of an owned skill that counts for {@code requirement} (switched on, not yet evolved), or null. */
    private static Tracked trackedOf(ServerPlayer p, UniqueBooks.Owned o, String requirement) {
        if (!o.on()) return null;
        SkillRoute r = RouteRegistry.byId(o.skill().routeId());
        if (r == null || !r.isLine() || !r.hasEvolution()) return null;
        RouteEvolution ev = r.evolution();
        if (requirement != null && !requirement.equals(ev.requirement())) return null;
        if (EvolutionRules.evolved(progress(p, r.id()), ev.count())) return null;            // already there: nothing more to count
        return new Tracked(o, r, ev);
    }

    /** The skills of the player that count for {@code requirement} right now. Empty (no allocation) for a player who owns no book. */
    private static List<Tracked> tracked(ServerPlayer p, String requirement) {
        if (!EvolutionConfig.enabled()) return List.of();
        List<UniqueBooks.Owned> owned = UniqueBooks.ownedLines(p);
        if (owned.isEmpty()) return List.of();
        List<Tracked> out = null;
        for (UniqueBooks.Owned o : owned) {
            Tracked t = trackedOf(p, o, requirement);
            if (t == null) continue;
            if (out == null) out = new ArrayList<>(2);
            out.add(t);
        }
        return out == null ? List.of() : out;
    }

    private static void add(ServerPlayer p, Tracked t, long delta) {
        if (delta <= 0) return;
        String id = t.route.id();
        int count = t.evo.count();
        int before = progress(p, id);
        int after = bump(p, id, count, delta);
        if (after != before && EvolutionRules.evolved(after, count) && !EvolutionRules.evolved(before, count)) {
            announce(p, t.route, t.evo);
            mirror(p, t.owned, t.route, t.evo, true);
        }
    }

    /** Damage is counted in whole points; the fraction left over is carried in the player's data so many small hits are not lost. */
    private static void addPoints(ServerPlayer p, Tracked t, double amount) {
        String carryKey = LineState.EVO_CARRY + t.route.id();
        EvolutionRules.Whole w = EvolutionRules.takeWhole(LineState.getDouble(p, carryKey), amount);
        LineState.putDouble(p, carryKey, w.rest());
        add(p, t, w.whole());
    }

    // ================================================================== what is a foe
    /** A creature that can count as a foe of this player: not themselves, not a player, not an ally, not anyone's pet. */
    static boolean isFoe(ServerPlayer p, LivingEntity v) {
        if (v == null || v == p || v instanceof Player || v.isAlliedTo(p)) return false;
        return !(v instanceof OwnableEntity pet && pet.getOwnerUUID() != null);
    }

    static boolean isHostile(LivingEntity v) { return v instanceof Enemy || v.getType().getCategory() == MobCategory.MONSTER; }

    // ================================================================== the hooks
    /** The player killed {@code victim}. */
    public static void onKill(ServerPlayer killer, LivingEntity victim) {
        long t0 = PerfProbe.begin();
        try {
            List<Tracked> list = tracked(killer, null);
            if (list.isEmpty() || !isFoe(killer, victim)) return;
            boolean elite = EvolutionRules.isElite(victim.getType().is(Tags.EntityTypes.BOSSES), victim.getMaxHealth());
            boolean boss = BalanceLaw.isBoss(victim);
            boolean counts = EvolutionRules.countsAsFoe(isHostile(victim), elite);
            for (Tracked t : list) if (EvolutionRules.killCounts(t.evo.requirement(), counts, elite, boss)) add(killer, t, 1);
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    /** The player dealt {@code amount} (final damage) to {@code victim}. Only hostile creatures and elites count. */
    public static void onDealt(ServerPlayer attacker, LivingEntity victim, float amount) {
        if (!(amount > 0f)) return;
        long t0 = PerfProbe.begin();
        try {
            List<Tracked> list = tracked(attacker, "damage_dealt");
            if (list.isEmpty() || !isFoe(attacker, victim)) return;
            boolean elite = EvolutionRules.isElite(victim.getType().is(Tags.EntityTypes.BOSSES), victim.getMaxHealth());
            if (!EvolutionRules.countsAsFoe(isHostile(victim), elite)) return;
            for (Tracked t : list) addPoints(attacker, t, amount);
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    /** The player took {@code amount} (final damage) from {@code src} and is still standing or fell. */
    public static void onTaken(ServerPlayer victim, DamageSource src, float amount) {
        if (!(amount > 0f) || src.getEntity() == victim) return;                  // damage the player did to themselves is not a trial
        long t0 = PerfProbe.begin();
        try {
            for (Tracked t : tracked(victim, "damage_taken")) {
                if (EvolutionRules.damageKindMatches(t.evo.damageKindOrAny(), k -> LineLookups.damageKind(k, src))) addPoints(victim, t, amount);
            }
            if (EvolutionRules.lethalSurvived(victim.getHealth(), amount, victim.isAlive())) {
                List<Tracked> lethal = tracked(victim, "lethal_survived");
                if (!lethal.isEmpty() && LineState.tryRest(victim, "evo.lethal", victim.serverLevel().getGameTime(), EvolutionRules.LETHAL_REST_TICKS)) {
                    for (Tracked t : lethal) add(victim, t, 1);
                }
            }
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    /** The player cast an art of {@code route} (the Ultimate art itself does not count). */
    public static void onArtCast(ServerPlayer p, SkillRoute route, SkillNode node) {
        if (node.ultimate() || route == null || !route.hasEvolution() || !"arts_cast".equals(route.evolution().requirement())) return;
        countOwn(p, route, 1);
    }

    /** An art of {@code route} reached {@code allies} allies (a buff or a heal; the caster does not count). */
    public static void onAllyBuffs(ServerPlayer p, SkillRoute route, int allies) {
        if (allies <= 0 || route == null || !route.hasEvolution() || !"ally_buffs".equals(route.evolution().requirement())) return;
        countOwn(p, route, allies);
    }

    private static void countOwn(ServerPlayer p, SkillRoute route, long delta) {
        if (!EvolutionConfig.enabled()) return;
        UniqueBooks.Owned o = UniqueBooks.ownedOf(p, route.id());
        if (o == null) return;
        Tracked t = trackedOf(p, o, route.evolution().requirement());
        if (t != null) add(p, t, delta);
    }

    // ================================================================== the Ultimate gate
    /** Whether the player may cast {@code node} of {@code route} now (its mastery is read from the skill the player owns). */
    public static EvolutionRules.Gate gate(ServerPlayer p, SkillRoute route, SkillNode node) {
        if (node == null || !node.ultimate()) return EvolutionRules.Gate.OPEN;
        UniqueBooks.Owned o = UniqueBooks.ownedOf(p, route.id());
        return gate(p, route, node, o == null ? 0.0 : o.fraction());
    }

    /** The same with the mastery fraction given (the self-test and the tests hold it fixed). */
    public static EvolutionRules.Gate gate(LivingEntity holder, SkillRoute route, SkillNode node, double masteryFraction) {
        if (node == null || route == null) return EvolutionRules.Gate.OPEN;
        int count = route.hasEvolution() ? route.evolution().count() : 0;
        return EvolutionRules.gate(EvolutionConfig.enabled(), node.ultimate(), route.hasEvolution(), progress(holder, route.id()), count, masteryFraction);
    }

    /** Says why the Ultimate art is locked, on the action bar. */
    public static void refuse(ServerPlayer p, SkillRoute route, SkillNode node, EvolutionRules.Gate why) {
        if (why == EvolutionRules.Gate.OPEN) return;
        RouteEvolution ev = route.evolution();
        MutableComponent msg = why == EvolutionRules.Gate.NOT_EVOLVED
                ? EvolutionText.tr(EvolutionText.LOCKED, node.name(), ev == null ? "" : ev.text(), EvolutionRules.progressText(progress(p, route.id()), ev == null ? 0 : ev.count()))
                : EvolutionText.tr(EvolutionText.NEEDS_MASTERY, node.name());
        p.displayClientMessage(msg.withStyle(ChatFormatting.RED), true);
    }

    // ================================================================== telling the player
    private static void announce(ServerPlayer p, SkillRoute route, RouteEvolution ev) {
        p.displayClientMessage(EvolutionText.tr(EvolutionText.EVOLVED, route.name(), ev.ultimateName()).withStyle(ChatFormatting.GOLD), true);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.9f, 0.7f);
        LineFx.rukh(p.serverLevel(), route, p, 10);
    }

    /** The index of the Ultimate art among the skill's arts (the modes), -1 when the line has none. */
    static int ultimateIndex(SkillRoute route) {
        List<SkillNode> arts = UniqueBooks.arts(route);
        for (int i = 0; i < arts.size(); i++) if (arts.get(i).ultimate()) return i;
        return -1;
    }

    /**
     * Copies the player's progress into the skill instance's own tag (which ManasCore syncs to its owner) so the skill card can show "n / count" and the evolved name: at most every
     * {@link EvolutionRules#MIRROR_TICKS} ticks while it changes, at once when {@code force} (the skill just evolved), and once the first time so the card knows which art is the Ultimate.
     */
    private static void mirror(ServerPlayer p, UniqueBooks.Owned o, SkillRoute route, RouteEvolution ev, boolean force) {
        ManasSkillInstance inst = o.instance();
        int actual = progress(p, route.id());
        CompoundTag tag = inst.getTag();
        boolean published = tag != null && tag.contains(EvolutionRules.TAG_COUNT);
        int mirrored = tag == null ? 0 : tag.getInt(EvolutionRules.TAG_PROGRESS);
        long now = p.serverLevel().getGameTime();
        if (published && !force && !EvolutionRules.shouldMirror(mirrored, actual, ev.count(), LineState.getLong(p, LineState.EVO_MIRROR + route.id()), now)) return;
        if (published && mirrored == actual && tag.getInt(EvolutionRules.TAG_COUNT) == ev.count()) return;
        CompoundTag w = inst.getOrCreateTag();
        w.putInt(EvolutionRules.TAG_PROGRESS, actual);
        w.putInt(EvolutionRules.TAG_COUNT, ev.count());
        w.putInt(EvolutionRules.TAG_ULTIMATE, ultimateIndex(route));
        LineState.putLong(p, LineState.EVO_MIRROR + route.id(), now);
        inst.markDirty();
    }

    /** The half-second beat of a player (from {@link LinePassives#beat}): refreshes the progress mirror of every owned evolution skill and drops expired Debt. */
    public static void beat(ServerPlayer p) {
        Debts.prune(p);
        if (!EvolutionConfig.enabled()) return;
        List<UniqueBooks.Owned> owned = UniqueBooks.ownedLines(p);
        if (owned.isEmpty()) return;
        long t0 = PerfProbe.begin();
        try {
            for (UniqueBooks.Owned o : owned) {
                SkillRoute r = RouteRegistry.byId(o.skill().routeId());
                if (r != null && r.isLine() && r.hasEvolution()) mirror(p, o, r, r.evolution(), false);
            }
        } finally {
            PerfProbe.end(PROBE, t0);
        }
    }

    // ================================================================== death and respawn
    /**
     * A player's persistent data is copied to the new body on respawn by NeoForge's own player clone; as a belt-and-braces this copies the evolution keys over when they did not arrive, so the progress a
     * player worked for is never lost to a death (it never goes down on purpose either).
     */
    public static void onClone(ServerPlayer original, ServerPlayer fresh) {
        CompoundTag from = LineState.root(original), to = LineState.root(fresh);
        for (String k : new ArrayList<>(from.getAllKeys())) {
            if (k.startsWith("evo") && !to.contains(k)) to.put(k, from.get(k).copy());
        }
    }
}
