package com.newuniverse.nusmp.elite.records;

import com.newuniverse.nusmp.client.elite.records.RecordsClientState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the wire format of the records system. Server to client: the DEFINITIONS the player may see (hidden records only once unlocked; sent in chunks, the first one replaces), the player's STATE
 * (unlocked and held with timestamps, the active title, the progress of every locked record and title requirement, the synergies in effect and their progress, the bonus lines in effect) and a TOAST for
 * one unlock. Client to server: ask for a fresh state ({@code Request}, sent when the screen opens and every couple of seconds while it is open) and choose the active title ({@code SetActive}). The server
 * never trusts the client: a request is rate-limited and a title choice is checked against the titles held. All text of a definition travels as the data file wrote it (English), already formatted by
 * {@link com.newuniverse.nusmp.elite.records.rule.Describe}. Every string is length-capped when read, so a malformed packet cannot allocate without bound. Payload ids {@code nusmp:records_*}.
 */
public final class RecordsNet {
    private RecordsNet() {}

    public static final int TEXT = 1024, MAX_LIST = 2048;

    // ------------------------------------------------------------------ buffer helpers
    private static void writeStrings(FriendlyByteBuf b, List<String> l) {
        b.writeVarInt(l.size());
        for (String s : l) b.writeUtf(s, TEXT);
    }

    private static List<String> readStrings(FriendlyByteBuf b) {
        int n = Math.max(0, Math.min(b.readVarInt(), MAX_LIST));
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(b.readUtf(TEXT));
        return out;
    }

    private static void writeTimes(FriendlyByteBuf b, Map<String, Long> m) {
        b.writeVarInt(m.size());
        for (var e : m.entrySet()) {
            b.writeUtf(e.getKey(), 128);
            b.writeVarLong(e.getValue());
        }
    }

