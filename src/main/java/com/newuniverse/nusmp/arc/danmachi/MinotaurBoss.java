package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: the Minotaur is the first monster of the DanMachi saga: the Level 2 bull that does not belong on the 5th floor and hunts one boy through it (see
 * {@code docs/arc/minotaur.md}). It is a close, heavy fight (horn, fist, slam, sweep), a bull that charges at whoever it has marked, and its own mechanic is the
 * Hunt: it fixes on the weakest fighter (lowest Dungeon Level) and takes more from them, until a louder fighter draws its eye ({@link PreyBook}); the charge
 * ends in a stun when it rams a wall, so the answer to a bull is a wall at your back. When the party is strong enough the Gates bring it back as Asterius, the
 * one-horned (the leveled form, with the Wild Cross). Its Howl frightens fighters below its Level (a slow and a weakness, never a hard stop). It changes no
 * block, heals nothing, and its moves are the table of {@link MinotaurTable}.
 */
public class MinotaurBoss extends DanMachiBoss {
    public static final ArcCharacter CHARACTER = ArcRoster.register(DanMachiRoster.MINOTAUR);

    public static final DeferredHolder<EntityType<?>, EntityType<MinotaurBoss>> TYPE = ArcEntities.boss("minotaur", MinotaurBoss::new, 1.6f, 3.09f,
            new ArcLook("minotaur", 3.09f, 1.55f, 2.82f, 0.70f, 1.73f, 0.60f, true), MinotaurBoss::createAttributes);

    /** Blocks a tick of a Rampage dash, and of the Bull Run. */
    private static final double DASH_SPEED = 1.3, RUN_SPEED = 1.6;
    private static final DmFx.Style STYLE = new DmFx.Style(DmFx.Look.STONE, 0xFFAA33, 0xFF5A2E);

    private final PreyBook prey = new PreyBook();

    public MinotaurBoss(EntityType<MinotaurBoss> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() { return ZogratisBoss.bossAttributes(1000, 16, 0.30, 8, 3); }

    @Override public ArcCharacter character() { return CHARACTER; }

    @Override protected List<DmSpec> table() { return MinotaurTable.specs(); }

    @Override protected int baseLevel() { return 2; }

    @Override protected DmFx.Style style() { return STYLE; }

    /** It wants to be close: its horn and fist reach a few blocks, and it never stands off. */
    @Override protected double preferredRange(double suggested) { return Math.max(2.5, Math.min(9.0, suggested)); }

    // ================================================================ sounds
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.RAVAGER_HURT; }

    @Override protected SoundEvent getDeathSound() { return SoundEvents.RAVAGER_DEATH; }

    @Override protected SoundEvent getAmbientSound() { return SoundEvents.RAVAGER_AMBIENT; }

    // ================================================================ the Hunt
    @Override
    protected void perSecond(long now) {
        if (!fightActive() || !(level() instanceof ServerLevel sl)) return;
        List<PreyBook.Candidate> candidates = new ArrayList<>();
        for (LivingEntity e : foesAround(position(), 40.0)) candidates.add(new PreyBook.Candidate(e.getId(), PartyLevels.levelOf(e), e.distanceTo(this)));
        int before = prey.prey();
        int id = prey.update(now, candidates);
        if (id < 0 || !(sl.getEntity(id) instanceof LivingEntity victim) || !victim.isAlive()) return;
        if (id != before && victim instanceof ServerPlayer sp) sp.displayClientMessage(Component.literal(DmText.marked()), true);
        if (getTarget() != victim) setTarget(victim);
        Tells.ring(sl, victim.position(), 1.1, STYLE.tint(this), 1.2f, 16);
    }

    /**
     * The framework's own target goals retarget to whoever hurt it last or to the nearest player, which would make every blow a theft of the mark. While it hunts, only the prey
     * (or nobody) is accepted as its target; the mark moves by the Hunt's own rules (the prey is chosen again, or a louder fighter steals it).
     */
    @Override
    public void setTarget(LivingEntity target) {
        if (target != null && prey != null && fightActive() && prey.hasPrey() && target.getId() != prey.prey() && level() instanceof ServerLevel sl
                && sl.getEntity(prey.prey()) instanceof LivingEntity hunted && hunted.isAlive()) return;
        super.setTarget(target);
    }

