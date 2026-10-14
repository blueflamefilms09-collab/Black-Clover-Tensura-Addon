package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.relics.djinn.HouseholdRules.Verdict;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/**
 * Household Vessels: an owner lends a fraction of their Djinn to up to two allies. The ally accepts (a minute to answer), gets only the Lesser Summon,
 * and their assimilation rises with use until the Djinn withdraws at 50%. The Djinn stays the owner's: one of each type, one possessor.
 */
public final class DjinnHousehold {
    private DjinnHousehold() {}

    /** The owner offers their Djinn to an ally. */
    public static void share(ServerPlayer owner, ServerPlayer ally) {
        MinecraftServer server = owner.getServer();
        if (server == null) return;
        DjinnState os = DjinnState.of(owner);
        Djinn d = os.djinn();
        if (com.newuniverse.nusmp.relics.djinn.dark.DarkDjinn.active(os)) {                       // household members never get a dark Djinn
            owner.sendSystemMessage(Component.literal("A dark Djinn cannot be lent. Cleanse it first: Holy Water on the vessel, sneaking.").withStyle(ChatFormatting.RED));
            return;
        }
        DjinnLedger ledger = DjinnLedger.get(server);
        int members = d == null ? 0 : ledger.household(d).size();
        boolean already = d != null && ledger.isMember(d, ally.getUUID());
        Verdict v = HouseholdRules.invite(d != null, owner == ally, members, already, Bonds.mask(ally), Bonds.multiBond(ally));
        if (v != Verdict.OK) { owner.sendSystemMessage(Component.literal(shareText(v, ally)).withStyle(ChatFormatting.RED)); return; }
        long now = owner.level().getGameTime();
        PendingInvites.offer(ally.getUUID(), owner.getUUID(), now);
        owner.sendSystemMessage(Component.literal("You offer " + ally.getName().getString() + " a fraction of " + d.displayName() + ". They have a minute to accept.").withColor(d.color()));
        Component accept = Component.literal(" [Accept] ").withStyle(s -> s.withColor(ChatFormatting.GREEN).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/multiverse djinn accept"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Take the Lesser Summon of " + d.displayName()))));
        ally.sendSystemMessage(Component.literal(owner.getName().getString() + " offers you a fraction of " + d.fullName() + ": only the Lesser Summon, and it changes you the more you use it (it withdraws at "
                + (int) DjinnPhaseRules.HOUSEHOLD_CAP + "% assimilation). While you hold it you cannot take a Spirit or a Devil.").withColor(d.color()).append(accept));
    }

    private static String shareText(Verdict v, ServerPlayer ally) {
        String n = ally.getName().getString();
        return switch (v) {
            case NOT_OWNER -> "You hold no Djinn to lend.";
            case SELF -> "A Djinn is lent to others, not to yourself.";
            case FULL -> "Your household is full: " + DjinnPhaseRules.HOUSEHOLD_MAX + " allies at the most.";
            case ALREADY -> n + " already shares your Djinn.";
            case ALLY_BONDED -> n + " already holds a Spirit or a Devil.";
            case ALLY_HAS_DJINN -> n + " already carries a Djinn.";
            default -> "Not now.";
        };
    }

    /** The ally takes the offer. */
    public static void accept(ServerPlayer ally) {
        MinecraftServer server = ally.getServer();
        if (server == null) return;
        UUID ownerId = PendingInvites.take(ally.getUUID(), ally.level().getGameTime());
        ServerPlayer owner = ownerId == null ? null : server.getPlayerList().getPlayer(ownerId);
        if (owner == null) { ally.sendSystemMessage(Component.literal("No offer is waiting for you (or it ran out).").withStyle(ChatFormatting.GRAY)); return; }
        DjinnLedger ledger = DjinnLedger.get(server);
        Djinn d = DjinnState.of(owner).djinn();
        if (d == null) { ally.sendSystemMessage(Component.literal(owner.getName().getString() + " no longer holds that Djinn.").withStyle(ChatFormatting.GRAY)); return; }
        if (com.newuniverse.nusmp.relics.djinn.dark.DarkDjinn.active(DjinnState.of(owner))) {
            ally.sendSystemMessage(Component.literal(owner.getName().getString() + "'s Djinn has since drunk the black rukh and cannot be lent.").withStyle(ChatFormatting.GRAY));
            return;
        }
        Verdict v = HouseholdRules.invite(true, false, ledger.household(d).size(), ledger.isMember(d, ally.getUUID()), Bonds.mask(ally), Bonds.multiBond(ally));
        if (v != Verdict.OK) { ally.sendSystemMessage(Component.literal(shareText(v, ally)).withStyle(ChatFormatting.RED)); return; }
        if (!ledger.addMember(d, ally.getUUID())) return;
        DjinnState.of(ally).lend(owner.getUUID(), d);
        DjinnEngine.restore(ally);
        ally.sendSystemMessage(Component.literal("You share " + d.fullName() + " with " + owner.getName().getString() + ". Press the Djinn key (default J) for the Lesser Summon.").withColor(d.color()));
        owner.sendSystemMessage(Component.literal(ally.getName().getString() + " accepts a fraction of " + d.displayName() + ".").withColor(d.color()));
    }

    /** The ally gives the Djinn back. */
    public static void leave(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        DjinnState s = DjinnState.of(p);
        Djinn d = s.lentDjinn();
        if (server == null || d == null) { p.sendSystemMessage(Component.literal("You share no Djinn.").withStyle(ChatFormatting.GRAY)); return; }
        DjinnLedger.get(server).removeMember(d, p.getUUID());
        DjinnEngine.dropLent(p);
    }

    /** The owner takes the Djinn back from an ally. */
    public static void unshare(ServerPlayer owner, ServerPlayer ally) {
        MinecraftServer server = owner.getServer();
        Djinn d = DjinnState.of(owner).djinn();
        if (server == null || d == null) { owner.sendSystemMessage(Component.literal("You hold no Djinn.").withStyle(ChatFormatting.GRAY)); return; }
        if (!DjinnLedger.get(server).removeMember(d, ally.getUUID())) {
            owner.sendSystemMessage(Component.literal(ally.getName().getString() + " does not share your Djinn.").withStyle(ChatFormatting.GRAY));
            return;
        }
        DjinnEngine.dropLent(ally);
        owner.sendSystemMessage(Component.literal(d.displayName() + " is called back from " + ally.getName().getString() + ".").withColor(d.color()));
    }
}
