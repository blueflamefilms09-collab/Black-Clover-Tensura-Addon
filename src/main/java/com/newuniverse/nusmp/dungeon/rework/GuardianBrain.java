package com.newuniverse.nusmp.dungeon.rework;

import com.newuniverse.nusmp.audio.MobVoice;
import com.newuniverse.nusmp.dungeon.rework.DungeonReworkRules.Move;
import com.newuniverse.nusmp.dungeon.rework.GuardianRules.Flavour;
import com.newuniverse.nusmp.dungeon.rework.GuardianRules.Kit;
import com.newuniverse.nusmp.dungeon.rework.GuardianRules.Verdict;
import com.newuniverse.nusmp.dungeon.rework2.DungeonBossSoundRules;
import com.newuniverse.nusmp.particle.fx.ThemeKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the Labyrinth Guardians (and the Decennial bosses that ride on the same entity) fought with the Floor Warden's old two moves and no telegraph. This is their move set (docs/dungeon_bosses_rework2.md):
 * a Slam (a ring on the floor, step out), a Lance (a line of motes locks onto you, step aside), a Rain of marks (up to three discs follow three players and lock one second before they land), a Wave
 * (phase 2: three rings roll out, jump them) and the Verdict (phase 3 at a quarter of its health: stand in a lit sigil, or get out of the ring, three and a half seconds later the whole hall is hit). What
 * each move adds besides damage, the colours, the sounds and the Verdict's kind come from the boss's {@link Kit} (one per sector trait), so a frost Guardian chills and shimmers, a flame Guardian burns
 * and crackles, a gravity Guardian pulls and slams. The numbers are {@link GuardianRules}; this class is the Minecraft-facing half.
 * <p>
 * SERVER ONLY, ONE PER ENTITY, NO STATIC STATE: cooldowns, the active move, the marked players (as UUIDs) and the mark positions are fields of the brain, so everything goes with the entity (death, despawn,
 * dimension change; a player who logs out only makes the Rain keep the last position of his mark). Damage goes through {@link DungeonStrikes#strike} (the boss damage law and the player's hit cap), a
 * slow through the control law ({@link DungeonStrikes#slow}: one control per caster per 12 s), the other effects are the short ones of {@link DungeonStrikes#effect} (at most 6 s). NO ENTITY IS SPAWNED here
 * (the Guardian's own adds and the Decennial layer's are theirs and keep their own caps). Cost per tick: one boolean when it has no target; during a move at most one bounded player query (8 players)
 * per tick and a few facade effects every 3 to 5 ticks.
 */
public final class GuardianBrain {
    /** What the brain needs from its Guardian: the bar colour at a phase change and the cast clip (1 a move, 2 a phase change) the clients play. */
    public interface Host {
        void phaseChanged(int phase);

        void casting(int kind);
    }

    private final Kit kit;
    private final ThemeKey theme;
    private final long[] readyAt = new long[GuardianRules.BOOK.size()];
    private final DungeonBossSoundRules.Limiter cues = new DungeonBossSoundRules.Limiter();
    private final List<UUID> marked = new ArrayList<>(GuardianRules.MAX_MARKS);
    private final Vec3[] marks = new Vec3[GuardianRules.MAX_MARKS];
    private final Map<UUID, Integer> waveHits = new HashMap<>();
    private int move = -1, t, phase = 1;
    private long gapUntil;
    private double lanceDx = 1, lanceDz;
    private double[] sigils;
    private boolean slowed;
    private double size = 1.0, attack = 1.0;
    private int tier;

    public GuardianBrain(Kit kit) {
        this.kit = kit;
        this.theme = ThemeKey.of(kit.theme());
    }

    public Kit kit() { return kit; }

    /** Is a move winding up or landing (the melee goal then stands still)? */
    public boolean busy() { return move >= 0; }

    /** The move being wound up or resolved (an index of {@link GuardianRules#BOOK}), or -1. */
    public int activeMove() { return move; }

    public int phase() { return phase; }

    /** Forgets the move in progress (death, a shift, a wipe). */
    public void reset() {
        move = -1;
        t = 0;
        marked.clear();
        waveHits.clear();
        java.util.Arrays.fill(marks, null);
        sigils = null;
    }

    // ---------------------------------------------------------------- the tick
    /**
     * @param attack the Guardian's attack damage right now (the scaled attribute, with the Decennial phase's factor)
     * @param scale  its SCALE attribute (radii grow with it)
     * @param tier   the Decennial tier 1 to 5, or 0 for a sector Guardian
     * @param paused true while it must not act (a Decennial shift, a hall not built yet): the move in progress is dropped
     */
    public void tick(Mob boss, ServerLevel sl, Host host, double attack, double scale, int tier, boolean paused) {
        if (!boss.isAlive()) return;
        this.attack = attack;
        this.size = GuardianRules.sizeFactor(scale);
        this.tier = tier;
        long now = sl.getGameTime();
        int p = DungeonReworkRules.phase(boss.getMaxHealth() <= 0f ? 1.0 : boss.getHealth() / (double) boss.getMaxHealth());
        if (p > phase) {
            phase = p;
            onPhase(boss, sl, host);
        }
        if (paused) {
            if (move >= 0) reset();
            return;
        }
        LivingEntity target = boss.getTarget();
        boolean fighting = target != null && target.isAlive();
        if (fighting && boss.tickCount % 20 == 0) DungeonStrikes.burst(sl, theme, boss.getX(), boss.getY() + boss.getBbHeight() * 0.55, boss.getZ(), phase >= 3 ? 5 : phase == 2 ? 3 : 2, 0.6 * size, kit.dark());
        if (move >= 0) {
            advance(boss, sl, host, target, now);
            return;
        }
        if (!fighting || now < gapUntil) return;
        int i = DungeonReworkRules.choose(GuardianRules.BOOK, Math.sqrt(boss.distanceToSqr(target)), phase, now, readyAt, boss.getRandom().nextInt());
        if (i >= 0) begin(boss, sl, host, i, target, now);
    }

    // ---------------------------------------------------------------- phases and warnings
    private void onPhase(Mob boss, ServerLevel sl, Host host) {
        host.phaseChanged(phase);
        host.casting(2);
        String family = MobVoice.familyOf(boss);
        if (family != null) MobVoice.play(boss, family, com.newuniverse.nusmp.audio.Cue.AGGRO);
        DungeonStrikes.ring(sl, theme, boss.getX(), boss.getY() + 0.1, boss.getZ(), 10.0 * size, 36, phase >= 3 || kit.dark());
        DungeonStrikes.burst(sl, theme, boss.getX(), boss.getY() + boss.getBbHeight() * 0.5, boss.getZ(), 30, 1.0 * size, phase >= 3 || kit.dark());
        DungeonStrikes.flash(sl, theme, boss.getX(), boss.getY() + 1.0, boss.getZ());
        if (phase >= 3) DungeonStrikes.cue(sl, boss, cues, "gong", SoundEvents.WITHER_SPAWN, 0.9f, 1.3f);
        else DungeonStrikes.cue(sl, boss, cues, "roar", SoundEvents.RAVAGER_ROAR, 1.5f, 0.8f);
        announce(boss, sl, phase >= 3 ? "p3" : "p2");
    }

    private void announce(Mob boss, ServerLevel sl, String key) {
        Component line = Component.translatable("message.anime_multiverse.dboss.guardian." + key).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.ITALIC);
        for (Player p : DungeonStrikes.victims(sl, boss, 48.0)) p.displayClientMessage(line, true);
    }

    // ---------------------------------------------------------------- a move begins
    private void begin(Mob boss, ServerLevel sl, Host host, int i, LivingEntity target, long now) {
        Move m = GuardianRules.BOOK.get(i);
        marked.clear();
        waveHits.clear();
        java.util.Arrays.fill(marks, null);
        sigils = null;
        slowed = false;
        switch (i) {
            case GuardianRules.G_LANCE -> aim(boss, target);
            case GuardianRules.G_RAIN -> {
                for (Player p : DungeonStrikes.victims(sl, boss, 28.0)) {
                    if (marked.size() >= GuardianRules.MAX_MARKS) break;
                    marks[marked.size()] = p.position();
                    marked.add(p.getUUID());
                }
                if (marked.isEmpty()) {
                    readyAt[i] = now + 20;
                    return;
                }
            }
            case GuardianRules.G_VERDICT -> {
                if (kit.verdict() == Verdict.SAFE_ZONE) sigils = GuardianRules.sigils(boss.getX(), boss.getZ(), boss.getRandom().nextDouble() * Math.PI * 2.0, size);
                announce(boss, sl, "verdict." + kit.id());
            }
            default -> { }
        }
        DungeonStrikes.cue(sl, boss, cues, i == GuardianRules.G_VERDICT ? "charge" : kit.beginCue(), SoundEvents.ANVIL_PLACE, 1.0f, 0.5f);
        host.casting(1);
        move = i;
        t = 0;
        readyAt[i] = now + m.windup() + DungeonReworkRules.cooldown(m.cooldown(), phase, false);
    }

    private void aim(Mob boss, LivingEntity target) {
        double dx = target.getX() - boss.getX(), dz = target.getZ() - boss.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-3) return;
        lanceDx = dx / len;
        lanceDz = dz / len;
    }

    // ---------------------------------------------------------------- wind-up, landing, aftermath
    private static int resolveTicks(int i) { return i == GuardianRules.G_WAVE ? 30 : 1; }

    private void advance(Mob boss, ServerLevel sl, Host host, LivingEntity target, long now) {
        Move m = GuardianRules.BOOK.get(move);
        t++;
        if (t < m.windup()) {
            boss.getNavigation().stop();
            if (target != null && target.isAlive()) {
                boss.getLookControl().setLookAt(target, 40f, 40f);
                if (move == GuardianRules.G_LANCE && t <= m.windup() - 8) aim(boss, target);                    // the lance tracks until its last third, then it is locked
            }
            telegraph(boss, sl);
        } else if (t == m.windup()) {
            land(boss, sl);
        } else if (move == GuardianRules.G_WAVE) {
            pulse(boss, sl, t - m.windup() - 1);
        }
        if (t >= m.windup() + resolveTicks(move)) {
            gapUntil = now + GuardianRules.GAP + (move == GuardianRules.G_VERDICT ? GuardianRules.VERDICT_WINDED : 0);
            reset();
        }
    }

    private void telegraph(Mob boss, ServerLevel sl) {
        double x = boss.getX(), y = boss.getY() + 0.1, z = boss.getZ();
        switch (move) {
            case GuardianRules.G_SLAM -> {
                if (t % 4 == 1) DungeonStrikes.ring(sl, theme, x, y, z, GuardianRules.SLAM_RADIUS * size, 24, kit.dark());
            }
            case GuardianRules.G_LANCE -> {
                if (t % 3 == 1) {
                    Vec3 from = new Vec3(x, boss.getY() + boss.getBbHeight() * 0.45, z);
                    DungeonStrikes.line(sl, theme, from, from.add(lanceDx * GuardianRules.LANCE_LENGTH, 0, lanceDz * GuardianRules.LANCE_LENGTH), kit.dark(), 3);
                }
            }
            case GuardianRules.G_RAIN -> {
                if (t <= GuardianRules.MARK_FOLLOW_TICKS) followMarks(sl);
                if (t % 4 == 1) for (int i = 0; i < marked.size(); i++) if (marks[i] != null) DungeonStrikes.ring(sl, theme, marks[i].x, marks[i].y + 0.1, marks[i].z, GuardianRules.MARK_RADIUS, 14, kit.dark());
            }
            case GuardianRules.G_WAVE -> {
                if (t % 5 == 1) DungeonStrikes.ring(sl, theme, x, y, z, Math.max(2.0, 14.0 * size - t * 0.4), 28, kit.dark());
            }
            case GuardianRules.G_VERDICT -> {
                if (kit.verdict() == Verdict.SAFE_ZONE) {
                    if (t % 5 == 1 && sigils != null) {
                        for (int i = 0; i + 1 < sigils.length; i += 2) {
                            DungeonStrikes.ring(sl, theme, sigils[i], y, sigils[i + 1], GuardianRules.SIGIL_RADIUS, 24, false);
                            DungeonStrikes.burst(sl, theme, sigils[i], y + 1.0, sigils[i + 1], 3, 0.3, false);
                        }
                    }
                    if (t % 10 == 1) DungeonStrikes.ring(sl, theme, x, y, z, 22.0, 40, true);
                    if (t == 40 || t == 60) DungeonStrikes.cue(sl, boss, cues, "toll", SoundEvents.BELL_RESONATE, 2.0f, t == 40 ? 0.6f : 0.75f);
                } else {
                    if (t % 4 == 1) DungeonStrikes.ring(sl, theme, x, y, z, GuardianRules.KEEP_OUT_RADIUS * size, 40, true);
                    if (t % 8 == 1) DungeonStrikes.ring(sl, theme, x, y, z, Math.max(2.0, GuardianRules.KEEP_OUT_RADIUS * size * (1.0 - t / 80.0)), 28, false);
                    if (t == 40) DungeonStrikes.cue(sl, boss, cues, "rise", SoundEvents.WARDEN_SONIC_CHARGE, 1.0f, 0.8f);
                }
            }
            default -> { }
        }
    }

    /** The marks follow their players while the Rain winds up (a player who left keeps his last mark). */
    private void followMarks(ServerLevel sl) {
        for (int i = 0; i < marked.size(); i++) {
            Player p = sl.getPlayerByUUID(marked.get(i));
            if (p != null && p.isAlive()) marks[i] = p.position();
        }
    }

    private void land(Mob boss, ServerLevel sl) {
        double x = boss.getX(), y = boss.getY() + 0.1, z = boss.getZ();
        Flavour f = kit.flavour(move);
        switch (move) {
            case GuardianRules.G_SLAM -> {
                double r = GuardianRules.SLAM_RADIUS * size;
                for (Player p : DungeonStrikes.victims(sl, boss, r)) {
                    if (DungeonStrikes.strike(boss, p, GuardianRules.damage(move, attack, tier), false)) {
                        if (f != Flavour.PULL && f != Flavour.LAUNCH) DungeonStrikes.knock(boss, p, 1.0);
                        flavour(boss, p, f);
                    }
                }
                DungeonStrikes.ring(sl, theme, x, y, z, r, 36, kit.dark());
                DungeonStrikes.flash(sl, theme, x, y + 0.1, z);
                DungeonStrikes.cue(sl, boss, cues, kit.hitCue(), SoundEvents.ANVIL_LAND, 1.4f, 0.6f);
            }
            case GuardianRules.G_LANCE -> {
                Vec3 from = new Vec3(x, boss.getY() + boss.getBbHeight() * 0.45, z);
                for (Player p : DungeonStrikes.victims(sl, boss, GuardianRules.LANCE_LENGTH + 2.0)) {
                    if (!GuardianRules.onLine(x, z, lanceDx, lanceDz, p.getX(), p.getZ(), GuardianRules.LANCE_LENGTH, GuardianRules.LANCE_HALF_WIDTH)) continue;
                    if (DungeonStrikes.strike(boss, p, GuardianRules.damage(move, attack, tier), true)) flavour(boss, p, f);
                }
                DungeonStrikes.line(sl, theme, from, from.add(lanceDx * GuardianRules.LANCE_LENGTH, 0, lanceDz * GuardianRules.LANCE_LENGTH), kit.dark(), 8);
                DungeonStrikes.flash(sl, theme, x + lanceDx * 2.0, y + 1.0, z + lanceDz * 2.0);
                DungeonStrikes.cue(sl, boss, cues, kit.hitCue(), SoundEvents.WARDEN_SONIC_BOOM, 1.2f, 1.2f);
            }
            case GuardianRules.G_RAIN -> {
                List<UUID> struck = new ArrayList<>(GuardianRules.MAX_MARKS);
                for (Player p : DungeonStrikes.victims(sl, boss, 40.0)) {
                    for (int i = 0; i < marked.size(); i++) {
                        if (marks[i] == null || struck.contains(p.getUUID()) || !GuardianRules.inCircle(marks[i].x, marks[i].z, GuardianRules.MARK_RADIUS, p.getX(), p.getZ())) continue;
                        struck.add(p.getUUID());
                        if (DungeonStrikes.strike(boss, p, GuardianRules.damage(move, attack, tier), true)) flavour(boss, p, f);
                    }
                }
                for (int i = 0; i < marked.size(); i++) {
                    if (marks[i] == null) continue;
                    DungeonStrikes.ring(sl, theme, marks[i].x, marks[i].y + 0.1, marks[i].z, GuardianRules.MARK_RADIUS, 20, kit.dark());
                    DungeonStrikes.burst(sl, theme, marks[i].x, marks[i].y + 0.6, marks[i].z, 8, 0.5, kit.dark());
                    DungeonStrikes.flash(sl, theme, marks[i].x, marks[i].y + 0.3, marks[i].z);
                    DungeonStrikes.cueAt(sl, marks[i].x, marks[i].y, marks[i].z, cues, kit.accentCue(), SoundEvents.ANVIL_LAND, 1.0f, 0.8f);
                }
            }
            case GuardianRules.G_VERDICT -> {
                double reach = 30.0 * size;
                for (Player p : DungeonStrikes.victims(sl, boss, reach)) {
                    boolean inside = sigils != null && GuardianRules.insideSigil(sigils, p.getX(), p.getZ());
                    double share = GuardianRules.verdictShare(kit.verdict(), inside, Math.sqrt(p.distanceToSqr(boss)), size);
                    if (share <= 0.0) continue;
                    if (DungeonStrikes.strike(boss, p, GuardianRules.damage(move, attack, tier) * share, true) && kit.verdict() == Verdict.KEEP_OUT) DungeonStrikes.knock(boss, p, 1.2);
                }
                DungeonStrikes.ring(sl, theme, x, y, z, 22.0, 48, true);
                DungeonStrikes.ring(sl, theme, x, y, z, 12.0 * size, 40, false);
                DungeonStrikes.flash(sl, theme, x, y + 1.0, z);
                DungeonStrikes.cue(sl, boss, cues, "blast", SoundEvents.WARDEN_SONIC_BOOM, 1.6f, 0.7f);
            }
            default -> { }                                                                                  // the Wave resolves over the next 30 ticks
        }
    }

    /** Three rings, ten ticks apart, each growing two blocks a tick (times the size) to 16: a player on the floor in the band is hit once per ring; a jump clears it. */
    private void pulse(Mob boss, ServerLevel sl, int k) {
        List<Player> near = null;
        Flavour f = kit.wave();
        for (int j = 0; j < 3; j++) {
            int age = k - 10 * j;
            if (age < 0 || age >= 8) continue;
            double r = GuardianRules.waveRadius(age, size);
            if (age == 0) DungeonStrikes.cue(sl, boss, cues, kit.accentCue(), SoundEvents.ANVIL_LAND, 1.0f, 0.8f);
            if (age % 2 == 0) DungeonStrikes.ring(sl, theme, boss.getX(), boss.getY() + 0.1, boss.getZ(), r, 28, j == 2 || kit.dark());
            if (near == null) near = DungeonStrikes.victims(sl, boss, 17.0 * size);
            for (Player p : near) {
                double dx = p.getX() - boss.getX(), dz = p.getZ() - boss.getZ();
                int mask = waveHits.getOrDefault(p.getUUID(), 0);
                if ((mask & (1 << j)) != 0 || !DungeonReworkRules.inWave(Math.sqrt(dx * dx + dz * dz), r, p.getY(), boss.getY())) continue;
                waveHits.put(p.getUUID(), mask | (1 << j));
                if (DungeonStrikes.strike(boss, p, GuardianRules.damage(move, attack, tier), true)) {
                    DungeonStrikes.knock(boss, p, 0.6);
                    flavour(boss, p, f);
                }
            }
        }
    }

    // ---------------------------------------------------------------- what a move adds to its damage (short, bounded, through the laws)
    private void flavour(Mob boss, Player p, Flavour f) {
        switch (f) {
            case KNOCK -> DungeonStrikes.knock(boss, p, 0.9);
            case PULL -> {
                Vec3 pull = boss.position().subtract(p.position());
                Vec3 flat = new Vec3(pull.x, 0, pull.z);
                if (flat.lengthSqr() > 4.0) {
                    flat = flat.normalize().scale(0.8);
                    p.setDeltaMovement(flat.x, 0.2, flat.z);
                    p.hurtMarked = true;
                }
            }
            case LAUNCH -> {
                p.setDeltaMovement(p.getDeltaMovement().x, 0.7, p.getDeltaMovement().z);
                p.hurtMarked = true;
            }
            case SLOW -> {
                if (!slowed) slowed = DungeonStrikes.slow(boss, p, 20, 1);                                  // one control per caster per 12 s: the law decides, the rest of the move is damage
            }
            case BURN -> p.igniteForSeconds(3);
            case WEAKEN -> DungeonStrikes.effect(p, MobEffects.WEAKNESS, 100, 0);
            case POISON -> DungeonStrikes.effect(p, MobEffects.POISON, 80, 0);
            case WITHER -> DungeonStrikes.effect(p, MobEffects.WITHER, 60, 0);
            default -> { }
        }
    }
}
