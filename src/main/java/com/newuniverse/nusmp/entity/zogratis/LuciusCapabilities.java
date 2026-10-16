package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.boss.god.CounterRules;
import com.newuniverse.nusmp.boss.god.DodgeEvents;
import com.newuniverse.nusmp.boss.god.StarvationMath;
import com.newuniverse.nusmp.boss.rules.BossSkillRules;
import com.newuniverse.nusmp.mana.ManaLaw;
import com.newuniverse.nusmp.relics.Relics;
import dev.architectury.event.EventResult;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillEvents;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the god-tier Lucius reaches into the players' own powers, and each reach is a small capability that does not belong to any one
 * boss class: draining the magicule pool (the Tensura EP pool that Black Clover's mana shares), reversing an Ultimate skill the moment
 * it is activated (ManasCore's skill events), silencing a player's relics and severing their Djinn and spirits (the three mob effects of
 * {@link LuciusGodEffects} and the switch points that read them), and breaking a channel. They are public static and take plain
 * arguments so the few places that must tell the boss something (the grimoire, when a page is held or released) can do it with one call,
 * and so {@link LuciusBossEventHandler} and {@link GodSteps} share one implementation.
 * <ul>
 *   <li><b>Ultimate reversal</b>: ManasCore's activation, release and toggle events. An activation of an Ultimate-grade skill (Tensura's
 *       own {@code Skill.getType()} through {@code BossSkillRules.categoryOf}) by a player in the arena is vetoed when the boss may answer
 *       ({@link LuciusGod#reverseUltimate}); a cast that was just broken ({@link #interruptCast}) vetoes the skills for a moment.</li>
 *   <li><b>Held-skill channels</b>: a skill with a hold time of 30 ticks or more is watched from its activation; held for 30 ticks it is
 *       a "massive channel" (the Checkmate counter), released after that it is a "launched devastating attack" (the spatial swap).</li>
 *   <li><b>Dodge skills</b>: a skill whose id names a dodge, an evasion, a blink or a foresight is reported to {@link DodgeEvents}.</li>
 * </ul>
 */
public final class LuciusCapabilities {
    private LuciusCapabilities() {}

    private static boolean done;
    private static final Map<UUID, Long> INTERRUPT_UNTIL = new HashMap<>();
    private static final Map<UUID, CounterRules.ChannelWatch> WATCH = new HashMap<>();

    /** Registers the ManasCore skill events, once. Called by {@link LuciusBossEventHandler#register()}. */
    static void register() {
        if (done) return;
        done = true;
        SkillEvents.ACTIVATE_SKILL.register(LuciusCapabilities::onActivate);
        SkillEvents.RELEASE_SKILL.register(LuciusCapabilities::onRelease);
        SkillEvents.TOGGLE_SKILL.register(LuciusCapabilities::onToggle);
    }

    static void forget(UUID id) {
        INTERRUPT_UNTIL.remove(id);
        WATCH.remove(id);
    }

    static void clearStatics() {
        INTERRUPT_UNTIL.clear();
        WATCH.clear();
    }

    /** The god-tier boss whose arena this player stands in, or null. */
    static LuciusBoss arenaBoss(ServerPlayer p) {
        for (LuciusBoss b : LuciusGod.live()) if (b.isAlive() && b.fightActive() && b.god().inArena(p)) return b;
        return null;
    }

    /** Is a god-tier fight on anywhere? (The grimoire asks before it does any work for the boss.) */
    public static boolean fightOn() { return LuciusGod.anyActive(); }

    private static boolean interrupted(ServerPlayer p, long now) {
        Long t = INTERRUPT_UNTIL.get(p.getUUID());
        return t != null && now < t;
    }

    // ================================================================ the pool
    /**
     * Drains 'fraction' (0..1) of the player's maximum magicule, never below the starvation floor ({@link StarvationMath#FLOOR_FRACTION}),
     * never touching health. Creative and spectator players and players without a Tensura record are left alone. True if the pool went down.
     */
    public static boolean drainMagicule(ServerPlayer p, double fraction) {
        if (p == null || p.isCreative() || p.isSpectator() || !(fraction > 0.0)) return false;
        try {
            var ex = TensuraStorages.getExistenceFrom(p);
            if (ex == null) return false;
            double max = EnergyHelper.getMaxMagicule(p);
            if (!(max > 0.0)) return false;
            double cur = ex.getMagicule(), next = StarvationMath.apply(cur, max, max * fraction);
            if (!(next < cur)) return false;
            ex.setMagicule(next);
            ex.markDirty();
            return true;
        } catch (Throwable noTensura) {
            return false;
        }
    }

    // ================================================================ sabotage
    /** De-attunes every relic of the player for 'ticks' (the effect {@code nusmp:relic_nullified}); nothing is unequipped. */
    public static void nullifyRelics(ServerPlayer p, int ticks) {
        if (!LuciusGodEffects.mark(p, LuciusGodEffects.RELIC_NULLIFIED, ticks)) return;
        Relics.invalidate(p);
        com.newuniverse.nusmp.dungeon.nexus.relic.Worn.forget(p.getUUID());
        p.displayClientMessage(Component.literal("Spatial pulse: your relics go silent for " + ticks / 20 + " seconds. They are still yours.")
                .withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
    }

    /** Severs the player from their Djinn and spirits for 'ticks' (the effect {@code nusmp:severed}); the handler suspends the Djinn on the next tick. */
    public static void sever(ServerPlayer p, int ticks) {
        if (!LuciusGodEffects.mark(p, LuciusGodEffects.SEVERED, ticks)) return;
        p.displayClientMessage(Component.literal("Severance: your Djinn and your spirits do not answer for " + ticks / 20 + " seconds. Nothing is lost.")
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
    }

    /** Takes all three of the boss's effects off one player (leaving the arena, the end of the fight) and lets the relics and the Djinn come back at once. */
    static void clearSabotage(ServerPlayer p) {
        boolean any = p.hasEffect(LuciusGodEffects.RELIC_NULLIFIED) || p.hasEffect(LuciusGodEffects.SEVERED) || p.hasEffect(LuciusGodEffects.TIME_DECAY);
        p.removeEffect(LuciusGodEffects.RELIC_NULLIFIED);
        p.removeEffect(LuciusGodEffects.SEVERED);
        p.removeEffect(LuciusGodEffects.TIME_DECAY);
        TimeStasisTrap.clearMark(p);
        if (any) Relics.invalidate(p);
    }

    /** The fight is over or the boss is gone: nobody in this level keeps a mark of his. */
    static void clearAll(ServerLevel sl) {
        for (ServerPlayer p : sl.players()) clearSabotage(p);
    }

    // ================================================================ breaking a channel
    /**
     * Breaks whatever the player is channelling: the grimoire refuses casts for 'ticks' (the cast that is winding up fizzles), a Mana
     * Zone is switched off, anything in use is let go, and for the same time ManasCore skills are vetoed. Nothing is spent or lost
     * beyond the cast itself.
     */
    public static void interruptCast(ServerPlayer p, int ticks) {
        long now = p.level().getGameTime(), until = now + Math.max(1, ticks);
        INTERRUPT_UNTIL.put(p.getUUID(), until);
        var data = p.getPersistentData();
        if (data.getLong(SpellSeal.SEALED_FLAG) < until) data.putLong(SpellSeal.SEALED_FLAG, until);
        if (ManaLaw.zoneActive(p)) ManaLaw.toggleZone(p);
        p.stopUsingItem();
        CounterRules.ChannelWatch w = WATCH.get(p.getUUID());
        if (w != null) w.end();
    }

    // ================================================================ what the grimoire tells us
    /** A grimoire key is held: a heavy page held long enough is a massive channel (the Checkmate counter). Called every tick the key is held. */
    public static void onGrimoireHold(ServerPlayer p, BookPage page, int heldTicks, int castTicks, boolean castable) {
        if (page == null || !LuciusGod.anyActive()) return;
        if (!CounterRules.massiveGrimoire(page.costPercent(), false, heldTicks, castTicks, castable)) return;
        LuciusBoss b = arenaBoss(p);
        if (b != null) b.god().requestCheckmate(p);
    }

    /** A grimoire key was released: a heavy page that was held long enough is a launched devastating attack (the spatial swap). */
    public static void onGrimoireRelease(ServerPlayer p, BookPage page, int heldTicks, int castTicks, boolean castable) {
        if (page == null || !LuciusGod.anyActive()) return;
        if (!CounterRules.massiveGrimoire(page.costPercent(), false, heldTicks, castTicks, castable)) return;
        LuciusBoss b = arenaBoss(p);
        if (b != null) b.god().trySwap(p);
    }

    /** The Mana Zone was switched on: that is a massive channel at once (the Checkmate counter). */
    public static void onManaZone(ServerPlayer p) {
        if (!LuciusGod.anyActive()) return;
        LuciusBoss b = arenaBoss(p);
        if (b != null) b.god().requestCheckmate(p);
    }

    /** Is a held skill of this player a massive channel by now? (Polled by the handler's player tick.) */
    static boolean skillChannelMassive(ServerPlayer p, long now) {
        CounterRules.ChannelWatch w = WATCH.get(p.getUUID());
        return w != null && w.massive(now);
    }

    // ================================================================ ManasCore's events
    private static boolean dodgeLike(ManasSkillInstance inst) {
        var id = inst.getSkillId();
        if (id == null) return false;
        String path = id.getPath().toLowerCase(Locale.ROOT);
        return path.contains("dodge") || path.contains("evasion") || path.contains("evade") || path.contains("blink") || path.contains("instant_move")
                || path.contains("foresight") || path.contains("precognition") || path.contains("future_sight");
    }

    /** Tensura's own flying skills: Gravity Flight, the Flight magic and the Air Flight art (they fly by velocity, not by the abilities flag). */
    private static boolean flightSkill(ManasSkillInstance inst) {
        var s = inst.getSkill();
        return s instanceof io.github.manasmods.tensura.ability.skill.common.GravityFlightSkill
                || s instanceof io.github.manasmods.tensura.ability.magic.aspectual.misc.FlightMagic
                || s instanceof io.github.manasmods.tensura.ability.battlewill.utility.AirFlightArt;
    }

    private static EventResult onActivate(Changeable<ManasSkillInstance> skill, LivingEntity user, int key, int mode) {
        if (!(user instanceof ServerPlayer p) || p.level().isClientSide || !LuciusGod.anyActive()) return EventResult.pass();
        ManasSkillInstance inst = skill.get();
        if (inst == null || inst.getSkill() == null) return EventResult.pass();
        long now = p.level().getGameTime();
        if (interrupted(p, now)) return EventResult.interruptFalse();
        LuciusBoss b = arenaBoss(p);
        if (b == null) return EventResult.pass();
        if (com.newuniverse.nusmp.boss.god.LuciusGodConfig.antiFlight() && flightSkill(inst) && (!p.isCreative() || com.newuniverse.nusmp.boss.god.LuciusGodConfig.affectCreative())) {
            p.displayClientMessage(Component.literal("Lucifero's gravity holds you to the ground. You cannot fly here.").withStyle(ChatFormatting.DARK_PURPLE), true);
            return EventResult.interruptFalse();
        }
        String cat = BossSkillRules.categoryOf(inst.getSkill());
        if (CounterRules.reversible(cat) && b.god().reverseUltimate(p, inst)) {
            INTERRUPT_UNTIL.put(p.getUUID(), now + 40);                       // the release of the cancelled activation is cancelled too
            return EventResult.interruptFalse();
        }
        if (dodgeLike(inst)) DodgeEvents.record(p);
        if (!(inst.getSkill() instanceof com.newuniverse.nusmp.book.GrimoireBook) && inst.getMaxHeldTime(p) >= CounterRules.SKILL_CHANNEL_TICKS)
            WATCH.computeIfAbsent(p.getUUID(), k -> new CounterRules.ChannelWatch()).begin(now, cat);
        return EventResult.pass();
    }

    private static EventResult onRelease(Changeable<ManasSkillInstance> skill, LivingEntity user, int key, int mode, Changeable<Integer> held) {
        if (!(user instanceof ServerPlayer p) || p.level().isClientSide) return EventResult.pass();
        CounterRules.ChannelWatch w = WATCH.get(p.getUUID());
        if (w == null && !interrupted(p, p.level().getGameTime())) return EventResult.pass();
        long now = p.level().getGameTime();
        if (interrupted(p, now)) {
            if (w != null) w.end();
            return EventResult.interruptFalse();
        }
        boolean massive = w != null && w.massive(now);
        if (w != null) w.end();
        if (massive && LuciusGod.anyActive()) {
            LuciusBoss b = arenaBoss(p);
            if (b != null) b.god().trySwap(p);
        }
        return EventResult.pass();
    }

    private static EventResult onToggle(Changeable<ManasSkillInstance> skill, LivingEntity user) {
        if (!(user instanceof ServerPlayer p) || p.level().isClientSide || !LuciusGod.anyActive()) return EventResult.pass();
        ManasSkillInstance inst = skill.get();
        if (inst == null || inst.getSkill() == null) return EventResult.pass();
        long now = p.level().getGameTime();
        if (interrupted(p, now)) return EventResult.interruptFalse();
        LuciusBoss b = arenaBoss(p);
        if (b == null || inst.isToggled()) return EventResult.pass();             // only switching an ultimate ON is reversed; switching it off is always allowed
        if (CounterRules.reversible(BossSkillRules.categoryOf(inst.getSkill())) && b.god().reverseUltimate(p, inst)) return EventResult.interruptFalse();
        return EventResult.pass();
    }
}
