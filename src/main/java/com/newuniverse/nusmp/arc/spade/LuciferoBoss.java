package com.newuniverse.nusmp.arc.spade;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.entity.zogratis.BoneShell;
import com.newuniverse.nusmp.entity.zogratis.DarkDiscipleEntity;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.entity.zogratis.ZogratisVfx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: Lucifero, the King of Devils, is the fight the owner described as "the Captains against Lucifero: pure overwhelming gravity that flattens the
 * strongest fighters", and the mod already has gravity bosses (Dante's fields and spheres on marks, the Avatar's ceiling pull). A third boss that only
 * drops more circles would not be that fight. This one is gravity on the scale of the room, and it has three rules that only he has, each with a counter the
 * story itself used:
 * <ul>
 *   <li><b>The Bow</b> (and Kneel Before the King): he orders the room to kneel and the weight falls whether you do or not. Crouch (hold sneak) in the
 *       wind-up and you have bowed of your own will and are spared; hold anti-magic and the gravity finds nothing to grip; for the ultimate there are
 *       pockets of still air to stand in. Everyone else is crushed to the knees (three seconds at most: the Balance Law holds). After the Bow he is
 *       exposed.</li>
 *   <li><b>The Gravity Shroud</b>: his gravity "tears apart spells". While it stands, ranged hits and spells are halved. Enough melee or anti-magic tears
 *       it and leaves him staggered. The counter is to close in with a blade, as Asta did.</li>
 *   <li><b>Flattened Air</b>: for a while nobody jumps or glides. His lanes, rings and well are meant to be walked out of, never jumped.</li>
 * </ul>
 * He opens the fight with the Bow (as in the anime), calls lesser devils and swallows them for a shell of their power (the shell is a pool of damage that
 * runs out, never a heal), and at the Gates his four wings become arms beside his two hands (Six Limbs). He never moves much: he makes the room do the work. All damage goes through the
 * framework's damage law and Tensura's gravity damage types; hard holds ask the Balance Law first. Numbers: {@link LuciferoTable}, {@link WeightMath}.
 */
public class LuciferoBoss extends ArcBoss {
    public static final ArcCharacter CHARACTER = SpadeRoster.LUCIFERO;

    /**
     * How he is drawn: the numbers of {@code tools/arc/manifest.json} for {@code arc_lucifero} (walk and run strides, and the culling box and shadow of the BIGGER of his
     * two forms, the wings and horns of the full-power model). Copy them again whenever {@code tools/arc/char_lucifero.py} changes.
     */
    private static final ArcLook LOOK = new ArcLook("lucifero", 2.69f, 1.73f, 3.1f, 1.4f, 2.5f, 1.72f, true);
    /** A hitbox wider than the 0.55 the kit suggests for his thin body: he is a king, and the wings are only the picture. */
    public static final DeferredHolder<EntityType<?>, EntityType<LuciferoBoss>> TYPE =
            ArcEntities.boss("lucifero", LuciferoBoss::new, 0.8f, LOOK.height(), LOOK, LuciferoBoss::createAttributes);

    private static final String WHO = "Lucifero";
    private static final int RGB = LuciferoMoves.VIOLET_RGB, ACCENT = LuciferoMoves.ACCENT_RGB, WARN = LuciferoMoves.WARN_RGB;

    private final ShroudState shroud = new ShroudState();
    private final BoneShell shell = new BoneShell();
    private final Map<UUID, Long> crouched = new HashMap<>();
    private boolean shellUp, openingBowPending, shroudUp;
    private long flatUntil, staggerUntil, lastQuip, lastShellNote, horizonEnd;
    private List<Vec3> marks = List.of(), pockets = List.of();
    private Vec3 stepTo, horizon, combOrigin, aimDir;
    private int combLanes = 3;
    private double armPhase;
    private UUID wanderTarget;

