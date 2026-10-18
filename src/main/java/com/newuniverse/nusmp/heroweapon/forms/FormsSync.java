package com.newuniverse.nusmp.heroweapon.forms;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBond;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Why: the open Weapon Book must show the truth without the server flooding it. Mastery changes on every blow and a fight is several a second, so state is pushed to
 * a player only while that player has the book open (the client says so by asking to open it, and by closing it), only when something actually changed, and at most
 * every {@code bookRefreshTicks}. "Changed" is decided by comparing the state with the one last shown, so the book also follows changes that other systems make
 * (the curse's despair and corruption, a summon, a form set by the curse system) without any of them having to call here; {@link #markDirty} only forces a push. The
 * forms themselves (large, and fixed until a data reload) are sent once, when the player's copy differs from the server's by checksum: at login, on a reload, when a
 * weapon is bound and when the book is opened. A click answers at once, because the player is waiting for it. This class holds the per-player bookkeeping (who is
 * watching, what each of them was last shown, what checksum each client has) and is cleared when the server stops.
 */
public final class FormsSync {
    private FormsSync() {}

    private static final Set<UUID> WATCHING = new HashSet<>();
    private static final Map<UUID, Boolean> DIRTY = new HashMap<>();
    private static final Map<UUID, FormsNet.State> SHOWN = new HashMap<>();
    private static final Map<UUID, Integer> SENT = new HashMap<>();

    // ------------------------------------------------------------------------------------------------ from the client
    /** The client asked to open the book (the key, or the command). Sends the forms if the client's copy is not current, then the state with {@code open}. */
    static void open(ServerPlayer player, String haveKind, int haveSum) {
        HeroBond bond = HeroBonds.of(player);
        if (!bond.isBound()) {
            FormsFeedback.bar(player, FormsLang.t("hero.forms.no_weapon", "You have no hero weapon."));
            return;
        }
        HeroKind kind = bond.kind();
        boolean current = kind.itemId().equals(haveKind) && haveSum == FormsStore.checksum(kind);
        sendDefs(player, kind, !current);
        WATCHING.add(player.getUUID());
        DIRTY.remove(player.getUUID());
        SHOWN.put(player.getUUID(), stateOf(player, false));
        send(player, stateOf(player, true));
    }

    /** A click in the book. Every action is checked again here; a refusal shows its reason on the action bar. */
    static void act(ServerPlayer player, FormsNet.Act a) {
        if (a.arg() == null || a.arg().length() > 200) return;
        switch (a.action()) {
            case FormsNet.ACT_USE -> HeroForms.setActive(player, a.arg());
            case FormsNet.ACT_LEARN -> {
                Component why = FormLearning.tryLearn(player, a.arg());
                if (why != null) FormsFeedback.bar(player, why);
            }
            case FormsNet.ACT_FEED -> HeroAbsorb.feed(player, a.arg(), a.whole());
            case FormsNet.ACT_EMBRACE -> CurseLink.embrace(player);
            case FormsNet.ACT_CLOSE -> {
                close(player);
                return;
            }
            default -> {
                return;
            }
        }
        if (WATCHING.contains(player.getUUID())) {
            DIRTY.remove(player.getUUID());
            FormsNet.State now = stateOf(player, false);
            SHOWN.put(player.getUUID(), now);
            send(player, now);
        }
    }

    static void close(ServerPlayer player) {
        WATCHING.remove(player.getUUID());
        DIRTY.remove(player.getUUID());
        SHOWN.remove(player.getUUID());
    }

    // ------------------------------------------------------------------------------------------------ from the server
    /** Something about the player's bond changed; if the book is open, it is pushed within the throttle even if it looks the same. Cheap to call. */
    public static void markDirty(ServerPlayer player) {
        if (player != null && WATCHING.contains(player.getUUID())) DIRTY.put(player.getUUID(), Boolean.TRUE);
    }

    /**
     * Every {@code bookRefreshTicks} ticks: each open book is brought up to date when what it shows is no longer what the server holds (or when {@link #markDirty} asked).
     * Comparing the states costs a few small maps for each player who has the book open, which is nobody most of the time.
     */
    static void tick(MinecraftServer server) {
        if (WATCHING.isEmpty()) return;
        int every = Math.max(1, FormsConfig.get(FormsConfig.BOOK_REFRESH_TICKS));
        if (server.getTickCount() % every != 0) return;
        for (UUID id : new ArrayList<>(WATCHING)) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p == null) {
                forget(id);
                continue;
            }
            FormsNet.State now = stateOf(p, false);
            boolean forced = DIRTY.remove(id) != null;
            if (!forced && now.equals(SHOWN.get(id))) continue;
            SHOWN.put(id, now);
            send(p, now);
        }
    }

    /** Sends the forms of the weapon to the player unless the client already has this exact copy ({@code force} sends regardless). */
    static void sendDefs(ServerPlayer player, HeroKind kind, boolean force) {
        if (!FormsFeedback.real(player)) return;
        int sum = FormsStore.checksum(kind);
        Integer had = SENT.get(player.getUUID());
        if (!force && had != null && had == sum) return;
        SENT.put(player.getUUID(), sum);
        PacketDistributor.sendToPlayer(player, new FormsNet.Defs(kind, sum, HeroForms.of(kind)));
    }

    /** After a login or a data reload: the forms of the player's weapon (if any) go to their client. */
    static void syncDefs(ServerPlayer player) {
        HeroBond bond = HeroBonds.of(player);
        if (bond.isBound()) sendDefs(player, bond.kind(), false);
    }

    private static void send(ServerPlayer player, FormsNet.State state) {
        if (FormsFeedback.real(player)) PacketDistributor.sendToPlayer(player, state);
    }

    /** The state of the player's bond as the book shows it. */
    static FormsNet.State stateOf(ServerPlayer p, boolean open) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) return new FormsNet.State(open, "", HeroForm.BASE_ID, 0, false, false, false, 0, 0, "", List.of(), Map.of(), List.of(), Map.of(), List.of());
        List<String> learned = new ArrayList<>(b.unlocked());
        if (!learned.contains(HeroForm.BASE_ID)) learned.add(0, HeroForm.BASE_ID);
        Map<String, Integer> absorbed = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> e : b.absorbedMap().entrySet()) {
            if (absorbed.size() >= 256) break;
            absorbed.put(e.getKey(), e.getValue());
        }
        return new FormsNet.State(open, b.kind().itemId(), b.form(), b.exp(), b.summoned(), HeroCurse.locked(p), HeroCurse.active(p), HeroCurse.despair(p), HeroCurse.corruption(p),
                CurseLink.offered(b), learned, new LinkedHashMap<>(b.masteryMap()), new ArrayList<>(b.bonusesGranted()), absorbed, new ArrayList<>(CurseLink.series(b)));
    }

    /** Called when a weapon is bound (the new weapon's forms are needed on the client) or released. */
    static void bondChanged(ServerPlayer player) {
        SENT.remove(player.getUUID());
        syncDefs(player);
        markDirty(player);
    }

    /** The weapon was released: an open book is told there is nothing left to show (it closes), then the player's bookkeeping goes. */
    static void released(ServerPlayer player) {
        if (WATCHING.remove(player.getUUID())) send(player, new FormsNet.State(false, "", HeroForm.BASE_ID, 0, false, false, false, 0, 0, "", List.of(), Map.of(), List.of(), Map.of(), List.of()));
        forget(player.getUUID());
    }

    static void forget(UUID player) {
        WATCHING.remove(player);
        DIRTY.remove(player);
        SHOWN.remove(player);
        SENT.remove(player);
    }

    static void clear() {
        WATCHING.clear();
        DIRTY.clear();
        SHOWN.clear();
        SENT.clear();
    }
}
