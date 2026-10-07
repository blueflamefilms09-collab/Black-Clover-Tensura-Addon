package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.GrimoireItem;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Summon Grimoire, the base ability of every grimoire book. Press the ability key while your grimoire is in its Grimoire Slot
 * (see blackclover.GrimoireSlot): it leaves its dormant spot at your right hip and floats up in front of your right hand, glowing
 * and bobbing, so you cast with your hands free (a summoned grimoire counts as held). It stays your item the whole time; only
 * its floating copy is drawn.
 *
 * <p>Put it away: press the ability key while sneaking (shift); it floats back down to your hip. It also goes away on death,
 * dimension change, logging out, or when the grimoire leaves the slot. Switching spells while it is out flips its pages
 * ({@link FlipPayload}).
 *
 * <p>Sync: {@link FloatPayload} (entity id + the grimoire stack, empty = put away) to the player and everyone tracking them.
 */
public final class GrimoireSummon {
    private static final class State {
        final MagicType magic;
        long lastFlip = -100;
        State(MagicType magic) { this.magic = magic; }
    }

    private static final Map<UUID, State> FLOATING = new HashMap<>();

    private GrimoireSummon() {}

    // ---------------------------------------------------------------- state
    public static boolean isFloating(Player p) { return FLOATING.containsKey(p.getUUID()); }

    /** True when this player's grimoire of {@code magic} is out and floating (null = any of theirs). */
    public static boolean isFloating(Player p, MagicType magic) {
        State s = FLOATING.get(p.getUUID());
        return s != null && (magic == null || s.magic == magic) && !find(p, s.magic).isEmpty();
    }

    /** The owner's grimoire of this magic in the Grimoire Slot, or EMPTY. */
    static ItemStack find(Player p, MagicType magic) {
        for (ItemStack s : com.newuniverse.nusmp.blackclover.GrimoireSlot.ready(p))
            if (GrimoireItem.isOwnedBy(s, p.getUUID()) && magic.name().equals(GrimoireItem.data(s).getString("Magic"))) return s;
        return ItemStack.EMPTY;
    }

    /** The ability key: summon; with shift held, stow it back at the hip. */
    public static void toggle(ServerPlayer p, GrimoireBook book) { toggle(p, book, p.isShiftKeyDown()); }

    public static void toggle(ServerPlayer p, GrimoireBook book, boolean stow) {
        if (stow) { dismiss(p, true); return; }
        State s = FLOATING.get(p.getUUID());
        if (s != null && s.magic == book.magic) {
            p.displayClientMessage(Component.literal("Your grimoire is already out. (sneak + ability key to stow it)").withStyle(ChatFormatting.GRAY), true);
            return;
        }
        summon(p, book);
    }

