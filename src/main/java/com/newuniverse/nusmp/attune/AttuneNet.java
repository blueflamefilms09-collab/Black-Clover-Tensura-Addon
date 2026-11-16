package com.newuniverse.nusmp.attune;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Why: meditation talks over two small packets. DOWN, only to the player who ran /meditate: what they own (at most {@link AttuneRules#MAX_OWNED} powers: id, system, elements, strength, name), what is
 * attuned now, the slot count, the cooldown left and the resonance per pair, so the screen can colour the pairs with the same {@link AttuneRules} table. UP, one per confirm: the chosen ids (at most
 * {@link AttuneRules#MAX_SLOTS}, each bounded). The server decides everything again on a confirm ({@link AttuneService#confirm}): the sender is the real player, a session is open, they stood still,
 * they own each id, the slots, the cooldown, the clashes. The client hook is {@link #clientOpen} (set by the client class), so this common file names no client class.
 */
public final class AttuneNet {
    private AttuneNet() {}

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<Open> clientOpen;

    public record Entry(String id, int system, int elements, int rank, String label) {}

    public record Open(int slots, int cooldownTicks, int resonancePercent, List<String> attuned, List<Entry> owned) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "attune_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> STREAM_CODEC = StreamCodec.of(AttuneNet::writeOpen, AttuneNet::readOpen);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Confirm(List<String> ids) implements CustomPacketPayload {
        public static final Type<Confirm> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "attune_confirm"));
        public static final StreamCodec<FriendlyByteBuf, Confirm> STREAM_CODEC = StreamCodec.of((buf, p) -> writeIds(buf, p.ids(), AttuneRules.MAX_SLOTS), buf -> new Confirm(readIds(buf, AttuneRules.MAX_SLOTS)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static final int LABEL_MAX = 48;

    private static void writeIds(FriendlyByteBuf buf, List<String> ids, int max) {
        int n = Math.min(max, ids.size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) buf.writeUtf(ids.get(i), AttuneRules.MAX_ID_LENGTH);
    }

    private static List<String> readIds(FriendlyByteBuf buf, int max) {
        int n = buf.readVarInt();
        if (n < 0 || n > max) throw new IllegalStateException("attune: " + n + " ids");          // a client that sends more than the slots could hold is not ours
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(buf.readUtf(AttuneRules.MAX_ID_LENGTH));
        return out;
    }

    private static void writeOpen(FriendlyByteBuf buf, Open p) {
        buf.writeByte(p.slots());
        buf.writeVarInt(Math.max(0, p.cooldownTicks()));
        buf.writeByte(Math.max(0, Math.min(100, p.resonancePercent())));
        writeIds(buf, p.attuned(), AttuneRules.MAX_SLOTS);
        int n = Math.min(AttuneRules.MAX_OWNED, p.owned().size());
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) {
            Entry e = p.owned().get(i);
            buf.writeUtf(e.id(), AttuneRules.MAX_ID_LENGTH);
            buf.writeByte(e.system());
            buf.writeVarInt(e.elements());
            buf.writeByte(e.rank());
            String l = e.label() == null ? "" : e.label();
            buf.writeUtf(l.length() > LABEL_MAX ? l.substring(0, LABEL_MAX) : l, LABEL_MAX);
        }
    }

    private static Open readOpen(FriendlyByteBuf buf) {
        int slots = buf.readUnsignedByte(), cd = buf.readVarInt(), res = buf.readUnsignedByte();
        List<String> attuned = readIds(buf, AttuneRules.MAX_SLOTS);
        int n = buf.readVarInt();
        if (n < 0 || n > AttuneRules.MAX_OWNED) throw new IllegalStateException("attune: " + n + " powers");
        List<Entry> owned = new ArrayList<>(n);
        for (int i = 0; i < n; i++) owned.add(new Entry(buf.readUtf(AttuneRules.MAX_ID_LENGTH), buf.readUnsignedByte(), buf.readVarInt(), buf.readUnsignedByte(), buf.readUtf(LABEL_MAX)));
        return new Open(slots, cd, res, attuned, owned);
    }

    public static void send(ServerPlayer p, Open o) {
        if (p.connection == null) return;                                    // a stand-in of a self-test has no connection
        try {
            PacketDistributor.sendToPlayer(p, o);
        } catch (RuntimeException noConnection) {
            // a player who is leaving: nothing to tell
        }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(Confirm.TYPE, Confirm.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) AttuneService.confirm(sp, payload.ids()); }));
        r.playToClient(Open.TYPE, Open.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<Open> c = clientOpen; if (c != null) c.accept(payload); }));
    }
}
