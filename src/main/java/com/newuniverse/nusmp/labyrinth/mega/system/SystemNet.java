package com.newuniverse.nusmp.labyrinth.mega.system;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the holographic window is drawn on the CLIENT from facts only the SERVER knows (which floor the player stands on, which road, the quest and its
 * progress, the stat points), so there are two server-to-client payloads, both under the ids the contract fixed. {@link Sync} is the whole picture in one
 * small packet (sent when something changes, at most once per ten ticks, and on login, respawn and dimension change), {@link Toast} is one pop-up line (a
 * lang key, its arguments and a kind). Neither carries translated text: they carry KEYS and numbers, so each player reads them in their own language, and the
 * English fallback of a datapack quest travels with the sync because such a quest has no lang entry. Both are optional channels (a client without the mod simply
 * gets nothing), and both handlers run on the client thread and call into the client package by name only inside the lambda, so a dedicated server never loads
 * a client class. The wire format is written by hand with bounded strings, so a hostile or damaged packet cannot make a client allocate without limit.
 */
public final class SystemNet {
    private SystemNet() {}

    /** Quest state codes on the wire. */
    public static final int Q_NONE = 0, Q_ACTIVE = 1, Q_DONE = 2, Q_FAILED = 3;

    /**
     * The whole state of the window.
     *
     * @param active        the window should be shown (the System is on, the player is on a floor of a road it covers)
     * @param floor         the signed floor (0 = none)
     * @param polarity      {@code holy}, {@code abyssal} or ""
     * @param threat        the floor's difficulty number
     * @param boss          the floor is a boss floor (no quest there)
     * @param questState    {@link #Q_NONE} .. {@link #Q_FAILED}
     * @param kind          the quest's kind id ({@code kill_n} ...) or ""
     * @param titleKey      lang key of the quest title (its format takes the target as %1$s)
     * @param titleFallback the English title, the target already put in
     * @param progress      count so far
     * @param target        count wanted (seconds for a timed quest)
     * @param timeLeft      ticks left on the clock, -1 when there is none
     * @param clockRunning  the clock is running now (the player is on the quest's floor)
     * @param stats         the points of STR AGI VIT INT SENSE
     */
    public record Sync(boolean active, int floor, String polarity, int threat, boolean boss, int questState, String kind, String titleKey,
                       String titleFallback, int progress, int target, int timeLeft, boolean clockRunning, List<Integer> stats) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "system_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> CODEC = StreamCodec.of(Sync::write, Sync::read);

        /** A packet that hides the window. */
        public static Sync hidden() {
            List<Integer> zero = new ArrayList<>();
            for (int i = 0; i < Stat.COUNT; i++) zero.add(0);
            return new Sync(false, 0, "", 0, false, Q_NONE, "", "", "", 0, 0, -1, false, zero);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, Sync p) {
            b.writeBoolean(p.active());
            b.writeInt(p.floor());
            b.writeUtf(p.polarity(), 16);
            b.writeVarInt(p.threat());
            b.writeBoolean(p.boss());
            b.writeByte(p.questState());
            b.writeUtf(p.kind(), 32);
            b.writeUtf(p.titleKey(), 160);
            b.writeUtf(p.titleFallback(), 256);
            b.writeVarInt(p.progress());
            b.writeVarInt(p.target());
            b.writeInt(p.timeLeft());
            b.writeBoolean(p.clockRunning());
            int n = Math.min(p.stats().size(), Stat.COUNT);
            b.writeByte(n);
            for (int i = 0; i < n; i++) b.writeVarInt(p.stats().get(i));
        }

        static Sync read(FriendlyByteBuf b) {
            boolean active = b.readBoolean();
            int floor = b.readInt();
            String polarity = b.readUtf(16);
            int threat = b.readVarInt();
            boolean boss = b.readBoolean();
            int state = b.readByte();
            String kind = b.readUtf(32);
            String key = b.readUtf(160);
            String fallback = b.readUtf(256);
            int progress = b.readVarInt();
            int target = b.readVarInt();
            int timeLeft = b.readInt();
            boolean clock = b.readBoolean();
            int n = Math.max(0, Math.min(b.readByte(), Stat.COUNT));
            List<Integer> stats = new ArrayList<>(Stat.COUNT);
            for (int i = 0; i < n; i++) stats.add(b.readVarInt());
            while (stats.size() < Stat.COUNT) stats.add(0);
            return new Sync(active, floor, polarity, threat, boss, state, kind, key, fallback, progress, target, timeLeft, clock, stats);
        }
    }

    /**
     * One pop-up line.
     *
     * @param kind the colour and marker, as a {@link ToastKind#code()}
     * @param key  lang key of the message
     * @param args its arguments; an argument that starts with {@code @} is itself a lang key that the client translates (an entity name), anything else is shown as it is
     */
    public record Toast(int kind, String key, List<String> args) implements CustomPacketPayload {
        public static final Type<Toast> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "system_toast"));
        public static final StreamCodec<FriendlyByteBuf, Toast> CODEC = StreamCodec.of(Toast::write, Toast::read);
        /** The most arguments a toast carries. */
        public static final int MAX_ARGS = 4;

        public static Toast of(ToastKind kind, String key, String... args) {
            List<String> list = new ArrayList<>();
            for (String a : args) if (list.size() < MAX_ARGS) list.add(a == null ? "" : a);
            return new Toast(kind.code(), key, list);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, Toast p) {
            b.writeByte(p.kind());
            b.writeUtf(p.key(), 160);
            int n = Math.min(p.args().size(), MAX_ARGS);
            b.writeByte(n);
            for (int i = 0; i < n; i++) b.writeUtf(p.args().get(i), 96);
        }

        static Toast read(FriendlyByteBuf b) {
            int kind = b.readByte();
            String key = b.readUtf(160);
            int n = Math.max(0, Math.min(b.readByte(), MAX_ARGS));
            List<String> args = new ArrayList<>(n);
            for (int i = 0; i < n; i++) args.add(b.readUtf(96));
            return new Toast(kind, key, args);
        }
    }

    /** Mod bus: registers both payloads (optional channels). The handlers are client-only code, reached by name inside the lambda. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Sync.TYPE, Sync.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.mega.system.SystemClient.receive(p)));
        r.playToClient(Toast.TYPE, Toast.CODEC, (p, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.mega.system.SystemClient.receiveToast(p)));
    }
}
