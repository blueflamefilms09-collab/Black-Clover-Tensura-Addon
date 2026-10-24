package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;
import com.newuniverse.nusmp.mana.method.RuneCharge.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * The spell filter of the Mana Method (pure, tested). In the lore a rune array does not make the spell, it COMMANDS it: the array hangs in the air a few blocks in front of the caster
 * and the projectile leaves from it carrying whatever the runes told it. This class is that filter, made explicit: {@link #inject} takes the rune's {@link RuneSpec} (which commands are
 * written) and the {@link Charge} (how much of the array the caster completed) and returns the {@link BoltState} the bolt carries from the launch on. The two commands the canon names:
 * <ul>
 *   <li><b>PURSUE</b> sets {@link BoltState#isHoming()} (the bolt turns toward the foe it picked at the launch, at most {@link BoltState#turn()} radians a tick for
 *       {@link BoltState#homingTicks()} ticks);</li>
 *   <li><b>GROW</b> sets {@link BoltState#grows()} and the scaling numbers: the hit volume and the damage rise with every block flown, up to a cap, and a bigger charge scales both the rate
 *       and the cap.</li>
 * </ul>
 * Both come out of the same record at the launch; the flight only reads it. "Explode on contact" (the third command the canon mentions) is not a flag: every bolt already bursts at the first
 * creature or wall it meets (the impact), so it needs no rune. The rune itself is a small server record, not an entity: a vanilla entity per rune would be a tracked, ticked, saved and synced
 * object for every cast on a public server (docs/magic/mana_method.md, "Why the rune is not an entity").
 */
public final class RuneCommands {
    private RuneCommands() {}

    public enum Command {
        PURSUE("pursue"), GROW("grow");

        public final String word;

        Command(String word) { this.word = word; }
    }

    /** The commands a spec writes, in the order the runes are read. */
    public static List<Command> commandsOf(RuneSpec s) {
        List<Command> out = new ArrayList<>(2);
        if (s.homing()) out.add(Command.PURSUE);
        if (s.growth()) out.add(Command.GROW);
        return out;
    }

    /** The upper bounds of everything a bolt can be scaled to (whatever the charge and the settings say). */
    public static final double MAX_HIT_RADIUS = 3.6, MAX_SPLASH = 4.8, MAX_DAMAGE_MULT = 3.0;

    /** The data the rune injects into the bolt. Immutable; the flight reads it, nothing writes it. */
    public record BoltState(boolean isHoming, int homingTicks, double turn,
                            boolean grows, double radiusBase, double radiusPerBlock, double radiusMax, double damagePerBlock, double damageMax,
                            double splashBase, double splashMax, double damageMult) {

        /** Is the bolt still steering at this age (ticks since the launch)? */
        public boolean homingActive(int age) { return isHoming && age < homingTicks; }

        /** x1 at the rune, rising with the distance up to the cap; always 1 without GROW. */
        public double growthScale(double travelled) {
            if (!grows) return 1.0;
            double d = Double.isNaN(travelled) ? 0.0 : Math.max(0.0, travelled);
            return Math.min(radiusMax, 1.0 + radiusPerBlock * d);
        }

        /** The radius of the hit volume after {@code travelled} blocks. */
        public double hitRadius(double travelled) { return Math.min(MAX_HIT_RADIUS, radiusBase * growthScale(travelled)); }

        /** The growth of the damage over the distance (1 without GROW); the charge's power is {@link #damageMult()}, applied once on top. */
        public double damageFactor(double travelled) {
            if (!grows) return 1.0;
            double d = Double.isNaN(travelled) ? 0.0 : Math.max(0.0, travelled);
            return Math.min(damageMax, 1.0 + damagePerBlock * d);
        }

        public double splashRadius(double travelled) {
            return Math.min(Math.min(MAX_SPLASH, splashMax), splashBase * (grows ? Math.sqrt(growthScale(travelled)) : 1.0));
        }
    }

    /** The turn rate multiplier of a stage (a basic spell pursues lazily, a full one sharply); 1 for the legacy spell. */
    public static double turnMult(Charge c) {
        if (c.legacy()) return 1.0;
        return c.stage() == Stage.BASIC ? 0.75 : c.stage() == Stage.FULL ? 1.25 : 1.0;
    }

    /**
     * The filter: the commands of {@code spec} and the completeness of the array ({@code c}) in, the bolt's state out. With {@link Charge#LEGACY} the numbers are exactly the 3.4.0 ones
     * ({@link RuneRules}), which the tests pin value by value.
     */
    public static BoltState inject(RuneSpec spec, Charge c) {
        double g = c.growth(), size = c.size();
        double radiusMax = 1.0 + (RuneRules.GROWTH_RADIUS_MAX - 1.0) * g;
        double damageMax = 1.0 + (RuneRules.GROWTH_DAMAGE_MAX - 1.0) * g;
        double power = Math.max(0.0, Math.min(MAX_DAMAGE_MULT, c.power()));
        return new BoltState(spec.homing(), RuneRules.HOMING_TICKS, RuneRules.HOMING_TURN * turnMult(c),
                spec.growth(), RuneRules.BASE_HIT_RADIUS * size, RuneRules.GROWTH_RADIUS_PER_BLOCK * g, radiusMax, RuneRules.GROWTH_DAMAGE_PER_BLOCK * g, damageMax,
                RuneRules.SPLASH_RADIUS * size, RuneRules.SPLASH_RADIUS_MAX * size, power);
    }

    /** The bolt of the 3.4.0 spell, for the setting that turns the filter off. */
    public static BoltState legacy(RuneSpec spec) { return inject(spec, Charge.LEGACY); }
}
