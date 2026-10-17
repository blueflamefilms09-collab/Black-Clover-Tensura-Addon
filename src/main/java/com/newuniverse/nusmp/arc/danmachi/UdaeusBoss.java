package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcEntities;
import com.newuniverse.nusmp.arc.ArcLook;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.dungeon.floor.goliath.GoliathMath;
import com.newuniverse.nusmp.entity.zogratis.Move;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
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

/**
 * Why: Udaeus is the Monster Rex of the 37th floor, the White Palace: a skeletal giant with a great black sword whose ribs move to block the blows aimed at it,
 * whose roar raises the Spartoi, and whose magic stone sits behind the sternum (see {@code docs/arc/udaeus.md}). The fight is built on the ribs: they turn to
 * the side the last blow came from and take 70% off every blow from that side ({@link RibGuard}), so one fighter hammering from one side is blocked for ever, but
 * a flank cracks a rib, and a few cracks break the cage open: Udaeus staggers and the stone shows, and everything hurts for a moment. Two fighters on opposite sides,
 * or one who keeps circling, break it in seconds; the Rib Cage Cry shuts the guard wide for a while (strike it from behind). It raises Spartoi, fires ribs, leaps,
 * and ends with the Ossuary Rise; a Leveled form adds the Black Sword Wave. It changes no block, heals nothing, and applies no hard control.
 */
public class UdaeusBoss extends DanMachiBoss {
    public static final ArcCharacter CHARACTER = ArcRoster.register(DanMachiRoster.UDAEUS);

    public static final DeferredHolder<EntityType<?>, EntityType<UdaeusBoss>> TYPE = ArcEntities.boss("udaeus", UdaeusBoss::new, 1.0f, 3.91f,
            new ArcLook("udaeus", 3.91f, 2.79f, 4.99f, 0.47f, 1.17f, 1.50f, true), UdaeusBoss::createAttributes);

    private static final DmFx.Style STYLE = new DmFx.Style(DmFx.Look.BONE, 0xC8B8FF, 0xE0C8FF);
    /** The Rise: the pillars come in four waves, each shown {@value #PILLAR_WARN} ticks before it erupts. */
    private static final int RISE_WAVES = 4, RISE_GAP = 18, PILLAR_WARN = 14;
    private static final double PILLAR_RADIUS = 2.4;
    private static final int CRY_CLOSE_TICKS = 160;

    private final RibGuard guard = new RibGuard();

    public UdaeusBoss(EntityType<UdaeusBoss> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() { return ZogratisBoss.bossAttributes(2600, 26, 0.25, 10, 4); }

    @Override public ArcCharacter character() { return CHARACTER; }

    @Override protected List<DmSpec> table() { return UdaeusTable.specs(); }

    @Override protected int baseLevel() { return 6; }

    @Override protected DmFx.Style style() { return STYLE; }

    @Override protected double preferredRange(double suggested) { return Math.max(3.0, Math.min(10.0, suggested)); }

    // ================================================================ sounds
    @Override protected SoundEvent getHurtSound(DamageSource source) { return SoundEvents.SKELETON_HURT; }

    @Override protected SoundEvent getDeathSound() { return SoundEvents.SKELETON_DEATH; }

    @Override public float getVoicePitch() { return 0.55f + (random.nextFloat() - random.nextFloat()) * 0.08f; }

    // ================================================================ the ribs
    /** The ribs block the side the last blow came from; a flank cracks one; enough cracks break the cage (a stagger, the stone shows, everything hurts more). */
    @Override
    protected float filterDamage(DamageSource source, float amount) {
        float a = super.filterDamage(source, amount);
        if (a <= 0f) return a;
        LivingEntity who = DmBlows.attacker(source);
        double yaw = who == null ? 0.0 : DmGeometry.yawOf(who.getX() - getX(), who.getZ() - getZ());
        RibGuard.Result r = guard.onHit(tickCount, yaw, power(), who != null);
        if (r.broke()) {
            breakCage();
        } else if (r.cracked() && who instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.literal(DmText.ribCracked(guard.cracks(), RibGuard.crackLimit(power()))), true);
            DmFx.burst(this, STYLE, position().add(0, getBbHeight() * 0.6, 0), 14);
        } else if (r.verdict() == RibGuard.Verdict.BLOCKED && who instanceof ServerPlayer sp && tickCount % 10 == 0) {
            sp.displayClientMessage(Component.literal("The ribs turn to meet your blow. Strike it from another side."), true);
        }
        return a * r.factor();
    }

