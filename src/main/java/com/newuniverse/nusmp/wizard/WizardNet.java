package com.newuniverse.nusmp.wizard;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** The questionnaire's two packets: the server opens it on the client, the client sends the answers back. The server decides everything. */
public final class WizardNet {
    private WizardNet() {}

    public record OpenPayload(CompoundTag sheet) implements CustomPacketPayload {
        public static final Type<OpenPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "wizard_open"));
        public static final StreamCodec<FriendlyByteBuf, OpenPayload> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, OpenPayload::sheet, OpenPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record AnswerPayload(CompoundTag answers) implements CustomPacketPayload {
        public static final Type<AnswerPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "wizard_answer"));
        public static final StreamCodec<FriendlyByteBuf, AnswerPayload> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, AnswerPayload::answers, AnswerPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(OpenPayload.TYPE, OpenPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.wizard.QuestionnaireScreen.open(payload.sheet())));
        r.playToServer(AnswerPayload.TYPE, AnswerPayload.CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> {
                    if (ctx.player() instanceof ServerPlayer sp) WizardSystem.answer(sp, payload.answers().getIntArray("A"));
                }));
    }
}
