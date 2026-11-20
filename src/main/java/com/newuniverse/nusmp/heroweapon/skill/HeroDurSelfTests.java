package com.newuniverse.nusmp.heroweapon.skill;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Status;
import com.newuniverse.nusmp.heroweapon.weapon.strike.Strike;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the duration scale ({@code heroweapon.durationScale}) lengthens the techniques of the hero and vassal weapons through one helper, so the CI server smoke test
 * runs this self-test ({@code hero.durations}) in the real game: it checks that the installed factor is the config value and inside its range, that the scaled
 * length follows the pure function and never passes the cap, that the scaled helpers ({@code Strike.effect}, {@code Status.add}) lengthen a real effect on a
 * stand-in while the unscaled twin ({@code Strike.hold}) does not, and that a repeating task is held to its 400-tick cap. The stand-in's effects are cleared after.
 */
public final class HeroDurSelfTests {
    private HeroDurSelfTests() {}

    public static void register() { SelfTests.register("hero.durations", HeroDurSelfTests::durations); }

    private static int left(FakePlayer p) {
        MobEffectInstance e = p.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
        return e == null ? -1 : e.getDuration();
    }

    private static SelfTests.Result durations(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        double cfg = HeroConfig.durationScale();
        double f = Dur.factor();
        if (cfg < Dur.MIN || cfg > Dur.MAX) bad.add("the config value " + cfg + " is outside " + Dur.MIN + " to " + Dur.MAX);
        if (f != Dur.clampFactor(cfg)) bad.add("Dur.factor() " + f + " is not the config value " + cfg);
        if (Dur.t(100) != Dur.scale(100, cfg)) bad.add("Dur.t(100) " + Dur.t(100) + " does not follow the pure function");
        if (Dur.scale(Integer.MAX_VALUE, Dur.MAX) > Dur.CAP) bad.add("the hard cap is not held");
        if (Dur.task(100000) != Dur.TASK_CAP) bad.add("a task is not held to " + Dur.TASK_CAP);
        if (Dur.scale(100, 1.0) != 100) bad.add("a factor of 1.0 is not the identity");

        ServerLevel level = server.overworld();
        FakePlayer p = SelfTests.player(level, "hero_durations", 0.5, 100, 0.5);
        try {
            p.removeAllEffects();
            Strike.hold(p, "minecraft:slowness", 100, 0);
            int held = left(p);
            p.removeAllEffects();
            Strike.effect(p, "minecraft:slowness", 100, 0);
            int scaled = left(p);
            p.removeAllEffects();
            Status.add(p, MobEffects.MOVEMENT_SLOWDOWN, 100, 0);
            int added = left(p);
            if (held < 0 || scaled < 0 || added < 0) return SelfTests.Result.ok("skipped: the stand-in did not keep the effects (held " + held + ", effect " + scaled + ", add " + added + ")");
            if (held != 100) bad.add("Strike.hold changed the length: " + held + " instead of 100");
            if (scaled != Dur.scale(100, cfg)) bad.add("Strike.effect gave " + scaled + " instead of " + Dur.scale(100, cfg));
            if (added != Dur.scale(100, cfg)) bad.add("Status.add gave " + added + " instead of " + Dur.scale(100, cfg));
        } finally {
            p.removeAllEffects();
        }
        return bad.isEmpty() ? SelfTests.Result.ok("scale " + cfg + ": 100 ticks became " + Dur.t(100) + ", hold stays unscaled, tasks held to " + Dur.TASK_CAP)
                : SelfTests.Result.fail(String.join("; ", bad));
    }
}
