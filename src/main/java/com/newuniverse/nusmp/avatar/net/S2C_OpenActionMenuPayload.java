package com.newuniverse.nusmp.avatar.net;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

/** Why: opens the small action menu (Name / Info) of the creator's own Avatar; carries what the Info tab shows, so the client needs no extra requests. */
public record S2C_OpenActionMenuPayload(UUID avatarId, String name, boolean named, String race, String primary, String secondary, double luck, float scale,
                                        List<String> info) implements CustomPacketPayload {
    public static final Type<S2C_OpenActionMenuPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "avatar_open_actions"));
    public static final StreamCodec<RegistryFriendlyByteBuf, S2C_OpenActionMenuPayload> CODEC = StreamCodec.of(S2C_OpenActionMenuPayload::write, S2C_OpenActionMenuPayload::read);

    private static void write(RegistryFriendlyByteBuf b, S2C_OpenActionMenuPayload p) {
        UUIDUtil.STREAM_CODEC.encode(b, p.avatarId);
        ByteBufCodecs.STRING_UTF8.encode(b, NetText.clean(p.name, NetText.NAME_MAX));
        b.writeBoolean(p.named);
        ByteBufCodecs.STRING_UTF8.encode(b, NetText.clean(p.race, NetText.ID_MAX));
        ByteBufCodecs.STRING_UTF8.encode(b, NetText.clean(p.primary, NetText.ID_MAX));
        ByteBufCodecs.STRING_UTF8.encode(b, NetText.clean(p.secondary, NetText.ID_MAX));
        b.writeDouble(p.luck); b.writeFloat(p.scale);
        int n = Math.min(p.info.size(), 12);
        b.writeVarInt(n);
        for (int i = 0; i < n; i++) ByteBufCodecs.STRING_UTF8.encode(b, NetText.clean(p.info.get(i), 120));
    }

    private static S2C_OpenActionMenuPayload read(RegistryFriendlyByteBuf b) {
        UUID id = UUIDUtil.STREAM_CODEC.decode(b);
        String name = NetText.clean(ByteBufCodecs.STRING_UTF8.decode(b), NetText.NAME_MAX);
        boolean named = b.readBoolean();
        String race = NetText.clean(ByteBufCodecs.STRING_UTF8.decode(b), NetText.ID_MAX), pr = NetText.clean(ByteBufCodecs.STRING_UTF8.decode(b), NetText.ID_MAX),
                se = NetText.clean(ByteBufCodecs.STRING_UTF8.decode(b), NetText.ID_MAX);
        double luck = b.readDouble();
        float scale = b.readFloat();
        int n = Math.min(b.readVarInt(), 12);
        java.util.List<String> info = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) info.add(NetText.clean(ByteBufCodecs.STRING_UTF8.decode(b), 120));
        return new S2C_OpenActionMenuPayload(id, name, named, race, pr, se, luck, scale, info);
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
