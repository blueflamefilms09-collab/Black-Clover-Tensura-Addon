package com.newuniverse.nusmp.arc.sds;

import com.mojang.logging.LogUtils;
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
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Why: King, the Grizzly's Sin of Sloth, is the fifth of the owner's Sins and the one who fights from the AIR: fairy wings, the spear Chastiefol, and Disaster, his power
 * over life and death. The owner wants fights that "transcend standard stat checks", and his canon gives three of them: the Guardian (a bear of moss that turns blades
 * and arrows aside and is undone by ice), the Pollen Garden / Disaster field (the field strikes everyone not standing in a garden of pollen) and the Sunflower (a flower of
 * lanes). He stands on the Zogratis framework (moves with telegraphs, the power curve, the arena, the damage budget, the second health bar, the Charter drop, all through
 * {@link ArcBoss}) and adds these rules, each telegraphed and each with a counter the party can learn (the numbers are in {@link KingTable}, the rules in
 * {@link GuardianShell}, {@link SafeGardens}, {@link DisasterBeats}, {@link SunflowerMath} and {@link KingFlight}; the whole design and its sources are in
 * {@code docs/arc/king.md}):
 * <ul>
 *   <li><b>The air</b>: he hovers six blocks up, out of a sword's reach (a player reaches three blocks from an eye 1.6 above their feet), and drifts slower than a sprint; he comes down for his close moves, and a dive leaves him on the
 *       ground, exposed. Counter: ranged blows while he hovers, a charge when he lands.</li>
 *   <li><b>The Guardian</b>: for eight seconds blades, arrows and blasts land at 35%, fire at 50%, spells in full; ice or anti-magic ends it. His Four Arms are only
 *       swung while it stands.</li>
 *   <li><b>The field</b>: every beat the gardens of pollen show twenty-four ticks before the pulse, one always within a walk of every foe; whoever is outside a garden is
 *       struck. A burst of 8% of his health in three seconds ends the field and leaves him dazed.</li>
 * </ul>
 * Nothing here changes a block, spawns an entity, or heals him; the only hold on a player is Fossilization, two seconds before the balance law's cap.
 */
public class KingBoss extends ArcBoss {
    public static final ArcCharacter CHARACTER = KingSheet.CHARACTER;

    /** The entity type, registered under the character's id (the roster checks the path). */
    public static final DeferredHolder<EntityType<?>, EntityType<KingBoss>> TYPE = ArcEntities.boss("king", KingBoss::new, KingSheet.WIDTH, KingSheet.HEIGHT,
            KingSheet.LOOK, KingBoss::createAttributes);

    private static final int RGB = KingSheet.RGB, ACCENT = KingSheet.ACCENT, POLLEN = KingMoves.POLLEN, DEATH = KingMoves.DEATH, WARN = KingMoves.WARN, WHITE = 0xFFFFFF;
    private static final int GREEN = 0xFF000000 | RGB, GOLD = 0xFF000000 | ACCENT, DARK = 0xFF000000 | DEATH;
    /** The height he hovers at over a field (low enough for a jumping blade to reach, high enough to be a flier). */
    private static final double FIELD_HEIGHT = 3.4;
    private static final int DAZE_TICKS = 50;

    private final GuardianShell shell = new GuardianShell();
    private final BurstGauge fieldGauge = new BurstGauge(KingTable.BREAK_SPAN, 400);
    private final AimLock throwLock = new AimLock(0.4);
    private final Map<Integer, Long> parried = new HashMap<>();
    private final Set<String> taught = new HashSet<>();
    private final Set<Integer> fieldTold = new HashSet<>();
    private Vec3 throwAim, diveMark, fieldCentre, flowerCentre, slamMark;
    private List<Vec3> marks = List.of();
    private List<Vec3> gardens = List.of();
    private double flowerPhase;
    private boolean hovering, fieldUlt;
    private int strafeSign = 1, diveToken, fieldToken;
    private long nextFlip, diveUntil, landedUntil, fieldUntil, dazeUntil, lastQuip;

