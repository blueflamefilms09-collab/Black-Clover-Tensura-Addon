package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules.Phase;
import com.newuniverse.nusmp.labyrinth.mega.theme.RelicThemes;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Why: nothing about the Crucible can be seen without a game, and an owner who wants to test a vault should not have to clear a Guardian and walk
 * a whole sector first. {@code /multiverse mega crucible ...} (permission 2; the lead attaches {@link #command()} under {@code mega}) is the
 * admin's handle on it, with the pattern of the other Labyrinth commands (messages as literals): {@code status [sector]} shows the saved state of a
 * vault (phase, the offered relic ids in slot order, the choice and the chooser, the catastrophe and whether one is running, the grades it offers and the theme the choice leads to);
 * {@code roll [sector]} forces the offer now (or, for a vault that is already ROLLED, re-shows the saved one on its pedestals) without a player in the
 * vault; {@code reset <sector>} puts a vault back to UNOPENED (stopping its catastrophe, emptying its pedestals) and rolls a fresh offer, so the whole
 * choice can be tried again; {@code catastrophe <collapse|flood|spirit>} runs one catastrophe around YOU as a test: it never resolves the Crucible and
 * never touches a vault's saved state. A sector given as a number is that sector; without one it is the sector the command's source stands in.
 */
public final class CrucibleCommand {
    private CrucibleCommand() {}

    private static int ok(CommandSourceStack s, String msg) {
        s.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int fail(CommandSourceStack s, String msg) {
        s.sendFailure(Component.literal(msg));
        return 0;
    }

    private static String xyz(BlockPos p) { return p == null ? "-" : p.getX() + " " + p.getY() + " " + p.getZ(); }

    /** {@code literal("crucible")}: {@code status [sector]}, {@code roll [sector]}, {@code reset <sector>}, {@code catastrophe <kind>}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("crucible").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("status").executes(c -> status(c.getSource(), null))
                .then(Commands.argument("sector", IntegerArgumentType.integer(0)).executes(c -> status(c.getSource(), IntegerArgumentType.getInteger(c, "sector")))));
        root.then(Commands.literal("roll").executes(c -> roll(c.getSource(), null))
                .then(Commands.argument("sector", IntegerArgumentType.integer(0)).executes(c -> roll(c.getSource(), IntegerArgumentType.getInteger(c, "sector")))));
        root.then(Commands.literal("reset")
                .then(Commands.argument("sector", IntegerArgumentType.integer(0)).executes(c -> reset(c.getSource(), IntegerArgumentType.getInteger(c, "sector")))));
        root.then(Commands.literal("catastrophe")
                .then(Commands.argument("kind", StringArgumentType.word()).suggests((ctx, b) -> SharedSuggestionProvider.suggest(CrucibleRules.KINDS, b))
                        .executes(c -> catastrophe(c.getSource(), StringArgumentType.getString(c, "kind")))));
        return root;
    }

    /** The sector with this id, or the one the source stands in. */
    private static Sector sectorOf(CommandSourceStack src, Integer id) {
        if (id != null) return MegaData.get(src.getServer()).sector(id);
        return MegaDungeon.sectorAt(src.getLevel(), BlockPos.containing(src.getPosition()));
    }

    private static int status(CommandSourceStack src, Integer id) {
        Sector s = sectorOf(src, id);
        if (s == null) return fail(src, id == null ? "You are not inside a sector of the Mega-Dungeon: give a sector number." : "There is no sector " + id + ".");
        Sector.Crucible c = s.crucible;
        RelicThemes.Road next = NextTheme.roadAfter(s);
        StringBuilder offered = new StringBuilder();
        for (String o : c.offered) offered.append(offered.length() == 0 ? "" : ", ").append(o == null || o.isEmpty() ? "-" : o);
        String running = CatastropheRunner.active(s.id) ? CatastropheRunner.kindOf(s.id) : "none";
        return ok(src, "Sector " + s.id + " (" + s.theme.id() + ", threat " + s.threat + ", " + s.state + "): crucible " + c.phase
                + "\n  pedestals " + CruciblePedestals.ordered(s).size() + ", relic count " + s.relicCount + ", vault centre " + xyz(CruciblePedestals.center(s))
                + "\n  offered [" + offered + "], chosen " + (c.chosen == null || c.chosen.isEmpty() ? "-" : c.chosen) + ", chooser " + (c.chooser == null ? "-" : c.chooser)
                + ", catastrophe " + (c.catastrophe == null || c.catastrophe.isEmpty() ? "-" : c.catastrophe) + " (running: " + running + ")"
                + "\n  offers up to " + CrucibleRules.offerCount(ThemeConfig.get(ThemeConfig.OFFER_COUNT), CruciblePedestals.ordered(s).size()) + " relics of " + ThemeConfig.minGrade().shortName()
                + " and above; the next region's road: " + (next == null ? "not decided yet" : next.theme().id() + ", ruleset " + next.ruleset() + " (" + NextTheme.describe(next) + ")")
                + "; roster " + CursedSpirits.roster().size() + " spirits");
    }

    private static int roll(CommandSourceStack src, Integer id) {
        Sector s = sectorOf(src, id);
        if (s == null) return fail(src, id == null ? "You are not inside a sector of the Mega-Dungeon: give a sector number." : "There is no sector " + id + ".");
        ServerLevel level = MegaDungeon.level(src.getServer());
        if (level == null) return fail(src, "The server has no Mega-Dungeon dimension.");
        Phase phase = Phase.of(s.crucible.phase.name());
        if (phase == Phase.UNOPENED) {
            if (!CrucibleOffer.roll(level, s)) return fail(src, "The offer could not be rolled (no pedestal marker in sector " + s.id + "?). See the log.");
            return ok(src, "Rolled the Crucible of sector " + s.id + ": " + s.crucible.phase + ", offered " + s.crucible.offered + ".");
        }
        if (phase == Phase.ROLLED) {
            int changed = CrucibleOffer.show(level, s);
            return ok(src, "The Crucible of sector " + s.id + " is already rolled (" + s.crucible.offered + "): " + changed + " pedestal(s) re-filled; pedestals in unloaded chunks are repaired on the first visit.");
        }
        return fail(src, "The Crucible of sector " + s.id + " is " + phase + ": run '/multiverse mega crucible reset " + s.id + "' first.");
    }

    private static int reset(CommandSourceStack src, int id) {
        Sector s = MegaData.get(src.getServer()).sector(id);
        if (s == null) return fail(src, "There is no sector " + id + ".");
        ServerLevel level = MegaDungeon.level(src.getServer());
        if (level == null) return fail(src, "The server has no Mega-Dungeon dimension.");
        CrucibleVault.reset(level, s);
        boolean rolled = CrucibleOffer.roll(level, s);
        return ok(src, "The Crucible of sector " + id + " is reset to UNOPENED" + (rolled ? " and rolled again: " + s.crucible.phase + ", offered " + s.crucible.offered + "." : "; it will roll when somebody enters the vault."));
    }

    private static int catastrophe(CommandSourceStack src, String kind) throws CommandSyntaxException {
        String k = kind == null ? "" : kind.toLowerCase(java.util.Locale.ROOT);
        if (!CrucibleRules.KINDS.contains(k)) return fail(src, "Unknown catastrophe '" + kind + "': " + String.join(", ", CrucibleRules.KINDS) + ".");
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        if (!MegaDungeon.in(level)) return fail(src, "Stand inside the Mega-Dungeon to test a catastrophe.");
        Sector s = MegaDungeon.sectorAt(level, p.blockPosition());
        if (s == null) return fail(src, "You are not inside a sector of the Mega-Dungeon.");
        if (!CatastropheRunner.start(level, s, k, p, p.blockPosition(), false)) return fail(src, "The catastrophe could not start.");
        String actual = CatastropheRunner.kindOf(s.id);
        List<String> note = actual.equals(k) ? List.of() : List.of(" (no Cursed Spirit could be summoned: it became a " + actual + ")");
        return ok(src, "A test " + actual + " runs around you in sector " + s.id + ": it never resolves the Crucible." + String.join("", note));
    }
}
