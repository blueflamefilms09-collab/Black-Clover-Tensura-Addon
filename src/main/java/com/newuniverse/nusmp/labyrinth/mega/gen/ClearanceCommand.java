package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Why: whether the 7 x 7 clearance really took in a world is something the owner can only see by walking, and a server owner wants a one-line answer first. {@code /multiverse
 * mega clearance} prints the switch and what the placer has cut since the server started; {@code /multiverse mega clearance <sector>} also plans that sector again from its
 * saved data and reads the world with the rule {@code clearance7} ({@link GeometryCheck#clearance7}), for up to 40 pieces spread over the sector. Read-only. Text goes through
 * translatable keys with an English fallback (fragment {@code tools/lang/clearance7.json}).
 */
final class ClearanceCommand {
    private ClearanceCommand() {}

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("clearance").executes(ctx -> status(ctx.getSource()))
                .then(Commands.argument("id", IntegerArgumentType.integer(0)).executes(ctx -> sector(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "id"))));
    }

    private static int say(CommandSourceStack s, Component c) {
        s.sendSuccess(() -> c, false);
        return 1;
    }

    private static int status(CommandSourceStack s) {
        return say(s, Component.translatableWithFallback("nusmp.mega.clearance7.status",
                "7x7 clearance: switch gen.clearance7 is %s (invisible ceiling lights %s). Since the server started the placer cut %s pieces: %s blocks removed, %s left standing (chests and mechanisms), %s roof blocks, %s light blocks, %s Way Onward seals opened wide.",
                ClearanceSettings.enabled() ? "on" : "off", ClearanceSettings.lights() ? "on" : "off", ClearanceCarver.PIECES.get(), ClearanceCarver.CLEARED.get(), ClearanceCarver.KEPT.get(),
                ClearanceCarver.CAPPED.get(), ClearanceCarver.LIT.get(), ClearanceCarver.SEALS.get()));
    }

    private static int sector(CommandSourceStack s, int id) {
        MegaData data = MegaData.get(s.getServer());
        Sector sec = data.sector(id);
        ServerLevel level = MegaDungeon.level(s.getServer());
        if (sec == null || level == null) return say(s, Component.translatableWithFallback("nusmp.mega.clearance7.none", "No such sector, or the Mega-Dungeon is not loaded."));
        if (!SectorGenerator.playable(sec)) return say(s, Component.translatableWithFallback("nusmp.mega.clearance7.unfinished", "Sector %s is not finished yet.", id));
        if (!ClearanceCarver.wasCut(s.getServer(), id)) {
            return say(s, Component.translatableWithFallback("nusmp.mega.clearance7.old", "Sector %s was finished before the 7x7 clearance, or with the switch off: regenerate it to cut it.", id));
        }
        SectorGenerator.Planned planned = SectorGenerator.plan(level, data, sec);
        if (!planned.ok()) return say(s, Component.translatableWithFallback("nusmp.mega.clearance7.noplan", "Sector %s cannot be planned again: %s", id, planned.failure()));
        List<Integer> pick = GeometryCheck.pick(planned.result(), 40);
        List<String> bad = GeometryCheck.clearance7(level, planned.result(), pick::contains);
        if (bad.isEmpty()) {
            return say(s, Component.translatableWithFallback("nusmp.mega.clearance7.good", "Sector %s: the %s pieces read in the world are all at least 7x7.", id, pick.size()));
        }
        return say(s, Component.translatableWithFallback("nusmp.mega.clearance7.bad", "Sector %s: below 7x7: %s", id, String.join("; ", bad.subList(0, Math.min(3, bad.size())))));
    }
}
