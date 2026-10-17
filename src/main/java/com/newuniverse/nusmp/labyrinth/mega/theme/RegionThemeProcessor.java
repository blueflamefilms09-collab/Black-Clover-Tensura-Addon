package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.mojang.serialization.MapCodec;
import com.newuniverse.nusmp.labyrinth.mega.RegionContext;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

/**
 * Why: the owner's brief for the Vanilla Palette Theme Processor: a custom Jigsaw {@code StructureProcessor} that reads the Region's assigned Relic
 * Theme and alters that sector's palette by swapping between EXISTING VANILLA BLOCK FAMILIES (deepslate to nether brick or blackstone) instead of
 * generating custom blocks. All templates are drawn once, in the deepslate palette, so ONE set of files yields nine different-looking sectors, and a
 * template builder never has to know which theme a room will meet. A processor sees one block at a time and knows nothing about the sector it
 * builds, so the sector generator publishes "I am building THIS sector" in {@link RegionContext} (a ThreadLocal) for the span of each piece, and
 * this class reads it: no context (a datapack Jigsaw, {@code /place}, the Overworld's world generation on its own threads) or the baseline theme
 * GRAVITY means "return the block untouched", which is exactly what keeps it harmless everywhere else. The swap itself is a lookup in
 * {@link ThemeTables}. The new state carries the SAME properties ({@code Block.withPropertiesOf}: facing, half, shape, axis, waterlogged, the
 * connection sides of walls and fences) so a rotated stair or an upside-down slab keeps its orientation in the new material, and the block entity
 * data of the template block travels unchanged. It never throws: one exception here would abort the whole piece.
 * <p>
 * Registered as {@code nusmp:region_theme} by {@link ThemeRegistry}; the processor list {@code nusmp:mega_interior} runs it BEFORE
 * {@code nusmp:mod_compat} (the theme changes the wood, the compat swap then replaces furniture on top of the new wood). It has no parameters.
 */
public final class RegionThemeProcessor extends StructureProcessor {
    /** The one instance: a processor without parameters is a unit codec, like vanilla's {@code NopProcessor}. */
    public static final RegionThemeProcessor INSTANCE = new RegionThemeProcessor();
    public static final MapCodec<RegionThemeProcessor> CODEC = MapCodec.unit(() -> INSTANCE);

    private RegionThemeProcessor() {}

    @Override
    protected StructureProcessorType<?> getType() { return ThemeRegistry.REGION_THEME.get(); }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos offset, BlockPos pos, StructureTemplate.StructureBlockInfo original,
                                                             StructureTemplate.StructureBlockInfo current, StructurePlaceSettings settings) {
        try {
            if (current == null) return current;
            RegionContext.Active ctx = RegionContext.current();
            if (ctx == null) return current;                                             // not the Mega-Dungeon generator: leave every block alone
            Sector sector = ctx.sector();
            if (sector == null || sector.theme == null || sector.theme == RegionTheme.GRAVITY) return current;
            BlockState state = current.state();
            if (state == null) return current;
            Block target = ThemeTables.targetOf(sector.theme, state.getBlock());
            if (target == null || target == state.getBlock()) return current;           // not a swapped block (most blocks: glass, lanterns, markers ...)
            return new StructureTemplate.StructureBlockInfo(current.pos(), target.withPropertiesOf(state), current.nbt());
        } catch (RuntimeException e) {
            return current;
        }
    }
}
