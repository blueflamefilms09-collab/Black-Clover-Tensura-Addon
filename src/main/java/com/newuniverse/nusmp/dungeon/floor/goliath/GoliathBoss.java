package com.newuniverse.nusmp.dungeon.floor.goliath;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.Devil;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.relics.djinn.DjinnSources;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxOld;
import com.newuniverse.nusmp.particle.fx.FxThemes;

/**
 * The Goliath, a floor boss of the dungeon: a stone giant six blocks tall that can be born from a bedrock seal in the floor and
 * fights with its weight. It is slow (speed 0.20), hard (1500 health, armour 12, toughness 6, no knockback) and everything it does is a
 * telegraphed ring or circle on the floor ({@link GoliathMoves}, numbers in {@link GoliathTable}):
 * <ul>
 *   <li><b>Rising</b>: called to rise ({@link #spawnRising}), the body is made at once, sunk into the floor: for three seconds the
 *       seal cracks (dust, grinding, a heartbeat), then it climbs out over four, shoving players off the seal. The synched {@link #rise} (0 to 1)
 *       tells the renderer how deep the model is sunk. While it rises it cannot be hurt and looks for nobody.</li>
 *   <li><b>The fight</b> is the framework's ({@link ZogratisBoss}): the percentage power system (20, 40, 60, 80 percent of its fury as its health falls, the
 *       list of moves opening up), then at a quarter of its health its stone cracks and glows (the framework's Gates, here the "cracked" form) and it
 *       moves faster. Seven moves and the plain blow: the Ground Slam, the Twin Stomp, the Roaring Shockwave, the Bedrock Fist, the Seismic Line,
 *       the Rubble Fall, and from 60 percent the ultimate, the Bedrock Quake, after which it is exposed for three seconds.</li>
 *   <li><b>The end</b>: when it falls inside the Labyrinth the server is told and the first fall in a world pays its Djinn Seal
 *       ({@link DjinnSources#floorBossFell}); it drops experience only. (Until 1.58.0 it also posted the Treasury Collapse of its pit in the Living
 *       Dungeon, and a collapsing pit crumbled it; both went with the Living Dungeon.)</li>
 * </ul>
 * The Goliath changes no block at all; its stone is particles and sound. Nothing here names a devil: the framework's devil lines are replaced
 * ({@link GoliathText}); the devil it reports is only the framework's way to colour its boss bar and effects (Astaroth, which is bound to no sibling).
 */
public class GoliathBoss extends ZogratisBoss {
    /** The scoreboard tags every Goliath carries: the first marks "a floor boss" (the boss rules and the economy read it), the second finds a Goliath. */
    public static final String FLOOR_BOSS_TAG = "nusmp_floor_boss", TAG = "nusmp_goliath";

    /** How far the body has climbed out of the floor, 0 (all of it below) to 1 (standing): eased, set by the server every tick of the rise. */
    private static final EntityDataAccessor<Float> RISE = SynchedEntityData.defineId(GoliathBoss.class, EntityDataSerializers.FLOAT);

    /** Ticks of the whole waking: the cracking and the climb. A body that is past it stands. */
    private static final int WOKEN = GoliathRise.CRACK_TICKS + GoliathRise.CLIMB_TICKS;

    /** Ticks into the waking (server). A Goliath made standing (a command, an egg) starts at {@link #WOKEN}. */
    private int wakeTicks = WOKEN;
    private boolean fellPosted, warned;
    private ServerPlayer killer;
    /** The marks a wind-up locked for the move that follows (server). */
    private String lockedFor = "";
    private List<Vec3> locked = List.of();
    /** The rise last tick and this tick, for the smooth sink of the model (both sides). */
    private float riseO = 1f, riseNow = 1f;
    private boolean riseSeen;

    public GoliathBoss(EntityType<GoliathBoss> type, Level level) {
        super(type, level);
        addTag(FLOOR_BOSS_TAG);
        addTag(TAG);
    }

    public static AttributeSupplier.Builder createAttributes() { return ZogratisBoss.bossAttributes(GoliathTable.HEALTH, GoliathTable.ATTACK, 0.20, GoliathTable.ARMOR, GoliathTable.TOUGHNESS); }

    @Override public String bossName() { return GoliathText.NAME; }

    /** The framework wants a devil to colour its effects with; Astaroth is the one that is bound to no sibling (the Triad's rage does not reach it). */
    @Override public Devil devil() { return Devil.ASTAROTH; }