    /** A louder fighter steals the mark: three hits within three seconds, or one blow of a twentieth of its health. */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        float a = super.filterDamage(source, amount);
        LivingEntity who = DmBlows.attacker(source);
        if (who != null && a > 0f && prey.onHit(tickCount, who.getId(), a, getMaxHealth())) {
            setTarget(who);
            announce(DmText.markStolen(who.getName().getString()));
        }
        return a;
    }

    /** The prey takes more of every blow ({@link PreyBook#PREY_BONUS}). */
    @Override protected float victimFactor(LivingEntity victim) { return prey.bonusFor(victim.getId()); }

    @Override protected void resetMechanics() { prey.clear(); }

    // ================================================================ its own moves
    @Override
    protected int runCustom(DmSpec s, LivingEntity target) {
        return switch (s.id()) {
            case MinotaurTable.HOWL -> howl(s);
            case MinotaurTable.RAMPAGE -> rampage(s);
            case MinotaurTable.BULL_RUN -> bullRun(s, target);
            case MinotaurTable.WILD_CROSS -> wildCross(s, target);
            default -> 0;
        };
    }

    @Override
    protected void tellCustom(DmSpec s, LivingEntity target, int left, int total) {
        if (!(level() instanceof ServerLevel sl)) return;
        boolean first = TellClock.first(left, total);
        Vec3 at = position();
        switch (s.id()) {
            case MinotaurTable.HOWL -> {
                DmShapes.groundRing(this, STYLE, at, HowlMath.radius(s.reach(), power()), first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.8, 0), 1.0, 8);
            }
            case MinotaurTable.RAMPAGE -> {
                DmShapes.groundRing(this, STYLE, at, 3.5, first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.5, 0), 1.4, 8);
            }
            case MinotaurTable.BULL_RUN -> {
                DmShapes.Aim a = DmShapes.aimOf(this, s, target);
                DmFx.laneTell(this, STYLE, at, at.add(a.fx * s.reach(), 0, a.fz * s.reach()), 4.5, first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.5, 0), 1.8, 10);
            }
            case MinotaurTable.WILD_CROSS -> {
                DmShapes.Aim a = DmShapes.aimOf(this, s, target);
                for (double[] d : crossDirs(a, 0.0)) DmFx.laneTell(this, STYLE, at, at.add(d[0] * s.reach(), 0, d[1] * s.reach()), 2.6, first ? total : 0);
                for (double[] d : crossDirs(a, Math.PI / 4)) Tells.line(sl, at, at.add(d[0] * s.reach(), 0, d[1] * s.reach()), STYLE.tint(this));
            }
            default -> super.tellCustom(s, target, left, total);
        }
    }

    /** The Howl: a ring that shouts (a small blow to everyone in it) and frightens the fighters below the Minotaur's Level. */
    private int howl(DmSpec s) {
        if (!(level() instanceof ServerLevel sl)) return 0;
        Vec3 at = position();
        double radius = HowlMath.radius(s.reach(), power());
        DmFx.ringStrike(this, STYLE, at, radius, 20);
        DmFx.burst(this, STYLE, at.add(0, getBbHeight() * 0.8, 0), 30);
        sl.playSound(null, blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 3.0f, 0.5f);
        DmShapes.frighten(this, at, radius, dungeonLevel(), power());
        for (LivingEntity e : foesAround(at, radius)) strike(e, MinotaurTable.HOWL_SHOUT, false);
        return 8;
    }

    /** The Rampage: three to five dashes (by power) at whoever is nearest, each shown for {@link ChargeMath#DASH_TELL} ticks before it runs. A wall ends it in a stun. */
    private int rampage(DmSpec s) {
        int dashes = ChargeMath.rampageDashes(power());
        if (dashes <= 0) return 0;
        double length = ChargeMath.dashLength(s.reach(), dashes);
        int breath = ChargeMath.rampageBreath(power());
        int each = ChargeMath.DASH_TELL + ChargeMath.ticks(length, DASH_SPEED) + breath;
        for (int i = 0; i < dashes; i++) later(i * each, DmSafe.task("rampage dash", () -> oneDash(length)));
        return dashes * each;
    }

    private void oneDash(double length) {
        if (!isAlive() || stunned()) return;
        LivingEntity foe = nearestFoe();
        if (foe == null) return;
        double[] f = DmGeometry.unit(foe.getX() - getX(), foe.getZ() - getZ(), DmGeometry.dirOfYaw(getYRot()));
        Vec3 at = position();
        DmFx.laneTell(this, STYLE, at, at.add(f[0] * length, 0, f[1] * length), 3.0, ChargeMath.DASH_TELL);
        later(ChargeMath.DASH_TELL, DmSafe.task("rampage run", () -> {
            if (isAlive() && !stunned()) DmShapes.charge(this, f[0], f[1], length, 3.0, MinotaurTable.RAMPAGE_DASH, 1.2, DASH_SPEED, null);
        }));
    }

    /** The Bull Run, its ultimate: one run the length of the room at the target's side, and a quake where it ends (a wall ends it sooner, and in a stun). */
    private int bullRun(DmSpec s, LivingEntity target) {
        DmShapes.Aim a = DmShapes.aimOf(this, s, target);
        Runnable after = DmSafe.task("bull run quake", () -> DmShapes.waves(this, position(), 10.0, MinotaurTable.BULL_RUN_QUAKE, 1.2, 1, 0));
        return DmShapes.charge(this, a.fx, a.fz, s.reach(), 4.5, MinotaurTable.BULL_RUN_HIT, 2.0, RUN_SPEED, after) + 24;
    }

    /** The four spokes of a cross from the boss, turned {@code turn} radians from the way the aim faces. */
    private static double[][] crossDirs(DmShapes.Aim a, double turn) { return DmGeometry.spokes(4, Math.atan2(a.fz, a.fx) + turn); }

    /** The Wild Cross of Asterius: a "+" at its aim and, twelve ticks later, an "x" between the arms. Each foe takes each stroke at most once. */
    private int wildCross(DmSpec s, LivingEntity target) {
        DmShapes.Aim a = DmShapes.aimOf(this, s, target);
        Set<Integer> hit = new HashSet<>();
        var budget = DmShapes.budget();
        DmShapes.spokesAt(this, crossDirs(a, 0.0), s.reach(), 2.6, MinotaurTable.CROSS_LANE, 1.0, hit, budget);
        later(12, DmSafe.task("wild cross", () -> {
            if (!isAlive()) return;
            hit.clear();
            DmShapes.spokesAt(this, crossDirs(a, Math.PI / 4), s.reach(), 2.6, MinotaurTable.CROSS_LANE, 1.0, hit, budget);
        }));
        return 14;
    }
}
