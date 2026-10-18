package com.newuniverse.nusmp.heroweapon.bond;

import com.newuniverse.nusmp.client.heroweapon.ClientHeroBond;
import com.newuniverse.nusmp.heroweapon.skill.HeroKeys;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the core talks to the client in four short messages and no more. Two go up: a hero key (a press, a heartbeat while held, a release) and an action
 * (summon or put away, change form); the server decides what each means and re-checks everything. Two go down: the {@link Sync} of the bond (what the HUD, the
 * tooltip and the Weapon Book need to draw: which weapon, its experience, the form, whether it is out, what is learned, the mastery, a short summary of what
 * the gem has absorbed, and the techniques on the active form's inputs) and a {@link Cooldown} (a technique was cast: the HUD starts its bar). Register
 * {@link #register} on the mod bus. The ids are {@code nusmp:hero_key}, {@code hero_act}, {@code hero_sync} and {@code hero_cooldown}.
 */
public final class HeroNet {
    private HeroNet() {}

    /** The phases of a {@link Key}: pressed, still held (a heartbeat every 10 ticks), released. */
    public static final int PRESS = 0, HOLD = 1, RELEASE = 2;

    /** The {@link Act} actions: toggle the weapon out or away, change to the next form, summon (only), put away (only). */
    public static final int ACT_TOGGLE = 0, ACT_CYCLE = 1, ACT_SUMMON = 2, ACT_DISMISS = 3;

    /** The bits of {@link Sync#flags()}: repelled by another Holy wielder, a curse form is active, the weapon holds its wielder (an Overrun). */
    public static final int FLAG_REPELLED = 1, FLAG_CURSED = 2, FLAG_LOCKED = 4;

    /** Most entries any list of a {@link Sync} may carry (the server keeps well below it; a longer list is a corrupt packet). */
    private static final int MAX_LIST = 512;

    // ================================================================ client to server
    /** A hero key: {@code slot} 0 to 2 (keys one to three), {@code phase} one of {@link #PRESS}, {@link #HOLD}, {@link #RELEASE}. */
    public record Key(int slot, int phase) implements CustomPacketPayload {
        public static final Type<Key> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_key"));
        public static final StreamCodec<FriendlyByteBuf, Key> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeByte(p.slot());
                    buf.writeByte(p.phase());
                },
                buf -> new Key(buf.readByte(), buf.readByte()));

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** A key action that is not a technique: one of the {@code ACT_} constants. */
    public record Act(int action) implements CustomPacketPayload {
        public static final Type<Act> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_act"));
        public static final StreamCodec<FriendlyByteBuf, Act> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> buf.writeByte(p.action()),
                buf -> new Act(buf.readByte()));

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ================================================================ server to client
    /** A name and a number: a form's mastery points, or an absorbed item's count. */
    public record Entry(String key, int value) {}

    /**
     * The bond as the client needs to draw it. {@code weapon} is the item id ({@code legendary_shield}), empty when the player has no bond (the client forgets
     * everything). {@code formName} is the active form's display name; {@code slots} are the global skill ids on the active form's five inputs in the order
     * use, sneak, key one, key two, key three (empty text for none). {@code absorbed} is the largest few entries of what the gem has absorbed;
     * {@code absorbedKinds} and {@code absorbedTotal} count all of it. A record, so two equal bonds compare equal and the server sends only what changed.
     */
    public record Sync(String weapon, long exp, String form, String formName, boolean summoned, int flags, List<String> unlocked, List<Entry> mastery,
                       int absorbedKinds, long absorbedTotal, List<Entry> absorbed, List<String> slots) implements CustomPacketPayload {
        public static final Type<Sync> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_sync"));
        public static final StreamCodec<FriendlyByteBuf, Sync> STREAM_CODEC = StreamCodec.of(Sync::write, Sync::read);

        /** The message that tells the client the player has no weapon. */
        public static Sync none() { return new Sync("", 0L, "base", "", false, 0, List.of(), List.of(), 0, 0L, List.of(), List.of()); }

        public boolean isNone() { return weapon.isEmpty(); }

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        static void write(FriendlyByteBuf buf, Sync s) {
            buf.writeUtf(s.weapon());
            buf.writeVarLong(s.exp());
            buf.writeUtf(s.form());
            buf.writeUtf(s.formName());
            buf.writeBoolean(s.summoned());
            buf.writeVarInt(s.flags());
            writeStrings(buf, s.unlocked());
            writeEntries(buf, s.mastery());
            buf.writeVarInt(s.absorbedKinds());
            buf.writeVarLong(s.absorbedTotal());
            writeEntries(buf, s.absorbed());
            writeStrings(buf, s.slots());
        }

        static Sync read(FriendlyByteBuf buf) {
            String weapon = buf.readUtf();
            long exp = buf.readVarLong();
            String form = buf.readUtf();
            String formName = buf.readUtf();
            boolean summoned = buf.readBoolean();
            int flags = buf.readVarInt();
            List<String> unlocked = readStrings(buf);
            List<Entry> mastery = readEntries(buf);
            int kinds = buf.readVarInt();
            long total = buf.readVarLong();
            List<Entry> absorbed = readEntries(buf);
            List<String> slots = readStrings(buf);
            return new Sync(weapon, exp, form, formName, summoned, flags, unlocked, mastery, kinds, total, absorbed, slots);
        }
    }

    /** A technique was cast: its id and its cooldown in ticks, so the HUD can run its bar. */
    public record Cooldown(String skill, int ticks) implements CustomPacketPayload {
        public static final Type<Cooldown> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "hero_cooldown"));
        public static final StreamCodec<FriendlyByteBuf, Cooldown> STREAM_CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeUtf(p.skill());
                    buf.writeVarInt(p.ticks());
                },
                buf -> new Cooldown(buf.readUtf(), buf.readVarInt()));

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ================================================================ list helpers
    private static void writeStrings(FriendlyByteBuf buf, List<String> l) {
        buf.writeVarInt(l.size());
        for (String s : l) buf.writeUtf(s);
    }

    private static List<String> readStrings(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > MAX_LIST) throw new IllegalStateException("hero sync: a list of " + n + " names is not valid");
        List<String> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(buf.readUtf());
        return List.copyOf(out);
    }

    private static void writeEntries(FriendlyByteBuf buf, List<Entry> l) {
        buf.writeVarInt(l.size());
        for (Entry e : l) {
            buf.writeUtf(e.key());
            buf.writeVarInt(e.value());
        }
    }

    private static List<Entry> readEntries(FriendlyByteBuf buf) {
        int n = buf.readVarInt();
        if (n < 0 || n > MAX_LIST) throw new IllegalStateException("hero sync: a list of " + n + " entries is not valid");
        List<Entry> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) out.add(new Entry(buf.readUtf(), buf.readVarInt()));
        return List.copyOf(out);
    }

    // ================================================================ registration
    /** Mod-bus listener: registers the four payloads. Both directions are optional, so a client without the mod can still join a server that has it. */
    public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(Sync.TYPE, Sync.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> ClientHeroBond.receive(payload)));
        r.playToClient(Cooldown.TYPE, Cooldown.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> ClientHeroBond.cooldown(payload)));
        r.playToServer(Key.TYPE, Key.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) HeroKeys.onKey(sp, payload.slot(), payload.phase());
        }));
        r.playToServer(Act.TYPE, Act.STREAM_CODEC, (payload, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer sp) HeroKeys.onAction(sp, payload.action());
        }));
    }
}
