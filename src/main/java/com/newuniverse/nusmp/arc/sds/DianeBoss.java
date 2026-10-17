package com.newuniverse.nusmp.arc.sds;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.sds.kit.AimLock;
import com.newuniverse.nusmp.arc.sds.kit.BurstGauge;
import com.newuniverse.nusmp.arc.sds.kit.DamageKind;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsKit;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsMath;
import com.newuniverse.nusmp.arc.sds.kit.MoveRow;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.boss.vfx.BossMoveVfx;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.ChatFormatting;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: Diane, the Serpent's Sin of Envy, is the last of the owner's four new Sins and the one whose fight is about SIZE: a giant with the hammer Gideon and the earth
 * power Creation, "a big slow body with earth magic and a size-change mechanic". The owner wants fights that "transcend standard stat checks", so her size is a rule a party
 * can read and use rather than a phase (the numbers are in {@link DianeTable}, the rules in {@link StanceClock}, {@link DanceStacks}, {@link MetalSkin},
 * {@link CrystalShield} and {@link WaveMath}; the whole design and its sources are in {@code docs/arc/diane.md}). She stands on the Zogratis framework (moves with
 * telegraphs, the power curve, the arena, the damage budget, the second health bar, the Charter drop, all through {@link ArcBoss}) and adds:
 * <ul>
 *   <li><b>Two sizes</b>: she opens at human size and grows (with a quake you can see coming) into a giant whose hitbox is 1.95 times as big, who is slow, wide and takes
 *       10% less. A burst of 9% of her health in four seconds CUTS HER DOWN TO SIZE: ten seconds of a human Diane who takes 15% more, then she grows again. If there is no
 *       room to grow she stays human. Each size has its own moves.</li>
 *   <li><b>Heavy Metal and the Diamond Shield</b>: metal cuts every blow by a flat amount (so a swarm of small hits is walled and a big hit passes), and fire melts it;
 *       the shield is a pool of damage that a burst breaks, and she is open afterwards.</li>
 *   <li><b>Drole's Dance</b> (her ultimate, as a giant): five stomps whose rings can be JUMPED, a stack of power for every beat she completes; a burst that cuts her down
 *       to size interrupts it and takes the stacks back.</li>
 * </ul>
 * Nothing here holds a player, changes a block, spawns an entity, or heals her.
 */
public class DianeBoss extends ArcBoss {
    public static final ArcCharacter CHARACTER = DianeSheet.CHARACTER;

    /** The entity type, registered under the character's id (the roster checks the path). It is her HUMAN size; a giant is {@link DianeTable#SCALE_GIANT} times as big. */
    public static final DeferredHolder<EntityType<?>, EntityType<DianeBoss>> TYPE = ArcEntities.boss("diane", DianeBoss::new, DianeSheet.WIDTH, DianeSheet.HEIGHT,
            DianeSheet.LOOK, DianeBoss::createAttributes);

