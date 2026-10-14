package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code nusmp:socket} data component: {@code {b: [{id: "spirit:fire", t: <game time bound>}, ...]}}, saved and synced (tooltips and the client
 * particles read it). It is a compound so the shape can grow without a new component, like {@code relic_uses}. Register {@link #COMPONENTS} on the
 * mod bus. Reading never changes the stack; writing sets a fresh compound (a stack's component values are shared, never mutated in place).
 */
public final class SocketComponents {
    private SocketComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<CompoundTag>> SOCKET =
            COMPONENTS.registerComponentType("socket", b -> b.persistent(CompoundTag.CODEC).networkSynchronized(ByteBufCodecs.COMPOUND_TAG));

    private static final String LIST = "b", ID = "id", TIME = "t";

    /** The sockets of a stack (empty if it has none). At most {@link SocketData#MAX} entries are read; ids the catalog does not know are kept (a newer version may know them). */
    public static SocketData get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return SocketData.EMPTY;
        CompoundTag t = stack.get(SOCKET.get());
        if (t == null) return SocketData.EMPTY;
        ListTag l = t.getList(LIST, Tag.TAG_COMPOUND);
        if (l.isEmpty()) return SocketData.EMPTY;
        List<SocketData.Entry> out = new ArrayList<>(Math.min(l.size(), SocketData.MAX));
        for (int i = 0; i < l.size() && out.size() < SocketData.MAX; i++) {
            CompoundTag e = l.getCompound(i);
            String id = e.getString(ID);
            if (!id.isEmpty()) out.add(new SocketData.Entry(id, e.getLong(TIME)));
        }
        return new SocketData(out);
    }

    /** Writes the sockets (the component is removed when there are none). */
    public static void set(ItemStack stack, SocketData data) {
        if (stack == null || stack.isEmpty()) return;
        if (data == null || data.isEmpty()) {
            stack.remove(SOCKET.get());
            return;
        }
        ListTag l = new ListTag();
        for (SocketData.Entry e : data.entries()) {
            CompoundTag c = new CompoundTag();
            c.putString(ID, e.id());
            c.putLong(TIME, e.boundAt());
            l.add(c);
        }
        CompoundTag t = new CompoundTag();
        t.put(LIST, l);
        stack.set(SOCKET.get(), t);
    }

    /**
     * The id of the first bound socket without building any list: for the render path and the particles, which ask for it every few ticks.
     * "" if the stack has none.
     */
    public static String firstId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return "";
        CompoundTag t = stack.get(SOCKET.get());
        if (t == null) return "";
        ListTag l = t.getList(LIST, Tag.TAG_COMPOUND);
        return l.isEmpty() ? "" : l.getCompound(0).getString(ID);
    }

    /** Does the stack carry at least one socket? Allocation free. */
    public static boolean hasAny(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        CompoundTag t = stack.get(SOCKET.get());
        return t != null && !t.getList(LIST, Tag.TAG_COMPOUND).isEmpty();
    }

    public static void init() {}
}