    // ================================================================ looks and numbers
    @Override protected BossEvent.BossBarColor barColor() { return BossEvent.BossBarColor.WHITE; }
    @Override protected ChatFormatting chatColor() { return ChatFormatting.GOLD; }
    @Override public String barTitle() { return GoliathText.bar(power(), gatesOpen()); }

    /** The cracked giant is a little bigger (a pure function of the form: it is asked while the entity is still being built). */
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.08f : 1.0f; }

    /** The plain blow is a half of the attack attribute: the giant is slow, and its real hits are the telegraphed ones. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * GoliathTable.MELEE_SHARE; }

    /** It wants to stand close enough for its slam and the stomp, never on top of whoever it fights. */
    @Override protected double preferredRange(double suggested) { return Math.max(3.0, Math.min(8.0, suggested)); }

    private double reach(double base) { return GoliathTable.reach(base, gatesOpen()); }

    // ================================================================ words
    @Override protected String openingLine() { return GoliathText.opening(); }
    @Override protected String powerLine(int old, int now) { return GoliathText.power(old, now); }
    @Override protected String gatesLine() { return GoliathText.gates(); }
    @Override protected String deathLine() { return GoliathText.death(); }

    /** The framework announces the Gates and the Triad's rage in a devil's voice; the Goliath's players read their own. */
    @Override public void announce(String text) { super.announce(GoliathText.rewrite(text)); }

    // ================================================================ sounds
    @Override protected SoundEvent getHurtSound(DamageSource source) { return com.newuniverse.nusmp.audio.MobVoice.hurt("golem"); }          // 1.99.0: the golem family of the bespoke audio engine
    @Override protected SoundEvent getDeathSound() { return com.newuniverse.nusmp.audio.MobVoice.death("golem"); }
    @Override protected float getSoundVolume() { return 2.0f; }
    @Override public float getVoicePitch() { return 0.5f + (random.nextFloat() - random.nextFloat()) * 0.08f; }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        if (rising()) return;
        playSound(SoundEvents.IRON_GOLEM_STEP, 1.6f, 0.5f);
        if (level() instanceof ServerLevel sl) sl.sendParticles(chips(), getX(), getY() + 0.1, getZ(), 5, 0.9, 0.05, 0.9, 0.04);
    }

    // ================================================================ rising
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(RISE, 1.0f);
    }

    /** 0 (all of the model below the floor) to 1 (standing). */
    public float rise() { return GoliathRise.clampRise(entityData.get(RISE)); }

    /** The rise for drawing between two ticks. */
    public float rise(float partial) { return GoliathRise.clampRise(riseO + (riseNow - riseO) * Math.max(0f, Math.min(1f, partial))); }

    /** Is the body still in the floor? (Both sides: it reads the synched value.) */
    public boolean rising() { return entityData.get(RISE) < 1f; }

    private void beginWake() {
        wakeTicks = 0;
        entityData.set(RISE, 0f);
    }

    private void syncRise() {
        float r = wakeTicks >= WOKEN ? 1f : GoliathRise.progress(wakeTicks - GoliathRise.CRACK_TICKS);
        if (entityData.get(RISE) != r) entityData.set(RISE, r);
    }

    @Override
    public void tick() {
        super.tick();
        float now = rise();
        if (!riseSeen) {
            riseSeen = true;
            riseO = riseNow = now;
        } else {
            riseO = riseNow;
            riseNow = now;
        }
    }

    /** Nothing hurts a body that is still in the floor, except what no invulnerability stops (a command, the void). */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!level().isClientSide && rising() && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false;
        return super.hurt(source, amount);
    }

    /** ... and it looks for nobody until it stands. */
    @Override
    public void setTarget(LivingEntity target) { super.setTarget(rising() ? null : target); }

    private static BlockParticleOption chips() { return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.STONE_BRICKS.defaultBlockState()); }

    private static BlockParticleOption bedrockBits() { return new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BEDROCK.defaultBlockState()); }

    /** One tick of the waking: three seconds of cracking, then the climb. */
    private void wakeStep(ServerLevel sl) {
        int age = wakeTicks++;
        Vec3 seal = position();
        if (age < GoliathRise.CRACK_TICKS) crackStep(sl, seal, age);
        else climbStep(sl, seal, age - GoliathRise.CRACK_TICKS);
        syncRise();
        if (wakeTicks >= WOKEN) risen(sl, seal);
    }

    private void crackStep(ServerLevel sl, Vec3 seal, int age) {
        if (age == 0) {
            announce(GoliathText.cracking());
            GoliathVfx.waking(this, seal);
            for (int k = 0; k < 4; k++) {                                              // four cracks run out of the seal
                double a = k * Math.PI / 2 + 0.4;
                VfxSpawn.send(sl, VfxShape.EARTH_FISSURE, seal.add(Math.cos(a) * 1.5, 0.05, Math.sin(a) * 1.5), seal.add(Math.cos(a) * 9.0, 0.05, Math.sin(a) * 9.0),
                        0xFFFFB45A, 50, 0.9f);
            }
        }
        if (age % 2 == 0) {
            sl.sendParticles(bedrockBits(), seal.x, seal.y + 0.3, seal.z, GoliathRise.crackBits(age), 3.0, 0.2, 3.0, 0.06);
            if (age % 4 == 0) Fx.spray(sl, FxThemes.BOSS_GOLIATH, FxOld.POOF, seal.x, seal.y + 0.2, seal.z, 3, 3.0, 0.1, 3.0, 0.02);
        }
        if (age % 10 == 5) GoliathMoves.dustRing(sl, seal, 1.5 + 3.0 * age / GoliathRise.CRACK_TICKS, GoliathMoves.AMBER, 1.0f);
        if (age % 12 == 0) GoliathMoves.sound(sl, seal, SoundEvents.DEEPSLATE_BREAK, 2.5f, GoliathRise.crackPitch(age));
        if (age % 20 == 0) GoliathMoves.sound(sl, seal, SoundEvents.WARDEN_HEARTBEAT, 3.0f, 0.5f);
    }

    private void climbStep(ServerLevel sl, Vec3 seal, int up) {
        if (up == 0) emerge(sl, seal);
        if (up % 3 == 0) {
            sl.sendParticles(chips(), seal.x, seal.y + 0.2, seal.z, 14, 2.6, 0.3, 2.6, 0.1);
            Fx.spray(sl, FxThemes.BOSS_GOLIATH, FxOld.POOF, seal.x, seal.y + 0.2, seal.z, 2, 2.6, 0.1, 2.6, 0.03);
        }
        if (up % 10 == 0) GoliathMoves.sound(sl, seal, SoundEvents.STONE_BREAK, 2.2f, 0.4f + 0.3f * GoliathRise.progress(up));
    }

    /** The body breaks the seal: a roar, a cloud of stone, and a shove for everyone standing on the seal (away from it, never hurting). */
    private void emerge(ServerLevel sl, Vec3 seal) {
        GoliathMoves.sound(sl, seal, SoundEvents.WARDEN_EMERGE, 4.0f, 0.5f);
        GoliathMoves.sound(sl, seal, SoundEvents.ANVIL_LAND, 2.5f, 0.3f);
        GoliathMoves.burst(sl, seal, 60, 2.2);
        GoliathVfx.emerged(this, seal);
        VfxSpawn.send(sl, VfxShape.EARTH_RISE, seal.add(-2.0, 0.0, 0.0), seal.add(2.0, 0.0, 0.0), 0xFFFFB45A, 30, 1.6f);
        for (ServerPlayer p : sl.players()) {
            if (p.isSpectator() || p.isCreative() || Math.abs(p.getY() - seal.y) > 4.0) continue;
            double dx = p.getX() - seal.x, dz = p.getZ() - seal.z, d = Math.sqrt(dx * dx + dz * dz);
            double k = GoliathRise.shove(d);
            if (k <= 0.0) continue;
            if (d < 0.05) {
                dx = 1.0;
                dz = 0.0;
                d = 1.0;
            }
            p.knockback(k, -dx / d, -dz / d);
            p.hurtMarked = true;
        }
    }

    private void risen(ServerLevel sl, Vec3 seal) {
        announce(GoliathText.risen());
        GoliathMoves.sound(sl, seal, SoundEvents.RAVAGER_ROAR, 4.0f, 0.4f);
        GoliathMoves.dustRing(sl, seal, 3.0, GoliathMoves.DUST, 1.5f);
        GoliathMoves.burst(sl, seal, 40, 2.0);
    }

    // ================================================================ spawning
    /**
     * A Goliath that rises from `seal` (the floor block it comes out of): sunk, cracking the seal for three seconds, then climbing out. Null if it
     * could not be made.
     */
    public static GoliathBoss spawnRising(ServerLevel level, BlockPos seal, float yaw) {
        GoliathBoss g = GoliathEntities.GOLIATH.get().create(level);
        if (g == null) return null;
        g.moveTo(seal.getX() + 0.5, seal.getY() + 1.0, seal.getZ() + 0.5, yaw, 0f);
        g.setYHeadRot(yaw);
        g.setYBodyRot(yaw);
        g.setArena(seal.above());
        g.beginWake();
        return level.addFreshEntity(g) ? g : null;
    }

    /** A Goliath that stands where it is made (a command or an egg): its arena is that spot. Null if it could not be made. */
    public static GoliathBoss spawnStanding(ServerLevel level, Vec3 at, float yaw) {
        GoliathBoss g = GoliathEntities.GOLIATH.get().create(level);
        if (g == null) return null;
        g.moveTo(at.x, at.y, at.z, yaw, 0f);
        g.setYHeadRot(yaw);
        g.setYBodyRot(yaw);
        g.setArena(BlockPos.containing(at));
        if (!level.addFreshEntity(g)) return null;
        GoliathMoves.burst(level, at, 50, 1.8);
        GoliathMoves.sound(level, at, SoundEvents.WARDEN_EMERGE, 3.0f, 0.5f);
        return g;
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        unlock();
        announce(GoliathText.hintWaves());
        announce(GoliathText.hintExposed());
    }

    @Override
    protected void onGatesOpened() {
        unlock();
        GoliathVfx.gates(this);
        for (int delay : new int[]{0, 20, 40}) {
            later(delay, GoliathMoves.safe("gates ring", () -> {
                if (!(level() instanceof ServerLevel sl)) return;
                GoliathMoves.dustRing(sl, position(), 3.0 + delay * 0.1, GoliathMoves.HOT, 1.4f);
                GoliathMoves.burst(sl, position().add(0, 2.5, 0), 30, 1.4);
                GoliathMoves.sound(sl, position(), SoundEvents.STONE_BREAK, 3.0f, 0.3f);
            }));
        }
    }

    @Override
    protected void onFightEnd(boolean won) {
        unlock();
        if (!won || !(level() instanceof ServerLevel sl)) return;
        try {
            fall(sl);
        } catch (RuntimeException | LinkageError failure) {
            LogUtils.getLogger().warn("[nusmp] The fall of the Goliath could not be announced: {}", failure.toString());
        }
    }

    @Override
    public void die(DamageSource source) {
        ServerPlayer who = null;
        if (source.getEntity() instanceof ServerPlayer p) who = p;
        else if (getKillCredit() instanceof ServerPlayer p) who = p;
        killer = who;
        try {
            super.die(source);
        } finally {
            killer = null;
        }
    }

    /** It falls: the effects play; inside the Labyrinth the server is told and the first fall in a world pays its Djinn Seal, once. */
    private void fall(ServerLevel sl) {
        if (fellPosted) return;
        fellPosted = true;
        GoliathMoves.sound(sl, position(), SoundEvents.WARDEN_ROAR, 5.0f, 0.4f);
        GoliathMoves.burst(sl, position().add(0, 1.5, 0), 80, 1.6);
        GoliathVfx.fall(this, position());
        Fx.spray(sl, FxThemes.BOSS_GOLIATH, FxOld.EXPLOSION_EMITTER, getX(), getY() + 2.0, getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        if (!Labyrinth.in(sl)) return;
        String who = killer == null ? null : killer.getName().getString();
        sl.getServer().getPlayerList().broadcastSystemMessage(Component.literal(GoliathText.slain(who)).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        DjinnSources.floorBossFell(sl, arena(), "GOLIATH_PIT", killer);
    }

    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            if (wakeTicks < WOKEN) wakeStep(sl);
            else if (gatesOpen() && tickCount % 8 == 0) embers(sl);
        } catch (RuntimeException | LinkageError failure) {
            if (!warned) {
                warned = true;
                LogUtils.getLogger().warn("[nusmp] The Goliath's tick failed: {}", failure.toString());
            }
        }
    }

    /** The stone cracks of the Gates give off sparks and the odd drop of molten rock. */
    private void embers(ServerLevel sl) {
        Vec3 c = position().add(0, getBbHeight() * 0.55, 0);
        Fx.spray(sl, FxThemes.BOSS_GOLIATH, FxOld.SMALL_FLAME, c.x, c.y, c.z, 3, 0.9, 1.6, 0.9, 0.01);
        if (tickCount % 24 == 0) Fx.spray(sl, FxThemes.BOSS_GOLIATH, FxOld.LAVA, c.x, c.y + 0.5, c.z, 1, 0.8, 1.2, 0.8, 0.0);
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (GoliathTable.Spec s : GoliathTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            Move.MoveTell tell = tellOf(s.id());
            if (tell != null) b.tell(tell);
            out.add(b.run(runOf(s.id())));
        }
        return out;
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case GoliathTable.GROUND_SLAM -> (b, t) -> GoliathMoves.groundSlam(this, reach(GoliathTable.SLAM_RADIUS), GoliathTable.SLAM);
            case GoliathTable.TWIN_STOMP -> (b, t) -> GoliathMoves.twinStomp(this, reach(GoliathTable.STOMP_RADIUS), GoliathTable.STOMP_FIRST, GoliathTable.STOMP_SECOND);
            case GoliathTable.ROARING_SHOCKWAVE -> (b, t) -> GoliathMoves.shockwave(this, GoliathTable.SHOCKWAVE_REACH, GoliathTable.SHOCKWAVE);
            case GoliathTable.BEDROCK_QUAKE -> (b, t) -> GoliathMoves.bedrockQuake(this, GoliathTable.QUAKE_REACH, GoliathTable.QUAKE);
            case GoliathTable.BEDROCK_FIST -> (b, t) -> {
                List<Vec3> m = lockedFor(id);
                Vec3 mark = m.isEmpty() ? fistMark(t) : m.get(0);
                unlock();
                GoliathMoves.bedrockFist(this, mark, reach(GoliathTable.FIST_RADIUS), GoliathTable.FIST);
            };
            case GoliathTable.SEISMIC_LINE -> (b, t) -> {
                List<Vec3> m = lockedFor(id);
                Vec3[] ends = m.size() == 2 ? new Vec3[]{m.get(0), m.get(1)} : lineTo(t);
                unlock();
                java.util.Set<Integer> forkHit = new java.util.HashSet<>();
                for (double yaw : GoliathMath.forkAngles(gatesOpen())) {                          // one line, and at the Gates a fork of three that share their hit list
                    Vec3[] fe = GoliathMoves.rotatedEnds(ends[0], ends[1], yaw);
                    Vec3 along = fe[1].subtract(fe[0]);
                    double length = along.length();
                    if (length > 1e-3) GoliathMoves.seismicLine(this, fe[0], along.scale(1.0 / length), length, GoliathTable.LINE, forkHit);
                }
            };
            case GoliathTable.RUBBLE_FALL -> (b, t) -> {
                List<Vec3> spots = lockedFor(id);
                if (spots.isEmpty() && level() instanceof ServerLevel sl) spots = rubbleSpots(sl, reach(GoliathTable.RUBBLE_RADIUS));
                unlock();
                GoliathMoves.rubbleFall(this, spots, reach(GoliathTable.RUBBLE_RADIUS), GoliathTable.RUBBLE);
            };
            default -> (b, t) -> { };
        };
    }

    // ================================================================ marks: where the big moves land
    private void lock(String id, List<Vec3> marks) {
        lockedFor = id;
        locked = marks;
    }

    private List<Vec3> lockedFor(String id) { return id.equals(lockedFor) ? locked : List.of(); }

    private void unlock() {
        lockedFor = "";
        locked = List.of();
    }

    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    private static boolean first(int left, int total) { return left >= total - 3; }

    /** Where the fist lands for this foe: on them, but no farther from the giant than its reach. */
    private Vec3 fistMark(LivingEntity target) {
        Vec3 look = getViewVector(1f);
        double[] m = GoliathMath.fistMark(getX(), getZ(), target.getX(), target.getZ(), reach(GoliathTable.FIST_REACH), look.x, look.z);
        return ground(level(), new Vec3(m[0], target.getY(), m[1]));
    }

    /** The two ends of the Seismic Line towards this foe: it starts just in front of the giant and runs past them a little. */
    private Vec3[] lineTo(LivingEntity target) {
        Vec3 flat = target.position().subtract(position()).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) flat = getViewVector(1f).multiply(1, 0, 1);
        if (flat.lengthSqr() < 1e-4) flat = new Vec3(0, 0, 1);
        Vec3 dir = flat.normalize();
        Vec3 from = position().add(dir.scale(1.4));
        return new Vec3[]{from, from.add(dir.scale(GoliathMath.lineLength(flat.length())))};
    }

    /**
     * The circles of the Rubble Fall: one at the feet of each foe (up on a gallery too), the rest spread over the pit; each on the ground where it
     * is (a spot over a void is dropped).
     */
    private List<Vec3> rubbleSpots(ServerLevel sl, double radius) {
        List<LivingEntity> foes = foes(44);
        List<double[]> at = new ArrayList<>();
        for (LivingEntity e : foes) at.add(new double[]{e.getX(), e.getZ()});
        Vec3 c = arenaCenter();
        List<double[]> plan = GoliathMath.rubbleSpots(at, GoliathMath.rubbleCount(foes.size(), tuning().castSlots(), gatesOpen()), c.x, c.z, GoliathTable.ARENA_R, radius * 1.8,
                getX(), getZ(), getRandom().nextDouble() * Math.PI * 2);
        List<Vec3> out = new ArrayList<>();
        for (double[] s : plan) {
            LivingEntity foe = null;
            for (LivingEntity e : foes) if (e.getX() == s[0] && e.getZ() == s[1]) { foe = e; break; }
            Vec3 g = foe != null ? ground(sl, foe.position()) : groundOrNull(sl, new Vec3(s[0], c.y + 0.5, s[1]));
            if (g != null) out.add(g);
        }
        return out;
    }

    // ================================================================ telegraphs
    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case GoliathTable.GROUND_SLAM -> (b, t, left, total) -> tellRing(id, reach(GoliathTable.SLAM_RADIUS), left, total);
            case GoliathTable.TWIN_STOMP -> (b, t, left, total) -> tellRing(id, reach(GoliathTable.STOMP_RADIUS), left, total);
            case GoliathTable.ROARING_SHOCKWAVE -> (b, t, left, total) -> tellRing(id, GoliathTable.SHOCKWAVE_REACH, left, total);
            case GoliathTable.BEDROCK_QUAKE -> (b, t, left, total) -> tellRing(id, GoliathTable.QUAKE_REACH, left, total);
            case GoliathTable.BEDROCK_FIST -> (b, t, left, total) -> tellFist(t, left, total);
            case GoliathTable.SEISMIC_LINE -> (b, t, left, total) -> tellLine(t, left, total);
            case GoliathTable.RUBBLE_FALL -> (b, t, left, total) -> tellRubble(left, total);
            default -> null;
        };
    }

    /** The action-bar warning every foe within reach reads once, when a wind-up begins. */
    private void warn(ServerLevel sl, String id) {
        String w = GoliathTable.warning(id);
        if (w.isEmpty()) return;
        Component c = Component.literal(w).withStyle(ChatFormatting.GOLD);
        for (LivingEntity e : foes(48)) if (e instanceof ServerPlayer p) p.displayClientMessage(c, true);
    }

    /** A ring on the floor that grows from the giant's feet to the reach of the wave, the reach itself now and then, and dust drawn into its fists. */
    private void tellRing(String id, double reach, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            warn(sl, id);
            GoliathVfx.ringTell(this, reach, total);
            GoliathMoves.sound(sl, position(), GoliathTable.BEDROCK_QUAKE.equals(id) ? SoundEvents.WARDEN_SONIC_CHARGE : SoundEvents.STONE_HIT, 2.5f, 0.4f);
        }
        GoliathMoves.dustRing(sl, position(), GoliathMath.tellRadius(reach, left, total), GoliathMoves.DUST, 1.3f);
        if (left % 12 == 0) GoliathMoves.dustRing(sl, position(), reach, GoliathMoves.HOT, 1.1f);
        Tells.gather(sl, position().add(0, getBbHeight() * 0.7, 0), 1.4, GoliathMoves.AMBER, 4);
    }

    /** A circle on the floor under the foe, locked where it was when the wind-up began, and the fist's shadow closing in on it. */
    private void tellFist(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        double radius = reach(GoliathTable.FIST_RADIUS);
        if (first(left, total)) {
            warn(sl, GoliathTable.BEDROCK_FIST);
            lock(GoliathTable.BEDROCK_FIST, List.of(fistMark(target)));
            GoliathVfx.fistTell(this, lockedFor(GoliathTable.BEDROCK_FIST).get(0), radius, total);
            if (gatesOpen()) GoliathVfx.aftershockTell(this, lockedFor(GoliathTable.BEDROCK_FIST).get(0), GoliathTable.AFTERSHOCK_REACH, total);
            GoliathMoves.sound(sl, position(), SoundEvents.STONE_HIT, 2.5f, 0.4f);
        }
        List<Vec3> ms = lockedFor(GoliathTable.BEDROCK_FIST);
        if (ms.isEmpty()) return;
        Vec3 m = ms.get(0);
        GoliathMoves.dustRing(sl, m, radius, GoliathMoves.HOT, 1.4f);
        GoliathMoves.dustRing(sl, m, Math.max(0.6, radius * left / Math.max(1, total)), GoliathMoves.DUST, 1.1f);
        Tells.column(sl, m, 5, GoliathMoves.AMBER);
    }

    /** The lane of the Seismic Line: its two edges, drawn from the giant to the end, locked where the foe was. */
    private void tellLine(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            warn(sl, GoliathTable.SEISMIC_LINE);
            Vec3[] ends = lineTo(target);
            lock(GoliathTable.SEISMIC_LINE, List.of(ends[0], ends[1]));
            for (double yaw : GoliathMath.forkAngles(gatesOpen())) {
                Vec3[] fe = GoliathMoves.rotatedEnds(ends[0], ends[1], yaw);
                GoliathVfx.lineTell(this, fe[0], fe[1], total);
            }
            GoliathMoves.sound(sl, position(), SoundEvents.STONE_HIT, 2.5f, 0.4f);
        }
        List<Vec3> ends = lockedFor(GoliathTable.SEISMIC_LINE);
        if (ends.size() != 2) return;
        for (double yaw : GoliathMath.forkAngles(gatesOpen())) {
            Vec3[] fe = GoliathMoves.rotatedEnds(ends.get(0), ends.get(1), yaw);
            Vec3 along = fe[1].subtract(fe[0]);
            if (along.lengthSqr() < 1e-4) continue;
            Vec3 d = along.normalize(), side = new Vec3(-d.z, 0, d.x).scale(GoliathTable.LINE_HALF_WIDTH);
            Vec3 a = fe[0].add(0, 0.15, 0), b = fe[1].add(0, 0.15, 0);
            Tells.line(sl, a.add(side), b.add(side), GoliathMoves.HOT);
            Tells.line(sl, a.subtract(side), b.subtract(side), GoliathMoves.HOT);
        }
        Tells.gather(sl, ends.get(0).add(0, 0.15, 0), 0.8, GoliathMoves.AMBER, 3);
    }

    /** A circle at each spot the rubble will fall on, locked when the wind-up began, with a smaller ring closing in on every one. */
    private void tellRubble(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        double radius = reach(GoliathTable.RUBBLE_RADIUS);
        if (first(left, total)) {
            warn(sl, GoliathTable.RUBBLE_FALL);
            lock(GoliathTable.RUBBLE_FALL, rubbleSpots(sl, radius));
            for (Vec3 s : lockedFor(GoliathTable.RUBBLE_FALL)) GoliathVfx.rubbleTell(this, s, radius, total);
            GoliathMoves.sound(sl, position(), SoundEvents.STONE_HIT, 2.5f, 0.4f);
        }
        int points = (int) Math.max(14, Math.min(28, radius * 6));
        boolean closing = left % 6 == 0;
        for (Vec3 s : lockedFor(GoliathTable.RUBBLE_FALL)) {
            Tells.ring(sl, s, radius, GoliathMoves.HOT, 1.3f, points);
            if (closing) Tells.ring(sl, s, Math.max(0.6, radius * left / Math.max(1, total)), GoliathMoves.DUST, 1.0f, Math.max(10, points / 2));
            if (closing) Tells.column(sl, s, 4, GoliathMoves.AMBER);
        }
    }

    // ================================================================ save data
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("GWake", wakeTicks);
        tag.putBoolean("GFell", fellPosted);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        wakeTicks = tag.contains("GWake") ? Math.max(0, Math.min(WOKEN, tag.getInt("GWake"))) : WOKEN;
        fellPosted = tag.getBoolean("GFell");
        syncRise();
    }
}
