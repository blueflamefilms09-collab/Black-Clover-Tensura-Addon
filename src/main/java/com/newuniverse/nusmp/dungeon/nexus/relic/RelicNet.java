package com.newuniverse.nusmp.dungeon.nexus.relic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * The one packet the relics need (docs/living_dungeon.md section 7): the "Use Relic" key. A relic worn in a Curios slot cannot be right-clicked, so
 * the Gambler's Grimoire Page, which is cast by using it, is cast with a key: the client tells the server the key was pressed, and the server finds
 * the Page the player wears (attuned, one of their two relics) and casts it. Register {@link #registerPayloads} on the mod bus.
 */
public final class RelicNet {
    private RelicNet() {}

    /** Ticks between two presses the server answers (a held-down key cannot flood it). */
    private static final int SPACING = 5;

    /** The "Use Relic" key (client to server): no data, the server knows who sent it. */
    public record UseRelic() implements CustomPacketPayload {
        public static final UseRelic INSTANCE = new UseRelic();
        public static final Type<UseRelic> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("nusmp", "use_relic"));
        public static final StreamCodec<RegistryFriendlyByteBuf, UseRelic> STREAM_CODEC = StreamCodec.unit(INSTANCE);

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Mod-bus listener. */
    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1").optional();
        r.playToServer(UseRelic.TYPE, UseRelic.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> { if (ctx.player() instanceof ServerPlayer sp) onUse(sp); }));
    }

    /** Server thread: the player pressed the key. Casts the Page they wear, or says there is none. */
    static void onUse(ServerPlayer p) {
        if (!p.isAlive() || p.isSpectator()) return;
        if (!RelicState.due(p.getUUID(), "use_relic", p.level().getGameTime(), SPACING)) return;
        ItemStack page = Worn.stack(p, Relic.GAMBLERS_GRIMOIRE_PAGE);
        if (page.isEmpty()) {
            p.displayClientMessage(Component.literal("You wear no attuned Gambler's Grimoire Page: wear one in a Dungeon Relic slot to use it with this key.")
                    .withStyle(ChatFormatting.GRAY), true);
            return;
        }
        GamblerPage.use(p, page);
    }
}
