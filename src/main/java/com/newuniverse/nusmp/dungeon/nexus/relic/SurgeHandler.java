package com.newuniverse.nusmp.dungeon.nexus.relic;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;

/**
 * The cost of a Wild Magic Surge (the Gambler's Page, docs/living_dungeon.md section 7): a tenth of the user's mana pool until the day ends. The game
 * time it ends is kept in the player's persistent data ({@link SurgePenalty}); the modifier on Tensura's maximum magicule is transient, so it never
 * reaches the save by itself, and is put back from that stamp when the player logs in, respawns, comes back from another dimension and, to be sure,
 * once a second, and is taken off when the time comes. A relog, a death or a server restart does not wash the surge away.
 */
final class SurgeHandler {
    private SurgeHandler() {}

    private static final ResourceLocation PENALTY_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_wild_surge");
    private static final String UNTIL = "SurgeUntil";

    /** The game time the player's penalty ends, or 0 if they have none. */
    static long stored(Player p) {
        CompoundTag d = Attunement.data(p, false);
        return d == null ? 0L : d.getLong(UNTIL);
    }

    /** A 1 was rolled: the penalty starts now and runs for a day (a second surge restarts the day, it does not stack). */
    static void start(ServerPlayer p) {
        Attunement.data(p, true).putLong(UNTIL, SurgePenalty.until(p.level().getGameTime()));
        sync(p);
    }

    /**
     * Puts the modifier on, or takes it off, to match the saved expiry; an expired stamp is forgotten. Cheap (a few lookups): the tick calls it
     * once a second for every player.
     */
    static void sync(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(TensuraAttributes.MAX_MAGICULE);
        if (inst == null) return;
        long now = p.level().getGameTime(), until = stored(p);
        boolean on = SurgePenalty.active(now, until), has = inst.hasModifier(PENALTY_ID);
        if (on && !has) inst.addTransientModifier(new AttributeModifier(PENALTY_ID, -WildSurge.PENALTY, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        else if (!on && has) inst.removeModifier(PENALTY_ID);
        if (!on && until != 0L) {
            CompoundTag d = Attunement.data(p, false);
            if (d != null) d.remove(UNTIL);
            if (until <= now) p.displayClientMessage(Component.literal("The Wild Surge's toll is lifted: your mana pool is whole again.").withStyle(ChatFormatting.GRAY), true);
        }
    }

    /** A new body (a death, a return from the End): the expiry goes with the player; the modifier follows at the next {@link #sync}. */
    static void copy(Player from, Player to) {
        long until = stored(from);
        if (until != 0L) Attunement.data(to, true).putLong(UNTIL, until);
    }
}
