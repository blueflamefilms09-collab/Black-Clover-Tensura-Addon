package com.newuniverse.nusmp.labyrinth.props;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import com.newuniverse.nusmp.labyrinth.mega.RegionContext;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Why: the dungeon's templates (about 245 structure files) carry armour stands as entities, and an armour stand is a lag source; the owner's spec bans them as scenery. Re-drawing 245 templates is
 * not needed and would lose the old look behind the flag, so the conversion is done where the stands are born: when the sector generator places a piece, the template's entities are spawned
 * through the world's normal entity join, and this handler (high priority, game bus) meets every armour stand of the mod's own building, builds a {@link WeaponRackBlock} where it stood, hangs
 * its weapons and armour on it (see {@link RackPlacement#pack}) and cancels the stand: it never exists. What counts as the mod's own building is the sector generator's {@link RegionContext} (set
 * while a piece is placed) or an {@code internal} span in a Labyrinth world; and the stand must carry the template's signature (invulnerable, no gravity), so a stand a player puts down, a stand
 * loaded from disk (an old world keeps the stands it already has) and a stand made by another system are left alone. Where the rack cannot go (the block at the stand's feet is solid and the one
 * above it too) the stand stays: nothing is lost. A share of the racks (config {@code props.rackLootChance}) are the loot variant: themed gear from {@code nusmp:racks/<family>}, takeable; a stand
 * with nothing on it gets the themed gear for show. With the flag off none of this runs and the templates give their armour stands as before.
 */
public final class RackConverter {
    private RackConverter() {}

    private static final Direction[] BY_INDEX = {Direction.SOUTH, Direction.WEST, Direction.NORTH, Direction.EAST};
    private static final EquipmentSlot[] ORDER = {EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final AtomicInteger CONVERTED = new AtomicInteger(), KEPT = new AtomicInteger();

    /** How many stands became racks and how many were kept (no room) since the game started (for the self-test and the log). */
    public static int converted() { return CONVERTED.get(); }

    public static int kept() { return KEPT.get(); }

    public static void onJoin(EntityJoinLevelEvent e) {
        if (!(e.getEntity() instanceof ArmorStand stand) || !(e.getLevel() instanceof ServerLevel level) || e.loadedFromDisk()) return;
        if (stand.getTags().contains(ArmourStandGate.RARE_TAG)) return;                        // the rare stand of the dungeon is a stand, never a rack
        RegionContext.Active ctx = RegionContext.current();
        boolean dungeon = Labyrinth.in(level);
        boolean own = ctx != null || (dungeon && DungeonGuard.inInternal());
        if (!RackPlacement.shouldConvert(PropsConfig.legacyRackConversion(), dungeon, own, false)) return;       // the generator places no rack unless the legacy path is switched on (default: off)
        if (!stand.isNoGravity() || !stand.isInvulnerable() || stand.isInvisible()) return;        // the template's signature (the generator writes both; an invisible stand is a marker, not furniture)
        String theme = ctx == null || ctx.sector() == null || ctx.sector().theme == null ? "" : ctx.sector().theme.id();
        long salt = ctx == null || ctx.sector() == null ? 0L : ctx.sector().seed;
        if (convert(level, stand, theme, salt)) e.setCanceled(true);
    }

    /**
     * Builds the rack for this stand; true when the stand has been replaced (the caller then drops the stand). Does not touch the stand itself.
     * {@code themeId} may be empty (then the plain table is used); {@code salt} varies the loot rolls between sectors.
     */
    public static boolean convert(ServerLevel level, ArmorStand stand, String themeId, long salt) {
        BlockPos feet = BlockPos.containing(stand.position());
        BlockState at = level.getBlockState(feet);
        RackPlacement.Feet kind = at.isAir() ? RackPlacement.Feet.AIR
                : at.getBlock() instanceof SlabBlock && at.hasProperty(SlabBlock.TYPE) && at.getValue(SlabBlock.TYPE) == SlabType.BOTTOM && stand.getY() - feet.getY() >= 0.4 ? RackPlacement.Feet.BOTTOM_SLAB : RackPlacement.Feet.OTHER;
        RackPlacement.Spot spot = RackPlacement.target(kind, level.getBlockState(feet.above()).isAir());
        BlockPos target;
        switch (spot) {
            case AT:
            case REPLACE_SLAB: target = feet; break;
            case ABOVE: target = feet.above(); break;
            default:
                KEPT.incrementAndGet();
                return false;
        }
        Direction facing = BY_INDEX[RackPlacement.facing(stand.getYRot())];
        BlockState rack = PropsRegistry.RACK.get().defaultBlockState().setValue(WeaponRackBlock.FACING, facing);
        if (!level.setBlock(target, rack, 3) || !(level.getBlockEntity(target) instanceof WeaponRackBlockEntity be)) {
            KEPT.incrementAndGet();
            return false;
        }
        ItemStack[] eq = new ItemStack[ORDER.length];
        boolean[] present = new boolean[ORDER.length];
        for (int i = 0; i < ORDER.length; i++) {
            eq[i] = stand.getItemBySlot(ORDER[i]);
            present[i] = !eq[i].isEmpty();
        }
        int[] slots = RackPlacement.pack(present);
        boolean loot = RackPlacement.lootVariant(target.asLong(), PropsConfig.rackLootChance());
        boolean bare = true;
        for (boolean p : present) bare &= !p;
        if (loot || bare) {
            fillFromTable(level, be, RackFamilies.family(themeId), target, salt);
        } else {
            for (int i = 0; i < ORDER.length; i++) if (slots[i] >= 0) be.set(slots[i], eq[i]);
        }
        be.setMode(loot ? RackMode.LOOT : RackMode.DECOR);
        CONVERTED.incrementAndGet();
        return true;
    }

    /** Fills a rack from the themed loot table (up to five pieces, in order). Returns how many were hung. */
    public static int fillFromTable(ServerLevel level, WeaponRackBlockEntity be, String family, BlockPos pos, long salt) {
        be.clear();
        ObjectArrayList<ItemStack> rolled = roll(level, family, pos, salt);
        int n = 0;
        for (ItemStack s : rolled) {
            if (n >= RackLayout.SLOTS) break;
            if (s.isEmpty()) continue;
            be.set(n++, s);
        }
        return n;
    }

    /** One roll of the family's table. An absent table (a broken datapack) is an empty roll, never an exception. */
    public static ObjectArrayList<ItemStack> roll(ServerLevel level, String family, BlockPos pos, long salt) {
        ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, RackFamilies.tablePath(family)));
        LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        LootParams params = new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos)).create(LootContextParamSets.CHEST);
        return table.getRandomItems(params, pos.asLong() * 31L + salt);
    }
}
