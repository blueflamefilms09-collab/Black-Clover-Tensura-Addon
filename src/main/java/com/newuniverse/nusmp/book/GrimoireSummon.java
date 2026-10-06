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
 * Summon Grimoire, the base ability of every grimoire book: your grimoire leaves your bag and floats, glowing, in front of your
 * right hand, so you can cast with your hands free (a summoned grimoire counts as held). It stays your item the whole time; only
 * its floating copy is drawn.
 *
 * <p>Put it away: double-tap sneak (shift), or use Summon Grimoire again while sneaking. It also goes away on death, dimension
 * change, logging out, or when the grimoire item leaves your inventory.
 *
 * <p>Sync: {@link FloatPayload} (entity id + the grimoire stack, empty = put away) to the player and everyone tracking them.
 */
public final class GrimoireSummon {
    /** Two sneak presses within this many ticks put the grimoire away. */
    public static final int DOUBLE_TAP_TICKS = 8;

    private static final class State {
        final MagicType magic;
        boolean wasSneaking;
        long lastSneakPress = -100;
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

    /** The owner's grimoire of this magic anywhere in their inventory, or EMPTY. */
    static ItemStack find(Player p, MagicType magic) {
        for (ItemStack s : p.getInventory().items) if (GrimoireItem.isOwnedBy(s, p.getUUID()) && magic.name().equals(GrimoireItem.data(s).getString("Magic"))) return s;
        for (ItemStack s : p.getInventory().offhand) if (GrimoireItem.isOwnedBy(s, p.getUUID()) && magic.name().equals(GrimoireItem.data(s).getString("Magic"))) return s;
        return ItemStack.EMPTY;
    }

    /** The ability: summon, or put away when already out (or when sneaking). */
    public static void toggle(ServerPlayer p, GrimoireBook book) {
        if (isFloating(p) && (p.isShiftKeyDown() || FLOATING.get(p.getUUID()).magic == book.magic)) { dismiss(p, true); return; }
        if (p.isShiftKeyDown()) return;
        summon(p, book);
    }

    public static void summon(ServerPlayer p, GrimoireBook book) {
        ItemStack stack = find(p, book.magic);
        if (stack.isEmpty()) { GrimoireBook.fail(p, "Your " + book.magic.displayName + " grimoire isn't with you."); return; }
        FLOATING.put(p.getUUID(), new State(book.magic));
        broadcast(p, stack.copy());
        Vec3 hand = handPoint(p);
        VfxSpawn.send(p.serverLevel(), VfxShape.MAGIC_CIRCLE, hand, hand.add(p.getViewVector(1f)), book.color, 16, 0.35f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0f, 0.8f);
        p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6f, 1.2f);
        p.displayClientMessage(Component.literal("Your grimoire answers. (double-tap sneak to put it away)").withStyle(ChatFormatting.GOLD), true);
    }

    public static void dismiss(ServerPlayer p, boolean effects) {
        if (FLOATING.remove(p.getUUID()) == null) return;
        broadcast(p, ItemStack.EMPTY);
        if (effects) {
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOOK_PUT, SoundSource.PLAYERS, 0.9f, 1.0f);
            p.displayClientMessage(Component.literal("You put your grimoire away.").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** Roughly where the floating book sits (in front of the right hand), for the summon flash. */
    static Vec3 handPoint(Player p) {
        double yaw = Math.toRadians(p.yBodyRot);
        Vec3 fwd = new Vec3(-Math.sin(yaw), 0, Math.cos(yaw)), right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
        return p.position().add(0, 1.05, 0).add(fwd.scale(0.55)).add(right.scale(0.45));
    }

    // ---------------------------------------------------------------- events
    /** Double-tap sneak puts it away; losing the item puts it away silently. */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        State s = FLOATING.get(p.getUUID());
        if (s == null) return;
        if (find(p, s.magic).isEmpty() || !p.isAlive()) { dismiss(p, false); return; }
        boolean sneaking = p.isShiftKeyDown();
        if (sneaking && !s.wasSneaking) {
            long now = p.level().getGameTime();
            if (now - s.lastSneakPress <= DOUBLE_TAP_TICKS) { s.wasSneaking = true; dismiss(p, true); return; }
            s.lastSneakPress = now;
        }
        s.wasSneaking = sneaking;
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

    /** Mod-bus listener. The handler only runs on clients, so the client class is never loaded on a server. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").optional().playToClient(FloatPayload.TYPE, FloatPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> com.newuniverse.nusmp.client.grimoire.GrimoireFloatClient.receive(payload.entityId(), payload.stack())));
    }
}
