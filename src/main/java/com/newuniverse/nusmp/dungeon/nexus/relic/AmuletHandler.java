package com.newuniverse.nusmp.dungeon.nexus.relic;

import io.github.manasmods.tensura.registry.attribute.TensuraAttributes;
import io.github.manasmods.tensura.storage.TensuraStorages;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The Amulet of the Juggernaut's Blindness (docs/living_dungeon.md section 7). Worn in a Dungeon Relic slot and attuned it masks its wearer's mana
 * signature: the Sentient Dungeon's immune system asks {@link Relics#masksMana} before it answers a massive spell, and does not answer theirs.
 * Its curse, Muffled Mana, lives here: the wearer's natural mana regeneration is halved, by a transient modifier on Tensura's magicule regeneration
 * multiplier that is put on, once a second, while the Amulet is worn and attuned, and taken off the moment it is not (and on death, a logout, a
 * change of dimension and the server stopping), so it can never outlast the Amulet. A player for whom that attribute does not exist gets the
 * fallback of {@link RegenMuffle}: half of what the magicule store rises by naturally is taken back.
 */
final class AmuletHandler {
    private AmuletHandler() {}

    static final ResourceLocation MUFFLE_ID = ResourceLocation.fromNamespaceAndPath("nusmp", "relic_muffled_mana");
    /** Halves the regeneration: the total is multiplied by one minus a half. */
    static final double MUFFLE = -0.5;

    /** The wearers the modifier is on right now (dropped when the server stops). */
    private static final Map<UUID, ServerPlayer> MUFFLED = new HashMap<>();
    /** The fallback's last look at each wearer's magicule store. */
    private static final Map<UUID, Double> LAST = new HashMap<>();

    /** Is anything of the Amulet's still on this player? (The tick must keep looking at them until it is gone.) */
    static boolean active(UUID id) { return MUFFLED.containsKey(id) || LAST.containsKey(id); }

    private static boolean wearing(int mask) { return (mask & Relic.AMULET_OF_THE_JUGGERNAUTS_BLINDNESS.bit()) != 0; }

    // ---------------------------------------------------------------- once a second
    /** Brings the curse in line with what the player wears: on while the Amulet is worn and attuned, off the moment it is not. */
    static void tick(ServerPlayer p, int mask) {
        UUID id = p.getUUID();
        boolean worn = wearing(mask);
        if (!worn && !active(id)) return;                                           // nothing worn and nothing left to take off
        AttributeInstance inst = p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER);
        if (inst == null) {
            fallback(p, worn);
            return;
        }
        LAST.remove(id);
        boolean has = inst.hasModifier(MUFFLE_ID);
        if (worn) {
            if (!has) {
                inst.addTransientModifier(new AttributeModifier(MUFFLE_ID, MUFFLE, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                if (RelicState.due(id, "amulet_on", p.level().getGameTime(), 1200)) {
                    p.displayClientMessage(Component.literal("The Amulet muffles your mana signature: your natural mana regeneration is halved.")
                            .withStyle(ChatFormatting.DARK_PURPLE), true);
                }
            }
            MUFFLED.put(id, p);
        } else {
            if (has) inst.removeModifier(MUFFLE_ID);
            MUFFLED.remove(id);
        }
    }

    /** No regeneration attribute: take back half of every natural rise of the store, seen a second apart. */
    private static void fallback(ServerPlayer p, boolean worn) {
        UUID id = p.getUUID();
        if (!worn) {
            LAST.remove(id);
            return;
        }
        var ex = TensuraStorages.getExistenceFrom(p);
        if (ex == null) return;
        double now = ex.getMagicule();
        Double was = LAST.get(id);
        if (was != null) {
            double back = RegenMuffle.takeBack(was, now, EnergyHelper.getMaxMagicule(p));
            if (back > 0.0) {
                now -= back;
                ex.setMagicule(now);
                ex.markDirty();
            }
        }
        LAST.put(id, now);
    }

    // ---------------------------------------------------------------- the end of a stay
    private static void remove(ServerPlayer p) {
        AttributeInstance inst = p.getAttribute(TensuraAttributes.MAGICULE_REGENERATION_MULTIPLIER);
        if (inst != null && inst.hasModifier(MUFFLE_ID)) inst.removeModifier(MUFFLE_ID);
    }

    /** Takes the curse off this player, whatever they wear (death, logout, a change of dimension, a new body). */
    static void clear(ServerPlayer p) {
        UUID id = p.getUUID();
        ServerPlayer before = MUFFLED.remove(id);
        remove(p);
        if (before != null && before != p) remove(before);
        LAST.remove(id);
    }

    /** The server is stopping: nobody is muffled, and no player is kept. */
    static void clearAll() {
        for (ServerPlayer p : MUFFLED.values()) remove(p);
        MUFFLED.clear();
        LAST.clear();
    }
}
