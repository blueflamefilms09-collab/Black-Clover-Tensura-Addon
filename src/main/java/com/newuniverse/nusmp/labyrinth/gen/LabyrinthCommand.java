package com.newuniverse.nusmp.labyrinth.gen;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.gate.LabyrinthTravel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Why: {@code /multiverse labyrinth ...}, the admin's way to test the Labyrinth without walking: {@code locate} (nearest Spire pad from
 * inside, nearest Gate from the Overworld), {@code enter} / {@code leave} (C's LabyrinthTravel, the same calls the Gate block makes),
 * {@code here} (which structure start the player stands in and how many Jigsaw pieces it has: the quickest check that the pool chain
 * built the whole spire), and {@code palette} (the biome the mirror resolved for this spot and the data-map entry it got: the way to
 * check a datapack's palette without regenerating a world). Permission 2 on the whole tree, messages as literals like DungeonCommand,
 * attached by the lead under {@code /multiverse} with {@code root.then(LabyrinthCommand.build())}.
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
            return LabyrinthTravel.enter(p, p.blockPosition()) ? ok(ctx.getSource(), "You step through into the Labyrinth.") : fail(ctx.getSource(), "The Labyrinth did not open (no Spire found, or you are already there).");
        }));
        root.then(Commands.literal("leave").executes(ctx -> {
            ServerPlayer p = ctx.getSource().getPlayerOrException();
            if (!Labyrinth.in(p)) return fail(ctx.getSource(), "You are not in the Labyrinth.");
            LabyrinthTravel.leave(p);
            return ok(ctx.getSource(), "You leave the Labyrinth.");
        }));
        root.then(Commands.literal("here").executes(ctx -> here(ctx.getSource())));
        root.then(Commands.literal("palette").executes(ctx -> palette(ctx.getSource())));
        return root;
    }

    /** Inside the Labyrinth: the nearest Spire's arrival pad. In the Overworld: the nearest Gate. Elsewhere: nothing to find. */
    private static int locate(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos here = p.blockPosition();
        if (Labyrinth.in(level)) {
            StructureStart start = LabyrinthLocate.nearestStart(level, Labyrinth.SPIRE_TAG, here);
            BlockPos pad = LabyrinthLocate.arrivalPadOf(start);
            if (pad == null) return fail(src, "No Labyrinth Spire within " + LabyrinthLocate.LOCATE_RADIUS_CHUNKS + " chunks.");
            BoundingBox box = start.getBoundingBox();
            return ok(src, "Nearest Spire: arrival pad at " + xyz(pad) + " (" + Math.round(Math.sqrt(pad.distSqr(here))) + " blocks away), "
                    + start.getPieces().size() + " pieces, y " + box.minY() + ".." + box.maxY() + ".");
        }
        if (level.dimension() == Level.OVERWORLD) {
            BlockPos gate = LabyrinthLocate.nearestGate(level, here);
            if (gate == null) return fail(src, "No Dungeon Gate within " + LabyrinthLocate.LOCATE_RADIUS_CHUNKS + " chunks.");
            return ok(src, "Nearest Dungeon Gate at " + xyz(gate) + " (" + Math.round(Math.sqrt(gate.distSqr(here))) + " blocks away).");
        }
        return fail(src, "Gates stand in the Overworld and Spires in the Labyrinth; there is nothing to locate here.");
    }

    /** The Labyrinth structure start the player stands in (Spire or Gate), its piece count and bounds. */
    private static int here(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos pos = p.blockPosition();
        StructureStart start = level.structureManager().getStructureWithPieceAt(pos, Labyrinth.SPIRE_TAG);
        if (start == null || !start.isValid()) start = level.structureManager().getStructureWithPieceAt(pos, Labyrinth.GATE_TAG);
        if (start == null || !start.isValid()) return fail(src, "No Labyrinth structure piece here.");
        ResourceLocation id = level.registryAccess().registryOrThrow(Registries.STRUCTURE).getKey(start.getStructure());
        BoundingBox box = start.getBoundingBox();
        BlockPos pad = LabyrinthLocate.arrivalPadOf(start);
        return ok(src, (id == null ? "?" : id.toString()) + ": " + start.getPieces().size() + " pieces, start chunk " + start.getChunkPos().x + " " + start.getChunkPos().z
                + ", bounds " + box.minX() + " " + box.minY() + " " + box.minZ() + " .. " + box.maxX() + " " + box.maxY() + " " + box.maxZ()
                + (pad == null ? "" : ", start-piece pad " + xyz(pad)) + ".");
    }

    /** The biome the palette mirror resolves for the player's spot and the palette entry it yields. */
    private static int palette(CommandSourceStack src) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        ServerLevel level = p.serverLevel();
        BlockPos pos = p.blockPosition();
        Holder<Biome> biome = BiomeMirror.biomeAt(level, pos);
        if (biome == null) return fail(src, "No biome could be resolved here.");
        LabyrinthPalette palette = BiomeMirror.palette(biome);
        String where = Labyrinth.in(level) ? "Overworld surface biome mirrored at this x/z: " : "Biome here: ";
        return ok(src, where + BiomeMirror.name(biome) + (palette == null ? " (no labyrinth_palette entry: default look)." : " -> " + palette.describe() + "."));
    }
}
