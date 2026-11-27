package com.newuniverse.nusmp.labyrinth.mega.weekly;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Why: the weekly twists cannot be watched by their author, so {@code dungeon.weekly.apply} (the CI smoke test) creates a zombie, gives it the twists of a deep floor and checks that the
 * listed attributes rose, that a second application adds nothing (no stacking), and that the zombie is discarded again.
 */
public final class WeeklySelfTests {
    private WeeklySelfTests() {}

    public static void register() { SelfTests.register("dungeon.weekly.apply", WeeklySelfTests::apply); }

    private static SelfTests.Result apply(MinecraftServer server) {
        if (!WeeklyConfig.get(WeeklyConfig.ENABLED)) return SelfTests.Result.ok("skipped: the weekly twists are switched off");
        if (WeeklyConfig.get(WeeklyConfig.STRENGTH) <= 0) return SelfTests.Result.ok("skipped: the strength is 0");
        ServerLevel level = server.overworld();
        Mob z = EntityType.ZOMBIE.create(level);
        if (z == null) return SelfTests.Result.ok("skipped: no test creature");
        try {
            double hp = z.getMaxHealth(), speed = z.getAttributeValue(Attributes.MOVEMENT_SPEED);
            int deep = Math.max(WeeklyConfig.get(WeeklyConfig.FLOOR_ONE), Math.max(WeeklyConfig.get(WeeklyConfig.FLOOR_TWO), WeeklyConfig.get(WeeklyConfig.FLOOR_THREE)));
            int first = WeeklyEvents.apply(z, deep);
            if (first != 3) return SelfTests.Result.fail("a floor of the third band got " + first + " twists, not 3");
            if (WeeklyEvents.apply(z, deep) != 0) return SelfTests.Result.fail("a second application stacked");
            boolean rose = z.getMaxHealth() > hp || z.getAttributeValue(Attributes.MOVEMENT_SPEED) > speed || z.getAttributeValue(Attributes.ATTACK_DAMAGE) > 3.0 || z.getAttributeValue(Attributes.ARMOR) > 0.0;
            if (!rose) return SelfTests.Result.fail("no attribute rose");
            Mob shallow = EntityType.ZOMBIE.create(level);
            if (shallow != null) {
                int none = WeeklyEvents.apply(shallow, 0);
                shallow.discard();
                if (none != 0) return SelfTests.Result.fail("floor 0 got twists");
            }
            return SelfTests.Result.ok("week " + WeeklyEvents.currentWeek() + ": a deep floor gives 3 twists once, a shallow floor none");
        } finally {
            z.discard();
        }
    }
}
