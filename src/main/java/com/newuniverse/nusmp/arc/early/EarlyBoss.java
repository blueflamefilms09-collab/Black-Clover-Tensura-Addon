package com.newuniverse.nusmp.arc.early;

import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Why: the five early bosses of Saga 1 are different fights on the same chassis, and a chassis of its own keeps them from repeating the same dozen small
 * chores five times. This base class adds, on top of {@link ArcBoss} (and so on top of the Zogratis framework: moves with telegraphs, the power curve, the
 * damage budget, the second health bar at the Gates), only what all five need: a boss-bar title that does not name a devil (the framework prints the
 * devil's name, and these characters have none); a line of speech that is not repeated every few seconds ({@link #quip}); the spot a move aims at, which
 * follows the target for the first part of the wind-up and then stays where it was drawn ({@link #aimMark}, so moving off a marked spot is always the
 * answer); a stagger (the boss is open to a blow and starts nothing, after a cast was broken or a plan failed); and the break of a long cast by a burst
 * of damage ({@link ChannelGuard}), which four of the five ultimates share, so the party learns one lesson and meets it again in every fight.
 *
 * <p>Subclasses override {@code filterDamage}, {@code chooseMove} and {@code onFightEnd} and must call {@code super} (the stagger and the guard live there).
 * Nothing here is saved: a fight that is cut by a reload starts its mechanics afresh, the way the framework resets the power when the arena is abandoned.
 */
public abstract class EarlyBoss extends ArcBoss {
    private long lastQuip;
    private Vec3 mark;
    private List<Vec3> marks = List.of();
    private final Window stagger = new Window();
    private float staggerBonus = 1f;
    private final ChannelGuard guard = new ChannelGuard();

    protected EarlyBoss(EntityType<? extends ArcBoss> type, Level level) {
        super(type, level);
    }

    // ================================================================ how the boss presents itself
    /** "Revchi Salik - 40%": the framework would add the devil's name; the number is how much of its strength it has drawn. */
    @Override public String barTitle() { return bossName() + " - " + power() + "%" + (gatesOpen() ? " - Full power" : ""); }

    /** The chat colour of its speech. */
    @Override protected abstract ChatFormatting chatColor();

    /** One of {@code lines} now and then (at most every eight seconds), so the chat is not a wall of shouting. */
    protected final void quip(String... lines) {
        if (tickCount - lastQuip < 160 || lines.length == 0) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    /** Is this the first call of a wind-up's telegraph (the tell runs every third tick, so the first is within three ticks of the start)? */
    protected static boolean first(int left, int total) { return left >= total - 3; }

    // ================================================================ the spot a move aims at
    /**
     * The spot a move aims at: the target's feet, followed for the first {@code followFraction} of the wind-up and then left where it was, so a foe who moves
     * off the drawn spot is out of the move. Call it from the tell; {@link #takeMark} reads it at the release.
     */
    protected final Vec3 aimMark(LivingEntity target, int left, int total, double followFraction) {
        double done = total <= 0 ? 1 : (total - left) / (double) total;
        if (mark == null || first(left, total) || done < followFraction) mark = ground(level(), target.position());
        return mark;
    }

    /** The spot the tell showed, or the target's feet now when it showed none; it is forgotten. */
    protected final Vec3 takeMark(LivingEntity target) {
        Vec3 m = mark != null ? mark : ground(level(), target.position());
        mark = null;
        return m;
    }

    protected final Vec3 peekMark() { return mark; }

    /** Several spots (one for each foe a move marks). */
    protected final List<Vec3> marks() { return marks; }

    protected final void setMarks(List<Vec3> spots) { marks = List.copyOf(spots); }

    protected final List<Vec3> takeMarks() {
        List<Vec3> m = marks;
        marks = List.of();
        return m;
    }

    protected final void forgetMarks() {
        mark = null;
        marks = List.of();
    }

    // ================================================================ stagger
    /** Opens the boss to a blow ({@code bonus} times the damage) and keeps it from starting a move for {@code ticks} ticks. */
    protected final void stagger(int ticks, float bonus) {
        stagger.open(level().getGameTime(), ticks);
        staggerBonus = Math.max(1f, bonus);
    }

    protected final boolean staggered() { return stagger.active(level().getGameTime()); }

    @Override
    protected int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (stagger.active(t)) return -1;
        return super.chooseMove(bk, s, t);
    }

    // ================================================================ the break of a long cast
    /** The id of the ultimate whose wind-up a burst of damage breaks ("" for none). */
    protected String guardedMove() { return ""; }

    /** The share of its maximum health that breaks the guarded cast. */
    protected double guardShare() { return ChannelGuard.DEFAULT_SHARE; }

    /** Called once when the guarded cast has just been broken (the cast is already dropped): say so, open the boss to a blow. */
    protected void onChannelBroken() {}

    /** Starts watching the cast: call it from the guarded move's tell, on the first call. */
    protected final void armGuard() { guard.arm(ChannelGuard.threshold(getMaxHealth(), guardShare())); }

    protected final void disarmGuard() { guard.disarm(); }

    protected final double guardProgress() { return guard.progress(); }

    // ================================================================ the damage that lands
    /**
     * The part of a blow that will count: the framework runs the vanilla hurt next, which ignores a blow that does not rise above the last one inside the
     * invulnerability frames, and only the difference of one that does. The meters and guards count what lands, not what was swung.
     */
    protected final float landed(DamageSource source, float amount) {
        float incoming = amount;
        if (invulnerableTime > 10 && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (incoming <= lastHurt) return 0f;
            incoming -= lastHurt;
        }
        return incoming;
    }

    /** Is this blow anti-magic: a demon sword in hand, the Anti-Magic Lord's own blows, or a grimoire of anti-magic floating? */
    protected final boolean antiMagic(DamageSource source) { return DanteMoves.antiMagicHit(source); }

    /** Does this fighter carry anti-magic (a demon sword in hand, or the Anti-Magic Lord)? */
    protected static boolean carriesAntiMagic(LivingEntity e) {
        return e instanceof Player p && DanteMoves.antiMagicHeld(p);
    }

    @Override
    protected float filterDamage(DamageSource source, float amount) {
        float a = amount;
        if (guard.armed() && !guard.broken() && state() == STATE_CASTING) {
            Move m = castingMoveOrNull();
            if (m != null && m.id().equals(guardedMove()) && guard.hit(landed(source, a))) {
                interruptCast();
                guard.disarm();
                if (level() instanceof ServerLevel) onChannelBroken();
            }
        }
        if (stagger.active(level().getGameTime())) a *= staggerBonus;
        return a;
    }

    @Override
    protected void onFightEnd(boolean won) {
        super.onFightEnd(won);
        stagger.close();
        guard.disarm();
        forgetMarks();
    }

    @Override
    protected void bossTick() {
        super.bossTick();
        if (level() instanceof ServerLevel sl && stagger.active(sl.getGameTime()) && tickCount % 6 == 0)
            Tells.gather(sl, position().add(0, getBbHeight() * 0.6, 0), 0.6, 0xFFFFFF, 5);
    }

    /** One move from its table row, the code that runs when the wind-up ends and its telegraph (null: the framework's default ring). */
    protected static Move build(MoveRow row, Move.MoveRun run, Move.MoveTell tell) {
        Move.Builder b = Move.builder(row.id(), row.label()).minPower(row.minPower()).windUp(row.windUpTicks()).cooldown(row.cooldownTicks())
                .range(row.minRange(), row.maxRange()).weight(row.weight()).mana(row.manaCost()).recovery(row.recovery());
        if (row.ultimate()) b.ultimate();
        if (row.crowd()) b.crowd();
        if (tell != null) b.tell(tell);
        return b.run(run);
    }

    /** The moves of a table, each paired with the code and telegraph the boss gives it (a missing telegraph is the framework's default). */
    protected static List<Move> buildAll(List<MoveRow> rows, Function<String, Move.MoveRun> runs, Function<String, Move.MoveTell> tells) {
        List<Move> out = new ArrayList<>();
        for (MoveRow r : rows) out.add(build(r, runs.apply(r.id()), tells.apply(r.id())));
        return out;
    }
}
