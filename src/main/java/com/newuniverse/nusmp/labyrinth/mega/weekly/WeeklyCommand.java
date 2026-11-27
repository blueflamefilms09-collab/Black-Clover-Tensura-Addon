package com.newuniverse.nusmp.labyrinth.mega.weekly;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Why: {@code /multiverse mega weekly} tells any player this week's twists per depth band and the floors where each band starts (read-only, no cost). */
public final class WeeklyCommand {
    private WeeklyCommand() {}

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("weekly").executes(c -> {
            CommandSourceStack s = c.getSource();
            if (!WeeklyConfig.get(WeeklyConfig.ENABLED)) {
                s.sendSuccess(() -> Component.literal("Weekly dungeon twists are switched off."), false);
                return 0;
            }
            int[] floors = {WeeklyConfig.get(WeeklyConfig.FLOOR_ONE), WeeklyConfig.get(WeeklyConfig.FLOOR_TWO), WeeklyConfig.get(WeeklyConfig.FLOOR_THREE)};
            java.util.Arrays.sort(floors);
            long week = WeeklyEvents.currentWeek();
            s.sendSuccess(() -> Component.literal("Dungeon twists of week " + week + " (the same for everyone, changes every Thursday 00:00 UTC):"), false);
            for (int i = 0; i < 3; i++) {
                List<WeeklyRules.Mod> mods = WeeklyRules.pick(week, i + 1);
                StringBuilder sb = new StringBuilder("From floor ").append(floors[i]).append(": ");
                for (int j = 0; j < mods.size(); j++) sb.append(j == 0 ? "" : ", ").append(mods.get(j).id().replace('_', ' ')).append(" +").append(WeeklyRules.scaledPercent(mods.get(j), WeeklyConfig.get(WeeklyConfig.STRENGTH))).append('%');
                String line = sb.toString();
                s.sendSuccess(() -> Component.literal(line), false);
            }
            return 1;
        });
    }
}