    public KingBoss(EntityType<KingBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(KingSheet.HEALTH, KingSheet.ATTACK, KingSheet.SPEED, KingSheet.ARMOR, KingSheet.TOUGHNESS);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    /** He is as big at the Gates as before: the second form is a different look (a halo, wider wings), not a bigger body. */
    @Override protected float formScale(int form) { return 1.0f; }

    /** A spear's jab is all the plain blow he has; the weight is in the moves. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }

    /** He wants to stand 7 to 13 blocks off, whatever his ready moves ask for. */
    @Override protected double preferredRange(double suggested) { return Math.max(7.0, Math.min(13.0, suggested)); }

    private Vec3 facing() {
        Vec3 v = getViewVector(1f).multiply(1, 0, 1);
        return v.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : v.normalize();
    }

    private Vec3 chest() { return position().add(0, getBbHeight() * 0.65, 0); }

    // ================================================================ lines
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Eighty percent. I am awake now. Do not make me regret it.";
        if (now >= 60 && old < 60) return "Sixty percent. Disaster is no longer a story I tell.";
        if (now >= 40 && old < 40) return "Forty percent. The spear is getting restless.";
        return "I use " + now + "% of myself.";
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
        announce("King fights from the air, out of a sword's reach. He comes down to dive and for his close moves, and a dive leaves him on the ground.");
        announce("In his fields the GREEN-GOLD rings are gardens of pollen: stand in one when the field pulses. Blades and arrows are turned aside by his Guardian: use spells or ice.");
    }

