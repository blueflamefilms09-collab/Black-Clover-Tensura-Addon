package com.newuniverse.nusmp.labyrinth.props;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Why: nothing here can be played by its author, so the CI server smoke test runs these in the real game (prefix {@code dungeon.props}). {@code dungeon.props.rack}: a rack takes five items and
 * refuses the sixth, gives the last back first, keeps its contents through a save and a load, and honours its mode. {@code dungeon.props.convert}: a dressed armour stand becomes a rack that
 * shows the stand's items (or, when the loot variant was drawn, the themed gear), faces the way the stand faced, and a stand with no room is kept. {@code dungeon.props.loot}: every family's
 * table exists and gives something. {@code dungeon.props.flag}: the old rack path defaults OFF (the dungeon places no rack) and the rack block stays registered. {@code dungeon.props.stands}: the rare armour stand is breakable, unlocked, tagged and dressed for its floor. The tests build in the Overworld above the spawn (a throw-away CI
 * world) and clean up after themselves.
 */
public final class PropsSelfTests {
    private PropsSelfTests() {}

    public static void register() {
        SelfTests.register("dungeon.props.rack", PropsSelfTests::rack);
        SelfTests.register("dungeon.props.convert", PropsSelfTests::convert);
        SelfTests.register("dungeon.props.loot", PropsSelfTests::loot);
        SelfTests.register("dungeon.props.flag", PropsSelfTests::flag);
        SelfTests.register("dungeon.props.stands", PropsSelfTests::stands);
    }

