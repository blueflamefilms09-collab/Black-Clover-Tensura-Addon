package com.newuniverse.nusmp.beastwill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: a Beast Will Core (3.45.0) is an ordinary item with ONE data component, so a core is never "a will" until a player absorbs it. The component names the will (the data file id), the beast it came from
 * and its rank (for the preview), and carries a SERIAL: a random id that the ledger ({@link BeastWillLedger}) remembers once the core has been absorbed, so a duplicated stack can be absorbed only once
 * whatever happens to the original. Persistent and network synchronised (the tooltip reads it on the client). The id is new; nothing old is touched.
 */
public final class BeastWillComponents {
    private BeastWillComponents() {}

    public static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, NUSMP.MODID);

    /** The content of a core: which will, which beast, which rank (the name of a {@link BeastRank}), which serial. */
    public record Core(String will, String beast, String rank, String serial) {
        public static final Codec<Core> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("will").forGetter(Core::will),
                Codec.STRING.fieldOf("beast").forGetter(Core::beast),
                Codec.STRING.fieldOf("rank").forGetter(Core::rank),
                Codec.STRING.fieldOf("serial").forGetter(Core::serial)).apply(i, Core::new));
        public static final StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Core> STREAM = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(40), Core::will, ByteBufCodecs.stringUtf8(64), Core::beast, ByteBufCodecs.stringUtf8(4), Core::rank, ByteBufCodecs.stringUtf8(40), Core::serial, Core::new);

        /** The rank of the core, A when the text is damaged (a core is never worse than the lowest rank that can give one). */
        public BeastRank rankValue() {
            BeastRank r = BeastRank.byName(rank);
            return r == null ? BeastRank.A : r;
        }
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Core>> CORE =
            COMPONENTS.registerComponentType("beast_will_core", b -> b.persistent(Core.CODEC).networkSynchronized(Core.STREAM));

    /** The core of a stack, or null (not a core, or a bare creative stack). */
    public static Core of(ItemStack stack) { return stack == null || stack.isEmpty() ? null : stack.get(CORE.get()); }
}
