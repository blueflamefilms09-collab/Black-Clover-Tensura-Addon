package com.newuniverse.nusmp.arc.kakashi;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.boss.rules.BossHealGuard;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the Kakashi Hatake boss (docs/bosses/kakashi.md): the most powerful canon Kakashi (the Copy Ninja with the Mangekyo Sharingan and a Perfect Susanoo) as an arc boss. He stands on the
 * arcs' framework ({@link ArcBoss}: telegraphed moves, the per-second damage budget, the boss bar, the arena and its leash, the story credit) and adds, all server side and all bounded:
 * <ul>
 * <li><b>Four phases</b> ({@link KakashiMath#phaseOf}, at 75 / 50 / 25 percent): the Copy Ninja, the Pack, the Mangekyo, the Perfect Susanoo. A phase opens new moves ({@link KakashiMoves}); nothing
 *     is ever taken back. Each change is a visible power-up of 30 ticks in which he starts nothing and can be hit.</li>
 * <li><b>Sharingan copy</b>: a bounded ledger of the hardest hit each player landed on him ({@link KakashiMath.Ledger}, 8 players); the move throws a weaker copy of it back.</li>
 * <li><b>Kawarimi</b>: one single hit of 6 percent of his health or more is swapped for a log, once per 13 s.</li>
 * <li><b>Kamui</b>: from the Mangekyo phase he phases through a blow, only while he is not attacking ({@link KakashiMath.Kamui}), so there is always an opening.</li>
 * <li><b>Perfect Susanoo</b>: a shield of 7 percent of his health that breaks into a 2.5 s opening ({@link KakashiMath#absorb}).</li>
 * <li><b>Conductivity</b>: a foe made wet by the Water Dragon takes 35 percent more from his lightning.</li>
 * </ul>
 * He spawns NO entity (clones, hounds, the wolf of lightning and the Susanoo are effects and timers), changes NO block, and one hit on a player takes at most {@code maxHitPercent} of their health.
 * Cost per tick: one comparison per module; while a Lightning Beast is out one small box query per tick (at most 44 ticks); a move queries foes once or a few times (at most
 * {@link KakashiMath#MAX_FOES} foes each). No world scan, no player list walk, no chunk load.
 */
public class KakashiBoss extends ArcBoss {
    public static final String ID = "kakashi_hatake";

    /** Who he is (the roster record every system of the arcs reads). Tier 5, never a floor boss (no {@code floors(...)}): he is summoned by command, egg or arena. */
    public static final ArcCharacter CHARACTER = ArcCharacter.boss(ID, "Kakashi Hatake")
            .title("the Copy Ninja").source("Naruto").saga(ArcSaga.ALLIES).tier(5).colors(0xC8D0E8, 0xFF3030)
            .stats(28000, 36, 0.36, 16)
            .lines("Maa, maa. I came to read in peace, but it seems you are not going to allow that.", "Space bends where I look. Try to stand on the right side of it.",
                    "Well. I read the whole book and you wrote the last page. Good ending.")
            .build();

    /** Synced flags the client draws from (the pose, the glow). */
    public static final int F_INTANGIBLE = 1, F_SUSANOO = 2, F_BROKEN = 4, F_EYE = 8, F_RAIKIRI = 16, F_MIST = 32,
            F_SIXPATHS = 64, F_GATES = 128, F_STRAIN = 256;      // appended by the canon kit pass: the Six Paths flight, the opened gates, the bleeding eye

    private static final EntityDataAccessor<Integer> PHASE_IDX = SynchedEntityData.defineId(KakashiBoss.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FLAGS = SynchedEntityData.defineId(KakashiBoss.class, EntityDataSerializers.INT);

    private static final java.util.Set<KakashiBoss> LIVE = ConcurrentHashMap.newKeySet();
    private static final ResourceLocation HEALTH_ID = ResourceLocation.fromNamespaceAndPath("anime_multiverse", "kakashi_boss/health");

    /** The most wet foes remembered at once. */
    private static final int WET_MAX = 12;
    /** The last ticks of a telegraph in which the aimed spot no longer follows the target. */
    static final int FREEZE = 10;

    private List<Move> book;
    final KakashiMath.Kamui kamui = new KakashiMath.Kamui();
    final KakashiMath.Ledger ledger = new KakashiMath.Ledger();
    final KakashiLines.Gate voice = new KakashiLines.Gate();
    private final Map<UUID, Long> wet = new LinkedHashMap<>();
    private boolean modsApplied;
    private int lastCast = -1;
    private long staggerUntil, kawarimiReadyAt, intangibleUntil, susanooUntil, brokenUntil, nextSlash, earthWallUntil, mistUntil, susanooReadyAt;
    private float susanooShield;
    // ---- aim locks and move state (one move at a time, so one set of fields)
    Vec3 lock;
    List<Vec3> marks = new ArrayList<>();
    double lockAngle;
    private Vec3 fangPos;
    private double fangHeading;
    private int fangLeft;
    private UUID fangTarget;
    // ---- the vortex
    private Vec3 vortexAt;
    private int vortexLeft;
    // ---- the canon kit pass (KakashiMath: charges, strain, elements, the Six Paths, the clones of lightning)
    final KakashiMath.Charges charges = new KakashiMath.Charges();
    final KakashiMath.Strain strain = new KakashiMath.Strain();
    final KakashiMath.Elements elements = new KakashiMath.Elements();
    private long fightStart, exhaustedUntil, cutterUntil, gatesUntil, sixPathsUntil, sixPhaseReadyAt, lcloneUntil;
    private boolean sixPathsUsed;
    private double hoverBase;
    private final List<Vec3> lclones = new ArrayList<>();

    public KakashiBoss(EntityType<? extends KakashiBoss> type, Level level) {
        super(type, level);
        this.xpReward = 5000;
        if (!level.isClientSide) LIVE.add(this);
    }

    public static AttributeSupplier.Builder attributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), 12.0);
    }

    // ================================================================ the live set (cap and cleanup)
    public static int liveCount() {
        prune();
        return LIVE.size();
    }

    private static void prune() {
        LIVE.removeIf(b -> b.isRemoved() || (b.tickCount == 0 && b.level() instanceof ServerLevel s && s.getEntity(b.getId()) != b));
    }

    public static List<KakashiBoss> live() {
        prune();
        return List.copyOf(LIVE);
    }

    public static void clearLive() { LIVE.clear(); }

    @Override public ArcCharacter character() { return CHARACTER; }

    @Override
    protected List<Move> moves() {
        if (book == null) book = KakashiMoves.build();
        return book;
    }

    // ================================================================ synced state
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(PHASE_IDX, 0);
        b.define(FLAGS, 0);
    }

    /** 0..3: the Copy Ninja, the Pack, the Mangekyo, the Perfect Susanoo. */
    public int phaseIdx() { return Math.max(0, Math.min(KakashiMath.PHASES - 1, entityData.get(PHASE_IDX))); }

    public int flags() { return entityData.get(FLAGS); }

    public boolean flag(int f) { return (flags() & f) != 0; }

    private void setFlag(int f, boolean on) {
        int now = entityData.get(FLAGS), want = on ? now | f : now & ~f;
        if (want != now) entityData.set(FLAGS, want);
    }

    /** The shield of the Susanoo as a share of his maximum health (the bar shows it). */
    public float shieldShare() { return susanooShield <= 0f ? 0f : Math.min(1f, susanooShield / Math.max(1f, getMaxHealth())); }

    public boolean susanooUp() { return susanooUntil > tickCount && susanooShield > 0f; }

    public boolean mistUp() { return mistUntil > tickCount; }

    public boolean wallUp() { return earthWallUntil > tickCount; }

    // ================================================================ framework settings
    @Override protected int openingPower() { return PowerCurve.FULL; }

    @Override protected int gatesForm() { return FORM_BASE; }

    @Override protected boolean refillOnGates() { return false; }

    @Override public String barTitle() { return bossName() + " - " + KakashiMath.PHASE_NAMES[phaseIdx()]; }

    @Override protected double preferredRange(double suggested) { return Math.min(suggested, 8.0); }

    /** The framework multiplies by its power tuning: divide it out so the damage is base x the setting. */
    @Override
    protected float damageScale() { return (float) (KakashiConfig.damageMultiplier() / Math.max(0.1, tuning().damageMultiplier())); }

    /** The base of his blows: his melee attribute. */
    public float base() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE); }

    // ================================================================ the tick
    @Override
    protected void bossTick() {
        super.bossTick();
        if (level().isClientSide) return;
        LIVE.add(this);
        if (!(level() instanceof ServerLevel sl) || isDeadOrDying()) return;
        if (!modsApplied) applyHealth();
        long now = tickCount;
        if (fightActive()) {
            int want = KakashiMath.phaseOf(getHealth() / Math.max(1f, getMaxHealth()), phaseIdx());
            if (want > phaseIdx()) enterPhase(sl, want);
            noteCast();
            if (fangLeft > 0) tickFang(sl);
            if (vortexLeft > 0) tickVortex(sl);
            if (susanooUntil != 0L) tickSusanoo(sl, now);
            if (KakashiConfig.canonKit()) canonTick(sl, now);
        }
        setFlag(F_SIXPATHS, sixPathsUntil > now);
        setFlag(F_GATES, gatesUntil > now);
        setFlag(F_STRAIN, exhaustedUntil > now);
        setFlag(F_INTANGIBLE, kamui.intangible(now) || intangibleUntil > now);
        setFlag(F_EYE, phaseIdx() >= KakashiMath.MANGEKYO);
        setFlag(F_SUSANOO, susanooUp());
        setFlag(F_BROKEN, brokenUntil > now);
        setFlag(F_MIST, mistUntil > now);
        if (mistUntil > now && now % 20 == 0) tickMist(sl);
        if (now % 40 == 0) {
            wet.values().removeIf(t -> t < now);
            if (phaseIdx() >= KakashiMath.MANGEKYO && !kamui.intangible(now) && now >= kamui.readyAt() && now % 80 == 0) KakashiFx.puff(sl, KakashiFx.KAMUI, position().add(0, getBbHeight() * 0.8, 0), 2, 0.3);   // the open eye: a hint that the phase is ready
        }
        if (mistUntil != 0L && now >= mistUntil) mistUntil = 0L;
        if (earthWallUntil != 0L && now >= earthWallUntil) {
            earthWallUntil = 0L;
            KakashiFx.burst(sl, KakashiFx.EARTH, position().add(0, 1, 0), 10, 1.2);
        }
    }

    /** Notices a move that has just started (voice, the solid window of Kamui). */
    private void noteCast() {
        int c = state() == STATE_CASTING ? castingMove() : -1;
        if (c == lastCast) return;
        lastCast = c;
        if (c < 0) return;
        Move m = castingMoveOrNull();
        if (m == null) return;
        kamui.moveStarted(tickCount, m.windUpTicks());
        speak(KakashiMoves.voiceGroup(m.id()), false);
        if (!KakashiConfig.canonKit() || !(level() instanceof ServerLevel sl)) return;
        if (KakashiMath.CUTTER.equals(m.id())) cutterUntil = tickCount + m.windUpTicks() + KakashiMath.CUTTER_COUNTER_TICKS;     // his hand cuts any lightning that reaches him
        if (sixPathsUntil > tickCount) return;                                                                                   // the Six Paths chakra costs no strain and no charge
        charges.spend(m.id());
        if (strain.add(KakashiMath.Strain.cost(m.id()))) exhaust(sl);
    }

    // ================================================================ the canon kit pass
    /** Per-tick work of the pass: the strain of the eye, the clones of lightning, the gates and the Six Paths. One comparison each while idle. */
    private void canonTick(ServerLevel sl, long now) {
        if (sixPathsUntil == 0L && now % KakashiMath.Strain.PASSIVE_EVERY == 0 && strain.add(1)) exhaust(sl);
        if (!lclones.isEmpty()) tickLClones(sl, now);
        if (gatesUntil != 0L) {
            if (now >= gatesUntil) {
                gatesUntil = 0L;
                KakashiFx.puff(sl, KakashiFx.GATES, position().add(0, 1, 0), 8, 1.0);
            } else if (now % 5 == 0) {
                KakashiFx.puff(sl, KakashiFx.GATES, position().add(0, getBbHeight() * 0.5, 0), 5, 0.6);
            }
        }
        if (sixPathsUntil != 0L) tickSixPaths(sl, now);
        if (exhaustedUntil != 0L && now >= exhaustedUntil) exhaustedUntil = 0L;
    }

    /** The eye bleeds: he is spent for a moment (open to every blow, no Kamui) and says so. */
    private void exhaust(ServerLevel sl) {
        long now = tickCount;
        exhaustedUntil = now + KakashiMath.Strain.EXHAUST_TICKS;
        brokenUntil = Math.max(brokenUntil, exhaustedUntil);
        staggerUntil = Math.max(staggerUntil, now + 50);
        KakashiFx.burst(sl, KakashiFx.COPY, position().add(0, getBbHeight() * 0.85, 0), 10, 0.4);
        KakashiFx.dark(sl, KakashiFx.COPY, position().add(0, getBbHeight() * 0.85, 0), 8, 0.3);
        sl.playSound(null, blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.HOSTILE, 2f, 0.5f);
        speak("strain", true);
    }

    boolean exhausted() { return exhaustedUntil > tickCount; }

    boolean sixPathsActive() { return sixPathsUntil > tickCount; }

    /** Plants a clone of lightning (at most {@link KakashiMath#LCLONE_MAX} stand; the oldest goes). It breaks when a foe comes close and paralyses them. */
    void plantLClone(Vec3 at) {
        while (lclones.size() >= KakashiMath.LCLONE_MAX) lclones.remove(0);
        lclones.add(at);
        lcloneUntil = tickCount + KakashiMath.LCLONE_TICKS;
    }

    private void tickLClones(ServerLevel sl, long now) {
        if (now >= lcloneUntil) {
            for (Vec3 c : lclones) KakashiFx.puff(sl, KakashiFx.CLONE, c.add(0, 1, 0), 4, 0.4);
            lclones.clear();
            return;
        }
        if (now % 4 != 0) return;
        for (int i = lclones.size() - 1; i >= 0; i--) {
            Vec3 c = lclones.get(i);
            KakashiFx.puff(sl, KakashiFx.CLONE, c.add(0, 1, 0), 2, 0.3);
            KakashiFx.puff(sl, KakashiFx.RAIKIRI, c.add(0, 1.1, 0), 2, 0.5);                  // it crackles: the lightning shows through the clone
            LivingEntity touched = null;
            for (LivingEntity f : foesAround(c, KakashiMath.LCLONE_TOUCH)) {
                touched = f;
                break;
            }
            if (touched == null) continue;
            lclones.remove(i);
            KakashiFx.chidori(sl, c.add(0, 1.1, 0), 1.6, 4, now + i);
            KakashiFx.ring(sl, KakashiFx.RAIKIRI, c, 2.2, 12);
            sl.playSound(null, BlockPos.containing(c), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.4f, 1.9f);
            for (LivingEntity f : foesAround(c, KakashiMath.LCLONE_TOUCH + 0.8)) {
                lightning(f, 0.9f);
                f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, KakashiMath.LCLONE_PARALYSE, 6, true, false));       // paralysed: cannot walk for a moment
                f.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, KakashiMath.LCLONE_PARALYSE, 2, true, false));
            }
            speak("lclone_break", false);
        }
    }

    /** The Eight Gates opened: faster and 25 percent stronger for 10 s, at the price of strain. */
    void openGates() {
        gatesUntil = tickCount + KakashiMath.GATES_TICKS;
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, KakashiMath.GATES_TICKS, 1, true, false));
        if (level() instanceof ServerLevel sl) {
            KakashiFx.burst(sl, KakashiFx.GATES, position().add(0, 1, 0), 18, 1.0);
            KakashiFx.ring(sl, KakashiFx.GATES, position(), 4.0, 16);
            KakashiFx.shape(sl, VfxShape.SPIRIT_AURA, position(), position().add(0, 1, 0), KakashiFx.RGB_GATES, KakashiMath.GATES_TICKS, 2.0);
            sl.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 2f, 1.5f);
        }
    }

    /** The Six Paths awakening (once per fight): Susanoo flight, a bigger shield, frequent short Kamui windows, no strain; afterwards he is spent. */
    void startSixPaths() {
        if (sixPathsUntil > tickCount || !(level() instanceof ServerLevel sl)) return;
        sixPathsUsed = true;
        sixPathsUntil = tickCount + KakashiMath.SIX_PATHS_TICKS;
        hoverBase = getY();
        setNoGravity(true);
        charges.refill();
        strain.reset();
        raiseSusanoo();
        susanooShield *= 1.6f;
        susanooUntil = sixPathsUntil;
        Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
        KakashiFx.burst(sl, KakashiFx.CORE, c, 20, 1.4);
        KakashiFx.ring(sl, KakashiFx.SUSANOO, position(), 8.0, 24);
        KakashiFx.shape(sl, VfxShape.LIGHTNING_FIEND, c, c.add(0, 1, 0), KakashiFx.RGB_CORE, KakashiMath.SIX_PATHS_TICKS, 2.4);
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3f, 0.5f);
    }

    private void tickSixPaths(ServerLevel sl, long now) {
        if (now >= sixPathsUntil) {
            endSixPaths(sl);
            return;
        }
        double want = hoverBase + KakashiMath.SIX_PATHS_HOVER;
        Vec3 dm = getDeltaMovement();
        setDeltaMovement(dm.x, Math.max(-0.4, Math.min(0.4, (want - getY()) * 0.15)), dm.z);       // the flight of the Perfect Susanoo
        if (now % 8 == 0) KakashiFx.chidori(sl, position().add(0, getBbHeight() * 0.6, 0), 1.8, 3, now);
    }

    private void endSixPaths(ServerLevel sl) {
        sixPathsUntil = 0L;
        setNoGravity(false);
        long now = tickCount;
        brokenUntil = Math.max(brokenUntil, now + KakashiMath.SIX_PATHS_SPENT);
        staggerUntil = Math.max(staggerUntil, now + 60);
        KakashiFx.burst(sl, KakashiFx.SUSANOO, position().add(0, 1, 0), 14, 1.4);
        speak("sixpaths_end", true);
    }

    /** One short Kamui window per 2 s while the Six Paths chakra is with him (the dimension overlaps his body). */
    private boolean sixPathsPhase(long now) {
        if (sixPathsUntil <= now || now < sixPhaseReadyAt) return false;
        sixPhaseReadyAt = now + 40;
        intangibleUntil = Math.max(intangibleUntil, now + 10);
        return true;
    }

    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (staggerUntil > tickCount || fangLeft > 0 || vortexLeft > 0) return -1;
        boolean canon = KakashiConfig.canonKit();
        long since = tickCount - fightStart;
        float share = getHealth() / Math.max(1f, getMaxHealth());
        List<Move> sub = new ArrayList<>(bk.size());
        for (Move m : bk) {
            if (!canon && KakashiMath.BOOK.indexOf(m.id()) >= KakashiMath.CLASSIC_MOVES) continue;
            if (canon) {
                if (!charges.canUse(m.id()) && sixPathsUntil <= tickCount) continue;                                      // the canon limits: 4 Lightning Cutters, 3 Kamui per phase
                if (KakashiMath.SIX_PATHS.equals(m.id()) && !(KakashiConfig.susanoo() && KakashiMath.sixPathsReady(share, sixPathsUsed) && sixPathsUntil <= tickCount)) continue;
                if (sixPathsUntil > tickCount && KakashiMath.SIX_PATHS.equals(m.id())) continue;
                if (since < KakashiMath.OBSERVE_TICKS && !KakashiMath.observeMove(m.id())) continue;                      // observe first: clones, probes and replacement only
            }
            if (KakashiMath.openingPhase(m.id()) > phaseIdx()) continue;
            if (KakashiMoves.COPY.equals(m.id()) && (!KakashiConfig.copy() || ledger.strongest(tickCount) == null)) continue;
            if (KakashiMoves.SUSANOO.equals(m.id()) && (!KakashiConfig.susanoo() || susanooUp() || tickCount < susanooReadyAt)) continue;
            if (KakashiMoves.MIST.equals(m.id()) && mistUp()) continue;
            if (KakashiMoves.WALL.equals(m.id()) && wallUp()) continue;
            sub.add(m);
        }
        if (sub.isEmpty()) return -1;
        if (canon) {
            for (Move m : sub) if (KakashiMath.SIX_PATHS.equals(m.id()) && moveEligible(m, s)) return bk.indexOf(m);       // the awakening comes as soon as it is allowed
            int el = since >= KakashiMath.OBSERVE_TICKS ? elements.dominant() : -1;
            if (el >= 0 && getRandom().nextFloat() < 0.5f) {                                                              // he has watched: he now counters what the players use most
                List<Move> hit = new ArrayList<>(3);
                for (String id : KakashiMath.counters(el)) for (Move m : sub) if (m.id().equals(id) && moveEligible(m, s)) hit.add(m);
                if (!hit.isEmpty()) {
                    int ci = super.chooseMove(hit, s, t);
                    if (ci >= 0) {
                        speak("counter", false);
                        return bk.indexOf(hit.get(ci));
                    }
                }
            }
        }
        int i = super.chooseMove(sub, s, t);
        return i < 0 ? -1 : bk.indexOf(sub.get(i));
    }

    @Override
    protected void onFightStart() {
        super.onFightStart();
        fightStart = tickCount;
        sixPathsUsed = false;
        charges.refill();
        strain.reset();
        elements.clear();
        voice.reset();
        speak("intro", true);
    }

    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        resetStates();
        if (won) {
            speak("defeat", true);
        } else {
            entityData.set(PHASE_IDX, 0);                                   // an abandoned fight: back to the first phase at full health
            if (level() instanceof ServerLevel) {
                dpsBudget().bypass(() -> BossHealGuard.allow(() -> setHealth(getMaxHealth())));
            }
            ledger.clear();
            wet.clear();
            if (!isDeadOrDying()) speak("victory", true);
        }
    }

    private void resetStates() {
        fangLeft = 0;
        vortexLeft = 0;
        susanooUntil = 0L;
        susanooShield = 0f;
        brokenUntil = 0L;
        mistUntil = 0L;
        earthWallUntil = 0L;
        intangibleUntil = 0L;
        lock = null;
        marks.clear();
        kamui.reset();
        lclones.clear();
        lcloneUntil = cutterUntil = gatesUntil = sixPathsUntil = exhaustedUntil = sixPhaseReadyAt = 0L;
        setNoGravity(false);                                               // the Six Paths flight never outlives the fight
        entityData.set(FLAGS, 0);
    }

    @Override
    public void remove(RemovalReason reason) {
        LIVE.remove(this);
        super.remove(reason);
    }

    /** A player left, died or changed dimension: he forgets everything that names them. */
    public void forget(UUID id) {
        fighters().remove(id);
        ledger.forget(id);
        wet.remove(id);
        if (id.equals(fangTarget)) fangTarget = null;
    }

    private void applyHealth() {
        modsApplied = true;
        AttributeInstance max = getAttribute(Attributes.MAX_HEALTH);
        if (max == null) return;
        double want = KakashiConfig.healthMultiplier() - 1.0;
        AttributeModifier have = max.getModifier(HEALTH_ID);
        if (have != null && Math.abs(have.amount() - want) < 1.0e-6) return;
        if (Math.abs(want) < 1.0e-9) {
            max.removeModifier(HEALTH_ID);
            return;
        }
        float share = getHealth() / Math.max(1f, getMaxHealth());
        max.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_ID, want, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        float to = Math.min(getMaxHealth(), share * getMaxHealth());
        dpsBudget().bypass(() -> BossHealGuard.allow(() -> setHealth(to)));
    }

    // ================================================================ speech
    /** Says a line of a group (text in chat, plus the recorded sound if that line has one). Paced by the boss's own gate; {@code force} lines are always heard. */
    void speak(String group, boolean force) {
        if (!KakashiConfig.speak() || group == null || !(level() instanceof ServerLevel sl)) return;
        if (!voice.allow(group, tickCount, force)) return;
        KakashiLines.Line l = KakashiLines.pick(group, voice.nextRoll() + getId());
        if (l == null) return;
        say(l.text());
        KakashiSounds.play(sl, position().add(0, getBbHeight() * 0.8, 0), l.id());
    }

    // ================================================================ phases
    private void enterPhase(ServerLevel sl, int to) {
        entityData.set(PHASE_IDX, to);
        charges.refill();                                                  // canon: chakra for 4 Lightning Cutters or 3 Kamui, per phase here
        interruptCast();
        getNavigation().stop();
        staggerUntil = tickCount + 30;
        Vec3 mid = position().add(0, getBbHeight() * 0.6, 0);
        switch (to) {
            case KakashiMath.PACK -> {
                KakashiFx.burst(sl, KakashiFx.NINKEN, mid, 14, 1.2);
                KakashiFx.ring(sl, KakashiFx.WATER, position(), 5.0, 18);
                speak("phase_pack", true);
            }
            case KakashiMath.MANGEKYO -> {
                KakashiFx.burst(sl, KakashiFx.COPY, mid.add(0, 0.3, 0), 14, 0.6);
                KakashiFx.ring(sl, KakashiFx.KAMUI, position(), 6.0, 20);
                KakashiFx.shape(sl, VfxShape.SPACE_PORTAL, position().add(0, 1, 0), position().add(0, 1, 0).add(getLookAngle()), KakashiFx.RGB_KAMUI, 40, 2.2);
                speak("phase_mangekyo", true);
            }
            case KakashiMath.SUSANOO -> {
                KakashiFx.burst(sl, KakashiFx.SUSANOO, mid, 18, 1.4);
                KakashiFx.ring(sl, KakashiFx.PURPLE, position(), 7.0, 24);
                speak("phase_susanoo", true);
            }
            default -> { }
        }
        sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.5f, 1.2f);
        for (LivingEntity f : foes(5.0)) {                                   // the shock of the power-up shoves, never hurts
            Vec3 away = f.position().subtract(position()).multiply(1, 0, 1);
            if (away.lengthSqr() > 1e-4) f.knockback(0.6, -away.x, -away.z);
        }
    }

    // ================================================================ aim locks
    /** The spot a move aims at: the target's position while more than {@link #FREEZE} ticks of the telegraph are left, then frozen (so a player who keeps moving escapes it). */
    Vec3 aim(LivingEntity target, int ticksLeft) {
        if (lock == null || ticksLeft > FREEZE) lock = target.position();
        return lock;
    }

    void clearLock() {
        lock = null;
        marks.clear();
    }

    // ================================================================ hits
    @Override
    public boolean strike(LivingEntity victim, float baseDamage, boolean magic) {
        if (victim == null || !victim.isAlive() || isFamily(victim)) return false;
        DamageSource src = magic ? damageSources().indirectMagic(this, this) : damageSources().mobAttack(this);
        float dealt = bossDamage(victim, baseDamage);
        if (gatesUntil > tickCount) dealt *= KakashiMath.GATES_DAMAGE;
        if (sixPathsUntil > tickCount) dealt *= 1.2f;
        if (victim instanceof Player) {
            double share = KakashiConfig.maxHitPercent();
            if (src.scalesWithDifficulty() && level().getDifficulty() == Difficulty.HARD) share /= 1.5;
            dealt = KakashiMath.capHit(dealt, victim.getMaxHealth(), share);
        }
        return victim.hurt(src, dealt);
    }

    /** A lightning hit: {@code mul} x his base, 35 percent more on a wet foe. */
    boolean lightning(LivingEntity victim, float mul) {
        return strike(victim, base() * mul * KakashiMath.lightningFactor(isWet(victim)), true);
    }

    void makeWet(LivingEntity e) {
        if (!(e instanceof Player)) return;
        wet.remove(e.getUUID());
        wet.put(e.getUUID(), tickCount + (long) KakashiMath.WET_TICKS);
        while (wet.size() > WET_MAX) wet.remove(wet.keySet().iterator().next());
    }

    boolean isWet(LivingEntity e) {
        Long t = wet.get(e.getUUID());
        return t != null && t >= tickCount;
    }

    /** Knocks a foe away from a point. */
    void shove(LivingEntity f, Vec3 from, double power) {
        Vec3 away = f.position().subtract(from).multiply(1, 0, 1);
        if (away.lengthSqr() > 1e-4) f.knockback(power, -away.x, -away.z);
    }

    /** Foes in front of him in a lane (a thrust, a sword): along 0..reach, across within halfWidth. At most {@link KakashiMath#MAX_FOES} are looked at. */
    List<LivingEntity> lane(Vec3 from, Vec3 dir, double reach, double halfWidth) {
        List<LivingEntity> out = new ArrayList<>();
        Vec3 d = new Vec3(dir.x, 0, dir.z);
        if (d.lengthSqr() < 1e-6) return out;
        d = d.normalize();
        int n = 0;
        for (LivingEntity f : foesAround(from, reach + 2.0)) {
            if (n++ >= KakashiMath.MAX_FOES) break;
            Vec3 r = f.position().subtract(from);
            double along = r.x * d.x + r.z * d.z, across = Math.abs(r.x * d.z - r.z * d.x);
            if (along >= -0.6 && along <= reach && across <= halfWidth + f.getBbWidth() * 0.5) out.add(f);
        }
        return out;
    }

    boolean withinLeash(Vec3 p) {
        double leash = ARENA_RADIUS - 6.0;
        return p.distanceToSqr(arenaCenter()) <= leash * leash;
    }

    /** Teleports to a spot (a blink; false when his body does not fit or the spot is outside the arena). */
    boolean warp(Vec3 dest) {
        if (dest == null || !withinLeash(dest)) return false;
        if (!(level() instanceof ServerLevel sl)) return false;
        Vec3 from = position().add(0, 1, 0);
        KakashiFx.shape(sl, VfxShape.SPACE_PORTAL, from, from.add(0, 0.5, 0), KakashiFx.RGB_KAMUI, 16, 1.2);
        if (!blinkTo(dest)) return false;
        KakashiFx.shape(sl, VfxShape.SPACE_PORTAL, dest.add(0, 1, 0), dest.add(0, 1.5, 0), KakashiFx.RGB_KAMUI, 16, 1.2);
        KakashiFx.dark(sl, KakashiFx.KAMUI, dest.add(0, 1, 0), 10, 0.5);
        return true;
    }

    /** Intangible for {@code ticks} ticks (the dash of the Kamui Raikiri). */
    void phaseFor(int ticks) { intangibleUntil = Math.max(intangibleUntil, tickCount + ticks); }

    void raikiriHand(boolean on) { setFlag(F_RAIKIRI, on); }

    // ================================================================ taking damage
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && amount > 0f) {
            long now = tickCount;
            boolean canon = KakashiConfig.canonKit();
            if (intangibleUntil > now || (!(canon && exhaustedUntil > now) && kamui.tryPhase(now, phaseIdx(), KakashiConfig.kamui())) || (canon && KakashiConfig.kamui() && sixPathsPhase(now))) {
                if (level() instanceof ServerLevel sl) {
                    KakashiFx.dark(sl, KakashiFx.KAMUI, position().add(0, getBbHeight() * 0.5, 0), 6, 0.4);
                    if (now % 7 == 0) speak("kamui_phase", false);
                }
                return false;
            }
        }
        return super.hurt(source, amount);
    }

    /** The reductions that depend on what is hit: Kawarimi, the Susanoo shield, the open Susanoo, the Earth Wall and the Mist. Also where the copy ledger learns. */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        float a = super.filterDamage(source, amount);
        if (a <= 0f || !(level() instanceof ServerLevel sl)) return a;
        long now = tickCount;
        if (source.getEntity() instanceof ServerPlayer p) {
            ledger.record(p.getUUID(), a, now);
            if (KakashiConfig.canonKit()) {                                          // he watches how he is hurt (fire, lightning, water, arrows, fists, other magic)
                boolean lightningHit = source.is(DamageTypes.LIGHTNING_BOLT);
                elements.record(KakashiMath.classify(source.getMsgId(), source.is(DamageTypeTags.IS_FIRE), lightningHit, source.is(DamageTypeTags.IS_PROJECTILE), source.getDirectEntity() == source.getEntity()));
            }
        }
        if (cutterUntil > now && KakashiMath.E_LIGHTNING == KakashiMath.classify(source.getMsgId(), false, source.is(DamageTypes.LIGHTNING_BOLT), false, false)) {
            Vec3 hand = position().add(0, getBbHeight() * 0.6, 0);                  // the Lightning Cutter: a bolt that reaches him is cut in two
            KakashiFx.chidori(sl, hand, 2.2, 5, now);
            KakashiFx.burst(sl, KakashiFx.CORE, hand, 10, 0.6);
            sl.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.2f, 2.0f);
            speak("cutter_cut", false);
            return 0f;
        }
        if (KakashiMath.kawarimi(a, getMaxHealth(), now, kawarimiReadyAt) && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            kawarimiReadyAt = now + KakashiMath.KAWARIMI_COOLDOWN;
            kawarimi(sl);
            return 0f;
        }
        if (susanooUp()) {
            float[] r = KakashiMath.absorb(susanooShield, a);
            susanooShield = r[1];
            a = r[0];
            KakashiFx.puff(sl, KakashiFx.SUSANOO, position().add(0, getBbHeight() * 0.6, 0), 4, 0.9);
            if (susanooShield <= 0f) breakSusanoo(sl);
            if (a <= 0f) return 0f;
        }
        if (brokenUntil > now) a *= KakashiMath.BROKEN_FACTOR;
        Vec3 from = source.getEntity() == null ? null : source.getEntity().position();
        boolean far = from != null && from.distanceToSqr(position()) > 25.0;
        if (wallUp() && far) a *= 0.3f;
        if (mistUp() && from != null && from.distanceToSqr(position()) > 64.0) a *= 0.7f;
        return a;
    }

    /** The substitution: a log where he stood, and he is somewhere else within the arena. */
    private void kawarimi(ServerLevel sl) {
        Vec3 here = position().add(0, 0.8, 0);
        KakashiFx.burst(sl, KakashiFx.LOG, here, 14, 0.7);
        sl.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.HOSTILE, 1.6f, 0.8f);
        double a = getRandom().nextDouble() * Math.PI * 2;
        Vec3 dest = groundOrNull(sl, position().add(Math.cos(a) * 6.0, 0, Math.sin(a) * 6.0));
        if (dest != null) warp(dest);
        speak("kawarimi", false);
    }

    // ================================================================ the Susanoo
    /** Raises the Perfect Susanoo: the shield, its time, and the first slash. */
    void raiseSusanoo() {
        susanooShield = getMaxHealth() * KakashiMath.SUSANOO_SHIELD;
        susanooUntil = tickCount + KakashiMath.SUSANOO_TICKS;
        nextSlash = tickCount + 40;
        brokenUntil = 0L;
        if (level() instanceof ServerLevel sl) {
            Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
            KakashiFx.shape(sl, VfxShape.WEAPON_CONSTRUCTS, c, c.add(0, 1, 0), KakashiFx.RGB_SUSANOO, KakashiMath.SUSANOO_TICKS, 3.0);
            KakashiFx.burst(sl, KakashiFx.SUSANOO, c, 20, 1.6);
            sl.playSound(null, blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3f, 0.6f);
        }
    }

    private void breakSusanoo(ServerLevel sl) {
        susanooShield = 0f;
        susanooUntil = 0L;
        brokenUntil = tickCount + KakashiMath.SUSANOO_BROKEN_TICKS;
        susanooReadyAt = tickCount + 500;
        Vec3 c = position().add(0, getBbHeight() * 0.5, 0);
        KakashiFx.burst(sl, KakashiFx.SUSANOO, c, 22, 1.8);
        KakashiFx.ring(sl, KakashiFx.SUSANOO, position(), 5.0, 18);
        sl.playSound(null, blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 3f, 0.5f);
        speak("susanoo_break", true);
    }

    private void tickSusanoo(ServerLevel sl, long now) {
        if (susanooUntil != 0L && now >= susanooUntil) {                    // it ran out: it fades, nothing is owed
            susanooUntil = 0L;
            susanooShield = 0f;
            susanooReadyAt = now + 400;
            KakashiFx.puff(sl, KakashiFx.SUSANOO, position().add(0, 1, 0), 8, 1.0);
            return;
        }
        if (!susanooUp()) return;
        if (now % 6 == 0) KakashiFx.puff(sl, KakashiFx.SUSANOO, position().add(0, getBbHeight() * 0.7, 0), 3, 1.1);
        if (now < nextSlash) return;
        LivingEntity t = nearestFoe(14.0);
        if (t == null) {
            nextSlash = now + 20;
            return;
        }
        nextSlash = now + KakashiMath.SUSANOO_SLASH_EVERY;
        Vec3 dir = t.position().subtract(position()).multiply(1, 0, 1);
        if (dir.lengthSqr() < 1e-6) return;
        Vec3 d = dir.normalize();
        Vec3 from = position().add(0, 1.2, 0);
        KakashiFx.line(sl, KakashiFx.SUSANOO, from, from.add(d.scale(9.0)), 12);       // the sword is shown now, and lands 12 ticks later
        KakashiFx.ring(sl, KakashiFx.SUSANOO, position().add(d.scale(4.0)), 4.5, 14);
        later(12, () -> {
            for (LivingEntity f : lane(position(), d, 9.0, 3.2)) {
                if (strike(f, base() * 1.9f, true)) shove(f, position(), 0.9);
            }
            KakashiFx.burst(sl, KakashiFx.SUSANOO, position().add(d.scale(5.0)).add(0, 1, 0), 14, 1.4);
            sl.playSound(null, blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.5f, 0.6f);
        });
    }

    // ================================================================ Hidden Mist and Earth Wall
    void raiseMist(int ticks) {
        mistUntil = tickCount + ticks;
        if (level() instanceof ServerLevel sl) {
            KakashiFx.shape(sl, VfxShape.WATER_CRADLE, position().add(0, 1, 0), position().add(0, 1.5, 0), KakashiFx.RGB_MIST, ticks, 4.0);
            KakashiFx.puff(sl, KakashiFx.MIST, position().add(0, 1, 0), 24, 3.0);
        }
    }

    private void tickMist(ServerLevel sl) {
        KakashiFx.puff(sl, KakashiFx.MIST, position().add(0, 1, 0), 14, 3.5);
        for (LivingEntity f : foes(7.0)) f.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 50, 0, true, false));
    }

    void raiseWall(int ticks, Vec3 toward) {
        earthWallUntil = tickCount + ticks;
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 d = toward.subtract(position()).multiply(1, 0, 1);
        d = d.lengthSqr() < 1e-6 ? new Vec3(1, 0, 0) : d.normalize();
        Vec3 a = position().add(d.scale(3.0)).add(-d.z * 3.5, 0, d.x * 3.5), b = position().add(d.scale(3.0)).add(d.z * 3.5, 0, -d.x * 3.5);
        KakashiFx.shape(sl, VfxShape.EARTH_RISE, a, b, KakashiFx.RGB_EARTH, 24, 3.0);
        KakashiFx.burst(sl, KakashiFx.EARTH, position().add(d.scale(3.0)).add(0, 1, 0), 16, 1.6);
        sl.playSound(null, blockPosition(), SoundEvents.GRAVEL_BREAK, SoundSource.HOSTILE, 2f, 0.6f);
    }

    // ================================================================ Lightning Beast Tracking Fang
    /** Sends the wolf of lightning from his chest at the target. Only one at a time. */
    void releaseFang(LivingEntity target) {
        if (fangLeft > 0 || target == null) return;
        fangPos = position().add(0, getBbHeight() * 0.6, 0);
        Vec3 d = target.position().subtract(position());
        fangHeading = Math.atan2(d.z, d.x);
        fangLeft = KakashiMath.FANG_TICKS;
        fangTarget = target.getUUID();
        if (level() instanceof ServerLevel sl) sl.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 1.6f, 1.6f);
    }

    private void tickFang(ServerLevel sl) {
        fangLeft--;
        LivingEntity t = fangTarget != null && sl.getEntity(fangTarget) instanceof LivingEntity le && le.isAlive() ? le : null;
        Vec3 prev = fangPos;
        double dy = 0;
        if (t != null) {
            Vec3 d = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(fangPos);
            fangHeading = KakashiMath.steer(fangHeading, Math.atan2(d.z, d.x), KakashiMath.FANG_TURN);
            dy = Math.max(-0.25, Math.min(0.25, d.y * 0.2));
        }
        fangPos = fangPos.add(Math.cos(fangHeading) * KakashiMath.FANG_SPEED, dy, Math.sin(fangHeading) * KakashiMath.FANG_SPEED);
        KakashiFx.line(sl, KakashiFx.FANG, prev, fangPos, 3);
        if (fangLeft % 3 == 0) KakashiFx.puff(sl, KakashiFx.FANG, fangPos, 4, 0.35);
        LivingEntity hit = null;
        for (LivingEntity f : foesAround(fangPos, KakashiMath.FANG_HIT + 0.6)) {
            hit = f;
            break;
        }
        boolean wall = sl.getBlockState(BlockPos.containing(fangPos)).blocksMotion();
        if (hit != null) {
            lightning(hit, 1.9f);
            shove(hit, prev, 0.8);
        }
        if (hit != null || wall || fangLeft <= 0) {
            KakashiFx.burst(sl, KakashiFx.FANG, fangPos, 12, 0.8);
            sl.playSound(null, BlockPos.containing(fangPos), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 1.8f, 1.5f);
            fangLeft = 0;
            fangTarget = null;
        }
    }

    // ================================================================ Kamui Vortex
    void startVortex(Vec3 at) {
        vortexAt = at;
        vortexLeft = KakashiMath.PULL_TICKS;
        if (level() instanceof ServerLevel sl) {
            KakashiFx.shape(sl, VfxShape.SPACE_PORTAL, at.add(0, 1.2, 0), at.add(0, 2.0, 0), KakashiFx.RGB_KAMUI, KakashiMath.PULL_TICKS + 6, 4.0);
            sl.playSound(null, BlockPos.containing(at), SoundEvents.PORTAL_TRAVEL, SoundSource.HOSTILE, 1.2f, 0.5f);
        }
    }

    private void tickVortex(ServerLevel sl) {
        vortexLeft--;
        if (vortexAt == null) {
            vortexLeft = 0;
            return;
        }
        if (vortexLeft % 2 == 0) {
            KakashiFx.dark(sl, KakashiFx.KAMUI, vortexAt.add(0, 1.2, 0), 10, 1.8);
            int n = 0;
            for (LivingEntity f : foesAround(vortexAt, 14.0)) {
                if (n++ >= KakashiMath.MAX_FOES) break;
                Vec3 to = vortexAt.subtract(f.position());
                if (to.lengthSqr() < 0.04) continue;
                f.setDeltaMovement(f.getDeltaMovement().add(to.normalize().scale(KakashiMath.PULL * 2.0)));
                f.hurtMarked = true;
            }
        }
        if (vortexLeft <= 0) {
            for (LivingEntity f : foesAround(vortexAt, 3.4)) strike(f, base() * 2.2f, true);
            KakashiFx.burst(sl, KakashiFx.KAMUI, vortexAt.add(0, 1, 0), 20, 1.6);
            KakashiFx.ring(sl, KakashiFx.KAMUI, vortexAt, 5.0, 20);
            sl.playSound(null, BlockPos.containing(vortexAt), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 3f, 0.4f);
            vortexAt = null;
        }
    }

    // ================================================================ small helpers
    /** The nearest foe within a radius, or null. */
    LivingEntity nearestFoe(double radius) {
        LivingEntity best = null;
        double bd = Double.MAX_VALUE;
        int n = 0;
        for (LivingEntity f : foes(radius)) {
            if (n++ >= KakashiMath.MAX_FOES) break;
            double d = f.distanceToSqr(this);
            if (d < bd) {
                bd = d;
                best = f;
            }
        }
        return best;
    }

    /** The player whose hit he remembers best, if he is still near. */
    ServerPlayer copyTarget() {
        UUID id = ledger.strongest(tickCount);
        if (id == null || !(level() instanceof ServerLevel sl)) return null;
        return sl.getServer().getPlayerList().getPlayer(id) instanceof ServerPlayer p && p.isAlive() && p.level() == level() && p.distanceToSqr(this) < 48.0 * 48.0 ? p : null;
    }

    /** Called by a foe's death: he says a line (paced). */
    public void noteKill() { speak("kill", false); }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("KakashiPhase", phaseIdx());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(PHASE_IDX, Math.max(0, Math.min(KakashiMath.PHASES - 1, tag.getInt("KakashiPhase"))));
        modsApplied = false;
    }

    // ================================================================ the drop
    /** Magic Stones and gold coins as the other arc bosses give, and two Naruto-themed relics the mod already has (skipped if one is not registered). */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        give("anime_multiverse", "magic_stone", 12 + getRandom().nextInt(13));
        give("tensura", "gold_coin", 2 + getRandom().nextInt(3));
        give("anime_multiverse", "shinobi_smoke_pellet_pouch", 1);                 // his two Naruto relics already in the mod (no new item was made)
        if (getRandom().nextFloat() < 0.5f) give("anime_multiverse", "chunin_exam_scroll_case_charm", 1);
    }

    private void give(String ns, String path, int n) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ns, path));
        if (item != null && item != Items.AIR) spawnAtLocation(new ItemStack(item, n));
    }

    // ================================================================ test seams (package-private, never used by the game)
    void forcePhaseForTest(int phase) { entityData.set(PHASE_IDX, Math.max(0, Math.min(KakashiMath.PHASES - 1, phase))); }

    void clearForTest() { resetStates(); }

    boolean fangActive() { return fangLeft > 0; }

    boolean vortexActive() { return vortexLeft > 0; }

    void staggerForTest(int ticks) { staggerUntil = tickCount + ticks; }

    boolean staggeredForTest() { return staggerUntil > tickCount; }

    /** A Raikiri dash that found nothing leaves him wide open for {@code ticks} (the brief: the Lightning Cutter staggers him on a miss); never shortens an existing stagger. */
    void missStagger(int ticks) {
        if (ticks <= 0 || !KakashiConfig.missStagger()) return;
        long now = tickCount;
        staggerUntil = Math.max(staggerUntil, now + ticks);
        brokenUntil = Math.max(brokenUntil, now + ticks);
    }

    float shieldForTest() { return susanooShield; }

    void setShieldForTest(float s) { susanooShield = s; }

    void kawarimiForTest(ServerLevel sl) { kawarimi(sl); }

    /** Runs the Lightning Beast and the Vortex to their end at once (the self-test cannot wait 44 ticks). */
    void runStatesForTest(ServerLevel sl) {
        for (int i = 0; i < 80 && fangLeft > 0; i++) tickFang(sl);
        for (int i = 0; i < 80 && vortexLeft > 0; i++) tickVortex(sl);
    }

    List<Move> moveListForTest() { return moves(); }

    /** Runs the clones of lightning, the gates and the Six Paths for a while (the self-test cannot wait). */
    void runCanonForTest(ServerLevel sl) {
        if (lclones.isEmpty() && gatesUntil == 0L && sixPathsUntil == 0L) return;
        for (int i = 0; i < 6; i++) canonTick(sl, tickCount + i);
    }

    boolean hasStatesForTest() { return fangLeft > 0 || vortexLeft > 0 || susanooUntil != 0L || mistUntil != 0L || earthWallUntil != 0L || !lclones.isEmpty() || gatesUntil != 0L || sixPathsUntil != 0L || cutterUntil != 0L || exhaustedUntil != 0L || isNoGravity(); }
}
