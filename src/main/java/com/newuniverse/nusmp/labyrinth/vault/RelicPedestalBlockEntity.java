package com.newuniverse.nusmp.labyrinth.vault;

import com.newuniverse.nusmp.relics.Grade;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Why: the state of a Relic Pedestal: the one stack it holds, the loot table it still has to roll, the seed, and whether it was taken.
 * A pedestal rolls its table on the first use, not when the structure is placed: at placement time there is no player (no luck, no
 * {@code THIS_ENTITY}), the chunk may be generated long before anyone arrives, and a roll at generation would stamp relics into the God of
 * Order's ledger for pedestals nobody has reached yet, draining the pool of unissued relics for the whole world. Rolling on use is what a
 * vanilla chest does ({@code unpackLootTable}), so a Lootr-like per-player behaviour is NOT applied: a pedestal is a single, visible item and
 * whoever takes it has it; the ledger makes the second player's pedestal elsewhere roll a different relic.
 * <p>
 * {@code LootTable} / {@code LootTableSeed} are read with the chest's NBT names so a template's block entity NBT is written exactly like a
 * chest's. The held stack is sent to the client with the block entity's update tag and packet: the renderer draws it, and the grade of the
 * relic colours the glow. Only the first non-empty stack of the roll is held: the rest (the second half of a pair, the mundane fallback of a
 * table that also rolled filler) is dropped at the pedestal, so a pair never loses a half and nothing the table rolled is thrown away.
 */
public final class RelicPedestalBlockEntity extends BlockEntity {
    private ItemStack held = ItemStack.EMPTY;
    private ResourceKey<LootTable> lootTable;
    private long lootTableSeed;
    private boolean taken;

    /** Client only: the tick phase of the bob and the turn (advanced by the client ticker, read with the partial tick by the renderer). */
    private float phase;

    /**
     * 1.54.0: a rule another system puts in front of the pick. It answers {@code TRUE} (it handled the click and something was given), {@code FALSE} (it
     * handled the click and nothing was given) or {@code null} (not its pedestal: the old behaviour below runs unchanged). The Crucible of Choice installs
     * one so a vault's pedestals obey "one relic per region" while a Spire's pedestal, or one placed by an admin, works exactly as before.
     */
    @FunctionalInterface
    public interface TakeHook extends java.util.function.BiFunction<ServerPlayer, RelicPedestalBlockEntity, Boolean> {}

    private static volatile TakeHook takeHook;

    /** Installs (or, with null, removes) the take hook; called once at start-up by the Mega-Dungeon's theme builder. */
    public static void setTakeHook(TakeHook hook) { takeHook = hook; }

    public RelicPedestalBlockEntity(BlockPos pos, BlockState state) { super(VaultRegistry.PEDESTAL_BE.get(), pos, state); }

