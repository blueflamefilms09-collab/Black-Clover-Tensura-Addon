package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Why: the admin's handle on the guard systems without a fight: {@code give <player> [sector]} hands out a Mana Core (the way to test a
 * sector's door, or to repair a world where a core was lost with no altar nearby), {@code forge} makes the nearest loaded Guardian Altar
 * forge a core at once (no waiting, no rules: the admin's override of the re-forge), and {@code guardian} spawns a test Guardian at your
 * feet (no altar, so no leash, no core, and nothing it does can unseal a sector). The lead attaches {@link #command()} under
 * {@code /multiverse mega}, so the whole tree is permission 2 already; each literal asks again, so the node is safe on its own. Messages
 * are literals, like the other admin commands of the Labyrinth.
 */
public final class GuardCommand {
    private GuardCommand() {}

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), true); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    /** The {@code core} node: {@code give <player> [sector]}, {@code forge}, {@code guardian}. */
    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("core").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("give").requires(s -> s.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player()).executes(ctx -> give(ctx, -1))
                        .then(Commands.argument("sector", IntegerArgumentType.integer(0)).executes(ctx -> give(ctx, IntegerArgumentType.getInteger(ctx, "sector"))))));
        root.then(Commands.literal("forge").requires(s -> s.hasPermission(2)).executes(ctx -> forge(ctx.getSource())));
        root.then(Commands.literal("guardian").requires(s -> s.hasPermission(2)).executes(ctx -> guardian(ctx.getSource())));
        return root;
    }

    /** {@code give}: a core of the named sector, else of the sector the player stands in, else of the newest sector. */
    private static int give(CommandContext<CommandSourceStack> ctx, int sectorId) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
        MegaData data = MegaData.get(src.getServer());
        Sector sector = sectorId >= 0 ? data.sector(sectorId) : MegaDungeon.sectorAt(target.level(), target.blockPosition());
        if (sector == null && sectorId < 0) sector = data.latest();
        if (sector == null) return fail(src, sectorId >= 0 ? "There is no sector " + sectorId + "." : "The Mega-Dungeon has no sector yet.");
        ItemStack core = ManaCoreItem.forSector(sector.id, sector.theme);
        if (!target.getInventory().add(core)) target.drop(core, false);
        return ok(src, "Gave " + target.getGameProfile().getName() + " the Mana Core of sector " + sector.id + " (" + sector.theme.id() + ").");
    }

    /** {@code forge}: the nearest LOADED Guardian Altar of any sector forges a core now. */
    private static int forge(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        if (!MegaDungeon.in(level)) return fail(src, "Stand in the Mega-Dungeon, near the Guardian's hall.");
        BlockPos here = BlockPos.containing(src.getPosition());
        GuardianAltarBlockEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (Sector s : MegaData.get(src.getServer()).sectors()) {
            if (s.guardianSpawn == null) continue;
            BlockPos altar = s.guardianSpawn.below();                                      // the marker stands in the air cell above the altar
            if (!level.isLoaded(altar) || !(level.getBlockEntity(altar) instanceof GuardianAltarBlockEntity be)) continue;
            double d = altar.distSqr(here);
            if (d < bestD) { best = be; bestD = d; }
        }
        if (best == null) return fail(src, "No loaded Guardian Altar: walk into the Guardian's hall first.");
        return best.forgeNow(level) ? ok(src, "The altar forges a Mana Core above itself.") : fail(src, "That altar belongs to no sector.");
    }

    /** {@code guardian}: a test Guardian for the sector under the player (threat 1, gravity when there is none), free of any altar. */
    private static int guardian(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos at = p.blockPosition();
        Sector sector = MegaDungeon.sectorAt(level, at);
        LabyrinthGuardianEntity g = LabyrinthGuardianEntity.create(level, at, sector, null);
        if (g == null) return fail(src, "The Guardian could not be made.");
        level.addFreshEntity(g);
        return ok(src, "A Labyrinth Guardian answers: threat " + g.threat() + ", " + g.theme().id() + ", trait " + g.trait().name().toLowerCase(java.util.Locale.ROOT)
                + (sector == null ? " (no sector here)." : " (sector " + sector.id + "; it has no altar, so it drops nothing and unseals nothing)."));
    }
}
