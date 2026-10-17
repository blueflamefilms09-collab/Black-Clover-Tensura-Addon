package com.newuniverse.nusmp.arc.sds;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.sds.kit.BurstGauge;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsKit;
import com.newuniverse.nusmp.arc.sds.kit.FourSinsMath;
import com.newuniverse.nusmp.arc.sds.kit.MoveRow;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.DomainCube;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
import java.util.UUID;

/**
 * Why: Merlin, the Boar's Sin of Gluttony, is the third Sin of the owner's brief and the one whose fight has to answer the player's own magic: the owner asked that
 * these fights "transcend standard stat checks", and a mage who never runs out of power (her Infinity) and who can erase a spell with an open hand is exactly the
 * boss for it. She stands on the Zogratis framework (moves with telegraphs, the power curve, the arena, the damage budget, the second health bar, the Charter drop,
 * all through {@link ArcBoss}) and adds three rules of her own, each telegraphed and each with a counter the party can learn (the numbers are in {@link MerlinTable},
 * the rules in {@link CancelStance}, {@link CubeShell} and {@link InfinityFields}; the whole design and its sources are in {@code docs/arc/merlin.md}):
 * <ul>
 *   <li><b>Absolute Cancel</b>: a stance in which the first spell that reaches her open hand (from the front, 130 degrees) is erased, its caster's grimoire silenced for a
 *       moment, and its power DEVOURED and sent back a moment later (Gluttony). Counters: a cheap spell first to spend it, a blow from the side, a blade, anti-magic.</li>
 *   <li><b>Perfect Cube</b>: a shell that reflects spells and shots (three times, four at the Gates; a multi-hit spell costs one) and dulls blows of the arm. Counters: melee,
 *       anti-magic (breaks it), or spending the reflections on small shots.</li>
 *   <li><b>Infinity</b>: every move costs no mana (her magic never runs dry) and the fields she lays last until she is staggered by a burst (7% of her health in three
 *       seconds, anti-magic counting double). The Chrono Coffin, her ultimate, has the longest wind-up of any move she has and is broken by 9% of her health dealt
 *       while she casts it (she forgets her surroundings when she concentrates).</li>
 * </ul>
 * Nothing here changes a block, spawns an entity, or heals her.
 */
public class MerlinBoss extends ArcBoss {
    public static final ArcCharacter CHARACTER = MerlinSheet.CHARACTER;

    /** The entity type, registered under the character's id (the roster checks the path). */
    public static final DeferredHolder<EntityType<?>, EntityType<MerlinBoss>> TYPE = ArcEntities.boss("merlin", MerlinBoss::new, MerlinSheet.WIDTH, MerlinSheet.HEIGHT,
            MerlinSheet.LOOK, MerlinBoss::createAttributes);

    private static final int RGB = MerlinSheet.RGB, ACCENT = MerlinSheet.ACCENT, WARN = 0xFF5A7A, WHITE = 0xFFFFFF;
    private static final int VIOLET = 0xFF000000 | RGB, GOLD = 0xFF000000 | ACCENT;

    private final CancelStance cancel = new CancelStance();
    private final CubeShell cube = new CubeShell();
    private final InfinityFields fields = new InfinityFields();
    private final BurstGauge stagger = new BurstGauge(MerlinTable.STAGGER_SPAN, MerlinTable.STAGGER_COOLDOWN);
    private final Map<Integer, Integer> exposure = new HashMap<>();
    private List<Vec3> marks = List.of();
    private List<LivingEntity> markFoes = List.of();
    private DomainCube coffin;
    private boolean coffinFocus;
    private float focusTaken;
    private int coffinCalls;
    private long ampUntil, staggerUntil, lastQuip;
    private UUID lastCaster;

    public MerlinBoss(EntityType<MerlinBoss> type, Level level) { super(type, level); }

    public static AttributeSupplier.Builder createAttributes() {
        return ZogratisBoss.bossAttributes(MerlinSheet.HEALTH, MerlinSheet.ATTACK, MerlinSheet.SPEED, MerlinSheet.ARMOR, MerlinSheet.TOUGHNESS);
    }

    @Override public ArcCharacter character() { return CHARACTER; }

    // ================================================================ tuning
    /** She is as tall at the Gates as before: the second form is a different look, not a bigger body. */
    @Override protected float formScale(int form) { return 1.0f; }

    @Override protected float damageScale() { return amplified() ? 1.2f : 1.0f; }