    private static Map<String, Long> readTimes(FriendlyByteBuf b) {
        int n = Math.max(0, Math.min(b.readVarInt(), MAX_LIST * 4));
        Map<String, Long> out = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) out.put(b.readUtf(128), b.readVarLong());
        return out;
    }

    // ------------------------------------------------------------------ definitions
    /** A record the player may see. {@code category} and {@code rarity} are enum ordinals. */
    public record RecordInfo(String id, String name, String description, int category, int rarity, boolean hidden) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(id, 128);
            b.writeUtf(name, 128);
            b.writeUtf(description, TEXT);
            b.writeByte(category);
            b.writeByte(rarity);
            b.writeBoolean(hidden);
        }

        static RecordInfo read(FriendlyByteBuf b) { return new RecordInfo(b.readUtf(128), b.readUtf(128), b.readUtf(TEXT), b.readByte(), b.readByte(), b.readBoolean()); }
    }

    public record TitleInfo(String id, String name, String description, int rarity, List<String> tags, List<String> requirements, List<String> bonusLines) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(id, 128);
            b.writeUtf(name, 128);
            b.writeUtf(description, TEXT);
            b.writeByte(rarity);
            writeStrings(b, tags);
            writeStrings(b, requirements);
            writeStrings(b, bonusLines);
        }

        static TitleInfo read(FriendlyByteBuf b) {
            return new TitleInfo(b.readUtf(128), b.readUtf(128), b.readUtf(TEXT), b.readByte(), readStrings(b), readStrings(b), readStrings(b));
        }
    }

    public record SynergyInfo(String id, String name, String description, List<String> requires, String tag, int tagCount, List<String> bonusLines) {
        void write(FriendlyByteBuf b) {
            b.writeUtf(id, 128);
            b.writeUtf(name, 128);
            b.writeUtf(description, TEXT);
            writeStrings(b, requires);
            b.writeUtf(tag, 64);
            b.writeVarInt(tagCount);
            writeStrings(b, bonusLines);
        }

        static SynergyInfo read(FriendlyByteBuf b) {
            return new SynergyInfo(b.readUtf(128), b.readUtf(128), b.readUtf(TEXT), readStrings(b), b.readUtf(64), b.readVarInt(), readStrings(b));
        }
    }

    /** One chunk of definitions; {@code replace} marks the first chunk of a set (the client drops what it had). */
    public record Defs(boolean replace, List<RecordInfo> records, List<TitleInfo> titles, List<SynergyInfo> synergies) implements CustomPacketPayload {
        public static final Type<Defs> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "records_defs"));
        public static final StreamCodec<FriendlyByteBuf, Defs> STREAM_CODEC = StreamCodec.of(
                (b, p) -> {
                    b.writeBoolean(p.replace());
                    b.writeVarInt(p.records().size());
                    for (RecordInfo r : p.records()) r.write(b);
                    b.writeVarInt(p.titles().size());
                    for (TitleInfo t : p.titles()) t.write(b);
                    b.writeVarInt(p.synergies().size());
                    for (SynergyInfo s : p.synergies()) s.write(b);
                },
                b -> {
                    boolean replace = b.readBoolean();
                    List<RecordInfo> rs = new ArrayList<>();
                    for (int i = 0, n = Math.max(0, Math.min(b.readVarInt(), MAX_LIST)); i < n; i++) rs.add(RecordInfo.read(b));
                    List<TitleInfo> ts = new ArrayList<>();
                    for (int i = 0, n = Math.max(0, Math.min(b.readVarInt(), MAX_LIST)); i < n; i++) ts.add(TitleInfo.read(b));
                    List<SynergyInfo> ss = new ArrayList<>();
                    for (int i = 0, n = Math.max(0, Math.min(b.readVarInt(), MAX_LIST)); i < n; i++) ss.add(SynergyInfo.read(b));
                    return new Defs(replace, rs, ts, ss);
                });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ------------------------------------------------------------------ state
    /** Progress of one condition: state ordinal (0 met, 1 not met, 2 unavailable), current and target. */
    public record Prog(int state, double current, double target) {
        void write(FriendlyByteBuf b) {
            b.writeByte(state);
            b.writeDouble(current);
            b.writeDouble(target);
        }

        static Prog read(FriendlyByteBuf b) { return new Prog(b.readByte(), b.readDouble(), b.readDouble()); }

        public double fraction() {
            if (state == 0) return 1.0;
            if (state == 2 || target <= 0) return 0.0;
            return Math.max(0.0, Math.min(1.0, current / target));
        }
    }

    /** The player's whole state. Progress lists are parallel to the ids. */
    public record State(Map<String, Long> unlocked, Map<String, Long> held, String active, int points, int totalPoints, List<String> recordIds, List<Prog> recordProgress,
                        List<String> titleIds, List<List<Prog>> titleProgress, List<String> synergyIds, List<int[]> synergyProgress, List<String> bonusLines) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "records_state"));
        public static final StreamCodec<FriendlyByteBuf, State> STREAM_CODEC = StreamCodec.of(
                (b, p) -> {
                    writeTimes(b, p.unlocked());
                    writeTimes(b, p.held());
                    b.writeUtf(p.active(), 128);
                    b.writeVarInt(p.points());
                    b.writeVarInt(p.totalPoints());
                    writeStrings(b, p.recordIds());
                    for (Prog x : p.recordProgress()) x.write(b);
                    writeStrings(b, p.titleIds());
                    for (List<Prog> l : p.titleProgress()) {
                        b.writeVarInt(l.size());
                        for (Prog x : l) x.write(b);
                    }
                    writeStrings(b, p.synergyIds());
                    for (int[] x : p.synergyProgress()) {
                        b.writeVarInt(x[0]);
                        b.writeVarInt(x[1]);
                    }
                    writeStrings(b, p.bonusLines());
                },
                b -> {
                    Map<String, Long> unlocked = readTimes(b), held = readTimes(b);
                    String active = b.readUtf(128);
                    int points = b.readVarInt(), total = b.readVarInt();
                    List<String> rids = readStrings(b);
                    List<Prog> rp = new ArrayList<>();
                    for (int i = 0; i < rids.size(); i++) rp.add(Prog.read(b));
                    List<String> tids = readStrings(b);
                    List<List<Prog>> tp = new ArrayList<>();
                    for (int i = 0; i < tids.size(); i++) {
                        int n = Math.max(0, Math.min(b.readVarInt(), 64));
                        List<Prog> l = new ArrayList<>(n);
                        for (int j = 0; j < n; j++) l.add(Prog.read(b));
                        tp.add(l);
                    }
                    List<String> sids = readStrings(b);
                    List<int[]> sp = new ArrayList<>();
                    for (int i = 0; i < sids.size(); i++) sp.add(new int[]{b.readVarInt(), b.readVarInt()});
                    return new State(unlocked, held, active, points, total, rids, rp, tids, tp, sids, sp, readStrings(b));
                });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** A toast: {@code kind} 0 = record, 1 = title. */
    public record Toast(int kind, String id, String name, int rarity) implements CustomPacketPayload {
        public static final Type<Toast> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "records_toast"));
        public static final StreamCodec<FriendlyByteBuf, Toast> STREAM_CODEC = StreamCodec.of(
                (b, p) -> {
                    b.writeByte(p.kind());
                    b.writeUtf(p.id(), 128);
                    b.writeUtf(p.name(), 128);
                    b.writeByte(p.rarity());
                },
                b -> new Toast(b.readByte(), b.readUtf(128), b.readUtf(128), b.readByte()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ------------------------------------------------------------------ client to server
    public record Request() implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "records_request"));
        public static final StreamCodec<FriendlyByteBuf, Request> STREAM_CODEC = StreamCodec.of((b, p) -> { }, b -> new Request());

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Choose the active title ("" = none). */
    public record SetActive(String titleId) implements CustomPacketPayload {
        public static final Type<SetActive> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "records_set_active"));
        public static final StreamCodec<FriendlyByteBuf, SetActive> STREAM_CODEC = StreamCodec.of((b, p) -> b.writeUtf(p.titleId(), 128), b -> new SetActive(b.readUtf(128)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener. The client handlers only touch the side-neutral {@link RecordsClientState}; the server handlers go through {@link RecordsService}. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Defs.TYPE, Defs.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> RecordsClientState.receiveDefs(p)));
        r.playToClient(State.TYPE, State.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> RecordsClientState.receiveState(p)));
        r.playToClient(Toast.TYPE, Toast.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> RecordsClientState.receiveToast(p)));
        r.playToServer(Request.TYPE, Request.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) RecordsService.onRequest(sp);
        }));
        r.playToServer(SetActive.TYPE, SetActive.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) RecordsService.onSetActive(sp, p.titleId());
        }));
    }
}
