package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.poise.Desperation;
import com.newuniverse.nusmp.labyrinth.mega.poise.PoiseCue;
import com.newuniverse.nusmp.labyrinth.mega.poise.PoiseMath;
import com.newuniverse.nusmp.labyrinth.mega.poise.PoiseSystem;
import com.newuniverse.nusmp.labyrinth.mega.poise.TelegraphDef;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the owner's design is that a strong enemy WARNS before it strikes, and that warning is a fair fight: the party sees the ground the attack will hit, has long enough to read it and
 * move, and can break the attacker's poise to stop it. This is the ability goal of the Mob Foundry with that added, for every ability the telegraph table
 * ({@code data/nusmp/mega/poise/telegraphs.json}) knows, whether it is a cleave, a slam, a leap, a dash, a dive, an aura or an ultimate: the abilities, their cooldowns and the brain that picks
 * one are the existing ones ({@link FoundryAbility}, {@link FoundryBrain}, {@link FoundryAbilityRunner}); this goal changes only the wind-up. (1) The wind-up is at least the telegraph's minimum
 * (so a 4-tick lunge becomes a readable 10), shorter in desperation but never under the readable floor. (2) While it lasts, the outline of the exact ground the attack will hit is drawn
 * with the mod's particles every four ticks, with a smaller copy growing inside it. (3) The mob has super armor from the first tick of the wind-up until just after the swing, if the row
 * says so ({@code PoiseSystem}); a bar that breaks cancels the cast, starts the whole cooldown and leaves the mob helpless. (4) The aim follows the target until the last third of the wind-up,
 * then freezes, and the attack lands where it was frozen: what the floor showed is what is hit, and a party that steps aside is safe. It extends {@link FoundryGoals.AbilityGoal}, so the old goal
 * is what runs when {@code foundry.poise.telegraphedAttacks} is off.
 */
final class TelegraphedAttackGoal extends FoundryGoals.AbilityGoal {
    private TelegraphDef def;
    private FoundryAbility ability;
    private int wind;
    private double aimX, aimY, aimZ;
    private boolean dark;

    TelegraphedAttackGoal(FoundryMobEntity m) { super(m); }

    @Override
    public boolean canUse() { return !PoiseSystem.staggered(m) && super.canUse(); }

    @Override
    public boolean canContinueToUse() { return !PoiseSystem.staggered(m) && super.canContinueToUse(); }

    @Override
    public void start() {
        active = pending;
        ability = m.brain().abilities().get(active);
        locked = m.foe();
        def = PoiseSystem.telegraphs().def(ability);
        boolean desperate = PoiseSystem.desperate(m);
        wind = Math.max(1, def == null ? Desperation.wind(Math.max(1, ability.wind()), desperate, PoiseSystem.tuning())
                : PoiseMath.wind(ability.wind(), def.minWind(), desperate, PoiseSystem.tuning()));
        dark = ability.dark(m.road() == Polarity.ABYSSAL);
        Vec3 at = locked != null ? locked.position() : m.position().add(m.getLookAngle());
        aimX = at.x;
        aimY = at.y;
        aimZ = at.z;
        m.setCast(wind, wind);
        m.getNavigation().stop();
        if (def != null && def.armor()) PoiseSystem.raiseArmor(m, wind + PoiseSystem.tuning().armorTail());
    }

    @Override
    public void tick() {
        LivingEntity t = locked != null && locked.isAlive() ? locked : m.foe();
        int left = m.castLeft() - 1;
        boolean frozen = left <= Math.max(5, wind / 3);
        if (t != null && !frozen) {
            aimX = t.getX();
            aimY = t.getY();
            aimZ = t.getZ();
            m.getLookControl().setLookAt(t, 40.0f, 40.0f);
        } else if (frozen) {
            m.getLookControl().setLookAt(aimX, m.getEyeY(), aimZ);
        }
        if (m.level() instanceof ServerLevel level && left > 0 && left % 4 == 0) {
            RukhFx.gather(level, m, RukhFx.Alignment.of(dark), 3);
            if (def != null) draw(level, left);
        }
        if (left <= 0) {
            m.setCast(0, 0);
            m.brain().used(active, m.tickCount);
            if (def != null) m.fx.aim = new Vec3(aimX, aimY, aimZ);
            FoundryAbilityRunner.fire(m, ability, t);
            m.fx.aim = null;
            active = -1;
        } else {
            m.setCast(left, m.castMax());
        }
    }

    /** Draws the warning of this tick: the shape at its place, facing the aim, with the share of the wind-up that has passed. */
    private void draw(ServerLevel level, int left) {
        double ox = def.atTarget() ? aimX : m.getX(), oy = def.atTarget() ? aimY : m.getY(), oz = def.atTarget() ? aimZ : m.getZ();
        double dx = aimX - m.getX(), dz = aimZ - m.getZ();
        double yaw = dx * dx + dz * dz < 1.0E-4 ? Math.toRadians(m.yBodyRot + 90.0) : Math.atan2(dz, dx);
        double progress = 1.0 - left / (double) Math.max(1, wind);
        PoiseCue.shape(level, def.shape(), def.radiusFor(ability), def.angleFor(ability), def.lengthFor(ability), def.halfWidth(), dark, ox, oy, oz, yaw, progress);
    }

    @Override
    public void stop() {
        if (active >= 0) {                                   // cut short (the bar broke, it was pushed off by another goal): the cast is lost and its whole cooldown starts
            m.brain().used(active, m.tickCount);
            if (def != null && def.armor()) PoiseSystem.dropArmor(m);
        }
        m.fx.aim = null;
        super.stop();
    }

    /** For the in-game self-test: starts the ability at index {@code index} against {@code target} without waiting for the brain to choose it. */
    void begin(int index, LivingEntity target) {
        m.setTarget(target);
        pending = index;
        start();
    }

    /** For the self-test: one tick of the cast; false when the cast is over (it fired, or it was cut short). */
    boolean advance() {
        if (!canContinueToUse()) return false;
        tick();
        return true;
    }

    /** For the self-test: runs one whole cast and returns the ticks it took. */
    int drill(int index, LivingEntity target) {
        begin(index, target);
        int ticks = 0;
        while (ticks < 400 && advance()) ticks++;
        stop();
        return ticks;
    }

    /** The wind-up the last cast got, and whether it drew a warning (for the self-test). */
    int lastWind() { return wind; }

    boolean lastTelegraphed() { return def != null; }
}
