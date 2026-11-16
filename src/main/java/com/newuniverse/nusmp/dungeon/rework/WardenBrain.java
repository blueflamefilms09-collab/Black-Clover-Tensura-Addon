package com.newuniverse.nusmp.dungeon.rework;

import com.newuniverse.nusmp.audio.Cue;
import com.newuniverse.nusmp.audio.MobVoice;
import com.newuniverse.nusmp.dungeon.rework.DungeonReworkRules.Move;
import com.newuniverse.nusmp.dungeon.rework2.DungeonBossSoundRules;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import com.newuniverse.nusmp.labyrinth.seal.FloorWardenEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Floor Warden's move set (docs/dungeon_bosses_rework.md). Until this rework it had two moves (a Ground Slam when close, a Shadow Step when far) and no telegraph, so a fight was "hit it
 * until it dies". The brain gives it five distinct moves with a readable wind-up each: Hammer Slam (a ring on the floor, step out), Chain Lash (violet chains tie up to three players and, when they
 * snap, drag them in), Shadow Step (a mark on the floor where it will land, then a cleave there), Seal Pulse (phase 2: three rings roll out from it, jump them) and, in phase 3 at a quarter of its
 * health, the Key Toll (two lit sigils appear: stand in one or take the whole toll). Phases change at 50 % and 25 % (its bar, its pace, its cooldowns), and a soft timer (150 s of one fight) enrages it.
 * The Warden's old two-moves AI stays in {@link FloorWardenEntity} behind the server setting {@code dungeonBosses.rework}; this class runs only when {@link FloorWardenEntity#reworkOn()} says so.
 * <p>
 * SERVER ONLY, ONE PER ENTITY, NO STATIC STATE: all of it (cooldowns, the active move, the marked players as UUIDs) is a field of the brain, so a second Warden never shares anything and everything
 * disappears with the entity (death, despawn, dimension change: the entity is gone and so is the brain; a logout of a marked player only makes the lash skip it). Cost per tick: one boolean when it
 * has no target; during a move at most one bounded player query ({@link DungeonStrikes#victims}, 8 players) per tick and a few facade effects every 3 to 5 ticks. No entity is ever spawned here.
 */
public final class WardenBrain {
    private static final ResourceLocation PHASE3_SPEED = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "warden_phase3_speed");

    private final long[] readyAt = new long[DungeonReworkRules.WARDEN.size()];
    /** Rework 2: its own sound gate (cooldown per cue, four a second) and its own particle theme (colours of the Warden: seal gold over iron, a violet shadow set). */
    private final DungeonBossSoundRules.Limiter cues = new DungeonBossSoundRules.Limiter();
    private final ThemeKey theme = ThemeKey.of("dungeon.boss.warden");
    private int move = -1, t, fight, idle, phase = 1;
    private long gapUntil;
    private boolean enraged;
    private Vec3 stepTo;
    private double[] sigils;
    private final List<UUID> marked = new ArrayList<>(3);
    private final Map<UUID, Integer> waveHits = new HashMap<>();

    /** The move being wound up or resolved (an index of {@link DungeonReworkRules#WARDEN}), or -1. */
    public int activeMove() { return move; }

    public int phase() { return phase; }

    public boolean enraged() { return enraged; }

    /** Forgets everything (death, a wipe). */
    public void reset() {
        move = -1;
        t = 0;
        marked.clear();
        waveHits.clear();
        stepTo = null;
        sigils = null;
    }

    private double dmg(double base) { return base * DungeonReworkRules.damageFactor(enraged); }

    // ---------------------------------------------------------------- the tick
    public void tick(FloorWardenEntity w, ServerLevel sl) {
        if (!w.isAlive()) return;
        long now = sl.getGameTime();
        LivingEntity target = w.getTarget();
        boolean fighting = target != null && target.isAlive();
        if (fighting) {
            fight++;
            idle = 0;
        } else if (move < 0 && ++idle > 200) {                                      // ten quiet seconds: the fight is over, the soft timer starts again
            fight = 0;
            enraged = false;
        }
        int p = DungeonReworkRules.phase(w.getMaxHealth() <= 0f ? 1.0 : w.getHealth() / (double) w.getMaxHealth());
        if (p > phase) {
            phase = p;
            onPhase(w, sl);
        }
        if (!enraged && DungeonReworkRules.enraged(fight)) {
            enraged = true;
            announce(w, sl, "enrage");
            DungeonStrikes.ring(sl, theme, w.getX(), w.getY() + 0.1, w.getZ(), 8.0, 32, true);
            DungeonStrikes.cue(sl, w, cues, "roar", SoundEvents.RAVAGER_ROAR, 1.6f, 0.6f);
        }
        if (phase >= 2 && w.tickCount % (phase >= 3 ? 10 : 20) == 0) DungeonStrikes.burst(sl, theme, w.getX(), w.getY() + w.getBbHeight() * 0.55, w.getZ(), phase >= 3 ? 5 : 3, 0.6, phase >= 3);
        if (move >= 0) {
            advance(w, sl, target, now);
            return;
        }
        if (!fighting || now < gapUntil) return;
        int i = DungeonReworkRules.choose(DungeonReworkRules.WARDEN, Math.sqrt(w.distanceToSqr(target)), phase, now, readyAt, w.getRandom().nextInt());
        if (i >= 0) begin(w, sl, i, target, now);
    }

    // ---------------------------------------------------------------- phases and warnings
    private void onPhase(FloorWardenEntity w, ServerLevel sl) {
        w.setBarPhase(phase);
        MobVoice.play(w, w.soundFamily(), Cue.AGGRO);
        DungeonStrikes.ring(sl, theme, w.getX(), w.getY() + 0.1, w.getZ(), 10.0, 36, phase >= 3);
        DungeonStrikes.burst(sl, theme, w.getX(), w.getY() + w.getBbHeight() * 0.5, w.getZ(), 30, 1.0, phase >= 3);
        DungeonStrikes.flash(sl, theme, w.getX(), w.getY() + 1.0, w.getZ());
        if (phase >= 3) {
            DungeonStrikes.cue(sl, w, cues, "gong", SoundEvents.WITHER_SPAWN, 0.9f, 1.3f);
            AttributeInstance speed = w.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null && speed.getModifier(PHASE3_SPEED) == null) speed.addTransientModifier(new AttributeModifier(PHASE3_SPEED, 0.12, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        } else {
            DungeonStrikes.cue(sl, w, cues, "gong", SoundEvents.RAVAGER_ROAR, 1.5f, 0.8f);
        }
        announce(w, sl, phase >= 3 ? "p3" : "p2");
    }

    private void announce(FloorWardenEntity w, ServerLevel sl, String key) {
        Component line = Component.translatable("message.anime_multiverse.dboss.warden." + key).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC);
        for (Player p : DungeonStrikes.victims(sl, w, 48.0)) p.displayClientMessage(line, true);
    }

    // ---------------------------------------------------------------- a move begins
    private void begin(FloorWardenEntity w, ServerLevel sl, int i, LivingEntity target, long now) {
        Move m = DungeonReworkRules.WARDEN.get(i);
        marked.clear();
        waveHits.clear();
        stepTo = null;
        sigils = null;
        switch (i) {
            case DungeonReworkRules.W_LASH -> {
                for (Player p : DungeonStrikes.victims(sl, w, 24.0)) {
                    if (p.distanceToSqr(w) >= 16.0 && marked.size() < 3) marked.add(p.getUUID());
                }
                if (marked.isEmpty()) {
                    readyAt[i] = now + 20;
                    return;
                }
                DungeonStrikes.cue(sl, w, cues, "chain", SoundEvents.CHAIN_PLACE, 1.6f, 0.5f);
            }
            case DungeonReworkRules.W_STEP -> {
                stepTo = findSpot(sl, w, target);
                if (stepTo == null) {
                    readyAt[i] = now + 40;
                    return;
                }
                DungeonStrikes.burst(sl, theme, w.getX(), w.getY() + 1.0, w.getZ(), 14, 0.5, true);
                DungeonStrikes.cue(sl, w, cues, "step", SoundEvents.ENDERMAN_TELEPORT, 1.0f, 0.5f);
            }
            case DungeonReworkRules.W_PULSE -> DungeonStrikes.cue(sl, w, cues, "rise", SoundEvents.WARDEN_SONIC_CHARGE, 1.0f, 0.8f);
            case DungeonReworkRules.W_TOLL -> {
                sigils = DungeonReworkRules.sigils(w.getX(), w.getZ(), w.getRandom().nextDouble() * Math.PI * 2.0);
                announce(w, sl, "toll");
                DungeonStrikes.cue(sl, w, cues, "charge", SoundEvents.BELL_RESONATE, 2.0f, 0.5f);
            }
            default -> DungeonStrikes.cue(sl, w, cues, "rise", SoundEvents.ANVIL_PLACE, 1.0f, 0.5f);          // the hammer rises
        }
        move = i;
        t = 0;
        readyAt[i] = now + m.windup() + DungeonReworkRules.cooldown(m.cooldown(), phase, enraged);
    }

    // ---------------------------------------------------------------- wind-up, landing, aftermath
    private static int resolveTicks(int i) { return i == DungeonReworkRules.W_PULSE ? 30 : 1; }

    private void advance(FloorWardenEntity w, ServerLevel sl, LivingEntity target, long now) {
        Move m = DungeonReworkRules.WARDEN.get(move);
        t++;
        if (t < m.windup()) {
            if (move != DungeonReworkRules.W_STEP) w.getNavigation().stop();
            if (target != null && target.isAlive()) w.getLookControl().setLookAt(target, 40f, 40f);
            telegraph(w, sl, t);
        } else if (t == m.windup()) {
            land(w, sl);
        } else if (move == DungeonReworkRules.W_PULSE) {
            pulse(w, sl, t - m.windup() - 1);
        }
        if (t >= m.windup() + resolveTicks(move)) {
            gapUntil = now + DungeonReworkRules.WARDEN_GAP + (move == DungeonReworkRules.W_TOLL ? 60 : 0);        // the Toll leaves it winded: a window to hit it
            move = -1;
            t = 0;
            marked.clear();
            waveHits.clear();
            stepTo = null;
            sigils = null;
        }
    }

    private void telegraph(FloorWardenEntity w, ServerLevel sl, int tick) {
        switch (move) {
            case DungeonReworkRules.W_SLAM -> {
                if (tick % 4 == 1) DungeonStrikes.ring(sl, theme, w.getX(), w.getY() + 0.1, w.getZ(), 4.5, 24, false);
            }
            case DungeonReworkRules.W_LASH -> {
                if (tick % 3 == 1) for (UUID id : marked) {
                    Player p = sl.getPlayerByUUID(id);
                    if (p != null && p.isAlive()) DungeonStrikes.line(sl, theme, w.position().add(0, w.getBbHeight() * 0.7, 0), p.position().add(0, 1.0, 0), true, 3);
                }
            }
            case DungeonReworkRules.W_STEP -> {
                if (stepTo != null && tick % 3 == 1) DungeonStrikes.ring(sl, theme, stepTo.x, stepTo.y + 0.1, stepTo.z, 1.8, 16, true);
            }
            case DungeonReworkRules.W_PULSE -> {
                if (tick % 5 == 1) DungeonStrikes.ring(sl, theme, w.getX(), w.getY() + 0.1, w.getZ(), Math.max(2.0, 14.0 - tick * 0.4), 28, false);
            }
            case DungeonReworkRules.W_TOLL -> {
                if (tick % 5 == 1 && sigils != null) {
                    for (int i = 0; i + 1 < sigils.length; i += 2) {
                        DungeonStrikes.ring(sl, theme, sigils[i], w.getY() + 0.1, sigils[i + 1], DungeonReworkRules.SIGIL_RADIUS, 24, false);
                        DungeonStrikes.burst(sl, theme, sigils[i], w.getY() + 1.0, sigils[i + 1], 3, 0.3, false);
                    }
                }
                if (tick % 10 == 1) DungeonStrikes.ring(sl, theme, w.getX(), w.getY() + 0.1, w.getZ(), 22.0, 40, true);
                if (tick == 35 || tick == 55) DungeonStrikes.cue(sl, w, cues, "toll", SoundEvents.BELL_RESONATE, 2.0f, tick == 35 ? 0.6f : 0.75f);
            }
            default -> { }
        }
    }

    private void land(FloorWardenEntity w, ServerLevel sl) {
        switch (move) {
            case DungeonReworkRules.W_SLAM -> {
                for (Player p : DungeonStrikes.victims(sl, w, 4.5)) {
                    if (DungeonStrikes.strike(w, p, dmg(9.0), false)) DungeonStrikes.knock(w, p, 1.2);
                }
                DungeonStrikes.ring(sl, theme, w.getX(), w.getY() + 0.1, w.getZ(), 4.5, 36, false);
                DungeonStrikes.flash(sl, theme, w.getX(), w.getY() + 0.2, w.getZ());
                DungeonStrikes.cue(sl, w, cues, "slam", SoundEvents.ANVIL_LAND, 1.4f, 0.6f);
                MobVoice.play(w, w.soundFamily(), Cue.ATTACK);
            }
            case DungeonReworkRules.W_LASH -> {
                boolean slowed = false;
                for (UUID id : marked) {
                    Player p = sl.getPlayerByUUID(id);
                    if (p == null || !p.isAlive() || p.distanceToSqr(w) > 30.0 * 30.0) continue;
                    DungeonStrikes.strike(w, p, dmg(5.0), false);
                    Vec3 pull = w.position().subtract(p.position());
                    Vec3 flat = new Vec3(pull.x, 0, pull.z);
                    if (flat.lengthSqr() > 9.0) {
                        flat = flat.normalize().scale(0.9);
                        p.setDeltaMovement(flat.x, 0.25, flat.z);
                        p.hurtMarked = true;
                    }
                    if (!slowed) slowed = DungeonStrikes.slow(w, p, 20, 1);
                    DungeonStrikes.line(sl, theme, w.position().add(0, w.getBbHeight() * 0.7, 0), p.position().add(0, 1.0, 0), false, 6);
                }
                DungeonStrikes.cue(sl, w, cues, "chain", SoundEvents.CHAIN_PLACE, 1.6f, 0.9f);
            }
            case DungeonReworkRules.W_STEP -> {
                if (stepTo == null) return;
                DungeonStrikes.burst(sl, theme, w.getX(), w.getY() + 1.0, w.getZ(), 20, 0.5, true);
                w.teleportTo(stepTo.x, stepTo.y, stepTo.z);
                w.getNavigation().stop();
                LivingEntity target = w.getTarget();
                if (target != null) w.lookAt(target, 180f, 180f);
                for (Player p : DungeonStrikes.victims(sl, w, 3.2)) {
                    if (DungeonStrikes.strike(w, p, dmg(6.0), false)) DungeonStrikes.knock(w, p, 0.9);
                }
                DungeonStrikes.ring(sl, theme, stepTo.x, stepTo.y + 0.1, stepTo.z, 3.2, 24, true);
                DungeonStrikes.flash(sl, theme, stepTo.x, stepTo.y + 0.5, stepTo.z);
                DungeonStrikes.cue(sl, w, cues, "step", SoundEvents.ENDERMAN_TELEPORT, 1.2f, 0.6f);
            }
            case DungeonReworkRules.W_TOLL -> {
                if (sigils == null) return;
                for (Player p : DungeonStrikes.victims(sl, w, 30.0)) {
                    boolean inside = DungeonReworkRules.insideSigil(sigils, p.getX(), p.getZ());
                    DungeonStrikes.strike(w, p, dmg(22.0) * DungeonReworkRules.tollShare(inside), true);
                }
                DungeonStrikes.ring(sl, theme, w.getX(), w.getY() + 0.1, w.getZ(), 22.0, 48, true);
                DungeonStrikes.ring(sl, theme, w.getX(), w.getY() + 0.1, w.getZ(), 12.0, 40, false);
                DungeonStrikes.flash(sl, theme, w.getX(), w.getY() + 1.0, w.getZ());
                DungeonStrikes.cue(sl, w, cues, "blast", SoundEvents.WARDEN_SONIC_BOOM, 1.6f, 0.7f);
            }
            default -> { }                                                                                  // the Seal Pulse resolves over the next 30 ticks
        }
    }

    /** Three rings, ten ticks apart, each growing two blocks a tick to 16: a player on the floor in the band is hit once per ring; a jump clears it. */
    private void pulse(FloorWardenEntity w, ServerLevel sl, int k) {
        List<Player> near = null;
        for (int j = 0; j < 3; j++) {
            int age = k - 10 * j;
            if (age < 0 || age >= 8) continue;
            double r = (age + 1) * 2.0;
            if (age == 0) DungeonStrikes.cue(sl, w, cues, "slam", SoundEvents.ANVIL_LAND, 1.0f, 0.8f);
            if (age % 2 == 0) DungeonStrikes.ring(sl, theme, w.getX(), w.getY() + 0.1, w.getZ(), r, 28, j == 2);
            if (near == null) near = DungeonStrikes.victims(sl, w, 17.0);
            for (Player p : near) {
                double dx = p.getX() - w.getX(), dz = p.getZ() - w.getZ();
                int mask = waveHits.getOrDefault(p.getUUID(), 0);
                if ((mask & (1 << j)) != 0 || !DungeonReworkRules.inWave(Math.sqrt(dx * dx + dz * dz), r, p.getY(), w.getY())) continue;
                waveHits.put(p.getUUID(), mask | (1 << j));
                if (DungeonStrikes.strike(w, p, dmg(7.0), true)) DungeonStrikes.knock(w, p, 0.6);
            }
        }
    }

    /** A free spot beside the target (behind it first, then the sides, then in front), or null: the old Shadow Step's search. */
    private static Vec3 findSpot(ServerLevel sl, FloorWardenEntity w, LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 flat = new Vec3(look.x, 0, look.z);
        if (flat.length() < 1e-3) flat = new Vec3(1, 0, 0);
        flat = flat.normalize().scale(2.2);
        Vec3[] spots = {target.position().subtract(flat), target.position().add(flat.z, 0, -flat.x), target.position().add(-flat.z, 0, flat.x), target.position().add(flat)};
        for (Vec3 c : spots) {
            BlockPos feet = BlockPos.containing(c);
            if (sl.getBlockState(feet.below()).getCollisionShape(sl, feet.below()).isEmpty()) continue;
            if (!sl.noCollision(w, w.getBoundingBox().move(c.subtract(w.position())))) continue;
            return c;
        }
        return null;
    }
}
