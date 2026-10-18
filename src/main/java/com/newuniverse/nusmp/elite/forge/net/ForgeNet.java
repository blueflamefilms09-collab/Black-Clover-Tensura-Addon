package com.newuniverse.nusmp.elite.forge.net;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the forge talks to its screens in a handful of small messages and no more. Server to client: {@link OpenStation} (the station screen: the recipes with what
 * the player has of each ingredient, and their progress), {@link Start} (a session: the minigame steps and one seed per step, so the client can draw exactly the
 * game the server will re-score), {@link Done} (the outcome) and {@link Sync} (the progress, on login and after a change). Client to server: {@link Begin}
 * (start this recipe at this station), {@link Result} (what was PLAYED per step, never a score), {@link Abort}, {@link Spec} (choose a specialization) and
 * {@link Engrave} (engrave the item in the main hand). Every list read from the network is bounded, so a malformed or hostile packet is refused by the codec
 * instead of exhausting memory. All ids are {@code nusmp:elite_forge_*}. Registration is optional in both directions, so a client without the mod can still join.
 */
public final class ForgeNet {
    private ForgeNet() {}

    static final int MAX_RECIPES = 256, MAX_ROWS = 16, MAX_STEPS = 8, MAX_INPUT = 2048, MAX_TEXT = 256;

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", "elite_forge_" + path); }

    // ================================================================ shared pieces
    /** An item, how many are needed and how many the player has. */
    public record StackView(String item, int count, int have) {}

    /** A minigame step: the type id and the difficulty. */
    public record StepView(String type, int difficulty) {}

    /** One recipe as the screen shows it. */
    public record RecipeView(String id, int tier, String category, String outItem, int outCount, List<StackView> inputs, List<StackView> premium, List<Integer> premiumBonus,
                             List<StepView> steps, int baseQuality, boolean engrave, boolean unlocked, boolean available, boolean known) {}

    /** The player's progress as the screen shows it. */
    public record Progress(int crafted, int best, int topStation, String spec, boolean changeCosts, int changeLevels, int engraveLevels, int skillPoints, List<Integer> byTier) {
        public static Progress empty() { return new Progress(0, 0, 0, "", false, 0, 0, 0, List.of(0, 0, 0, 0)); }
    }

    static void writeProgress(FriendlyByteBuf b, Progress p) {
        b.writeVarInt(p.crafted());
        b.writeVarInt(p.best());
        b.writeVarInt(p.topStation());
        b.writeUtf(p.spec(), MAX_TEXT);
        b.writeBoolean(p.changeCosts());
        b.writeVarInt(p.changeLevels());
        b.writeVarInt(p.engraveLevels());
        b.writeVarInt(p.skillPoints());
        b.writeVarInt(p.byTier().size());
        for (int v : p.byTier()) b.writeVarInt(v);
    }

    static Progress readProgress(FriendlyByteBuf b) {
        int crafted = b.readVarInt(), best = b.readVarInt(), top = b.readVarInt();
        String spec = b.readUtf(MAX_TEXT);
        boolean costs = b.readBoolean();
        int cl = b.readVarInt(), el = b.readVarInt(), sp = b.readVarInt();
        int n = bounded(b.readVarInt(), 8);
        List<Integer> by = new ArrayList<>(n);
        for (int i = 0; i < n; i++) by.add(b.readVarInt());
        return new Progress(crafted, best, top, spec, costs, cl, el, sp, List.copyOf(by));
    }

    static int bounded(int n, int max) {
        if (n < 0 || n > max) throw new IllegalStateException("forge packet: a list of " + n + " entries is not valid (max " + max + ")");
        return n;
    }

    static void writeStacks(FriendlyByteBuf b, List<StackView> l) {
        b.writeVarInt(l.size());
        for (StackView s : l) {
            b.writeUtf(s.item(), MAX_TEXT);
            b.writeVarInt(s.count());
            b.writeVarInt(s.have());
        }
    }

    static List<StackView> readStacks(FriendlyByteBuf b) {
        int n = bounded(b.readVarInt(), MAX_ROWS);
        List<StackView> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(new StackView(b.readUtf(MAX_TEXT), b.readVarInt(), b.readVarInt()));
        return List.copyOf(out);
    }

    static void writeSteps(FriendlyByteBuf b, List<StepView> l) {
        b.writeVarInt(l.size());
        for (StepView s : l) {
            b.writeUtf(s.type(), MAX_TEXT);
            b.writeVarInt(s.difficulty());
        }
    }

    static List<StepView> readSteps(FriendlyByteBuf b) {
        int n = bounded(b.readVarInt(), MAX_STEPS);
        List<StepView> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(new StepView(b.readUtf(MAX_TEXT), b.readVarInt()));
        return List.copyOf(out);
    }

