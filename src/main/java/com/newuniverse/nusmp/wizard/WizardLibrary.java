package com.newuniverse.nusmp.wizard;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * 1.11.0: the Tower Wizard's library, a pocket dimension in the void (data/nusmp/dimension/grimoire_library.json): a round reading room
 * with shelves, lanterns and a central rug, hung with books that fly around whoever stands in it. Right-clicking the Grimoire Summons
 * carries you here; the wizard waits at the centre; when your grimoire has come to you (or you ask him, sneaking) he sends you back
 * to where you stood. The room is built the first time it is entered; the flying books exist only while somebody is inside.
 */
public final class WizardLibrary {
    private WizardLibrary() {}

    public static final ResourceKey<Level> DIMENSION = ResourceKey.create(Registries.DIMENSION, ResourceLocation.fromNamespaceAndPath("nusmp", "grimoire_library"));
    private static final String BOOK_TAG = "nusmp_library_book";
    private static final int FLOOR_Y = 64, BOOKS = 26;
    private static final String ORIGIN = "nusmp_library_origin";

    public static boolean inLibrary(net.minecraft.world.entity.Entity e) { return e.level().dimension() == DIMENSION; }

    // ---------------------------------------------------------------- entering and leaving
    public static boolean enter(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        ServerLevel lib = server.getLevel(DIMENSION);
        if (lib == null) return false;
        build(lib);
        WizardSystem.ensureWizard(lib, new BlockPos(0, FLOOR_Y, 0));
        if (!inLibrary(p)) {
            CompoundTag o = new CompoundTag();
            o.putString("Dim", p.level().dimension().location().toString());
            o.putDouble("X", p.getX());
            o.putDouble("Y", p.getY());
            o.putDouble("Z", p.getZ());
            o.putFloat("Yaw", p.getYRot());
            o.putFloat("Pitch", p.getXRot());
            p.getPersistentData().put(ORIGIN, o);
        }
        p.teleportTo(lib, 0.5, FLOOR_Y + 1, 6.5, 180f, 8f);
        lib.playSound(null, p.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.6f, 0.6f);
        p.sendSystemMessage(Component.literal("Books drift through the air of a quiet room. An old wizard looks up from the middle of it. Speak to him.").withStyle(ChatFormatting.DARK_PURPLE));
        return true;
    }

    public static void leave(ServerPlayer p) {
        if (!inLibrary(p)) return;
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
        p.sendSystemMessage(Component.literal("The library fades behind you.").withStyle(ChatFormatting.GRAY));
    }

    // ---------------------------------------------------------------- the room (built once)
    private static void build(ServerLevel lib) {
        if (!lib.getBlockState(new BlockPos(0, FLOOR_Y, 0)).isAir()) return;
        BlockState floor = Blocks.DARK_OAK_PLANKS.defaultBlockState(), rim = Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState(),
                rug = Blocks.PURPLE_CARPET.defaultBlockState(), shelf = Blocks.BOOKSHELF.defaultBlockState(), pillar = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        int R = 10;
        for (int x = -R; x <= R; x++) {
            for (int z = -R; z <= R; z++) {
                double d = Math.sqrt(x * x + z * z);
                if (d > R + 0.5) continue;
                BlockPos fl = new BlockPos(x, FLOOR_Y, z);
                lib.setBlock(fl, d > R - 1 ? rim : floor, 3);
                if (d <= 4.5) lib.setBlock(fl.above(), rug, 3);
                if (d > R - 1.5 && d <= R - 0.5) {                                            // a ring of shelves, with gaps
                    boolean gap = (Math.floorMod(Math.round(Math.atan2(z, x) * 8 / Math.PI), 4) == 0);
                    if (!gap) for (int y = 1; y <= 4; y++) lib.setBlock(fl.above(y), shelf, 3);
                }
            }
        }
        for (int k = 0; k < 8; k++) {                                                          // eight pillars with lanterns
            double a = k * Math.PI / 4;
            BlockPos b = new BlockPos((int) Math.round(Math.cos(a) * (R - 2)), FLOOR_Y + 1, (int) Math.round(Math.sin(a) * (R - 2)));
            for (int y = 0; y < 5; y++) lib.setBlock(b.above(y), pillar, 3);
            lib.setBlock(b.above(5), Blocks.LANTERN.defaultBlockState(), 3);
        }
        lib.setBlock(new BlockPos(0, FLOOR_Y, 0), Blocks.CRYING_OBSIDIAN.defaultBlockState(), 3);   // the marker: the room exists
        lib.setBlock(new BlockPos(2, FLOOR_Y + 1, -1), Blocks.LECTERN.defaultBlockState(), 3);
        lib.setBlock(new BlockPos(-2, FLOOR_Y + 1, -1), Blocks.ENCHANTING_TABLE.defaultBlockState(), 3);
        lib.setDefaultSpawnPos(new BlockPos(0, FLOOR_Y + 1, 6), 0f);
    }

    // ---------------------------------------------------------------- the books
    public static void onServerTick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 2 != 0) return;
        ServerLevel lib = e.getServer().getLevel(DIMENSION);
        if (lib == null) return;
        List<ServerPlayer> players = lib.players();
        AABB all = new AABB(-80, FLOOR_Y - 20, -80, 80, FLOOR_Y + 60, 80);
        List<ItemEntity> books = lib.getEntitiesOfClass(ItemEntity.class, all, b -> b.getTags().contains(BOOK_TAG));
        if (players.isEmpty()) { for (ItemEntity b : books) b.discard(); return; }
        for (int i = books.size(); i < BOOKS; i++) {
            ItemStack it = new ItemStack(i % 3 == 0 ? Items.ENCHANTED_BOOK : i % 3 == 1 ? Items.BOOK : Items.WRITABLE_BOOK);
            ItemEntity b = new ItemEntity(lib, 0.5, FLOOR_Y + 3, 0.5, it);
            b.setNoGravity(true);
            b.setNeverPickUp();
            b.setUnlimitedLifetime();
            b.setInvulnerable(true);
            b.addTag(BOOK_TAG);
            var d = b.getPersistentData();
            d.putDouble("A", lib.random.nextDouble() * Math.PI * 2);
            d.putDouble("R", 1.6 + lib.random.nextDouble() * 3.4);
            d.putDouble("H", -0.6 + lib.random.nextDouble() * 3.0);
            d.putDouble("W", (0.04 + lib.random.nextDouble() * 0.07) * (lib.random.nextBoolean() ? 1 : -1));
            lib.addFreshEntity(b);
        }
        for (ItemEntity b : books) {
            ServerPlayer near = players.get(0);
            for (ServerPlayer p : players) if (p.distanceToSqr(b) < near.distanceToSqr(b)) near = p;
            var d = b.getPersistentData();
            double a = d.getDouble("A") + d.getDouble("W");
            d.putDouble("A", a);
            double r = d.getDouble("R"), bob = Math.sin(a * 2.0 + d.getDouble("H")) * 0.25;
            Vec3 target = near.position().add(Math.cos(a) * r, 1.0 + d.getDouble("H") + bob, Math.sin(a) * r);
            Vec3 to = target.subtract(b.position());
            b.setDeltaMovement(to.scale(0.28));
            b.hurtMarked = true;
        }
    }

    /** Nothing in the library can be broken or taken (outside creative). */
    public static void onBreak(BlockEvent.BreakEvent e) {
        if (e.getPlayer() instanceof Player p && !p.isCreative() && p.level().dimension() == DIMENSION) e.setCanceled(true);
    }
}
