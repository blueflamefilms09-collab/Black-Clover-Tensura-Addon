package com.newuniverse.nusmp.heroweapon.gem;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.rules.HeroAbandonRules;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Why: a hero weapon is for life (3.73.0 asks before it binds). When a Calling Gem or a Seal has finished its ritual, the weapon has only ANSWERED; this class holds the
 * question "Do you want to equip it?" with two clickable choices in chat ({@code /multiverse hero accept} / {@code decline}) and, on accept, calls
 * {@link GemChoosing#complete}, which checks the gates again and binds. The gem is kept until then. The question lives 30 seconds (the same window as the abandon question,
 * {@link HeroAbandonRules}), one per player, keyed by UUID, pruned on every call and capped at 64 so the map cannot grow; a player who leaves or dies forgets theirs. Server thread
 * only. Cost: a map lookup per command, one chat line per finished ritual.
 */
public final class GemOffer {
    private GemOffer() {}

    private static final int MAX_OPEN = 64;
    private record Offer(long at, HeroKind seal) {}
    private static final Map<UUID, Offer> OPEN = new HashMap<>();

    public enum Result { BOUND, DECLINED, NONE, GEM_GONE, REFUSED }

    /** Asks the player; replaces an older question of theirs. {@code seal} null: the Holy Calling Gem (which weapon is decided only on accept). */
    static void make(ServerPlayer p, HeroKind seal) {
        long now = p.level().getGameTime();
        prune(now);
        OPEN.put(p.getUUID(), new Offer(now, seal));
        String what = seal == null ? "A Legendary Weapon" : "The " + seal.display();
        p.sendSystemMessage(Component.literal(what + " answers your call. Do you want to equip it?").withStyle(ChatFormatting.GOLD));
        p.sendSystemMessage(Component.literal("It stays yours until you release it or it is taken from you; its level and forms grow with you. A Legendary Weapon also shocks you if you fight with another class of weapon.")
                .withStyle(ChatFormatting.GRAY));
        MutableComponent line = Component.literal("  ");
        line.append(Component.literal("[Equip it]").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/multiverse hero accept"))));
        line.append(Component.literal("   "));
        line.append(Component.literal("[Not now]").withStyle(s -> s.withColor(ChatFormatting.RED).withBold(true).withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/multiverse hero decline"))));
        line.append(Component.literal("   (30 seconds; your gem is kept)").withStyle(ChatFormatting.DARK_GRAY));
        p.sendSystemMessage(line);
        GemFx.say(p, Component.literal("Equip the weapon? Click in chat.").withStyle(ChatFormatting.GOLD));
    }

    /** True while the player has an open question (the self-test asks). */
    static boolean pending(UUID id, long now) { Offer o = OPEN.get(id); return o != null && HeroAbandonRules.confirmed(o.at(), now); }

    /** The player says yes. */
    public static Result accept(ServerPlayer p) {
        long now = p.level().getGameTime();
        Offer o = OPEN.remove(p.getUUID());
        if (o == null || !HeroAbandonRules.confirmed(o.at(), now)) return Result.NONE;
        ItemStack stack = find(p, o.seal());
        if (stack.isEmpty()) return Result.GEM_GONE;
        return GemChoosing.complete(p, o.seal(), stack) ? Result.BOUND : Result.REFUSED;
    }

    /** The player says no: the gem is kept, the question closed. */
    public static Result decline(ServerPlayer p) {
        Offer o = OPEN.remove(p.getUUID());
        return o == null ? Result.NONE : Result.DECLINED;
    }

    private static ItemStack find(ServerPlayer p, HeroKind seal) {
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!GemItems.isGem(s)) continue;
            if (GemItems.sealKind(s) == seal) return s;                                      // null equals null: the Holy Calling Gem
        }
        return ItemStack.EMPTY;
    }

    private static void prune(long now) {
        for (Iterator<Map.Entry<UUID, Offer>> it = OPEN.entrySet().iterator(); it.hasNext(); )
            if (!HeroAbandonRules.confirmed(it.next().getValue().at(), now)) it.remove();
        if (OPEN.size() >= MAX_OPEN) OPEN.clear();
    }

    static void forget(UUID id) { OPEN.remove(id); }

    static void clear() { OPEN.clear(); }
}
