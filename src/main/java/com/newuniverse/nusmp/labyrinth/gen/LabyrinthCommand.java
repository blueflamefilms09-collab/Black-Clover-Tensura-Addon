package com.newuniverse.nusmp.labyrinth.gen;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Why: {@code /multiverse labyrinth ...}, the admin's way to test the way into the dungeon without walking: {@code locate} (the nearest Gateway
 * from the Overworld), {@code enter} / {@code leave} (LabyrinthTravel, the same calls the Gate block makes) and {@code here} (which structure
 * start the player stands in and how many Jigsaw pieces it has: the quickest check that the pool chain built the whole Gateway). Permission 2 on
 * the whole tree, messages as literals, attached by the lead under {@code /multiverse} with {@code root.then(LabyrinthCommand.build())}. Until
 * 1.58.0 it also located the Spire and showed the biome palette of its mirror ({@code palette}); the Spire was removed.
 */
public final class LabyrinthCommand {
    private LabyrinthCommand() {}

    private static int ok(CommandSourceStack s, String msg) { s.sendSuccess(() -> Component.literal(msg), false); return 1; }

    private static int fail(CommandSourceStack s, String msg) { s.sendFailure(Component.literal(msg)); return 0; }

    private static String xyz(BlockPos p) { return p.getX() + " " + p.getY() + " " + p.getZ(); }

    public static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("labyrinth").requires(s -> s.hasPermission(2));
        root.then(Commands.literal("locate").executes(ctx -> locate(ctx.getSource())));
        root.then(Commands.literal("enter").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            return LabyrinthTravel.enter(p, p.blockPosition()) ? ok(ctx.getSource(), "You step through into the Labyrinth.") : fail(ctx.getSource(), "The Labyrinth did not open (see the message above, or you are already there).");
        }));
        root.then(Commands.literal("leave").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (!Labyrinth.in(p)) return fail(ctx.getSource(), "You are not in the Labyrinth.");
            LabyrinthTravel.leave(p);
            return ok(ctx.getSource(), "You leave the Labyrinth.");
        }));
        root.then(Commands.literal("here").executes(ctx -> here(ctx.getSource())));
        return root;
    }

    /** In the Overworld: the nearest Gateway. Elsewhere: nothing to find. */
    private static int locate(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos here = p.blockPosition();
        if (level.dimension() == Level.OVERWORLD) {
            BlockPos gate = LabyrinthLocate.nearestGate(level, here);
            if (gate == null) return fail(src, "No Gateway within " + LabyrinthLocate.LOCATE_RADIUS_CHUNKS + " chunks.");
            return ok(src, "Nearest Gateway at " + xyz(gate) + " (" + Math.round(Math.sqrt(gate.distSqr(here))) + " blocks away).");
        }
        return fail(src, "The Gateway stands in the Overworld; there is nothing to locate here.");
    }

    /** The Gateway structure start the player stands in, its piece count and bounds. */
    private static int here(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos pos = p.blockPosition();
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, Labyrinth.GATE_TAG);
        if (start == null || !start.isValid()) return fail(src, "No Labyrinth structure piece here.");
        ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
        BoundingBox box = start.getBoundingBox();
        return ok(src, (id == null ? "?" : id.toString()) + ": " + start.getPieces().size() + " pieces, start chunk " + start.getChunkPos().x + " " + start.getChunkPos().z
                + ", bounds " + box.minX() + " " + box.minY() + " " + box.minZ() + " .. " + box.maxX() + " " + box.maxY() + " " + box.maxZ() + ".");
    }
}