    public LuciferoBoss(EntityType<LuciferoBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(CHARACTER.health(), CHARACTER.attack(), CHARACTER.speed(), CHARACTER.armor(), 12);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    @Override protected float formScale(int form) { return form == FORM_HYBRID ? 1.22f : 1.0f; }
    @Override protected float damageScale() { return 1.2f * SpadeConfig.luciferoDamage(); }
    /** A king's plain blow is a flick of the wrist next to the Palm, which is the real heavy hit. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f; }
    /** He does not chase: he stands where the room is his, five to ten blocks from the foe. */
    @Override protected double preferredRange(double suggested) { return Math.max(5, Math.min(10, suggested)); }

    @Override
    public String barTitle() {
        return bossName() + " - King of Devils " + power() + "%" + (gatesOpen() ? " - Gates open" : "");
    }

    // ================================================================ lines
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Do you feel it? That is not me pressing down. That is the world, remembering its weight.";
        if (now >= PowerCurve.INFINITE_MANA_FROM && old < PowerCurve.INFINITE_MANA_FROM) return "Kneel. Again. Slower this time, so that you can feel it.";
        if (now >= 40 && old < 40) return "Hm. You are still standing. Interesting.";
        return "I draw " + now + "% of my weight.";
    }

    /** One of 'lines' now and then (at most every eight seconds), so the chat is not a wall of speech. */
    private void quip(String... lines) {
        if (tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        openingBowPending = true;
        crouched.clear();
        shroud.drop();
        dropShell();
        shroudUp = false;
        flatUntil = 0;
        staggerUntil = 0;
        horizonEnd = 0;
        horizon = null;
        marks = List.of();
        pockets = List.of();
        announce("Lucifero will order the room to kneel. When he does, crouch (hold sneak) or hold anti-magic, or be crushed.");
        announce("His gravity shreds arrows and spells. Close in with a blade or with anti-magic to tear the Gravity Shroud.");
    }

    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        openingBowPending = false;
        crouched.clear();
        shroud.drop();
        dropShell();
        shroudUp = false;
        flatUntil = 0;
        staggerUntil = 0;
        horizonEnd = 0;
        horizon = null;
        marks = List.of();
        pockets = List.of();
    }

    @Override
    protected void onPowerChanged(int old, int now) {
        int before = PowerCurve.stage(old), after = PowerCurve.stage(now);
        if (after <= before || after >= 4) return;
        announce(switch (after) {
            case 1 -> "The air in the room grows heavy.";
            case 2 -> "The weight doubles. Even breathing takes effort.";
            default -> "Lucifero's wings unfold. The floor itself seems to sink.";
        });
        if (summonDisciples(after) > 0) announce("Lesser devils pour through the gate at Lucifero's call.");
    }

    @Override
    protected void onGatesOpened() {
        announce("The Gates open. Lucifero's four wings become arms: Six Limbs, a wider Gravity Comb, and the weight of the whole sky.");
        if (level() instanceof ServerLevel sl) {
            ZogratisVfx.gravityAura(sl, this, 2.0, 240, RGB);
            ZogratisVfx.well(sl, position(), 14, 70, RGB, false);
        }
        for (int delay : new int[]{80, 260, 460}) {
            later(delay, SpadeFx.safe(WHO, "devil wave", () -> {
                if (fightActive() && summonDisciples(2) > 0) announce("A wave of lesser devils steps out of the gate.");
            }));
        }
    }

