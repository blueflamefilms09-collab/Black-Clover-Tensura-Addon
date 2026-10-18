package com.newuniverse.nusmp.heroweapon.rules;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.quests.NusmpEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the third restriction of the Holy Weapons (the owner's text): the heroes gain no experience when they stand close to one another, because their weapons
 * interfere. One question, answered in one place, for every place that grants experience (vanilla orbs, kill experience, the weapon's own experience): is this
 * player's gain blocked right now? The answer comes from a set of blocked players that is rebuilt once a second from the Holy wielders online (the geometry is
 * {@link InterferenceGeometry}), so the hot path (every orb picked up, every blow) is one hash lookup. Seven Star and Vassal wielders are never blocked and
 * never block, and neither does a wielder in creative or spectator mode (an operator testing must not silence the players). The line "The Legendary Weapons
 * repel each other" is shown when a gain is refused, at most once a minute per player, and fires the quest hook {@code hero.repelled}.
 */
public final class HeroInterference {
    private HeroInterference() {}

    private static volatile Set<UUID> blocked = Set.of();
    private static final Map<UUID, Long> NOTICE = new HashMap<>();

    /**
     * True while a Holy wielder has another Holy wielder within the repel radius (config) in the same dimension, as of the last once-a-second refresh. Always
     * false on the client, when the rule is switched off, or for anyone who is not a Holy wielder.
     */
    public static boolean blocked(Player player) {
        if (player == null || player.level().isClientSide || !HeroConfig.enabled() || !HeroConfig.repelXp()) return false;
        return blocked.contains(player.getUUID());
    }

    /**
     * Rebuilds the blocked set from these players (the server tick passes everyone online; a self-test passes its stand-ins) and returns it. A wielder counts
     * only while alive and in survival or adventure mode, and only for a family that repels experience ({@code HeroClass.repelsExperience}).
     */
    public static Set<UUID> refresh(Collection<? extends ServerPlayer> players) {
        if (!HeroConfig.enabled() || !HeroConfig.repelXp()) {
            blocked = Set.of();
            return blocked;
        }
        List<InterferenceGeometry.Wielder> holy = new ArrayList<>();
        for (ServerPlayer p : players) {
            if (!p.isAlive() || p.isSpectator() || p.isCreative()) continue;
            HeroKind k = HeroBonds.kindOf(p);
            if (k == null || !k.heroClass().repelsExperience()) continue;
            holy.add(new InterferenceGeometry.Wielder(p.getUUID().toString(), p.level().dimension().location().toString(), p.getX(), p.getY(), p.getZ()));
        }
        Set<UUID> out = new HashSet<>();
        for (String id : InterferenceGeometry.blocked(holy, HeroConfig.repelRadius())) out.add(UUID.fromString(id));
        blocked = Set.copyOf(out);
        return blocked;
    }

    /** A gain of this player was refused because of the rule: tell them (at most once per configured minute) and fire the quest hook. */
    public static void noteRepelled(ServerPlayer p) {
        long now = p.level().getGameTime();
        Long last = NOTICE.get(p.getUUID());
        if (!Spacing.due(now, last == null ? Spacing.LONG_AGO : last, HeroConfig.repelNoticeSeconds() * 20)) return;
        NOTICE.put(p.getUUID(), now);
        if (HeroConfig.showMessages()) p.displayClientMessage(Component.translatable("hero.msg.repel"), true);
        NusmpEvents.fire(p, "hero.repelled");
    }

    /** Forgets one player's notice time (they left). */
    public static void forget(UUID player) { NOTICE.remove(player); }

    /** Forgets everything (the server stopped). */
    public static void clear() {
        blocked = Set.of();
        NOTICE.clear();
    }
}
