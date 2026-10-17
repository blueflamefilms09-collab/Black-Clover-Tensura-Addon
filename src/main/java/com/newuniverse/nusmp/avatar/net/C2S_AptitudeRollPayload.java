package com.newuniverse.nusmp.avatar.net;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the "Randomize Aptitude" button. It carries only the aptitudes the creator has already picked by hand, so the free roll never lands on one of them;
 * everything else (the pool, the luck, the odds, the result) is the server's. Rate limited and capped per ritual on the server.
 */
public record C2S_AptitudeRollPayload(List<String> picked) implements CustomPacketPayload {
    public static final int MAX = 8;
    public static final Type<C2S_AptitudeRollPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "forge_roll"));
    public static final StreamCodec<RegistryFriendlyByteBuf, C2S_AptitudeRollPayload> CODEC = StreamCodec.of(
            (b, p) -> {
                int n = Math.min(p.picked.size(), MAX);
                b.writeVarInt(n);
                for (int i = 0; i < n; i++) b.writeUtf(NetText.clean(p.picked.get(i), NetText.ID_MAX), NetText.ID_MAX);
            },
            b -> {
                int n = Math.max(0, Math.min(b.readVarInt(), MAX));
                List<String> l = new ArrayList<>(n);
                for (int i = 0; i < n; i++) l.add(b.readUtf(NetText.ID_MAX));
                return new C2S_AptitudeRollPayload(l);
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
