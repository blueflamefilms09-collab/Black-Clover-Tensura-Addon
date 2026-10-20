package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.relics.djinn.DjinnSources;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The Charybdis Spawn, the Deep Waterways' floor boss (section 2, Type B of the design): a floating, aquatic leviathan about five blocks long that
 * calls smaller megalodons out of the basin walls and fires dense magicule scales that players must parry or deflect. It is a Zogratis-framework
 * boss ({@link ZogratisBoss}: the Percentage Power System, the move scheduler, the telegraphs, the damage law, the boss bar and the arena) in a
 * water body of its own:
 * <ul>
 *   <li><b>It swims by hand.</b> A floating water mob has no walkable path, so the framework's walking goal is not registered and every tick
 *       {@link Swim} gives the velocity: a slow orbit round the dais, an approach to any foe near, a brake to a standstill while it casts. It has no
 *       gravity, is not pushed by currents and breathes water because the entity type tag {@code minecraft:can_breathe_under_water} lists it.</li>
 *   <li><b>It surfaces.</b> When it is called it can appear in the water beneath and rise six blocks over three seconds, unhurt and untargeting
 *       ({@link #beginRise}).</li>
 *   <li><b>Its moves</b> ({@link CharybdisMoves}, numbers in {@link CharybdisTable}): the Magicule Scale Volley, the Tentacle Sweep, the
 *       Whirlpool, the Beak Lunge, Summon Megalodons, the Tidal Crush and, from 60% power, the Maelstrom of Scales. Its scales are real projectiles
 *       ({@link MagiculeScaleEntity}) that a shield, the Magebane Shield's parry or a blow turns back on it.</li>
 *   <li><b>Its voice</b> is narration ({@link CharybdisText}): nothing it says names a devil. It reports the Beelzebub of the framework only because
 *       a boss must have a devil; the bar, the chat colour and every line are its own.</li>
 *   <li><b>Its end</b>: inside the Labyrinth the server is told and the first fall in a world pays its Djinn Seal ({@link DjinnSources#floorBossFell});
 *       it drops experience and nothing else, and its megalodons go with it. (Until 1.58.0 it also posted the collapse event of the Charybdis Basin of
 *       the Living Dungeon, which was removed.)</li>
 * </ul>
 */
public class CharybdisSpawnBoss extends ZogratisBoss {
    private static final org.slf4j.Logger LOG = LogUtils.getLogger();
    /** Degrees a tick it may turn: slow enough to circle, quick enough to keep a foe in front. */
    private static final float TURN_YAW = 14f, TURN_PITCH = 6f, MAX_PITCH = 38f;

    private Swim swim;
    private long swimTick;
    private float faceYaw, facePitch;
    private boolean faceSet;
    private int riseLeft, riseTotal;
    private double riseFrom;
    private UUID killer;
    private final Set<Integer> lungeHits = new HashSet<>();
    private String lockId = "";
    private Vec3 lockPoint, lockDir;
    private List<Vec3> lockSpots = List.of();

    public CharybdisSpawnBoss(EntityType<CharybdisSpawnBoss> type, Level level) {
        super(type, level);
        setNoGravity(true);
        addTag(CharybdisTable.TAG_FLOOR_BOSS);
        addTag(CharybdisTable.TAG_BOSS);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CharybdisTable.HEALTH, CharybdisTable.ATTACK, CharybdisTable.SPEED, CharybdisTable.ARMOR, CharybdisTable.TOUGHNESS);
    }

    // ================================================================ identity
    @Override public String bossName() { return "Charybdis Spawn"; }

    /** A framework boss must have a devil; the nearest in colour (space, blue) is lent, and nothing the leviathan shows or says uses the name. */
    @Override public Devil devil() { return Devil.BEELZEBUB; }

    @Override protected BossEvent.BossBarColor barColor() { return BossEvent.BossBarColor.BLUE; }

    @Override protected ChatFormatting chatColor() { return ChatFormatting.AQUA; }

    @Override public String barTitle() { return bossName() + " - Tide " + power() + "%" + (gatesOpen() ? " - Frenzy" : ""); }

    // ---- tuning
    /** The Gates make it brighter and faster, not bigger: its hitbox stays the size the basin's ruins were laid out for. */
    @Override protected float formScale(int form) { return 1.0f; }

    @Override protected float damageScale() { return CharybdisTable.DAMAGE_SCALE; }

    /** The plain bite is lighter than the telegraphed moves. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * CharybdisTable.MELEE_FACTOR; }

    // ---- lines: the beast's own, as narration
    @Override protected String openingLine() { return CharybdisText.opening(); }

    @Override protected String powerLine(int old, int now) { return CharybdisText.power(old, now); }

    @Override protected String gatesLine() { return CharybdisText.gates(); }

    @Override protected String deathLine() { return CharybdisText.death(); }

    @Override public void say(String line) { announce(line); }

    /** The framework's announcement of the Gates names a Tree of Qliphoth: it is dropped (the leviathan's own line follows it at once). */
    @Override
    public void announce(String text) {
        String t = CharybdisText.translate(text);
        if (!t.isEmpty()) super.announce(t);
    }

    // ---- sounds: something old and vast
    @Override protected SoundEvent getAmbientSound() { return com.newuniverse.nusmp.audio.MobVoice.ambient("aquatic"); }          // 1.99.0: the drowned family of the bespoke audio engine

    @Override protected SoundEvent getHurtSound(DamageSource source) { return com.newuniverse.nusmp.audio.MobVoice.hurt("aquatic"); }

    @Override protected SoundEvent getDeathSound() { return com.newuniverse.nusmp.audio.MobVoice.death("aquatic"); }

    // ================================================================ body: a floating water mob
    /** No walking goal and no floating goal (which would push it up): it only picks a target; it is steered by {@link #bossTick}. */
    @Override
    protected void registerGoals() {
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<Player>(this, Player.class, true) {
            @Override public boolean canUse() { return !rising() && super.canUse(); }
        });
    }

    @Override public boolean isPushedByFluid() { return false; }

    /** Its megalodons are its own: nothing it does to foes touches them. */
    @Override public boolean isAlliedTo(Entity other) { return CharybdisMinions.isFriend(other) || super.isAlliedTo(other); }

    // ================================================================ surfacing
    /** The rise out of the well: for 'ticks' ticks it climbs the curve of {@link CharybdisMath#riseY}, invulnerable and with no target. */
    public void beginRise(int ticks) {
        riseTotal = Math.max(1, ticks);
        riseLeft = riseTotal;
        riseFrom = getY();
    }

    public boolean rising() { return riseLeft > 0; }

    /** Nothing hurts it while it rises (this is not saved: a boss reloaded in the middle of its rise is simply a boss). */
    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return (rising() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) || super.isInvulnerableTo(source);
    }

    private void tickRise(ServerLevel sl) {
        riseLeft--;
        double y = CharybdisMath.riseY(riseFrom, riseLeft, riseTotal);
        setDeltaMovement(0, y - getY(), 0);
        if (getTarget() != null) setTarget(null);
        if (tickCount % 3 == 0) sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY() + 1.2, getZ(), 8, 1.4, 1.1, 1.4, 0.05);
        if (riseLeft == riseTotal - 1) CharybdisVfx.surfacing(this, riseTotal);
        if (riseLeft == 0) {
            sl.playSound(null, blockPosition(), SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 3f, 0.5f);
            CharybdisVfx.burst(this);
        }
    }

    // ================================================================ every tick: swim, face, lunge
    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            if (!faceSet) {
                faceYaw = getYRot();
                facePitch = 0f;
                faceSet = true;
            }
            if (tickCount % 20 == 0 && getAirSupply() < getMaxAirSupply()) setAirSupply(getMaxAirSupply());       // belt and braces: the tag already lets it breathe
            if (rising()) {
                tickRise(sl);
                return;
            }
            Swim s = swim();
            LivingEntity target = getTarget();
            boolean has = target != null && target.isAlive();
            Vec3 tc = has ? target.getBoundingBox().getCenter() : position();
            int st = state();
            boolean hold = st == STATE_CASTING || st == STATE_TRANSFORMING || st == STATE_RECOVERING;
            double scale = getAttributeValue(Attributes.MOVEMENT_SPEED) / CharybdisTable.SPEED;                    // the power's pace, and any slowing laid on it
            s.step(getX(), getY(), getZ(), has, tc.x, tc.y, tc.z, hold, scale, swimTick++);
            Vec3 v = new Vec3(s.vx(), s.vy(), s.vz());
            setDeltaMovement(v);
            steerFacing(has ? tc : null, hold, v);
            if (s.lunging()) CharybdisMoves.lungeTick(this);
            else if (!lungeHits.isEmpty()) lungeHits.clear();
            if (s.stuckTooLong()) putBack(sl);
            if (tickCount % 5 == 0) CharybdisMinions.drive(this);
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] The Charybdis Spawn's tick failed: {}", e.toString());
        }
    }

    /** The steering of this boss (made on first use, round the arena point). */
    Swim swim() {
        if (swim == null) {
            Vec3 c = arenaCenter();
            swim = new Swim(c.x, c.y, c.z, arena().asLong());
        }
        return swim;
    }

    /** The way it looks: along a locked line while it winds up a sweep or a lunge, at its target while it casts or waits, otherwise the way it swims. */
    private void steerFacing(Vec3 targetCentre, boolean hold, Vec3 v) {
        Vec3 want = null;
        if (lockDir != null && (isLocked(CharybdisTable.BEAK_LUNGE) || isLocked(CharybdisTable.TENTACLE_SWEEP)) && state() == STATE_CASTING) want = lockDir;
        else if (swim != null && swim.lunging()) want = v;
        else if (targetCentre != null && (hold || v.x * v.x + v.z * v.z < 0.0009)) want = targetCentre.subtract(position().add(0, getBbHeight() / 2, 0));
        else if (v.lengthSqr() > 0.0009) want = v;
        if (want == null || want.lengthSqr() < 1e-6) return;
        faceYaw = Swim.turn(faceYaw, Swim.yawOf(want.x, want.z), TURN_YAW);
        facePitch = Swim.turn(facePitch, Mth.clamp(Swim.pitchOf(want.x, want.y, want.z), -MAX_PITCH, MAX_PITCH), TURN_PITCH);
    }

    /** Stuck against the stonework for ten seconds: put back on the open orbit where its body fits. */
    private void putBack(ServerLevel sl) {
        for (int k = 0; k < 6; k++) {
            double[] p = swim().orbitPoint(swimTick + 40L * k);
            Vec3 to = new Vec3(p[0], p[1], p[2]);
            if (!sl.noCollision(this, getBoundingBox().move(to.subtract(position())))) continue;
            sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, getX(), getY() + 1, getZ(), 20, 1.2, 1.0, 1.2, 0.05);
            teleportTo(to.x, to.y, to.z);
            sl.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, to.x, to.y + 1, to.z, 20, 1.2, 1.0, 1.2, 0.05);
            break;
        }
        swim().clearStuck();
    }

    /** The rotation is the server's own, set at the very end of the tick (the look control and the body-turn of every mob would otherwise level it out). */
    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && faceSet) {
            setYRot(faceYaw);
            setXRot(facePitch);
            yBodyRot = faceYaw;
            yHeadRot = faceYaw;
        }
    }

    /** The way it faces now, as a unit vector along the floor. */
    Vec3 facing() {
        double a = Math.toRadians(faceYaw);
        return new Vec3(-Math.sin(a), 0, Math.cos(a));
    }

    // ================================================================ what the wind-ups drew
    /** Remembers what the wind-up of move 'id' drew (a point and/or a direction), so the move lands exactly there. */
    void lock(String id, Vec3 point, Vec3 dir) {
        lockId = id;
        lockPoint = point;
        lockDir = dir;
    }

    boolean isLocked(String id) { return lockId.equals(id); }

    /** The point the wind-up of move 'id' drew so far (null if another move's was the last), without forgetting it: the telegraph draws it every few ticks. */
    Vec3 peekPoint(String id) { return isLocked(id) ? lockPoint : null; }

    /** The wall spots the summon's wind-up has marked so far, without forgetting them. */
    List<Vec3> peekSpots() { return isLocked(CharybdisTable.SUMMON) ? lockSpots : List.of(); }

    /** The point the wind-up of move 'id' drew (null if another move's was the last), forgetting it. */
    Vec3 takePoint(String id) {
        Vec3 p = isLocked(id) ? lockPoint : null;
        if (isLocked(id)) forgetLock();
        return p;
    }

    /** The direction the wind-up of move 'id' drew (null if another move's was the last), forgetting it. */
    Vec3 takeDir(String id) {
        Vec3 d = isLocked(id) ? lockDir : null;
        if (isLocked(id)) forgetLock();
        return d;
    }

    void lockSpots(List<Vec3> spots) {
        lockId = CharybdisTable.SUMMON;
        lockSpots = spots;
    }

    /** The wall spots the summon's wind-up marked (empty if there are none), forgetting them. */
    List<Vec3> takeSpots() {
        List<Vec3> out = isLocked(CharybdisTable.SUMMON) ? lockSpots : List.of();
        forgetLock();
        return out;
    }

    Vec3 lockedDir() { return lockDir; }

    void forgetLock() {
        lockId = "";
        lockPoint = null;
        lockDir = null;
        lockSpots = List.of();
    }

    /** The foes the current lunge has struck already (each is struck once per lunge). */
    Set<Integer> lungeHits() { return lungeHits; }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (CharybdisTable.Spec s : CharybdisTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            Move.MoveTell tell = CharybdisMoves.tellOf(s.id());
            if (tell != null) b.tell(tell);
            out.add(b.run(CharybdisMoves.runOf(s.id())));
        }
        return out;
    }

    // ================================================================ the fight's hooks
    /** The Frenzy (the 100% mode): a ring of cold tide, a vortex and a spout; the numbers of the moves grow with {@link CharybdisMath#sweepReach} and its kin. */
    @Override
    protected void onGatesOpened() {
        CharybdisVfx.frenzy(this);
    }

    @Override
    protected void onFightStart() {
        forgetLock();
        lungeHits.clear();
        announce(CharybdisText.hint());
    }

    @Override
    protected void onFightEnd(boolean won) {
        forgetLock();
        lungeHits.clear();
        if (swim != null) swim.halt();
        if (!(level() instanceof ServerLevel sl)) return;
        CharybdisMinions.dismiss(sl, this);
        if (won) CharybdisVfx.burst(this);
        if (won) reportFall(sl, killer == null ? null : sl.getServer().getPlayerList().getPlayer(killer));
    }

    /** The leviathan fell: inside the Labyrinth the server is told, once, and the first fall in a world pays its Djinn Seal. */
    private void reportFall(ServerLevel sl, ServerPlayer slayer) {
        if (!Labyrinth.in(sl)) return;
        try {
            String who = slayer == null ? null : slayer.getName().getString();
            sl.getServer().getPlayerList().broadcastSystemMessage(Component.literal(CharybdisText.slain(who)).withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD), false);
            DjinnSources.floorBossFell(sl, arena(), "CHARYBDIS_BASIN", slayer);
        } catch (RuntimeException | LinkageError failure) {
            LOG.warn("[nusmp] Could not announce the fall of the Charybdis Spawn", failure);
        }
    }

    /** Remembers who struck the last blow (the Seal goes to them). */
    @Override
    public void die(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer p) killer = p.getUUID();
        else if (getKillCredit() instanceof ServerPlayer credit) killer = credit.getUUID();
        super.die(source);
    }
}
