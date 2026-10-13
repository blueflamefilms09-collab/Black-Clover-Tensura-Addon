package com.newuniverse.nusmp.relics.djinn;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.book.ForbiddenMagic;
import com.newuniverse.nusmp.book.SpiritBond;
import com.newuniverse.nusmp.book.SpiritSlots;
import com.newuniverse.nusmp.multiverse.SpiritLordSkill;
import com.newuniverse.nusmp.relics.Relics;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * The exclusive bond, read from the real state of the mod. A player holds ONE of:
 * <ul>
 *   <li>a <b>Black Clover Spirit</b>: the spirit bond ({@link SpiritBond#bonded}), the Spirit Lord skill, or the owner's slot of any of the four
 *       elemental spirits ({@link SpiritSlots});</li>
 *   <li>a <b>Devil</b>: a devil contract (the pact record of {@link ForbiddenMagic}) or a grimoire that a devil lives in (its "Devil" tag: a
 *       five-leaf book, the Anti-Magic book with Liebe). Not every grimoire has a devil, so "holds a devil" is exactly this;</li>
 *   <li>a <b>Djinn</b>: one this player owns, or one lent to them as a household member.</li>
 * </ul>
 * Unless the multi-bond relic (Astralis, the Eye of the Universe) is worn and working: then more than one kind is allowed, at the price of
 * {@link BondRules}. The existing binding methods ask {@link #refuse} first, so the rule holds both ways.
 */
public final class Bonds {
    private Bonds() {}

    public enum Kind {
        SPIRIT(BondRules.SPIRIT), DEVIL(BondRules.DEVIL), DJINN(BondRules.DJINN);
        private final int bit;
        Kind(int bit) { this.bit = bit; }
        public int bit() { return bit; }
    }

    /** The registry path of the one relic that allows more than one bond. */
    public static final String MULTI_BOND_RELIC = "astralis_the_eye_of_the_universe";

    /** The persistent key under which the Infernal Contract (Devil Contract relic) records the contract a player signed. */
    public static final String INFERNAL_CONTRACT = "nusmp_infernal_contract";

    private static final String[] SPIRITS = {"Salamander", "Undine", "Sylph", "Gnome"};

    /** The kinds of bond this player holds now. */
    public static Set<Kind> held(ServerPlayer p) {
        Set<Kind> out = EnumSet.noneOf(Kind.class);
        if (holdsSpirit(p)) out.add(Kind.SPIRIT);
        if (holdsDevil(p)) out.add(Kind.DEVIL);
        if (holdsDjinn(p)) out.add(Kind.DJINN);
        return out;
    }

    /** The same as a bit mask for {@link BondRules}. */
    public static int mask(ServerPlayer p) {
        int m = 0;
        for (Kind k : held(p)) m |= k.bit();
        return m;
    }

    public static boolean holds(ServerPlayer p, Kind k) {
        return switch (k) {
            case SPIRIT -> holdsSpirit(p);
            case DEVIL -> holdsDevil(p);
            case DJINN -> holdsDjinn(p);
        };
    }

    private static boolean holdsSpirit(ServerPlayer p) {
        if (SpiritBond.bonded(p)) return true;
        var lord = SpiritLordSkill.instance(p);
        if (lord.isPresent() && !"Anti".equals(lord.get().getOrCreateTag().getString("Spirit"))) return true;      // the Anti-Magic lord is the devil of the grimoire: one bond, counted as a Devil
        if (p.getServer() == null) return false;
        SpiritSlots slots = SpiritSlots.get(p.getServer());
        UUID id = p.getUUID();
        for (String s : SPIRITS) if (id.equals(slots.owner(s))) return true;
        return false;
    }

    private static boolean holdsDevil(ServerPlayer p) {
        if (ForbiddenMagic.devilOf(p) != null) return true;
        if (!p.getPersistentData().getCompound(net.minecraft.world.entity.player.Player.PERSISTED_NBT_TAG).getString(INFERNAL_CONTRACT).isEmpty()) return true;      // a signed Infernal Contract (wave 0) is a Devil bond
        return GrimoirePages.grimoireOf(p).map(i -> !i.getOrCreateTag().getString("Devil").isEmpty()).orElse(false) || AntiMagic.lord(p).isPresent();
    }

    private static boolean holdsDjinn(ServerPlayer p) {
        DjinnState s = DjinnState.of(p);
        return s.owns() || s.lent();
    }

    /** Is the multi-bond relic worn and working (attuned where it needs that)? */
    public static boolean multiBond(ServerPlayer p) { return Relics.worn(p, MULTI_BOND_RELIC); }

    /** May this player take a bond of this kind now? */
    public static boolean canBind(ServerPlayer p, Kind k) { return BondRules.canBind(mask(p), k.bit(), multiBond(p)); }

    /**
     * The one-line guard for the existing binding moments: if the player may not take this bond, tells them why and returns true (the caller then
     * returns without binding). {@code what} reads in "You cannot take a devil contract while you hold ...".
     */
    public static boolean refuse(ServerPlayer p, Kind k, String what) {
        if (canBind(p, k)) return false;
        StringBuilder held = new StringBuilder();
        for (Kind h : held(p)) {
            if (h == k) continue;
            if (held.length() > 0) held.append(" and ");
            held.append(switch (h) { case SPIRIT -> "a Spirit"; case DEVIL -> "a Devil"; case DJINN -> "a Djinn"; });
        }
        p.displayClientMessage(Component.literal("You cannot take " + what + " while you hold " + held + ". A mage holds one bond: a Spirit, a Devil or a Djinn.")
                .withStyle(ChatFormatting.RED), false);
        return true;
    }

    // ---------------------------------------------------------------- the price of more than one
    /** The factor every bond's Magoi drain is multiplied by (1.4 for a multi-bonded player). */
    public static double drainMultiplier(ServerPlayer p) { return BondRules.drainMultiplier(mask(p), multiBond(p)); }

    /** A trust gain after the multi-bond price (halved per extra bond; never below 1 for a gain). */
    public static int scaleTrustGain(ServerPlayer p, int gain) {
        if (gain <= 0 || !multiBond(p)) return gain;
        return Math.max(1, (int) Math.round(gain * BondRules.trustGainMultiplier(mask(p), multiBond(p))));
    }

    /** True while natural mana regeneration is off for this player (multi-bonded, in Full Djinn Equip). */
    public static boolean regenBlocked(ServerPlayer p) {
        return BondRules.regenBlocked(mask(p), multiBond(p), DjinnState.of(p).phase());
    }
}