    /** The framework's end of a fight first (the story's defeat listeners hear of a win there), then his own state is cleared and he comes down. */
    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        resetState();
        releaseHover();
    }

    private void resetState() {
        shell.close();
        fieldGauge.reset();
        throwLock.reset();
        parried.clear();
        taught.clear();
        fieldTold.clear();
        throwAim = null;
        diveMark = null;
        fieldCentre = null;
        flowerCentre = null;
        slamMark = null;
        marks = List.of();
        gardens = List.of();
        diveToken++;
        fieldToken++;
        diveUntil = 0;
        landedUntil = 0;
        fieldUntil = 0;
        dazeUntil = 0;
    }

    @Override
    protected void onGatesOpened() {
        announce("King's true form: eight petals and a second Sunflower, six beats of Disaster, a field that pulses harder.");
        if (level() instanceof ServerLevel sl) {
            ZogratisVfx.halo(sl, this, 1.4, 240, RGB);
            VfxSpawn.send(sl, VfxShape.TREE_ROOTS, position(), position().add(0, 1, 0), GREEN, 60, 5.0f);
        }
    }

    // ================================================================ every tick: the hover, the shell, the field's rim
    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            long now = sl.getGameTime();
            tickFlight(sl, now);
            if (shell.isOpen(now) && tickCount % 10 == 0) {
                Tells.ring(sl, position().add(0, 0.1, 0), 2.6, RGB, 1.2f, 20);
                Tells.gather(sl, position().add(0, 1.0, 0), 1.0, RGB, 5);
            }
            if (now < fieldUntil && fieldCentre != null && tickCount % 10 == 0)
                Tells.ring(sl, fieldCentre.add(0, 0.1, 0), fieldUlt ? KingTable.DISASTER_RADIUS : KingTable.POLLEN_RADIUS, DEATH, 1.3f, 64);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] King's tick failed: {}", e.toString());
        }
    }

    // ---------------------------------------------------------------- the hover
    /** The height above the ground he wants now. */
    private double wantedHeight(long now) {
        if (now < landedUntil) return KingFlight.LANDED;
        if (now < fieldUntil) return FIELD_HEIGHT;
        Move c = castingMoveOrNull();
        if (c != null && (c.id().equals(KingTable.WING_GUST) || c.id().equals(KingTable.FOSSILIZATION) || c.id().equals(KingTable.GUARDIAN_SLAM))) return KingFlight.LOW;
        if (shell.isOpen(now)) return KingFlight.LOW;
        return KingFlight.HIGH + KingFlight.bob(tickCount);
    }

    /** Hover (gravity off, the height steered), unless a dive steers him, there is no ground within the probe, or the fight is not on (then he falls like any mob). */
    private void tickFlight(ServerLevel sl, long now) {
        LivingEntity t = getTarget();
        boolean fighting = isAlive() && t != null && t.isAlive() && !transforming();
        if (!fighting) {
            releaseHover();
            return;
        }
        if (now < diveUntil) return;                                            // the dive's own steps move him
        Vec3 from = position().add(0, 0.3, 0);
        BlockHitResult hit = sl.clip(new ClipContext(from, from.add(0, -KingFlight.PROBE - 1, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        double gd = hit.getType() == HitResult.Type.MISS ? Double.NaN : getY() - hit.getLocation().y;
        if (!KingFlight.canHover(gd)) {                                           // nothing below: he falls like any mob, he never drifts into the sky
            releaseHover();
            return;
        }
        setNoGravity(true);
        hovering = true;
        fallDistance = 0;
        double vx = 0, vz = 0;
        boolean still = castingMoveOrNull() != null || exposed() || now < dazeUntil || now < fieldUntil || now < landedUntil;
        if (!still) {
            if (now >= nextFlip) {
                strafeSign = -strafeSign;
                nextFlip = now + 60 + getRandom().nextInt(80);
            }
            Vec3 dir = FourSinsKit.flatDir(position(), t.position(), facing());
            double dist = Math.hypot(t.getX() - getX(), t.getZ() - getZ());
            double[] d = KingFlight.drift(dist, 7.0, 13.0, strafeSign);
            vx = dir.x * d[0] - dir.z * d[1];
            vz = dir.z * d[0] + dir.x * d[1];
        }
        setDeltaMovement(vx, KingFlight.verticalSpeed(gd, wantedHeight(now)), vz);
        getNavigation().stop();
    }

    private void releaseHover() {
        if (!hovering && !isNoGravity()) return;                                 // also frees a boss that was saved in the air and loaded after the fight ended
        hovering = false;
        setNoGravity(false);
        fallDistance = 0;
    }

    // ================================================================ the defences: the Guardian, the field's break
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            if (source.is(DamageTypeTags.IS_FALL)) return 0f;                      // coming down from the air is not a wound
            long now = sl.getGameTime();
            boolean anti = FourSinsKit.isAntiMagic(source);
            DamageKind kind = FourSinsKit.kindOf(source);
            float a = amount;
            if (!exposed()) {
                GuardianShell.Verdict v = shell.judge(now, kind, anti, a);
                switch (v.result()) {
                    case PARRIED, DAMPED -> {
                        a = v.passed();
                        parryFeedback(sl, now, source, kind);
                    }
                    case THAWED -> {
                        a = v.passed();
                        shellEnds(sl, now, "Ice freezes the Guardian's moss solid: the shell cracks and King is staggered!", KingTable.THAW_DAZE);
                    }
                    case BROKEN -> {
                        a = v.passed();
                        shellEnds(sl, now, "Anti-magic unmakes the Guardian!", 20);
                    }
                    default -> { }
                }
            }
            if (now < dazeUntil) a *= KingTable.DAZED_TAKES;
            if (now < fieldUntil && fieldGauge.hit(now, a, KingTable.BREAK_FRACTION * getMaxHealth(), anti)) breakField(sl, now);
            return a;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] King's defence failed: {}", e.toString());
            return amount;
        }
    }

    /** A spark so the player sees the moss work, and (at most every two seconds per attacker) a line on their action bar saying what to use instead. */
    private void parryFeedback(ServerLevel sl, long now, DamageSource source, DamageKind kind) {
        Tells.burst(sl, position().add(0, getBbHeight() * 0.5, 0), RGB, 4);
        if (source.getEntity() instanceof LivingEntity att && now - parried.getOrDefault(att.getId(), -1000L) >= 40) {
            parried.put(att.getId(), now);
            String what = kind == DamageKind.FIRE ? "Fire is damped by the Guardian's moss (50%). Use spells or ice."
                    : "The Guardian's moss turns your " + kind.label() + " aside (65%). Use spells, or freeze it with ice.";
            FourSinsKit.note(att, what, ChatFormatting.GREEN);
        }
    }

    private void shellEnds(ServerLevel sl, long now, String line, int daze) {
        dazeUntil = Math.max(dazeUntil, now + daze);
        Move casting = castingMoveOrNull();
        if (casting != null && !casting.ultimate()) interruptCast();
        VfxSpawn.send(sl, VfxShape.TREE_ROOTS, position(), position().add(0, 1, 0), GREEN, 30, 3.0f);
        FourSinsKit.sound(sl, position(), SoundEvents.GLASS_BREAK, 2f, 0.8f);
        announce(line);
    }

    private void breakField(ServerLevel sl, long now) {
        fieldToken++;
        fieldUntil = 0;
        gardens = List.of();
        dazeUntil = now + DAZE_TICKS;
        Vec3 c = fieldCentre != null ? fieldCentre : position();
        ZogratisVfx.spaceShatter(sl, c.add(0, 1, 0), fieldUlt ? 6.0 : 4.0, RGB);
        FourSinsKit.sound(sl, c, SoundEvents.GLASS_BREAK, 3f, 0.6f);
        announce("King's concentration breaks: the " + (fieldUlt ? "Disaster" : "Pollen Garden") + " ends and he is staggered!");
        say("...Fine. That was rude.");
    }

    /** He does not start a move while he is dazed, diving, or holding a field; the Four Arms only while the Guardian stands, and the Guardian not while it already does. */
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        long now = level().getGameTime();                                           // ({@code t} is the framework's own clock, the boss's age)
        if (now < dazeUntil || now < fieldUntil || now < diveUntil) return -1;
        boolean up = shell.isOpen(now);
        List<Move> allowed = new ArrayList<>();
        List<Integer> index = new ArrayList<>();
        for (int i = 0; i < bk.size(); i++) {
            String id = bk.get(i).id();
            if (id.equals(KingTable.GUARDIAN_SLAM) && !up) continue;
            if (id.equals(KingTable.GUARDIAN) && up) continue;
            allowed.add(bk.get(i));
            index.add(i);
        }
        int j = super.chooseMove(allowed, s, t);
        return j < 0 ? -1 : index.get(j);
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (MoveRow r : KingTable.rows()) {
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
            case KingTable.CHASTIEFOL_THROW -> (bs, t) -> runThrow(t);
            case KingTable.INCREASE_RAIN -> (bs, t) -> runRain(t);
            case KingTable.DIVE_STRIKE -> (bs, t) -> runDive(t);
            case KingTable.WING_GUST -> (bs, t) -> {
                Vec3 dir = FourSinsKit.flatDir(position(), t.position(), facing());
                if (KingMoves.gust(this, position(), dir) > 0) quip("Please keep your distance. It is tiring otherwise.");
            };
            case KingTable.POLLEN_GARDEN -> (bs, t) -> runField(false);
            case KingTable.GUARDIAN -> (bs, t) -> runGuardian();
            case KingTable.GUARDIAN_SLAM -> (bs, t) -> {
                KingMoves.slam(this, slamMark != null ? slamMark : ZogratisBoss.ground(level(), t.position()));
                slamMark = null;
            };
            case KingTable.SUNFLOWER -> (bs, t) -> runSunflower();
            case KingTable.FOSSILIZATION -> (bs, t) -> {
                Vec3 dir = FourSinsKit.flatDir(position(), t.position(), facing());
                if (KingMoves.fossilize(this, position(), dir) > 0) quip("Stay still. It will be over soon.");
            };
            case KingTable.DISASTER -> (bs, t) -> runField(true);
            default -> (bs, t) -> { };
        };
    }

    private void runThrow(LivingEntity target) {
        Vec3 from = chest();
        Vec3 aim = throwAim != null ? throwAim : target.getBoundingBox().getCenter();
        Vec3 dir = aim.subtract(from);
        dir = dir.lengthSqr() < 1e-4 ? getViewVector(1f) : dir.normalize();
        throwAim = null;
        throwLock.reset();
        KingMoves.throwSpear(this, from, dir);
        quip("Chastiefol, go.", "Hold still. It is faster than you.");
    }

    private void runRain(LivingEntity target) {
        List<Vec3> spears = marks.isEmpty() ? pickRain(target) : marks;
        marks = List.of();
        Set<Integer> struck = new HashSet<>();
        for (int i = 0; i < spears.size(); i++) {
            final Vec3 at = spears.get(i);
            later(i * 3, KingMoves.safe("spear", () -> KingMoves.rainSpear(this, at, struck)));
        }
        quip("Increase.", "There are more of them than you think.");
    }

    /** The spears of one rain: on the target, on the other foes, then on a ring round the target, never stacked. */
    private List<Vec3> pickRain(LivingEntity target) {
        List<double[]> others = new ArrayList<>();
        for (LivingEntity e : foes(44)) if (e != target) others.add(new double[]{e.getX(), e.getZ()});
        double[][] plan = FourSinsMath.spreadMarks(new double[]{target.getX(), target.getZ()}, others.toArray(new double[0][]),
                KingTable.rainCount(tuning().castSlots()), 5.0, KingTable.RAIN_RADIUS * 1.7, getRandom().nextDouble() * Math.PI * 2);
        List<Vec3> out = new ArrayList<>();
        for (double[] m : plan) out.add(ZogratisBoss.ground(level(), new Vec3(m[0], target.getY(), m[1])));
        return out;
    }

    private void runDive(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 mark = diveMark != null ? diveMark : ZogratisBoss.ground(sl, target.position());
        diveMark = null;
        final double[] from = {getX(), getY(), getZ()};
        final double[] to = {mark.x, mark.y + 0.05, mark.z};
        double dist = Math.sqrt(Math.pow(to[0] - from[0], 2) + Math.pow(to[1] - from[1], 2) + Math.pow(to[2] - from[2], 2));
        final int n = KingFlight.diveTicks(dist);
        long now = sl.getGameTime();
        diveUntil = now + n + 3;
        landedUntil = now + n + 3 + 50;
        final int token = ++diveToken;
        setNoGravity(true);
        hovering = true;
        quip("Down.", "Gravity does the rest.");
        SpellRuntime.zone(sl, n + 1, 1, KingMoves.safe("dive", k -> diveStep(sl, token, from, to, k, n)));
    }

    private void diveStep(ServerLevel sl, int token, double[] from, double[] to, int k, int n) {
        if (token != diveToken || !isAlive() || isRemoved()) return;
        if (k < n) {
            double[] next = KingFlight.divePoint(from, to, k + 1, n);
            Vec3 step = new Vec3(next[0] - getX(), next[1] - getY(), next[2] - getZ());
            setDeltaMovement(step);
            hurtMarked = true;
            fallDistance = 0;
            getLookControl().setLookAt(to[0], to[1], to[2]);
            if (k % 2 == 0) Tells.gather(sl, position().add(0, 0.8, 0), 0.4, ACCENT, 4);
        } else if (k == n) {
            setDeltaMovement(Vec3.ZERO);
            KingMoves.diveImpact(this, position());
            diveUntil = sl.getGameTime();
            announce("King lands: he is exposed for a moment. Strike him now.");
        }
    }

    private void runGuardian() {
        long now = level().getGameTime();
        shell.open(now, KingTable.GUARDIAN_TICKS);
        if (level() instanceof ServerLevel sl) {
            VfxSpawn.send(sl, VfxShape.TREE_ROOTS, position(), position().add(0, 1, 0), GREEN, 50, 3.4f);
            FourSinsKit.sound(sl, position(), SoundEvents.MOSS_PLACE, 2.2f, 0.6f);
        }
        announce("Chastiefol: Guardian. A bear of moss stands over King for " + KingTable.GUARDIAN_TICKS / 20 + " seconds: blades, arrows and blasts land at 35%, fire at 50%. "
                + "Spells land in full. Ice or anti-magic ends it.");
        quip("Guardian, if you would.");
    }

    private void runField(boolean ult) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 c = fieldCentre != null ? fieldCentre : ZogratisBoss.ground(sl, position());
        fieldCentre = c;
        fieldUlt = ult;
        boolean gates = gatesOpen();
        final DisasterBeats.Spec spec = ult ? KingTable.disasterSpec(gates) : KingTable.POLLEN;
        final double radius = ult ? KingTable.DISASTER_RADIUS : KingTable.POLLEN_RADIUS;
        final float damage = ult ? KingTable.disasterPulse(gates) : KingTable.POLLEN_PULSE;
        long now = sl.getGameTime();
        fieldUntil = now + spec.total() + 2;
        fieldGauge.reset();
        fieldTold.clear();
        gardens = List.of();
        final int token = ++fieldToken;
        if (ult) VfxSpawn.send(sl, VfxShape.TREE_CANOPY, c, c.add(0, 1, 0), GREEN, 60, 6.0f);                 // the Sacred Tree watches, for a moment
        FourSinsKit.sound(sl, c, SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.5f);
        teach(ult ? "disaster" : "pollen", (ult ? "Disaster" : "Pollen Garden") + ": a field of death closes round " + (ult ? "the arena" : "the target") + ". Every "
                + spec.period() / 20.0 + " seconds green-gold gardens of pollen show; when the field pulses, only those standing in one are spared. A burst of "
                + Math.round(KingTable.BREAK_FRACTION * 100) + "% of King's health in three seconds ends it.");
        say(ult ? "This is Disaster. Find the pollen." : "Come into the garden.");
        SpellRuntime.zone(sl, spec.total(), 1, KingMoves.safe("field", age -> fieldStep(sl, token, spec, radius, damage, age)));
    }

    /** One tick of a field: at the start of a beat the gardens are planned and shown, they are redrawn every third tick, and the pulse lands when the beat's warning is over. */
    private void fieldStep(ServerLevel sl, int token, DisasterBeats.Spec spec, double radius, float damage, int age) {
        if (token != fieldToken || !isAlive() || isRemoved() || fieldCentre == null) return;
        if (DisasterBeats.showsAt(age, spec)) {
            gardens = planGardens(sl, radius);
            // the pool of shadow is sent once a beat and lasts that beat only, so a field that is broken leaves at most one beat of it behind
            VfxSpawn.send(sl, VfxShape.SHADOW_POOL, fieldCentre, fieldCentre.add(0, 1, 0), DARK, spec.period() + 4, (float) radius);
        }
        int left = DisasterBeats.warnLeft(age, spec);
        if (left > 0 && age % 3 == 0) KingMoves.drawGardens(sl, gardens, left, spec.warn());
        if (DisasterBeats.pulsesAt(age, spec)) {
            KingMoves.pulse(this, fieldCentre, radius, gardens, damage, fieldTold);
            gardens = List.of();
        }
    }

    /** The gardens of one beat: one within a walk of every foe in the field, more up to the count, on floor. */
    private List<Vec3> planGardens(ServerLevel sl, double radius) {
        List<LivingEntity> in = new ArrayList<>();
        for (LivingEntity e : foesAround(fieldCentre, radius + 2)) if (Math.hypot(e.getX() - fieldCentre.x, e.getZ() - fieldCentre.z) <= radius && in.size() < 8) in.add(e);
        double[][] pos = new double[in.size()][];
        for (int i = 0; i < in.size(); i++) pos[i] = new double[]{in.get(i).getX(), in.get(i).getZ()};
        int count = fieldUlt ? KingTable.disasterGardens(in.size()) : KingTable.pollenGardens(in.size());
        List<SafeGardens.Garden> plan = SafeGardens.plan(pos, fieldCentre.x, fieldCentre.z, radius, count, KingTable.GARDEN_RADIUS, KingTable.MAX_WALK, KingTable.GARDEN_GAP,
                new Random(getRandom().nextLong()), (x, z) -> ZogratisBoss.groundOrNull(sl, new Vec3(x, fieldCentre.y, z)) != null);
        List<Vec3> out = new ArrayList<>();
        for (SafeGardens.Garden g : plan) {
            Vec3 floor = ZogratisBoss.groundOrNull(sl, new Vec3(g.x(), fieldCentre.y, g.z()));
            if (floor != null) out.add(floor);
        }
        return out;
    }

    private void runSunflower() {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 c = flowerCentre != null ? flowerCentre : ZogratisBoss.ground(sl, position());
        flowerCentre = null;
        boolean gates = gatesOpen();
        int petals = KingTable.petalCount(gates);
        final double phase = flowerPhase;
        KingMoves.fireSunflower(this, c, petals, phase);
        quip("Sunflower.", "Look at the flower. Not at me.");
        if (KingTable.sunflowerVolleys(gates) > 1) {
            final double second = phase + Math.PI / petals;                               // turned half a petal, so the first volley's safe place is not the second's
            SpellRuntime.zone(sl, 24, 3, KingMoves.safe("sunflower tell", age -> KingMoves.drawPetals(sl, c, petals, second, WHITE)));
            later(24, KingMoves.safe("sunflower second", () -> KingMoves.fireSunflower(this, c, petals, second)));
        }
    }

    // ================================================================ telegraphs
    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case KingTable.CHASTIEFOL_THROW -> (bs, t, left, total) -> tellThrow(t, left, total);
            case KingTable.INCREASE_RAIN -> (bs, t, left, total) -> tellRain(t, left, total);
            case KingTable.DIVE_STRIKE -> (bs, t, left, total) -> tellDive(t, left, total);
            case KingTable.WING_GUST -> (bs, t, left, total) -> {
                if (level() instanceof ServerLevel sl) FourSinsKit.coneTell(sl, position(), FourSinsKit.flatDir(position(), t.position(), facing()), KingTable.GUST_RANGE, KingTable.GUST_HALF, RGB);
            };
            case KingTable.POLLEN_GARDEN -> (bs, t, left, total) -> tellField(t, left, total, false);
            case KingTable.GUARDIAN -> (bs, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                Tells.ring(sl, position(), 2.8, RGB, 1.3f, 24);
                Tells.gather(sl, position().add(0, 1, 0), 1.2, RGB, 8);
            };
            case KingTable.GUARDIAN_SLAM -> (bs, t, left, total) -> tellSlam(t, left, total);
            case KingTable.SUNFLOWER -> (bs, t, left, total) -> tellSunflower(t, left, total);
            case KingTable.FOSSILIZATION -> (bs, t, left, total) -> {
                if (level() instanceof ServerLevel sl) FourSinsKit.coneTell(sl, position(), FourSinsKit.flatDir(position(), t.position(), facing()), KingTable.FOSSIL_RANGE, 0.7, KingMoves.STONE);
            };
            case KingTable.DISASTER -> (bs, t, left, total) -> tellField(t, left, total, true);
            default -> null;
        };
    }

    /** The spear's lane follows the target for 60% of the wind-up, then locks (white): after that the way out is sideways. */
    private void tellThrow(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            throwLock.reset();                                                          // a lane left locked by an interrupted wind-up is forgotten
            throwAim = null;
        }
        Vec3 from = chest();
        boolean locked = throwLock.update(left, total, from.x, from.y, from.z, target.getX(), target.getZ());
        if (!locked || throwAim == null) throwAim = target.getBoundingBox().getCenter();
        Vec3 dir = throwAim.subtract(from);
        if (dir.lengthSqr() < 1e-4) return;
        dir = dir.normalize();
        Vec3 end = KingMoves.spearEnd(this, from, dir);
        Tells.line(sl, from, end, locked ? WHITE : ACCENT);
        FourSinsKit.markTell(sl, ZogratisBoss.ground(sl, end), KingTable.THROW_BURST_RADIUS, left, total, locked ? WHITE : RGB, ACCENT);
        Tells.gather(sl, from, 0.4, ACCENT, 3);
    }

    private void tellRain(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            marks = pickRain(target);
            announce("Increase: " + marks.size() + " spears. Leave the rings.");
        }
        for (Vec3 m : marks) {
            Tells.ring(sl, m, KingTable.RAIN_RADIUS, WARN, 1.3f, 24);
            Tells.ring(sl, m, Math.max(0.5, KingTable.RAIN_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 18);
            Tells.column(sl, m, 5, ACCENT);
        }
    }

    private void tellDive(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            diveMark = ZogratisBoss.ground(sl, target.position());
            teach("dive", "Fairy Dive: King drops on the marked ring. Leave it, then strike him while he is on the ground.");
        }
        if (diveMark == null) return;
        Tells.line(sl, chest(), diveMark.add(0, 0.4, 0), WARN);
        FourSinsKit.markTell(sl, diveMark, KingTable.DIVE_RADIUS, left, total, WARN, ACCENT);
    }

    private void tellSlam(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) slamMark = ZogratisBoss.ground(sl, target.position());
        if (slamMark == null) return;
        FourSinsKit.markTell(sl, slamMark, KingTable.SLAM_RADIUS, left, total, RGB, ACCENT);
    }

    private void tellSunflower(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            flowerCentre = ZogratisBoss.ground(sl, target.position());
            flowerPhase = getRandom().nextDouble() * Math.PI * 2;
            teach("sunflower", "Chastiefol: Sunflower. Beams leave the flower along its petals; stand BETWEEN two of them, not on one and not at the centre.");
            VfxSpawn.send(sl, VfxShape.LIGHT_FLARE, flowerCentre.add(0, 1.6, 0), flowerCentre.add(0, 1.6, 0), GOLD, total, 0.8f);
        }
        if (flowerCentre == null) return;
        KingMoves.drawPetals(sl, flowerCentre, KingTable.petalCount(gatesOpen()), flowerPhase, ACCENT);
    }

    private void tellField(LivingEntity target, int left, int total, boolean ult) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            fieldCentre = ZogratisBoss.ground(sl, target.position());
            fieldUlt = ult;
            teach(ult ? "disaster-tell" : "pollen-tell", (ult ? "Disaster" : "Pollen Garden") + ": the ring on the ground is the field. When it closes, stay where the green-gold gardens show.");
            FourSinsKit.sound(sl, fieldCentre, SoundEvents.WARDEN_SONIC_CHARGE, 2.5f, 0.7f);
        }
        if (fieldCentre == null) return;
        double radius = ult ? KingTable.DISASTER_RADIUS : KingTable.POLLEN_RADIUS;
        Tells.ring(sl, fieldCentre.add(0, 0.1, 0), radius, DEATH, 1.4f, 64);
        Tells.ring(sl, fieldCentre.add(0, 0.1, 0), Math.max(1.0, radius * left / Math.max(1, total)), ACCENT, 1.1f, 40);
        Tells.gather(sl, chest(), 1.0, POLLEN, 8);
    }
}
