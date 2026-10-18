package com.newuniverse.nusmp.heroweapon.curse;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the wielder accepts or turns away the weapon's offer with a deliberate act on their own screen (holding Sneak and the Embrace key for three seconds), and the
 * client has to tell the server. This is that one small client-to-server packet ({@code nusmp:hero_curse_act}): an action number and nothing else. The server never
 * trusts it: it asks whether an offer is really open, whether the player really has a bond, whether the weapon holds them, and refuses otherwise, exactly as it
 * does for the command and the Weapon Book's button, which reach the same methods.
 *
 * @param action {@link #EMBRACE} or {@link #DECLINE}
 */
public record CurseActPayload(int action) implements CustomPacketPayload {
    public static final int EMBRACE = 0, DECLINE = 1;

    public static final Type<CurseActPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_curse_act"));
    public static final StreamCodec<FriendlyByteBuf, CurseActPayload> CODEC = StreamCodec.of((b, p) -> b.writeByte(p.action()), b -> new CurseActPayload(b.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