    /** The plain swat: a caster keeps her distance, so the blow she uses when someone is on her is light. */
    @Override protected float meleeDamage() { return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.5f; }

    /** She wants to stand 8 to 14 blocks off, whatever her ready moves ask for. */
    @Override protected double preferredRange(double suggested) { return Math.max(8.0, Math.min(14.0, suggested)); }

    @Override protected String[] tensuraKeywords() { return new String[]{"teleport", "warp", "dimension", "explosion"}; }

    @Override protected int tensuraKitSize() { return 3; }

    private boolean amplified() { return level().getGameTime() < ampUntil; }

    private Vec3 facing() {
        Vec3 v = getViewVector(1f).multiply(1, 0, 1);
        return v.lengthSqr() < 1e-4 ? new Vec3(0, 0, 1) : v.normalize();
    }

    private double facingBearing() {
        Vec3 f = facing();
        return FourSinsMath.bearing(f.x, f.z);
    }

    private int stage() { return com.newuniverse.nusmp.entity.zogratis.PowerCurve.stage(power()); }

    // ================================================================ lines
    @Override
    protected String powerLine(int old, int now) {
        if (now >= 80 && old < 80) return "Eighty percent. You are more interesting than I expected.";
        if (now >= 60 && old < 60) return "Sixty percent. Time itself is within reach now.";
        if (now >= 40 && old < 40) return "Forty percent. The analysis begins.";
        return "I draw " + now + "% of my power.";
    }

