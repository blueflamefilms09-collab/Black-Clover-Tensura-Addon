package com.newuniverse.nusmp.entity.zogratis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.boss.arc.CloneScheduler;
import com.newuniverse.nusmp.boss.arc.CoreGemMath;
import com.newuniverse.nusmp.boss.arc.HoverMath;
import com.newuniverse.nusmp.boss.arc.LuciusArc;
import com.newuniverse.nusmp.boss.arc.LuciusChains;
import com.newuniverse.nusmp.boss.arc.PaladinTurn;
import com.newuniverse.nusmp.boss.arc.PrecogMath;
import com.newuniverse.nusmp.boss.arc.TimeReversalMath;
import com.newuniverse.nusmp.boss.bar.BossBarSync;
import com.newuniverse.nusmp.boss.god.LuciusBalance;
import com.newuniverse.nusmp.boss.god.LuciusGodConfig;
import com.newuniverse.nusmp.boss.god.TrueDamage;
import com.newuniverse.nusmp.boss.rules.BossHealGuard;
import com.newuniverse.nusmp.boss.rules.BossRulesConfig;
import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

/**
 * Why: the final-arc Lucius is one idea (a three-act fight of spell chains) that touches ten things: the hover of act 1, the chains, the
 * Soul Touch, the new precognition, the Time Reversal channel, the clones, the gem. Putting it all into {@link LuciusBoss} would add
 * a thousand lines to a class that is already twelve hundred; instead each LuciusBoss hook (the tick, the damage filter, the move
 * chooser, the save data, the end of the fight) is one call into this director, which owns the pure state machines from
 * {@code boss/arc} (acts, chains, precognition history, reversal channel, clone schedule, turned companions, burn) and the glue that
 * turns their answers into world effects. Everything it keeps is either in the entity's save data ({@link #save}) or recomputed on the
 * first tick after a load, so a reload cannot lose a chain, a channel or a loan; nothing is static.
 * <p>
 * It exists only while {@code luciusAnimeArc} is on: with the switch off the boss never calls it and behaves as in 1.37.0.
 */
