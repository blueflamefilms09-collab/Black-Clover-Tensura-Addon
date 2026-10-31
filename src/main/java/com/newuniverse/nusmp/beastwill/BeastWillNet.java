package com.newuniverse.nusmp.beastwill;

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
 * Why: the Beast Will system talks over four tiny packets (3.45.0) and the server decides everything. UP: {@link Action}, one number for what the player asked (switch a form, use an ability, swap the
 * will, say yes or no to the absorption); it is only an ask: the sender must be a real player with a will, the numbers are range-checked, and asks are rate limited ({@link #MIN_GAP_TICKS}). DOWN, only to
 * that player: {@link Sync} (the HUD state: at most four a second, only when something changed), {@link Info} (the active will's static facts, when it changes) and {@link Preview} (the absorption ask). The client side is hooked in by {@link #clientSync}, {@link #clientInfo} and
 * {@link #clientPreview} (set by the client class), so this common file names no client class. Both directions are optional: a client without the mod simply never gets them.
 */
public final class BeastWillNet {
    private BeastWillNet() {}

    public static final int OP_FORM = 1, OP_ABILITY = 2, OP_SWAP = 3, OP_CONFIRM = 4, OP_DECLINE = 5;
    public static final int MIN_GAP_TICKS = 4;
    public static final int NAME_MAX = 40;

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<Sync> clientSync;
    public static volatile Consumer<Preview> clientPreview;
    public static volatile Consumer<Info> clientInfo;

    // ---------------------------------------------------------------- client to server
    /** What the player asked: {@code op} (see OP_*) and one argument (the form 1 or 2, the ability slot 1 or 2, the swap step). */
    public record Action(int op, int arg) implements CustomPacketPayload {
        public static final Type<Action> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "beastwill_action"));
        public static final StreamCodec<FriendlyByteBuf, Action> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeByte(p.op());
            buf.writeByte(p.arg());
        }, buf -> new Action(buf.readByte(), buf.readByte()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------------------------------------------------------------- server to client
    /** One bound will in the HUD list: the id and the two progress shares (0..100). */
    public record Entry(String id, int assimilation, int mastery) {}

    /** The HUD state (everything 0..100 or small): the active will, the form, the strain, the progress, the locks and cooldowns in whole seconds (the client counts them down between packets), the flags, the fit with the player's race in percent and the bound wills. */
    public record Sync(String active, int phase, int strain, int assimilation, int mastery, int lockSec, int swapSec, int cd1Sec, int cd2Sec, int flags, int compat, List<Entry> wills)
            implements CustomPacketPayload {
        public static final int F_MEDITATING = 1, F_VEILED = 2, F_REDUCED = 4, F_CONFLICT = 8;
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "beastwill_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeUtf(p.active(), NAME_MAX);
            buf.writeByte(p.phase());
            buf.writeByte(p.strain());
            buf.writeByte(p.assimilation());
            buf.writeByte(p.mastery());
            buf.writeVarInt(p.lockSec());
            buf.writeVarInt(p.swapSec());
            buf.writeVarInt(p.cd1Sec());
            buf.writeVarInt(p.cd2Sec());
            buf.writeByte(p.flags());
            buf.writeByte(p.compat());
            buf.writeByte(p.wills().size());
            for (Entry e : p.wills()) {
                buf.writeUtf(e.id(), NAME_MAX);
                buf.writeByte(e.assimilation());
                buf.writeByte(e.mastery());
            }
        }, buf -> {
            String active = buf.readUtf(NAME_MAX);
            int phase = buf.readByte(), strain = buf.readByte(), assim = buf.readByte(), mastery = buf.readByte();
            int lock = buf.readVarInt(), swap = buf.readVarInt(), c1 = buf.readVarInt(), c2 = buf.readVarInt(), flags = buf.readByte(), compat = buf.readByte();
            int n = Math.min(buf.readByte(), BeastWillState.MAX_WILLS);
            List<Entry> list = new ArrayList<>();
            for (int i = 0; i < n; i++) list.add(new Entry(buf.readUtf(NAME_MAX), buf.readByte(), buf.readByte()));
            return new Sync(active, phase, strain, assim, mastery, lock, swap, c1, c2, flags, compat, list);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** One ability of the active will as the HUD lists it: the phase (1 Acquire, 2 Integrate), the slot, the id (the lang key {@code nusmp.beastwill.ability.<id>}), the cooldown in ticks and the cost and strain. */
    public record AbilityInfo(int phase, int slot, String id, int cooldownTicks, int costPct, int strain) {}

    /**
     * What the client can not know because the data files live on the server: the active will's beast, affinity, lowest rank, tint, the four unlock shares (percent) and drains (tenths of a percent per
     * second) and its abilities. Sent when the active will changes (and after a data reload), never per tick. The numbers are display only; the server decides.
     */
    public record Info(String will, String beast, String affinity, int rank, int tint, boolean legendary, int acqAssim, int acqMastery, int intAssim, int intMastery, int acqDrain, int intDrain, List<AbilityInfo> abilities)
            implements CustomPacketPayload {
        public static final Type<Info> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "beastwill_info"));
        public static final StreamCodec<FriendlyByteBuf, Info> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeUtf(p.will(), NAME_MAX);
            buf.writeUtf(p.beast(), 64);
            buf.writeUtf(p.affinity(), 24);
            buf.writeByte(p.rank());
            buf.writeInt(p.tint());
            buf.writeBoolean(p.legendary());
            buf.writeByte(p.acqAssim());
            buf.writeByte(p.acqMastery());
            buf.writeByte(p.intAssim());
            buf.writeByte(p.intMastery());
            buf.writeShort(p.acqDrain());
            buf.writeShort(p.intDrain());
            buf.writeByte(p.abilities().size());
            for (AbilityInfo a : p.abilities()) {
                buf.writeByte(a.phase());
                buf.writeByte(a.slot());
                buf.writeUtf(a.id(), NAME_MAX);
                buf.writeVarInt(a.cooldownTicks());
                buf.writeByte(a.costPct());
                buf.writeByte(a.strain());
            }
        }, buf -> {
            String will = buf.readUtf(NAME_MAX), beast = buf.readUtf(64), affinity = buf.readUtf(24);
            int rank = buf.readByte(), tint = buf.readInt();
            boolean legendary = buf.readBoolean();
            int aa = buf.readByte(), am = buf.readByte(), ia = buf.readByte(), im = buf.readByte(), ad = buf.readShort(), idr = buf.readShort();
            int n = Math.min(buf.readByte(), 8);
            List<AbilityInfo> list = new ArrayList<>();
            for (int i = 0; i < n; i++) list.add(new AbilityInfo(buf.readByte(), buf.readByte(), buf.readUtf(NAME_MAX), buf.readVarInt(), buf.readByte(), buf.readByte()));
            return new Info(will, beast, affinity, rank, tint, legendary, aa, am, ia, im, ad, idr, list);
        });
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The absorption ask: which will, which beast, its affinity, the rank's ordinal, the source (core or offer), whether the player may take it now, the reason if not, the fit in percent and the EP needed. */
    public record Preview(String will, String beast, String affinity, int rank, int source, boolean ok, String reason, int fitPercent, double haveEp, double needEp) implements CustomPacketPayload {
        public static final Type<Preview> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "beastwill_preview"));
        public static final StreamCodec<FriendlyByteBuf, Preview> STREAM_CODEC = StreamCodec.of((buf, p) -> {
            buf.writeUtf(p.will(), NAME_MAX);
            buf.writeUtf(p.beast(), 64);
            buf.writeUtf(p.affinity(), 24);
            buf.writeByte(p.rank());
            buf.writeByte(p.source());
            buf.writeBoolean(p.ok());
            buf.writeUtf(p.reason(), 24);
            buf.writeByte(p.fitPercent());
            buf.writeDouble(p.haveEp());
            buf.writeDouble(p.needEp());
        }, buf -> new Preview(buf.readUtf(NAME_MAX), buf.readUtf(64), buf.readUtf(24), buf.readByte(), buf.readByte(), buf.readBoolean(), buf.readUtf(24), buf.readByte(), buf.readDouble(), buf.readDouble()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ---------------------------------------------------------------- the server's side of the asks
    /** True for an ask the server will look at (numbers in range); everything else is dropped without a word. */
    static boolean valid(int op, int arg) {
        return switch (op) {
            case OP_FORM, OP_ABILITY -> arg == 1 || arg == 2;
            case OP_SWAP -> arg == 1 || arg == -1;
            case OP_CONFIRM, OP_DECLINE -> true;
            default -> false;
        };
    }

    static void onAction(ServerPlayer p, int op, int arg) {
        if (!valid(op, arg)) return;
        BeastWillManager.onAction(p, op, arg);
    }

    public static void send(ServerPlayer p, Sync s) { if (p.connection != null) PacketDistributor.sendToPlayer(p, s); }

    public static void send(ServerPlayer p, Preview s) { if (p.connection != null) PacketDistributor.sendToPlayer(p, s); }

    public static void send(ServerPlayer p, Info s) { if (p.connection != null) PacketDistributor.sendToPlayer(p, s); }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(Action.TYPE, Action.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onAction(sp, payload.op(), payload.arg()); }));
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<Sync> c = clientSync; if (c != null) c.accept(payload); }));
        r.playToClient(Info.TYPE, Info.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<Info> c = clientInfo; if (c != null) c.accept(payload); }));
        r.playToClient(Preview.TYPE, Preview.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<Preview> c = clientPreview; if (c != null) c.accept(payload); }));
    }
}