    // ---------------------------------------------------------------- nbt
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        held = tag.contains("Held", 10) ? ItemStack.parseOptional(registries, tag.getCompound("Held")) : ItemStack.EMPTY;
        ResourceLocation table = tag.contains("LootTable", 8) ? ResourceLocation.tryParse(tag.getString("LootTable")) : null;
        lootTable = table == null ? null : ResourceKey.create(Registries.LOOT_TABLE, table);
        lootTableSeed = tag.getLong("LootTableSeed");
        taken = tag.getBoolean("Taken");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!held.isEmpty()) tag.put("Held", held.save(registries));
        if (lootTable != null) tag.putString("LootTable", lootTable.location().toString());
        if (lootTableSeed != 0L) tag.putLong("LootTableSeed", lootTableSeed);
        if (taken) tag.putBoolean("Taken", true);
    }

    /** The full tag: the client needs the held stack to draw it. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    // ---------------------------------------------------------------- state
    public ItemStack getHeld() { return held; }

    public boolean isTaken() { return taken; }

    public ResourceKey<LootTable> getLootTable() { return lootTable; }

    /** Sets what floats above the pedestal (admin / future use); the client is told. */
    public void setHeld(ItemStack stack) {
        held = stack == null ? ItemStack.EMPTY : stack;
        sync();
    }

    /** 1.54.0: marks the pedestal taken (the Crucible's crumbled slots) or fresh again (an admin reset); saved with the block entity. */
    public void setTaken(boolean value) {
        taken = value;
        sync();
    }

    /** The 0xRRGGBB of the held relic's grade, white for anything else: the renderer's glow colour. */
    public int glowRgb() {
        RelicDef d = Relics.defOf(held);
        Grade g = d == null ? null : d.grade();
        return g == null ? 0xFFFFFF : g.rgb();
    }

    public float phase() { return phase; }

    /** Client ticker: only the phase moves; the server never ticks a pedestal. */
    public static void clientTick(Level level, BlockPos pos, BlockState state, RelicPedestalBlockEntity be) {
        be.phase += 1f;
        if (be.phase >= 24000f) be.phase -= 24000f;
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    // ---------------------------------------------------------------- the pick
    /**
     * Rolls the loot table once, on the first use: the first non-empty stack becomes the held relic, the rest drops at the pedestal (a
     * pair's second half, a fallback the table rolled beside the relic). The table is cleared afterwards, like a chest's, so a pedestal
     * never rolls twice. Seed 0 is a fresh random seed; any other seed makes the roll reproducible (map makers).
     */
    public void unpackLoot(ServerPlayer player) {
        if (!held.isEmpty() || lootTable == null || taken || !(level instanceof ServerLevel server)) return;
        ResourceKey<LootTable> table = lootTable;
        lootTable = null;                                                                   // one roll, ever; even if it yields nothing
        LootTable t = server.getServer().reloadableRegistries().getLootTable(table);
        LootParams params = new LootParams.Builder(server)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(worldPosition))
                .withLuck(player.getLuck())
                .withOptionalParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.CHEST);
        long seed = lootTableSeed == 0L ? server.getRandom().nextLong() : lootTableSeed;
        List<ItemStack> rolled = t.getRandomItems(params, seed);
        for (ItemStack s : rolled) {
            if (s == null || s.isEmpty()) continue;
            if (held.isEmpty()) held = s;
            else Block.popResource(level, worldPosition.above(), s);                        // the second half of a pair is never lost
        }
        sync();
    }

    /**
     * The pick: roll if needed, then hand the held stack to the player (inventory, else dropped at their feet), mark the pedestal taken
     * and tell everyone (particles, the beacon chime, an action-bar line). Returns true when something was given.
     */
    public boolean take(ServerPlayer player) {
        TakeHook hook = takeHook;                                                           // 1.54.0: a Crucible pedestal is judged first; null = not its pedestal, carry on
        if (hook != null) {
            Boolean handled = hook.apply(player, this);
            if (handled != null) return handled;
        }
        if (!(level instanceof ServerLevel server)) return false;
        boolean rolling = !taken && held.isEmpty() && lootTable != null;
        unpackLoot(player);
        if (held.isEmpty()) {
            boolean exhausted = rolling;                                                    // rolled just now and got nothing: the ledger is exhausted
            taken = true;
            sync();
            player.displayClientMessage(Component.translatable(exhausted ? "nusmp.relic_pedestal.exhausted" : "nusmp.relic_pedestal.empty"), true);
            return false;
        }
        ItemStack give = held.copy();
        Component name = give.getHoverName();
        held = ItemStack.EMPTY;
        taken = true;
        if (!player.getInventory().add(give) && !give.isEmpty()) Block.popResource(level, player.blockPosition(), give);
        sync();
        Vec3 c = Vec3.atCenterOf(worldPosition);
        server.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.9, c.z, 24, 0.25, 0.25, 0.25, 0.04);
        server.sendParticles(ParticleTypes.ENCHANT, c.x, c.y + 1.2, c.z, 40, 0.4, 0.4, 0.4, 0.6);
        level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8f, 1.5f);
        level.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.6f, 1.0f);
        player.displayClientMessage(Component.translatable("nusmp.relic_pedestal.taken", name), true);
        com.newuniverse.nusmp.quests.NusmpEvents.fire(player, "labyrinth.vault_opened");
        return true;
    }
}