    /** Opening: the Bow first, as in the anime. After it the scheduler chooses as for any boss. */
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (openingBowPending) {
            int i = moveIndex(LuciferoTable.THE_BOW);
            if (i >= 0 && i < bk.size() && moveEligible(bk.get(i), s)) {
                openingBowPending = false;
                return i;
            }
        }
        return super.chooseMove(bk, s, t);
    }

    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            long now = sl.getGameTime();
            if (!fightActive()) {
                if (shroudUp || shellUp || flatUntil != 0 || horizonEnd != 0) {
                    shroud.drop();
                    dropShell();
                    shroudUp = false;
                    flatUntil = 0;
                    horizonEnd = 0;
                    horizon = null;
                }
                return;
            }
            if (shellUp) {
                if (!shell.active(now)) {
                    dropShell();
                    announce("The shell of swallowed devils cracks away.");
                } else if (tickCount % 8 == 0) Tells.gather(sl, position().add(0, 1.3, 0), 0.9, ACCENT, 4);
            }
            if (shroud.up(now)) {
                shroudUp = true;
                if (tickCount % 20 == 0) ZogratisVfx.gravityAura(sl, this, 1.8, 26, RGB);
            } else if (shroudUp) {
                shroudUp = false;
                announce("The Gravity Shroud falls away.");
            }
            if (now < flatUntil && tickCount % 10 == 0) LuciferoMoves.flattenPass(this, 30);
            Move casting = castingMoveOrNull();
            if (casting != null && LuciferoTable.THE_BOW.equals(casting.id())) {
                for (LivingEntity e : foes(56)) if (SpadeFx.crouching(e)) crouched.put(e.getUUID(), now);
            }
            if (horizonEnd > 0) tickHorizon(sl, now, casting);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Lucifero's tick failed: {}", e.toString());
        }
    }

    /** The great well pulls for the last 24 ticks of the Event Horizon's wind-up; a cast that was dropped ends it. */
    private void tickHorizon(ServerLevel sl, long now, Move casting) {
        if (casting == null || !LuciferoTable.EVENT_HORIZON.equals(casting.id()) || horizon == null) {
            horizonEnd = 0;
            horizon = null;
            return;
        }
        if (now >= horizonEnd - WeightMath.PULL_TICKS && tickCount % 2 == 0) {
            LuciferoMoves.horizonPull(this, horizon);
            if (tickCount % 10 == 0) for (LivingEntity e : foes(WeightMath.PULL_RADIUS)) if (!SpadeFx.antiMagic(e)) SpadeFx.flatten(e, 30);
        }
    }

    // ================================================================ the shroud, the shell, the stagger
    private void raiseShroud() {
        long now = level().getGameTime();
        shroud.raise(now + 160, getMaxHealth() * 0.06);
        shroudUp = true;
        announce("Lucifero's gravity shreds anything that flies at him: arrows and spells do half. Melee and anti-magic tear the shroud.");
        if (level() instanceof ServerLevel sl) ZogratisVfx.gravityAura(sl, this, 1.8, 160, RGB);
    }

    private void tearShroud(ServerLevel sl, long now) {
        staggerUntil = now + ShroudState.STAGGER_TICKS;
        announce("The Gravity Shroud is torn! Lucifero staggers: strike him now.");
        Tells.burst(sl, position().add(0, 1.3, 0), ACCENT, 50);
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.5f);
        quip("Insects with teeth.", "Tch. A blade, then.");
    }

    private void dropShell() {
        shell.drop();
        shellUp = false;
    }

    /** What a blow will really do to him once the invulnerability frames and the exposed bonus have had their say (0 = nothing). */
    private float effective(DamageSource source, float amount) {
        float incoming = exposed() ? amount * ZogratisBoss.EXPOSED_BONUS : amount;
        if (invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (incoming <= lastHurt) return 0f;
            incoming -= lastHurt;
        }
        return incoming;
    }

    /**
     * In order: the shroud halves what flies and is cast (and is worn by what is swung or anti-magic), a staggered king takes a fifth more, and the shell
     * of swallowed devils swallows what is left. The framework's exposed bonus, step guard and damage budget come after.
     */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            float eff = effective(source, amount);
            if (eff <= 0f) return amount;
            long now = sl.getGameTime();
            DamageKind kind = SpadeFx.kindOf(source);
            float a = amount;
            if (shroud.up(now)) {
                a *= shroud.factor(kind, now);
                if (shroud.wear(kind, eff, now)) tearShroud(sl, now);
            }
            if (now < staggerUntil) a *= ShroudState.STAGGER_BONUS;
            if (shell.active(now)) return throughShell(sl, now, a, effective(source, a));
            return a;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Lucifero's defence failed: {}", e.toString());
            return amount;
        }
    }

    private float throughShell(ServerLevel sl, long now, float amount, float eff) {
        double left = shell.absorb(now, eff);
        sl.playSound(null, blockPosition(), SoundEvents.SHIELD_BLOCK, SoundSource.HOSTILE, 1.2f, 0.7f);
        if (!shell.active(now)) {
            dropShell();
            announce("The shell of swallowed devils shatters!");
            Tells.burst(sl, position().add(0, 1.3, 0), ACCENT, 40);
        } else if (now - lastShellNote > 60) {
            lastShellNote = now;
            announce("A shell of swallowed devils holds (" + Math.round(shell.fraction() * 100) + "% left). Burst it, or wait it out.");
        }
        float bonus = exposed() ? ZogratisBoss.EXPOSED_BONUS : 1f;
        return Math.max(0f, amount - (float) (eff - left) / bonus);
    }

    /** The hand was cut by anti-magic: the shroud goes with it and he staggers. */
    private void parried(ServerLevel sl, long now) {
        shroud.drop();
        staggerUntil = now + ShroudState.STAGGER_TICKS;
        announce("Anti-magic cut the hand of gravity! Lucifero staggers.");
        sl.playSound(null, blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 1.5f, 0.6f);
        quip("That sword again...", "Cut me, would you.");
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (LuciferoTable.Spec s : LuciferoTable.specs()) {
            Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                    .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
            if (s.ultimate()) b.ultimate();
            if (s.crowd()) b.crowd();
            b.tell(tellOf(s.id()));
            out.add(b.run(runOf(s.id())));
        }
        return out;
    }

    private static boolean first(int left, int total) { return left >= total - 3; }

    private double progress(int left, int total) { return (double) (total - left) / Math.max(1, total); }

    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case LuciferoTable.CRUSHING_PALM -> (bs, t, left, total) -> tellPalm(left, total);
            case LuciferoTable.FALLING_HEAVENS -> (bs, t, left, total) -> tellHeavens(t, left, total);
            case LuciferoTable.DISTORTION_STEP -> (bs, t, left, total) -> tellStep(t, left, total);
            case LuciferoTable.THE_BOW -> (bs, t, left, total) -> tellBow(left, total);
            case LuciferoTable.GRAVITY_SHROUD, LuciferoTable.LESSER_DEVILS, LuciferoTable.FLATTEN_AIR -> (bs, t, left, total) -> tellGather(id, left, total);
            case LuciferoTable.GRAVITY_COMB -> (bs, t, left, total) -> tellComb(t, left, total);
            case LuciferoTable.GRAVITY_GRASP -> (bs, t, left, total) -> tellGrasp(t, left, total);
            case LuciferoTable.WANDERING_WEIGHT -> (bs, t, left, total) -> tellWander(t, left, total);
            case LuciferoTable.KNEEL_BEFORE_THE_KING -> (bs, t, left, total) -> tellKneel(left, total);
            case LuciferoTable.EVENT_HORIZON -> (bs, t, left, total) -> tellHorizon(left, total);
            case LuciferoTable.SIX_LIMBS -> (bs, t, left, total) -> tellArms(left, total);
            default -> (bs, t, left, total) -> { };
        };
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case LuciferoTable.CRUSHING_PALM -> (b, t) -> {
                LuciferoMoves.palm(this, LuciferoTable.PALM);
                quip("On your knees.", "Is that all the resistance you have?");
            };
            case LuciferoTable.FALLING_HEAVENS -> (b, t) -> {
                LuciferoMoves.heavens(this, marks.isEmpty() ? LuciferoMoves.heavenMarks(this, t, RainMath.marksFor(tuning().castSlots())) : marks, LuciferoTable.HEAVEN);
                marks = List.of();
                quip("Look up. Then down.");
            };
            case LuciferoTable.DISTORTION_STEP -> (b, t) -> {
                Vec3 old = position();
                if (stepTo != null) blinkTo(stepTo);
                stepTo = null;
                LuciferoMoves.lens(this, old, LuciferoTable.LENS);
            };
            case LuciferoTable.THE_BOW -> (b, t) -> {
                int n = LuciferoMoves.bow(this, crouched, LuciferoTable.BOW);
                crouched.clear();
                announce(n == 0 ? "Everyone bowed. Lucifero is not pleased, and is open for the moment." : n + " did not bow and are on their knees. The rest may strike while he is open.");
                quip("Good. That is where you belong.", "You bowed. A wise instinct.");
            };
            case LuciferoTable.GRAVITY_SHROUD -> (b, t) -> raiseShroud();
            case LuciferoTable.LESSER_DEVILS -> (b, t) -> lesserDevils();
            case LuciferoTable.GRAVITY_COMB -> (b, t) -> {
                if (combOrigin == null || aimDir == null) planComb(t);
                LuciferoMoves.comb(this, combOrigin, aimDir, combLanes, LuciferoTable.COMB, LuciferoTable.COMB_PULSE);
                combOrigin = null;
                aimDir = null;
                quip("Find the gaps. If you can.");
            };
            case LuciferoTable.GRAVITY_GRASP -> (b, t) -> {
                Vec3 dir = aimDir != null ? aimDir : SpadeFx.flatUnit(t.position().subtract(position()));
                LuciferoMoves.Grasp g = LuciferoMoves.grasp(this, dir, LuciferoTable.GRASP);
                aimDir = null;
                if (g.parried() && level() instanceof ServerLevel sl) parried(sl, sl.getGameTime());
                else if (g.foe() != null) quip("Caught.", "You do not get to stand there.");
            };
            case LuciferoTable.WANDERING_WEIGHT -> (b, t) -> {
                Vec3 at = marks.isEmpty() ? ZogratisBoss.ground(level(), t.position()) : marks.get(0);
                marks = List.of();
                LuciferoMoves.wandering(this, at, wanderTarget != null ? wanderTarget : t.getUUID(), gatesOpen() ? 160 : 120,
                        Math.min(3.0f, 1.2f + 0.5f * PowerCurve.stage(power())), LuciferoTable.WANDER);
                wanderTarget = null;
                quip("It will follow you. Slowly. Forever.");
            };
            case LuciferoTable.FLATTEN_AIR -> (b, t) -> {
                flatUntil = level().getGameTime() + WeightMath.flattenTicks(power());
                LuciferoMoves.flattenPass(this, 30);
                quip("There is no up. Only down.");
            };
            case LuciferoTable.KNEEL_BEFORE_THE_KING -> (b, t) -> {
                int n = LuciferoMoves.kneelBeforeTheKing(this, pockets, LuciferoTable.KNEEL);
                pockets = List.of();
                announce(n == 0 ? "Nobody was caught. Lucifero is open." : n + " were crushed to their knees. Lucifero is open: strike!");
                say("KNEEL.");
            };
            case LuciferoTable.EVENT_HORIZON -> (b, t) -> {
                Vec3 c = horizon != null ? horizon : ZogratisBoss.ground(level(), position());
                horizonEnd = 0;
                horizon = null;
                int n = LuciferoMoves.horizonCollapse(this, c, LuciferoTable.HORIZON);
                announce(n == 0 ? "The well collapsed on nothing. Lucifero is open." : n + " were caught in the collapse. Lucifero is open: strike!");
            };
            case LuciferoTable.SIX_LIMBS -> (b, t) -> sixLimbs();
            default -> (b, t) -> { };
        };
    }

    // ---------------------------------------------------------------- telegraphs
    private void tellPalm(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 face = SpadeFx.flatUnit(getViewVector(1f)), o = position().add(0, 0.2, 0);
        Tells.ring(sl, position(), 1.5 + (LuciferoMoves.PALM_REACH - 1.5) * progress(left, total), WARN, 1.2f, 32);
        for (double side : new double[]{-Math.toRadians(55), Math.toRadians(55)}) Tells.line(sl, o, o.add(SpadeFx.rotateY(face, side).scale(LuciferoMoves.PALM_REACH)), WARN);
        Tells.gather(sl, position().add(0, 1.6, 0), 0.7, ACCENT, 4);
    }

    private void tellHeavens(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) marks = LuciferoMoves.heavenMarks(this, target, RainMath.marksFor(tuning().castSlots()));
        for (Vec3 m : marks) {
            Tells.ring(sl, m, LuciferoMoves.HEAVEN_RADIUS, WARN, 1.3f, 28);
            Tells.column(sl, m, 4, ACCENT);
        }
        Tells.gather(sl, position().add(0, getBbHeight() * 0.9, 0), 1.0, ACCENT, 5);
    }

    private void tellStep(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) stepTo = LuciferoMoves.stepDestination(this, target);
        Tells.ring(sl, position(), 4.0 - 2.0 * progress(left, total), ACCENT, 1.2f, 36);
        if (stepTo != null) {
            Tells.ring(sl, stepTo, 1.8, RGB, 1.3f, 24);
            Tells.column(sl, stepTo, 3, ACCENT);
        }
    }

    private void tellBow(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            crouched.clear();
            say("Bow.");
            SpadeFx.hintAround(this, 64, "message.nusmp.spade.bow_order", "BOW! Crouch (hold sneak) before the weight falls, or hold anti-magic.");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.4f);
        }
        Tells.ring(sl, position(), 3 + 20 * progress(left, total), RGB, 1.5f, 72);
        for (LivingEntity e : foes(56)) Tells.ring(sl, e.position(), 1.2, SpadeFx.crouching(e) ? ACCENT : WARN, 1.2f, 20);
        Tells.gather(sl, position().add(0, 2.0, 0), 1.4, ACCENT, 6);
    }

    private void tellGather(String id, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        Tells.gather(sl, position().add(0, 1.4, 0), 1.0, ACCENT, 6);
        Tells.ring(sl, position(), 3 + 4 * progress(left, total), RGB, 1.3f, 40);
        if (LuciferoTable.FLATTEN_AIR.equals(id)) {
            if (first(left, total)) SpadeFx.hintAround(this, 64, "message.nusmp.spade.flatten", "The air is pressed flat: no jumping, no gliding. Walk. Anti-magic holders are not held.");
            Tells.ring(sl, position(), 6 + 22 * progress(left, total), ACCENT, 1.4f, 80);
        }
    }

    private void planComb(LivingEntity target) {
        aimDir = SpadeFx.flatUnit(target.position().subtract(position()));
        combOrigin = ZogratisBoss.ground(level(), target.position());
        combLanes = WeightMath.laneCount(power());
    }

    private void tellComb(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            planComb(target);
            for (Vec3[] line : LuciferoMoves.laneLines(combOrigin, aimDir, combLanes)) ZogratisVfx.lane(sl, line[0], line[1], WeightMath.LANE_WIDTH, total + 2, RGB, true);
            announce("Gravity Comb: " + combLanes + " lanes of crushing weight. Stand in the gaps between them.");
        }
        if (combOrigin == null || left % 6 != 0) return;
        Vec3 side = SpadeFx.side(aimDir);
        for (Vec3[] line : LuciferoMoves.laneLines(combOrigin, aimDir, combLanes)) {
            Tells.line(sl, line[0].add(side.scale(WeightMath.LANE_WIDTH / 2)), line[1].add(side.scale(WeightMath.LANE_WIDTH / 2)), WARN);
            Tells.line(sl, line[0].subtract(side.scale(WeightMath.LANE_WIDTH / 2)), line[1].subtract(side.scale(WeightMath.LANE_WIDTH / 2)), WARN);
        }
    }

    private void tellGrasp(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) aimDir = SpadeFx.flatUnit(target.position().subtract(position()));
        if (aimDir == null) return;
        Vec3 o = position().add(0, 1.0, 0);
        Tells.line(sl, o, o.add(aimDir.scale(LuciferoMoves.GRASP_REACH)), WARN);
        Tells.ring(sl, target.position(), 1.4, WARN, 1.2f, 24);
        Tells.gather(sl, o, 0.7, ACCENT, 4);
    }

    private void tellWander(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            marks = List.of(ZogratisBoss.ground(sl, target.position()));
            wanderTarget = target.getUUID();
            announce("Wandering Weight: a heavy well will follow its target slowly. Keep walking out of it.");
        }
        if (marks.isEmpty()) return;
        Vec3 m = marks.get(0);
        Tells.ring(sl, m, LuciferoMoves.WANDER_RADIUS, WARN, 1.4f, 44);
        Tells.ring(sl, m, Math.max(0.8, LuciferoMoves.WANDER_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 28);
        Tells.column(sl, m, 4, ACCENT);
    }

    private void tellKneel(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            pockets = LuciferoMoves.pockets(this, WeightMath.pocketCount(foes(48).size()));
            say("Kneel before the King.");
            SpadeFx.hintAround(this, 64, "message.nusmp.spade.kneel_order", "KNEEL BEFORE THE KING! Stand in a white ring of still air or hold anti-magic. Crouching will not save you.");
            sl.playSound(null, blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.3f);
            for (Vec3 p : pockets) ZogratisVfx.pillar(sl, p, WeightMath.POCKET_RADIUS, total + 2, 0xFFFFFF, true);
        }
        for (Vec3 p : pockets) {
            Tells.ring(sl, p, WeightMath.POCKET_RADIUS, LuciferoMoves.STILL_RGB, 1.6f, 40);
            Tells.column(sl, p, 5, LuciferoMoves.STILL_RGB);
        }
        Tells.ring(sl, position(), 4 + 30 * progress(left, total), RGB, 1.5f, 96);
        Tells.gather(sl, position().add(0, 2.2, 0), 1.6, ACCENT, 8);
    }

    private void tellHorizon(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            horizon = ZogratisBoss.ground(sl, position());
            horizonEnd = sl.getGameTime() + left;
            announce("EVENT HORIZON: the room is being pulled into Lucifero. Walk away from him: everything within " + (int) WeightMath.CORE_RADIUS + " blocks is crushed when it collapses.");
            SpadeFx.hintAround(this, 64, "message.nusmp.spade.horizon_order", "EVENT HORIZON! Walk away from Lucifero: the core is 9 blocks wide.");
            ZogratisVfx.singularity(sl, horizon.add(0, 3.0, 0), WeightMath.CORE_RADIUS, total + 6, RGB);
            ZogratisVfx.well(sl, horizon, WeightMath.CORE_RADIUS, total + 2, RGB, true);
        }
        if (horizon == null) return;
        Tells.ring(sl, horizon, WeightMath.CORE_RADIUS, WARN, 1.5f, 72);
        Tells.ring(sl, horizon, Math.max(WeightMath.CORE_RADIUS, 26 - 17 * progress(left, total)), ACCENT, 1.1f, 80);
        Tells.gather(sl, horizon.add(0, 3.0, 0), 2.0, ACCENT, 8);
    }

    private void tellArms(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (first(left, total)) {
            armPhase = getRandom().nextDouble() * Math.PI * 2;
            announce("Six Limbs: six strikes round him, one after another. Back off beyond " + WeightMath.ARM_REACH + " blocks.");
        }
        Tells.ring(sl, position(), WeightMath.ARM_REACH, WARN, 1.3f, 40);
        for (double a : WeightMath.armAngles(6, armPhase)) SpadeFx.spoke(sl, position(), a, WeightMath.ARM_REACH, WARN);
    }

    // ---------------------------------------------------------------- the moves that need more than a call
    /** Calls two or three lesser devils; if two or more already stand (from 60%) he swallows them instead and wears their strength as a shell. */
    private void lesserDevils() {
        if (!(level() instanceof ServerLevel sl)) return;
        List<DarkDiscipleEntity> mine = disciples();
        if (power() >= PowerCurve.INFINITE_MANA_FROM && mine.size() >= 2) {
            int count = 0;
            for (DarkDiscipleEntity d : mine) {
                if (count >= 4) break;
                ZogratisVfx.lane(sl, d.position().add(0, 1, 0), position().add(0, 1.5, 0), 1.2, 14, ACCENT, false);
                Tells.burst(sl, d.position().add(0, 1, 0), RGB, 12);
                d.discard();
                count++;
            }
            double pool = Math.min(0.08, 0.02 * (0.75 + 0.25 * PowerCurve.stage(power())) * count) * getMaxHealth();
            shell.raise(pool, sl.getGameTime() + 200);
            shellUp = true;
            announce("Lucifero swallows " + count + " lesser devils and wears a shell of their strength (" + Math.round(pool) + " damage). Burst it, or wait 10 s.");
            quip("Come back to me. All of you.");
        } else {
            int made = summonDisciples(2 + (gatesOpen() ? 1 : 0));
            if (made > 0) {
                ZogratisVfx.well(sl, position(), 5, 30, RGB, false);
                quip("Rise, my lesser ones.");
            }
        }
    }

    /** Six strikes, six ticks apart, round him; one foe is struck at most three times by all six together. */
    private void sixLimbs() {
        Map<Integer, Integer> hits = LuciferoMoves.newHits();
        double[] angles = WeightMath.armAngles(6, armPhase);
        for (int i = 0; i < angles.length; i++) {
            final double angle = angles[i];
            later(i * 6, SpadeFx.safe(WHO, "arm", () -> LuciferoMoves.arm(this, angle, LuciferoTable.ARM, hits)));
        }
        quip("All of me, now.");
    }
}