    // ================================================================ server to client
    /** Open the station screen ({@code refresh} true: only update a screen that is already open). */
    public record OpenStation(BlockPos pos, int tier, int forgedHere, boolean refresh, Progress progress, List<RecipeView> recipes) implements CustomPacketPayload {
        public static final Type<OpenStation> TYPE = new Type<>(id("open"));
        public static final StreamCodec<FriendlyByteBuf, OpenStation> STREAM_CODEC = StreamCodec.of(OpenStation::write, OpenStation::read);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, OpenStation p) {
            b.writeBlockPos(p.pos());
            b.writeVarInt(p.tier());
            b.writeVarInt(p.forgedHere());
            b.writeBoolean(p.refresh());
            writeProgress(b, p.progress());
            b.writeVarInt(p.recipes().size());
            for (RecipeView r : p.recipes()) {
                b.writeUtf(r.id(), MAX_TEXT);
                b.writeVarInt(r.tier());
                b.writeUtf(r.category(), MAX_TEXT);
                b.writeUtf(r.outItem(), MAX_TEXT);
                b.writeVarInt(r.outCount());
                writeStacks(b, r.inputs());
                writeStacks(b, r.premium());
                b.writeVarInt(r.premiumBonus().size());
                for (int v : r.premiumBonus()) b.writeVarInt(v);
                writeSteps(b, r.steps());
                b.writeVarInt(r.baseQuality());
                b.writeBoolean(r.engrave());
                b.writeBoolean(r.unlocked());
                b.writeBoolean(r.available());
                b.writeBoolean(r.known());
            }
        }

