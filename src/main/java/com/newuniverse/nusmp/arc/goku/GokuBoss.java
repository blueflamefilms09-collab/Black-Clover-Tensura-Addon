package com.newuniverse.nusmp.arc.goku;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.avatar.TensuraBridge;
import com.newuniverse.nusmp.boss.rules.BossHealGuard;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.saiyan.SaiyanForms;
import com.newuniverse.nusmp.saiyan.SaiyanState;
import com.newuniverse.nusmp.saiyan.SaiyanStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the Goku boss (docs/goku_boss.md): Son Goku as an arc boss that fights in the canon order of his forms, very overpowered, and grows with the Tensura EP of the players who face him.
 * He stands on the arcs' boss framework ({@link ArcBoss}: telegraphed moves, the per-second damage budget, the boss bar, the arena and its leash, the story credit, the buff row), and adds:
 * <ul>
 * <li><b>The form ladder</b> ({@link GokuLadder}): Base to True Ultra Instinct, chosen by health and fight time, one step at a time. Every change is a 40-tick visible power-up (a gathering,
 *     a ring, a burst; he can be hit all the time, nothing is invulnerable) and then the form's damage, speed and armour are applied as TRANSIENT attribute modifiers (replaced on the next
 *     change, removed on death, removal and a lost fight, never saved).</li>
 * <li><b>Scaling</b> ({@link GokuBossMath}): once at spawn and then at most every {@link GokuBossMath#RESCAN_TICKS} ticks he reads the players near him with ONE box query, their EP with the
 *     repo's reader ({@code TensuraBridge.maxEp}), else their Saiyan Battle Power, else the difficulty preset's fixed value; health and damage follow it as a capped smooth function and never
 *     fall during a fight.</li>
 * <li><b>Moves</b> ({@link GokuMoves}): a melee combo, the Kamehameha (one server raycast, one foe query), Instant Transmission behind the target, the Spirit Bomb (a long telegraph, he is exposed
 *     afterwards, a hard hit interrupts it), a Kaio-ken burst in the early forms and, from Ultra Instinct Sign, a capped automatic dodge with a cooldown and a limit per window.</li>
 * <li><b>Safety</b>: one hit on a player takes at most {@code maxHitPercent} of their health; the Kamehameha and Spirit Bomb break no blocks unless {@code blockDamage} and the griefBlocks rule are
 *     both on; at most {@code globalCap} bosses live in the server; every particle goes through the shared budget.</li>
 * </ul>
 * He starts at 100% of the framework's power curve (no Gates, no refill, no invulnerable cutscene: the forms replace them), so the framework's damage tuning (x1.6) is divided out of
 * {@link #damageScale()} and the damage he deals is exactly the table's.
 * <p>Cost per tick: one comparison per module; once a second the framework's upkeep; every 100 ticks one box query of the players near him (at most 10 counted) with one EP read each; a form
 * change every 120 ticks at most; a particle packet every 4 ticks (at most 8 particles) while a form is on. Every move is bounded (see {@link GokuMoves}).
 */
public class GokuBoss extends ArcBoss {
    public static final String ID = "goku_boss";

    /** Who he is (the roster record every system of the arcs reads). Tier 5 (the god tier of the buff ladder). */
    public static final ArcCharacter CHARACTER = ArcCharacter.boss(ID, "Son Goku")
            .title("the Saiyan Raised on Earth").source("Dragon Ball").saga(ArcSaga.ALLIES).tier(5).colors(0xFFD020, 0xFFFFFF)
            .stats(30000, 40, 0.34, 20)
            .lines("Wow, you're strong! This is going to be fun!", "I haven't felt this alive in years!", "Heh... that was a great fight. Thanks.")
            .build();

    /** The most players read in one scan, and the radius of the box (blocks). */
    public static final int SCAN_MAX = GokuBossMath.MAX_PLAYERS;
    public static final double SCAN_RADIUS = 64.0;
    /** The form index from which his Ultra Instinct dodge exists, the last form of the early Kaio-ken bursts, and the first form that may throw a Spirit Bomb. */
    public static final int DODGE_FROM = 7, KAIO_UP_TO = 3, BOMB_FROM = 2;
    /** A hard hit of this share of his maximum health, taken while a Spirit Bomb gathers, breaks it. */
    public static final float BOMB_INTERRUPT = 0.02f;
    /** The last ticks of the Spirit Bomb telegraph in which its landing spot no longer follows the target. */
    public static final int BOMB_FREEZE = 36;

    private static final EntityDataAccessor<Integer> FORM_IDX = SynchedEntityData.defineId(GokuBoss.class, EntityDataSerializers.INT);

    /** The bosses of this class made on the server and not yet removed (the cap and the logout cleanup read it). A boss joins at construction, so a burst of summonings in one tick cannot pass the cap. */
    private static final java.util.Set<GokuBoss> LIVE = ConcurrentHashMap.newKeySet();

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("anime_multiverse", "goku_boss/" + path); }

    private static final ResourceLocation HEALTH_ID = id("health"), DMG_ID = id("form_damage"), SPD_ID = id("form_speed"), DEF_ID = id("form_armor"),
            KAIO_DMG_ID = id("kaio_damage"), KAIO_SPD_ID = id("kaio_speed");

    private List<Move> book;
    private GokuBossMath.Reading reading = new GokuBossMath.Reading(GokuBossMath.Preset.OVERPOWERED.floorEp(), GokuBossMath.Source.PRESET);
    private int players = 1;
    private boolean scanned, modsApplied;
    private long nextScan;
    private double healthFactor = 1.0, damageFactor = 1.0, pace = 1.0;
    private long fightTicks, inForm, kaioUntil, staggerUntil, dodgeReadyAt, windowStart;
    private int pending = -1, windLeft, windowCount;
    private float bombDamage;
    private Vec3 bombSpot;

    public GokuBoss(EntityType<? extends GokuBoss> type, Level level) {
        super(type, level);
        this.xpReward = 5000;
        if (!level.isClientSide) LIVE.add(this);
    }

    /** His base attributes (the settings and the players scale them afterwards): 30,000 health, 40 melee, 20 armour. */
    public static AttributeSupplier.Builder attributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), 12.0);
    }

    /** How many of him stand in loaded chunks (server side). */
    public static int liveCount() {
        prune();
        return LIVE.size();
    }

    /** Drops the removed ones and the ones that were made but never put into the world (a failed summoning); at most a handful of entries, one level lookup for each that has not ticked yet. */
    private static void prune() {
        LIVE.removeIf(b -> b.isRemoved() || (b.tickCount == 0 && b.level() instanceof ServerLevel s && s.getEntity(b.getId()) != b));
    }

    /** The live bosses (a copy; for the command and the cleanup events). */
    public static List<GokuBoss> live() {
        prune();
        return List.copyOf(LIVE);
    }

    /** Forgets everything (the server is stopping). */
    public static void clearLive() { LIVE.clear(); }

    @Override public ArcCharacter character() { return CHARACTER; }

    @Override
    protected List<Move> moves() {
        if (book == null) book = GokuMoves.build();
        return book;
    }

    // ================================================================ synced state
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(FORM_IDX, 0);
    }

    /** The step of the {@link GokuLadder} he wears (0 = Base). Synced, so the client picks his skin from it. */
    public int formIdx() { return GokuLadder.clamp(entityData.get(FORM_IDX)); }

    /** The form's plain name ("Super Saiyan Blue Kaioken"). */
    public static String formName(int idx) {
        String id = GokuLadder.step(idx).id();
        StringBuilder sb = new StringBuilder();
        for (String w : id.split("_")) {
            if (w.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return sb.toString();
    }

    public boolean kaioActive() { return kaioUntil > tickCount; }

    public boolean windingUp() { return windLeft > 0; }

    public GokuBossMath.Reading reading() { return reading; }

    public int countedPlayers() { return players; }

    public double healthFactor() { return healthFactor; }

    public double damageFactorValue() { return damageFactor; }

    public long fightTicks() { return fightTicks; }

    // ================================================================ framework settings: no Gates, no refill, the forms replace them
    @Override protected int openingPower() { return PowerCurve.FULL; }

    @Override protected int gatesForm() { return FORM_BASE; }

    @Override protected boolean refillOnGates() { return false; }

    @Override public String barTitle() { return bossName() + " - " + formName(formIdx()); }

    /** He brawls up close. */
    @Override protected double preferredRange(double suggested) { return Math.min(suggested, 6.0); }

    /** The framework multiplies by its power tuning (x1.6 at 100%): divide it out so the damage is exactly base x form x scaling. */
    @Override
    protected float damageScale() { return (float) (damageFactor / Math.max(0.1, tuning().damageMultiplier())); }

    /** The base of his blows: his melee attribute (form and Kaio-ken modifiers included). */
    public float base() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE); }

    // ================================================================ the tick
    @Override
    protected void bossTick() {
        super.bossTick();
        if (level().isClientSide) return;
        LIVE.add(this);
        if (!(level() instanceof ServerLevel sl) || isDeadOrDying()) return;
        if (tickCount >= nextScan) scan(sl);
        if (!modsApplied) applyForm(formIdx());
        if (fightActive()) {
            fightTicks++;
            inForm++;
            if (windLeft > 0) tickWindup(sl);
            else stepForm(sl);
        }
        if (kaioUntil != 0 && tickCount >= kaioUntil) endKaio();
        if (tickCount % 4 == 0) fx(sl);
        if (bombDamage > 0f && !"spirit_bomb".equals(castId())) bombDamage = 0f;
    }

    private String castId() {
        Move m = castingMoveOrNull();
        return m == null ? "" : m.id();
    }

    /** Asks for a new reading at the next tick (a player left, died or changed dimension). */
    public void rescanSoon() { nextScan = 0L; }

    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (windLeft > 0 || staggerUntil > tickCount) return -1;
        int i = super.chooseMove(bk, s, t);
        if (i < 0) return -1;
        String id = bk.get(i).id();
        int f = formIdx();
        if (GokuMoves.KAIO.equals(id) && (f > KAIO_UP_TO || kaioActive())) return -1;      // tried again at the next decision
        if (GokuMoves.BOMB.equals(id) && f < BOMB_FROM) return -1;
        return i;
    }

    @Override
    protected void onFightStart() {
        super.onFightStart();
        fightTicks = 0;
        inForm = 0;
        scanned = false;
        nextScan = 0L;
    }

    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        windLeft = 0;
        pending = -1;
        endKaio();
        if (won) {
            clearMods();
        } else {
            entityData.set(FORM_IDX, 0);                         // an abandoned fight: back to Base at full health, the next fight is read fresh
            fightTicks = 0;
            inForm = 0;
            scanned = false;
            applyForm(0);
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        LIVE.remove(this);
        if (!level().isClientSide) clearMods();
        super.remove(reason);
    }

    // ================================================================ the scaling read
    /** One box query of the players near him, one EP read each (at most {@link #SCAN_MAX} counted). */
    private void scan(ServerLevel sl) {
        nextScan = tickCount + GokuBossMath.RESCAN_TICKS;
        boolean useEp = GokuBossConfig.epScaling();
        double topEp = 0.0, topBp = 0.0;
        int n = 0;
        for (Player p : sl.getEntitiesOfClass(Player.class, getBoundingBox().inflate(SCAN_RADIUS), pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator())) {
            if (n >= SCAN_MAX) break;
            n++;
            if (useEp) topEp = Math.max(topEp, TensuraBridge.maxEp(p));
            SaiyanState s = SaiyanStore.peek(p);
            if (s != null && s.saiyan) topBp = Math.max(topBp, s.battlePower);
        }
        GokuBossMath.Params params = GokuBossConfig.params();
        GokuBossMath.Reading r = GokuBossMath.resolve(useEp, topEp, topBp, params.preset());
        boolean first = !scanned;
        if (first || !fightActive()) {                          // before a fight he may follow the party both ways; in a fight he only ever grows
            reading = r;
            players = Math.max(1, n);
        } else {
            if (r.ep() > reading.ep()) reading = r;
            players = Math.max(players, Math.max(1, n));
        }
        scanned = true;
        applyScaling(params);
    }

    private void applyScaling(GokuBossMath.Params params) {
        healthFactor = GokuBossMath.healthFactor(reading, players, params);
        damageFactor = GokuBossMath.damageFactor(reading, players, params);
        pace = GokuBossMath.paceFactor(reading, params);
        AttributeInstance max = getAttribute(Attributes.MAX_HEALTH);
        if (max == null) return;
        double want = healthFactor - 1.0;
        AttributeModifier have = max.getModifier(HEALTH_ID);
        if (have != null && Math.abs(have.amount() - want) < 1.0e-6) return;
        float before = getMaxHealth(), hp = getHealth();
        max.addOrReplacePermanentModifier(new AttributeModifier(HEALTH_ID, want, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        float to = GokuBossMath.keepShare(hp, before, getMaxHealth());
        dpsBudget().bypass(() -> BossHealGuard.allow(() -> setHealth(to)));      // a change of the maximum is not damage and not a second wind
    }

    // ================================================================ the forms
    private void stepForm(ServerLevel sl) {
        double hp = getHealth() / Math.max(1f, getMaxHealth());
        long enrage = GokuBossMath.enrageTicks(GokuBossConfig.enrageMinutes(), pace);
        int next = GokuLadder.next(formIdx(), hp, fightTicks, enrage, inForm);
        if (next != formIdx()) beginChange(sl, next);
    }

    private void beginChange(ServerLevel sl, int to) {
        pending = to;
        windLeft = GokuLadder.WINDUP;
        interruptCast();
        getNavigation().stop();
        say(GokuLadder.step(to).line());
        sl.playSound(null, blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3f, 1.4f);
    }

    private void tickWindup(ServerLevel sl) {
        windLeft--;
        getNavigation().stop();
        int rgb = GokuLadder.rgb(pending);
        Vec3 mid = position().add(0, getBbHeight() * 0.5, 0);
        if (windLeft % 4 == 0) GokuFx.gather(sl, mid, 1.6, rgb, 8);
        if (windLeft % 10 == 0) GokuFx.ring(sl, position(), 2.0 + (GokuLadder.WINDUP - windLeft) * 0.1, rgb, 14);
        if (windLeft == GokuLadder.WINDUP / 2) {
            entityData.set(FORM_IDX, pending);                  // the skin changes in the middle of the power-up
            GokuFx.burst(sl, mid, rgb, 16);
            sl.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2.5f, 1.3f);
        }
        if (windLeft <= 0) {
            applyForm(pending);
            inForm = 0;
            pending = -1;
            GokuFx.burst(sl, mid, rgb, 20);
            GokuFx.ring(sl, position(), 6.0, rgb, 20);
            sl.playSound(null, blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 3f, 0.7f);
            for (LivingEntity f : foes(6.0)) {                  // the shock of the power-up shoves, never hurts
                Vec3 away = f.position().subtract(position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) f.knockback(0.7, -away.x, -away.z);
            }
        }
    }

    /** Puts the form's damage, speed and armour on him as transient modifiers (replacing the last form's). */
    private void applyForm(int idx) {
        double strength = GokuBossConfig.preset().formStrength();
        setModifier(Attributes.ATTACK_DAMAGE, DMG_ID, GokuLadder.damageMul(idx, strength) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(Attributes.MOVEMENT_SPEED, SPD_ID, GokuLadder.speedMul(idx, strength) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(Attributes.ARMOR, DEF_ID, GokuLadder.defense(idx) * 10.0, AttributeModifier.Operation.ADD_VALUE);
        modsApplied = true;
    }

    private void setModifier(Holder<Attribute> attr, ResourceLocation modId, double amount, AttributeModifier.Operation op) {
        AttributeInstance a = getAttribute(attr);
        if (a == null) return;
        if (Math.abs(amount) < 1.0e-9) a.removeModifier(modId);
        else a.addOrUpdateTransientModifier(new AttributeModifier(modId, amount, op));
    }

    private void removeModifiers(Holder<Attribute> attr) {
        AttributeInstance a = getAttribute(attr);
        if (a == null) return;
        a.removeModifier(DMG_ID);
        a.removeModifier(SPD_ID);
        a.removeModifier(DEF_ID);
        a.removeModifier(KAIO_DMG_ID);
        a.removeModifier(KAIO_SPD_ID);
    }

    /** Takes every transient modifier off (death, removal, a lost fight). */
    private void clearMods() {
        removeModifiers(Attributes.ATTACK_DAMAGE);
        removeModifiers(Attributes.MOVEMENT_SPEED);
        removeModifiers(Attributes.ARMOR);
        kaioUntil = 0L;
        modsApplied = false;
    }

    // ================================================================ Kaio-ken burst
    /** A few seconds of Kaio-ken: x2 stage damage and the chart's speed, and he takes 10% more while it lasts (the strain). */
    void startKaio(int ticks) {
        kaioUntil = tickCount + Math.max(20, Math.min(200, ticks));
        setModifier(Attributes.ATTACK_DAMAGE, KAIO_DMG_ID, SaiyanForms.kaioDamageMul(2) - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        setModifier(Attributes.MOVEMENT_SPEED, KAIO_SPD_ID, SaiyanForms.KAIOKEN.speed(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        say("Kaio-ken!");
        if (level() instanceof ServerLevel sl) {
            GokuFx.burst(sl, position().add(0, getBbHeight() * 0.5, 0), 0xE03020, 14);
            sl.playSound(null, blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.HOSTILE, 2f, 1.6f);
        }
    }

    private void endKaio() {
        kaioUntil = 0L;
        AttributeInstance d = getAttribute(Attributes.ATTACK_DAMAGE), s = getAttribute(Attributes.MOVEMENT_SPEED);
        if (d != null) d.removeModifier(KAIO_DMG_ID);
        if (s != null) s.removeModifier(KAIO_SPD_ID);
    }

    // ================================================================ the aura
    private void fx(ServerLevel sl) {
        int idx = formIdx();
        boolean kaio = kaioActive();
        if (idx == 0 && !kaio) return;
        GokuFx.aura(sl, this, kaio && idx <= KAIO_UP_TO ? 0xE03020 : GokuLadder.rgb(idx), GokuLadder.electric(idx) || kaio);
        if (idx >= DODGE_FROM && tickCount % 20 == 0 && tickCount >= dodgeReadyAt) GokuFx.shimmer(sl, this, 4);   // the dodge is ready: the players can see it
    }

    // ================================================================ his attacks
    /** Hits one target with a blow of {@code mul} x his base, with the pvp hit cap of the setting. Returns whether it landed. */
    @Override
    public boolean strike(LivingEntity victim, float baseDamage, boolean magic) {
        if (victim == null || !victim.isAlive() || isFamily(victim)) return false;
        DamageSource src = magic ? damageSources().indirectMagic(this, this) : damageSources().mobAttack(this);
        float dealt = bossDamage(victim, baseDamage);
        if (victim instanceof Player) {
            double share = GokuBossConfig.maxHitPercent();
            if (src.scalesWithDifficulty() && level().getDifficulty() == Difficulty.HARD) share /= 1.5;     // the game multiplies a mob's damage on Hard after the cap
            dealt = GokuBossMath.capHit(dealt, victim.getMaxHealth(), share);
        }
        return victim.hurt(src, dealt);
    }

    /**
     * The Kamehameha: one raycast against the blocks and one foe query; every foe within {@code radius} of the line is hit once (magic, so armour does not stop it). Breaks nothing unless
     * {@code blockDamage} and the griefBlocks rule allow, and then at most nine blocks where it ends. Returns how many foes it hit.
     */
    int fireBeam(LivingEntity target, double reach, double radius, float mul, int rgb) {
        if (!(level() instanceof ServerLevel sl)) return 0;
        Vec3 from = getEyePosition();
        Vec3 aim = target.position().add(0, target.getBbHeight() * 0.5, 0).subtract(from);
        if (aim.lengthSqr() < 1e-6) return 0;
        Vec3 to = from.add(aim.normalize().scale(reach));
        BlockHitResult hit = sl.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
        GokuFx.beam(sl, from, end, rgb);
        swing(InteractionHand.MAIN_HAND);
        int hits = 0;
        float dmg = base() * mul;
        for (LivingEntity f : foes(reach + 3.0)) {
            Vec3 c = f.position().add(0, f.getBbHeight() * 0.5, 0);
            if (GokuBossMath.distToSegment(c.x, c.y, c.z, from.x, from.y, from.z, end.x, end.y, end.z) > radius + f.getBbWidth() * 0.5) continue;
            if (strike(f, dmg, true)) {
                hits++;
                Vec3 away = f.position().subtract(position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) f.knockback(0.8, -away.x, -away.z);
            }
        }
        if (hit.getType() == HitResult.Type.BLOCK) crater(hit.getBlockPos(), 1);
        GokuFx.burst(sl, end, rgb, 10);
        return hits;
    }

    /** Where the Spirit Bomb will land: the target's spot while more than {@link #BOMB_FREEZE} ticks of the telegraph are left, then that spot is frozen (a call with 0 ticks left just reads it). */
    Vec3 aimBomb(LivingEntity target, int ticksLeft) {
        if (bombSpot == null || ticksLeft > BOMB_FREEZE) bombSpot = target.position();
        return bombSpot;
    }

    /** The Spirit Bomb falls: every foe within {@code radius} of the spot is hit once (magic). Returns how many. */
    int dropBomb(Vec3 at, double radius, float mul, int rgb) {
        if (!(level() instanceof ServerLevel sl)) return 0;
        int hits = 0;
        float dmg = base() * mul;
        for (LivingEntity f : foesAround(at, radius)) {
            if (strike(f, dmg, true)) {
                hits++;
                Vec3 away = f.position().subtract(at).multiply(1, 0, 1);
                if (away.lengthSqr() > 1e-4) f.knockback(1.2, -away.x, -away.z);
            }
        }
        bombSpot = null;
        GokuFx.burst(sl, at.add(0, 1, 0), rgb, 24);
        GokuFx.ring(sl, at, radius, rgb, 24);
        sl.playSound(null, BlockPos.containing(at), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.HOSTILE, 4f, 0.5f);
        crater(BlockPos.containing(at), 2);
        return hits;
    }

    /** Breaks the blocks round a point, at most 9 (radius 1) or 25 (radius 2) of them in a flat disc, only when the setting AND the griefBlocks rule allow it (through the boss framework's ledger, put back later). */
    private void crater(BlockPos centre, int r) {
        if (!GokuBossConfig.blockDamage() || !blocksMayChange()) return;
        for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) tryDestroy(centre.offset(dx, 0, dz));
    }

    /** Teleports behind the target (a blink inside the arena, at most the move's range away) and strikes. False when there is no safe spot. */
    boolean transmit(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl) || distanceTo(target) > 48.0) return false;
        Vec3 look = target.getLookAngle().multiply(1, 0, 1);
        if (look.lengthSqr() < 1e-4) look = new Vec3(1, 0, 0);
        Vec3 dest = groundOrNull(sl, target.position().subtract(look.normalize().scale(2.2)));
        double leash = ARENA_RADIUS - 6.0;
        if (dest == null || dest.distanceToSqr(arenaCenter()) > leash * leash) return false;
        if (!blinkTo(dest)) return false;
        getLookControl().setLookAt(target, 60f, 60f);
        later(6, () -> {
            if (target.isAlive() && distanceTo(target) < 6.0) {
                swing(InteractionHand.MAIN_HAND);
                strike(target, base() * 1.3f, false);
            }
        });
        return true;
    }

    // ================================================================ taking damage: the dodge, the interrupt, the defence
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return super.hurt(source, amount);
        if (tryDodge(source, amount)) return false;
        float before = getHealth();
        boolean landed = super.hurt(source, amount);
        float dealt = before - getHealth();
        if (landed && dealt > 0f && "spirit_bomb".equals(castId())) {
            bombDamage += dealt;
            if (bombDamage >= getMaxHealth() * BOMB_INTERRUPT) interruptBomb();
        }
        return landed;
    }

    /** A hard enough hit breaks the Spirit Bomb: the cast is dropped, he is dazed for 3 s and the players see it. */
    private void interruptBomb() {
        bombDamage = 0f;
        interruptCast();
        staggerUntil = tickCount + 60;
        say("Nngh... the Spirit Bomb!");
        if (level() instanceof ServerLevel sl) {
            GokuFx.burst(sl, position().add(0, getBbHeight() * 0.9, 0), 0x8CD8FF, 12);
            sl.playSound(null, blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.HOSTILE, 2f, 0.4f);
        }
    }

    /**
     * Ultra Instinct: from Sign on, a hit can be avoided by itself. The chance is the chart's, never above {@code uiDodgeCap}; there is a cooldown (at least 2 s) and a limit per window; he
     * cannot dodge while he casts, is exposed after a big move or powers up; /kill and the void are never dodged. A silver flicker shows when the dodge is ready.
     */
    private boolean tryDodge(DamageSource source, float amount) {
        int idx = formIdx();
        if (idx < DODGE_FROM || windLeft > 0 || !(amount > 0f) || source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        if (state() == STATE_CASTING || exposed() || staggerUntil > tickCount) return false;
        SaiyanForms.Dodge d = SaiyanForms.dodge(GokuLadder.step(idx).id(), 1.0);
        if (d == null || tickCount < dodgeReadyAt) return false;
        if (tickCount - windowStart > d.window()) {
            windowStart = tickCount;
            windowCount = 0;
        }
        if (windowCount >= d.perWindow()) return false;
        if (getRandom().nextDouble() >= GokuBossMath.dodgeChance(d.chance(), GokuBossConfig.uiDodgeCap())) return false;
        windowCount++;
        dodgeReadyAt = tickCount + Math.max(40, d.cooldown());
        if (level() instanceof ServerLevel sl) {
            GokuFx.shimmer(sl, this, 12);
            sl.playSound(null, blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.2f, 1.8f);
        }
        if (source.getEntity() instanceof ServerPlayer p) p.displayClientMessage(net.minecraft.network.chat.Component.literal("Goku's body moved on its own."), true);
        return true;
    }

    /** The form's defence: half of its chart share less per hit (the other half is the armour modifier); the Kaio-ken strain makes him take 10% more. */
    @Override
    protected float mitigate(DamageSource source, float amount) {
        float a = super.mitigate(source, amount);
        a *= (float) (1.0 - GokuLadder.reduction(formIdx()) * 0.5);
        if (kaioActive()) a *= 1.10f;
        return a;
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GokuForm", formIdx());
        tag.putLong("GokuFight", fightTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        entityData.set(FORM_IDX, GokuLadder.clamp(tag.getInt("GokuForm")));
        fightTicks = Math.max(0L, tag.getLong("GokuFight"));
        modsApplied = false;                                    // the transient modifiers are not saved: applied again on the first tick
        scanned = false;
    }

    // ================================================================ the drop
    /** A Goku-themed reward: Magic Stones, a gold coin, and the Half Senzu Bean Charm (an item of the mod; skipped if it is not registered). */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        give("anime_multiverse", "magic_stone", 12 + getRandom().nextInt(13));
        give("tensura", "gold_coin", 2 + getRandom().nextInt(3));
        give("anime_multiverse", "half_senzu_bean_charm", 1);
    }

    private void give(String ns, String path, int n) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ns, path));
        if (item != null && item != Items.AIR) spawnAtLocation(new ItemStack(item, n));
    }

    // ================================================================ test seams (package-private, never used by the game)
    /** The self-test sets his form directly (as if the windup had finished). */
    void forceFormForTest(int idx) {
        entityData.set(FORM_IDX, GokuLadder.clamp(idx));
        applyForm(formIdx());
    }

    /** The modifier of the form's damage, or 0 when there is none. */
    double formDamageModifier() {
        AttributeInstance a = getAttribute(Attributes.ATTACK_DAMAGE);
        AttributeModifier m = a == null ? null : a.getModifier(DMG_ID);
        return m == null ? 0.0 : m.amount();
    }

    /** Are any of his transient modifiers still on him? */
    boolean hasFormModifiers() {
        AttributeInstance a = getAttribute(Attributes.ATTACK_DAMAGE);
        return a != null && (a.getModifier(DMG_ID) != null || a.getModifier(KAIO_DMG_ID) != null);
    }

    /** Runs the scan now (the self-test cannot wait a hundred ticks). */
    void scanNow() {
        if (level() instanceof ServerLevel sl) scan(sl);
    }

    /** Takes the form modifiers off, as a death or a removal does. */
    void clearModsForTest() { clearMods(); }

    /** Starts a change to {@code idx} and runs the whole windup at once. */
    void changeNowForTest(int idx) {
        if (!(level() instanceof ServerLevel sl)) return;
        beginChange(sl, GokuLadder.clamp(idx));
        while (windLeft > 0) tickWindup(sl);
    }
}
