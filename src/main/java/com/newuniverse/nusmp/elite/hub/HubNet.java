package com.newuniverse.nusmp.elite.hub;

import com.mojang.logging.LogUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the quest overlay, the quest dialogue, the shop and the Dungeon Map are all drawn on the CLIENT from facts only the SERVER knows, and the dialogue and the shop send requests back, so the
 * Hub has six small packets under their own ids: {@code hub_hud}, {@code hub_board}, {@code hub_shop}, {@code hub_map} (server to client) and {@code hub_quest_action}, {@code hub_buy} (client to
 * server). None carries a translated text, a price the server would trust or a decision: the server packets carry the English text of a quest (it comes from data, so there is no lang key per
 * quest, the System's approach) and numbers; the client packets carry ONLY an id, and the server re-derives everything (the price from the shop table, the progress from the saved data, the
 * distance from the open menu) before it acts. The wire format is written by hand with bounded strings and bounded lists, so a hostile or damaged packet cannot make either side allocate without
 * limit, and every text is cut ({@link #clip}) before {@code writeUtf}, which would otherwise THROW on the network thread and disconnect the player. Handlers hop to the main thread, never throw
 * (a throwing handler disconnects the sender) and reach client code by name only inside a lambda, so a dedicated server never loads a client class. Optional channels, like every payload of the mod.
 */
public final class HubNet {
    private HubNet() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String PROTOCOL = "1";
    /** Bounds of the lists on the wire. */
    public static final int MAX_HUD = 12, MAX_BOARD = 48, MAX_SHOP = 128;

    static String clip(String s, int max) { return s == null ? "" : s.length() <= max ? s : s.substring(0, max); }

    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("nusmp", path); }

    // ================================================================ server to client
    /** One line of the overlay. {@code state}: 0 active, 1 ready to hand in. */
    public record HudEntry(String title, String label, int progress, int target, int state, long reward) {}

    /** The overlay: the player's accepted quests. */
    public record HudSync(List<HudEntry> entries) implements CustomPacketPayload {
        public static final Type<HudSync> TYPE = new Type<>(id("hub_hud"));
        public static final StreamCodec<FriendlyByteBuf, HudSync> CODEC = StreamCodec.of(HudSync::write, HudSync::read);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, HudSync p) {
            int n = Math.min(p.entries().size(), MAX_HUD);
            b.writeVarInt(n);
            for (int i = 0; i < n; i++) {
                HudEntry e = p.entries().get(i);
                b.writeUtf(clip(e.title(), 120), 120);
                b.writeUtf(clip(e.label(), 80), 80);
                b.writeVarInt(e.progress());
                b.writeVarInt(e.target());
                b.writeByte(e.state());
                b.writeVarLong(e.reward());
            }
        }

        static HudSync read(FriendlyByteBuf b) {
            int n = Math.max(0, Math.min(b.readVarInt(), MAX_HUD));
            List<HudEntry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(new HudEntry(b.readUtf(120), b.readUtf(80), b.readVarInt(), b.readVarInt(), b.readByte(), b.readVarLong()));
            return new HudSync(list);
        }
    }

    /** Board states. */
    public static final int OFFER = 0, ACTIVE = 1, READY = 2, DONE = 3;

    /** One row of the Quest Giver's board. {@code type}: 0 daily, 1 progression. */
    public record BoardEntry(String id, int type, int state, String title, String desc, String label, int progress, int target, long reward) {}

    /** The dialogue's content: what is on offer today, what the player carries, what they have done today, and their money. */
    public record BoardSync(long balance, List<BoardEntry> entries) implements CustomPacketPayload {
        public static final Type<BoardSync> TYPE = new Type<>(id("hub_board"));
        public static final StreamCodec<FriendlyByteBuf, BoardSync> CODEC = StreamCodec.of(BoardSync::write, BoardSync::read);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, BoardSync p) {
            b.writeVarLong(p.balance());
            int n = Math.min(p.entries().size(), MAX_BOARD);
            b.writeVarInt(n);
            for (int i = 0; i < n; i++) {
                BoardEntry e = p.entries().get(i);
                b.writeUtf(clip(e.id(), 48), 48);
                b.writeByte(e.type());
                b.writeByte(e.state());
                b.writeUtf(clip(e.title(), 120), 120);
                b.writeUtf(clip(e.desc(), 240), 240);
                b.writeUtf(clip(e.label(), 80), 80);
                b.writeVarInt(e.progress());
                b.writeVarInt(e.target());
                b.writeVarLong(e.reward());
            }
        }

        static BoardSync read(FriendlyByteBuf b) {
            long balance = b.readVarLong();
            int n = Math.max(0, Math.min(b.readVarInt(), MAX_BOARD));
            List<BoardEntry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(new BoardEntry(b.readUtf(48), b.readByte(), b.readByte(), b.readUtf(120), b.readUtf(240), b.readUtf(80), b.readVarInt(), b.readVarInt(), b.readVarLong()));
            return new BoardSync(balance, list);
        }
    }

    /** One ware as the shop screen shows it. {@code remaining}: stacks the player may still buy today, -1 = no limit. {@code lockedFloor}: 0 or the floor needed. */
    public record ShopEntry(String id, String item, int count, long price, String category, int remaining, int lockedFloor) {}

    /** The shop's content and the player's money. {@code unit} is the word for the money in the current mode. */
    public record ShopSync(long balance, String unit, List<ShopEntry> entries) implements CustomPacketPayload {
        public static final Type<ShopSync> TYPE = new Type<>(id("hub_shop"));
        public static final StreamCodec<FriendlyByteBuf, ShopSync> CODEC = StreamCodec.of(ShopSync::write, ShopSync::read);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, ShopSync p) {
            b.writeVarLong(p.balance());
            b.writeUtf(clip(p.unit(), 24), 24);
            int n = Math.min(p.entries().size(), MAX_SHOP);
            b.writeVarInt(n);
            for (int i = 0; i < n; i++) {
                ShopEntry e = p.entries().get(i);
                b.writeUtf(clip(e.id(), 40), 40);
                b.writeUtf(clip(e.item(), 80), 80);
                b.writeVarInt(e.count());
                b.writeVarLong(e.price());
                b.writeUtf(clip(e.category(), 24), 24);
                b.writeVarInt(e.remaining() + 1);
                b.writeVarInt(e.lockedFloor());
            }
        }

        static ShopSync read(FriendlyByteBuf b) {
            long balance = b.readVarLong();
            String unit = b.readUtf(24);
            int n = Math.max(0, Math.min(b.readVarInt(), MAX_SHOP));
            List<ShopEntry> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) list.add(new ShopEntry(b.readUtf(40), b.readUtf(80), b.readVarInt(), b.readVarLong(), b.readUtf(24), b.readVarInt() - 1, b.readVarInt()));
            return new ShopSync(balance, unit, list);
        }
    }

    /** The Dungeon Map: the rooms and doors of one floor. {@code note} is a short line for the footer (what the use cost). */
    public record MapSync(MapView view, String note) implements CustomPacketPayload {
        public static final Type<MapSync> TYPE = new Type<>(id("hub_map"));
        public static final StreamCodec<FriendlyByteBuf, MapSync> CODEC = StreamCodec.of(MapSync::write, MapSync::read);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf b, MapSync p) {
            MapView v = p.view();
            b.writeInt(v.floor());
            b.writeUtf(clip(v.road(), 16), 16);
            b.writeInt(v.arrivalX());
            b.writeInt(v.arrivalZ());
            b.writeBoolean(v.hasExit());
            b.writeInt(v.exitX());
            b.writeInt(v.exitZ());
            b.writeUtf(clip(p.note(), 80), 80);
            int n = Math.min(v.rooms().size(), MapView.HARD_CAP);
            b.writeVarInt(n);
            for (int i = 0; i < n; i++) {
                MapView.Room r = v.rooms().get(i);
                b.writeVarInt(r.id());
                b.writeByte(r.kind());
                b.writeInt(r.minX());
                b.writeInt(r.minZ());
                b.writeInt(r.maxX());
                b.writeInt(r.maxZ());
                b.writeBoolean(r.visited());
            }
            List<int[]> edges = v.edges();
            int m = Math.min(edges.size(), MapView.HARD_CAP * 4);
            b.writeVarInt(m);
            for (int i = 0; i < m; i++) {
                b.writeVarInt(edges.get(i)[0]);
                b.writeVarInt(edges.get(i)[1]);
            }
        }

        static MapSync read(FriendlyByteBuf b) {
            int floor = b.readInt();
            String road = b.readUtf(16);
            int ax = b.readInt(), az = b.readInt();
            boolean hasExit = b.readBoolean();
            int ex = b.readInt(), ez = b.readInt();
            String note = b.readUtf(80);
            int n = Math.max(0, Math.min(b.readVarInt(), MapView.HARD_CAP));
            List<MapView.Room> rooms = new ArrayList<>(n);
            for (int i = 0; i < n; i++) rooms.add(new MapView.Room(b.readVarInt(), b.readByte(), b.readInt(), b.readInt(), b.readInt(), b.readInt(), b.readBoolean()));
            int m = Math.max(0, Math.min(b.readVarInt(), MapView.HARD_CAP * 4));
            List<int[]> edges = new ArrayList<>(m);
            for (int i = 0; i < m; i++) edges.add(new int[] {b.readVarInt(), b.readVarInt()});
            return new MapSync(new MapView(floor, road, rooms, edges, ax, az, ex, ez, hasExit), note);
        }
    }

    // ================================================================ client to server
    /** Quest dialogue requests. */
    public static final int ACT_ACCEPT = 0, ACT_TURN_IN = 1, ACT_ABANDON = 2;

    public record QuestAction(int action, String questId) implements CustomPacketPayload {
        public static final Type<QuestAction> TYPE = new Type<>(id("hub_quest_action"));
        public static final StreamCodec<FriendlyByteBuf, QuestAction> CODEC = StreamCodec.of((b, p) -> {
            b.writeByte(p.action());
            b.writeUtf(clip(p.questId(), 48), 48);
        }, b -> new QuestAction(b.readByte(), b.readUtf(48)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** A purchase of one stack of an offer. */
    public record Buy(String offerId) implements CustomPacketPayload {
        public static final Type<Buy> TYPE = new Type<>(id("hub_buy"));
        public static final StreamCodec<FriendlyByteBuf, Buy> CODEC = StreamCodec.of((b, p) -> b.writeUtf(clip(p.offerId(), 40), 40), b -> new Buy(b.readUtf(40)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ================================================================ registration
    /** Mod-bus listener: registers the six payloads. */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar r = event.registrar(PROTOCOL).optional();
        r.playToClient(HudSync.TYPE, HudSync.CODEC, (p, ctx) -> ctx.enqueueWork(() -> client(() -> com.newuniverse.nusmp.client.elite.hub.HubClient.hud(p))));
        r.playToClient(BoardSync.TYPE, BoardSync.CODEC, (p, ctx) -> ctx.enqueueWork(() -> client(() -> com.newuniverse.nusmp.client.elite.hub.HubClient.board(p))));
        r.playToClient(ShopSync.TYPE, ShopSync.CODEC, (p, ctx) -> ctx.enqueueWork(() -> client(() -> com.newuniverse.nusmp.client.elite.hub.HubClient.shop(p))));
        r.playToClient(MapSync.TYPE, MapSync.CODEC, (p, ctx) -> ctx.enqueueWork(() -> client(() -> com.newuniverse.nusmp.client.elite.hub.HubClient.map(p))));
        r.playToServer(QuestAction.TYPE, QuestAction.CODEC, (p, ctx) -> ctx.enqueueWork(() -> onQuestAction(ctx.player(), p)));
        r.playToServer(Buy.TYPE, Buy.CODEC, (p, ctx) -> ctx.enqueueWork(() -> onBuy(ctx.player(), p)));
    }

    private static void client(Runnable r) {
        try {
            r.run();
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] elite hub: a client packet could not be applied", e);
        }
    }

    private static void onQuestAction(Player player, QuestAction p) {
        try {
            if (player instanceof ServerPlayer sp && sp.isAlive() && ActionGate.allow(sp)) QuestService.handle(sp, p.action(), p.questId());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] elite hub: a quest request failed", e);
        }
    }

    private static void onBuy(Player player, Buy p) {
        try {
            if (player instanceof ServerPlayer sp && sp.isAlive() && ActionGate.allow(sp)) ShopService.buy(sp, p.offerId());
        } catch (RuntimeException e) {
            LOG.warn("[nusmp] elite hub: a purchase request failed", e);
        }
    }
}
