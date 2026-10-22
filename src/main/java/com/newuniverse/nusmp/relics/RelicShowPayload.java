package com.newuniverse.nusmp.relics;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server to client, one player: {@code /multiverse relic show <on|off|toggle>} is a SERVER command (a client command with the root word {@code multiverse} would take the word away from
 * the server's own tree), and the preference it sets is the player's own looks setting {@code relicfx.showWorn} in the client config. So the server only says to the player who ran the command
 * what to do with it; the client sets its own option. The server keeps nothing and learns nothing about the preference. {@code mode}: 0 off, 1 on, 2 toggle, 3 say the state.
 */
public record RelicShowPayload(int mode) implements CustomPacketPayload {
    public static final Type<RelicShowPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "relic_show"));
    public static final int OFF = 0, ON = 1, TOGGLE = 2, STATUS = 3;

    public static final StreamCodec<RegistryFriendlyByteBuf, RelicShowPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> buf.writeByte(p.mode()),
            buf -> {
                int m = buf.readByte();
                if (m < OFF || m > STATUS) throw new IllegalStateException("relic show mode out of range: " + m);
                return new RelicShowPayload(m);
            });

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
