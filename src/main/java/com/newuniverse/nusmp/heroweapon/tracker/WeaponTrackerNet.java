package com.newuniverse.nusmp.heroweapon.tracker;

import com.newuniverse.nusmp.heroweapon.HeroKind;
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
 * Why: the Weapon Tracker talks over two tiny packets. UP, {@link Request}: "send me the table" (no data at all, so there is nothing to forge; the server checks the sender is a real player and answers one player
 * at most once per {@link TrackerLimiter#GAP_TICKS} ticks). DOWN, {@link Reply}, only to the player who asked (or ran {@code /multiverse weapons}): the table of at most 21 rows, each a weapon index, a flag byte, a
 * holder count and one short name (at most {@link TrackerRules#MAX_NAME} characters), so a full reply is under 700 bytes and a normal one about 200. Nothing is broadcast and nothing is sent on a timer. The
 * reader refuses a row count above the number of weapons, a weapon index or status out of range, and a name longer than the cap. The client hook {@link #clientReceive} is set by the client class, so this
 * common file names no client class. Register {@link #register} on the mod bus.
 */
public final class WeaponTrackerNet {
    private WeaponTrackerNet() {}

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<Reply> clientReceive;

    private static final int F_OPEN = 1, F_ENABLED = 2, F_HOLY_UNIQUE = 4, F_VASSAL_UNIQUE = 8, F_NAMES = 16;
    private static final int R_STATUS = 3, R_ONLINE = 4, R_UNIQUE = 8;
    private static final int MAX_HOLDERS = 9999;

    /** The client asks for the table. No data. */
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "weapon_tracker_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.unit(new Request());

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The table. {@code open} says the server wants the screen opened (the command); a plain refresh only updates a screen that is already open. */
    public record Reply(boolean open, TrackerRules.Table table) implements CustomPacketPayload {
        public static final Type<Reply> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "weapon_tracker_reply"));
        public static final StreamCodec<FriendlyByteBuf, Reply> STREAM_CODEC = StreamCodec.of(WeaponTrackerNet::write, WeaponTrackerNet::read);

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    static void write(FriendlyByteBuf buf, Reply p) {
        TrackerRules.Table t = p.table();
        TrackerRules.Settings s = t.settings();
        int flags = (p.open() ? F_OPEN : 0) | (s.enabled() ? F_ENABLED : 0) | (s.holyUnique() ? F_HOLY_UNIQUE : 0) | (s.vassalUnique() ? F_VASSAL_UNIQUE : 0) | (s.showNames() ? F_NAMES : 0);
        buf.writeByte(flags);
        buf.writeByte(Math.max(0, Math.min(9, s.maxBonds())));
        buf.writeByte(t.own() == null ? 0 : t.own().ordinal() + 1);
        int n = Math.min(TrackerRules.maxRows(), t.rows().size());
        buf.writeByte(n);
        for (int i = 0; i < n; i++) {
            TrackerRules.Row r = t.rows().get(i);
            buf.writeByte(r.kind().ordinal());
            buf.writeByte(r.status().ordinal() | (r.online() ? R_ONLINE : 0) | (r.unique() ? R_UNIQUE : 0));
            buf.writeVarInt(Math.max(0, Math.min(MAX_HOLDERS, r.holders())));
            buf.writeUtf(TrackerRules.clean(r.holder()), TrackerRules.MAX_NAME);
        }
    }

    static Reply read(FriendlyByteBuf buf) {
        int flags = buf.readUnsignedByte();
        int maxBonds = buf.readUnsignedByte();
        int own = buf.readUnsignedByte();
        HeroKind[] kinds = HeroKind.values();
        if (own > kinds.length) throw new IllegalStateException("weapon tracker: own weapon " + own + " is not valid");
        int n = buf.readUnsignedByte();
        if (n > kinds.length) throw new IllegalStateException("weapon tracker: " + n + " rows is not valid");
        TrackerRules.Status[] statuses = TrackerRules.Status.values();
        List<TrackerRules.Row> rows = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            int k = buf.readUnsignedByte();
            if (k >= kinds.length) throw new IllegalStateException("weapon tracker: weapon " + k + " is not valid");
            int f = buf.readUnsignedByte();
            int st = f & R_STATUS;
            if (st >= statuses.length) throw new IllegalStateException("weapon tracker: status " + st + " is not valid");
            int holders = buf.readVarInt();
            if (holders < 0 || holders > MAX_HOLDERS) throw new IllegalStateException("weapon tracker: " + holders + " holders is not valid");
            String name = buf.readUtf(TrackerRules.MAX_NAME);
            rows.add(new TrackerRules.Row(kinds[k], statuses[st], holders, name, (f & R_ONLINE) != 0, (f & R_UNIQUE) != 0));
        }
        TrackerRules.Settings s = new TrackerRules.Settings((flags & F_ENABLED) != 0, (flags & F_HOLY_UNIQUE) != 0, (flags & F_VASSAL_UNIQUE) != 0, (flags & F_NAMES) != 0, maxBonds);
        return new Reply((flags & F_OPEN) != 0, new TrackerRules.Table(s, own == 0 ? null : kinds[own - 1], List.copyOf(rows)));
    }

    /** Sends a reply to one player. A stand-in of a self-test has no connection; a player who is leaving has nobody to tell. */
    static void send(ServerPlayer p, Reply reply) {
        if (p.connection == null) return;
        try {
            PacketDistributor.sendToPlayer(p, reply);
        } catch (RuntimeException leaving) {
            // nothing to tell a player who is going away
        }
    }

    /** Mod-bus listener: registers the two payloads. Both directions are optional, so a client without the mod can still join a server that has it. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(Request.TYPE, Request.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) WeaponTrackerService.onRequest(sp);
        }));
        r.playToClient(Reply.TYPE, Reply.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            Consumer<Reply> c = clientReceive;
            if (c != null) c.accept(payload);
        }));
    }
}
