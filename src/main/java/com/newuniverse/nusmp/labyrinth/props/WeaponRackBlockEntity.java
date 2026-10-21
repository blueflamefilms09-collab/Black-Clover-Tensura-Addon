package com.newuniverse.nusmp.labyrinth.props;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Why: the contents of a weapon rack. Five item stacks and a {@link RackMode}; nothing else, no ticker, no entity: the renderer reads {@link #get} on the client and the block's use handlers
 * call {@link #put} and {@link #take} on the server. The items are saved with the vanilla container helper (so a rack carries enchantments and the mod's own components like a chest does) and
 * synced to the client in the update tag, once per change ({@link #sync}). The stacks hold ONE item each: a rack shows a thing, it does not store a pile. {@link #getRenderBoundingBox} is taller
 * than the block because the frame and the weapons on it rise above the block's top face.
 */
public final class WeaponRackBlockEntity extends BlockEntity {
    private final NonNullList<ItemStack> items = NonNullList.withSize(RackLayout.SLOTS, ItemStack.EMPTY);
    private RackMode mode = RackMode.OPEN;
    private AABB renderBox;

    public WeaponRackBlockEntity(BlockPos pos, BlockState state) { super(PropsRegistry.RACK_BE.get(), pos, state); }

    public RackMode mode() { return mode; }

    public void setMode(RackMode m) {
        mode = m == null ? RackMode.DECOR : m;
        sync();
    }

    /** The stack in a slot (never null; an out-of-range slot is empty). */
    public ItemStack get(int slot) { return slot < 0 || slot >= RackLayout.SLOTS ? ItemStack.EMPTY : items.get(slot); }

    /** Replaces the stack in a slot (a copy of one item) and syncs. */
    public void set(int slot, ItemStack stack) {
        if (slot < 0 || slot >= RackLayout.SLOTS) return;
        items.set(slot, stack == null || stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        sync();
    }

    public int count() {
        int n = 0;
        for (ItemStack s : items) if (!s.isEmpty()) n++;
        return n;
    }

    public int firstEmpty() {
        for (int i = 0; i < RackLayout.SLOTS; i++) if (items.get(i).isEmpty()) return i;
        return -1;
    }

    public int lastFilled() {
        for (int i = RackLayout.SLOTS - 1; i >= 0; i--) if (!items.get(i).isEmpty()) return i;
        return -1;
    }

    /** Hangs one item of the stack on the first free slot and returns the slot, or -1 when the rack is full (the stack is not changed here). */
    public int put(ItemStack stack) {
        int slot = firstEmpty();
        if (slot < 0 || stack == null || stack.isEmpty()) return -1;
        items.set(slot, stack.copyWithCount(1));
        sync();
        return slot;
    }

    /** Takes the last item off the rack and returns it (empty when the rack is bare). */
    public ItemStack take() {
        int slot = lastFilled();
        if (slot < 0) return ItemStack.EMPTY;
        ItemStack out = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        sync();
        return out;
    }

    public void clear() {
        for (int i = 0; i < RackLayout.SLOTS; i++) items.set(i, ItemStack.EMPTY);
        sync();
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        ContainerHelper.saveAllItems(tag, items, lookup);
        tag.putInt("Mode", mode.id());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        for (int i = 0; i < RackLayout.SLOTS; i++) items.set(i, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items, lookup);
        mode = tag.contains("Mode") ? RackMode.byId(tag.getInt("Mode")) : RackMode.OPEN;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    /**
     * Made once (the culling test asks for it every frame, and a frame must not allocate). This is the loader's {@code IBlockEntityExtension.getRenderBoundingBox}; it carries no override mark so that a
     * loader without that hook still builds (the rack is then culled by its block box, which only costs the top of the frame at the screen edge).
     */
    public AABB getRenderBoundingBox() {
        AABB box = renderBox;
        if (box == null) {
            box = new AABB(worldPosition.getX() - 0.1, worldPosition.getY(), worldPosition.getZ() - 0.1, worldPosition.getX() + 1.1, worldPosition.getY() + RackLayout.FRAME_HEIGHT + 0.1, worldPosition.getZ() + 1.1);
            renderBox = box;
        }
        return box;
    }
}
