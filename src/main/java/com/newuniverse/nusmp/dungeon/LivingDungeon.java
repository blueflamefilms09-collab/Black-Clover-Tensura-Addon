package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Living Dungeon (docs/living_dungeon.md): a closed, self-sustaining ecosystem under the world, reached through Dungeon Maws that tear
 * open in the Overworld where too much magic has been spent. This class holds what everything else shares: the dimension, the per-chunk
 * ecosystem attachment, and the way in and out.
 */
public final class LivingDungeon {
    private LivingDungeon() {}

    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "living_dungeon"));
    private static final String ORIGIN = "nusmp_dungeon_origin", GATE_USED = "nusmp_dungeon_gate";

    // ---------------------------------------------------------------- the per-chunk ecosystem (a data attachment, not a Forge capability)
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<EcosystemData>> ECOSYSTEM = ATTACHMENTS.register("ecosystem",
            () -> AttachmentType.builder(() -> new EcosystemData()).serialize(EcosystemData.CODEC).build());

    public static boolean in(Level level) { return level != null && level.dimension() == DIMENSION; }

    public static boolean in(Entity e) { return in(e.level()); }

    /** The ecosystem of the chunk holding this position, or null if the chunk is not loaded. */
    public static EcosystemData eco(ServerLevel level, BlockPos pos) {
        LevelChunk c = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        return c == null ? null : c.getData(ECOSYSTEM);
    }

    /** Marks the chunk of this position as changed (after the ecosystem was mutated). */
    public static void touch(ServerLevel level, BlockPos pos) {
        LevelChunk c = level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4);
        if (c != null) c.setUnsaved(true);
    }

    // ---------------------------------------------------------------- the way in and out
    /** Carries a player down through a Maw: the same x and z, onto a safe floor of the Dungeon, with a Root Gate beside them to come back by. */
    public static boolean enter(ServerPlayer p, BlockPos from) {
        MinecraftServer server = p.getServer();
        ServerLevel dungeon = server == null ? null : server.getLevel(DIMENSION);
        if (dungeon == null) { p.sendSystemMessage(Component.literal("The Dungeon does not answer: its dimension is not loaded.").withStyle(ChatFormatting.GRAY)); return false; }
        if (!DungeonConfig.get(DungeonConfig.ENABLED)) return false;
        if (!in(p)) {
            CompoundTag o = new CompoundTag();
            o.putString("Dim", p.level().dimension().location().toString());
            o.putDouble("X", p.getX());
            o.putDouble("Y", p.getY());
            o.putDouble("Z", p.getZ());
            o.putFloat("Yaw", p.getYRot());
            o.putFloat("Pitch", p.getXRot());
            p.getPersistentData().put(ORIGIN, o);
        }
        BlockPos floor = findFloor(dungeon, from.getX(), from.getZ());
        placeGate(dungeon, floor);
        p.getPersistentData().putLong(GATE_USED, dungeon.getGameTime() + 80);
        p.teleportTo(dungeon, floor.getX() + 0.5, floor.getY(), floor.getZ() + 0.5, p.getYRot(), 0f);
        dungeon.playSound(null, floor, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5f, 0.6f);
        p.sendSystemMessage(Component.literal("The roots close behind you. The dark here breathes. A Root Gate glows beside you: it is the way back.").withStyle(ChatFormatting.DARK_GREEN));
        return true;
    }

    /** Takes a player back to where they went down from (or to the world spawn if that is lost). */
    public static void leave(ServerPlayer p) {
        if (!in(p)) return;
        MinecraftServer server = p.getServer();
        CompoundTag o = p.getPersistentData().getCompound(ORIGIN);
        ServerLevel to = o.isEmpty() ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(o.getString("Dim"))));
        if (to == null) {
            to = server.overworld();
            BlockPos s = p.getRespawnPosition() != null ? p.getRespawnPosition() : to.getSharedSpawnPos();
            p.teleportTo(to, s.getX() + 0.5, s.getY(), s.getZ() + 0.5, p.getYRot(), p.getXRot());
        } else {
            p.teleportTo(to, o.getDouble("X"), o.getDouble("Y"), o.getDouble("Z"), o.getFloat("Yaw"), o.getFloat("Pitch"));
        }
        p.getPersistentData().remove(ORIGIN);
        p.getPersistentData().putLong(GATE_USED, to.getGameTime() + 80);
        p.sendSystemMessage(Component.literal("Daylight, or what passes for it. The Dungeon lets you go.").withStyle(ChatFormatting.GRAY));
    }

    /** Has this player just used a way in or out (so the block they were put beside does not send them straight back)? */
    public static boolean justTravelled(ServerPlayer p) { return p.level().getGameTime() < p.getPersistentData().getLong(GATE_USED); }

    // ---------------------------------------------------------------- finding a floor
    /** The first standing place at or near this column of the Dungeon, searching down from just under the roof; a small chamber is cut if there is none. */
    private static BlockPos findFloor(ServerLevel d, int x, int z) {
        for (int ring = 0; ring <= 8; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    BlockPos hit = column(d, x + dx * 3, z + dz * 3);
                    if (hit != null) return hit;
                }
            }
        }
        BlockPos c = new BlockPos(x, 40, z);                                         // nothing open anywhere near: cut a chamber
        for (int dx = -2; dx <= 2; dx++)
            for (int dz = -2; dz <= 2; dz++) {
                d.setBlock(c.offset(dx, -1, dz), Blocks.STONE_BRICKS.defaultBlockState(), 3);
                for (int dy = 0; dy < 4; dy++) d.setBlock(c.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
            }
        return c;
    }

    private static BlockPos column(ServerLevel d, int x, int z) {
        d.getChunk(x >> 4, z >> 4);                                                  // generates the chunk if it is new
        for (int y = d.getMaxBuildHeight() - 8; y > d.getMinBuildHeight() + 4; y--) {
            BlockPos p = new BlockPos(x, y, z);
            if (d.getBlockState(p).isAir() && d.getBlockState(p.above()).isAir() && d.getFluidState(p).isEmpty() && d.getFluidState(p.below()).isEmpty()
                    && d.getBlockState(p.below()).isFaceSturdy(d, p.below(), Direction.UP)) return p;
        }
        return null;
    }

    /** The Root Gate beside the landing: a lit block that takes a player back up. */
    private static void placeGate(ServerLevel d, BlockPos floor) {
        for (Direction dir : new Direction[]{Direction.EAST, Direction.WEST, Direction.SOUTH, Direction.NORTH}) {
            BlockPos g = floor.relative(dir, 2);
            if (d.getBlockState(g).isAir() && d.getBlockState(g.above()).isAir() && d.getBlockState(g.below()).isFaceSturdy(d, g.below(), Direction.UP)) {
                d.setBlock(g, DungeonBlocks.DUNGEON_GATE.get().defaultBlockState(), 3);
                return;
            }
        }
        BlockPos g = floor.east(2);                                                  // no room: make some
        d.setBlock(g.below(), Blocks.STONE_BRICKS.defaultBlockState(), 3);
        d.setBlock(g, DungeonBlocks.DUNGEON_GATE.get().defaultBlockState(), 3);
        d.setBlock(g.above(), Blocks.AIR.defaultBlockState(), 3);
    }
}
