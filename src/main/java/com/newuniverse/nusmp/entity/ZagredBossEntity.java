package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.GrimoireSummon;
import com.newuniverse.nusmp.book.KotodamaWords;
import com.newuniverse.nusmp.book.KotodamaWords.Source;
import com.newuniverse.nusmp.book.KotodamaWords.Word;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.book.UnderworldMatter;
import com.newuniverse.nusmp.sound.NUSounds;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 0.47: Zagred, the devil of Kotodama, as a 4-phase boss. Off by default (server config zagredBossEnabled); ops start it with
 * {@code /multiverse boss zagred}. Beating it is the future way to earn Kotodama (config kotodamaBossReward, also off).
 * <ol>
 *   <li><b>Phase 1 (100-75%):</b> the Word Soul: "Halt" paralyses and jams everyone around it; "Shatter" breaks whatever is
 *       fired at it back into magicules.</li>
 *   <li><b>Phase 2 (75-50%):</b> elemental adaptation: every 10 s it adapts to the element that hurt it most and takes only a
 *       fifth from it, so the party has to keep changing elements. Void lances join the words.</li>
 *   <li><b>Phase 3 (50-25%):</b> void flooding: underworld matter floods the arena from its centre again and again, and demon
 *       sword storms fall on its target.</li>
 *   <li><b>Phase 4 (25-0%):</b> the true Word Soul: it takes only 15% of any damage unless the hit is anti-magic, or it has just
 *       been struck by three different elements within 5 s (then it is "exposed" for 5 s and takes full damage). It speaks "Heal"
 *       once, and hurls its otherworldly trident.</li>
 * </ol>
 * A purple boss bar shows the phase; the arena's matter is put back when it dies or is removed.
 */
public class ZagredBossEntity extends Monster {
    /** 0.48: what it is doing (synced with ZagredStatePayload for the model's pose and the target reticle). */
    public static final int STATE_IDLE = 0, STATE_STALK = 1, STATE_COMBAT = 2, STATE_CASTING = 3;
    private static final EntityDataAccessor<Integer> PHASE = SynchedEntityData.defineId(ZagredBossEntity.class, EntityDataSerializers.INT);
    public static final int VIOLET = KotodamaWords.VIOLET;

