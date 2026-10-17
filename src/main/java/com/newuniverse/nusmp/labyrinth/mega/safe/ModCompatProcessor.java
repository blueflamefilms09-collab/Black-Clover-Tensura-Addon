package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.mojang.serialization.MapCodec;
import com.newuniverse.nusmp.labyrinth.gen.BiomePaletteProcessor;
import com.newuniverse.nusmp.labyrinth.mega.Marker;
import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.RegionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Why: the brief wants Farmer's Delight stoves, skillets and cutting boards in place of generic campfires, and MrCrayfish's Furniture Mod:
 * Refurbished sofas, tables and storage in place of stair-chairs and wool beds, when those mods are installed, and plain vanilla otherwise, from
 * ONE set of templates. A {@code StructureProcessor} sees every block at placement time, so the swap costs nothing at runtime and leaves nothing
 * in the world file but the final block. The templates mark what a block is with a data marker in the cell above it
 * ({@code nusmp:seat;as=chair}, {@code nusmp:hearth;as=stove}, {@code nusmp:bed;as=bed}, {@code nusmp:furniture;as=table}); the generator hands
 * the markers of the sector it is building to {@link RegionContext}, and this processor looks for one above each block it is given. If the
 * {@link CompatTable} has a candidate for that hint whose mod is loaded and whose block exists, the block is replaced and keeps its
 * {@code facing}, {@code half}, {@code part}, {@code lit} ... by name ({@link BiomePaletteProcessor#carryProperties}, which never throws on a
 * mismatch); a seat flips its facing, because a stair "faces" up its slope, behind the sitter, while a chair faces the way the sitter looks.
 * A bed is two blocks and the marker is above the FOOT, so the head looks for it across the bed (turned the way the piece will be turned), and
 * both halves swap together or neither does. Outside the generator the context is null and the block is returned untouched, so a datapack Jigsaw
 * or a command that uses the processor list is unharmed; with no compat mod loaded the table is empty and the first two checks end the work, so
 * the cost for the great majority of blocks is one thread-local read. The block entity data of a swapped block is dropped (the replacement is a
 * different block); the templates only mark decorative blocks.
 */
public final class ModCompatProcessor extends StructureProcessor {
    /** The processor has no fields: what to swap is data ({@code data/nusmp/mega/compat}) and markers. */
    public static final MapCodec<ModCompatProcessor> CODEC = MapCodec.unit(ModCompatProcessor::new);

    public ModCompatProcessor() {}

    @Override
    protected StructureProcessorType<?> getType() { return SafeRegistry.MOD_COMPAT.get(); }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos piecePos, BlockPos structureOrigin,
                                                             StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo placed,
                                                             StructurePlaceSettings settings) {
        try {
            RegionContext.Active ctx = RegionContext.current();
            if (ctx == null || placed == null) return placed;                                                 // not the generator: leave every block alone
            CompatTable table = CompatLoader.table();
            if (table.isEmpty()) return placed;                                                               // no compat mod loaded: nothing to swap
            BlockState state = placed.state();
            if (state == null || state.isAir() || !SafeConfig.get(SafeConfig.COMPAT_ENABLED)) return placed;
            BlockPos at = placed.pos();
            BlockPos markerCell = markerCell(state, at, settings);
            Marker marker = ctx.markers().get(markerCell);
            if (marker == null || !hintable(marker.kind())) return placed;
            String hint = marker.arg("as", null);
            if (hint == null || !table.hasHint(hint)) return placed;
            ResourceLocation here = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            CompatTable.Pick pick = table.pick(hint, CompatLoader::modLoaded, ModCompatProcessor::blockExists, CompatTable.tokensFor(here.getPath()));
            if (pick == null) return placed;
            ResourceLocation target = ResourceLocation.tryParse(pick.id());
            if (target == null || !BuiltInRegistries.BLOCK.containsKey(target)) return placed;
            Block block = BuiltInRegistries.BLOCK.get(target);
            if (block == state.getBlock()) return placed;
            BlockState next = BiomePaletteProcessor.carryProperties(state, block.defaultBlockState());
            next = faceLikeThe(state, next, pick);
            return new StructureTemplate.StructureBlockInfo(at, next, null);
        } catch (RuntimeException e) {
            return placed;                                                                                    // never let one odd block abort the piece
        }
    }

    /** The marker kinds whose {@code as} hint asks for a swap (a spawn point or a patrol node has no hint). */
    static boolean hintable(MarkerKind kind) {
        return kind == MarkerKind.SEAT || kind == MarkerKind.HEARTH || kind == MarkerKind.BED || kind == MarkerKind.FURNITURE;
    }

    /** The cell whose marker describes this block: the cell above it, or above the FOOT of a bed when this is the head. */
    private static BlockPos markerCell(BlockState state, BlockPos at, StructurePlaceSettings settings) {
        if (state.getBlock() instanceof BedBlock && state.hasProperty(BedBlock.PART) && state.getValue(BedBlock.PART) == BedPart.HEAD) {
            BlockState turned = state.mirror(settings.getMirror()).rotate(settings.getRotation());           // the state is still in the template's frame: turn it the way placement will
            Direction toHead = turned.hasProperty(BedBlock.FACING) ? turned.getValue(BedBlock.FACING) : Direction.NORTH;
            return at.relative(toHead.getOpposite()).above();
        }
        return at.above();
    }

    private static boolean blockExists(String id) {
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl != null && BuiltInRegistries.BLOCK.containsKey(rl) && BuiltInRegistries.BLOCK.get(rl) != Blocks.AIR;
    }

    /**
     * Sets the replacement's facing property (named in the data, default {@code facing}) from the old block's horizontal {@code facing}, reversed
     * when the data says so. Does nothing when either side lacks the property or the value does not exist on the target.
     */
    private static BlockState faceLikeThe(BlockState old, BlockState next, CompatTable.Pick pick) {
        Property<?> from = old.getBlock().getStateDefinition().getProperty("facing");
        if (from == null || !old.hasProperty(from)) return next;
        Direction d = directionOf(old, from);
        if (d == null) return next;
        if (pick.flipFacing()) d = d.getOpposite();
        Property<?> to = next.getBlock().getStateDefinition().getProperty(pick.facingProperty());
        return to == null ? next : setByName(next, to, d.getName());
    }

    private static <T extends Comparable<T>> Direction directionOf(BlockState state, Property<T> property) {
        Direction d = Direction.byName(property.getName(state.getValue(property)));
        return d != null && d.getAxis().isHorizontal() ? d : null;
    }

    private static <T extends Comparable<T>> BlockState setByName(BlockState state, Property<T> property, String valueName) {
        Optional<T> value = property.getValue(valueName);
        return value.isPresent() ? state.trySetValue(property, value.get()) : state;
    }

    /** For tests and the admin command: the compat hints currently available (empty when no compat mod is loaded). */
    public static Map<String, Integer> availableHints() {
        Map<String, Integer> out = new LinkedHashMap<>();
        CompatTable t = CompatLoader.table();
        for (String h : t.hints()) out.put(h, t.candidateCount(h));
        return out;
    }
}