    public static void summon(ServerPlayer p, GrimoireBook book) {
        ItemStack stack = find(p, book.magic);
        if (stack.isEmpty()) { GrimoireBook.fail(p, "Your " + book.magic.displayName + " grimoire isn't in your Grimoire Slot."); return; }
        FLOATING.put(p.getUUID(), new State(book.magic));
        broadcast(p, stack.copy());
        Vec3 hand = handPoint(p);
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE, hand, hand.add(p.getViewVector(1f)), book.color, 16, 0.35f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.2f);
        p.displayClientMessage(Component.literal("Your grimoire answers. (sneak + ability key to stow it)").withStyle(ChatFormatting.GOLD), true);
        PaintStudio.onSummon(p, book.magic);                                                    // 0.44: a Painting grimoire brings its palette & brush
    }

    public static void dismiss(ServerPlayer p, boolean effects) {
        if (FLOATING.remove(p.getUUID()) == null) return;
        broadcast(p, ItemStack.EMPTY);
        PaintStudio.onDismiss(p);                                                               // 0.44: the palette & brush dissolve
        if (effects) {
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 0.9f, 1.0f);
            p.displayClientMessage(Component.literal("Your grimoire returns to your side.").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** The magic of the grimoire this player has out, or null (0.44: the palette & brush, element reading). */
    public static MagicType floatingMagic(Player p) {
        State s = FLOATING.get(p.getUUID());
        return s == null ? null : s.magic;
    }

    /** Roughly where the floating book sits (in front of the right hand), for the summon flash. */
    static Vec3 handPoint(Player p) {
        double yaw = Math.toRadians(p.yBodyRot);
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        return p.position().add(0, 1.05, 0).add(fwd.scale(0.55)).add(right.scale(0.45));
    }

    // ---------------------------------------------------------------- events
    /** Losing the grimoire (it left the slot and the hands) puts it away silently. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        State s = FLOATING.get(p.getUUID());
        if (s == null) return;
        if (find(p, s.magic).isEmpty() || !p.isAlive()) dismiss(p, false);
    }

    /** A spell switch on a book of this magic: if that grimoire is out, its pages flip for everyone who can see it. */
    public static void onSpellSwitch(ServerPlayer p, MagicType magic, boolean reverse) {
        State s = FLOATING.get(p.getUUID());
        if (s == null || (magic != null && s.magic != magic)) return;
        long now = p.level().getGameTime();
        if (now - s.lastFlip < 2) return;                                                       // one flip per switch
        s.lastFlip = now;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new FlipPayload(p.getId(), reverse));
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.7f, 1.3f);
    }

    public static void onDeath(LivingDeathEvent e) { if (e.getEntity() instanceof ServerPlayer p) dismiss(p, false); }
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) { FLOATING.remove(e.getEntity().getUUID()); }
    public static void onDimension(PlayerEvent.PlayerChangedDimensionEvent e) { if (e.getEntity() instanceof ServerPlayer p) dismiss(p, false); }

    /** Someone starts seeing this player: tell them about the floating book. */
    public static void onStartTracking(PlayerEvent.StartTracking e) {
        if (!(e.getEntity() instanceof ServerPlayer watcher) || !(e.getTarget() instanceof ServerPlayer target)) return;
        State s = FLOATING.get(target.getUUID());
        if (s == null) return;
        ItemStack stack = find(target, s.magic);
        if (!stack.isEmpty()) PacketDistributor.sendToPlayer(watcher, new FloatPayload(target.getId(), stack.copy()));
    }

    private static void broadcast(ServerPlayer p, ItemStack stack) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(p, new FloatPayload(p.getId(), stack));
    }

    // ---------------------------------------------------------------- network
    /** Player {@code entityId}'s floating grimoire; an empty stack means it was put away. */
    public record FloatPayload(int entityId, ItemStack stack) implements CustomPacketPayload {
        public static final Type<FloatPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "grimoire_float"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FloatPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, FloatPayload::entityId,
                ItemStack.OPTIONAL_STREAM_CODEC, FloatPayload::stack,
                FloatPayload::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Player {@code entityId} switched spells while their grimoire is out: play the page flip ({@code reverse} = flip backwards). */
    public record FlipPayload(int entityId, boolean reverse) implements CustomPacketPayload {
        public static final Type<FlipPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "grimoire_flip"));
        public static final StreamCodec<RegistryFriendlyByteBuf, FlipPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, FlipPayload::entityId,
                ByteBufCodecs.BOOL, FlipPayload::reverse,
                FlipPayload::new);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** The "Summon Grimoire" key (client): summon your book from its slot, or stow it ({@code stow} = shift was held). */
    public record KeyPayload(boolean stow) implements CustomPacketPayload {
        public static final Type<KeyPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "grimoire_summon_key"));
        public static final StreamCodec<RegistryFriendlyByteBuf, KeyPayload> STREAM_CODEC = StreamCodec.composite(ByteBufCodecs.BOOL, KeyPayload::stow, KeyPayload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Server side of the key: the player's own grimoire book decides which magic floats. */
    static void onKey(ServerPlayer p, boolean stow) {
        if (stow) { dismiss(p, true); return; }
        var g = com.newuniverse.nusmp.blackclover.GrimoirePages.grimoireOf(p);
        if (g.isEmpty() || !(g.get().getSkill() instanceof GrimoireBook book)) {
            GrimoireBook.fail(p, "You have no grimoire bound yet.");
            return;
        }
        toggle(p, book, false);
    }

    /** Mod-bus listener. The handler only runs on clients, so the client class is never loaded on a server. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToClient(FloatPayload.TYPE, FloatPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.grimoire.GrimoireFloatClient.receive(payload.entityId(), payload.stack())));
        r.playToServer(KeyPayload.TYPE, KeyPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onKey(sp, payload.stow()); }));
        r.playToClient(FlipPayload.TYPE, FlipPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.grimoire.GrimoireFloatClient.flip(payload.entityId(), payload.reverse())));
    }
}
