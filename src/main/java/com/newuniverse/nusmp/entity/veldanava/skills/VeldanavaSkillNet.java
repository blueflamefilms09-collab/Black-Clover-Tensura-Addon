package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.entity.veldanava.WillOfTheCreator;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Why: the card screen of the Will of the Creator. Two packets, both tiny: {@code nusmp:veldanava_skills_open} (server to client: open the screen, with the id of the skill the player already holds,
 * "" for none) and {@code nusmp:veldanava_skills_claim} (client to server: "I take this card", or "" for the loot alone). The server decides everything: the claim is read with a 24-character limit,
 * only a player can send it, the player must have an orb waiting for them ({@link WillOfTheCreator#waitingFor}), and the card must be one this build can give ({@link JudgmentRules#claimable}).
 * A client that does not have the mod never gets the open packet (the registrar is optional) and uses the clickable chat lines instead. Register {@link #registerPayloads} on the mod bus.
 */
public final class VeldanavaSkillNet {
    private VeldanavaSkillNet() {}

    public static final int ID_MAX = 24;

    public record Open(String chosen) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "veldanava_skills_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = StreamCodec.of((buf, p) -> buf.writeUtf(p.chosen(), ID_MAX), buf -> new Open(buf.readUtf(ID_MAX)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Claim(String id) implements CustomPacketPayload {
        public static final Type<Claim> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "veldanava_skills_claim"));
        public static final StreamCodec<FriendlyByteBuf, Claim> CODEC = StreamCodec.of((buf, p) -> buf.writeUtf(p.id(), ID_MAX), buf -> new Claim(buf.readUtf(ID_MAX)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Open.TYPE, Open.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.veldanava.VeldanavaSkillClient.onOpen(p.chosen())));
        r.playToServer(Claim.TYPE, Claim.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) onClaim(sp, p.id());
        }));
    }

    public static void sendOpen(ServerPlayer p) {
        PacketDistributor.sendToPlayer(p, new Open(VeldanavaSkills.chosen(p)));
    }

    /** The server end of both the packet and the command: finds the orb that waits for the player and lets it judge the claim. Answers the player. */
    public static boolean onClaim(ServerPlayer p, String id) {
        WillOfTheCreator orb = WillOfTheCreator.waitingFor(p);
        if (orb == null) { say(p, "The Will of the Creator is not waiting for you.", false); return false; }
        String answer = orb.claimSkill(p, id == null ? "" : id);
        boolean ok = orb.hasClaimed(p.getUUID());
        say(p, answer, ok);
        return ok;
    }

    private static void say(ServerPlayer p, String text, boolean ok) {
        p.sendSystemMessage(Component.literal(text).withStyle(ok ? ChatFormatting.GOLD : ChatFormatting.RED));
    }
}
