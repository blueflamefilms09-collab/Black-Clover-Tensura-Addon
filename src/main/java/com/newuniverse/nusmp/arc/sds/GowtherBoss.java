package com.newuniverse.nusmp.arc.sds;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: Gowther, the Goat's Sin of Lust, is the fourth Sin of the owner's brief and the one whose power is the mind, which a game boss must NOT take from a player.
 * The owner's rule is "no mind control of players", and the owner also wants fights that "transcend standard stat checks", so his Invasion is turned into things a
 * party can SEE and LEARN: he reads what you keep doing and shrugs it off, he hides his real strikes among illusions that flicker, and he strikes where you were.
 * He stands on the Zogratis framework (moves with telegraphs, the power curve, the arena, the damage budget, the second health bar, the Charter drop, all through
 * {@link ArcBoss}) and adds three rules of his own (the numbers are in {@link GowtherTable}, the arithmetic in {@link ReadWindow}, {@link MarkMix}, {@link EchoTrail}
 * and {@link StringPull}; the whole design and its sources are in {@code docs/arc/gowther.md}):
 * <ul>
 *   <li><b>Light of Prying Eyes</b>: he reads the KIND of blow dealt to him the most in the last eight seconds (blades, arrows, spells, fire ...), and for eight seconds
 *       (twelve at the Gates) turns 55% of that kind aside. Every other kind lands in full. Counter: change what you use; anti-magic ends the reading.</li>
 *   <li><b>The code of an illusion</b>: in Phantom Garden, Illusory Step and the Nightmare Teller there are always more illusions than real strikes, and they can be
 *       told apart: a real ring is gold and steady with a column of light, an illusion is violet and flickers. The Nightmare Teller, his ultimate, is a dome that holds
 *       him still for seven seconds of waves of such rings; a burst of 8% of his health inside three seconds wakes him from it.</li>
 *   <li><b>Echo of Memory</b>: he remembers where each foe stood one, two and three seconds ago (five at the Gates) and strikes those places. Counter: keep moving.</li>
 * </ul>
 * Nothing here takes a player's aim or body: the only effects on a player's hands are the short grimoire silence of Rewrite Light and the pull of the Marionette
 * Strings, both under the balance law and both avoidable. Nothing changes a block, spawns an entity, or heals him.
 */
public class GowtherBoss extends ArcBoss {
    public static final ArcCharacter CHARACTER = GowtherSheet.CHARACTER;

    /** The entity type, registered under the character's id (the roster checks the path). */
    public static final DeferredHolder<EntityType<?>, EntityType<GowtherBoss>> TYPE = ArcEntities.boss("gowther", GowtherBoss::new, GowtherSheet.WIDTH, GowtherSheet.HEIGHT,
            GowtherSheet.LOOK, GowtherBoss::createAttributes);

    private static final int RGB = GowtherSheet.RGB, ACCENT = GowtherSheet.ACCENT, REAL = GowtherMoves.REAL, FAKE = GowtherMoves.FAKE, WARN = 0xFF5A7A;
    private static final int PINK = 0xFF000000 | RGB, PALE = 0xFF000000 | ACCENT;
    private static final int DAZE_TICKS = 50;

    private final ReadWindow read = new ReadWindow(GowtherTable.READ_SPAN);
    private final EchoTrail trail = new EchoTrail(GowtherTable.TRAIL_TICKS);
    private final BurstGauge domeGauge = new BurstGauge(GowtherTable.BREAK_SPAN, 400);
    private final Map<Integer, Long> shrugged = new HashMap<>();
    private final Set<String> taught = new HashSet<>();
    private List<GowtherMoves.Ring> rings = List.of();
    private List<GowtherMoves.Ring> waveRings = List.of();
    private List<List<Vec3>> echoSets = List.of();
    private List<LivingEntity> stringFoes = List.of();
    private Vec3 mark, laneFrom, laneDir, domeCentre;
    private Vec3[] stepSpots = new Vec3[0];
    private int stepReal, tellCalls, domeToken;
    private boolean laneLocked;
    private long domeUntil, dazeUntil, lastQuip;