    /** Synced to the clients: is she a giant? (The size of her hitbox follows it, on both sides, and the renderer scales the model by the hitbox.) */
    private static final EntityDataAccessor<Boolean> GIANT = SynchedEntityData.defineId(DianeBoss.class, EntityDataSerializers.BOOLEAN);
    private static final ResourceLocation GIANT_SPEED = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "diane_giant_speed");
    private static final int RGB = DianeSheet.RGB, ACCENT = DianeSheet.ACCENT, AMBER = DianeMoves.AMBER, DUST = DianeMoves.DUST, WARN = DianeMoves.WARN, WHITE = 0xFFFFFF;
    private static final int METAL = 0xB8BCC4, CRYSTAL = 0x9AE6FF;

    private final StanceClock stance = new StanceClock();
    private final DanceStacks stacks = new DanceStacks();
    private final MetalSkin metal = new MetalSkin();
    private final CrystalShield crystal = new CrystalShield();
    private final BurstGauge cutGauge = new BurstGauge(DianeTable.CUT_SPAN, DianeTable.CUT_COOLDOWN);
    private final AimLock sweepLock = new AimLock(0.4), laneLock = new AimLock(0.5);
    private final Map<Integer, Long> noted = new HashMap<>();
    private final Set<String> taught = new HashSet<>();
    private Vec3 smashMark;
    private List<Vec3> marks = List.of();
    private boolean noRoomTold;
    private int danceToken, danceGained;
    private long dazeUntil, danceUntil, lastQuip;

    public DianeBoss(EntityType<DianeBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(DianeSheet.HEALTH, DianeSheet.ATTACK, DianeSheet.SPEED, DianeSheet.ARMOR, DianeSheet.TOUGHNESS);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ size
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder b) {
        super.defineSynchedData(b);
        b.define(GIANT, false);
    }

    /** Is she a giant now (the synced flag; false while the entity is still being built). */
    public boolean giant() {
        try {
            return entityData != null && entityData.get(GIANT);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** The hitbox of her size: the type's (human) box, times the form's scale, times the giant's. */
    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).scale(giant() ? (float) DianeTable.SCALE_GIANT : 1f);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (GIANT.equals(key)) refreshDimensions();
    }

    /** Sets her size: the flag, the hitbox, and a giant's slower walk (15% less). */
    private void setSize(boolean giant) {
        if (entityData.get(GIANT) != giant) entityData.set(GIANT, giant);
        refreshDimensions();
        AttributeInstance speed = getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        if (giant) speed.addOrReplacePermanentModifier(new AttributeModifier(GIANT_SPEED, -0.15, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else speed.removeModifier(GIANT_SPEED);
    }

    private static boolean fits(ServerLevel sl, DianeBoss who, double x, double y, double z, double w, double h) {
        return sl.noCollision(who, new AABB(x - w / 2, y, z - w / 2, x + w / 2, y + h, z + w / 2));
    }

    /** Is there room for her giant hitbox where she stands, or within a few blocks (she steps there)? A low ceiling or a wall means she stays human. */
    private boolean makeRoom(ServerLevel sl) {
        double w = DianeSheet.WIDTH * DianeTable.SCALE_GIANT, h = DianeSheet.HEIGHT * DianeTable.SCALE_GIANT;
        if (fits(sl, this, getX(), getY(), getZ(), w, h)) return true;
        for (double r : new double[]{1.5, 3.0, 4.5}) {
            for (int k = 0; k < 8; k++) {
                double a = k * Math.PI / 4, x = getX() + Math.cos(a) * r, z = getZ() + Math.sin(a) * r;
                Vec3 floor = ZogratisBoss.groundOrNull(sl, new Vec3(x, getY(), z));
                if (floor == null || Math.abs(floor.y - getY()) > 1.5 || !fits(sl, this, x, floor.y, z, w, h)) continue;
                teleportTo(x, floor.y, z);
                return true;
            }
        }
        return false;
    }

    // ================================================================ tuning
    /** She is as big at the Gates as before: the second form is a different look (gold lines, glowing scales), not a bigger body. */
    @Override protected float formScale(int form) { return 1.0f; }

    /** Her power from the dance (a little more damage for every stack) and the lighter blows of her human size. */
    @Override protected float damageScale() { return stacks.multiplier(level().getGameTime()) * (giant() ? 1.0f : 0.92f); }

    /** The plain swat: a giant's is the full blow, a human's a lighter one. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * (giant() ? 1.0f : 0.8f); }

    /** A giant wants to be 3.5 to 9 blocks off (her reach is her hammer), a human 5 to 14. */
    @Override protected double preferredRange(double suggested) { return giant() ? Math.max(3.5, Math.min(9.0, suggested)) : Math.max(5.0, Math.min(14.0, suggested)); }

    private Vec3 facing() {
        Vec3 v = getViewVector(1f).multiply(1, 0, 1);
        return v.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : v.normalize();
    }

    private Vec3 chest() { return position().add(0, getBbHeight() * 0.7, 0); }

    private Vec3 aimAt(LivingEntity t) {
        Vec3 a = t.getBoundingBox().getCenter().subtract(chest());
        return a.lengthSqr() < 1e-4 ? getViewVector(1f) : a.normalize();
    }

    // ================================================================ lines
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Eighty percent! Now we are dancing!";
        if (now >= 60 && old < 60) return "Sixty percent. Do you want to see Drole's Dance?";
        if (now >= 40 && old < 40) return "Forty percent. The ground is listening to me now.";
        return "I am at " + now + "%!";
    }

    /** One of {@code lines} now and then (at most every eight seconds), so the chat is not a wall of speech. */
    private void quip(String... lines) {
        if (tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    /** The first time a rule is needed in a fight it is told in full; afterwards the tell itself is the reminder. */
    private void teach(String key, String line) {
        if (taught.add(key)) announce(line);
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        resetState();
        setSize(false);
        stance.start(level().getGameTime());
        announce("Diane starts at human size and grows into a giant. A giant is slow and tough; hurt her for " + Math.round(DianeTable.CUT_FRACTION * 100)
                + "% of her health in four seconds and she is CUT DOWN TO SIZE.");
        announce("Her Heavy Metal cuts every blow by a fixed amount: use big hits or fire. Her Diamond Shield is a pool of damage: break it. Jump the rings of Drole's Dance.");
    }

    /** The framework's end of a fight first (the story's defeat listeners hear of a win there); a fight that was given up sets her back to human size. */
    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        resetState();
        if (!won) setSize(false);
    }

    private void resetState() {
        metal.close();
        crystal.close();
        cutGauge.reset();
        stacks.clear();
        sweepLock.reset();
        laneLock.reset();
        noted.clear();
        taught.clear();
        smashMark = null;
        marks = List.of();
        noRoomTold = false;
        danceToken++;
        danceGained = 0;
        danceUntil = 0;
        dazeUntil = 0;
    }

    @Override
    protected void onGatesOpened() {
        announce("Diane's true strength: a longer dance (seven beats), a harder time cutting her down to size, and she grows at once.");
        if (level() instanceof ServerLevel sl) {
            ZogratisVfx.halo(sl, this, 1.6, 240, RGB);
            BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, position(), position().add(0, 1, 0), DianeMoves.HOT, 6.0);
            stance.growNow(sl.getGameTime());
        }
    }

    // ================================================================ every tick
    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            long now = sl.getGameTime();
            if (now < danceUntil) getNavigation().stop();                              // she dances where she stands
            if (metal.isOpen(now) && tickCount % 10 == 0) {
                Tells.ring(sl, position().add(0, 0.1, 0), giant() ? 3.2 : 1.8, METAL, 1.2f, 22);
                Tells.gather(sl, position().add(0, getBbHeight() * 0.5, 0), 0.9, METAL, 4);
            }
            if (crystal.isOpen(now) && tickCount % 10 == 0) {
                Tells.ring(sl, position().add(0, 0.1, 0), giant() ? 3.4 : 2.0, CRYSTAL, 1.2f, 22);
                Tells.gather(sl, position().add(0, getBbHeight() * 0.5, 0), 1.0, CRYSTAL, 4);
            }
            if (stacks.stacks(now) > 0 && tickCount % 20 == 0) Tells.gather(sl, position().add(0, getBbHeight() + 0.3, 0), 0.4, AMBER, 2 + stacks.stacks(now));
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Diane's tick failed: {}", e.toString());
        }
    }

    // ================================================================ the defences: the metal, the shield, being cut down to size
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            long now = sl.getGameTime();
            boolean anti = FourSinsKit.isAntiMagic(source);
            DamageKind kind = FourSinsKit.kindOf(source);
            float a = amount;
            if (!exposed()) {
                MetalSkin.Verdict mv = metal.judge(now, kind, a, DianeTable.METAL_FLAT_FRACTION * getMaxHealth(), DianeTable.METAL_MELT_FRACTION * getMaxHealth());
                switch (mv.result()) {
                    case REDUCED -> {
                        a = mv.passed();
                        clang(sl, now, source, "Diane's metal skin cuts each blow by " + Math.round(DianeTable.METAL_FLAT_FRACTION * getMaxHealth())
                                + ". Use big hits, or fire.");
                    }
                    case HEATED -> a = mv.passed();
                    case MELTED -> {
                        a = mv.passed();
                        dazeUntil = Math.max(dazeUntil, now + 20);
                        announce("The heat melts Diane's metal skin!");
                        FourSinsKit.sound(sl, position(), SoundEvents.LAVA_EXTINGUISH, 2f, 0.7f);
                    }
                    default -> { }
                }
                if (crystal.isOpen(now)) {
                    CrystalShield.Verdict cv = crystal.absorb(now, a);
                    a = cv.passed();
                    if (cv.absorbed() > 0f) Tells.burst(sl, position().add(0, getBbHeight() * 0.6, 0), CRYSTAL, 4);
                    if (cv.broke()) {
                        dazeUntil = Math.max(dazeUntil, now + DianeTable.DAZE_TICKS);
                        Move casting = castingMoveOrNull();
                        if (casting != null && !casting.ultimate()) interruptCast();
                        ZogratisVfx.spaceShatter(sl, position().add(0, getBbHeight() * 0.5, 0), 2.6, 0x9AE6FF);
                        FourSinsKit.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_BREAK, 3f, 0.6f);
                        announce("The Diamond Shield shatters: Diane is open!");
                    }
                }
            }
            if (giant()) a *= DianeTable.GIANT_TAKES;
            else if (stance.cutDown()) a *= DianeTable.CUT_TAKES;
            if (now < dazeUntil) a *= DianeTable.DAZED_TAKES;
            if (giant() && cutGauge.hit(now, a, DianeTable.cutFraction(gatesOpen()) * getMaxHealth(), anti)) cutDownNow(sl, now);
            return a;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Diane's defence failed: {}", e.toString());
            return amount;
        }
    }

    /** A spark so the player sees the metal work, and (at most every two seconds per attacker) a line on their action bar saying what to use instead. */
    private void clang(ServerLevel sl, long now, DamageSource source, String line) {
        Tells.burst(sl, position().add(0, getBbHeight() * 0.5, 0), METAL, 4);
        if (source.getEntity() instanceof LivingEntity att && now - noted.getOrDefault(att.getId(), -1000L) >= 40) {
            noted.put(att.getId(), now);
            FourSinsKit.note(att, line, ChatFormatting.GRAY);
        }
    }

    /** The party's burst: she is cut down to size at once. Whatever she was casting is lost, a dance in progress ends and takes its stacks back, and she is human for ten seconds. */
    private void cutDownNow(ServerLevel sl, long now) {
        Move casting = castingMoveOrNull();
        if (casting != null) interruptCast();
        if (now < danceUntil) endDance(now, true);
        stacks.clear();
        metal.close();
        crystal.close();
        setSize(false);
        stance.becomeHuman(now, true);
        dazeUntil = Math.max(dazeUntil, now + 20);
        Vec3 at = position();
        BossMoveVfx.strike(sl, VfxShape.BOSS_STONE_RING, at, at.add(0, 1, 0), DianeMoves.AMBER, 5.0);
        FourSinsKit.sound(sl, at, SoundEvents.GENERIC_EXPLODE.value(), 1.6f, 1.2f);
        announce("Diane is CUT DOWN TO SIZE! For " + StanceClock.CUT_DOWN_HUMAN / 20 + " seconds she is human sized: quicker, and she takes 15% more.");
        say("Hey! That is not fair! I was just getting started!");
    }

    // ================================================================ choosing: the clock's size changes first, then the moves of her size
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        long now = level().getGameTime();                                           // ({@code t} is the framework's own clock, the boss's age)
        if (now < dazeUntil || now < danceUntil) return -1;
        if (stance.wantsGrow(now)) {
            int i = indexOf(bk, DianeTable.GROW);
            if (i >= 0) return i;
        }
        if (stance.wantsShrink(now)) {
            int i = indexOf(bk, DianeTable.SHRINK);
            if (i >= 0) return i;
        }
        boolean defended = metal.isOpen(now) || crystal.isOpen(now);
        List<Move> allowed = new ArrayList<>();
        List<Integer> index = new ArrayList<>();
        boolean giant = stance.giant();
        for (int i = 0; i < bk.size(); i++) {
            String id = bk.get(i).id();
            MoveRow row = DianeTable.byId(id);
            if (row == null || DianeTable.isSizeChange(id)) continue;
            if (row.group() != DianeTable.ANY && row.group() != (giant ? DianeTable.GIANT : DianeTable.HUMAN)) continue;
            if (defended && (id.equals(DianeTable.HEAVY_METAL) || id.equals(DianeTable.DIAMOND_SHIELD))) continue;            // one defence at a time
            allowed.add(bk.get(i));
            index.add(i);
        }
        int j = super.chooseMove(allowed, s, t);
        return j < 0 ? -1 : index.get(j);
    }

    private static int indexOf(List<Move> bk, String id) {
        for (int i = 0; i < bk.size(); i++) if (bk.get(i).id().equals(id)) return i;
        return -1;
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (MoveRow r : DianeTable.rows()) {
            Move.Builder mb = Move.builder(r.id(), r.label()).minPower(r.minPower()).windUp(r.windUpTicks()).cooldown(r.cooldownTicks())
                    .range(r.minRange(), r.maxRange()).weight(r.weight()).mana(r.manaCost()).recovery(r.recovery());
            if (r.ultimate()) mb.ultimate();
            if (r.crowd()) mb.crowd();
            final Move.MoveTell own = tellOf(r.id());
            mb.tell((bs, t, left, total) -> {
                if (own != null) own.tell(bs, t, left, total);
                else if (level() instanceof ServerLevel sl && castingMoveOrNull() != null) defaultTell(sl, castingMoveOrNull(), t, left);
            });
            out.add(mb.run(runOf(r.id())));
        }
        return out;
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case DianeTable.GIDEON_SMASH -> (bs, t) -> {
                Vec3 at = smashMark != null ? smashMark : ZogratisBoss.ground(level(), t.position());
                smashMark = null;
                if (DianeMoves.smash(this, at, gatesOpen()) > 0) quip("Smash!", "Did that hurt? Sorry!");
            };
            case DianeTable.GIDEON_SWEEP -> (bs, t) -> {
                double[] d = sweepLock.has() ? sweepLock.dir() : null;
                Vec3 dir = d != null ? new Vec3(d[0], 0, d[1]) : FourSinsKit.flatDir(position(), t.position(), facing());
                sweepLock.reset();
                DianeMoves.sweep(this, position(), dir, gatesOpen());
                quip("Out of my way!");
            };
            case DianeTable.QUEEN_EMBRACE -> (bs, t) -> {
                double[] d = laneLock.has() ? laneLock.dir() : null;
                Vec3 dir = d != null ? new Vec3(d[0], 0, d[1]) : FourSinsKit.flatDir(position(), t.position(), facing());
                laneLock.reset();
                DianeMoves.queenEmbrace(this, position(), dir, gatesOpen());
                quip("Queen Embrace!", "Ride the wave!");
            };
            case DianeTable.RISING_METEOR -> (bs, t) -> runMeteor(t);
            case DianeTable.DROLE_DANCE -> (bs, t) -> runDance();
            case DianeTable.RUSH_ROCK -> (bs, t) -> {
                DianeMoves.rocks(this, aimAt(t), DianeTable.rockCount(tuning().castSlots()), DianeTable.ROCK);
                quip("Rush Rock!", "Catch!");
            };
            case DianeTable.HEAVY_METAL -> (bs, t) -> runMetal();
            case DianeTable.DIAMOND_SHIELD -> (bs, t) -> runCrystal();
            case DianeTable.HAMMER_SWING -> (bs, t) -> {
                Vec3 dir = FourSinsKit.flatDir(position(), t.position(), facing());
                DianeMoves.swing(this, position(), dir);
            };
            case DianeTable.GROW -> (bs, t) -> runGrow();
            case DianeTable.SHRINK -> (bs, t) -> runShrink();
            default -> (bs, t) -> { };
        };
    }

    private void runMeteor(LivingEntity target) {
        List<Vec3> pillars = marks.isEmpty() ? pickPillars(target) : marks;
        marks = List.of();
        Set<Integer> struck = new HashSet<>();
        boolean gates = gatesOpen();
        for (int i = 0; i < pillars.size(); i++) {
            final Vec3 at = pillars.get(i);
            later(i * 3, DianeMoves.safe("pillar", () -> DianeMoves.pillar(this, at, struck, gates)));
        }
        quip("Rising Meteor!", "The ground is on my side.");
    }

    /** The pillars of one volley: on the target, on the other foes, then on a ring round the target, never stacked. */
    private List<Vec3> pickPillars(LivingEntity target) {
        List<double[]> others = new ArrayList<>();
        for (LivingEntity e : foes(44)) if (e != target) others.add(new double[]{e.getX(), e.getZ()});
        double[][] plan = FourSinsMath.spreadMarks(new double[]{target.getX(), target.getZ()}, others.toArray(new double[0][]),
                DianeTable.meteorCount(tuning().castSlots()), 5.5, DianeTable.METEOR_RADIUS * 1.7, getRandom().nextDouble() * Math.PI * 2);
        List<Vec3> out = new ArrayList<>();
        for (double[] m : plan) out.add(ZogratisBoss.ground(level(), new Vec3(m[0], target.getY(), m[1])));
        return out;
    }

    private void runMetal() {
        long now = level().getGameTime();
        metal.open(now, DianeTable.METAL_TICKS);
        if (level() instanceof ServerLevel sl) FourSinsKit.sound(sl, position(), SoundEvents.ANVIL_PLACE, 2f, 0.6f);
        announce("Heavy Metal: Diane's skin is metal for " + DianeTable.METAL_TICKS / 20 + " seconds. Every blow loses " + Math.round(DianeTable.METAL_FLAT_FRACTION * getMaxHealth())
                + " (a fifth always gets through): big hits pass, swarms of small ones do not. Fire passes in full, and enough of it melts the metal.");
        quip("Heavy Metal!");
    }

    private void runCrystal() {
        long now = level().getGameTime();
        float pool = DianeTable.CRYSTAL_FRACTION * getMaxHealth();
        crystal.open(now, DianeTable.CRYSTAL_TICKS, pool);
        if (level() instanceof ServerLevel sl) {
            ZogratisVfx.divineSigil(sl, position(), giant() ? 3.0 : 1.8, 40, CRYSTAL, false);
            FourSinsKit.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_CHIME, 2.5f, 0.6f);
        }
        announce("Diamond Shield: a crystal barrier takes " + Math.round(pool) + " damage before it breaks, and Diane is open when it does. It lasts " + DianeTable.CRYSTAL_TICKS / 20 + " seconds.");
        quip("Diamond Shield!");
    }

    private void runGrow() {
        if (!(level() instanceof ServerLevel sl) || stance.giant()) return;
        long now = sl.getGameTime();
        if (!makeRoom(sl)) {
            stance.blockGrowth(now);
            if (!noRoomTold) {
                noRoomTold = true;
                announce("There is no room here for Diane to grow: she fights at human size, and tries again in a while.");
            }
            return;
        }
        setSize(true);
        stance.becomeGiant(now);
        DianeMoves.growQuake(this, position(), gatesOpen());
        announce("Diane grows to her true size: slow, wide and tough. Hurt her for " + Math.round(DianeTable.cutFraction(gatesOpen()) * 100)
                + "% of her health in four seconds to cut her down to size.");
        quip("This is my real size!", "Ta-da! Bigger, right?");
    }

    private void runShrink() {
        if (!(level() instanceof ServerLevel sl) || !stance.giant()) return;
        long now = sl.getGameTime();
        metal.close();
        crystal.close();
        setSize(false);
        stance.becomeHuman(now, false);
        stacks.clear();
        Tells.burst(sl, position().add(0, 1, 0), DUST, 30);
        FourSinsKit.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.2f);
        announce("Diane shrinks to human size: she is quicker now, and will grow again soon.");
        quip("Let me see you up close.");
    }

    // ---------------------------------------------------------------- Drole's Dance
    private void runDance() {
        if (!(level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        int beats = DianeTable.danceBeats(gatesOpen());
        int total = beats * DianeTable.BEAT_TICKS;
        danceUntil = now + total + WaveMath.ticksToReach(DianeTable.DANCE_RADIUS, DianeTable.DANCE_SPEED) + 4;
        danceGained = 0;
        final int token = ++danceToken;
        teach("dance", "Drole's Dance: " + beats + " stomps, one every second. A ring of earth runs out from her feet after each: JUMP it (or stay out of ten blocks). "
                + "Every stomp she completes makes her hit 6% harder; cut her down to size to stop the dance and take it all back.");
        say("Drole taught me this one!");
        SpellRuntime.zone(sl, total, 1, DianeMoves.safe("dance", age -> danceStep(sl, token, beats, age)));
    }

    /** One tick of the dance: the foot rises for {@link DianeTable#PREBEAT} ticks (drawn), then the stomp: a ring leaves her feet and she earns a stack. */
    private void danceStep(ServerLevel sl, int token, int beats, int age) {
        if (token != danceToken || !isAlive() || isRemoved()) return;
        int beat = age / DianeTable.BEAT_TICKS, into = age % DianeTable.BEAT_TICKS;
        if (beat >= beats) return;
        Vec3 feet = ZogratisBoss.ground(sl, position());
        if (into < DianeTable.PREBEAT) {
            if (into % 2 == 0) DianeMoves.drawBeat(sl, feet, into, into == 0);
        } else if (into == DianeTable.PREBEAT) {
            DianeMoves.stomp(this, feet, gatesOpen());
            danceGained++;
            stacks.add(sl.getGameTime(), 1);
            if (beat == beats - 1) announce("Drole's Dance is done: Diane hits " + Math.round((stacks.multiplier(sl.getGameTime()) - 1f) * 100) + "% harder, fading over the next seconds.");
        }
    }

    /** The dance ends now. When it was interrupted, the stacks it gave are taken back. */
    private void endDance(long now, boolean interrupted) {
        danceToken++;
        danceUntil = 0;
        if (interrupted) stacks.remove(now, danceGained);
        danceGained = 0;
    }

    // ================================================================ telegraphs
    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case DianeTable.GIDEON_SMASH -> (bs, t, left, total) -> tellSmash(t, left, total);
            case DianeTable.GIDEON_SWEEP -> (bs, t, left, total) -> tellSweep(t, left, total);
            case DianeTable.QUEEN_EMBRACE -> (bs, t, left, total) -> tellQueen(t, left, total);
            case DianeTable.RISING_METEOR -> (bs, t, left, total) -> tellMeteor(t, left, total);
            case DianeTable.DROLE_DANCE -> (bs, t, left, total) -> tellDance(left, total);
            case DianeTable.RUSH_ROCK -> (bs, t, left, total) -> tellRocks(t);
            case DianeTable.HEAVY_METAL -> (bs, t, left, total) -> {
                if (level() instanceof ServerLevel sl) Tells.gather(sl, position().add(0, getBbHeight() * 0.5, 0), 1.0, METAL, 8);
            };
            case DianeTable.DIAMOND_SHIELD -> (bs, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                Tells.ring(sl, position(), giant() ? 3.4 : 2.0, CRYSTAL, 1.2f, 24);
                Tells.gather(sl, position().add(0, getBbHeight() * 0.5, 0), 1.0, CRYSTAL, 8);
            };
            case DianeTable.HAMMER_SWING -> (bs, t, left, total) -> {
                if (level() instanceof ServerLevel sl) FourSinsKit.coneTell(sl, position(), FourSinsKit.flatDir(position(), t.position(), facing()), DianeTable.SWING_RANGE, DianeTable.SWING_HALF, WARN);
            };
            case DianeTable.GROW -> (bs, t, left, total) -> tellGrow(left, total);
            case DianeTable.SHRINK -> (bs, t, left, total) -> {
                if (level() instanceof ServerLevel sl) Tells.gather(sl, position().add(0, getBbHeight() * 0.5, 0), 1.6, DUST, 10);
            };
            default -> null;
        };
    }

    private void tellSmash(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            smashMark = ZogratisBoss.ground(sl, target.position());
            BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_RING, smashMark, smashMark.add(0, 1, 0), DianeMoves.tint(gatesOpen()), total, DianeTable.SMASH_RADIUS);
        }
        if (smashMark == null) return;
        FourSinsKit.markTell(sl, smashMark, DianeTable.SMASH_RADIUS, left, total, WARN, ACCENT);
    }

    /** The sweep's cone follows the target for 60% of the wind-up and then locks (white): after that the way out is behind her or beyond her reach. */
    private void tellSweep(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) sweepLock.reset();                              // a lane left locked by an interrupted wind-up is forgotten
        boolean locked = sweepLock.update(left, total, getX(), getY(), getZ(), target.getX(), target.getZ());
        double[] d = sweepLock.dir();
        FourSinsKit.coneTell(sl, position(), new Vec3(d[0], 0, d[1]), DianeTable.SWEEP_RANGE, DianeTable.SWEEP_HALF, locked ? WHITE : WARN);
    }

    /** The lane of Queen Embrace follows the target for half the wind-up and then locks (white); the wave then takes a second to run its length. */
    private void tellQueen(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) laneLock.reset();
        boolean locked = laneLock.update(left, total, getX(), getY(), getZ(), target.getX(), target.getZ());
        double[] d = laneLock.dir();
        DianeMoves.drawLane(sl, position(), new Vec3(d[0], 0, d[1]), locked ? WHITE : WARN);
        Tells.gather(sl, position().add(0, 0.4, 0), 1.2, DUST, 5);
    }

    private void tellMeteor(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            marks = pickPillars(target);
            announce("Rising Meteor: " + marks.size() + " pillars rise under the rings. Leave them.");
        }
        for (Vec3 m : marks) {
            Tells.ring(sl, m, DianeTable.METEOR_RADIUS, WARN, 1.3f, 24);
            Tells.ring(sl, m, Math.max(0.5, DianeTable.METEOR_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 18);
            Tells.column(sl, m, 4, ACCENT);
        }
    }

    private void tellDance(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 feet = ZogratisBoss.ground(sl, position());
        Tells.ring(sl, feet.add(0, 0.1, 0), DianeTable.DANCE_RADIUS, AMBER, 1.2f, 56);
        Tells.ring(sl, feet.add(0, 0.1, 0), Math.max(1.0, DianeTable.DANCE_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 36);
        Tells.gather(sl, position().add(0, getBbHeight() * 0.4, 0), 1.4, DUST, 8);
    }

    private void tellRocks(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = chest();
        Vec3 aim = aimAt(target);
        int n = DianeTable.rockCount(tuning().castSlots());
        for (double a : FourSinsMath.fan(n, Math.min(1.0, 0.14 * n))) Tells.line(sl, from, from.add(FourSinsKit.rotateY(aim, a).scale(14)), DianeMoves.ROCK);
        Tells.gather(sl, from, 0.5, DUST, 4);
    }

    private void tellGrow(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            BossMoveVfx.telegraph(sl, VfxShape.BOSS_STONE_RING, position(), position().add(0, 1, 0), DianeMoves.tint(gatesOpen()), total, DianeTable.GROW_RADIUS);
            teach("grow", "Diane is about to grow to giant size: the ring on the ground is the quake. Step out of it.");
        }
        Tells.ring(sl, position().add(0, 0.1, 0), DianeTable.GROW_RADIUS, WARN, 1.3f, 40);
        Tells.ring(sl, position().add(0, 0.1, 0), Math.max(1.0, DianeTable.GROW_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 28);
        Tells.gather(sl, position().add(0, getBbHeight() * 0.5, 0), 1.2, DUST, 8);
    }
}
