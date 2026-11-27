package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.balance.DamageScaleConfig;
import com.newuniverse.nusmp.NUGameRules;
import com.newuniverse.nusmp.antimagic.Nullification;
import com.newuniverse.nusmp.book.EnergyBridge;
import com.newuniverse.nusmp.entity.veldanava.skills.SolomonHooks;
import com.newuniverse.nusmp.entity.zogratis.DamageMath;
import com.newuniverse.nusmp.mana.ManaLaw;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the brain of Goku Omega (docs/goku_omega.md), one small object per boss that holds everything the Omega mode adds on top of the Goku of before: his level, the hacks and their clocks, the
 * Great Sage adaptation and the experience of the fight. {@link GokuBoss} calls into it at a handful of places (each guarded by {@link GokuBossConfig#omega()}), so with the setting off none of
 * this runs and he is exactly the old Goku.
 * <p><b>Hacks</b> ({@link GokuOmegaMath.Hack}, each unlocked by level): Ki Sense (never loses a player), Counter (a strike after every dodge), Energy Drain (magicules and aura of a player, never hunger),
 * Mirror (turns the first projectile of each form back), adaptation (resistance to a damage kind he keeps being hit with), Skill Seal (one skill of a player on cooldown), Time Skip (blinks behind
 * whoever burst him), Second Wind (a small capped heal at a form change) and the Ki Barrier (turns away one huge hit). <b>Bounds:</b> every one has a cooldown, a cap per fight and, when aimed at a player,
 * a cap per player (per UUID, at most {@link #MAX_PLAYER_ROWS} rows, forgotten on logout, death, dimension change, the end of the fight and the removal of the boss). No entity is spawned, no world scan
 * is made (the players come from his own fighter list, at most {@link #SENSE_LOOKUPS} lookups a second), nothing runs per tick but a few comparisons. Packets: the existing budgeted particles and one
 * action-bar line per ability use to the one player hit.
 */
final class GokuOmega {
    /** At most this many players have a row of per-player counters; the rows are cleared with the fight. */
    static final int MAX_PLAYER_ROWS = 32;
    /** The fighter lookups of Ki Sense per use. */
    static final int SENSE_LOOKUPS = 16;
    static final String[] KIND_NAMES = {"melee", "projectile", "magic", "fire", "explosion", "lightning", "other"};

    private final GokuBoss g;
    private int level = GokuOmegaMath.START_LEVEL;
    private final GokuOmegaMath.Adapt adapt = new GokuOmegaMath.Adapt();
    private final long[] readyAt = new long[GokuOmegaMath.Hack.values().length];
    private final int[] uses = new int[GokuOmegaMath.Hack.values().length];
    private final Map<UUID, int[]> perPlayer = new HashMap<>();
    private final boolean[] adaptSaid = new boolean[GokuOmegaMath.KINDS];
    /** 5.1.1 (mahoragaAdapt): hits taken and the stage already announced, per damage kind. */
    private final int[] mHits = new int[GokuOmegaMath.KINDS], mStageSaid = new int[GokuOmegaMath.KINDS];
    private int kills, peakFighters, mirroredForm = -1;
    private float healed, burst;
    private long burstStart;
    private boolean fightOpen;

    GokuOmega(GokuBoss g) { this.g = g; }

    boolean on() { return GokuBossConfig.omega(); }

    int level() { return level; }

    /** The top step of his ladder: Mastered Ultra Instinct with Omega on, True Ultra Instinct with it off. */
    int ladderTop() { return on() ? GokuLadder.last() : GokuLadder.CLASSIC_LAST; }

    private boolean has(GokuOmegaMath.Hack h) { return on() && GokuBossConfig.omegaHacks() && h.unlocked(level); }

    private boolean hasSkill(GokuOmegaMath.Hack h) { return on() && GokuBossConfig.omegaEpSkills() && h.unlocked(level); }

    // ================================================================ level and scaling
    /** Reads the level from the world's record (a hash lookup; called at spawn, at a fight's start and end and every 10 s while he waits). */
    void refreshLevel() {
        if (!(g.level() instanceof ServerLevel sl)) return;
        level = GokuWorld.get(sl.getServer()).level();
    }

    /** His EP reading raised by his level (+10% per level), so the existing curve sees a stronger Goku. */
    GokuBossMath.Reading boost(GokuBossMath.Reading r) {
        return on() ? new GokuBossMath.Reading(r.ep() * GokuOmegaMath.levelEpMul(level), r.source()) : r;
    }

    private static volatile double[] floors;

    /** The stat lines of every other boss of the roster (read once; the roster is complete after boot). */
    static List<GokuOmegaMath.Sheet> rivals() {
        List<GokuOmegaMath.Sheet> out = new ArrayList<>();
        for (ArcCharacter c : ArcRoster.bosses()) if (!GokuBoss.ID.equals(c.id())) out.add(new GokuOmegaMath.Sheet(c.id(), c.health(), c.attack(), c.speed(), c.armor()));
        return out;
    }

    static GokuOmegaMath.Sheet sheet() {
        ArcCharacter c = GokuBoss.CHARACTER;
        return new GokuOmegaMath.Sheet(c.id(), c.health(), c.attack(), c.speed(), c.armor());
    }

    /** {health floor, damage floor}: the factors that keep him above the strongest rival (computed once, then cached). */
    static double[] floors() {
        double[] f = floors;
        if (f == null) {
            List<GokuOmegaMath.Sheet> r = rivals();
            f = new double[]{GokuOmegaMath.healthFloor(sheet(), r), GokuOmegaMath.damageFloor(sheet(), r)};
            if (!r.isEmpty()) floors = f;                         // an empty roster (a test before boot) is not cached
        }
        return f;
    }

    double health(double classic) {
        return on() ? GokuOmegaMath.omegaHealth(classic, GokuBossConfig.get(GokuBossConfig.OMEGA_HEALTH), level, floors()[0]) : classic;
    }

    double damage(double classic) {
        return on() ? GokuOmegaMath.omegaDamage(classic, GokuBossConfig.get(GokuBossConfig.OMEGA_DAMAGE), level, floors()[1]) : classic;
    }

    /**
     * The damage of one blow on a player in Omega mode: the same chain as the balance law (his base, the power multiplier, the victim's EP, the spell-damage rule, the mana law) but WITHOUT the law's own
     * pvp cap, which would stop every hit at 40% whatever this boss's cap says; the caller applies the Omega cap ({@code omega.maxHitPercent}, at most 95%) afterwards, and the damage-scale ceiling of the server is kept.
     */
    float hit(LivingEntity victim, float base) {
        double m = Math.max(0.0, Math.min(1.0, g.power() / 100.0));
        double raw = DamageMath.raw(base, g.tuning().damageMultiplier(), g.damageScalePublic() * g.partyDamage(), Nullification.maxEP(victim));
        double r = raw * BalanceLaw.epScale(g) * (0.85 + 0.3 * m) * NUGameRules.spellDamage(g.level()) * ManaLaw.powerMult(g);
        return (float) Math.min(r, DamageScaleConfig.softCeiling());
    }

    // ================================================================ the fight
    void fightStart() {
        resetFight();
        fightOpen = true;
        if (!on() || !(g.level() instanceof ServerLevel sl)) return;
        GokuWorld w = GokuWorld.get(sl.getServer());
        w.settle(sl.getServer(), false);                      // the training since the last credit, by arithmetic
        level = w.level();
        if (level > w.progress.seenLevel) {
            g.say("I've been training while you were gone. Level " + level + " now!");
            g.announce("Goku has been training... he is level " + level + ".");
        }
        w.progress.seenLevel = level;
        w.setDirty();
    }

    /** The fight is over: {@code won} = the players beat him (the farewell too). Credits the experience once. */
    void fightEnd(boolean won) {
        if (fightOpen && on() && g.level() instanceof ServerLevel sl) {
            GokuOmegaMath.Outcome o = won ? GokuOmegaMath.Outcome.FAREWELL : kills > 0 ? GokuOmegaMath.Outcome.WIN : GokuOmegaMath.Outcome.SURVIVED;
            GokuWorld w = GokuWorld.get(sl.getServer());
            w.progress.fight(o, kills, Math.max(1, peakFighters), GokuWorld.now(sl.getServer()), GokuBossConfig.get(GokuBossConfig.OMEGA_FIGHT_DAILY));
            level = w.level();
            w.progress.seenLevel = level;
            w.setDirty();
        }
        fightOpen = false;
        resetFight();
    }

    private void resetFight() {
        adapt.clear();
        java.util.Arrays.fill(mHits, 0);
        java.util.Arrays.fill(mStageSaid, 0);
        java.util.Arrays.fill(readyAt, 0L);
        java.util.Arrays.fill(uses, 0);
        java.util.Arrays.fill(adaptSaid, false);
        perPlayer.clear();
        kills = 0;
        peakFighters = 0;
        mirroredForm = -1;
        healed = 0f;
        burst = 0f;
        burstStart = 0L;
    }

    /** A player of this fight died to him. */
    void noteKill() {
        if (fightOpen && kills < 64) kills++;
    }

    /** A player left (logout, death, dimension change): his counters go. */
    void forgetPlayer(UUID id) { perPlayer.remove(id); }

    /** The boss is removed: nothing is kept. */
    void clear() {
        fightOpen = false;
        resetFight();
    }

    /** Every tick a few comparisons; once a second the fighter count and Ki Sense; every 10 s the decay of what he learned. */
    void tick(ServerLevel sl) {
        if (!on()) return;
        long t = g.tickCount;
        if (!g.fightActive()) {
            if (t % 200 == 0) refreshLevel();
            return;
        }
        if (t % 20 == 0) {
            peakFighters = Math.max(peakFighters, Math.min(GokuOmegaMath.MAX_FIGHTERS, g.fighters().size()));
            if (has(GokuOmegaMath.Hack.KI_SENSE)) kiSense(sl);
        }
        if (t % 200 == 0 && !GokuBossConfig.mahoragaAdapt()) adapt.decay();   // Mahoraga does not forget inside a fight
    }

    // ================================================================ ready / use
    private boolean ready(GokuOmegaMath.Hack h) {
        int i = h.ordinal();
        return g.tickCount >= readyAt[i] && (h.fightCap <= 0 || uses[i] < h.fightCap);
    }

    private void use(GokuOmegaMath.Hack h) {
        int i = h.ordinal();
        uses[i]++;
        readyAt[i] = g.tickCount + Math.max(0, h.cooldown);
    }

    /** The per-player counters of a player, or null when the table is full (then nothing is aimed at him). */
    private int[] row(UUID id) {
        int[] r = perPlayer.get(id);
        if (r == null) {
            if (perPlayer.size() >= MAX_PLAYER_ROWS) return null;
            r = new int[GokuOmegaMath.Hack.values().length];
            perPlayer.put(id, r);
        }
        return r;
    }

    // ================================================================ Ki Sense
    /** If his target is gone or far, he turns to the nearest player of the fight (from his own fighter list, at most {@link #SENSE_LOOKUPS} lookups). */
    private void kiSense(ServerLevel sl) {
        LivingEntity t = g.getTarget();
        if (t != null && t.isAlive() && g.distanceToSqr(t) <= 40.0 * 40.0) return;
        ServerPlayer best = null;
        double bd = 64.0 * 64.0;
        int n = 0;
        for (UUID id : g.fighters()) {
            if (++n > SENSE_LOOKUPS) break;
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p == null || p.level() != sl || !p.isAlive() || p.isCreative() || p.isSpectator()) continue;
            double d = g.distanceToSqr(p);
            if (d < bd) { bd = d; best = p; }
        }
        if (best != null) g.setTarget(best);
    }

    // ================================================================ EP skills on a player
    /** Called after a blow of his landed: an Energy Drain, else a Skill Seal, whichever is ready (never both on one blow). */
    void onStrike(LivingEntity victim) {
        if (!(victim instanceof Player) || !victim.isAlive() || !g.fightActive()) return;
        if (com.newuniverse.nusmp.boss.rules.BossRulesConfig.noPoolTheft()) return;          // 4.98.0: no boss drains or seals a player (bossRules.noPoolTheft)
        int[] row = row(victim.getUUID());
        if (row == null) return;
        if (hasSkill(GokuOmegaMath.Hack.ENERGY_DRAIN) && ready(GokuOmegaMath.Hack.ENERGY_DRAIN) && row[GokuOmegaMath.Hack.ENERGY_DRAIN.ordinal()] < GokuOmegaMath.Hack.ENERGY_DRAIN.perPlayer) {
            use(GokuOmegaMath.Hack.ENERGY_DRAIN);
            row[GokuOmegaMath.Hack.ENERGY_DRAIN.ordinal()]++;
            drain(victim);
        } else if (hasSkill(GokuOmegaMath.Hack.SKILL_SEAL) && ready(GokuOmegaMath.Hack.SKILL_SEAL) && row[GokuOmegaMath.Hack.SKILL_SEAL.ordinal()] < GokuOmegaMath.Hack.SKILL_SEAL.perPlayer) {
            use(GokuOmegaMath.Hack.SKILL_SEAL);
            row[GokuOmegaMath.Hack.SKILL_SEAL.ordinal()]++;
            seal(victim);
        }
    }

    /** Takes a share of the player's magicule and aura (they regenerate; never hunger, never max EP, never a skill). Returns the share of magicule actually taken (0 when immune or none). */
    double drain(LivingEntity victim) {
        double f = GokuOmegaMath.drainFraction(level);
        double mp = 0.0, au = 0.0;
        try { mp = EnergyBridge.drain(victim, null, f); } catch (RuntimeException | LinkageError ignored) { /* no Tensura data */ }
        try {
            if (!SolomonHooks.antiMagicImmune(victim)) {
                var ex = TensuraStorages.getExistenceFrom(victim);
                if (ex != null) {
                    au = Math.min(ex.getAura(), EnergyHelper.getMaxAura(victim) * f);
                    if (au > 0.0) {
                        ex.setAura(ex.getAura() - au);
                        ex.markDirty();
                    }
                }
            }
        } catch (RuntimeException | LinkageError ignored) { /* no Tensura data */ }
        if (g.level() instanceof ServerLevel sl) {
            GokuFx.beam(sl, g.getEyePosition(), victim.position().add(0, victim.getBbHeight() * 0.5, 0), 0x7FD0FF);
            sl.playSound(null, victim.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 1.2f, 0.6f);
        }
        if (victim instanceof ServerPlayer p && (mp > 0.0 || au > 0.0)) p.displayClientMessage(Component.literal("Goku's ki overwhelms yours: " + Math.round(f * 100) + "% of your magicules and aura drained."), true);
        return mp;
    }

    /** Puts one random active skill of the player on cooldown for {@link GokuOmegaMath#sealSeconds} (Tensura's own skill cooldown, the one the anti-magic aura uses; nothing is removed or forgotten). With no skill to seal, a short silence. */
    boolean seal(LivingEntity victim) {
        int seconds = GokuOmegaMath.sealSeconds(level);
        String name = sealOne(victim, seconds);
        if (name == null) {
            try { EnergyBridge.effect(victim, "silence", seconds * 20, 0); } catch (RuntimeException | LinkageError ignored) { /* no such effect */ }
            name = "magic";
        }
        if (g.level() instanceof ServerLevel sl) {
            GokuFx.burst(sl, victim.position().add(0, victim.getBbHeight() * 0.6, 0), 0x9FD8FF, 8);
            sl.playSound(null, victim.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 0.8f, 1.9f);
        }
        if (victim instanceof ServerPlayer p) p.displayClientMessage(Component.literal("Goku sealed your " + name + " for " + seconds + " seconds."), true);
        return true;
    }

    /** The name of the skill sealed, or null when the player has none to seal. */
    private String sealOne(LivingEntity t, int seconds) {
        Skills skills;
        try { skills = SkillAPI.getSkillsFrom(t); } catch (RuntimeException | LinkageError noSkills) { return null; }
        if (skills == null) return null;
        List<ManasSkillInstance> pool = new ArrayList<>();
        for (ManasSkillInstance i : skills.getLearnedSkills()) {
            if (!(i.getSkill() instanceof Skill s)) continue;
            Skill.SkillType type = s.getType();
            if (type == Skill.SkillType.RESISTANCE || type == Skill.SkillType.INTRINSIC) continue;
            pool.add(i);
        }
        if (pool.isEmpty()) return null;
        ManasSkillInstance i = pool.get(g.getRandom().nextInt(pool.size()));
        int modes = Math.max(1, i.getModes());
        for (int m = 0; m < modes; m++) if (i.getCoolDown(m) < seconds) i.setCoolDown(seconds, m);
        if (i.isToggled()) {
            i.setToggled(false);
            try { i.onToggleOff(t); } catch (RuntimeException ignored) { /* a skill that cannot switch off */ }
        }
        i.markDirty();
        skills.markDirty();
        var id = i.getSkillId();
        return id == null ? "skill" : id.getPath().replace('_', ' ');
    }

    // ================================================================ taking a hit
    /** The colour of the ring that shows which damage kind he adapted to (melee, projectile, magic, fire, explosion, lightning, other). */
    static final int[] KIND_RGB = {0xFF6A5A, 0xFFD27A, 0xB58CFF, 0xFF8A2A, 0xFFE08A, 0xFFF27A, 0xC0C0C0};

    /** A new adaptation stage: a ring in the damage kind's colour (bigger per stage) and a sound that rises per stage; the old chat line only when {@code adaptCue} is off. */
    private void adaptCue(int kind, int stage, String chat) {
        if (!GokuBossConfig.adaptCue()) { g.announce(chat); return; }
        if (!(g.level() instanceof ServerLevel sl)) return;
        GokuFx.ring(sl, g.position().add(0, 0.1, 0), 1.8 + 0.35 * Math.max(1, stage), KIND_RGB[Math.floorMod(kind, KIND_RGB.length)], 16);
        sl.playSound(null, g.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 1.6f, 0.7f + 0.15f * Math.max(1, stage));
    }

    /** The kind of a damage source, for the adaptation. */
    static int kind(DamageSource s) {
        if (s.is(DamageTypeTags.IS_FIRE)) return GokuOmegaMath.FIRE;
        if (s.is(DamageTypeTags.IS_EXPLOSION)) return GokuOmegaMath.EXPLOSION;
        if (s.is(DamageTypeTags.IS_LIGHTNING)) return GokuOmegaMath.LIGHTNING;
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return GokuOmegaMath.PROJECTILE;
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC) || s.is(DamageTypes.WITHER) || s.is(DamageTypes.DRAGON_BREATH)) return GokuOmegaMath.MAGIC;
        if (s.getEntity() != null && s.getDirectEntity() == s.getEntity()) return GokuOmegaMath.MELEE;
        return GokuOmegaMath.OTHER;
    }

    /** True when the hit is turned away: the Mirror (the first projectile of each form goes back at the shooter) or the Ki Barrier (one huge hit per cooldown). */
    boolean onIncoming(DamageSource src, float amount) {
        if (!g.fightActive() || !(g.level() instanceof ServerLevel sl)) return false;
        Entity a = src.getEntity();
        if (has(GokuOmegaMath.Hack.MIRROR) && mirroredForm != g.formIdx() && a instanceof LivingEntity shooter && g.isFoe(shooter) && src.is(DamageTypeTags.IS_PROJECTILE)) {
            mirroredForm = g.formIdx();
            uses[GokuOmegaMath.Hack.MIRROR.ordinal()]++;
            GokuFx.burst(sl, g.position().add(0, g.getBbHeight() * 0.6, 0), 0xBFE4FF, 8);
            sl.playSound(null, g.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 1.4f, 1.6f);
            g.strike(shooter, g.base() * 0.7f, true);
            if (shooter instanceof ServerPlayer p) p.displayClientMessage(Component.literal("Goku turned your shot back."), true);
            return true;
        }
        if (has(GokuOmegaMath.Hack.KI_BARRIER) && amount >= g.getMaxHealth() * GokuOmegaMath.BARRIER_SHARE && ready(GokuOmegaMath.Hack.KI_BARRIER)) {
            use(GokuOmegaMath.Hack.KI_BARRIER);
            GokuFx.ring(sl, g.position(), 3.0, 0xFFFFFF, 16);
            sl.playSound(null, g.blockPosition(), SoundEvents.BEACON_DEACTIVATE, SoundSource.HOSTILE, 2f, 1.5f);
            for (LivingEntity f : g.foes(6.0)) {            // the barrier's shock shoves, never hurts
                Vec3 away = f.position().subtract(g.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) f.knockback(0.6, -away.x, -away.z);
            }
            if (a instanceof ServerPlayer p) p.displayClientMessage(Component.literal("A Ki Barrier took your blow."), true);
            return true;
        }
        return false;
    }

    /** The multiplier of a hit he took: 1 - what he has learned of its kind (Great Sage), 1 when the hack is not unlocked. */
    float resistMul(DamageSource src) {
        if (!has(GokuOmegaMath.Hack.ADAPTATION)) return 1.0f;
        if (GokuBossConfig.mahoragaAdapt()) return (float) GokuAdaptMath.multiplier(mHits[kind(src)], GokuBossConfig.ADAPT_BASE.get(), GokuBossConfig.ADAPT_HITS_PER.get(), adaptCapPct());
        return (float) (1.0 - adapt.resist(kind(src), GokuOmegaMath.adaptCap(level)));
    }

    /** A hit landed: it teaches him (once per kind he says so), and a burst of damage makes him skip through time behind whoever did it. */
    void onHurt(DamageSource src, float dealt) {
        if (!g.fightActive() || !(dealt > 0f)) return;
        if (has(GokuOmegaMath.Hack.ADAPTATION) && GokuBossConfig.mahoragaAdapt()) {
            int k = kind(src);
            mHits[k] = Math.min(GokuAdaptMath.MAX_HITS, mHits[k] + 1);
            int st = GokuAdaptMath.stage(mHits[k]);
            if (st > mStageSaid[k]) {
                mStageSaid[k] = st;
                adaptCue(k, st, "Goku adapts to " + KIND_NAMES[k] + " damage (stage " + st + ").");
            }
        } else if (has(GokuOmegaMath.Hack.ADAPTATION)) {
            int k = kind(src);
            adapt.hit(k);
            if (!adaptSaid[k] && adapt.resist(k, GokuOmegaMath.adaptCap(level)) > 0.0) {
                adaptSaid[k] = true;
                adaptCue(k, 1, "Goku is adapting to " + KIND_NAMES[k] + " damage.");
            }
        }
        long now = g.tickCount;
        if (now - burstStart > GokuOmegaMath.BURST_TICKS) {
            burstStart = now;
            burst = 0f;
        }
        burst += dealt;
        if (burst >= g.getMaxHealth() * GokuOmegaMath.BURST_SHARE && has(GokuOmegaMath.Hack.TIME_SKIP) && ready(GokuOmegaMath.Hack.TIME_SKIP)
                && src.getEntity() instanceof LivingEntity by && g.isFoe(by)) {
            use(GokuOmegaMath.Hack.TIME_SKIP);
            burst = 0f;
            g.later(1, () -> { if (by.isAlive()) g.transmit(by); });
        }
    }

    /** He dodged: a counter at the one who swung (one per dodge, at most {@link GokuOmegaMath.Hack#COUNTER} fightCap per fight). */
    void onDodged(DamageSource src) {
        if (!has(GokuOmegaMath.Hack.COUNTER) || !ready(GokuOmegaMath.Hack.COUNTER)) return;
        if (!(src.getEntity() instanceof LivingEntity by) || !g.isFoe(by)) return;
        use(GokuOmegaMath.Hack.COUNTER);
        g.later(5, () -> {
            if (by.isAlive() && g.distanceTo(by) < 8.0) {
                g.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                g.strike(by, g.base() * 0.9f, false);
            }
        });
    }

    /** A form took hold: Second Wind (a small heal, capped per fight) and a fresh Mirror. */
    void onFormChanged() {
        mirroredForm = -1;
        if (!on() || !GokuBossConfig.omegaSecondWind() || !GokuOmegaMath.Hack.SECOND_WIND.unlocked(level) || !ready(GokuOmegaMath.Hack.SECOND_WIND)) return;
        float max = g.getMaxHealth();
        float room = (float) (max * GokuOmegaMath.WIND_FIGHT_CAP) - healed;
        if (room <= 0f) return;
        float amt = Math.min(room, (float) (max * GokuOmegaMath.WIND_SHARE));
        use(GokuOmegaMath.Hack.SECOND_WIND);
        healed += amt;
        g.healQuiet(amt);
    }

    // ================================================================ test seams
    int usesForTest(GokuOmegaMath.Hack h) { return uses[h.ordinal()]; }

    void setLevelForTest(int l) { level = Math.max(1, l); }

    void fightOpenForTest() {
        resetFight();
        fightOpen = true;
    }

    float healedForTest() { return healed; }

    double adaptHitsForTest(int kind) { return GokuBossConfig.mahoragaAdapt() ? mHits[kind] : adapt.hits(kind); }

    /** The reduction limit in percent: the mastered one in Mastered Ultra Instinct. */
    private double adaptCapPct() {
        return GokuLadder.MASTERED.equals(GokuLadder.step(g.formIdx()).id()) ? GokuBossConfig.ADAPT_MAX_MASTERED.get() : GokuBossConfig.ADAPT_MAX.get();
    }
}
