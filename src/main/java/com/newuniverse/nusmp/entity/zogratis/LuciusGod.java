package com.newuniverse.nusmp.entity.zogratis;

import com.newuniverse.nusmp.boss.arc.LuciusChains;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.boss.god.ArenaScaling;
import com.newuniverse.nusmp.boss.god.CounterRules;
import com.newuniverse.nusmp.boss.god.HitGate;
import com.newuniverse.nusmp.boss.god.LuciusBalance;
import com.newuniverse.nusmp.boss.god.LuciusGodConfig;
import com.newuniverse.nusmp.boss.god.LuciusLines;
import com.newuniverse.nusmp.boss.god.LuciusStage;
import com.newuniverse.nusmp.boss.god.ReflectWindow;
import com.newuniverse.nusmp.boss.god.TrapLedger;
import com.newuniverse.nusmp.boss.god.TrueDamage;
import com.newuniverse.nusmp.boss.rules.BossHealGuard;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the god-tier Lucius (1.45.0) is a stack of counter-mechanics on top of the 1.42.0 final arc, and most of them are about
 * <i>this one boss</i> (his stages, his hit cap, his voice, his traps, his answers to what players do), so they live in one object per
 * boss instead of being spread over {@link LuciusBoss} (already twelve hundred lines) and {@link LuciusArcDirector}. The boss forwards a
 * handful of hooks to it, the way it does to the director; the pure rules are in {@code boss/god} and are unit tested. The things that
 * are about <i>players</i> (the Time Decay aura, flight, starvation, sabotage effects, ManasCore skill events) are in
 * {@link LuciusBossEventHandler} and {@link LuciusCapabilities}, which find the boss through the registry of live god-tier fights kept
 * here. Everything is switchable ({@link LuciusGodConfig}); with the master switch off no hook does anything and the boss is the 1.42.0 one.
 * <p>
 * The three stages. A fatal blow in stage 1 or 2 does not kill him: {@link #fall} puts him back on his feet at 1 health at once (a body
 * left at 0 health is removed by the game after a second), advances the stage, and starts the existing invulnerable refill
 * ({@code ZogratisBoss.refillForMode}, the machinery of the Gates and of PhaseRefill: health eases up to the maximum, the damage budget is
 * bypassed, the heal guard is allowed, nothing can hurt him meanwhile). Each stage has its own refill id, remembered in the save data
 * by {@code PhaseRefill}, so the same refill can not fire twice; if the refill is refused for any reason he dies for real rather than
 * stay at 1 health. The Gates' own refill stays off for him (as in 1.42.0): the stage change is the only refill.
 */
final class LuciusGod {
    /** Live fights with the god tier on (server thread only). Boss -> registered while its fight is active. */
    private static final Set<LuciusBoss> ACTIVE = Collections.newSetFromMap(new IdentityHashMap<>());

    static boolean anyActive() { return !ACTIVE.isEmpty(); }

    /** The live god-tier bosses themselves, for a read-only walk on the server thread (no copy: the player tick asks this every tick). */
    static Iterable<LuciusBoss> live() { return ACTIVE; }

    /** A copy of the live god-tier bosses. */
    static List<LuciusBoss> active() { return ACTIVE.isEmpty() ? List.of() : new ArrayList<>(ACTIVE); }

    static void forgetAll() { ACTIVE.clear(); }