final class LuciusArcDirector {
    private static final ResourceLocation STAGGER_SLOW = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "lucius_arc_stagger");
    static final String SAVE_KEY = "LArc";
    private static final int GOLD = FxMath.LUCIUS_GOLD;

    private final LuciusBoss b;
    private final Random rng = new Random();
    LuciusArc arc = new LuciusArc();
    final LuciusChains.Runner runner = new LuciusChains.Runner();
    final PrecogMath.Tracker precog = new PrecogMath.Tracker();
    final TimeReversalMath.Window window = new TimeReversalMath.Window();
    final TimeReversalMath.Channel channel = new TimeReversalMath.Channel();
    final CloneScheduler clones = new CloneScheduler();
    final CloneScheduler.PressureBudget pressure = new CloneScheduler.PressureBudget();
    final PaladinTurn<UUID> turns = new PaladinTurn<>();
    private final CoreGemMath.Burn burn = new CoreGemMath.Burn();
    private final ServerBossEvent channelBar = new ServerBossEvent(Component.empty(), BossEvent.BossBarColor.YELLOW, BossEvent.BossBarOverlay.PROGRESS);

    // the state of the step that is running (re-derived by the BEGIN event after a load)
    private UUID chainTarget;
    private Vec3 mark, dest, retreat;
    private TowerSpells.Entry tower;
    // timers (absolute game ticks, saved)
    private long actStart = -1, staggerUntil, gemClosedUntil, cloneAt, burnUntil;
    private boolean hovering, gemWasOut, warned;
    private double slowAmount;
    private int syncedFlags = -1;
    private long lastNote, lastAura;

    LuciusArcDirector(LuciusBoss boss) { this.b = boss; }

    static boolean on() { return BossRulesConfig.luciusAnimeArc(); }

    // ================================================================ questions the boss asks
    int phase() { return arc.phase(); }

    boolean staggered(long now) { return channel.staggered(now) || now < staggerUntil; }

    boolean channeling() { return channel.channeling(); }

    boolean chainRunning() { return runner.active(); }

    /** Precognition, the chains and the casts all wait while one of these holds. */
    boolean blocked(long now) {
        return runner.active() || channel.channeling() || staggered(now) || b.transforming() || b.julius() || b.refilling();
    }

    /** Is the gem out now (server truth)? */
    boolean gemOut(long now) {
        if (arc.phase() != LuciusArc.CLONES || now < gemClosedUntil || actStart < 0 || b.transforming()) return false;
        return gemTiming().exposed(now - actStart, channelsClones());
    }

    private boolean channelsClones() {
        LuciusChains.Step st = runner.currentStep();
        return runner.active() && st != null && st.kind() == LuciusChains.Kind.CLONE_WAVE;
    }

    /** He holds still while he is hovering (act 1) or in the middle of a chain or a channel; the framework's walking goal stays quiet. */
    boolean holdsStill() { return arc.phase() == LuciusArc.MESSIAH || runner.active() || channel.channeling(); }

    /** Mitigation is lifted while he is open: staggered, channelling, or the gem is out. */
    boolean open(long now) { return staggered(now) || channel.channeling() || gemOut(now); }

    // ================================================================ the tick
    void tick(ServerLevel sl, long now) {
        float hp = b.getHealth(), max = b.getMaxHealth();
        int entered = b.godOn() && b.godStages() ? arc.enterStage(b.godStage()) : arc.update(max > 0 ? hp / max : 1f);      // 1.45.0: with the stages on, the act is the stage
        if (entered != LuciusArc.NONE) onPhase(sl, now, entered);
        window.record(now, hp);
        if (b.julius()) {
            if (runner.active()) runner.abort();
            if (channel.channeling()) breakChannel(sl, now, TimeReversalMath.Interrupt.PHASE_CHANGE);
        }
        tickReversal(sl, now, hp, max);
        tickBurn(sl, now);
        tickStagger(now);
        tickChains(sl, now);
        tickClones(sl, now, hp / Math.max(1f, max));
        tickHover(sl);
        tickAura(sl, now);
        if (b.tickCount % 10 == 0) tickTurns(sl, now);
        tickGem(sl, now);
        syncFlags(now);
    }

    // ================================================================ acts
    private void onPhase(ServerLevel sl, long now, int phase) {
        runner.abort();
        String name = LuciusArc.name(phase);
        switch (phase) {
            case LuciusArc.MESSIAH -> {
                b.announce("Act 1, " + name + ": he floats above the fight. Time bolts that hold you, the towers, and a step away whenever you come near.");
            }
            case LuciusArc.SOUL_CORRUPTOR -> {
                b.announce("Act 2, " + name + ": he comes down. Gravity, then space, then time, then his touch (it corrupts and turns your companions). "
                        + "When he is hurt badly he rewinds time for 3 seconds: break it with anti-magic or a heavy blow.");
                b.say("Enough of heaven. Come, let me show you what a soul weighs.");
                ZogratisVfx.pillar(sl, b.position(), 4, 40, FxMath.divine(b.power()), false);
            }
            case LuciusArc.CLONES -> {
                actStart = now;
                cloneAt = now;
                gemClosedUntil = 0;
                b.announce("Act 3, " + name + ": Body, Blood and Bone make copies of him, each with its own grimoire. His body takes almost no damage now. "
                        + "When the gem on his chest shows, hit it with a critical blow or anti-magic: it shatters every copy.");
                b.say("One of me was never enough for you. Then let there be many.");
                ZogratisVfx.halo(sl, b, 1.8, 120, FxMath.divine(b.power()));
                ZogratisVfx.pillar(sl, b.position(), 5, 40, FxMath.divine(b.power()), false);
            }
            default -> { }
        }
        b.arcRefreshBar();
        if (phase >= LuciusArc.SOUL_CORRUPTOR) releaseHover();
    }

    // ================================================================ chains
    /** Called by the boss's move chooser when nothing is casting: starts the next chain (or a filler move through the framework). */
    int choose(List<Move> bk, MoveScheduler.Situation s, long now) {
        if (arc.phase() == LuciusArc.NONE || blocked(now)) return -1;
        LivingEntity target = b.getTarget();
        if (target == null || !target.isAlive()) return -1;
        int ph = arc.phase();
        double dist = b.distanceTo(target);
        if (ph == LuciusArc.MESSIAH) {                                           // the teleport away, whenever somebody comes within six blocks
            List<LivingEntity> near = b.foes(6.0);
            LuciusChains.Chain ret = LuciusChains.byId(LuciusChains.RETREAT);
            if (!near.isEmpty() && runner.readyAt().getOrDefault(ret.id(), 0L) <= now) {
                startChain(ret, near.get(0), now);
                return -1;
            }
        }
        if (ph == LuciusArc.CLONES && needsClones() && now >= cloneAt) {
            LuciusChains.Chain wave = LuciusChains.byId(LuciusChains.CLONE_WAVE);
            if (runner.readyAt().getOrDefault(wave.id(), 0L) <= now) {
                startChain(wave, target, now);
                return -1;
            }
        }
        Map<String, Long> ready = new java.util.HashMap<>(runner.readyAt());
        ready.put(LuciusChains.CLONE_WAVE, Long.MAX_VALUE);                      // the clone wave is started by need above, never by the draw
        Optional<LuciusChains.Chain> pick = LuciusChains.pick(ph, now, ready, rng, dist, b.godOn());
        if (pick.isPresent()) {
            startChain(pick.get(), target, now);
            return -1;
        }
        return filler(bk, s, ph);
    }

    private static final String[][] FILLERS = {
            {},
            {LuciusTable.TOWER_SPELL, LuciusTable.SOUL_MOTES},
            {LuciusTable.TOWER_SPELL, LuciusTable.LIGHT_STEP, LuciusTable.SOUL_MOTES, LuciusTable.BLOOD_TIDE},
            {LuciusTable.TOWER_SPELL, LuciusTable.SOUL_MOTES, LuciusTable.BONE_ARSENAL}};

    private int filler(List<Move> bk, MoveScheduler.Situation s, int ph) {
        List<Integer> ok = new ArrayList<>();
        for (String id : FILLERS[Math.max(0, Math.min(3, ph))]) {
            for (int i = 0; i < bk.size(); i++) if (bk.get(i).id().equals(id) && b.moveEligible(bk.get(i), s)) ok.add(i);
        }
        return ok.isEmpty() ? -1 : ok.get(rng.nextInt(ok.size()));
    }

    private void startChain(LuciusChains.Chain c, LivingEntity target, long now) {
        if (runner.start(c, now, chainCooldown(c))) chainTarget = target.getUUID();
    }

    /**
     * 1.45.0: starts one of the god tier's answer chains (the Checkmate counter, the reply to a clamped hit) on 'target'. With 'preempt'
     * a running chain or a move being wound up is dropped for it (the AI interrupt); never over a Time Reversal channel, a stagger, a
     * transformation, Julius's window or a stage change. False if it could not start.
     */
    boolean startGodChain(String id, LivingEntity target, long now, boolean preempt) {
        LuciusChains.Chain c = LuciusChains.byId(id);
        if (c == null || target == null || !target.isAlive() || channel.channeling() || staggered(now) || b.transforming() || b.julius() || b.refilling()) return false;
        if (runner.active()) {
            if (!preempt) return false;
            runner.abort();
        }
        if (b.state() == ZogratisBoss.STATE_CASTING) {
            if (!preempt) return false;
            b.interruptCast();
        }
        if (!runner.start(c, now, chainCooldown(c))) return false;
        chainTarget = target.getUUID();
        mark = dest = retreat = null;
        return true;
    }

    /**
     * 1.45.0: a stage ended and the next begins: everything that belonged to the old stage is dropped (the running chain, the rewind
     * channel and its window, the unpaid shatter damage, the stagger, the gem's clock, the copies, the precognition history, the
     * cooldowns of the chains) so the new stage starts clean; the new act is announced by the next tick through {@code enterStage}.
     */
    void onStageChange(ServerLevel sl, long now) {
        runner.abort();
        runner.readyAt().clear();
        if (channel.channeling()) hideChannelBar();
        channel.reset();
        window.clear();
        burn.clear();
        precog.reset();
        pressure.clear();
        staggerUntil = 0;
        gemClosedUntil = 0;
        burnUntil = 0;
        warned = false;
        gemWasOut = false;
        cloneAt = now + 40;
        actStart = -1;
        clones.clear();
        for (ZogratisCloneEntity c : b.clones()) c.fadeAway();
        releaseAllTurns(sl);
        mark = dest = retreat = null;
        tower = null;
    }

    private LivingEntity chainTarget(ServerLevel sl) {
        if (chainTarget == null) return null;
        Entity e = sl.getEntity(chainTarget);
        return e instanceof LivingEntity le && le.isAlive() && ArcSteps.fair(le) ? le : null;
    }

    private void tickChains(ServerLevel sl, long now) {
        if (!runner.active()) return;
        if (channel.channeling() || staggered(now) || b.transforming() || b.julius()) { runner.abort(); return; }
        LuciusChains.Step cur = runner.currentStep();
        boolean needsTarget = cur != null && cur.kind() != LuciusChains.Kind.CLONE_WAVE && cur.kind() != LuciusChains.Kind.RETREAT;
        LivingEntity t = chainTarget(sl);
        if (needsTarget && (t == null || t.distanceToSqr(b) > 64 * 64)) { runner.abort(); return; }    // its target died or left: the chain is over
        LuciusChains.Event e = runner.tick();
        try {
            switch (e.type()) {
                case BEGIN -> begin(sl, e.kind(), e.step(), t);
                case FIRE -> fire(sl, e.kind(), e.step(), t);
                case DONE -> mark = dest = retreat = null;
                default -> { }
            }
        } catch (RuntimeException ex) {
            LogUtils.getLogger().warn("[nusmp] Lucius's chain step {} failed: {}", e.kind(), ex.toString());
            runner.abort();
        }
    }

    private void begin(ServerLevel sl, LuciusChains.Kind kind, int step, LivingEntity t) {
        LuciusChains.Step st = runner.currentStep();
        int tel = st == null ? 12 : st.telegraph();
        switch (kind) {
            case GRAVITY_PIN -> {
                mark = ArcSteps.groundUnder(t);
                ArcSteps.faceTowards(b, t);
                ArcSteps.pinTell(sl, mark, tel);
                b.quip("Kneel.", "Gravity is a courtesy I extend to the small.");
            }
            case SPATIAL_BLINK -> {
                dest = ArcSteps.behindSpot(b, t);
                if (dest != null) ArcSteps.blinkTell(sl, dest, Vec3.directionFromRotation(0f, t.getYRot()), tel);
            }
            case CHRONO_STASIS -> {
                ArcSteps.stasisTell(sl, t, tel);
                b.quip("A moment is all I need.");
            }
            case SOUL_TOUCH -> {
                mark = ArcSteps.groundUnder(t);
                ArcSteps.faceTowards(b, t);
                ArcSteps.touchTell(b, sl, mark, t, tel);
                b.quip("Come. Let me hold what you are.");
            }
            case CHRONO_BOLT -> {
                ArcSteps.faceTowards(b, t);
                ArcSteps.boltTell(sl, b, t, tel);
            }
            case TOWER_VOLLEY -> {
                tower = TowerSpells.draw(b.getRandom());
                ArcSteps.towerTell(b, sl, tower, tel);
            }
            case CRUSH_ZONE -> {
                mark = ArcSteps.groundUnder(t);
                ArcSteps.zoneTell(sl, mark, ArcSteps.zoneRadius(b), tel);
            }
            case CLONE_WAVE -> {
                ZogratisVfx.divineSigil(sl, b.position(), 5.0, tel + 2, FxMath.divine(b.power()), true);
                ZogratisVfx.soulVortex(sl, b.position(), 3.0, tel + 2, ArcSteps.SOUL);
                b.announce("Lucius casts Unlimited Cloning: his chest gem is open while he does.");
                if (b.godOn()) b.god().windup();
            }
            case RETREAT -> {
                List<LivingEntity> near = b.foes(8.0);
                Vec3 from = !near.isEmpty() ? near.get(0).position() : t != null ? t.position() : b.position();
                retreat = ArcSteps.retreatSpot(b, from);
                ArcSteps.retreatTell(b, sl, tel);
            }
            // ---- 1.45.0, the god tier's steps (GodSteps)
            case SOUL_RAY -> {
                GodSteps.soulRayTell(b, sl, t, tel);
                b.quip("Come. Let me hold what you are.");
            }
            case RELIC_EMP -> {
                GodSteps.empTell(b, sl, tel);
                b.god().windup();
            }
            case SEVERANCE -> {
                GodSteps.severTell(b, sl, t, tel);
                b.god().windup();
            }
            case STARVATION_PULSE -> {
                GodSteps.starveTell(b, sl, tel);
                b.god().windup();
            }
            case BLOOD_TIDE -> {
                GodSteps.bloodTell(b, sl, t, tel);
                b.quip("Vanica's blood, my hands.");
            }
            case COUNTER_STRIKE -> {
                mark = ArcSteps.groundUnder(t);
                GodSteps.counterTell(b, sl, mark, t, tel);
            }
            default -> { }
        }
    }

    private void fire(ServerLevel sl, LuciusChains.Kind kind, int step, LivingEntity t) {
        switch (kind) {
            case GRAVITY_PIN -> { if (mark != null) ArcSteps.pinFire(b, sl, mark); }
            case SPATIAL_BLINK -> {
                ArcSteps.blinkFire(b, dest, t);
                if (LuciusChains.CHECKMATE_COUNTER.equals(runner.chainId()) && t instanceof ServerPlayer p) LuciusCapabilities.interruptCast(p, 40);      // 1.45.0: he is behind them, the channel breaks
            }
            case CHRONO_STASIS -> ArcSteps.bubble(b, b, t);
            case SOUL_TOUCH -> { if (mark != null) ArcSteps.touchFire(b, turns, mark, t); }
            case CHRONO_BOLT -> ArcSteps.chronoBolt(b, b, t);
            case TOWER_VOLLEY -> { if (tower != null) ArcSteps.towerFire(b, tower, t, step); }
            case CRUSH_ZONE -> { if (mark != null) ArcSteps.zoneFire(b, mark); }
            case CLONE_WAVE -> spawnClones(sl, t);
            case RETREAT -> ArcSteps.retreatFire(b, retreat);
            case SOUL_RAY -> GodSteps.soulRayFire(b, t);
            case RELIC_EMP -> { if (!b.god().graceOn(sl.getGameTime())) GodSteps.empFire(b, sl); }          // 1.46.0: not in the grace of a stage change
            case SEVERANCE -> { if (!b.god().graceOn(sl.getGameTime())) GodSteps.severFire(b, t); }
            case STARVATION_PULSE -> { if (!b.god().graceOn(sl.getGameTime())) GodSteps.starveFire(b, sl); }
            case BLOOD_TIDE -> GodSteps.bloodFire(b, t);
            case COUNTER_STRIKE -> GodSteps.counterFire(b, mark, t, runner.chainId() != null && LuciusChains.CHECKMATE_COUNTER.equals(runner.chainId()) ? TrueDamage.Tier.FINISHER : TrueDamage.Tier.HEAVY);
            default -> { }
        }
    }

    // ================================================================ precognition and the damage path
    /** The player (or the owner of a pet or a relic summon) that really struck: pets and summons do not count as a second attacker. */
    private static int attackerKey(ServerLevel sl, Entity attacker) {
        if (attacker == null) return -1;
        UUID owner = PaladinTurns.ownerOf(attacker);
        if (owner != null && !(attacker instanceof Player) && sl.getPlayerByUUID(owner) instanceof ServerPlayer p) return p.getId();
        return attacker.getId();
    }

    private static int worn(Player p, Grade atLeast) {
        int n = 0;
        for (Relics.Worn w : Relics.loadout(p).active()) if (w.def().grade().atLeast(atLeast)) n++;
        return n;
    }

    /**
     * The whole damage filter of the arc, called from {@code LuciusBoss.filterDamage}: Julius's window first (more damage, no precognition),
     * then the Time Reversal channel (it can be broken by this very blow; while it runs he takes 25% more), then precognition (a blow is
     * phased out unless it is one of the four answers), then the body factor of act 3. Returns the amount to apply, 0 for a phased blow.
     */
    float filter(ServerLevel sl, DamageSource source, float amount, long now, float raw) {
        Entity attacker = source.getEntity();
        boolean anti = LuciusArcHooks.antiMagic(source);
        Player player = attacker instanceof Player p ? p : null;
        if (b.julius()) return amount;
        float out = amount;
        if (channel.channeling()) {
            boolean relicHeavy = player != null && worn(player, Grade.LEGEND) > 0;
            if (anti) breakChannel(sl, now, TimeReversalMath.Interrupt.ANTI_MAGIC);
            else if (TimeReversalMath.heavy(raw, b.getMaxHealth(), relicHeavy)) breakChannel(sl, now, TimeReversalMath.Interrupt.HEAVY_HIT);      // 'raw' is the blow before the god tier's hit cap
            out = amount * TimeReversalMath.DAMAGE_PERCENT / 100f;
        }
        boolean active = !channel.channeling() && !staggered(now) && !gemOut(now) && !b.transforming() && !b.exposed();
        boolean synced = player != null && PrecogMath.synchronizedRelics(worn(player, Grade.UNIQUE));
        PrecogMath.Verdict v = precog.onHit(now, attackerKey(sl, attacker), anti, synced, active);
        if (!v.lands()) {
            phaseOut(sl, attacker, now);
            return 0f;
        }
        switch (v) {
            case ANTI_MAGIC -> note(now, "Anti-magic is outside the time he reads: the blow lands.");
            case SYNCED_RELICS -> note(now, "Synchronized relics break his precognition: the blow lands.");
            case MULTI_ATTACKER -> note(now, "Two strikes at once are out of his sequence: the blow lands.");
            case RAPID -> note(now, "Too fast to read: the blow lands.");
            default -> { }
        }
        return out * (b.godOn() && arc.phase() == LuciusArc.CLONES ? GOD_BODY_FACTOR : CoreGemMath.bodyFactor(arc.phase()));
    }

    /** 1.45.0: in the god tier the last stage starts from a full bar, so the body takes half of a blow, not 15% (the hit cap and the gem still rule). */
    static final float GOD_BODY_FACTOR = 0.5f;

    private void note(long now, String text) {
        if (now - lastNote < 60) return;
        lastNote = now;
        b.announce(text);
    }

    /** The after-image: a rewind dial where he stood, a chime, and a word to the attacker (at most every two seconds). */
    private void phaseOut(ServerLevel sl, Entity attacker, long now) {
        Vec3 at = b.position();
        VfxSpawn.send(sl, VfxShape.TIME_REWIND, at.add(0, 0.05, 0), at.add(0, 1, 0), 0, 12, 0.6f);
        ZogratisVfx.soulVortex(sl, at, 1.0, 10, FxMath.divine(b.power()));
        LuciusFx.chime(sl, at, 1.8f);
        if (attacker instanceof ServerPlayer p && now - p.getPersistentData().getLong("nusmp_lu_phase_note") >= 40) {
            p.getPersistentData().putLong("nusmp_lu_phase_note", now);
            p.displayClientMessage(Component.literal("Lucius phased out of the blow. Strike together, very fast, with anti-magic, or with synchronized relics.")
                    .withStyle(ChatFormatting.GOLD), true);
        }
    }

    // ================================================================ time reversal
    private void tickReversal(ServerLevel sl, long now, float hp, float max) {
        if (channel.channeling()) {
            if (TimeStop.isFrozen(b) || locked()) breakChannel(sl, now, TimeReversalMath.Interrupt.LOCK);
            else {
                int left = channel.left(now);
                if (channelBar.getName().getString().isEmpty()) {            // a channel resumed after a load
                    channelBar.setName(Component.literal("TIME REVERSAL - break it with anti-magic or a heavy blow!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    BossBarSync.update(b, channelBar, "lucius_zogratis", GOLD, 0xFFFFFF, 4, true);
                }
                if (now % 20 == 0) for (ServerPlayer p : sl.players()) if (p.distanceToSqr(b) < 128 * 128) channelBar.addPlayer(p);
                channelBar.setProgress(Math.max(0f, Math.min(1f, 1f - left / (float) TimeReversalMath.CHANNEL_TICKS)));
                if (now % 20 == 0) {
                    VfxSpawn.send(sl, VfxShape.TIME_REWIND, b.position().add(0, 0.05, 0), b.position().add(0, 1, 0), 0, 24, 1.8f);
                    ZogratisVfx.soulVortex(sl, b.position(), 2.4, 24, GOLD);
                }
                b.getNavigation().stop();
            }
        }
        if (channel.tick(now)) completeChannel(sl, now);
        if (!channel.channeling()) hideChannelBar();
        boolean allowed = BossRulesConfig.luciusTimeReversal() && arc.phase() == LuciusArc.SOUL_CORRUPTOR && !b.julius() && !b.transforming()
                && !staggered(now) && b.state() != ZogratisBoss.STATE_CASTING;
        if (allowed && channel.ready(now) && window.burst(now, hp, max)) beginChannel(sl, now);
    }

    /** A spatial or time lock on him: any active effect named for a stasis, a stop or a lock. */
    private boolean locked() {
        for (MobEffectInstance i : b.getActiveEffects()) {
            String path = i.getEffect().unwrapKey().map(k -> k.location().getPath()).orElse("");
            if (path.contains("stasis") || path.contains("time_stop") || path.contains("spatial_lock") || path.contains("restrain")) return true;
        }
        return false;
    }

    private void beginChannel(ServerLevel sl, long now) {
        float start = window.highest(now);
        if (!channel.begin(now, start)) return;
        runner.abort();
        Vec3 g = ZogratisBoss.ground(sl, b.position());
        VfxSpawn.send(sl, VfxShape.TIME_CLOCK, g, g.add(0, 6, 0), 0, TimeReversalMath.CHANNEL_TICKS + 6, 4.5f);
        VfxSpawn.send(sl, VfxShape.TIME_REWIND, b.position().add(0, 0.05, 0), b.position().add(0, 1, 0), 0, TimeReversalMath.CHANNEL_TICKS, 2.0f);
        ZogratisVfx.divineSigil(sl, g, 5.0, TimeReversalMath.CHANNEL_TICKS, GOLD, true);
        LuciusFx.sound(sl, b.position(), SoundEvents.BELL_RESONATE, 3f, 0.5f);
        channelBar.setName(Component.literal("TIME REVERSAL - break it with anti-magic or a heavy blow!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        channelBar.setProgress(0f);
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(b) < 128 * 128) channelBar.addPlayer(p);
        BossBarSync.update(b, channelBar, "lucius_zogratis", GOLD, 0xFFFFFF, 4, true);
        b.announce("Lucius channels Time Reversal for 3 seconds: he is rooted, takes 25% more damage, and will undo what you did. "
                + "Interrupt it with anti-magic, a heavy blow (6% of his health) or a lock.");
        b.say("Four seconds ago, you had not hurt me yet.");
        window.clear();
    }

    private void breakChannel(ServerLevel sl, long now, TimeReversalMath.Interrupt why) {
        if (!channel.interrupt(now)) return;
        hideChannelBar();
        Vec3 at = b.position();
        ZogratisVfx.spaceShatter(sl, at.add(0, 1, 0), 3.5, GOLD);
        LuciusFx.sound(sl, at, SoundEvents.AMETHYST_BLOCK_BREAK, 3f, 0.6f);
        LuciusFx.sound(sl, at, SoundEvents.BEACON_DEACTIVATE, 2f, 0.6f);
        LuciusFx.bar(sl, at, 64, Component.literal("TIME REVERSAL BROKEN! Lucius is staggered: strike now!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        b.announce("Time Reversal is broken (" + why.name().toLowerCase(java.util.Locale.ROOT).replace('_', ' ') + "): Lucius is staggered for 6 seconds, "
                + "slowed and unable to cast, and the damage stays.");
        window.clear();
    }

    private void completeChannel(ServerLevel sl, long now) {
        hideChannelBar();
        float hp = b.getHealth(), max = b.getMaxHealth();
        float ceiling = b.godOn() && b.godStages() ? max : LuciusArc.P2_BELOW * max - 1f;      // a stage starts from a full bar: the rewind may go back as far as the burst began
        float to = TimeReversalMath.restoreTarget(channel.startHealth(), hp, max, ceiling);
        if (to > hp) BossHealGuard.allow(() -> b.dpsBudget().bypass(() -> b.setHealth(to)));   // the owner's exception; PhaseRefill does the same
        Vec3 at = b.position();
        VfxSpawn.send(sl, VfxShape.TIME_REWIND, at.add(0, 0.05, 0), at.add(0, 1, 0), 0, 40, 2.4f);
        ZogratisVfx.divineSigil(sl, at, 6, 30, GOLD, false);
        LuciusFx.sound(sl, at, SoundEvents.BELL_RESONATE, 3f, 1.4f);
        b.announce("Time turns back for Lucius: " + Math.round(Math.max(0, b.getHealth() - hp)) + " health returns. It was not interrupted. The next rewind needs 25 seconds.");
        window.clear();
    }

    private void hideChannelBar() {
        if (channelBar.getPlayers().isEmpty()) return;
        BossBarSync.remove(channelBar);
        channelBar.removeAllPlayers();
    }

    void watchedBy(ServerPlayer p) { if (channel.channeling()) channelBar.addPlayer(p); }

    void unwatchedBy(ServerPlayer p) { channelBar.removePlayer(p); }

    // ================================================================ stagger and the burn
    private void tickStagger(long now) {
        AttributeInstance sp = b.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp == null) return;
        double want = channel.channeling() ? -1.0 : staggered(now) ? -0.6 : 0.0;      // rooted while he channels, slowed while he is staggered
        if (want == slowAmount) return;
        slowAmount = want;
        if (want == 0.0) sp.removeModifier(STAGGER_SLOW);
        else sp.addOrUpdateTransientModifier(new AttributeModifier(STAGGER_SLOW, want, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    private void tickBurn(ServerLevel sl, long now) {
        if (!burn.active()) return;
        if (now > burnUntil) { burn.clear(); return; }
        if (b.transforming()) return;                                                  // the Gates' cutscene shield holds against this too
        float hp = b.getHealth(), max = b.getMaxHealth();
        int due = PowerCurve.next(b.powerHealth() / b.powerMaxHealth(), b.power());
        if (due >= PowerCurve.FULL && b.power() < PowerCurve.FULL) return;          // the Gates are about to open: nothing may skip them
        float want = Math.min(burn.want(), hp - 1f);
        want = PowerCurve.clampDamage(b.powerHealth(), b.powerMaxHealth(), due, want);
        if (want <= 0f) { if (hp <= 1f) burn.clear(); return; }
        b.setHealth(hp - want);                                                      // ZogratisBoss.setHealth: cut to the per-second budget
        burn.paid(hp - b.getHealth());
    }

    // ================================================================ the gem and the clones
    /** Where the gem is: his chest, a little in front of it. */
    Vec3 gemCenter() {
        Vec3 fwd = Vec3.directionFromRotation(0f, b.yBodyRot);
        return b.position().add(0, b.getBbHeight() * 0.68, 0).add(fwd.scale(0.35));
    }

    private void tickGem(ServerLevel sl, long now) {
        boolean out = gemOut(now);
        if (arc.phase() == LuciusArc.CLONES && actStart >= 0 && !out) {
            CoreGemMath.Timing tm = gemTiming();
            int in = tm.opensIn(now - actStart);
            if (in == tm.warnTicks() && !warned && now >= gemClosedUntil) {
                warned = true;
                LuciusFx.chime(sl, b.position(), 1.2f);
                ZogratisVfx.divineSigil(sl, b.position(), 2.2, tm.warnTicks(), GOLD, true);
            }
            if (in != tm.warnTicks()) warned = false;
        }
        if (out && !gemWasOut) {
            CoreGemMath.Timing tmo = gemTiming();
            int ticks = channelsClones() ? 80 : (int) Math.max(20, tmo.closedFirst() + tmo.openTicks() - ((now - actStart) % tmo.cycleTicks()));
            VfxSpawn.sendFollowing(sl, VfxShape.LUCIUS_CORE_GEM, b, b.position().add(0, b.getBbHeight() * 0.68, 0), 0xFF000000 | FxMath.divine(b.power()), ticks, 0.5f);
            LuciusFx.sound(sl, b.position(), SoundEvents.BEACON_ACTIVATE, 2f, 1.6f);
            b.announce("The gem on Lucius's chest is open: a critical blow or anti-magic on it shatters his resonance.");
        }
        gemWasOut = out;
    }

    /** 1.46.0: the gem window of this fight: the god tier's is 50% longer and announced earlier (the owner's balance pass). */
    private CoreGemMath.Timing gemTiming() { return b.godOn() ? CoreGemMath.Timing.GOD : CoreGemMath.Timing.CLASSIC; }

    /** 1.46.0: how many copies are wanted: the god tier shows two to three (one more per player beyond two, five at most), the 1.42.0 fight three to five. */
    private int cloneWant(float healthFraction) {
        if (b.godOn()) return CloneScheduler.cloneCountGod(healthFraction, b.god().arenaPlayers());
        return Math.max(CloneScheduler.MIN_CLONES, CloneScheduler.cloneCount(healthFraction));
    }

    /** 1.46.0: the cooldown a chain gets when it starts: the god tier divides it by the difficulty, above the floors of {@link LuciusBalance}; the 1.42.0 fight keeps the table's number. */
    private int chainCooldown(LuciusChains.Chain c) {
        return b.godOn() ? LuciusBalance.cooldown(c.id(), c.cooldown(), LuciusGodConfig.difficulty()) : c.cooldown();
    }

    private boolean needsClones() {
        float f = b.getHealth() / Math.max(1f, b.getMaxHealth());
        return b.clones().size() < cloneWant(f);
    }

    private void spawnClones(ServerLevel sl, LivingEntity around) {
        float f = b.getHealth() / Math.max(1f, b.getMaxHealth());
        int want = cloneWant(f);
        List<ZogratisCloneEntity> live = b.clones();
        boolean[] used = new boolean[CloneScheduler.MAX_CLONES];
        for (ZogratisCloneEntity c : live) if (c.arcSlot() >= 0 && c.arcSlot() < used.length) used[c.arcSlot()] = true;
        Vec3 centre = b.position();
        double phase0 = b.getRandom().nextDouble() * Math.PI * 2;
        int made = 0;
        for (int slot = 0; slot < want && live.size() + made < want; slot++) {
            if (used[slot]) continue;
            for (int tries = 0; tries < 6; tries++) {
                double a = phase0 + (slot + tries * 0.37) * Math.PI * 2 / Math.max(3, want), r = 6.5 + 2.0 * b.getRandom().nextDouble();
                Vec3 spot = LuciusFx.standable(sl, b, centre.add(Math.cos(a) * r, 0, Math.sin(a) * r));
                if (spot == null || spot.distanceToSqr(b.arenaCenter()) > (double) ZogratisBoss.ARENA_RADIUS * ZogratisBoss.ARENA_RADIUS) continue;
                if (ZogratisCloneEntity.spawnArc(b, sl, spot, slot) != null) made++;
                break;
            }
        }
        cloneAt = sl.getGameTime() + 240;
        clones.resize(want, sl.getGameTime());
        if (made > 0) b.announce("Lucius is now " + (live.size() + made + 1) + " Luciuses. Only the one with the gem is the real one.");
    }

    private void tickClones(ServerLevel sl, long now, float frac) {
        if (arc.phase() != LuciusArc.CLONES) return;
        clones.setInterval(b.godOn() ? CloneScheduler.GOD_CAST_INTERVAL : CloneScheduler.CAST_INTERVAL);
        int want = cloneWant(frac);
        if (clones.slots() != want) clones.resize(want, now);
        if (b.tickCount % 10 != 0 || b.julius()) return;
        List<ZogratisCloneEntity> live = b.clones();
        boolean[] alive = new boolean[CloneScheduler.MAX_CLONES];
        ZogratisCloneEntity[] bySlot = new ZogratisCloneEntity[CloneScheduler.MAX_CLONES];
        LivingEntity tg = b.getTarget();
        for (ZogratisCloneEntity c : live) {
            int s = c.arcSlot();
            if (s < 0 || s >= alive.length) continue;
            bySlot[s] = c;
            alive[s] = c.arcIdle() && tg != null && tg.isAlive();
            if (c.distanceToSqr(b.arenaCenter()) > (double) ZogratisBoss.ARENA_RADIUS * ZogratisBoss.ARENA_RADIUS) {     // never outside the arena
                Vec3 back = LuciusFx.standable(sl, c, b.position().add(2, 0, 2));
                if (back != null) c.teleportTo(back.x, back.y, back.z);
            }
        }
        // the schedule ticks once per game tick, but the clones' readiness is read every ten: catch up with the ticks in between
        for (int k = 0; k < 10; k++) {
            for (CloneScheduler.Cast cast : clones.tick(now - 9 + k, alive)) {
                ZogratisCloneEntity c = bySlot[cast.slot()];
                if (c != null && tg != null) { c.beginArcCast(cast.spell(), tg); alive[cast.slot()] = false; }
            }
        }
    }

    /** The gem was struck by something that shatters it: every clone goes, the core takes the damage over a few seconds, he is staggered. */
    void shatter(Entity by) {
        if (!(b.level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        if (!gemOut(now)) return;
        Vec3 at = gemCenter();
        ZogratisVfx.spaceShatter(sl, at, 5.0, GOLD);
        ZogratisVfx.pillar(sl, b.position(), 4, 30, FxMath.divine(b.power()), false);
        LuciusFx.sound(sl, at, SoundEvents.AMETHYST_BLOCK_BREAK, 3f, 0.5f);
        LuciusFx.sound(sl, at, SoundEvents.BEACON_DEACTIVATE, 3f, 0.5f);
        int gone = 0;
        for (ZogratisCloneEntity c : b.clones()) { c.fadeAway(); gone++; }
        burn.add(CoreGemMath.shatterDamage(b.getMaxHealth(), b.getHealth()));
        burnUntil = now + 200;
        staggerUntil = now + CoreGemMath.SHATTER_STAGGER;
        gemClosedUntil = now + 400;
        cloneAt = now + CoreGemMath.SHATTER_STAGGER + 100;
        runner.abort();
        precog.reset();
        b.announce("THE CORE GEM SHATTERS" + (by instanceof Player p ? " (" + p.getName().getString() + ")" : "") + ": " + gone
                + " clones are gone, Lucius takes massive true damage and is staggered for 10 seconds.");
        LuciusFx.bar(sl, b.position(), 64, Component.literal("RESONANCE SHATTERED - strike him now!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        if (b.godOn()) b.god().gemShattered();
    }

    // ================================================================ hover and aura
    private void tickHover(ServerLevel sl) {
        boolean want = arc.phase() == LuciusArc.MESSIAH && !b.transforming() && b.isAlive();
        if (!want) { releaseHover(); return; }
        Vec3 from = b.position().add(0, 0.3, 0);
        BlockHitResult hit = sl.clip(new ClipContext(from, from.add(0, -HoverMath.PROBE - 1, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, b));
        double gd = hit.getType() == HitResult.Type.MISS ? Double.NaN : b.getY() - hit.getLocation().y;
        if (!HoverMath.canHover(gd)) { releaseHover(); return; }                 // nothing below: he falls like any mob, he never drifts into the sky
        b.setNoGravity(true);
        hovering = true;
        b.fallDistance = 0;
        double vx = 0, vz = 0;
        LivingEntity t = b.getTarget();
        if (t != null && !runner.active() && !channel.channeling()) {
            Vec3 dir = LuciusFx.flatDir(b.position(), t.position(), b.getViewVector(1f));
            double drift = HoverMath.drift(b.distanceTo(t));
            vx = dir.x * drift;
            vz = dir.z * drift;
        }
        b.setDeltaMovement(vx, HoverMath.verticalSpeed(gd), vz);
        b.getNavigation().stop();
    }

    private void releaseHover() {
        if (!hovering && !b.isNoGravity()) return;                  // also frees a boss that was saved in the air and loaded after the act changed
        hovering = false;
        b.setNoGravity(false);
        b.fallDistance = 0;
    }

    /** The aura of superiority: a light Weakness on everyone within twelve blocks, renewed every two seconds. It does not stun. */
    private void tickAura(ServerLevel sl, long now) {
        if (arc.phase() != LuciusArc.MESSIAH || now - lastAura < 40 || b.julius()) return;
        lastAura = now;
        boolean any = false;
        for (LivingEntity f : b.foes(12)) {
            if (!ArcSteps.fair(f)) continue;
            f.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 50, 0, true, false, true));
            any = true;
        }
        if (any) ZogratisVfx.divineSigil(sl, b.position(), 12, 36, FxMath.divine(b.power()), true);
    }

    // ================================================================ turned companions
    private void tickTurns(ServerLevel sl, long now) {
        if (turns.isEmpty()) return;
        for (PaladinTurn.Loan<UUID> loan : turns.loans()) {
            Entity e = sl.getEntity(loan.id());
            if (!(e instanceof Mob m) || !m.isAlive()) {
                turns.release(loan.id());
                if (e instanceof Mob gone) PaladinTurns.restore(gone);
                continue;
            }
            if (!PaladinTurns.isTurned(m)) {                      // its tag is gone (it was given back on a load): the loan is over
                turns.release(loan.id());
                continue;
            }
            if (now >= loan.until()) {
                turns.release(loan.id());
                PaladinTurns.restore(m);
                if (sl.getPlayerByUUID(loan.owner()) instanceof ServerPlayer p)
                    p.displayClientMessage(Component.literal("Your companion is itself again.").withStyle(ChatFormatting.GREEN), true);
                continue;
            }
            PaladinTurns.drive(m, now);
        }
    }

    /** One creature is freed early (anti-magic): removed from the loans and given back. */
    void free(Mob m) {
        turns.release(m.getUUID());
        PaladinTurns.restore(m);
    }

    /** Every creature is given back (he died, the fight ended, he left the world). */
    void releaseAllTurns(ServerLevel sl) {
        for (PaladinTurn.Loan<UUID> l : turns.releaseAll()) if (sl.getEntity(l.id()) instanceof Mob m) PaladinTurns.restore(m);
    }

    // ================================================================ sync
    private void syncFlags(long now) {
        int f = Math.max(0, arc.phase())
                | (gemOut(now) ? LuciusBoss.ARC_GEM : 0)
                | (channel.channeling() ? LuciusBoss.ARC_CHANNEL : 0)
                | (staggered(now) ? LuciusBoss.ARC_STAGGER : 0)
                | (hovering ? LuciusBoss.ARC_LEVITATE : 0);
        if (f != syncedFlags) {
            syncedFlags = f;
            b.setArcFlags(f);
        }
    }

    // ================================================================ lifecycle
    /** A fresh fight (or its end): every state machine back to the start, every loan returned, the hover off. */
    void reset() {
        arc.reset();
        runner.abort();
        runner.readyAt().clear();
        precog.reset();
        window.clear();
        channel.reset();
        clones.clear();
        pressure.clear();
        burn.clear();
        chainTarget = null;
        mark = dest = retreat = null;
        tower = null;
        actStart = -1;
        staggerUntil = gemClosedUntil = cloneAt = burnUntil = 0;
        gemWasOut = false;
        warned = false;
        releaseHover();
        if (b.level() instanceof ServerLevel sl) releaseAllTurns(sl);
        AttributeInstance sp = b.getAttribute(Attributes.MOVEMENT_SPEED);
        if (sp != null) sp.removeModifier(STAGGER_SLOW);
        slowAmount = 0.0;
        hideChannelBar();
        syncedFlags = -1;
        b.setArcFlags(0);
    }

    /** The boss leaves the world: loans back, bar gone. */
    void close() {
        if (b.level() instanceof ServerLevel sl) releaseAllTurns(sl);
        hideChannelBar();
        releaseHover();
    }

    // ================================================================ save data
    void save(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        int[] a = arc.save();
        t.putInt("Phase", a[0]);
        t.putInt("Entered", a[1]);
        LuciusChains.Saved c = runner.save();
        if (c != null) {
            CompoundTag ct = new CompoundTag();
            ct.putString("Id", c.chainId());
            ct.putInt("Step", c.step());
            ct.putInt("Left", c.left());
            t.put("Chain", ct);
        }
        if (chainTarget != null) t.putUUID("Target", chainTarget);
        ListTag ready = new ListTag();
        for (Map.Entry<String, Long> e : runner.readyAt().entrySet()) {
            CompoundTag r = new CompoundTag();
            r.putString("Id", e.getKey());
            r.putLong("At", e.getValue());
            ready.add(r);
        }
        t.put("Ready", ready);
        long[] ch = channel.save();
        t.putLongArray("Rev", ch);
        t.putLong("ActStart", actStart);
        t.putLong("Stagger", staggerUntil);
        t.putLong("GemClosed", gemClosedUntil);
        t.putLong("CloneAt", cloneAt);
        t.putLong("BurnUntil", burnUntil);
        t.putFloat("Burn", burn.pending());
        ListTag loans = new ListTag();
        for (PaladinTurn.Loan<UUID> l : turns.loans()) {
            CompoundTag lt = new CompoundTag();
            lt.putUUID("Id", l.id());
            lt.putUUID("Owner", l.owner());
            lt.putLong("Until", l.until());
            loans.add(lt);
        }
        t.put("Turns", loans);
        tag.put(SAVE_KEY, t);
    }

    void load(CompoundTag tag) {
        if (!tag.contains(SAVE_KEY)) return;
        CompoundTag t = tag.getCompound(SAVE_KEY);
        arc = LuciusArc.restore(t.getInt("Phase"), t.getInt("Entered"));
        for (Tag r : t.getList("Ready", Tag.TAG_COMPOUND)) {
            CompoundTag rt = (CompoundTag) r;
            runner.readyAt().put(rt.getString("Id"), rt.getLong("At"));
        }
        if (t.hasUUID("Target")) chainTarget = t.getUUID("Target");
        if (t.contains("Chain")) {
            CompoundTag ct = t.getCompound("Chain");
            runner.restore(new LuciusChains.Saved(ct.getString("Id"), ct.getInt("Step"), ct.getInt("Left")), 0);
        }
        channel.restore(t.getLongArray("Rev"));
        actStart = t.getLong("ActStart");
        if (!t.contains("ActStart")) actStart = -1;
        staggerUntil = t.getLong("Stagger");
        gemClosedUntil = t.getLong("GemClosed");
        cloneAt = t.getLong("CloneAt");
        burnUntil = t.getLong("BurnUntil");
        burn.clear();
        burn.add(t.getFloat("Burn"));
        for (Tag l : t.getList("Turns", Tag.TAG_COMPOUND)) {
            CompoundTag lt = (CompoundTag) l;
            if (lt.hasUUID("Id") && lt.hasUUID("Owner")) turns.restore(lt.getUUID("Id"), lt.getUUID("Owner"), lt.getLong("Until"));
        }
    }
}
