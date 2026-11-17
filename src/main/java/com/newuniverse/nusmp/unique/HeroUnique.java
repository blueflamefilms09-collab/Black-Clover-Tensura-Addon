package com.newuniverse.nusmp.unique;

import com.newuniverse.nusmp.heroweapon.HeroConfig;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.bond.HeroRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Why: a Holy Weapon (Legendary Shield, Sword, Spear, Bow) is one per world while {@code holyUnique} is on, but it is not an item that can be lost: the bond on the player is the truth and the item in the hand is a projection
 * that is made again every few ticks. The only way a Holy Weapon is lost for good is a holder who never comes back. So the guardian watches holders, not items: once a minute, a holder who has been offline for the
 * configured days frees the weapon in the registry (the weapon is back with its normal source, the acquisition ritual, for the next player). If that player ever returns while somebody else now holds it, their bond is
 * dissolved (no duplicate); if nobody took it they keep it. Cost: one registry read per Holy Weapon (four) a minute and one map write per online holder.
 */
final class HeroUnique {
    private HeroUnique() {}

    static final String PREFIX = "hero:";

    static boolean owns(String id) { return kind(id) != null; }

    static HeroKind kind(String id) {
        if (id == null || !id.startsWith(PREFIX)) return null;
        String item = id.substring(PREFIX.length());
        for (HeroKind k : HeroKind.values()) if (k.isHoly() && k.itemId().equals(item)) return k;
        return null;
    }

    static List<String> ids() {
        List<String> out = new ArrayList<>(4);
        for (HeroKind k : HeroKind.values()) if (k.isHoly()) out.add(PREFIX + k.itemId());
        return out;
    }

    /** The holders of a Holy Weapon id, as a readable list. */
    static String holders(MinecraftServer sv, String id) {
        HeroKind k = kind(id);
        if (k == null) return "none";
        List<String> names = new ArrayList<>(2);
        for (UUID h : HeroRegistry.holdersOf(sv, k)) names.add(UniqueGuardian.holderName(sv, h));
        return names.isEmpty() ? "nobody" : String.join(", ", names);
    }

    /** Once a minute: remember who was seen, free the weapon of a holder who is gone for good. */
    static void minute(MinecraftServer sv, UniqueSaved saved, long nowMs) {
        if (!HeroConfig.holyUnique()) return;
        long absence = UniqueRules.absenceMs(UniqueConfig.absenceDays());
        for (HeroKind k : HeroKind.values()) {
            if (!k.isHoly()) continue;
            boolean freed = false;
            for (UUID h : HeroRegistry.holdersOf(sv, k)) {
                if (sv.getPlayerList().getPlayer(h) != null) {
                    saved.heroSeen.put(h, nowMs);
                    saved.setDirty();
                    continue;
                }
                Long seen = saved.heroSeen.get(h);
                if (seen == null) {                                              // first time the guardian meets this holder offline: the clock starts now
                    saved.heroSeen.put(h, nowMs);
                    saved.setDirty();
                } else if (absence > 0 && nowMs - seen >= absence) {
                    saved.heroDissolve.put(h, k.itemId());
                    saved.heroSeen.remove(h);
                    freed = true;
                }
            }
            if (freed) {
                HeroRegistry.clear(sv, k);
                saved.setDirty();
                UniqueGuardian.announce(sv, "unique.msg.ops.hero", "[Unique Guardian] The %s lost its holder (away for good) and is back with its normal source: it can be claimed again.", k.display());
            }
        }
    }

    /** A holder returns: if their weapon was freed and somebody else holds it now, the bond dissolves. */
    static void onLogin(ServerPlayer p, UniqueSaved saved) {
        if (p.getServer() == null) return;
        String weapon = saved.heroDissolve.remove(p.getUUID());
        if (weapon == null) return;
        saved.setDirty();
        HeroKind mine = HeroBonds.kindOf(p);
        if (mine == null || !mine.itemId().equals(weapon)) return;
        for (UUID h : HeroRegistry.holdersOf(p.getServer(), mine)) {
            if (!h.equals(p.getUUID())) {
                HeroBonds.release(p);
                p.sendSystemMessage(Component.translatableWithFallback("unique.msg.hero_dissolved", "Your bond has dissolved: the weapon was returned to the world while you were away and has a new wielder."));
                return;
            }
        }
    }

    /** An operator forces the weapon back to the pool now. Returns the number of holders freed. */
    static int force(MinecraftServer sv, UniqueSaved saved, HeroKind k) {
        List<UUID> holders = HeroRegistry.holdersOf(sv, k);
        for (UUID h : holders) saved.heroDissolve.put(h, k.itemId());
        int n = HeroRegistry.clear(sv, k);
        saved.setDirty();
        return n;
    }

    /** At logout: a bonded holder is seen now. */
    static void onLogout(ServerPlayer p, UniqueSaved saved) {
        if (HeroBonds.kindOf(p) == null) return;
        saved.heroSeen.put(p.getUUID(), System.currentTimeMillis());
        saved.setDirty();
    }
}
