package com.newuniverse.nusmp.weaponpage;

import com.newuniverse.nusmp.weaponpage.WeaponPageData.Bar;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Entry;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.Opt;
import com.newuniverse.nusmp.weaponpage.WeaponPageData.View;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Why: the Weapon page of the Codex talks over two small packets and nothing else. ONE goes up ({@link Act}: open the page, or one click: switch form, learn, feed, half-starvation) and is only an ask: the
 * server checks the sender, the weapon id, the action and the argument ({@link WeaponPageRules#check}), the player's own cooldown, then the game state (ownership, what the weapon's own rules allow), and only
 * then does what the existing system would have done. ONE goes down ({@link Sync}: everything the page shows, a few hundred bytes) to the player it is about, only when it differs from what that player holds
 * (the open request carries the checksum of the client's copy). No tick sends anything. The client hook {@link #clientSync} is set by the client class, so this common file names no client class. Both
 * packets are registered optional on channel version 1 like the rest of the mod.
 */
public final class WeaponPageNet {
    private WeaponPageNet() {}

    /** Set by the client at start-up; null on a dedicated server. */
    public static volatile Consumer<View> clientSync;

    /** Client to server: {@code weapon} ("" for an open), {@code action} ({@code WeaponPageRules.ACT_*}), {@code arg} (a form id, an item id, a form number, or the checksum of the client's copy for an open). */
    public record Act(String weapon, int action, String arg) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "weaponpage_act"));
        public static final StreamCodec<FriendlyByteBuf, Act> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.weapon(), WeaponPageRules.ID);
                    buf.writeByte(p.action());
                    buf.writeUtf(p.arg(), WeaponPageRules.ARG);
                },
                buf -> new Act(buf.readUtf(WeaponPageRules.ID), buf.readUnsignedByte(), buf.readUtf(WeaponPageRules.ARG)));

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server to client: the whole page. */
    public record Sync(View view) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("anime_multiverse", "weaponpage_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(WeaponPageNet::write, WeaponPageNet::read);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    private static void write(FriendlyByteBuf buf, Sync p) {
        View v = p.view();
        if (!v.bounded()) throw new IllegalArgumentException("weapon page out of bounds");
        buf.writeVarInt(v.held() + 1);
        buf.writeVarInt(v.entries().size());
        for (Entry e : v.entries()) {
            buf.writeUtf(e.id(), WeaponPageRules.ID);
            buf.writeUtf(e.name(), WeaponPageRules.TEXT);
            buf.writeUtf(e.grade(), WeaponPageRules.TEXT);
            buf.writeVarInt(e.level());
            buf.writeVarInt(e.max());
            buf.writeVarInt(e.pct());
            buf.writeUtf(e.form(), WeaponPageRules.TEXT);
            buf.writeVarInt(e.bars().size());
            for (Bar b : e.bars()) {
                buf.writeUtf(b.label(), WeaponPageRules.TEXT);
                buf.writeUtf(b.arg(), WeaponPageRules.TEXT);
                buf.writeVarInt(b.level());
                buf.writeVarInt(b.max());
                buf.writeVarInt(b.pct());
            }
            buf.writeVarInt(e.opts().size());
            for (Opt o : e.opts()) {
                buf.writeByte(o.action());
                buf.writeUtf(o.arg(), WeaponPageRules.ARG);
                buf.writeUtf(o.label(), WeaponPageRules.TEXT);
                buf.writeByte(o.state());
                buf.writeUtf(o.note(), WeaponPageRules.TEXT);
            }
        }
    }

    private static Sync read(FriendlyByteBuf buf) {
        int held = buf.readVarInt() - 1;
        int n = buf.readVarInt();
        if (n < 0 || n > WeaponPageRules.MAX_ENTRIES) throw new IllegalArgumentException("weapon count " + n);
        List<Entry> entries = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String id = buf.readUtf(WeaponPageRules.ID), name = buf.readUtf(WeaponPageRules.TEXT), grade = buf.readUtf(WeaponPageRules.TEXT);
            int level = buf.readVarInt(), max = buf.readVarInt(), pct = buf.readVarInt();
            String form = buf.readUtf(WeaponPageRules.TEXT);
            int nb = buf.readVarInt();
            if (nb < 0 || nb > WeaponPageRules.MAX_BARS) throw new IllegalArgumentException("bar count " + nb);
            List<Bar> bars = new ArrayList<>(nb);
            for (int b = 0; b < nb; b++) bars.add(new Bar(buf.readUtf(WeaponPageRules.TEXT), buf.readUtf(WeaponPageRules.TEXT), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
            int no = buf.readVarInt();
            if (no < 0 || no > WeaponPageRules.MAX_OPTIONS) throw new IllegalArgumentException("option count " + no);
            List<Opt> opts = new ArrayList<>(no);
            for (int o = 0; o < no; o++) opts.add(new Opt(buf.readUnsignedByte(), buf.readUtf(WeaponPageRules.ARG), buf.readUtf(WeaponPageRules.TEXT), buf.readUnsignedByte(), buf.readUtf(WeaponPageRules.TEXT)));
            entries.add(new Entry(id, name, grade, level, max, pct, form, List.copyOf(bars), List.copyOf(opts)));
        }
        View v = new View(held, List.copyOf(entries));
        if (!v.bounded()) throw new IllegalArgumentException("weapon page out of bounds");
        return new Sync(v);
    }

    /** Sends the page to one player (a stand-in of a self-test has no connection and gets nothing). */
    public static void send(ServerPlayer p, View v) {
        if (p.connection == null) return;
        try {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new Sync(v));
        } catch (RuntimeException noConnection) {
            // a player who is leaving: nothing to tell
        }
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(Act.TYPE, Act.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) WeaponPageService.onAct(sp, payload); }));
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> { Consumer<View> c = clientSync; if (c != null) c.accept(payload.view()); }));
    }
}
