package com.newuniverse.nusmp.heroweapon.bond;

import com.newuniverse.nusmp.core.magic.profile.SafeCast;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import com.newuniverse.nusmp.heroweapon.curse.HeroCurse;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.rules.HeroInterference;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: the client draws the weapon (the HUD, the tooltip, the Weapon Book's header) from a copy of the bond, and the bond changes in many places that do not
 * know about the client: the forms system learns a form, the gem absorbs an item, a blow adds experience, the curse changes the look. Rather than every one of
 * them calling a sync, this class builds the copy from the bond and the sync is checked once a second per bonded player: the copy is built, compared with the
 * last one sent, and sent only if it differs (so a quiet second costs one comparison and no packet, and "every change, at most once a second" holds by
 * construction). The events that matter at once (login, respawn, dimension change, bind, release) send immediately. A fake player (a self-test) has no
 * connection and is skipped; a send that throws is logged once and never reaches the game.
 */
public final class HeroSync {
    private HeroSync() {}

    /** At most this many absorbed items travel in the copy (the largest counts); the totals always count all of them. */
    private static final int MAX_ABSORBED = 24;

    private static final Map<UUID, HeroNet.Sync> LAST = new HashMap<>();

    /** Can a packet go to this player at all (a fake player of a self-test cannot)? */
    static boolean canSend(ServerPlayer p) { return !(p instanceof FakePlayer) && p.connection != null; }

    // ================================================================ building
    /** The copy of the player's bond for the client ({@link HeroNet.Sync#none()} when they have no weapon). */
    public static HeroNet.Sync build(ServerPlayer p) {
        HeroBond b = HeroBonds.of(p);
        if (!b.isBound()) return HeroNet.Sync.none();
        String formName = b.form();
        List<String> slots = List.of("", "", "", "", "");
        try {
            HeroForm f = HeroForms.active(b);
            formName = f.name();
            List<String> s = new ArrayList<>(5);
            for (HeroTrigger t : HeroTrigger.values()) {
                String id = f.skill(t);
                s.add(id == null ? "" : id);
            }
            slots = List.copyOf(s);
        } catch (RuntimeException broken) {
            // a form that cannot be read leaves the HUD without technique names; the sync itself still goes
        }
        int flags = 0;
        if (HeroInterference.blocked(p)) flags |= HeroNet.FLAG_REPELLED;
        try {
            if (HeroCurse.active(p)) flags |= HeroNet.FLAG_CURSED;
            if (HeroCurse.locked(p)) flags |= HeroNet.FLAG_LOCKED;
        } catch (RuntimeException broken) {
            // the curse system is another builder's; its failure must not stop the weapon's own sync
        }
        List<HeroNet.Entry> mastery = new ArrayList<>();
        for (Map.Entry<String, Integer> e : b.masteryMap().entrySet()) mastery.add(new HeroNet.Entry(e.getKey(), e.getValue()));
        mastery.sort((x, y) -> x.key().compareTo(y.key()));
        List<HeroNet.Entry> absorbed = new ArrayList<>();
        long total = 0L;
        for (Map.Entry<String, Integer> e : b.absorbedMap().entrySet()) {
            absorbed.add(new HeroNet.Entry(e.getKey(), e.getValue()));
            total += e.getValue();
        }
        int kinds = absorbed.size();
        absorbed.sort((x, y) -> x.value() != y.value() ? Integer.compare(y.value(), x.value()) : x.key().compareTo(y.key()));
        if (absorbed.size() > MAX_ABSORBED) absorbed = new ArrayList<>(absorbed.subList(0, MAX_ABSORBED));
        return new HeroNet.Sync(b.kind().itemId(), b.exp(), b.form(), formName, b.summoned(), flags, List.copyOf(b.unlocked()), List.copyOf(mastery), kinds, total,
                List.copyOf(absorbed), slots);
    }

    // ================================================================ sending
    /** Builds and sends the copy at once (login, respawn, dimension change, bind). */
    public static void sendNow(ServerPlayer p) {
        if (!canSend(p)) return;
        SafeCast.tick("hero.sync", () -> {
            HeroNet.Sync s = build(p);
            LAST.put(p.getUUID(), s);
            PacketDistributor.sendToPlayer(p, s);
        });
    }

    /** Tells the client the player has no weapon (after a release). */
    public static void sendEmpty(ServerPlayer p) {
        if (!canSend(p)) return;
        SafeCast.tick("hero.sync", () -> {
            LAST.put(p.getUUID(), HeroNet.Sync.none());
            PacketDistributor.sendToPlayer(p, HeroNet.Sync.none());
        });
    }

    /** The once-a-second check: sends the copy only when it differs from the last one sent. Players who never had a weapon cost nothing. */
    public static void poll(ServerPlayer p) {
        if (!canSend(p)) return;
        UUID id = p.getUUID();
        boolean bound = HeroBonds.has(p);
        if (!bound && !LAST.containsKey(id)) return;
        SafeCast.tick("hero.sync", () -> {
            HeroNet.Sync s = build(p);
            HeroNet.Sync was = LAST.get(id);
            if (was != null && was.equals(s)) return;
            if (was == null && s.isNone()) return;
            LAST.put(id, s);
            PacketDistributor.sendToPlayer(p, s);
        });
    }

    /** A technique was cast: the client starts its cooldown bar. */
    public static void sendCooldown(ServerPlayer p, String skillId, int ticks) {
        if (!canSend(p)) return;
        SafeCast.tick("hero.sync", () -> PacketDistributor.sendToPlayer(p, new HeroNet.Cooldown(skillId, ticks)));
    }

    /** Forgets what was sent to one player (they left): the next login sends in full. */
    public static void forget(UUID player) { LAST.remove(player); }

    /** Forgets everything (the server stopped). */
    public static void clear() { LAST.clear(); }
}
