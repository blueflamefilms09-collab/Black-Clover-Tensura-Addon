package com.newuniverse.nusmp.labyrinth.mega.system;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the floor report is drawn on the CLIENT from facts only the SERVER knows (the sector's theme, the mechanics planned for the floor, the depth), so there are
 * two payloads. {@link Card} (server to client) is the whole report as ids, English fallbacks and numbers, never translated text: each player reads it in their own
 * language. {@link Request} (client to server) is the key press: "tell me again", answered with a fresh card (the server rebuilds it, so a mechanic that changed since
 * is in it). Both channels are optional (a client without the mod gets nothing and never asks), and every string and every count on the wire is bounded, so a
 * hostile or damaged packet cannot make the receiver allocate without limit and a datapack text of any length is cut before it is written (the encoder runs later
 * on the network thread, where an exception would disconnect the player). The client handler is client-only code, reached by name inside the lambda, so a dedicated
 * server never loads a client class.
 */
public final class FloorInfoNet {
    private FloorInfoNet() {}

    /** Wire limits. */
    static final int ID = 96, NAME = 96, DESC = 256, HINT = 160, SHORT = 24;

    /** Server to client: the floor report. */
    public record Card(FloorCard card) implements CustomPacketPayload {
        public static final Type<Card> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "system_floor_card"));
        public static final StreamCodec<FriendlyByteBuf, Card> CODEC = StreamCodec.of(Card::write, Card::read);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, Card p) {
            FloorCard c = p.card();
            b.writeByte(c.reason());
            b.writeInt(c.floor());
            b.writeInt(c.previous());
            b.writeBoolean(c.up());
            b.writeVarInt(c.depth());
            b.writeBoolean(c.boss());
            b.writeUtf(FloorInfoRules.clip(c.themeId(), ID), ID);
            b.writeUtf(FloorInfoRules.clip(c.themeName(), NAME), NAME);
            b.writeUtf(FloorInfoRules.clip(c.themeDesc(), DESC), DESC);
            b.writeUtf(FloorInfoRules.clip(c.align(), SHORT), SHORT);
            b.writeVarInt(c.dangerHundredths());
            b.writeVarInt(c.healthPct());
            b.writeVarInt(c.damagePct());
            b.writeVarInt(c.mind());
            b.writeVarInt(c.xpTimes100());
            b.writeBoolean(c.mechanicsOn());
            b.writeVarInt(c.hiddenRooms());
            writeMechs(b, c.floorMechs());
            writeMechs(b, c.hereMechs());
        }

        static Card read(FriendlyByteBuf b) {
            int reason = b.readByte();
            int floor = b.readInt();
            int previous = b.readInt();
            boolean up = b.readBoolean();
            int depth = b.readVarInt();
            boolean boss = b.readBoolean();
            String themeId = b.readUtf(ID);
            String themeName = b.readUtf(NAME);
            String themeDesc = b.readUtf(DESC);
            String align = b.readUtf(SHORT);
            int danger = b.readVarInt();
            int health = b.readVarInt();
            int damage = b.readVarInt();
            int mind = b.readVarInt();
            int xp = b.readVarInt();
            boolean on = b.readBoolean();
            int hidden = b.readVarInt();
            List<FloorCard.Mech> floorMechs = readMechs(b);
            List<FloorCard.Mech> hereMechs = readMechs(b);
            return new Card(new FloorCard(reason, floor, previous, up, depth, boss, themeId, themeName, themeDesc, align, danger, health, damage, mind, xp, on, hidden, floorMechs, hereMechs));
        }

        private static void writeMechs(FriendlyByteBuf b, List<FloorCard.Mech> list) {
            int n = Math.min(list.size(), FloorCard.MAX_MECHS + 1);
            b.writeByte(n);
            for (int i = 0; i < n; i++) {
                FloorCard.Mech m = list.get(i);
                b.writeUtf(FloorInfoRules.clip(m.id(), ID), ID);
                b.writeUtf(FloorInfoRules.clip(m.name(), NAME), NAME);
                b.writeUtf(FloorInfoRules.clip(m.desc(), DESC), DESC);
                b.writeUtf(FloorInfoRules.clip(m.counterKind(), SHORT), SHORT);
                b.writeUtf(FloorInfoRules.clip(m.hint(), HINT), HINT);
                b.writeUtf(FloorInfoRules.clip(m.scope(), SHORT), SHORT);
                b.writeUtf(FloorInfoRules.clip(m.state(), SHORT), SHORT);
            }
        }

        private static List<FloorCard.Mech> readMechs(FriendlyByteBuf b) {
            int n = Math.max(0, Math.min(b.readByte(), FloorCard.MAX_MECHS + 1));
            List<FloorCard.Mech> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) out.add(new FloorCard.Mech(b.readUtf(ID), b.readUtf(NAME), b.readUtf(DESC), b.readUtf(SHORT), b.readUtf(HINT), b.readUtf(SHORT), b.readUtf(SHORT)));
            return out;
        }
    }

    /** Client to server: the key was pressed, send the report again. */
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "system_floor_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> CODEC = StreamCodec.of((b, p) -> { }, b -> new Request());

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod bus: registers both payloads (optional channels). */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Card.TYPE, Card.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.mega.system.FloorCardClient.receive(p.card())));
        r.playToServer(Request.TYPE, Request.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) FloorInfo.request(sp);
        }));
    }
}