    /** One of {@code lines} now and then (at most every eight seconds), so the chat is not a wall of speech. */
    private void quip(String... lines) {
        if (tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    // ================================================================ the fight's hooks
    @Override
    protected void onFightStart() {
        resetState();
        announce("Absolute Cancel: the first spell that reaches Merlin's open hand is erased and sent back. Bait it with a cheap one, strike from the side, or use a blade.");
        announce("Perfect Cube reflects spells and shots a few times: use your arms, anti-magic, or small shots. Her Infinity fields last until a burst staggers her.");
    }

    /** The framework's end of a fight first (the story's defeat listeners hear of a win there), then her own state is cleared. */
    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        resetState();
    }

    private void resetState() {
        cancel.reset();
        cube.close();
        fields.clear();
        stagger.reset();
        exposure.clear();
        marks = List.of();
        markFoes = List.of();
        coffin = null;
        coffinFocus = false;
        focusTaken = 0f;
        ampUntil = 0;
        staggerUntil = 0;
        lastCaster = null;
    }

    @Override
    protected void onGatesOpened() {
        announce("Infinity unbound: three fields at once, a cube that holds four reflections, and an Absolute Cancel that takes two spells.");
        if (level() instanceof ServerLevel sl) {
            ZogratisVfx.halo(sl, this, 1.4, 240, RGB);
            ZogratisVfx.gate(sl, position().add(0, 1.0, 0), facing(), 1.8, 60, ACCENT);
        }
    }

    // ================================================================ every tick: the fields, the stance, the cube, the amplifier
    @Override
    protected void bossTick() {
        if (!(level() instanceof ServerLevel sl)) return;
        try {
            long now = sl.getGameTime();
            tickFields(sl, now);
            if (cancel.isOpen(now) && tickCount % 8 == 0) FourSinsKit.coneTell(sl, position(), facing(), 6.5, cancel.halfArc(), WHITE);
            if (cube.isOpen(now) && tickCount % 10 == 0) Tells.ring(sl, position().add(0, 0.1, 0), 2.4, RGB, 1.2f, 20);
            if (ampUntil > 0 && now >= ampUntil) {
                ampUntil = 0;
                announce("Power Amplify fades.");
            }
            if (coffinFocus && castingMoveOrNull() == null) coffinFocus = false;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Merlin's tick failed: {}", e.toString());
        }
    }

    /** Infinity: the fields stand until she is staggered. Drawn every half second, and once a second each foe inside takes a tick that grows the longer it stays. */
    private void tickFields(ServerLevel sl, long now) {
        if (fields.count() == 0) {
            if (!exposure.isEmpty()) exposure.clear();
            return;
        }
        if (tickCount % 10 != 0) return;
        List<InfinityFields.Field> all = fields.all();
        for (InfinityFields.Field f : all) {
            Vec3 c = new Vec3(f.x(), f.y(), f.z());
            Tells.ring(sl, c, f.radius(), RGB, 1.2f, (int) Math.min(72, 12 + f.radius() * 4));
            Tells.gather(sl, c.add(0, 2.2, 0), f.radius() * 0.5, ACCENT, 6);
            if (tickCount % 40 == 0) ZogratisVfx.divineSigil(sl, c, f.radius(), 44, RGB, true);
        }
        if (tickCount % 20 != 0) return;
        Set<Integer> inside = new HashSet<>();
        for (InfinityFields.Field f : all) {
            for (LivingEntity e : foesAround(new Vec3(f.x(), f.y(), f.z()), f.radius() + 3)) {
                if (!f.contains(e.getX(), e.getY(), e.getZ()) || !inside.add(e.getId())) continue;
                int seconds = exposure.merge(e.getId(), 1, Integer::sum) - 1;
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0));
                strike(e, MerlinTable.fieldTick(seconds), true);
            }
        }
        exposure.keySet().retainAll(inside);
    }

    // ================================================================ the defences: Absolute Cancel, Perfect Cube, the stagger
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        if (!(level() instanceof ServerLevel sl)) return amount;
        try {
            long now = sl.getGameTime();
            boolean anti = FourSinsKit.isAntiMagic(source);
            float a = amount;
            if (!exposed()) {
                a = defend(sl, now, source, a, anti);
                if (a <= 0f) return 0f;
            }
            if (anti && ampUntil > 0) {
                ampUntil = 0;
                announce("Anti-magic cuts Power Amplify short.");
            }
            if (now < staggerUntil) a *= MerlinTable.STAGGERED_TAKES;
            noteBurst(sl, now, a, anti);
            return a;
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] Merlin's defence failed: {}", e.toString());
            return amount;
        }
    }

    private float defend(ServerLevel sl, long now, DamageSource src, float amount, boolean anti) {
        boolean spell = FourSinsKit.isSpell(src);
        Vec3 from = FourSinsKit.originOf(src);
        float a = amount;
        if (cancel.isOpen(now)) {
            double offset = from == null ? 0.0 : Math.abs(FourSinsMath.angleDiff(FourSinsMath.bearing(from.x - getX(), from.z - getZ()), facingBearing()));
            CancelStance.Verdict v = cancel.judge(now, spell, anti, offset, a);
            switch (v.result()) {
                case CANCELLED -> {
                    onCancelled(sl, now, src, v);
                    return 0f;
                }
                case BROKEN -> {
                    announce("Anti-magic breaks Merlin's Absolute Cancel!");
                    staggerUntil = Math.max(staggerUntil, now + 20);
                }
                default -> { }
            }
        }
        if (cube.isOpen(now)) {
            CubeShell.Verdict v = cube.judge(now, spell, FourSinsKit.isRanged(this, src), anti, a);
            switch (v.result()) {
                case REFLECTED -> {
                    onReflected(sl, now, src, v);
                    return 0f;
                }
                case DULLED -> a = v.passed();
                case BROKEN_BY_ANTI_MAGIC -> shatterCube(sl, now, true);
                default -> { }
            }
        }
        return a;
    }

    /** The spell was erased: the caster is told, silenced for a moment (under the balance law), and the stance schedules the return of what she devoured. */
    private void onCancelled(ServerLevel sl, long now, DamageSource src, CancelStance.Verdict v) {
        LivingEntity thrower = src.getEntity() instanceof LivingEntity le ? le : null;
        Vec3 hand = position().add(facing().scale(1.2)).add(0, 1.2, 0);
        VfxSpawn.send(sl, VfxShape.KOTO_SHATTER, hand, position().add(0, 1.4, 0), GOLD, 16, 1.2f);
        FourSinsKit.sound(sl, hand, SoundEvents.AMETHYST_BLOCK_BREAK, 2f, 0.7f);
        if (thrower != null) {
            lastCaster = thrower.getUUID();
            FourSinsKit.note(thrower, "Absolute Cancel: your spell is erased.", ChatFormatting.LIGHT_PURPLE);
            if (BalanceLaw.beginControl(this)) FourSinsKit.silence(thrower, 30);
        }
        quip("Cancelled.", "That spell no longer exists.", "I will keep that one.");
        if (v.closedNow()) scheduleReturn(sl);
    }

    /** Gluttony: what she devoured comes back at whoever cast it, a lane of dust first, then one bolt. */
    private void scheduleReturn(ServerLevel sl) {
        float base = cancel.takeReturn();
        if (base <= 0f) return;
        UUID who = lastCaster;
        SpellRuntime.zone(sl, 12, 3, MerlinMoves.safe("return tell", age -> {
            LivingEntity foe = returnTarget(sl, who);
            if (foe != null) Tells.line(sl, position().add(0, 1.4, 0), foe.getBoundingBox().getCenter(), WARN);
        }));
        later(12, MerlinMoves.safe("devoured return", () -> {
            LivingEntity foe = returnTarget(sl, who);
            if (foe == null) return;
            announce("Merlin sends back the spell she devoured.");
            MerlinMoves.volley(this, foe, 1, base);
        }));
    }

    private LivingEntity returnTarget(ServerLevel sl, UUID who) {
        if (who != null && sl.getEntity(who) instanceof LivingEntity le && le.isAlive() && isFoe(le)) return le;
        LivingEntity t = getTarget();
        return t != null && t.isAlive() ? t : null;
    }

    private void onReflected(ServerLevel sl, long now, DamageSource src, CubeShell.Verdict v) {
        LivingEntity thrower = src.getEntity() instanceof LivingEntity le ? le : null;
        if (v.reflectBase() > 0f && thrower != null && isFoe(thrower)) {
            strike(thrower, v.reflectBase(), true);
            VfxSpawn.send(sl, VfxShape.MIRROR_RAY, position().add(0, 1.2, 0), thrower.getBoundingBox().getCenter(), GOLD, 12, 1f);
            FourSinsKit.note(thrower, "Perfect Cube: your attack is reflected.", ChatFormatting.LIGHT_PURPLE);
            FourSinsKit.sound(sl, position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.6f, 1.4f);
            quip("Nothing crosses it that I did not allow.", "Returned to sender.");
        }
        if (v.shattered()) shatterCube(sl, now, false);
    }

    private void shatterCube(ServerLevel sl, long now, boolean byAntiMagic) {
        cube.close();
        ZogratisVfx.spaceShatter(sl, position().add(0, 1, 0), 2.6, RGB);
        FourSinsKit.sound(sl, position(), SoundEvents.GLASS_BREAK, 2.5f, 0.6f);
        if (byAntiMagic) {
            announce("Anti-magic breaks the Perfect Cube!");
        } else {
            announce("The Perfect Cube shatters: Merlin is staggered!");
            staggerUntil = Math.max(staggerUntil, now + 30);
        }
    }

    /** Every blow that lands is counted: the burst that staggers her, and the damage that breaks her focus while she casts the Coffin. */
    private void noteBurst(ServerLevel sl, long now, float dealt, boolean anti) {
        if (coffinFocus && castingMoveOrNull() != null) {
            focusTaken += dealt;
            if (focusTaken >= MerlinTable.FOCUS_FRACTION * getMaxHealth()) {
                breakCoffin(sl, now);
                return;
            }
        }
        if (stagger.hit(now, dealt, MerlinTable.STAGGER_FRACTION * getMaxHealth(), anti)) staggerNow(sl, now);
    }

    /** A burst: her fields end, her stance and cube close, a cast that is not the Coffin is lost, and for two seconds she takes more and starts nothing. */
    private void staggerNow(ServerLevel sl, long now) {
        int ended = fields.clear();
        cancel.close();
        cube.close();
        ampUntil = 0;
        Move casting = castingMoveOrNull();
        if (casting != null && !casting.ultimate()) interruptCast();
        staggerUntil = now + MerlinTable.STAGGER_TICKS;
        ZogratisVfx.spaceShatter(sl, position().add(0, 1, 0), 2.2, ACCENT);
        FourSinsKit.sound(sl, position(), SoundEvents.GLASS_BREAK, 2f, 0.8f);
        announce("Merlin's focus breaks" + (ended > 0 ? ": her Infinity fields end!" : "!"));
        quip("Impressive. That hurt.", "You broke my concentration.");
    }

    private void breakCoffin(ServerLevel sl, long now) {
        coffinFocus = false;
        focusTaken = 0f;
        coffin = null;
        marks = List.of();
        interruptCast();
        staggerUntil = now + 50;
        ZogratisVfx.spaceShatter(sl, position().add(0, 1, 0), 3.0, RGB);
        FourSinsKit.sound(sl, position(), SoundEvents.GLASS_BREAK, 3f, 0.5f);
        announce("Merlin loses her focus: the Chrono Coffin falls apart!");
        say("You... interrupted me.");
    }

    /** She does not start a move while she is staggered. ({@code t} is the framework's own clock, the boss's age; the stagger is kept in game time.) */
    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (level().getGameTime() < staggerUntil) return -1;
        return super.chooseMove(bk, s, t);
    }

    // ================================================================ moves
    @Override
    protected List<Move> moves() {
        List<Move> out = new ArrayList<>();
        for (MoveRow r : MerlinTable.rows()) {
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
        Vec3 m = marks.isEmpty() ? ZogratisBoss.ground(level(), target.position()) : marks.get(0);
        marks = List.of();
        return m;
    }

    private Move.MoveRun runOf(String id) {
        return switch (id) {
            case MerlinTable.ARCANE_VOLLEY -> (bs, t) -> {
                MerlinMoves.volley(this, t, MerlinTable.boltCount(tuning().castSlots(), gatesOpen()), MerlinTable.BOLT);
                quip("Let us see how you dodge.", "A little arithmetic.");
            };
            case MerlinTable.ALDAN_STRIKE -> (bs, t) -> {
                MerlinMoves.aldanImpact(this, takeMark(t), MerlinTable.ALDAN_RADIUS, MerlinTable.ALDAN);
                quip("Aldan.", "Close enough to touch.");
            };
            case MerlinTable.TRANSFER -> (bs, t) -> runTransfer(t);
            case MerlinTable.ABSOLUTE_CANCEL -> (bs, t) -> openCancel();
            case MerlinTable.PERFECT_CUBE -> (bs, t) -> openCube();
            case MerlinTable.INFINITY_FIELD -> (bs, t) -> layField(takeMark(t));
            case MerlinTable.STARFALL -> (bs, t) -> runStarfall(t);
            case MerlinTable.MAGIC_SEAL -> (bs, t) -> {
                int sealed = MerlinMoves.seal(this, takeMark(t));
                if (sealed > 0) quip("Magic Seal. Quiet now.");
            };
            case MerlinTable.POWER_AMPLIFY -> (bs, t) -> runAmplify();
            case MerlinTable.MASS_TRANSFER -> (bs, t) -> runMassTransfer(t);
            case MerlinTable.SPELL_LIBRARY -> (bs, t) -> {
                if (!castTensura(t)) MerlinMoves.volley(this, t, 3, MerlinTable.BOLT);
                quip("From the library.");
            };
            case MerlinTable.CHRONO_COFFIN -> (bs, t) -> runCoffin();
            default -> (bs, t) -> { };
        };
    }

    private void runTransfer(LivingEntity target) {
        Vec3 dest = marks.isEmpty() ? null : marks.get(0);
        marks = List.of();
        if (dest != null && blinkTo(dest)) {
            getLookControl().setLookAt(target, 360f, 360f);
            MerlinMoves.volley(this, target, 1, MerlinTable.SNAP);
            quip("Behind you. No, in front. Never mind.");
        } else {
            MerlinMoves.volley(this, target, 2, MerlinTable.BOLT);
        }
    }

    private void openCancel() {
        long now = level().getGameTime();
        boolean gates = gatesOpen();
        int ticks = gates ? MerlinTable.CANCEL_TICKS_GATES : MerlinTable.CANCEL_TICKS;
        cancel.open(now, ticks, gates ? MerlinTable.CANCEL_CHARGES_GATES : MerlinTable.CANCEL_CHARGES, gates ? CancelStance.HALF_ARC_GATES : CancelStance.HALF_ARC);
        if (level() instanceof ServerLevel sl) {
            Vec3 hand = position().add(facing().scale(1.0)).add(0, getBbHeight() * 0.7, 0);
            ZogratisVfx.gate(sl, hand, facing(), 1.5, ticks, WHITE);
            FourSinsKit.coneTell(sl, position(), facing(), 6.5, cancel.halfArc(), WHITE);
            FourSinsKit.sound(sl, hand, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 1.6f);
        }
        announce("Absolute Cancel: Merlin's hand is open. The first spell that reaches it is erased and sent back. Bait it, strike from the side, or use a blade.");
        quip("Show me your best spell.", "Go on. Try me.");
    }

    private void openCube() {
        long now = level().getGameTime();
        boolean gates = gatesOpen();
        int ticks = gates ? MerlinTable.CUBE_TICKS_GATES : MerlinTable.CUBE_TICKS;
        cube.open(now, ticks, gates ? MerlinTable.CUBE_CHARGES_GATES : MerlinTable.CUBE_CHARGES);
        if (level() instanceof ServerLevel sl) {
            Vec3 floor = new Vec3(getX(), getY() - 1.2, getZ());
            VfxSpawn.send(sl, VfxShape.SPACE_CUBE, floor, floor.add(0, 1, 0), VIOLET, ticks + 8, 2.2f);
            FourSinsKit.sound(sl, position(), SoundEvents.BEACON_ACTIVATE, 2.5f, 0.7f);
        }
        for (LivingEntity e : foes(3.4)) {                       // the cube denies entry: whoever is that close is pushed out, unhurt
            e.knockback(0.9, getX() - e.getX(), getZ() - e.getZ());
            e.hurtMarked = true;
        }
        announce("Perfect Cube: spells and shots are reflected, " + cube.chargesLeft() + " times. Strike with your arms, or use anti-magic.");
        quip("Perfect Cube. Nothing crosses it that I did not allow.");
    }

    private void layField(Vec3 at) {
        long now = level().getGameTime();
        InfinityFields.Field pushed = fields.add(at.x, at.y, at.z, MerlinTable.FIELD_RADIUS, now, gatesOpen() ? MerlinTable.FIELD_CAP_GATES : MerlinTable.FIELD_CAP);
        if (level() instanceof ServerLevel sl) {
            ZogratisVfx.divineSigil(sl, at, MerlinTable.FIELD_RADIUS, 44, RGB, false);
            FourSinsKit.sound(sl, at, SoundEvents.AMETHYST_BLOCK_RESONATE, 2f, 0.6f);
        }
        if (pushed != null) announce("An older Infinity field fades as a new one takes hold.");
        else announce("Infinity: a field that will not end. Burst Merlin down to break it.");
        quip("Infinity.", "It will not end.");
    }

    private void runStarfall(LivingEntity target) {
        List<Vec3> stars = marks.isEmpty() ? pickStars(target) : marks;
        marks = List.of();
        Set<Integer> struck = new HashSet<>();
        for (int i = 0; i < stars.size(); i++) {
            final Vec3 at = stars.get(i);
            later(i * 3, MerlinMoves.safe("star", () -> MerlinMoves.starImpact(this, at, struck)));
        }
        quip("Look up.", "Stars, by the dozen.");
    }

    /** The stars of one fall: on the target, on the other foes, then on a ring round the target, never stacked. */
    private List<Vec3> pickStars(LivingEntity target) {
        List<double[]> others = new ArrayList<>();
        for (LivingEntity e : foes(44)) if (e != target) others.add(new double[]{e.getX(), e.getZ()});
        double[][] plan = FourSinsMath.spreadMarks(new double[]{target.getX(), target.getZ()}, others.toArray(new double[0][]),
                MerlinTable.starCount(tuning().castSlots()), 5.5, MerlinTable.STAR_RADIUS * 1.6, getRandom().nextDouble() * Math.PI * 2);
        List<Vec3> out = new ArrayList<>();
        for (double[] m : plan) out.add(ZogratisBoss.ground(level(), new Vec3(m[0], target.getY(), m[1])));
        return out;
    }

    private void runAmplify() {
        ampUntil = level().getGameTime() + MerlinTable.AMP_TICKS;
        if (level() instanceof ServerLevel sl) VfxSpawn.sendFollowing(sl, VfxShape.MANA_CHARGE, this, position().add(0, 1, 0), GOLD, MerlinTable.AMP_TICKS, 1.6f);
        announce("Power Amplify: Merlin's spells hit 20% harder for 8 seconds. Anti-magic or a burst ends it.");
        quip("A little more power.");
    }

    private void runMassTransfer(LivingEntity target) {
        List<LivingEntity> foes = markFoes.isEmpty() ? foes(40) : markFoes;
        List<Vec3> spots = marks.size() == foes.size() && !marks.isEmpty() ? marks : groundOf(foes);
        markFoes = List.of();
        marks = List.of();
        int moved = MerlinMoves.massTransfer(this, foes, spots);
        if (moved > 0) announce("Mass Transfer: " + moved + " moved across the arena.");
        quip("Everyone, over there.");
    }

    private List<Vec3> groundOf(List<LivingEntity> foes) {
        List<Vec3> out = new ArrayList<>();
        for (LivingEntity e : foes) out.add(ZogratisBoss.ground(level(), e.position()));
        return out;
    }

    private void runCoffin() {
        coffinFocus = false;
        focusTaken = 0f;
        DomainCube box = coffin;
        coffin = null;
        if (box == null) {
            LivingEntity t = getTarget();
            Vec3 g = ZogratisBoss.ground(level(), t != null ? t.position() : position());
            box = DomainCube.around(g.x, g.y, g.z, MerlinTable.coffinHalf(stage()));
        }
        final DomainCube cubeNow = box;
        int held = MerlinMoves.coffinFreeze(this, cubeNow);
        say("Chrono Coffin.");
        announce(held > 0 ? "Time has stopped for " + held + " in the cube. It collapses in two seconds: leave it if you can move." : "Time slips: nobody was held. The cube collapses in two seconds.");
        later(MerlinTable.COFFIN_COLLAPSE_DELAY, MerlinMoves.safe("coffin collapse", () -> MerlinMoves.coffinCollapse(this, cubeNow)));
    }

    // ================================================================ telegraphs
    private Move.MoveTell tellOf(String id) {
        return switch (id) {
            case MerlinTable.ARCANE_VOLLEY -> (bs, t, left, total) -> tellVolley(t);
            case MerlinTable.ALDAN_STRIKE -> (bs, t, left, total) -> tellMark(t, left, total, MerlinTable.ALDAN_RADIUS, WARN, false);
            case MerlinTable.TRANSFER -> (bs, t, left, total) -> tellTransfer(t, left, total);
            case MerlinTable.ABSOLUTE_CANCEL -> (bs, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                Vec3 hand = position().add(facing().scale(1.0)).add(0, getBbHeight() * 0.7, 0);
                if (FourSinsKit.first(left, total)) ZogratisVfx.gate(sl, hand, facing(), 1.5, total + 2, WHITE);
                FourSinsKit.coneTell(sl, position(), facing(), 6.5, gatesOpen() ? CancelStance.HALF_ARC_GATES : CancelStance.HALF_ARC, WHITE);
                Tells.gather(sl, hand, 0.5, ACCENT, 4);
            };
            case MerlinTable.PERFECT_CUBE -> (bs, t, left, total) -> {
                if (!(level() instanceof ServerLevel sl)) return;
                Tells.ring(sl, position(), 3.4, RGB, 1.3f, 32);
                Tells.gather(sl, position().add(0, 1, 0), 1.4, ACCENT, 6);
            };
            case MerlinTable.INFINITY_FIELD -> (bs, t, left, total) -> tellMark(t, left, total, MerlinTable.FIELD_RADIUS, RGB, true);
            case MerlinTable.STARFALL -> (bs, t, left, total) -> tellStars(t, left, total);
            case MerlinTable.MAGIC_SEAL -> (bs, t, left, total) -> tellMark(t, left, total, MerlinTable.SEAL_RADIUS, WARN, false);
            case MerlinTable.POWER_AMPLIFY -> (bs, t, left, total) -> {
                if (level() instanceof ServerLevel sl) Tells.gather(sl, position().add(0, 1.0, 0), 0.9, ACCENT, 8);
            };
            case MerlinTable.MASS_TRANSFER -> (bs, t, left, total) -> tellMass(t, left, total);
            case MerlinTable.CHRONO_COFFIN -> (bs, t, left, total) -> tellCoffin(t, left, total);
            default -> null;
        };
    }

    private void tellVolley(LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return;
        Vec3 from = position().add(0, getBbHeight() * 0.75, 0);
        Vec3 aim = target.getBoundingBox().getCenter().subtract(from);
        if (aim.lengthSqr() < 1e-4) return;
        aim = aim.normalize();
        int n = MerlinTable.boltCount(tuning().castSlots(), gatesOpen());
        for (double a : FourSinsMath.fan(n, Math.min(1.1, 0.16 * n))) Tells.line(sl, from, from.add(FourSinsKit.rotateY(aim, a).scale(14)), ACCENT);
        Tells.gather(sl, from, 0.6, ACCENT, 4);
    }

    /** A ring at the spot the target stands on when the wind-up begins; it stays there, so moving off it is the answer. */
    private void tellMark(LivingEntity target, int left, int total, double radius, int rgb, boolean sigil) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            marks = List.of(ZogratisBoss.ground(sl, target.position()));
            Vec3 m = marks.get(0);
            if (sigil) ZogratisVfx.divineSigil(sl, m, radius, total + 2, RGB, true);
            else VfxSpawn.send(sl, VfxShape.MAGIC_CIRCLE, m.add(0, 0.05, 0), m.add(0, 1, 0), VIOLET, total, (float) (radius / 1.6));
        }
        if (marks.isEmpty()) return;
        FourSinsKit.markTell(sl, marks.get(0), radius, left, total, rgb, ACCENT);
    }

    private void tellTransfer(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            Vec3 dest = pickTransfer(sl, target);
            marks = dest == null ? List.of() : List.of(dest);
            if (dest != null) ZogratisVfx.gate(sl, dest.add(0, 1.0, 0), FourSinsKit.flatDir(dest, target.position(), facing()), 1.1, total + 2, RGB);
        }
        if (marks.isEmpty()) return;
        Tells.ring(sl, marks.get(0), 1.3, ACCENT, 1.2f, 20);
        Tells.column(sl, marks.get(0), 3, ACCENT);
    }

    /** A spot 10 to 13 blocks round the target where she fits and can see it (a dozen tries); any standable spot if none can. */
    private Vec3 pickTransfer(ServerLevel sl, LivingEntity target) {
        double phase = getRandom().nextDouble() * Math.PI * 2;
        Vec3 first = null;
        for (int k = 0; k < 12; k++) {
            double a = phase + k * 0.5236, r = 10 + getRandom().nextInt(4);
            Vec3 s = FourSinsKit.standable(sl, this, target.position().add(Math.cos(a) * r, 0, Math.sin(a) * r));
            if (s == null) continue;
            if (first == null) first = s;
            Vec3 tEye = target.getEyePosition();
            if (FourSinsKit.clipWall(sl, this, s.add(0, 1.6, 0), tEye).distanceToSqr(tEye) < 0.25) return s;
        }
        return first;
    }

    private void tellStars(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            marks = pickStars(target);
            announce("Starfall: " + marks.size() + " stars. Leave the rings.");
        }
        for (Vec3 m : marks) {
            Tells.ring(sl, m, MerlinTable.STAR_RADIUS, WARN, 1.3f, 28);
            Tells.ring(sl, m, Math.max(0.6, MerlinTable.STAR_RADIUS * left / Math.max(1, total)), ACCENT, 1.1f, 20);
            Tells.column(sl, m, 5, ACCENT);
        }
    }

    private void tellMass(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            List<LivingEntity> foes = foes(40);
            if (foes.size() > 4) foes = new ArrayList<>(foes.subList(0, 4));
            markFoes = foes;
            marks = groundOf(foes);
            announce("Mass Transfer: step out of your ring or be moved.");
        }
        for (Vec3 m : marks) FourSinsKit.markTell(sl, m, MerlinTable.MASS_RADIUS, left, total, RGB, ACCENT);
    }

    private void tellCoffin(LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        if (FourSinsKit.first(left, total)) {
            Vec3 g = ZogratisBoss.ground(sl, target.position());
            coffin = DomainCube.around(g.x, g.y, g.z, MerlinTable.coffinHalf(stage()));
            coffinFocus = true;
            focusTaken = 0f;
            coffinCalls = 0;
            Vec3 floor = new Vec3(coffin.cx(), coffin.minY(), coffin.cz());
            VfxSpawn.send(sl, VfxShape.SPACE_CUBE, floor, floor.add(0, 1, 0), VIOLET, total + 6, (float) coffin.half());
            VfxSpawn.send(sl, VfxShape.TIME_CLOCK, g, g.add(0, 3, 0), 0, total + 6, (float) coffin.half() * 0.6f);
            FourSinsKit.sound(sl, g, SoundEvents.WARDEN_SONIC_CHARGE, 3f, 0.6f);
            announce("Chrono Coffin: a cube of stopped time closes in " + Math.round(total / 20.0) + " seconds. Leave it, or hit Merlin hard enough to break her focus ("
                    + Math.round(MerlinTable.FOCUS_FRACTION * 100) + "% of her health).");
        }
        if (coffin != null) MerlinMoves.cubeEdges(sl, coffin, coffinCalls++, RGB);
        Tells.gather(sl, position().add(0, 1.2, 0), 1.2, ACCENT, 8);
    }
}
