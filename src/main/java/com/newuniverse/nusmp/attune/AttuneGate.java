package com.newuniverse.nusmp.attune;

import com.newuniverse.nusmp.devil.DevilContract;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import com.newuniverse.nusmp.spiritcontrol.SpiritHandle;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Why: the owner (3.95.0): "players shouldn't be able to equip more than two of any of the things ... players can still not use a devil and a spirit at the same time". The damage suppression alone
 * left the abilities working, so each system's server entry point (Beast Will form and ability, Djinn phase up, Devil let in and its host blow, Spirit summon, ability and combat cast) asks here first.
 * The decision is {@link AttuneRules#gate} (pure, tested); this only reads the player ({@link AttunePowers#out}: a few attachment reads, one map lookup per registered spirit) and says why on the
 * action bar. Off ({@code enabled = false} or {@code equipLimit = false}): always yes, the old behaviour. Never throws: an error lets the power work.
 */
public final class AttuneGate {
    private AttuneGate() {}

    public static boolean beast(ServerPlayer p, String willId) { return willId == null || willId.isEmpty() || allowed(p, () -> AttunePowers.beast(willId)); }

    public static boolean djinn(ServerPlayer p, Djinn d) { return d == null || allowed(p, () -> AttunePowers.djinnPower(p, d)); }

    public static boolean devil(ServerPlayer p, DevilContract c) { return c == null || !c.hasContract() || allowed(p, () -> AttunePowers.devil(c)); }

    public static boolean spirit(ServerPlayer p, SpiritHandle h) { return h == null || allowed(p, () -> AttunePowers.spirit(h)); }

    /** May {@code p} use this power now? A refusal tells the player (action bar) to meditate. */
    static boolean allowed(ServerPlayer p, java.util.function.Supplier<AttuneRules.Power> use) {
        if (p == null || !AttuneConfig.enabled() || !AttuneConfig.equipLimit()) return true;
        try {
            AttuneStore.State s = AttuneStore.peek(p);
            List<String> attuned = s == null ? List.of() : s.slots;
            String why = AttuneRules.gate(AttunePowers.out(p), use.get(), attuned);
            if (why == null) return true;
            p.displayClientMessage(why.equals("limit")
                    ? Component.translatableWithFallback("attune.msg.limit", "Only two powers at once. Meditate (/meditate) to choose which two.").withStyle(ChatFormatting.YELLOW)
                    : Component.translatableWithFallback("attune.msg.limit_devil_spirit", "A Devil and a Spirit will not work together. Meditate (/meditate) to choose one.").withStyle(ChatFormatting.YELLOW), true);
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }
}