    public GowtherBoss(EntityType<GowtherBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(GowtherSheet.HEALTH, GowtherSheet.ATTACK, GowtherSheet.SPEED, GowtherSheet.ARMOR, GowtherSheet.TOUGHNESS);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    /** He is as big at the Gates as before: the second form is a different look (long hair, wings of gears), not a bigger body. */
    @Override protected float formScale(int form) { return 1.0f; }

    /** A doll's plain swat is light: his weight is in the moves. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.6f; }

    /** He stands 8 to 16 blocks off: an archer's distance, whatever his ready moves ask for. */
    @Override protected double preferredRange(double suggested) { return Math.max(8.0, Math.min(16.0, suggested)); }

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
        if (now >= 80 && old < 80) return "Eighty percent. Is this what you call a good fight?";
        if (now >= 60 && old < 60) return "Sixty percent. Now I may use the dream.";
        if (now >= 40 && old < 40) return "Forty percent. I am starting to remember you.";
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
        announce("Gowther reads what you do: if one kind of blow (blades, arrows, spells, fire ...) makes most of your damage, he turns it aside for a while. Change what you use.");
        announce("His illusions can be read: a GOLD, steady ring with a column of light is real; a VIOLET, flickering ring is an illusion and does nothing.");
    }

    /** The framework's end of a fight first (the story's defeat listeners hear of a win there), then his own state is cleared. */
    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        resetState();
    }

    private void resetState() {
        read.reset();
        trail.clear();
        domeGauge.reset();
        shrugged.clear();
        taught.clear();
        rings = List.of();
        waveRings = List.of();
        echoSets = List.of();
        stringFoes = List.of();
        mark = null;
        laneFrom = null;
        laneDir = null;
        domeCentre = null;
        stepSpots = new Vec3[0];
        domeToken++;
        domeUntil = 0;
        dazeUntil = 0;
        laneLocked = false;
    }

    @Override
    protected void onGatesOpened() {
        announce("Gowther's true form: five echoes, a reading that lasts twelve seconds and cuts deeper, more real rings in every garden, a stronger Nightmare.");
        if (level() instanceof ServerLevel sl) {
            ZogratisVfx.halo(sl, this, 1.4, 240, RGB);
            ZogratisVfx.gate(sl, position().add(0, 1.0, 0), facing(), 1.8, 60, ACCENT);
        }
    }

    // ================================================================ every tick: the trail, the reading, the dream
    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            long now = sl.getGameTime();
            if (tickCount % 4 == 0) {
                for (LivingEntity e : foes(70)) trail.record(e.getId(), now, e.getX(), e.getY(), e.getZ());
                if (tickCount % 100 == 0) trail.prune(now);
            }
            if (read.active(now) && tickCount % 10 == 0) Tells.ring(sl, position().add(0, 0.1, 0), 1.8, FAKE, 1.1f, 18);
            if (now < domeUntil) {
                getNavigation().stop();                                   // he concentrates: he does not walk while he dreams
                if (domeCentre != null && tickCount % 10 == 0) Tells.ring(sl, domeCentre.add(0, 0.1, 0), GowtherTable.NIGHTMARE_RADIUS, RGB, 1.3f, 64);
            }
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Gowther's tick failed: {}", e.toString());
        }
    }

    // ================================================================ the reading and the dream's break
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            long now = sl.getGameTime();
            boolean anti = FourSinsKit.isAntiMagic(source);
            DamageKind kind = FourSinsKit.kindOf(source);
            float a = amount;
            if (source.getEntity() instanceof LivingEntity) read.record(now, kind, amount);   // what was meant to land is what he reads, not what got through
            if (anti && read.active(now)) {
                read.end();
                announce("Anti-magic cuts Gowther's reading short.");
            }
            if (!exposed()) {
                float f = read.factor(now, kind, GowtherTable.readCut(gatesOpen()));
                if (f < 1f) {
                    a *= f;
                    shrug(sl, now, source, kind, f);
                }
            }
            if (now < dazeUntil) a *= 1.15f;
            if (now < domeUntil && domeGauge.hit(now, a, GowtherTable.BREAK_FRACTION * getMaxHealth(), anti)) breakDome(sl, now);
            return a;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Gowther's defence failed: {}", e.toString());
            return amount;
        }
    }

    /** A little feedback so the player sees the reading work: a spark, and (at most every two seconds per attacker) a line on their action bar. */
    private void shrug(ServerLevel sl, long now, DamageSource source, DamageKind kind, float factor) {
        Tells.burst(sl, position().add(0, getBbHeight() * 0.6, 0), FAKE, 4);
        if (source.getEntity() instanceof LivingEntity att && now - shrugged.getOrDefault(att.getId(), -1000L) >= 40) {
            shrugged.put(att.getId(), now);
            FourSinsKit.note(att, "Gowther has read your " + kind.label() + ": " + Math.round((1f - factor) * 100) + "% turned aside. Use something else.", ChatFormatting.LIGHT_PURPLE);
        }
    }

    private void breakDome(ServerLevel sl, long now) {
        domeToken++;
        domeUntil = 0;
        waveRings = List.of();
        dazeUntil = now + DAZE_TICKS;
        Vec3 c = domeCentre != null ? domeCentre : position();
        VfxSpawn.send(sl, VfxShape.DREAM_SHATTER, c, c.add(0, GowtherTable.NIGHTMARE_RADIUS, 0), PALE, 40, (float) GowtherTable.NIGHTMARE_RADIUS);
        FourSinsKit.sound(sl, c, SoundEvents.GLASS_BREAK, 3f, 0.6f);
        announce("Gowther is woken from his own dream: the Nightmare Teller breaks!");
        say("...That hurt. I did not know a dream could hurt.");
    }

    /** He does not start a move while he is dazed or dreaming. ({@code t} is the framework's own clock, the boss's age; the daze and the dream are kept in game time.) */
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        long now = level().getGameTime();
        if (now < dazeUntil || now < domeUntil) return -1;
        return super.chooseMove(bk, s, t);
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (MoveRow r : GowtherTable.rows()) {
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

    private Vec3 takeMark(LivingEntity target) {
        Vec3 m = mark != null ? mark : ZogratisBoss.ground(level(), target.position());
        mark = null;
        return m;
    }

    /** Up to four foes within {@code radius}, the nearest first. */
    private List<LivingEntity> partyWithin(double radius) {
        List<LivingEntity> all = new ArrayList<>(foes(radius));
        all.sort((a, b) -> Double.compare(a.distanceToSqr(this), b.distanceToSqr(this)));
        return all.size() > 4 ? new ArrayList<>(all.subList(0, 4)) : all;
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case GowtherTable.HERRITT_VOLLEY -> (bs, t) -> {
                GowtherMoves.arrows(this, aimAt(t), GowtherTable.arrowCount(tuning().castSlots()), GowtherTable.ARROW);
                quip("Herritt. Two bows, so you have two ways to be wrong.", "Please do not stand still.");
            };
            case GowtherTable.HERRITT_CROSS -> (bs, t) -> {
                GowtherMoves.crossLock(this, takeMark(t), GowtherTable.CROSS_ARROW);
                quip("Locked.", "Two lines. One of you.");
            };
            case GowtherTable.PHANTOM_GARDEN -> (bs, t) -> runGarden();
            case GowtherTable.ECHO_OF_MEMORY -> (bs, t) -> runEcho();
            case GowtherTable.PRYING_EYES -> (bs, t) -> runRead(t);
            case GowtherTable.NIGHTMARE_TELLER -> (bs, t) -> runNightmare();
            case GowtherTable.MARIONETTE -> (bs, t) -> runStrings();
            case GowtherTable.ILLUSORY_STEP -> (bs, t) -> runStep(t);
            case GowtherTable.REWRITE_LIGHT -> (bs, t) -> runRewrite();
            case GowtherTable.PUPPET_RUSH -> (bs, t) -> {
                GowtherMoves.rush(this, t);
                quip("Closer than you thought.");
            };
            default -> (bs, t) -> { };
        };
    }

    private void runGarden() {
        List<GowtherMoves.Ring> rs = rings;
        rings = List.of();
        if (rs.isEmpty()) return;
        int struck = GowtherMoves.blast(this, rs, GowtherTable.GARDEN_RADIUS, GowtherTable.GARDEN_BLAST);
        if (struck == 0) quip("You read it correctly. Well done.");
        else quip("That one was real.");
    }

    private void runEcho() {
        List<List<Vec3>> sets = echoSets;
        echoSets = List.of();
        int made = 0;
        for (List<Vec3> set : sets) {
            for (int j = 0; j < set.size() && made < GowtherTable.MAX_MARKS; j++, made++) {
                final Vec3 at = set.get(j);
                later(j * 4, GowtherMoves.safe("echo", () -> GowtherMoves.echoAt(this, at)));
            }
        }
        quip("You stood here. And here. I remember.");
    }

    private void runRead(LivingEntity target) {
        long now = level().getGameTime();
        DamageKind kind = read.dominant(now, GowtherTable.READ_MIN);
        if (level() instanceof ServerLevel sl) {
            Vec3 above = position().add(0, getBbHeight() + 0.7, 0);
            VfxSpawn.send(sl, VfxShape.EYE_FX3, above, above, PINK, 40, 1.0f);
            FourSinsKit.sound(sl, position(), SoundEvents.ENCHANTMENT_TABLE_USE, 2f, 0.6f);
        }
        if (kind == null) {
            announce("Light of Prying Eyes: Gowther finds nothing to read yet.");
            GowtherMoves.arrows(this, aimAt(target), 2, GowtherTable.ARROW);
            quip("You have hardly touched me. There is nothing to read.");
            return;
        }
        boolean gates = gatesOpen();
        read.begin(now, kind, GowtherTable.readTicks(gates));
        announce("Light of Prying Eyes: Gowther has read your " + kind.label() + " and turns " + Math.round(GowtherTable.readCut(gates) * 100) + "% of them aside for "
                + GowtherTable.readTicks(gates) / 20 + " seconds. Use another kind of attack, or anti-magic.");
        quip("You rely on " + kind.label() + ". Do try something else.", "I see how you fight. All of it.");
    }

    private void runNightmare() {
        if (!(level() instanceof ServerLevel sl)) return;
        long now = sl.getGameTime();
        Vec3 c = domeCentre != null ? domeCentre : ZogratisBoss.ground(sl, position());
        domeCentre = c;
        domeUntil = now + GowtherTable.NIGHTMARE_TICKS + 2;
        domeGauge.reset();
        final int token = ++domeToken;
        VfxSpawn.send(sl, VfxShape.DREAM_DOME, c, c.add(0, GowtherTable.NIGHTMARE_RADIUS, 0), PALE, GowtherTable.NIGHTMARE_TICKS, (float) GowtherTable.NIGHTMARE_RADIUS);
        FourSinsKit.sound(sl, c, SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.5f);
        teach("nightmare", "Nightmare Teller: waves of rings fall inside the dome. Gold and steady is real; violet and flickering is an illusion. Burst Gowther for "
                + Math.round(GowtherTable.BREAK_FRACTION * 100) + "% of his health in three seconds to wake him from it.");
        say("Let me tell you a nightmare.");
        SpellRuntime.zone(sl, GowtherTable.NIGHTMARE_TICKS, 1, GowtherMoves.safe("nightmare", age -> nightmareStep(sl, token, age)));
    }

    /** One tick of the dream: a wave of rings every 28 ticks, drawn for 20 and then struck, 5 waves in all. */
    private void nightmareStep(ServerLevel sl, int token, int age) {
        if (token != domeToken || !isAlive() || isRemoved()) return;
        int wave = age / GowtherTable.NIGHTMARE_WAVE, phase = age % GowtherTable.NIGHTMARE_WAVE;
        if (wave >= GowtherTable.nightmareWaves()) return;
        if (phase == 0) {
            List<LivingEntity> in = new ArrayList<>();
            for (LivingEntity e : foesAround(domeCentre, GowtherTable.NIGHTMARE_RADIUS + 2))
                if (Math.hypot(e.getX() - domeCentre.x, e.getZ() - domeCentre.z) <= GowtherTable.NIGHTMARE_RADIUS && in.size() < 4) in.add(e);
            waveRings = GowtherMoves.plan(this, in, GowtherTable.realPerFoe(tuning().castSlots()), domeCentre, GowtherTable.NIGHTMARE_RADIUS);
        }
        if (phase < GowtherTable.NIGHTMARE_FALL && age % 3 == 0)
            GowtherMoves.drawRings(sl, waveRings, GowtherTable.NIGHTMARE_BLAST_RADIUS, GowtherTable.NIGHTMARE_FALL - phase, GowtherTable.NIGHTMARE_FALL, (age / 3) % 2 == 0);
        if (phase == GowtherTable.NIGHTMARE_FALL) {
            GowtherMoves.blast(this, waveRings, GowtherTable.NIGHTMARE_BLAST_RADIUS, GowtherTable.NIGHTMARE_BLAST);
            waveRings = List.of();
        }
    }

    private void runStrings() {
        List<LivingEntity> foes = stringFoes;
        stringFoes = List.of();
        int pulled = GowtherMoves.pull(this, foes);
        if (pulled > 0) {
            announce("Marionette Strings: " + pulled + " pulled in. A wall between you and Gowther cuts the threads.");
            quip("Come here. Please.");
        }
    }

    private void runStep(LivingEntity target) {
        Vec3[] spots = stepSpots;
        int real = stepReal;
        stepSpots = new Vec3[0];
        if (!(level() instanceof ServerLevel sl)) return;
        for (int i = 0; i < spots.length; i++) {
            Vec3 s = spots[i];
            VfxSpawn.send(sl, VfxShape.MIRROR_STEP, s, s.add(0, 2, 0), PINK, 20, 1.9f);
            if (i != real) Tells.burst(sl, s.add(0, 1, 0), FAKE, 8);
        }
        if (real >= 0 && real < spots.length && blinkTo(spots[real])) {
            getLookControl().setLookAt(target, 360f, 360f);
            GowtherMoves.arrows(this, aimAt(target), 2, GowtherTable.STEP_SNAP);
            quip("Which one was me?", "Behind the third mirror.");
        } else {
            GowtherMoves.arrows(this, aimAt(target), 2, GowtherTable.ARROW);
        }
    }

    private void runRewrite() {
        Vec3 from = laneFrom != null ? laneFrom : chest();
        Vec3 dir = laneDir != null ? laneDir : facing();
        laneFrom = null;
        laneDir = null;
        laneLocked = false;
        int n = GowtherMoves.rewrite(this, from, dir);
        if (n > 0) quip("Forget what you were holding. Just for a moment.");
    }

    // ================================================================ telegraphs
    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case GowtherTable.HERRITT_VOLLEY -> (bs, t, left, total) -> tellVolley(t);
            case GowtherTable.HERRITT_CROSS -> (bs, t, left, total) -> tellCross(t, left, total);
            case GowtherTable.PHANTOM_GARDEN -> (bs, t, left, total) -> tellGarden(left, total);
            case GowtherTable.ECHO_OF_MEMORY -> (bs, t, left, total) -> tellEcho(left, total);
            case GowtherTable.PRYING_EYES -> (bs, t, left, total) -> {
                if (level() instanceof ServerLevel sl) Tells.gather(sl, position().add(0, getBbHeight() + 0.3, 0), 0.7, ACCENT, 6);
            };
            case GowtherTable.NIGHTMARE_TELLER -> (bs, t, left, total) -> tellNightmare(t, left, total);
            case GowtherTable.MARIONETTE -> (bs, t, left, total) -> tellStrings(left, total);
            case GowtherTable.ILLUSORY_STEP -> (bs, t, left, total) -> tellStep(t, left, total);
            case GowtherTable.REWRITE_LIGHT -> (bs, t, left, total) -> tellRewrite(t, left, total);
            case GowtherTable.PUPPET_RUSH -> (bs, t, left, total) -> {
                if (level() instanceof ServerLevel sl) FourSinsKit.coneTell(sl, position(), FourSinsKit.flatDir(position(), t.position(), facing()), GowtherTable.RUSH_RADIUS, 0.85, WARN);
            };
            default -> null;
        };
    }

    private void tellVolley(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = chest();
        Vec3 aim = aimAt(target);
        int n = GowtherTable.arrowCount(tuning().castSlots());
        for (double a : FourSinsMath.fan(n, Math.min(1.0, 0.13 * n))) Tells.line(sl, from, from.add(FourSinsKit.rotateY(aim, a).scale(16)), ACCENT);
        Tells.gather(sl, from, 0.5, ACCENT, 4);
    }

    private void tellCross(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) mark = ZogratisBoss.ground(sl, target.position());
        if (mark == null) return;
        for (Vec3 start : GowtherMoves.crossStarts(this, mark)) {
            double[] len = new double[1];
            Vec3 dir = GowtherMoves.crossLane(start, mark, len);
            FourSinsKit.laneTell(sl, start.subtract(0, start.y - mark.y, 0), dir, len[0], 0.5, ACCENT);
        }
        FourSinsKit.markTell(sl, mark, 1.3, left, total, RGB, ACCENT);
    }

    private void tellGarden(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            rings = GowtherMoves.plan(this, partyWithin(40), GowtherTable.realPerFoe(tuning().castSlots()), null, 0);
            tellCalls = 0;
            teach("garden", "Phantom Garden: GOLD, steady rings with a column of light are real; VIOLET, flickering rings are illusions. Leave the gold ones.");
        }
        tellCalls++;
        GowtherMoves.drawRings(sl, rings, GowtherTable.GARDEN_RADIUS, left, total, tellCalls % 2 == 0);
        Tells.gather(sl, chest(), 0.5, ACCENT, 3);
    }

    private void tellEcho(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            long now = sl.getGameTime();
            int[] ages = GowtherTable.echoAges(gatesOpen());
            List<List<Vec3>> sets = new ArrayList<>();
            for (LivingEntity e : partyWithin(48)) {
                List<double[]> pts = trail.echoes(e.getId(), now, ages);
                List<Vec3> set = new ArrayList<>();
                for (int k = pts.size() - 1; k >= 0; k--) {                     // the oldest memory first, so the echoes land in the order the foe walked
                    double[] p = pts.get(k);
                    Vec3 raw = new Vec3(p[0], p[1], p[2]);
                    Vec3 g = ZogratisBoss.groundOrNull(sl, raw);
                    set.add(g != null ? g : raw);
                }
                if (set.isEmpty()) set.add(ZogratisBoss.ground(sl, e.position()));
                sets.add(set);
            }
            echoSets = sets;
            teach("echo", "Echo of Memory: Gowther strikes the places you stood one, two and three seconds ago. Keep moving.");
        }
        int drawn = 0;
        for (List<Vec3> set : echoSets) for (Vec3 p : set) if (drawn++ < GowtherTable.MAX_MARKS) GowtherMoves.drawEcho(sl, p, left, total);
    }

    private void tellNightmare(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            domeCentre = ZogratisBoss.ground(sl, target.position());
            teach("nightmare-tell", "Nightmare Teller: a dome of dream closes round " + target.getName().getString() + " in " + Math.round(total / 20.0)
                    + " seconds. Inside, gold rings are real and violet ones are illusions; a burst of damage wakes him.");
            FourSinsKit.sound(sl, domeCentre, SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.6f);
        }
        if (domeCentre == null) return;
        Tells.ring(sl, domeCentre.add(0, 0.1, 0), GowtherTable.NIGHTMARE_RADIUS, RGB, 1.4f, 64);
        Tells.ring(sl, domeCentre.add(0, 0.1, 0), Math.max(1.0, GowtherTable.NIGHTMARE_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 40);
        Tells.gather(sl, chest(), 1.0, ACCENT, 8);
    }

    private void tellStrings(int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            stringFoes = GowtherMoves.reachable(this);
            teach("strings", "Marionette Strings: threads reach every foe in his sight. A wall between you and Gowther cuts them; so does a distance of thirty blocks.");
            FourSinsKit.sound(sl, position(), SoundEvents.CHAIN_PLACE, 1.6f, 1.2f);
        }
        tellCalls++;
        for (LivingEntity e : stringFoes) {
            if (!e.isAlive()) continue;
            if (GowtherMoves.seen(this, e)) {
                if (tellCalls % 2 == 0) GowtherMoves.drawThread(this, e, 8);
            } else {
                Tells.burst(sl, e.position().add(0, 1, 0), WARN, 3);                      // the thread has snapped on a wall
            }
        }
    }

    private void tellStep(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            List<Vec3> spots = new ArrayList<>();
            double phase = getRandom().nextDouble() * Math.PI * 2;
            for (int k = 0; k < 3; k++) {
                for (int tries = 0; tries < 4; tries++) {
                    double a = phase + k * 2.0944 + tries * 0.4, r = 8 + getRandom().nextInt(5);
                    Vec3 s = FourSinsKit.standable(sl, this, target.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
                    if (s != null) {
                        spots.add(s);
                        break;
                    }
                }
            }
            stepSpots = spots.toArray(new Vec3[0]);
            stepReal = stepSpots.length == 0 ? -1 : getRandom().nextInt(stepSpots.length);
            tellCalls = 0;
            teach("step", "Illusory Step: Gowther will stand where the GOLD ring is; the violet flickering ones are mirrors.");
        }
        tellCalls++;
        for (int i = 0; i < stepSpots.length; i++) {
            if (i == stepReal) FourSinsKit.markTell(sl, stepSpots[i], 1.3, left, total, REAL, REAL);
            else if (tellCalls % 2 == 0) Tells.ring(sl, stepSpots[i], 1.3, FAKE, 1.0f, 14);
        }
    }

    /** The lane follows the target for the first 60% of the wind-up and then locks (white): after that the way out is sideways. */
    private void tellRewrite(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) laneLocked = false;                           // a new wind-up: a lane left locked by an interrupted one is forgotten
        if (!laneLocked) {
            laneFrom = chest();
            laneDir = FourSinsKit.flatDir(position(), target.position(), facing());
            if (left <= total * 0.4) laneLocked = true;
        }
        if (laneFrom == null || laneDir == null) return;
        FourSinsKit.laneTell(sl, laneFrom.subtract(0, laneFrom.y - position().y, 0), laneDir, GowtherTable.REWRITE_LENGTH, GowtherTable.REWRITE_WIDTH, laneLocked ? 0xFFFFFF : RGB);
        Tells.gather(sl, laneFrom, 0.4, ACCENT, 3);
    }
}
