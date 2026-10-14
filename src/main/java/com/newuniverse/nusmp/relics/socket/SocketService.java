package com.newuniverse.nusmp.relics.socket;

import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.RelicSlot;
import com.newuniverse.nusmp.relics.Relics;
import com.newuniverse.nusmp.relics.djinn.Bonds;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * The server side of putting a Core into a relic and drawing it out again, with the Great Sage style answers. {@link SocketEngine} calls it from the
 * sneak-use flow and {@link SocketCommands} from the ops commands. Everything about whether a Core may go in is {@link SocketRules}'s; this class reads
 * the real state (the relic's grade, the player's bonds) and writes the component.
 */
public final class SocketService {
    private SocketService() {}

    /** Weapons in the hand and armour on the head, chest and feet carry sockets; Curios and consumables do not (unless a wave opted them in). */
    public static boolean eligibleSlot(RelicSlot s) { return s != null && (s.isHand() || s.isArmor()); }

    /** How many sockets this relic stack has (0 for a stack that is not a relic). */
    public static int slotsOf(ItemStack relic) {
        RelicDef def = Relics.defOf(relic);
        if (def == null) return 0;
        return SocketRules.slots(SocketGrades.tier(relic), eligibleSlot(def.slot()), SocketCatalog.isOptIn(def.id()));
    }

    /**
     * Puts the Core into the relic. {@code force} skips only the bond check (an operator's command). The caller takes the Core item from the player when
     * this returns OK. Tells the player the answer either way.
     */
    public static SocketRules.Outcome socket(ServerPlayer p, ItemStack relic, SocketDef core, boolean force) {
        RelicDef def = Relics.defOf(relic);
        if (def == null || core == null) return SocketRules.Outcome.NO_SLOTS;
        SocketData cur = SocketComponents.get(relic);
        int slots = slotsOf(relic);
        int held = force ? 0 : Bonds.mask(p);
        boolean multi = force || Bonds.multiBond(p);
        SocketRules.Outcome o = SocketRules.insert(cur, slots, core, held, multi);
        switch (o) {
            case NO_SLOTS -> say(p, ChatFormatting.RED, "socket.nusmp.msg.no_slots", def.cardName());
            case FULL -> say(p, ChatFormatting.RED, "socket.nusmp.msg.full", def.cardName());
            case DUPLICATE -> say(p, ChatFormatting.RED, "socket.nusmp.msg.duplicate", Component.translatable(core.nameKey()));
            case BOND_CONFLICT -> say(p, ChatFormatting.RED, "socket.nusmp.msg.conflict");
            case OK -> {
                SocketData next = cur.with(new SocketData.Entry(core.id(), p.level().getGameTime()));
                SocketComponents.set(relic, next);
                Relics.invalidate(p);
                p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.2f, core.kind() == SocketKind.DEVIL ? 0.6f : 1.1f);
                Fx.ring(p.serverLevel(), p.position(), 1.4, core.rgb());
                say(p, ChatFormatting.AQUA, "socket.nusmp.msg.bound", Component.translatable(core.nameKey()), def.cardName(), next.size(), slots);
            }
        }
        return o;
    }

    /**
     * Draws the Core at this index out of the relic and returns it as an item (EMPTY if the index is empty). The caller gives it to the player. A socket
     * id the catalog no longer knows is cleared and returns nothing.
     */
    public static ItemStack unsocket(ServerPlayer p, ItemStack relic, int index) {
        RelicDef def = Relics.defOf(relic);
        SocketData cur = SocketComponents.get(relic);
        if (def == null || index < 0 || index >= cur.size()) {
            say(p, ChatFormatting.RED, "socket.nusmp.msg.empty_socket");
            return ItemStack.EMPTY;
        }
        SocketData.Entry e = cur.entries().get(index);
        SocketComponents.set(relic, cur.without(index));
        Relics.invalidate(p);
        SocketDef d = SocketCatalog.byId(e.id());
        ItemStack core = SocketItems.stack(e.id());
        if (d != null) {
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK, SoundSource.PLAYERS, 1.2f, 0.8f);
            say(p, ChatFormatting.AQUA, "socket.nusmp.msg.freed", Component.translatable(d.nameKey()), def.cardName());
        }
        return core;
    }

    /** Gives a stack to the player, or drops it at their feet. */
    public static void give(ServerPlayer p, ItemStack s) {
        if (s.isEmpty()) return;
        if (!p.getInventory().add(s)) p.drop(s, false);
    }

    static void say(ServerPlayer p, ChatFormatting colour, String key, Object... args) {
        p.displayClientMessage(Component.translatable(key, args).withStyle(colour), false);
    }
}
