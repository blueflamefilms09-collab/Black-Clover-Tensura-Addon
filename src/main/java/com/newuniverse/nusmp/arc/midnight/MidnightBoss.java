package com.newuniverse.nusmp.arc.midnight;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcBoss;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.entity.zogratis.FxMath;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.MoveScheduler;
import com.newuniverse.nusmp.entity.zogratis.PowerCurve;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Why: Fana, Patolli, Rades Spirito and Rhya are four different fights built on one framework, and what they have in common is not small: the rewriting of
 * the devil-voiced lines of the Zogratis framework into their own, a bar title that names their magic and not a devil, the "open" window (a boss left
 * staggered for a moment, taking a quarter more and starting nothing, which each fight earns in its own way), the rule that no hook may throw out of the
 * server tick, the quip with a rate limit, a safe blink, the choice of spots under the foes for the marks of a crowd move, and the turning of a table row
 * into a framework {@link Move}. One base class holds all of that, so the four bosses are only what is different about them: their moves and their
 * signature mechanic. It adds NO new entity, renderer, registry or data: a boss class is still registered with {@code ArcEntities.boss}.
 * <p>
 * The hooks a boss overrides are the ones named here ({@link #onIncoming}, {@link #guard}, {@link #tickMidnight}, {@link #fightBegins}, {@link #fightEnds},
 * {@link #frozen}), never the framework's own, so the stagger and the safety net cannot be skipped by forgetting a {@code super} call.
 */
public abstract class MidnightBoss extends ArcBoss {
    private final StaggerClock stagger = new StaggerClock();
    private final FxMath.Throttle throttle = new FxMath.Throttle();
    private long lastQuip;
    /** The spots the current wind-up showed on the floor (locked by its first tell, read by the move when it lands). Server side only. */
    protected List<Vec3> marks = List.of();
    /** The direction the current wind-up aimed (locked a few ticks before it lands where the move says so). Server side only. */
    protected Vec3 aim = Vec3.ZERO;

    protected MidnightBoss(EntityType<? extends MidnightBoss> type, Level level) {
        super(type, level);
    }

    // ================================================================ what a boss says about itself
    /** The word on the bar after the name: "Light", "Salamander", "Soul Corpse", "Imitation". */
    protected abstract String word();

    /** The word added to the bar when the Gates are open ("Dark Elf"); empty for none. */
    protected abstract String gatesWord();

    /** What the party reads when the Gates open, instead of the framework's line about the Tree of Qliphoth. */
    protected abstract String gatesAnnouncement();

    /** The attributes of a boss from its record (the stats are the character's; the toughness is the boss's own). */
    protected static AttributeSupplier.Builder attributesOf(ArcCharacter c, double toughness) {
        return ZogratisBoss.bossAttributes(c.health(), c.attack(), c.speed(), c.armor(), toughness);
    }

    @Override public String barTitle() { return MidnightText.barTitle(bossName(), word(), power(), gatesOpen(), gatesWord()); }

    /** The framework announces the Gates and the Triad's rage in a devil's voice; the party reads their own. */
    @Override
    public void announce(String text) {
        super.announce(MidnightText.rewrite(text, devil().displayName, gatesAnnouncement()));
    }

    /** One of {@code lines} now and then (at most every eight seconds), so the chat is not a wall of shouting. */
    protected final void quip(String... lines) {
        if (lines.length == 0 || tickCount - lastQuip < 160) return;
        lastQuip = tickCount;
        say(lines[getRandom().nextInt(lines.length)]);
    }

    // ================================================================ looks and sounds
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.PLAYER_HURT; }

    @Override protected SoundEvent getDeathSound() { return SoundEvents.PLAYER_DEATH; }

    // ================================================================ the damage hooks
    /**
     * The boss's own look at an incoming hit (after the framework's invulnerability checks). Return the amount to apply, 0 or less to ignore the hit. The
     * stagger bonus is added by this class AFTER this hook, so a hook never has to think about it.
     */
    protected float onIncoming(DamageSource source, float amount) { return amount; }

    /** A cut of the hit that does NOT apply while the boss is open (the framework asks only when it is not exposed; an open boss has no guard). */
    protected float guard(DamageSource source, float amount) { return amount; }

    @Override
    protected final float filterDamage(DamageSource source, float amount) {
        float a;
        try {
            a = onIncoming(source, amount);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] {} could not judge a hit: {}", bossName(), e.toString());
            a = amount;
        }
        if (a <= 0f) return a;
        return staggered() ? a * StaggerClock.OPEN_BONUS : a;
    }

    @Override
    protected final float mitigate(DamageSource source, float amount) {
        if (staggered()) return amount;
        try {
            return guard(source, amount);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] {} could not guard a hit: {}", bossName(), e.toString());
            return amount;
        }
    }

    /** The plain blow is a little under the attack attribute (the real hits are the telegraphed ones); half of it while the boss is open. */
    @Override
    protected float meleeDamage() {
        return (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f * MidnightConfig.damageScale() * (staggered() ? 0.5f : 1f);
    }

    // ================================================================ the open window
    /** Is the boss open right now: it takes a quarter more, starts no move, and does only half its plain blow. */
    public boolean staggered() { return stagger.active(tickCount); }

    /**
     * Leaves the boss open for {@code ticks} (the server's lockout between two such windows applies): its move is dropped, it stops, a burst shows it and
     * {@code line} is announced. False when the lockout of the last window still runs.
     */
    protected final boolean openUp(int ticks, String line) {
        if (!stagger.tryStart(tickCount, ticks, MidnightConfig.lockout())) return false;
        interruptCast();
        getNavigation().stop();
        addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 4, false, false));
        if (level() instanceof ServerLevel sl) {
            Tells.burst(sl, position().add(0, getBbHeight() / 2, 0), styleAccent(), 40);
            Tells.ring(sl, position(), 2.5, styleColor());
        }
        if (line != null && !line.isEmpty()) announce(line);
        return true;
    }

    /** A boss in the middle of a channel that must not be interrupted by a new move (Fana's Evil Heart) says so here; the default is "never". */
    protected boolean frozen() { return false; }

    @Override
    protected final int chooseMove(List<Move> bk, MoveScheduler.Situation s, long t) {
        if (staggered() || frozen()) return -1;
        return super.chooseMove(bk, s, t);
    }

    // ================================================================ the fight's life
    /** Called every server tick after the framework's own work: the boss's own upkeep. An exception is caught and logged by the base. */
    protected void tickMidnight() {}

    /** The fight began: say what the party must know, reset what the fight kept. */
    protected void fightBegins() {}

    /** The fight is over (the boss died or the party was gone): put away what the fight made. */
    protected void fightEnds(boolean won) {}

    @Override
    protected final void bossTick() {
        try {
            tickMidnight();
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] {} tick failed: {}", bossName(), e.toString());
        }
    }

    @Override
    protected final void onFightStart() {
        stagger.clear();
        marks = List.of();
        aim = Vec3.ZERO;
        try {
            fightBegins();
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] {} could not begin its fight: {}", bossName(), e.toString());
        }
    }

    @Override
    protected final void onFightEnd(boolean won) {
        super.onFightEnd(won);
        stagger.clear();
        marks = List.of();
        aim = Vec3.ZERO;
        try {
            fightEnds(won);
        } catch (RuntimeException e) {
            LogUtils.getLogger().warn("[nusmp] {} could not end its fight: {}", bossName(), e.toString());
        }
    }

    // ================================================================ helpers for moves
    /** True while the effect packets of this tick are under the allowance (a boss with many marks never floods the clients). */
    final boolean fxAllowed() { return level() instanceof ServerLevel sl && throttle.allow(sl.getGameTime()); }

    protected final int stage() { return PowerCurve.stage(power()); }

    /** Where the boss's hands are: shoulder height. */
    protected final Vec3 hand() { return position().add(0, getBbHeight() * 0.75, 0); }

    protected final Vec3 ground(Vec3 p) { return ZogratisBoss.ground(level(), p); }

    /** The first call of a wind-up (the tell runs every third tick, so the first is within three ticks of the start). */
    protected static boolean first(int left, int total) { return left >= total - 3; }

    protected final void fx(VfxShape shape, Vec3 from, Vec3 to, int rgb, int ticks, float power) { MidnightFx.fx(this, shape, from, to, rgb, ticks, power); }

    /** A safe blink: the boss goes to {@code to} if its body fits there. No rift, no sound of its own; the move draws what it wants. */
    protected final boolean blink(Vec3 to) {
        if (!(level() instanceof ServerLevel sl) || to == null) return false;
        if (!sl.noCollision(this, getBoundingBox().move(to.subtract(position())))) return false;
        teleportTo(to.x, to.y, to.z);
        getNavigation().stop();
        return true;
    }

    /** The first foe in the list of the boss's foes that is alive and is not {@code not}, or null. */
    protected final LivingEntity otherFoe(LivingEntity not, double radius) {
        for (LivingEntity e : foes(radius)) if (e != not && e.isAlive()) return e;
        return null;
    }

    /**
     * Up to {@code n} spots on the floor for the marks of a crowd move: under the target first, then under the other foes (each at least two blocks from the
     * ones before), then scattered round them when there are fewer foes than marks. Never empty when the target lives.
     */
    protected final List<Vec3> spotsAround(LivingEntity target, int n, double scatter) {
        List<Vec3> out = new ArrayList<>();
        if (target != null && target.isAlive()) out.add(ground(target.position()));
        for (LivingEntity e : foes(44)) {
            if (out.size() >= n) break;
            Vec3 g = ground(e.position());
            if (apart(out, g, 2.0)) out.add(g);
        }
        int guard = 0;
        while (out.size() < n && !out.isEmpty() && guard++ < 40) {
            Vec3 base = out.get(getRandom().nextInt(out.size()));
            double a = getRandom().nextDouble() * Math.PI * 2, r = 2.5 + getRandom().nextDouble() * Math.max(0.5, scatter);
            Vec3 g = ground(base.add(Math.cos(a) * r, 0, Math.sin(a) * r));
            if (apart(out, g, 2.0)) out.add(g);
        }
        return out;
    }

    private static boolean apart(List<Vec3> spots, Vec3 p, double min) {
        for (Vec3 s : spots) if (s.distanceToSqr(p) < min * min) return false;
        return true;
    }

    /**
     * Marks {@code spots} on the floor (a ring, a closing ring and a column, redrawn every third tick) for {@code warn} ticks and then calls {@code impact}
     * on each. The marks are where the foes WERE when the move began: leaving them is the whole answer, which is why {@code warn} must be long enough to
     * walk out of a ring of {@code radius} (a table test holds every move to that).
     */
    protected final void markedStrike(List<Vec3> spots, double radius, int warn, int rgb, Consumer<Vec3> impact) {
        if (!(level() instanceof ServerLevel sl) || spots.isEmpty()) return;
        final List<Vec3> pts = List.copyOf(spots);
        final int total = Math.max(1, warn);
        SpellRuntime.zone(sl, total, 3, MidnightFx.safe("mark", age -> {
            if (!isAlive() || isRemoved()) return;
            int left = total - age;
            for (Vec3 c : pts) {
                Tells.ring(sl, c, radius, rgb, 1.3f, (int) Math.min(48, 12 + radius * 5));
                Tells.ring(sl, c, Math.max(0.5, radius * left / total), styleAccent(), 1.1f, 20);
                Tells.column(sl, c, 3.5, styleAccent());
            }
        }));
        later(total, MidnightFx.safe("impact", () -> { for (Vec3 c : pts) impact.accept(c); }));
    }

    /** Draws the marks of a wind-up that lands at its end: a ring, a ring closing in as the ticks run out, and a column on each spot. */
    protected final void drawMarks(List<Vec3> spots, double radius, int left, int total, int rgb) {
        if (!(level() instanceof ServerLevel sl)) return;
        for (Vec3 c : spots) {
            Tells.ring(sl, c, radius, rgb, 1.3f, (int) Math.min(48, 12 + radius * 5));
            Tells.ring(sl, c, Math.max(0.5, radius * left / Math.max(1, total)), styleAccent(), 1.1f, 20);
            Tells.column(sl, c, 3.5, styleAccent());
        }
    }

    /** Turns a table row into a framework move with its telegraph and its code. */
    protected final Move build(MidnightSpec s, Move.MoveTell tell, Move.MoveRun run) {
        Move.Builder b = Move.builder(s.id(), s.label()).minPower(s.minPower()).windUp(s.windUpTicks()).cooldown(s.cooldownTicks())
                .range(s.minRange(), s.maxRange()).weight(s.weight()).mana(s.manaCost()).recovery(s.recovery());
        if (s.ultimate()) b.ultimate();
        if (s.crowd()) b.crowd();
        if (tell != null) b.tell(tell);
        return b.run(run);
    }
}
