package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.client.heroweapon.forms.FormsClient;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the Weapon Book is a screen on the client but every fact in it, and every action it offers, belongs to the server. Four small packets carry the
 * conversation, and nothing the client says is trusted: it asks to open the book ({@link Open}), the server answers with the forms of the bonded weapon
 * ({@link Defs}, only when the client's copy is not current) and the state of the bond ({@link State}), keeps the open book fresh by sending {@code State} again when
 * something changes, and the client's clicks come back as {@link Act} and are checked again by the server like any other request. All four are registered
 * {@code optional} on channel version 1 like the rest of the mod's packets, so a client without the mod's forms system can still join. Register {@link #registerPayloads}
 * on the mod bus (done by {@link FormsSystem#init}).
 *
 * <p>Ids: {@code nusmp:hero_forms_open} and {@code nusmp:hero_forms_act} (client to server), {@code nusmp:hero_forms_defs} and {@code nusmp:hero_forms_state}
 * (server to client).
 */
public final class FormsNet {
    private FormsNet() {}

    /** What an {@link Act} asks for. */
    public static final int ACT_USE = 0, ACT_LEARN = 1, ACT_FEED = 2, ACT_EMBRACE = 3, ACT_CLOSE = 4;

    private static final int ID = 256, LIST = 256, TEXT = 1024;

    // ------------------------------------------------------------------------------------------------ the packets
    /** Client to server: open the Weapon Book. Carries the weapon and checksum of the client's copy of the forms ("" and 0 for none), so the server can skip sending them. */
    public record Open(String haveKind, int haveSum) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_forms_open"));
        public static final StreamCodec<FriendlyByteBuf, Open> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.haveKind(), ID);
                    buf.writeInt(p.haveSum());
                },
                buf -> new Open(buf.readUtf(ID), buf.readInt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Client to server: a click in the book. {@code arg} is a form id (use, learn) or an item id (feed); {@code whole} asks for the whole stack (feed). */
    public record Act(int action, String arg, boolean whole) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_forms_act"));
        public static final StreamCodec<FriendlyByteBuf, Act> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeByte(p.action());
                    buf.writeUtf(p.arg(), ID);
                    buf.writeBoolean(p.whole());
                },
                buf -> new Act(buf.readByte(), buf.readUtf(ID), buf.readBoolean()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server to client: the forms of the weapon the player is bonded to, with their checksum. */
    public record Defs(HeroKind kind, int sum, List<HeroForm> forms) implements CustomPacketPayload {
        public static final Type<Defs> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_forms_defs"));
        public static final StreamCodec<FriendlyByteBuf, Defs> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.kind().itemId(), ID);
                    buf.writeInt(p.sum());
                    FormsWire.writeForms(new BufOut(buf), p.forms());
                },
                buf -> {
                    HeroKind kind = HeroKind.byItemId(buf.readUtf(ID));
                    if (kind == null) throw new IllegalArgumentException("unknown hero weapon");
                    int sum = buf.readInt();
                    return new Defs(kind, sum, FormsWire.readForms(new BufIn(buf), kind));
                });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /**
     * Server to client: the state of the bond that the book shows. {@code open} is true only in the answer to a request to open the book (the client then opens the
     * screen); later pushes have it false and only refresh a book that is already open. An empty {@code kind} means the player has no hero weapon.
     *
     * @param kind         the weapon's item id, or ""
     * @param form         the active form id
     * @param exp          the weapon experience (the level is derived from it)
     * @param summoned     the weapon is in the hand
     * @param locked       the curse holds the wielder (no change of form)
     * @param curseActive  a curse form is active
     * @param despair      0 to 1000, from the curse system
     * @param corruption   0 to 1000, from the curse system
     * @param offered      the sin the weapon is offering to the wielder ("whispering"), or ""
     * @param learned      the ids of the forms learned
     * @param mastery      form id to mastery points
     * @param bonuses      the forms whose permanent bonus was given (mastered)
     * @param absorbed     item id to count absorbed
     * @param series       the unlocked Curse Series (sin ids)
     */
    public record State(boolean open, String kind, String form, long exp, boolean summoned, boolean locked, boolean curseActive, int despair, int corruption, String offered,
                        List<String> learned, Map<String, Integer> mastery, List<String> bonuses, Map<String, Integer> absorbed, List<String> series) implements CustomPacketPayload {
        public static final Type<State> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_forms_state"));
        public static final StreamCodec<FriendlyByteBuf, State> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBoolean(p.open());
                    buf.writeUtf(p.kind(), ID);
                    buf.writeUtf(p.form(), ID);
                    buf.writeVarLong(p.exp());
                    buf.writeByte((p.summoned() ? 1 : 0) | (p.locked() ? 2 : 0) | (p.curseActive() ? 4 : 0));
                    buf.writeVarInt(p.despair());
                    buf.writeVarInt(p.corruption());
                    buf.writeUtf(p.offered(), ID);
                    writeStrings(buf, p.learned());
                    writeCounts(buf, p.mastery());
                    writeStrings(buf, p.bonuses());
                    writeCounts(buf, p.absorbed());
                    writeStrings(buf, p.series());
                },
                buf -> {
                    boolean open = buf.readBoolean();
                    String kind = buf.readUtf(ID), form = buf.readUtf(ID);
                    long exp = buf.readVarLong();
                    int flags = buf.readByte();
                    int despair = buf.readVarInt(), corruption = buf.readVarInt();
                    String offered = buf.readUtf(ID);
                    List<String> learned = readStrings(buf);
                    Map<String, Integer> mastery = readCounts(buf);
                    List<String> bonuses = readStrings(buf);
                    Map<String, Integer> absorbed = readCounts(buf);
                    List<String> series = readStrings(buf);
                    return new State(open, kind, form, exp, (flags & 1) != 0, (flags & 2) != 0, (flags & 4) != 0, despair, corruption, offered, learned, mastery, bonuses, absorbed, series);
                });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        /** True when the player has a hero weapon. */
        public boolean bound() { return !kind.isEmpty(); }

        /** The weapon, or null. */
        public HeroKind weapon() { return HeroKind.byItemId(kind); }
    }

    /** Client side: the request to open the book, telling the server which forms this client already holds so it need not send them again. */
    public static Open openRequest() {
        HeroKind have = FormsStore.anySynced();
        return have == null ? new Open("", 0) : new Open(have.itemId(), FormsStore.checksum(have));
    }

    // ------------------------------------------------------------------------------------------------ registration
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Defs.TYPE, Defs.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> FormsStore.setSynced(payload.kind(), payload.forms())));
        r.playToClient(State.TYPE, State.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> FormsClient.onState(payload)));
        r.playToServer(Open.TYPE, Open.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) FormsSync.open(sp, payload.haveKind(), payload.haveSum());
        }));
        r.playToServer(Act.TYPE, Act.CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) FormsSync.act(sp, payload);
        }));
    }

    // ------------------------------------------------------------------------------------------------ codec helpers
    private static void writeStrings(FriendlyByteBuf buf, List<String> l) {
        int n = Math.min(l.size(), LIST);
        buf.writeVarInt(n);
        for (int i = 0; i < n; i++) buf.writeUtf(l.get(i), ID);
    }

    private static List<String> readStrings(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > LIST) throw new IllegalArgumentException("list size " + n);
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(buf.readUtf(ID));
        return Collections.unmodifiableList(out);
    }

    private static void writeCounts(FriendlyByteBuf buf, Map<String, Integer> m) {
        int n = Math.min(m.size(), LIST);
        buf.writeVarInt(n);
        int i = 0;
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            if (i++ >= n) break;
            buf.writeUtf(e.getKey(), ID);
            buf.writeVarInt(e.getValue());
        }
    }

    private static Map<String, Integer> readCounts(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > LIST) throw new IllegalArgumentException("map size " + n);
        Map<String, Integer> out = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) out.put(buf.readUtf(ID), buf.readVarInt());
        return Collections.unmodifiableMap(out);
    }

    /** The writer of {@link FormsWire} over the game's buffer. */
    private record BufOut(FriendlyByteBuf buf) implements FormsWire.Out {
        @Override public void utf(String s) { buf.writeUtf(s, TEXT); }

        @Override public void varInt(int v) { buf.writeVarInt(v); }

        @Override public void dbl(double d) { buf.writeDouble(d); }

        @Override public void bool(boolean b) { buf.writeBoolean(b); }
    }

    /** The reader of {@link FormsWire} over the game's buffer. */
    private record BufIn(FriendlyByteBuf buf) implements FormsWire.In {
        @Override public String utf() { return buf.readUtf(TEXT); }

        @Override public int varInt() { return buf.readVarInt(); }

        @Override public double dbl() { return buf.readDouble(); }

        @Override public boolean bool() { return buf.readBoolean(); }
    }
}