    private void breakCage() {
        if (level() instanceof ServerLevel sl) {
            VfxSpawn.sendFollowing(sl, VfxShape.LUCIUS_CORE_GEM, this, position().add(0, getBbHeight() * 0.68, 0), 0xFF000000 | STYLE.rgb(), RibGuard.STAGGER_TICKS, 0.6f);
            DmFx.burst(this, STYLE, position().add(0, getBbHeight() * 0.65, 0), 40);
            DmShapes.sound(sl, position(), SoundEvents.SKELETON_HURT, 2.0f, 0.4f);
        }
        busyFor(RibGuard.STAGGER_TICKS);
        Move casting = castingMoveOrNull();
        if (state() == STATE_CASTING && casting != null && !casting.ultimate()) interruptCast();       // a break answers an ordinary move, never an ultimate
        announce(DmText.cageBroken(bossName()));
    }

    @Override protected void resetMechanics() { guard.reset(); }

    // ================================================================ its own moves
    @Override
    protected int runCustom(DmSpec s, LivingEntity target) {
        return switch (s.id()) {
            case UdaeusTable.OVERHEAD_CLEAVE -> cleave(s, target);
            case UdaeusTable.SPARTOI_CALL -> spartoiCall();
            case UdaeusTable.RIB_CAGE_CRY -> cry(s);
            case UdaeusTable.OSSUARY_RISE -> rise();
            default -> 0;
        };
    }