    private final LuciusBoss b;
    final LuciusStage stage = new LuciusStage();
    final HitGate gate = new HitGate();
    final TrapLedger traps = new TrapLedger();
    final ReflectWindow reflect = new ReflectWindow();
    private final CounterRules.Cooldowns<UUID> checkmateCd = new CounterRules.Cooldowns<>(), swapCd = new CounterRules.Cooldowns<>();
    private final CounterRules.Cooldowns<Boolean> reversalCd = new CounterRules.Cooldowns<>();
    /** 1.46.0: half a second of grace after a true hit, per player. */
    private final CounterRules.Cooldowns<UUID> graceCd = new CounterRules.Cooldowns<>();
    private static final ResourceLocation SCALE_ID = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "arena_scale");
    /** 1.46.0: the tick the stage-change grace ends (aura, EMP, starvation off until then), and the player count his numbers were scaled for. */
    private long graceUntil;
    private boolean scaled, fillOnScale;
    private int scaledFor = 2;
    private final int[] spoken = new int[LuciusLines.Event.values().length];
    private long seed, lastWindup = -1000, lastClampNote, lastDecayRing = -1000, lastFieldRing = -1000;

    LuciusGod(LuciusBoss boss) { this.b = boss; }

    static boolean on() { return LuciusGodConfig.godTier(); }

    int stageNo() { return stage.stage(); }

    // ================================================================ registry and lifecycle
    private void register() { ACTIVE.add(b); }

    void unregister() { ACTIVE.remove(b); }

    /** A new fight, or the end of one: everything back to the start. */
    void reset() {
        stage.reset();
        gate.reset();
        reflect.clear();
        checkmateCd.clear();
        swapCd.clear();
        reversalCd.clear();
        graceCd.clear();
        graceUntil = 0;
        unscale();
        fillOnScale = true;                                               // a new fight starts full at its scaled maximum; a reload in the middle of one does not heal him
        java.util.Arrays.fill(spoken, 0);
        seed = 0;
        lastWindup = -1000;
        clearTraps();
        unregister();
    }

    /** The boss leaves the world (or the fight is over): nothing of ours is left on the players. */
    void close() {
        unregister();
        clearTraps();
        if (b.level() instanceof ServerLevel sl) LuciusCapabilities.clearAll(sl);
    }

    void clearTraps() { traps.clear(); }

    /** Every tick of an active fight (from the boss's tick, god tier on). */
    void tick(ServerLevel sl, long now) {
        if (!b.fightActive() || !b.isAlive()) {
            unregister();
            return;
        }
        register();
        if (!scaled) {
            rescale(fillOnScale);
            fillOnScale = false;
        }
        if (LuciusGodConfig.timeDecay() && !b.julius() && !graceOn(now) && now - lastDecayRing >= 50) {
            lastDecayRing = now;
            ZogratisVfx.divineSigil(sl, ZogratisBoss.ground(sl, b.position()), LuciusGodConfig.auraRadius(), 60, 0xD8A64A, false);
        }
        if (LuciusGodConfig.starvation() && stage.stage() >= 2 && !graceOn(now) && now - lastFieldRing >= 80) {
            lastFieldRing = now;
            ZogratisVfx.divineSigil(sl, ZogratisBoss.ground(sl, b.position()), GodSteps.STARVE_RADIUS, 90, GodSteps.INDIGO, false);
        }
    }

    // ================================================================ 1.46.0: balance (players in the arena, difficulty, grace)
    /** Is the stage-change grace running (the aura, the EMP and the starvation drain are off)? */
    boolean graceOn(long now) { return LuciusBalance.inStageGrace(now, graceUntil); }

    /** The survival players inside the arena now (at least one, so the arithmetic never divides by an empty room). */
    int arenaPlayers() {
        if (!(b.level() instanceof ServerLevel sl)) return 1;
        int n = 0;
        for (ServerPlayer p : sl.players()) if (ArcSteps.fair(p) && inArena(p)) n++;
        return Math.max(1, n);
    }

    /**
     * Scales his maximum health to the players in the arena ({@link ArenaScaling}); read at the start of the fight and of every stage,
     * not every tick. At the start of the fight ('fill') he is also set to the new maximum, as a scripted heal that the heal guard knows.
     */
    void rescale(boolean fill) {
        AttributeInstance mh = b.getAttribute(Attributes.MAX_HEALTH);
        if (mh == null) return;
        int n = arenaPlayers();
        scaled = true;
        scaledFor = n;
        mh.addOrReplacePermanentModifier(new AttributeModifier(SCALE_ID, ArenaScaling.healthModifier(n), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        if (fill) b.dpsBudget().bypass(() -> BossHealGuard.allow(() -> b.setHealth(b.getMaxHealth())));
    }

    private void unscale() {
        scaled = false;
        AttributeInstance mh = b.getAttribute(Attributes.MAX_HEALTH);
        if (mh != null) mh.removeModifier(SCALE_ID);
    }

    /** The factor on every number of his that hurts a player: the difficulty master times the table for the players present. */
    double damageScale() { return LuciusBalance.damageFactor(LuciusGodConfig.difficulty()) * ArenaScaling.damage(scaledFor); }

    boolean inArena(Entity e) {
        if (e.level() != b.level()) return false;
        double r = ZogratisBoss.ARENA_RADIUS + 4.0;
        return e.distanceToSqr(b.arenaCenter()) <= r * r;
    }

    private boolean busy(long now) {
        return b.director().blocked(now) || b.state() == ZogratisBoss.STATE_CASTING || b.refilling() || b.transforming();
    }

    // ================================================================ voice
    private long seed() {
        if (seed == 0) seed = (b.getUUID().getMostSignificantBits() ^ b.level().getGameTime() ^ 0x5DEECE66DL) | 1L;
        return seed;
    }

    /** The next line of an event for this fight (deterministic per fight seed). */
    String line(LuciusLines.Event e) { return LuciusLines.pick(e, seed(), spoken[e.ordinal()]++); }

    /** He speaks in chat to everyone in the arena (and the usual audience of his speech), if the voice switch is on. */
    void speak(LuciusLines.Event e) {
        if (!LuciusGodConfig.voiceLines() || !(b.level() instanceof ServerLevel sl)) return;
        broadcast(sl, Component.literal(b.bossName() + ": \"" + line(e) + "\"").withStyle(b.chatColor(), ChatFormatting.BOLD));
    }

    /** An ultimate-grade step begins: a line, but not more often than every 12 seconds. */
    void windup() {
        long now = b.level().getGameTime();
        if (now - lastWindup < 240) return;
        lastWindup = now;
        speak(LuciusLines.Event.ULTIMATE_WINDUP);
    }

    private void broadcast(ServerLevel sl, Component c) {
        for (ServerPlayer p : sl.players()) if (inArena(p) || p.distanceToSqr(b) < 64 * 64 || b.fighters().contains(p.getUUID())) p.sendSystemMessage(c);
    }

    private void title(ServerLevel sl, String title, String subtitle) {
        for (ServerPlayer p : sl.players()) {
            if (!inArena(p) && p.distanceToSqr(b) >= 64 * 64) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 50, 20));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle).withStyle(ChatFormatting.GOLD)));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(title).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)));
        }
    }

    // ================================================================ the stages
    /**
     * A fatal blow reached him. Returns true when it was turned into the next stage (the caller must not let him die); false when it is a
     * real death (the last stage, or a kill command, or the stages are off, or the refill was refused).
     */
    boolean fall(DamageSource source) {
        if (!LuciusGodConfig.threeStages() || !(b.level() instanceof ServerLevel sl) || b.level().isClientSide) return false;
        boolean real = source != null && (source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD));
        if (stage.onFatal(real) != LuciusStage.Fall.NEXT_STAGE) return false;
        int from = stage.stage();
        rescale(false);                                                 // 1.46.0: the next stage's health follows the players who are here now
        // alive again at once: a body at 0 health is taken by the game within a second; the heal is the scripted one, never a regeneration
        b.dpsBudget().bypass(() -> BossHealGuard.allow(() -> b.setHealth(1f)));
        if (!b.refillForMode(LuciusStage.refillMode(from), LuciusGodConfig.stageRefillTicks())) return false;     // spent or busy: no second wind, he dies for real
        stage.advance();
        for (net.minecraft.world.effect.MobEffectInstance debuff : new java.util.ArrayList<>(b.getActiveEffects())) {          // 1.56.0: a new stage starts clean: every harmful effect on him ends
            if (debuff.getEffect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL) b.removeEffect(debuff.getEffect());
        }
        long now = sl.getGameTime();
        graceUntil = LuciusBalance.stageGraceUntil(now, LuciusGodConfig.stageRefillTicks());          // 1.46.0: aura, EMP and starvation are off for the change and 10 s after it
        LuciusCapabilities.clearAll(sl);                                   // 1.46.0: and whatever of his is on the players comes off now
        b.interruptCast();
        b.director().onStageChange(sl, now);
        gate.reset();
        reflect.clear();
        clearTraps();
        int to = stage.stage();
        com.newuniverse.nusmp.quests.QuestHooks.bossPhase(b, "lucius", to);
        ZogratisVfx.pillar(sl, b.position(), 5, 60, FxMath.divine(100), false);
        ZogratisVfx.spaceShatter(sl, b.position().add(0, 1, 0), 6.0, FxMath.divine(100));
        ZogratisVfx.halo(sl, b, 1.8, 100, FxMath.divine(100));
        LuciusFx.sound(sl, b.position(), SoundEvents.BELL_RESONATE, 3f, 0.6f);
        LuciusFx.sound(sl, b.position(), SoundEvents.BEACON_ACTIVATE, 2f, 0.7f);
        b.announce("Stage " + (to - 1) + " is over, and Lucius is whole again: " + LuciusStage.label(to) + ", " + LuciusStage.arsenal(to) + ". He cannot be hurt for "
                + LuciusGodConfig.stageRefillTicks() / 20 + " seconds.");
        if (LuciusGodConfig.voiceLines()) {
            title(sl, LuciusLines.stageTitle(to), LuciusLines.stageSubtitle(to));
            speak(to == 2 ? LuciusLines.Event.STAGE_2 : LuciusLines.Event.STAGE_3);
        }
        b.arcRefreshBar();
        return true;
    }

    // ================================================================ damage in
    /** A blow of a player under Spatial Disorientation: absorbed, half of it goes back to its owner. True if it was absorbed. */
    boolean reflectBlow(DamageSource src, float raw, long now) {
        if (!LuciusGodConfig.spatialSwap() || !(src.getEntity() instanceof ServerPlayer p) || !reflect.reflecting(p.getUUID(), now)) return false;
        float back = CounterRules.swapMirror(raw, p.getMaxHealth());
        if (back > 0f) p.hurt(b.damageSources().indirectMagic(b, b), (float) (back * LuciusBalance.damageFactor(LuciusGodConfig.difficulty())));
        p.displayClientMessage(Component.literal("Spatial Disorientation: your own attack comes back at you.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
        if (b.level() instanceof ServerLevel sl) {
            ZogratisVfx.spaceShatter(sl, b.position().add(0, 1, 0), 2.4, GodSteps.ICE);
            LuciusFx.sound(sl, b.position(), SoundEvents.AMETHYST_BLOCK_BREAK, 1.4f, 0.6f);
        }
        return true;
    }

    /** The hit cap bit: say so to the attacker now and then, and answer (teleport, or a counter, alternately; never while busy; every 3 seconds at most). */
    void reactToClamp(Entity attacker, ServerLevel sl, long now) {
        if (attacker instanceof ServerPlayer p && now - lastClampNote >= 40) {
            lastClampNote = now;
            p.displayClientMessage(Component.literal("A single blow can take at most " + trimPct(LuciusGodConfig.hitCapPct()) + "% of his health.").withStyle(ChatFormatting.GOLD), true);
        }
        HitGate.Reaction r = gate.react(now, busy(now));
        if (r == null) return;
        LivingEntity foe = attacker instanceof LivingEntity le && ArcSteps.fair(le) ? le : null;
        if (r == HitGate.Reaction.COUNTER && foe != null && b.director().startGodChain(LuciusChains.HIT_COUNTER, foe, now, false)) return;
        Vec3 from = attacker != null ? attacker.position() : b.position().add(b.getViewVector(1f));
        Vec3 spot = ArcSteps.retreatSpot(b, from);
        if (spot == null) return;
        if (b.arcAct() == 1) ArcSteps.retreatFire(b, spot);
        else b.blinkTo(spot);
    }

    private static String trimPct(double v) { return v == Math.rint(v) ? Long.toString(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.1f", v); }

    // ================================================================ damage out
    /** Does a magic hit on this victim become a share of its maximum health? */
    private boolean trueApplies(LivingEntity victim, boolean magic) {
        return magic && LuciusGodConfig.trueDamage() && victim instanceof ServerPlayer p && ArcSteps.fair(p);
    }

    /**
     * Every hit of his goes through here (from {@link LuciusBoss#strike}): a magic hit on a player is true damage, a share of the
     * player's maximum health by the tier of the move; anything else keeps the old law. A critical waiting from a Time Stasis Trap is
     * used by whichever hit comes first.
     */
    boolean strike(LivingEntity victim, float base, boolean magic, com.newuniverse.nusmp.boss.LuciusProfile.Strike normal) {
        if (trueApplies(victim, magic)) return landTrue(victim, TrueDamage.tierOfBase(base));
        boolean crit = victim instanceof ServerPlayer sp && TimeStasisTrap.take(sp, b.level().getGameTime());
        float scaledBase = (float) (base * damageScale());                 // 1.46.0: the difficulty and the players
        return normal.hit(victim, crit ? scaledBase * TrueDamage.CRIT_FACTOR : scaledBase, magic);
    }

    /**
     * Percent-of-maximum true damage on a victim: the tier's share of its maximum health (a guaranteed critical x1.5 if a trap marked
     * it), never more than 40% in one hit, delivered as magic damage so armour is ignored, and under the shared 30%-per-second pressure
     * cap where the damage arrives. A player also loses the same share, scaled, of the magicule pool. Returns whether it landed.
     */
    boolean landTrue(LivingEntity victim, TrueDamage.Tier tier) {
        if (victim == null || !victim.isAlive() || ZogratisEntities.isFamily(victim)) return false;
        long now = b.level().getGameTime();
        if (victim instanceof ServerPlayer gp && !graceCd.use(gp.getUUID(), now, LuciusBalance.HIT_GRACE_TICKS)) return false;     // 1.46.0: half a second of grace after a true hit
        boolean crit = victim instanceof ServerPlayer sp && TimeStasisTrap.take(sp, now);
        float amount = TrueDamage.amount(tier, victim.getMaxHealth(), LuciusGodConfig.trueDamageScale() * damageScale(), crit);
        if (amount <= 0f) return false;
        boolean ok = victim.hurt(b.damageSources().indirectMagic(b, b), amount);
        if (ok && victim instanceof ServerPlayer sp) {
            double f = TrueDamage.poolFraction(tier, LuciusGodConfig.magiculeDrainFactor());
            if (f > 0.0) LuciusCapabilities.drainMagicule(sp, f);
            if (crit) sp.displayClientMessage(Component.literal("A critical blow: the trap had marked you.").withStyle(ChatFormatting.RED), true);
        }
        return ok;
    }

    // ================================================================ what the players do
    private boolean mayAnswer(ServerPlayer p, long now) {
        return b.fightActive() && b.isAlive() && ArcSteps.fair(p) && inArena(p) && !b.julius() && !b.transforming() && !b.refilling()
                && !b.director().channeling() && !b.director().staggered(now);
    }

    /**
     * The Checkmate counter: a player in range is channelling something massive (see {@link CounterRules}). He drops what he is doing
     * (a running chain is abandoned, a cast is cut), starts the chain {@code checkmate_counter} on them (a blink behind them, shown for 12
     * ticks, then a strike shown for 14) and says so. Once per target every 8 seconds. Returns whether it started.
     */
    boolean requestCheckmate(ServerPlayer p) {
        long now = b.level().getGameTime();
        if (!LuciusGodConfig.checkmateCounter() || !mayAnswer(p, now) || p.distanceToSqr(b) > 48 * 48 || !checkmateCd.ready(p.getUUID(), now)) return false;
        if (!b.director().startGodChain(LuciusChains.CHECKMATE_COUNTER, p, now, true)) return false;
        checkmateCd.use(p.getUUID(), now, LuciusBalance.cooldown("checkmate_counter", CounterRules.CHECKMATE_COOLDOWN, LuciusGodConfig.difficulty()));
        speak(LuciusLines.Event.CHECKMATE_COUNTER);
        p.displayClientMessage(Component.literal("Lucius saw the channel. Move.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        return true;
    }

    /**
     * Spatial Disorientation: the player launched a massive attack; he trades places with them (both spots must be safe floor, never
     * the void or lava), and for two seconds their blows on him are absorbed and half of each returns to them. Once per player every 12 seconds.
     */
    boolean trySwap(ServerPlayer p) {
        if (!(b.level() instanceof ServerLevel sl)) return false;
        long now = sl.getGameTime();
        if (!LuciusGodConfig.spatialSwap() || !mayAnswer(p, now) || b.director().chainRunning() || !swapCd.ready(p.getUUID(), now)) return false;
        Vec3 toPlayer = LuciusFx.standable(sl, p, b.position()), toBoss = LuciusFx.standable(sl, b, p.position());
        if (toPlayer == null || toBoss == null) return false;
        swapCd.use(p.getUUID(), now, LuciusBalance.cooldown("spatial_swap", CounterRules.SWAP_COOLDOWN, LuciusGodConfig.difficulty()));
        Vec3 pa = p.position(), ba = b.position();
        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, pa.add(0, 1, 0), pa.add(0, 1.5, 0), 0xFF000000 | GodSteps.ICE, 14, 0.9f);
        VfxSpawn.send(sl, VfxShape.SPATIAL_RIFT, ba.add(0, 1, 0), ba.add(0, 1.5, 0), 0xFF000000 | GodSteps.ICE, 14, 0.9f);
        b.teleportTo(toBoss.x, toBoss.y, toBoss.z);
        b.getNavigation().stop();
        p.teleportTo(toPlayer.x, toPlayer.y, toPlayer.z);
        p.fallDistance = 0;
        ArcSteps.faceTowards(b, p);
        reflect.open(p.getUUID(), now);
        LuciusFx.sound(sl, ba, SoundEvents.ENDERMAN_TELEPORT, 1.6f, 0.5f);
        p.displayClientMessage(Component.literal("Spatial Disorientation: he took your place. Your attack will come back at you.").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD), true);
        return true;
    }

    /**
     * Ultimate Skill Reversal: an Ultimate-grade skill was activated in the arena. The activation is cancelled by the caller; here he
     * shows a 12-tick reflection shell and the caster takes a share of their own maximum health as true damage (the damage the skill
     * would have done is not known: the activation is cancelled before it is worked out). Once per 10 seconds; false (and the skill
     * goes through) if he is busy or it is on cooldown.
     */
    boolean reverseUltimate(ServerPlayer p, ManasSkillInstance skill) {
        if (!(b.level() instanceof ServerLevel sl)) return false;
        long now = sl.getGameTime();
        if (!LuciusGodConfig.ultimateReversal() || !mayAnswer(p, now) || !reversalCd.ready(Boolean.TRUE, now)) return false;
        reversalCd.use(Boolean.TRUE, now, LuciusBalance.cooldown("ultimate_reversal", CounterRules.REVERSAL_COOLDOWN, LuciusGodConfig.difficulty()));
        Vec3 at = b.position();
        ZogratisVfx.divineSigil(sl, at, 3.4, 12, GodSteps.ICE, false);
        ZogratisVfx.spaceShatter(sl, at.add(0, 1, 0), 3.0, GodSteps.ICE);
        VfxSpawn.send(sl, VfxShape.TIME_REWIND, at.add(0, 0.05, 0), at.add(0, 1, 0), 0, 12, 1.6f);
        LuciusFx.sound(sl, at, SoundEvents.BELL_RESONATE, 2.5f, 1.2f);
        float dmg = CounterRules.mirrorDamage(p.getMaxHealth(), LuciusGodConfig.mirrorPct() * damageScale());
        if (dmg > 0f) p.hurt(b.damageSources().indirectMagic(b, b), dmg);
        String name = skill == null || skill.getSkill() == null ? "skill" : skill.getSkill().getName().getString();
        p.displayClientMessage(Component.literal("Ultimate Skill Reversal: " + name + " is turned back on you.").withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        speak(LuciusLines.Event.ULTIMATE_REVERSAL);
        return true;
    }

    /** A player dodged three times in six seconds: a Time Stasis Trap under their feet, unless one already lies there. */
    void layTrap(ServerPlayer p) {
        if (!(b.level() instanceof ServerLevel sl) || !LuciusGodConfig.stasisTraps() || !b.fightActive() || !ArcSteps.fair(p) || !inArena(p)) return;
        long now = sl.getGameTime();
        if (traps.near(p.getX(), p.getZ(), now, 2.5)) return;
        Vec3 at = ZogratisBoss.groundOrNull(sl, p.position());
        if (at == null) at = p.position();
        if (TimeStasisTrap.lay(b, traps, sl, at, LuciusGodConfig.trapTicks()))
            p.displayClientMessage(Component.literal("Dodge spam: Lucius lays a Time Stasis Trap where you stand. Do not be in it in one second.").withStyle(ChatFormatting.GOLD), true);
    }

    /** One look at a player against the traps (from the player tick, every other tick). */
    void checkTraps(ServerPlayer p, long now) {
        if (traps.liveCount(now) == 0) return;
        TimeStasisTrap.check(b, traps, p, now);
    }

    void gemShattered() { speak(LuciusLines.Event.GEM_SHATTER); }

    // ================================================================ save data
    void save(CompoundTag tag) {
        CompoundTag t = new CompoundTag();
        t.putInt("Stage", stage.stage());
        t.putLongArray("Gate", gate.save());
        t.putLong("Seed", seed);
        t.putIntArray("Spoken", spoken);
        tag.put("LGod", t);
    }

    void load(CompoundTag tag) {
        if (!tag.contains("LGod")) return;
        CompoundTag t = tag.getCompound("LGod");
        stage.restore(Math.max(1, t.getInt("Stage")));
        gate.restore(t.getLongArray("Gate"));
        seed = t.getLong("Seed");
        int[] s = t.getIntArray("Spoken");
        for (int i = 0; i < spoken.length && i < s.length; i++) spoken[i] = Math.max(0, s[i]);
    }
}