        static OpenStation read(FriendlyByteBuf b) {
            BlockPos pos = b.readBlockPos();
            int tier = b.readVarInt(), forged = b.readVarInt();
            boolean refresh = b.readBoolean();
            Progress pr = readProgress(b);
            int n = bounded(b.readVarInt(), MAX_RECIPES);
            List<RecipeView> out = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                String id = b.readUtf(MAX_TEXT);
                int t = b.readVarInt();
                String cat = b.readUtf(MAX_TEXT), outItem = b.readUtf(MAX_TEXT);
                int outCount = b.readVarInt();
                List<StackView> in = readStacks(b), prem = readStacks(b);
                int pn = bounded(b.readVarInt(), MAX_ROWS);
                List<Integer> bonus = new ArrayList<>(pn);
                for (int k = 0; k < pn; k++) bonus.add(b.readVarInt());
                List<StepView> steps = readSteps(b);
                int base = b.readVarInt();
                boolean engrave = b.readBoolean(), unlocked = b.readBoolean(), available = b.readBoolean(), known = b.readBoolean();
                out.add(new RecipeView(id, t, cat, outItem, outCount, in, prem, List.copyOf(bonus), steps, base, engrave, unlocked, available, known));
            }
            return new OpenStation(pos, tier, forged, refresh, pr, List.copyOf(out));
        }
    }

    /** A session was opened: play these steps with these seeds, then report with {@link Result}. */
    public record Start(int session, BlockPos pos, String recipe, String outItem, int tier, List<StepView> steps, long[] seeds) implements CustomPacketPayload {
        public static final Type<Start> TYPE = new Type<>(id("start"));
        public static final StreamCodec<FriendlyByteBuf, Start> STREAM_CODEC = StreamCodec.of(Start::write, Start::read);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, Start p) {
            b.writeVarInt(p.session());
            b.writeBlockPos(p.pos());
            b.writeUtf(p.recipe(), MAX_TEXT);
            b.writeUtf(p.outItem(), MAX_TEXT);
            b.writeVarInt(p.tier());
            writeSteps(b, p.steps());
            b.writeVarInt(p.seeds().length);
            for (long s : p.seeds()) b.writeLong(s);
        }

        static Start read(FriendlyByteBuf b) {
            int session = b.readVarInt();
            BlockPos pos = b.readBlockPos();
            String recipe = b.readUtf(MAX_TEXT), outItem = b.readUtf(MAX_TEXT);
            int tier = b.readVarInt();
            List<StepView> steps = readSteps(b);
            int n = bounded(b.readVarInt(), MAX_STEPS);
            long[] seeds = new long[n];
            for (int i = 0; i < n; i++) seeds[i] = b.readLong();
            return new Start(session, pos, recipe, outItem, tier, steps, seeds);
        }
    }

    /** The outcome of a session. {@code message} is a translation key (or plain text); {@code points10} is the roll total times ten. */
    public record Done(boolean ok, String message, int quality, int score, String outItem, int outCount, int points10, boolean capped, List<String> engravings) implements CustomPacketPayload {
        public static final Type<Done> TYPE = new Type<>(id("done"));
        public static final StreamCodec<FriendlyByteBuf, Done> STREAM_CODEC = StreamCodec.of(Done::write, Done::read);

        public static Done fail(String message) { return new Done(false, message, 0, 0, "", 0, 0, false, List.of()); }

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, Done p) {
            b.writeBoolean(p.ok());
            b.writeUtf(p.message(), MAX_TEXT);
            b.writeVarInt(p.quality());
            b.writeVarInt(p.score());
            b.writeUtf(p.outItem(), MAX_TEXT);
            b.writeVarInt(p.outCount());
            b.writeVarInt(p.points10());
            b.writeBoolean(p.capped());
            b.writeVarInt(p.engravings().size());
            for (String s : p.engravings()) b.writeUtf(s, MAX_TEXT);
        }

        static Done read(FriendlyByteBuf b) {
            boolean ok = b.readBoolean();
            String msg = b.readUtf(MAX_TEXT);
            int q = b.readVarInt(), score = b.readVarInt();
            String out = b.readUtf(MAX_TEXT);
            int cnt = b.readVarInt(), pts = b.readVarInt();
            boolean capped = b.readBoolean();
            int n = bounded(b.readVarInt(), MAX_ROWS);
            List<String> eng = new ArrayList<>(n);
            for (int i = 0; i < n; i++) eng.add(b.readUtf(MAX_TEXT));
            return new Done(ok, msg, q, score, out, cnt, pts, capped, List.copyOf(eng));
        }
    }

    /** The progress changed (login, after a forge, a specialization). */
    public record Sync(Progress progress) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(id("sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of((b, p) -> writeProgress(b, p.progress()), b -> new Sync(readProgress(b)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ================================================================ client to server
    /** Start this recipe at the station at {@code pos}; {@code premium} says the optional materials may be used. */
    public record Begin(BlockPos pos, String recipe, boolean premium) implements CustomPacketPayload {
        public static final Type<Begin> TYPE = new Type<>(id("begin"));
        public static final StreamCodec<FriendlyByteBuf, Begin> STREAM_CODEC = StreamCodec.of(
                (b, p) -> {
                    b.writeBlockPos(p.pos());
                    b.writeUtf(p.recipe(), MAX_TEXT);
                    b.writeBoolean(p.premium());
                },
                b -> new Begin(b.readBlockPos(), b.readUtf(MAX_TEXT), b.readBoolean()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** What was played, one int list per step (the format is each minigame's own). */
    public record Result(int session, List<int[]> inputs) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(id("result"));
        public static final StreamCodec<FriendlyByteBuf, Result> STREAM_CODEC = StreamCodec.of(
                (b, p) -> {
                    b.writeVarInt(p.session());
                    b.writeVarInt(p.inputs().size());
                    for (int[] in : p.inputs()) b.writeVarIntArray(in);
                },
                b -> {
                    int session = b.readVarInt();
                    int n = bounded(b.readVarInt(), MAX_STEPS);
                    List<int[]> in = new ArrayList<>(n);
                    for (int i = 0; i < n; i++) in.add(b.readVarIntArray(MAX_INPUT));
                    return new Result(session, in);
                });

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Give the session up (the screen was closed). */
    public record Abort(int session) implements CustomPacketPayload {
        public static final Type<Abort> TYPE = new Type<>(id("abort"));
        public static final StreamCodec<FriendlyByteBuf, Abort> STREAM_CODEC = StreamCodec.of((b, p) -> b.writeVarInt(p.session()), b -> new Abort(b.readVarInt()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Choose (or change to) a specialization at the station at {@code pos}. */
    public record Spec(BlockPos pos, String spec) implements CustomPacketPayload {
        public static final Type<Spec> TYPE = new Type<>(id("spec"));
        public static final StreamCodec<FriendlyByteBuf, Spec> STREAM_CODEC = StreamCodec.of(
                (b, p) -> {
                    b.writeBlockPos(p.pos());
                    b.writeUtf(p.spec(), MAX_TEXT);
                },
                b -> new Spec(b.readBlockPos(), b.readUtf(MAX_TEXT)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Engrave the forged item in the main hand at the station at {@code pos}. */
    public record Engrave(BlockPos pos) implements CustomPacketPayload {
        public static final Type<Engrave> TYPE = new Type<>(id("engrave"));
        public static final StreamCodec<FriendlyByteBuf, Engrave> STREAM_CODEC = StreamCodec.of((b, p) -> b.writeBlockPos(p.pos()), b -> new Engrave(b.readBlockPos()));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ================================================================ registration
    /** Mod-bus listener: registers the nine payloads. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(OpenStation.TYPE, OpenStation.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ForgeClientBridge.open.accept(p)));
        r.playToClient(Start.TYPE, Start.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ForgeClientBridge.start.accept(p)));
        r.playToClient(Done.TYPE, Done.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ForgeClientBridge.done.accept(p)));
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> ForgeClientBridge.sync.accept(p)));
        r.playToServer(Begin.TYPE, Begin.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) ForgeServer.onBegin(sp, p);
        }));
        r.playToServer(Result.TYPE, Result.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) ForgeServer.onResult(sp, p);
        }));
        r.playToServer(Abort.TYPE, Abort.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) ForgeServer.onAbort(sp, p);
        }));
        r.playToServer(Spec.TYPE, Spec.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) ForgeServer.onSpec(sp, p);
        }));
        r.playToServer(Engrave.TYPE, Engrave.STREAM_CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) ForgeServer.onEngrave(sp, p);
        }));
    }
}