    private static BlockPos spot(ServerLevel level) {
        BlockPos spawn = level.getSharedSpawnPos();
        level.getChunk(spawn);
        return level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, spawn).above(4);
    }

    private static SelfTests.Result rack(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos p = spot(level);
        BlockState state = PropsRegistry.RACK.get().defaultBlockState().setValue(WeaponRackBlock.FACING, Direction.WEST);
        try {
            level.setBlock(p, state, 3);
            if (!(level.getBlockEntity(p) instanceof WeaponRackBlockEntity be)) return SelfTests.Result.fail("the rack has no block entity");
            if (be.mode() != RackMode.OPEN) return SelfTests.Result.fail("a new rack is not OPEN: " + be.mode());
            for (int i = 0; i < RackLayout.SLOTS; i++) if (be.put(new ItemStack(Items.IRON_SWORD, 3)) != i) return SelfTests.Result.fail("slot " + i + " was not the next free hook");
            if (be.put(new ItemStack(Items.IRON_AXE)) != -1) return SelfTests.Result.fail("a sixth item was accepted");
            if (be.get(0).getCount() != 1) return SelfTests.Result.fail("a rack must hold one item per hook, has " + be.get(0).getCount());
            CompoundTag saved = be.saveWithoutMetadata(level.registryAccess());
            WeaponRackBlockEntity copy = new WeaponRackBlockEntity(p, state);
            copy.loadWithComponents(saved, level.registryAccess());
            if (copy.count() != RackLayout.SLOTS || !copy.get(4).is(Items.IRON_SWORD) || copy.mode() != RackMode.OPEN) return SelfTests.Result.fail("the contents did not survive a save and a load");
            if (!be.take().is(Items.IRON_SWORD) || be.count() != RackLayout.SLOTS - 1 || be.lastFilled() != RackLayout.SLOTS - 2) return SelfTests.Result.fail("take must give the last item first");
            be.setMode(RackMode.DECOR);
            if (be.mode().canTake() || be.mode().canPut()) return SelfTests.Result.fail("a DECOR rack must not be touched");
            be.setMode(RackMode.LOOT);
            if (!be.mode().canTake() || be.mode().canPut()) return SelfTests.Result.fail("a LOOT rack is take-only");
            return SelfTests.Result.ok("5 hooks, 6th refused, save/load kept, modes ok");
        } finally {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static SelfTests.Result convert(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos p = spot(level);
        BlockPos stone = p.above(8);
        ArmorStand stand = null, bare = null, boxed = null;
        try {
            stand = EntityType.ARMOR_STAND.create(level);
            if (stand == null) return SelfTests.Result.fail("no armour stand could be made");
            stand.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            stand.setYRot(90f);                                                                    // faces west
            stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
            stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.WITHER_SKELETON_SKULL));
            stand.setNoGravity(true);
            stand.setInvulnerable(true);
            if (!RackConverter.convert(level, stand, "necropolis", 7L)) return SelfTests.Result.fail("a dressed stand was not converted");
            BlockState s = level.getBlockState(p);
            if (!s.is(PropsRegistry.RACK.get())) return SelfTests.Result.fail("no rack at the stand's feet: " + s);
            if (s.getValue(WeaponRackBlock.FACING) != Direction.WEST) return SelfTests.Result.fail("the rack faces " + s.getValue(WeaponRackBlock.FACING) + ", the stand faced west");
            WeaponRackBlockEntity be = (WeaponRackBlockEntity) level.getBlockEntity(p);
            if (be == null || be.count() == 0) return SelfTests.Result.fail("the converted rack is empty");
            if (be.mode() == RackMode.DECOR && (!be.get(0).is(Items.IRON_SWORD) || !be.get(1).is(Items.WITHER_SKELETON_SKULL))) return SelfTests.Result.fail("the rack does not show the stand's items");
            if (be.mode() == RackMode.OPEN) return SelfTests.Result.fail("a converted rack must not be OPEN");
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);

            bare = EntityType.ARMOR_STAND.create(level);
            bare.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            if (!RackConverter.convert(level, bare, "holy_church", 8L)) return SelfTests.Result.fail("a bare stand was not converted");
            WeaponRackBlockEntity b2 = (WeaponRackBlockEntity) level.getBlockEntity(p);
            if (b2 == null || b2.count() == 0) return SelfTests.Result.fail("a bare stand's rack got no themed gear");
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);

            level.setBlock(stone, Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(stone.above(), Blocks.STONE.defaultBlockState(), 3);
            boxed = EntityType.ARMOR_STAND.create(level);
            boxed.setPos(stone.getX() + 0.5, stone.getY(), stone.getZ() + 0.5);
            int kept = RackConverter.kept();
            if (RackConverter.convert(level, boxed, "", 9L)) return SelfTests.Result.fail("a stand with no room was converted");
            if (RackConverter.kept() != kept + 1 || !level.getBlockState(stone).is(Blocks.STONE)) return SelfTests.Result.fail("a stand with no room must be kept and the world untouched");
            return SelfTests.Result.ok("dressed stand -> rack (" + be.mode() + "), bare stand -> themed rack, no room -> kept");
        } finally {
            level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(stone, Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(stone.above(), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private static SelfTests.Result loot(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos p = spot(level);
        StringBuilder detail = new StringBuilder();
        for (String family : RackFamilies.ALL) {
            int total = 0;
            for (int i = 0; i < 8; i++) {
                for (ItemStack s : RackConverter.roll(level, family, p.offset(i, 0, 0), 11L * i)) if (!s.isEmpty()) total++;
            }
            if (total == 0) return SelfTests.Result.fail("the table racks/" + family + " gave nothing in 8 rolls");
            detail.append(family).append('=').append(total).append(' ');
        }
        return SelfTests.Result.ok(detail.toString().trim());
    }

    private static SelfTests.Result flag(MinecraftServer server) {
        // rooms rebuilt: the generator places no weapon rack unless the legacy path is switched on (both keys), and the keys default to off
        if (PropsConfig.rackInsteadOfStand()) return SelfTests.Result.fail("props.rackInsteadOfArmourStand is on by default: the dungeon would place racks");
        if (PropsConfig.legacyRackConversion()) return SelfTests.Result.fail("the legacy rack conversion is on by default");
        if (RackPlacement.shouldConvert(PropsConfig.legacyRackConversion(), true, true, false)) return SelfTests.Result.fail("with the defaults a template stand would become a rack");
        if (RackPlacement.shouldConvert(false, true, true, false)) return SelfTests.Result.fail("the flag off must keep the armour stands");
        if (!RackPlacement.shouldConvert(true, true, true, false) || RackPlacement.shouldConvert(true, true, true, true)) return SelfTests.Result.fail("only the mod's own, freshly built stands convert");
        if (PropsRegistry.RACK.get() == null || PropsRegistry.RACK_ITEM.get() == null) return SelfTests.Result.fail("the weapon rack block or item is no longer registered (old worlds need it)");
        return SelfTests.Result.ok("racks never placed by default (legacy path off), rack block and item still registered");
    }

    private static SelfTests.Result stands(MinecraftServer server) {
        ServerLevel level = server.overworld();
        BlockPos p = spot(level);
        double rarity = PropsConfig.armourStandRarity();
        if (rarity < 0.0 || rarity > 100.0 || PropsConfig.armourStandFloorCap() < 0 || PropsConfig.armourStandFloorCap() > ArmourStandRarity.HARD_CAP) return SelfTests.Result.fail("the stand config is out of range: " + rarity + " / " + PropsConfig.armourStandFloorCap());
        ArmorStand stand = null;
        try {
            stand = EntityType.ARMOR_STAND.create(level);
            if (stand == null) return SelfTests.Result.fail("no armour stand could be made");
            stand.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
            stand.setNoGravity(true);
            stand.setInvulnerable(true);
            if (!ArmourStandGate.templateSignature(stand)) return SelfTests.Result.fail("a template stand (invulnerable, no gravity) is not recognised");
            if (ArmourStandGate.current() != null) return SelfTests.Result.fail("a generator scope is open outside the generator");
            ArmourStandGate.makeRare(level, stand, ArmourStandGate.Scope.of(true, 35, 7L));
            if (stand.isInvulnerable()) return SelfTests.Result.fail("the rare stand is still invulnerable (it must be breakable)");
            if (!stand.getTags().contains(ArmourStandGate.RARE_TAG)) return SelfTests.Result.fail("the rare stand is not tagged");
            if (stand.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) return SelfTests.Result.fail("the rare stand has no chest piece");
            int worn = 0;
            for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) if (!stand.getItemBySlot(slot).isEmpty()) worn++;
            if (worn < 2 || worn > 4) return SelfTests.Result.fail("the rare stand wears " + worn + " pieces (two to four)");
            CompoundTag tag = new CompoundTag();
            stand.saveWithoutId(tag);
            if (tag.getInt("DisabledSlots") != 0) return SelfTests.Result.fail("the rare stand's slots are locked (a player must be able to take its armour)");
            ArmourStandGate.run(ArmourStandGate.Scope.of(false, 0, 1L), () -> { });
            if (ArmourStandGate.current() != null) return SelfTests.Result.fail("a generator scope was left open");
            return SelfTests.Result.ok("rare stand: breakable, unlocked, tagged, " + worn + " pieces for floor 35 (" + RareStandSet.setOf(RareStandSet.band(35)) + "); chance " + rarity + "%, floor cap " + PropsConfig.armourStandFloorCap());
        } finally {
            if (stand != null) stand.discard();
        }
    }
}