    @Override
    protected void tellCustom(DmSpec s, LivingEntity target, int left, int total) {
        boolean first = TellClock.first(left, total);
        Vec3 at = position();
        switch (s.id()) {
            case UdaeusTable.OVERHEAD_CLEAVE -> {
                DmShapes.Aim a = DmShapes.aimOf(this, s, target);
                DmFx.laneTell(this, STYLE, at, at.add(a.fx * s.reach(), 0, a.fz * s.reach()), 3.0, first ? total : 0);
                if (level() instanceof ServerLevel sl) Tells.ring(sl, at.add(a.fx * (s.reach() - 1), 0, a.fz * (s.reach() - 1)), 5.0, STYLE.tint(this), 1.1f, 28);
            }
            case UdaeusTable.SPARTOI_CALL -> {
                DmShapes.groundRing(this, STYLE, at, 7.0, first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.8, 0), 1.2, 8);
            }
            case UdaeusTable.RIB_CAGE_CRY -> {
                DmShapes.groundRing(this, STYLE, at, s.reach(), first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.65, 0), 1.2, 8);
            }
            case UdaeusTable.OSSUARY_RISE -> {
                DmShapes.groundRing(this, STYLE, at, s.reach(), first ? total : 0);
                DmFx.gather(this, STYLE, at.add(0, getBbHeight() * 0.5, 0), 2.4, 14);
            }
            default -> super.tellCustom(s, target, left, total);
        }
    }

    /** The Overhead Cleave: the great sword comes down along a lane in front of it, and the ground answers with a ring where the blade lands. */
    private int cleave(DmSpec s, LivingEntity target) {
        if (!(level() instanceof ServerLevel sl)) return 0;
        DmShapes.Aim a = DmShapes.aimOf(this, s, target);
        Vec3 at = position();
        DmFx.laneStrike(this, STYLE, at, at.add(a.fx * s.reach(), 0, a.fz * s.reach()), 3.0, 14);
        DmShapes.sound(sl, at, SoundEvents.PLAYER_ATTACK_STRONG, 1.8f, 0.4f);
        GoliathMath.DamageBudget budget = DmShapes.budget();
        DmShapes.laneBlow(this, at, a.fx, a.fz, s.reach(), 3.0, UdaeusTable.CLEAVE_HIT, 1.6, 0.5, new HashSet<>(), budget, false);
        Vec3 landing = at.add(a.fx * (s.reach() - 1), 0, a.fz * (s.reach() - 1));
        later(4, DmSafe.task("cleave shock", () -> {
            if (isAlive()) DmShapes.waves(this, landing, 5.0, UdaeusTable.CLEAVE_SHOCK, 1.0, 1, 0);
        }));
        return 24;
    }

    /** The Call of the Spartoi: the roar raises skeleton warriors, as many as the cap of the power (and of the config) leaves room for. */
    private int spartoiCall() {
        if (!(level() instanceof ServerLevel sl)) return 0;
        int cap = AddPlan.cap(power(), DanMachiConfig.maxAdds());
        int n = AddPlan.toSummon(cap, DmAdds.alive(this), tuning().castSlots());
        sl.playSound(null, blockPosition(), SoundEvents.WITHER_SKELETON_AMBIENT, SoundSource.HOSTILE, 3.0f, 0.5f);
        if (DmAdds.summon(this, DmAdds.Kind.SPARTOI, n, position(), 6.5, STYLE) > 0) announce("The Spartoi rise from the bones of the White Palace!");
        return 10;
    }

    /** The Rib Cage Cry: the ribs shut wide for eight seconds (strike it from behind), and three small rings of sound run out. */
    private int cry(DmSpec s) {
        guard.close(tickCount, CRY_CLOSE_TICKS);
        announce("The ribs close like a cage. Strike from behind.");
        return DmShapes.waves(this, position(), s.reach(), UdaeusTable.CRY_PULSE, 0.8, 3, 12);
    }

    /**
     * The Ossuary Rise, its ultimate: four waves of bone pillars across the room, each wave shown for a second before it erupts. The first pillar of every wave stands
     * on the nearest fighter, the others on the rest and round them; the gaps are always wide enough to stand in. It is left exposed afterwards.
     */
    private int rise() {
        GoliathMath.DamageBudget budget = DmShapes.budget();
        for (int w = 0; w < RISE_WAVES; w++) later(w * RISE_GAP, DmSafe.task("ossuary wave", () -> pillarWave(budget)));
        return RISE_WAVES * RISE_GAP + PILLAR_WARN + 6;
    }

    private void pillarWave(GoliathMath.DamageBudget budget) {
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        LivingEntity first = nearestFoe();
        if (first == null) return;
        List<double[]> others = new ArrayList<>();
        for (LivingEntity e : foesAround(position(), 48.0)) if (e != first) others.add(new double[]{e.getX(), e.getZ()});
        int n = 4 + tuning().castSlots();
        List<double[]> plan = DmGeometry.markPlan(n, first.getX(), first.getZ(), others.toArray(new double[0][]), PILLAR_RADIUS, getRandom().nextDouble() * Math.PI * 2);
        List<Vec3> spots = new ArrayList<>();
        for (double[] p : plan) {
            Vec3 g = ZogratisBoss.groundOrNull(sl, new Vec3(p[0], first.getY(), p[1]));
            spots.add(g == null ? new Vec3(p[0], first.getY(), p[1]) : g);
        }
        int shown = 0;
        for (Vec3 m : spots) {
            if (shown++ < 4) DmFx.spotTell(this, STYLE, m, PILLAR_RADIUS, PILLAR_WARN);
            else Tells.ring(sl, m, PILLAR_RADIUS, STYLE.tint(this), 1.1f, 20);
        }
        later(PILLAR_WARN, DmSafe.task("ossuary pillars", () -> erupt(spots, budget)));
    }

    private void erupt(List<Vec3> spots, GoliathMath.DamageBudget budget) {
        if (!(level() instanceof ServerLevel sl) || !isAlive()) return;
        HashSet<Integer> hit = new HashSet<>();
        for (Vec3 m : spots) {
            DmFx.spotStrike(this, STYLE, m, PILLAR_RADIUS);
            DmShapes.sound(sl, m, SoundEvents.SKELETON_HURT, 1.0f, 0.5f);
            for (LivingEntity e : foesAround(m, PILLAR_RADIUS + 3)) {
                if (hit.contains(e.getId())) continue;
                double dx = e.getX() - m.x, dz = e.getZ() - m.z;
                if (!DmShapes.inBand(e.getY() - m.y) || Math.hypot(dx, dz) > PILLAR_RADIUS + e.getBbWidth() / 2.0) continue;
                double d = Math.max(1e-3, Math.hypot(dx, dz));
                if (DmShapes.land(this, e, UdaeusTable.RISE_PILLAR, 1.0, 0.7, dx / d, dz / d, budget, false)) hit.add(e.getId());
            }
        }
    }
}