    private final ServerBossEvent bar = new ServerBossEvent(Component.literal("Zagred"), BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Map<String, Float> elementDamage = new HashMap<>();
    private final Map<String, Long> lastElementHit = new HashMap<>();
    private final Set<UUID> fighters = new HashSet<>();
    private BlockPos arena;
    private String adapted = "";
    private long exposedUntil, nextHalt = 100, nextShatter, nextLance = 160, nextFlood, nextSwords = 200, nextAdapt = 200;
    private boolean healed;
    // 0.52: the Multilayer Barrier, Thought Acceleration, the acts, the daemons and the signature words (docs/zagred_boss_gdd.md)
    final ZagredDefense.Barrier barrier = new ZagredDefense.Barrier();
    final ZagredDefense.Reflex reflex = new ZagredDefense.Reflex();
    private boolean defenseReady;
    long reflexInvulnUntil, transitionUntil, staggerUntil, ruleUntil, lastWarn;
    private int archSpawned, archCount;
    private Word lastWord;
    private final Map<UUID, Float> barrierDamage = new HashMap<>();
    List<Vec3> redactPlan;
    ZagredAttacks.Rule ruleKind, pendingRule, lastRule;
    final List<UUID> stones = new ArrayList<>();
    int stoneKills;
    final Map<UUID, int[]> ruleViolation = new HashMap<>();
    // 0.48 utility AI: a word is chosen by scoring the situation, telegraphed (reticle + casting pose) for CAST_TICKS, then spoken
    static final int CAST_TICKS = 15;
    private final Map<Word, Long> wordReady = new java.util.EnumMap<>(Word.class);
    private int state = STATE_IDLE, stateWord = -1, stateTarget = -1;
    private Word pending;
    private long castAt, nextThink;
    private long nextVoiceLine;
    private float recentDamage;
    // 0.49: Tensura's own magic (TensuraCaster), learned on its first tick, cast between words
    private List<net.minecraft.resources.ResourceLocation> tensuraKit;
    private long nextTensura = 120;
    private int clientState = STATE_IDLE, clientWord = -1, clientTarget = -1;

    /** Client copies, set by ZagredStatePayload. */
    public int clientState() { return clientState; }
    public int clientWord() { return clientWord; }
    public int clientTarget() { return clientTarget; }

    public void applyState(int state, int word, int target) { clientState = state; clientWord = word; clientTarget = target; }

    public ZagredBossEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 500;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600).add(Attributes.ATTACK_DAMAGE, 14).add(Attributes.ATTACK_KNOCKBACK, 1.2)
                .add(Attributes.MOVEMENT_SPEED, 0.28).add(Attributes.FOLLOW_RANGE, 48).add(Attributes.ARMOR, 12).add(Attributes.ARMOR_TOUGHNESS, 6)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE, 1);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    /** 1..4 by health. */
    public int phase() { return entityData.get(PHASE); }

    /** Where the arena is (its centre): where it was summoned. */
    public void setArena(BlockPos p) { arena = p.immutable(); }

    @Override public boolean removeWhenFarAway(double d) { return false; }
    @Override protected boolean shouldDespawnInPeaceful() { return false; }

    // ---------------------------------------------------------------- boss bar
    @Override
    public void startSeenByPlayer(ServerPlayer p) {
        super.startSeenByPlayer(p);
        bar.addPlayer(p);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new ZagredStatePayload(getId(), state, stateWord, stateTarget, phase()));
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer p) { super.stopSeenByPlayer(p); bar.removePlayer(p); }

    // ---------------------------------------------------------------- the fight
    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel sl)) return;
        if (arena == null) arena = blockPosition();
        long t = tickCount;
        float frac = getHealth() / getMaxHealth();
        int ph = frac > 0.75f ? 1 : frac > 0.5f ? 2 : frac > 0.25f ? 3 : 4;
        if (!defenseReady) {                                                    // first tick: the barrier and the reflex for this act
            defenseReady = true;
            barrier.set(ZagredDefense.layersFor(ph), getMaxHealth(), t);
            reflex.set(ZagredDefense.tokensFor(ph));
            applySpeed(ph);
        }
        if (ph != phase()) enterPhase(sl, ph);
        bar.setProgress(frac);
        bar.setName(barName(ph, t));
        if (t % 60 == 0) VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, this, position(), VIOLET, 70, 1.6f);
        recentDamage *= 0.97f;
        defenseTick(sl, t, ph);
        if (tensuraKit == null) {
            tensuraKit = TensuraCaster.learn(this, 5, "dark", "death", "shadow", "black", "hell", "curse", "gravity", "dimension", "spatial",
                    "flare", "lightning", "bullet", "spear", "blade", "arrow");
            TensuraCaster.ensureMana(this, 2_000_000);
        }
        if (t % 200 == 0) TensuraCaster.ensureMana(this, 2_000_000);
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) {
            setState(sl, fighters.isEmpty() ? STATE_IDLE : STATE_STALK, null, null);
            pending = null; castAt = 0;
            return;
        }
        if (t < transitionUntil || t < staggerUntil || barrier.reforming(t)) {        // no attack while he changes act, is staggered or rewrites the barrier
            getNavigation().stop();
            setState(sl, STATE_CASTING, null, target);
            return;
        }
        if (ph >= 3 && t % 40 == 0) retarget(sl, ph);
        if (pending != null) {                                                   // a telegraphed word lands
            if (t >= castAt) {
                KotodamaWords.speak(this, pending, Source.BOSS);
                voiceLine(sl, voiceFor(pending), wordLine(pending), 60, false);
                if (pending == Word.HEAL) healed = true;
                pending = null;
                setState(sl, STATE_COMBAT, null, target);
            }
        } else if (t >= nextThink) {
            nextThink = t + (ph <= 2 ? 10 : ph == 3 ? 8 : 6);
            Word w = choose(sl, target, ph, frac);
            if (w != null) {
                pending = w;
                castAt = t + castTicks(w, ph);                                  // Shatter answers at once
                wordReady.put(w, t + cooldown(w, ph));
                lastWord = w;
                telegraph(sl, w, ph, t);
                setState(sl, STATE_CASTING, w, target);
                getNavigation().stop();
            } else setState(sl, STATE_COMBAT, null, target);
        }
        // Tensura's own magic between words: telegraphed like a word, then cast through ManasCore
        if (pending == null && t >= nextTensura && !tensuraKit.isEmpty() && distanceToSqr(target) < 32 * 32) {
            nextTensura = t + (ph == 4 ? 100 : 160);
            setState(sl, STATE_CASTING, null, target);
            getNavigation().stop();
            SpellRuntime.later(sl, 12, () -> {
                LivingEntity tg = getTarget();
                if (!isAlive() || tg == null || !tg.isAlive()) return;
                var id = TensuraCaster.cast(this, tg, tensuraKit, 10);
                if (id != null) {
                    voiceLine(sl, NUSounds.ZAGRED_WORD.get(), randomLine(
                            "Your borrowed arts are still subject to my words.",
                            "A different spell, the same inevitable ending.",
                            "Show me a power worth remembering."), 60, false);
                    VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getEyePosition(), tg.getBoundingBox().getCenter(), VIOLET, 20, 1.2f);
                }
                setState(sl, STATE_COMBAT, null, tg);
            });
        }
        // anti-magic up close: back off and fight from range
        if (antiMagicNear(target) && distanceToSqr(target) < 36 && t % 20 == 0) {
            Vec3 away = position().subtract(target.position()).normalize().scale(8).add(position());
            getNavigation().moveTo(away.x, away.y, away.z, 1.3);
            nextLance = Math.min(nextLance, t + 20);
        }
        if (ph >= 2 && t >= nextAdapt) { adapt(sl); nextAdapt = t + 200; }
        if (ph >= 2 && t >= nextLance) {
            int n = ph == 4 ? 3 : 1;
            for (int k = 0; k < n; k++) { int kk = k; SpellRuntime.later(sl, kk * 8, () -> { if (isAlive() && getTarget() != null) lance(sl, getTarget()); }); }
            nextLance = t + (ph == 4 ? 70 : 100);
        }
        if (ph >= 3 && t >= nextFlood) {
            UnderworldMatter.start(sl, this, arena, 14 + 2 * (ph - 3), 500);
            nextFlood = t + 600;
        }
    }

    // ---------------------------------------------------------------- 0.52: defences, daemons, the acts
    /** True while he is committed (a transition, a stagger, a Redact or Overwrite channel): no dodging then. */
    boolean busy(long t) { return t < transitionUntil || t < staggerUntil || pending == Word.REDACT || pending == Word.OVERWRITE; }

    BlockPos arenaPos() { return arena; }

    /** The boss bar's name: phase, barrier layers, reflex tokens, the rule in force. */
    Component barName(int ph, long t) {
        StringBuilder s = new StringBuilder("Zagred  \u00b7  Phase ").append(ph).append("  \u00b7  Barrier ");
        if (barrier.reforming(t)) s.append("writing...");
        else for (int i = 0, n = barrier.layers(t), a = barrier.alive(t); i < n; i++) s.append(i < a ? "\u25a0" : "\u25a1");
        s.append("  \u00b7  Reflex ");
        for (int i = 0; i < reflex.max(); i++) s.append(i < reflex.tokens() ? "\u25cf" : "\u25cb");
        if (reflex.out(t)) s.append(" OUT");
        if (t < exposedUntil) s.append("  \u00b7  EXPOSED");
        else if (!adapted.isEmpty() && ph >= 2) s.append("  \u00b7  adapted: ").append(adapted);
        if (ruleKind != null && t < ruleUntil) s.append("  \u00b7  RULE: ").append(ruleKind.text);
        return Component.literal(s.toString()).withStyle(ChatFormatting.DARK_PURPLE);
    }

    void applySpeed(int ph) {
        var a = getAttribute(Attributes.MOVEMENT_SPEED);
        if (a != null) a.setBaseValue(0.28 * (ph == 3 ? 1.08 : ph == 4 ? 1.15 : 1.0));
    }

    /** Barrier, reflex, regeneration, daemon packs, the leash and the rule, all on their own clocks. */
    void defenseTick(ServerLevel sl, long t, int ph) {
        barrier.tick(t, pending == Word.OVERWRITE, archCount > 0);
        reflex.tick(this, sl, t, ph, busy(t));
        if (t % 20 == 0 && t >= barrier.lockUntil && !barrier.reforming(t)) heal(getMaxHealth() * 0.004f);   // Ultra-Speed Regeneration
        if (t % 5 == 0) ZagredAttacks.enforce(this, sl);
        if (t % 10 == 0) {
            for (MobEffectInstance e : new ArrayList<>(getActiveEffects()))                                  // Abnormal Condition Nullification
                if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) removeEffect(e.getEffect());
        }
        if (t % 20 == 0) {
            barrierDamage.replaceAll((k, v) -> v * 0.9f);
            barrierDamage.values().removeIf(v -> v < 0.5f);
            leash(sl, t);
        }
        if (t % 40 == 0) upkeepDaemons(sl, ph, false);
    }

    Vec3 arenaCenter() { return arena == null ? position() : Vec3.atBottomCenterOf(arena); }

    /** Keeps the packs of daemons topped up for this act and the number of fighters. */
    void upkeepDaemons(ServerLevel sl, int ph, boolean force) {
        Vec3 c = arenaCenter();
        int n = Math.max(1, sl.getPlayers(p -> p.distanceToSqr(c) < 40 * 40 && !p.isCreative() && !p.isSpectator()).size());
        double scale = Math.min(1.5, 0.6 + 0.4 * n);
        int lesser = 0, greater = 0, arch = 0;
        for (GrimoireDaemonEntity d : GrimoireDaemonEntity.of(sl, getUUID(), c, 80)) {
            switch (d.tier()) { case GrimoireDaemonEntity.LESSER -> lesser++; case GrimoireDaemonEntity.GREATER -> greater++; case GrimoireDaemonEntity.ARCH -> arch++; default -> { } }
        }
        archCount = arch;
        int lesserCap = (int) Math.round((ph == 1 ? 4 : ph == 2 ? 6 : ph == 3 ? 8 : 12) * scale), greaterCap = ph == 3 ? 2 : ph == 4 ? 4 : 0;
        if (lesser < lesserCap / 2 || (tickCount % 300 < 40 && lesser < lesserCap))
            for (int i = 0; i < Math.min(3, lesserCap - lesser); i++) GrimoireDaemonEntity.spawn(sl, this, GrimoireDaemonEntity.LESSER, ringPos(sl, 8 + getRandom().nextDouble() * 5, 2.5));
        if (greater < greaterCap && (force || tickCount % 400 < 40))
            for (int i = 0; i < Math.min(2, greaterCap - greater); i++) GrimoireDaemonEntity.spawn(sl, this, GrimoireDaemonEntity.GREATER, ringPos(sl, 11, 0.8));
    }

    Vec3 ringPos(ServerLevel sl, double r, double up) {
        Vec3 c = arenaCenter();
        double a = getRandom().nextDouble() * Math.PI * 2;
        return ZagredAttacks.ground(sl, c.add(Math.cos(a) * r, 0, Math.sin(a) * r), c.y).add(0, up, 0);
    }

    /** A 40-block leash: someone who runs far from the arena is called back. */
    void leash(ServerLevel sl, long t) {
        Vec3 c = arenaCenter();
        for (ServerPlayer p : sl.players()) {
            double d2 = p.distanceToSqr(c);
            if (d2 < 40 * 40 || d2 > 120 * 120 || p.isCreative() || p.isSpectator()) continue;
            if (t < p.getPersistentData().getLong("nusmp_zagred_leash")) continue;
            p.getPersistentData().putLong("nusmp_zagred_leash", t + 100);
            Vec3 to = ZagredAttacks.ground(sl, c.add(p.position().subtract(c).multiply(1, 0, 1).normalize().scale(12)), c.y);
            say(sl, "Zagred: \"Return.\"");
            p.teleportTo(to.x, to.y, to.z);
        }
    }

    /** Act II goes for the strongest caster, Act III for whoever hurt the barrier most lately. */
    void retarget(ServerLevel sl, int ph) {
        LivingEntity best = null;
        double top = -1;
        if (ph == 3) {
            for (LivingEntity f : KotodamaWords.foes(this, position(), 32)) {
                if (!(f instanceof Player)) continue;
                double ep = com.newuniverse.nusmp.antimagic.Nullification.maxEP(f);
                if (ep > top) { top = ep; best = f; }
            }
        } else {
            for (var e : barrierDamage.entrySet()) {
                Player p = sl.getPlayerByUUID(e.getKey());
                if (p != null && p.isAlive() && !p.isCreative() && e.getValue() > top) { top = e.getValue(); best = p; }
            }
        }
        if (best != null && best != getTarget()) setTarget(best);
    }

    /** Wind-up tells that are not part of the model: the Halt ring, the Redact sentence, the Overwrite stones. */
    void telegraph(ServerLevel sl, Word w, int ph, long t) {
        switch (w) {
            case HALT -> {
                ZagredAttacks.ring(sl, position(), 12, 3, 5);
                wordReady.merge(Word.REDACT, t + 80, Math::max);
            }
            case REDACT -> {
                redactPlan = ZagredAttacks.plan(this, 5 + (ph >= 3 ? 2 : 0) + (ph == 4 ? 1 : 0));
                ZagredAttacks.telegraph(this, redactPlan);
                wordReady.merge(Word.HALT, t + 80, Math::max);
            }
            case OVERWRITE -> ZagredAttacks.overwriteStart(this, sl);
            default -> { }
        }
    }

    /** The sentence a Redact written during the wind-up (null if a player is speaking it). */
    public List<Vec3> takePlan() {
        List<Vec3> p = redactPlan;
        redactPlan = null;
        return p;
    }

    /** Wind-up ticks: 15 for most words, longer for the heavy ones, shorter in later acts (never below 8). */
    static int castTicks(Word w, int ph) {
        if (w == Word.SHATTER) return 3;
        int base = switch (w) { case REDACT -> 20; case FALL -> 18; case OVERWRITE -> 40; default -> CAST_TICKS; };
        float mult = ph <= 2 ? 1f : ph == 3 ? 0.8f : 0.67f;
        return Math.max(w == Word.OVERWRITE ? 30 : 8, Math.round(base * mult));
    }

    /** Last Word's third mark: the outermost barrier layer breaks. */
    public void breakBarrierLayer() {
        if (barrier.breakOne(tickCount) && level() instanceof ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getBoundingBox().getCenter(), position(), 0xFFFFFFFF, 16, 1.6f);
            sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 0.8f);
        }
    }

    /** An Arch Daemon fell: its anchor is gone and his outermost barrier layer breaks. */
    void archFell() {
        archCount = Math.max(0, archCount - 1);
        if (barrier.breakOne(tickCount) && level() instanceof ServerLevel sl) {
            say(sl, "An Arch Daemon is struck down: a layer of Zagred's barrier breaks.");
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getBoundingBox().getCenter(), position(), 0xFFFFFFFF, 20, 2f);
        }
    }

    /** A rule stone fell; the third one cuts the sentence short and staggers him for 2 s. */
    void stoneBroken() {
        if (++stoneKills < 3 || !(level() instanceof ServerLevel sl)) return;
        if (pending == Word.OVERWRITE) { pending = null; castAt = 0; }
        if (ruleKind != null) ZagredAttacks.endRule(this, sl);
        for (UUID id : new ArrayList<>(stones)) if (sl.getEntity(id) instanceof GrimoireDaemonEntity s) s.discard();
        stones.clear();
        staggerUntil = tickCount + 40;
        say(sl, "The sentence is cut. Zagred reels.");
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getBoundingBox().getCenter(), position(), 0xFFFFFFFF, 30, 3f);
    }

    @Override
    public boolean canBeAffected(MobEffectInstance effect) {                    // Abnormal Condition Nullification: no harmful effect ever lands
        return effect.getEffect().value().getCategory() != MobEffectCategory.HARMFUL && super.canBeAffected(effect);
    }

    /** Sends the state to the clients when it changes. */
    void setState(ServerLevel sl, int s, Word w, LivingEntity target) {
        int word = w == null ? -1 : w.ordinal(), tid = target == null ? -1 : target.getId();
        if (s == state && word == stateWord && tid == stateTarget) return;
        state = s; stateWord = word; stateTarget = tid;
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntity(this, new ZagredStatePayload(getId(), s, word, tid, phase()));
    }

    /** Ticks before a word can be chosen again. */
    static int cooldown(Word w, int ph) {
        int base = switch (w) {
            case SHATTER -> 100; case HALT -> 260; case SWORDS -> 220; case FALL -> 300; case REVEAL -> 400; case SEAL -> 400;
            case REJECT -> 400; case FEAR -> 500; case DRAIN -> 500; case SLEEP -> 600; case BANISH -> 600; case REVERSE -> 600;
            case PETRIFY -> 800; case REDACT -> 160; case OVERWRITE -> 900; default -> 100000;
        };
        return Math.round(base * (ph <= 2 ? 1f : ph == 3 ? 0.85f : 0.7f));
    }

    boolean ready(Word w) { return tickCount >= wordReady.getOrDefault(w, 0L); }

    /**
     * Target analysis: every word is scored for the situation (who is flying, hidden, buffed, summoning, casting Ultimates,
     * clustered, firing at it, hurting it fast) and the best one above the bar is spoken. Phases unlock more words.
     */
    Word choose(ServerLevel sl, LivingEntity target, int ph, float frac) {
        List<LivingEntity> foes = KotodamaWords.foes(this, position(), 24);
        if (foes.isEmpty()) return null;
        Map<Word, Float> score = new java.util.EnumMap<>(Word.class);
        int close = 0, flying = 0, hidden = 0, buffs = 0, summons = 0, casters = 0;
        for (LivingEntity f : foes) {
            if (f.distanceToSqr(this) < 12 * 12) close++;
            if (!f.onGround() && !f.isInWater() && (f instanceof Player pl && (pl.getAbilities().flying || pl.isFallFlying()) || f.getY() - getY() > 4)) flying++;
            if (f.isInvisible()) hidden++;
            for (var e : f.getActiveEffects()) if (e.getEffect().value().isBeneficial()) buffs++;
            try { if (io.github.manasmods.tensura.storage.ep.ExistenceStorage.isSummon(f)) summons++; } catch (Throwable ignored) {}
            if (f instanceof Player && com.newuniverse.nusmp.antimagic.Nullification.maxEP(f) > 100_000) casters++;
        }
        boolean shots = !sl.getEntitiesOfClass(Projectile.class, getBoundingBox().inflate(10), pr -> pr.getOwner() != this).isEmpty();
        if (shots) score.put(Word.SHATTER, 9f);
        if (ph == 4 && !healed && frac < 0.1f) score.put(Word.HEAL, 10f);
        if (hidden > 0) score.put(Word.REVEAL, 6f + hidden);
        if (flying > 0) score.put(Word.FALL, 5f + flying);
        if (close >= 2) score.put(Word.HALT, 3f + close);
        else if (distanceToSqr(target) < 36) score.put(Word.HALT, 2.6f);
        if (casters > 0) score.put(Word.SEAL, 4f + casters);
        if (buffs >= 2) score.put(Word.REJECT, 2f + Math.min(4, buffs * 0.5f));
        if (summons > 0) score.put(Word.BANISH, 5f + summons);
        if (recentDamage > getMaxHealth() * 0.12f) score.put(Word.REVERSE, 5.5f);
        if (ph >= 2) {
            if (close >= 2) score.put(Word.FEAR, 3f + close * 0.5f);
            if (frac < 0.5f) score.put(Word.DRAIN, 3.2f);
            if (close >= 3) score.put(Word.SLEEP, 4.5f);
        }
        score.put(Word.REDACT, (flying > 0 ? 3.4f : 4.6f) + (close >= 2 ? 1.2f : 0f));
        if (ph >= 3 && ruleKind == null && stones.isEmpty()) score.put(Word.OVERWRITE, 8f);
        if (ph >= 3) {
            score.put(Word.SWORDS, 3f);
            if (distanceToSqr(target) < 25) score.put(Word.PETRIFY, 3.4f);
        }
        Word best = null;
        float top = 2.5f;
        for (var e : score.entrySet()) {
            if (!ready(e.getKey()) || e.getKey() == lastWord) continue;
            float s = e.getValue() + getRandom().nextFloat() * 0.6f;
            if (s > top) { top = s; best = e.getKey(); }
        }
        return best;
    }

    /** Is 't' fighting with anti-magic (a demon sword in hand, the Lord, a summoned Anti-Magic grimoire)? */
    boolean antiMagicNear(LivingEntity t) { return t instanceof Player p && antiMagicHeld(p); }

    /** A demon sword in hand, or the Anti-Magic Lord. */
    static boolean antiMagicHeld(Player p) {
        return BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath().startsWith("demon_") || AntiMagic.lord(p).isPresent();
    }

    /** Client: black flakes and smoke shed from the shoulders and wings (as in the reference art). */
    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide || tickCount % 2 != 0) return;
        var r = getRandom();
        float yawRad = yBodyRot * net.minecraft.util.Mth.DEG_TO_RAD;
        double bx = getX() + Math.sin(yawRad) * 0.6, bz = getZ() - Math.cos(yawRad) * 0.6;
        for (int k = 0; k < 2; k++)                                             // black flakes shed off the shoulders and wings
            level().addParticle(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.03f, 0.03f, 0.035f), 2.2f),
                    bx + (r.nextDouble() - 0.5) * 3.6, getY() + 2.2 + r.nextDouble() * 1.4, bz + (r.nextDouble() - 0.5) * 3.6, 0, 0.015, 0);
        if (r.nextInt(3) == 0) level().addParticle(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                bx + (r.nextDouble() - 0.5) * 2, getY() + 2.0 + r.nextDouble() * 1.6, bz + (r.nextDouble() - 0.5) * 2, 0, 0.01, 0);
        if (clientState == STATE_CASTING && r.nextInt(2) == 0) level().addParticle(net.minecraft.core.particles.ParticleTypes.WITCH,
                getX() + (r.nextDouble() - 0.5), getY() + 2.4, getZ() + (r.nextDouble() - 0.5), 0, 0.05, 0);
    }

    void enterPhase(ServerLevel sl, int ph) {
        entityData.set(PHASE, ph);
        String line = switch (ph) {
            case 2 -> randomLine("Your magic... I have heard it before.", "Did you think that element could surprise me?", "I learn every weakness you reveal.");
            case 3 -> randomLine("Sink beneath the world you know.", "Let the underworld rise around you.", "The ground itself answers my words.");
            case 4 -> randomLine("Words are the only law.", "Now, even the laws of this world are mine.", "You will need more than courage to break my word.");
            default -> "You have earned my attention.";
        };
        voiceLine(sl, NUSounds.ZAGRED_PHASE.get(), line, 0, true);
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getBoundingBox().getCenter(), position(), VIOLET, 40, 3f);
        sl.playSound(null, blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 2f, 0.6f);
        if (ph >= 3) nextFlood = tickCount;
        barrier.set(ZagredDefense.layersFor(ph), getMaxHealth(), tickCount);
        reflex.set(ZagredDefense.tokensFor(ph));
        applySpeed(ph);
        if (ph >= 3) {                                                   // the act changes: 3 s of calm, an Arch Daemon, the greater turrets
            transitionUntil = tickCount + 60;
            pending = null; castAt = 0;
            wordReady.put(Word.OVERWRITE, tickCount + 400L);
            if (archSpawned < ph - 2) {
                archSpawned = ph - 2;
                GrimoireDaemonEntity.spawn(sl, this, GrimoireDaemonEntity.ARCH, ringPos(sl, 8, 2.2));
                archCount++;
            }
            upkeepDaemons(sl, ph, true);
            say(sl, ph == 3 ? "An Arch Daemon answers the call." : "A second Arch Daemon answers the call.");
        }
    }

    void say(ServerLevel sl, String line) {
        Component c = Component.literal(line).withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.BOLD);
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 64 * 64) p.sendSystemMessage(c);
    }

    private static SoundEvent voiceFor(Word word) {
        if (word == null) return NUSounds.ZAGRED_WORD.get();
        return switch (word) {
            case HALT, SEAL, SLEEP, FEAR, PETRIFY -> NUSounds.ZAGRED_SEAL.get();
            case FALL, SLUDGE -> NUSounds.ZAGRED_FALL.get();
            case SHATTER, TRIDENT, SWORDS, REJECT, BANISH -> NUSounds.ZAGRED_SHATTER.get();
            case HEAL -> NUSounds.ZAGRED_HEAL.get();
            case REDACT, OVERWRITE -> NUSounds.ZAGRED_OVERWRITE.get();
            default -> NUSounds.ZAGRED_WORD.get();
        };
    }

    private String wordLine(Word word) {
        return switch (word) {
            case HALT -> randomLine("Halt. Your body has heard me.", "Be still and witness your defeat.", "Not one step further.");
            case SHATTER -> randomLine("Break apart and return to nothing.", "Your defense ends with this word.", "All things yield when I name their end.");
            case HEAL -> randomLine("A minor correction. I will not fall here.", "I refuse the ending you chose for me.", "Even my wounds obey my words.");
            case SLUDGE -> randomLine("Let the underworld swallow you.", "Sink into the black tide.", "There is no ground beneath you now.");
            case TRIDENT -> randomLine("Pierce through every defense.", "A word made into a blade.", "Run from this, if you can.");
            case SWORDS -> randomLine("A thousand edges answer me.", "Be carved from this world.", "My blades will find you all.");
            case SEAL -> randomLine("Your power ends here.", "I close the path before you.", "Silence. Your spell is finished.");
            case REJECT -> randomLine("Denied.", "Your magic has no place here.", "I reject your defiance.");
            case FALL -> randomLine("Fall.", "The earth calls you down.", "Even the sky will not save you.");
            case REVEAL -> randomLine("There you are.", "No hiding from the word soul.", "I see through every illusion.");
            case SLEEP -> randomLine("Close your eyes.", "Rest now; the fight is over.", "Sleep beneath my command.");
            case PETRIFY -> randomLine("Become stone.", "Let your defiance harden into silence.", "You will stand there forever.");
            case FEAR -> randomLine("Tremble.", "Look upon me and despair.", "Your courage is only another word I can erase.");
            case BANISH -> randomLine("Begone from my sight.", "I cast you out.", "Leave this place.");
            case REVERSE -> randomLine("Your attack returns to its source.", "Turn against the hand that sent you.", "What you give, I send back.");
            case DRAIN -> randomLine("Your strength belongs to me now.", "I will take what little power you have.", "Wither beneath my gaze.");
            case REDACT -> randomLine("I erase your advantage.", "That power no longer exists.", "Let the page forget you.");
            case OVERWRITE -> randomLine("I rewrite the law of this world.", "Your rules end where my words begin.", "The world will obey my version.");
        };
    }

    private String randomLine(String... lines) {
        return lines[getRandom().nextInt(lines.length)];
    }

    private void voiceLine(ServerLevel sl, SoundEvent sound, String line, long cooldown, boolean force) {
        if (voice(sl, sound, cooldown, force)) say(sl, "Zagred: \"" + line + "\"");
    }

    private boolean voice(ServerLevel sl, SoundEvent sound, long cooldown, boolean force) {
        if (!force && tickCount < nextVoiceLine) return false;
        nextVoiceLine = tickCount + cooldown;
        sl.playSound(null, this, sound, SoundSource.HOSTILE, 2.4f, 0.96f + getRandom().nextFloat() * 0.08f);
        return true;
    }

    /** A void lance: a black-violet spear thrown along a line, striking everything on it a moment later. */
    void lance(ServerLevel sl, LivingEntity target) {
        Vec3 from = getEyePosition(), to = target.getBoundingBox().getCenter(), dir = to.subtract(from).normalize(), end = from.add(dir.scale(28));
        voiceLine(sl, NUSounds.ZAGRED_SHATTER.get(), randomLine(
                "A lance through the heart of your defense.",
                "I have already chosen where this ends.",
                "Run. The word will still find you."), 60, false);
        VfxSpawn.send(sl, VfxShape.KOTO_TRIDENT, from, end, VIOLET, 14, 1.2f);
        sl.playSound(null, blockPosition(), SoundEvents.TRIDENT_THROW.value(), SoundSource.HOSTILE, 1.5f, 0.5f);
        SpellRuntime.later(sl, 5, () -> {
            for (LivingEntity e : KotodamaWords.foes(this, from.add(dir.scale(14)), 15)) {
                Vec3 c = e.getBoundingBox().getCenter();
                double s = Math.max(0, Math.min(28, c.subtract(from).dot(dir)));
                if (from.add(dir.scale(s)).distanceTo(c) > 1.2 + e.getBbWidth() / 2) continue;
                KotodamaWords.hurt(this, e, 12f);
                KotodamaWords.spirit(this, e, 2);
            }
        });
    }

    /** Phase 2+: adapt to the element that has hurt it most since the last adaptation. */
    void adapt(ServerLevel sl) {
        String best = "";
        float most = 0;
        for (var e : elementDamage.entrySet()) if (e.getValue() > most && !e.getKey().equals("physical")) { most = e.getValue(); best = e.getKey(); }
        elementDamage.clear();
        if (best.isEmpty() || best.equals(adapted)) return;
        adapted = best;
        voiceLine(sl, NUSounds.ZAGRED_WORD.get(), "I have learned your " + best + " magic. It will not save you again.", 100, false);
        VfxSpawn.sendFollowing(sl, VfxShape.KOTO_AURA, this, position(), 0xFFC8A0FF, 30, 2f);
    }

    /** The element of a hit (from its damage type), for adaptation and the phase-4 exposure. */
    static String element(DamageSource s) {
        String path = s.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
        if (path.contains("lightning")) return "lightning";
        if (path.contains("fire") || path.contains("lava") || s.is(DamageTypeTags.IS_FIRE)) return "fire";
        if (path.contains("water") || path.contains("drown")) return "water";
        if (path.contains("wind")) return "wind";
        if (path.contains("earth")) return "earth";
        if (path.contains("ice") || path.contains("freez") || s.is(DamageTypeTags.IS_FREEZING)) return "ice";
        if (path.contains("light") || path.contains("holy")) return "light";
        if (path.contains("dark") || path.contains("curse")) return "dark";
        if (path.contains("space") || path.contains("spatial") || path.contains("gravity")) return "space";
        if (s.is(DamageTypeTags.IS_EXPLOSION)) return "explosion";
        if (AntiMagic.isMagic(s)) return "magic";
        if (s.is(DamageTypeTags.IS_PROJECTILE)) return "projectile";
        return "physical";
    }

    /** Anti-magic: a demon sword, the Anti-Magic Lord's blows, or anyone fighting with their Anti-Magic grimoire out. */
    static boolean antiMagic(DamageSource s) {
        if (!(s.getEntity() instanceof Player p)) return false;
        if (BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath().startsWith("demon_")) return true;
        if (AntiMagic.lord(p).isPresent() && s.getDirectEntity() == p) return true;
        return GrimoireSummon.isFloating(p, MagicType.ANTI_MAGIC)
                && GrimoirePages.grimoireOf(p).map(i -> GrimoirePages.magicOf(i) == MagicType.ANTI_MAGIC).orElse(false);
    }

    /** Order of operations (docs/zagred_boss_gdd.md 4.1): transitions, rule, nullification, reflex, resistances, adaptation, anchor, barrier, act 4. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurt(source, amount);
        long t = tickCount;
        if (t < transitionUntil || t < reflexInvulnUntil) return false;
        if (source.getEntity() instanceof ServerPlayer p) fighters.add(p.getUUID());
        String el = element(source);
        ZagredDefense.Kind kind = ZagredDefense.classify(source, el);
        boolean summoned = ZagredDefense.isSummon(source.getEntity());                // 0.53: summons can hurt him (as arcane blows) and credit their owner
        if (summoned) {
            if (kind == ZagredDefense.Kind.PHYSICAL) kind = ZagredDefense.Kind.MAGIC;
            if (source.getEntity() instanceof net.minecraft.world.entity.OwnableEntity oe && oe.getOwnerUUID() != null) fighters.add(oe.getOwnerUUID());
        }
        int ph = phase();
        if (ruleKind != null && t < ruleUntil && ruleKind.element != null && ruleKind.element.equals(el)
                && source.getEntity() instanceof LivingEntity a && a != this) ZagredAttacks.punish(this, a);   // Overwrite: the banned element burns its caster
        if (kind == ZagredDefense.Kind.PHYSICAL) {                                   // Physical Attack Nullification
            if (source.getEntity() instanceof ServerPlayer p && t - lastWarn > 100) {
                lastWarn = t;
                p.displayClientMessage(Component.literal("Your blade cannot touch Zagred. Magic can.").withStyle(ChatFormatting.DARK_PURPLE), true);
            }
            return false;
        }
        if (reflex.dodgeHit(this, source, t, busy(t), kind)) return false;          // Thought Acceleration
        recentDamage += amount;
        amount *= ZagredDefense.resistance(el);
        if (ph >= 2) {
            elementDamage.merge(el, amount, Float::sum);
            if (el.equals(adapted)) amount *= 0.35f;
        }
        if (ph == 4 && !el.equals("physical") && !el.equals("projectile") && !el.equals("magic")) {
            lastElementHit.put(el, t);
            lastElementHit.values().removeIf(at -> t - at > 100);
            if (lastElementHit.size() >= 3 && t >= exposedUntil && level() instanceof ServerLevel sl) {
                exposedUntil = t + 100;
                lastElementHit.clear();
                say(sl, "Three elements at once! Zagred's word falters: EXPOSED.");
                VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getBoundingBox().getCenter(), position(), 0xFFFFFFFF, 30, 2.5f);
            }
        }
        if (reflex.out(t)) amount *= 1.35f;                                          // out of thought
        if (archCount > 0) amount *= 0.85f;                                          // an Arch Daemon anchors him
        ZagredDefense.Grain grain = summoned && (el.equals("physical") || el.equals("projectile")) ? ZagredDefense.Grain.KINETIC : ZagredDefense.grainOf(kind, el);
        float through = barrier.absorb(amount, grain, kind == ZagredDefense.Kind.ANTI_MAGIC, t);
        if (barrier.broken > 0 && level() instanceof ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, getBoundingBox().getCenter(), position(), 0xFFFFFFFF, 14, 1.6f);
            sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5f, 0.8f);
            voiceLine(sl, NUSounds.ZAGRED_SHATTER.get(), randomLine(
                    "You have broken one layer. There are more.",
                    "That was only the outer shell.",
                    "A little progress. Do not mistake it for victory."), 60, false);
            if (barrier.reformStarted) say(sl, "Zagred: \"Again.\"");
        }
        if (through <= 0) {                                                          // the layers took all of it
            if (source.getEntity() instanceof ServerPlayer p) barrierDamage.merge(p.getUUID(), amount, Float::sum);
            if (level() instanceof ServerLevel sl) sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.HOSTILE, 1f, 1.2f);
            return false;
        }
        amount = through;
        if (ph == 4 && !antiMagic(source) && t >= exposedUntil) amount *= 0.3f;
        return super.hurt(source, clampToThreshold(amount));
    }

    /** A single burst can't carry him past the next act: it stops just under the threshold. */
    float clampToThreshold(float amount) {
        int ph = phase();
        float thr = ph == 1 ? 0.75f : ph == 2 ? 0.5f : ph == 3 ? 0.25f : 0f;
        if (thr <= 0f) return amount;
        float floor = thr * getMaxHealth();
        return getHealth() - amount < floor ? Math.max(0f, getHealth() - floor + 0.5f) : amount;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (!(level() instanceof ServerLevel sl)) return;
        UnderworldMatter.endAll(this);
        voiceLine(sl, NUSounds.ZAGRED_PHASE.get(), randomLine(
                "So... words can be broken.",
                "This silence... cannot be...",
                "You have written an ending I did not foresee."), 0, true);
        for (UUID id : fighters) {
            ServerPlayer p = sl.getServer().getPlayerList().getPlayer(id);
            if (p != null) { KotodamaWords.reward(p); com.newuniverse.nusmp.item.BossRelics.give(p); }
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!level().isClientSide) UnderworldMatter.endAll(this);
        super.remove(reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (arena != null) tag.put("Arena", NbtUtils.writeBlockPos(arena));
        tag.putBoolean("Healed", healed);
        tag.putString("Adapted", adapted);
        tag.putInt("ArchSpawned", archSpawned);
        tag.putInt("Tokens", reflex.tokens());
        barrier.save(tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        NbtUtils.readBlockPos(tag, "Arena").ifPresent(p -> arena = p);
        healed = tag.getBoolean("Healed");
        adapted = tag.getString("Adapted");
        archSpawned = tag.getInt("ArchSpawned");
        if (tag.contains("BCount")) { barrier.load(tag); defenseReady = true; reflex.set(Math.max(1, tag.getInt("Tokens"))); }
        if (hasCustomName()) bar.setName(getDisplayName());
    }

    /** Summons the boss at 'at' with its arena there (the command). */
    public static ZagredBossEntity summon(ServerLevel sl, Vec3 at) {
        ZagredBossEntity z = NUEntities.ZAGRED.get().create(sl);
        if (z == null) return null;
        z.moveTo(at.x, at.y, at.z, 0, 0);
        z.setArena(BlockPos.containing(at));
        sl.addFreshEntity(z);
        VfxSpawn.send(sl, VfxShape.KOTO_SLUDGE, at.add(0, 0.05, 0), at, VIOLET, 60, 3f);
        sl.playSound(null, z.blockPosition(), SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3f, 0.5f);
        return z;
    }
}
