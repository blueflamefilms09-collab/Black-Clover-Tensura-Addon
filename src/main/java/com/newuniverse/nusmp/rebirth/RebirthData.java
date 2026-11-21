package com.newuniverse.nusmp.rebirth;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Why: the three per-player values of the feature live in the player's persistent data (survives death and relog): the mark {@code nusmp_rebirth}, the tick of the last rite and the read-only resolve score. */
public final class RebirthData {
    private RebirthData() {}

    public static final String MARK = "nusmp_rebirth", LAST_AT = "nusmp_rebirth_at", RESOLVE = "nusmp_resolve";

    private static CompoundTag root(Player p) {
        CompoundTag pd = p.getPersistentData();
        if (!pd.contains(Player.PERSISTED_NBT_TAG, 10)) pd.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        return pd.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static RebirthRules.Mode mode(Player p) { return RebirthRules.parse(root(p).getString(MARK)); }

    public static void setMode(Player p, RebirthRules.Mode m) {
        if (m == RebirthRules.Mode.NONE) root(p).remove(MARK); else root(p).putString(MARK, m.name());
    }

    /** Game tick of the last rite, or -1 if the player never used one. */
    public static long lastAt(Player p) { return root(p).contains(LAST_AT) ? root(p).getLong(LAST_AT) : -1; }

    public static void setLastAt(Player p, long tick) { root(p).putLong(LAST_AT, tick); }

    /** The questionnaire's resolve score if another system wrote one (an int under {@code nusmp_resolve}, here or in the wizard's section), else {@link RebirthRules#RESOLVE_UNKNOWN}. Read only. */
    public static int resolve(Player p) {
        CompoundTag r = root(p);
        if (r.contains(RESOLVE, 99)) return r.getInt(RESOLVE);
        if (r.contains("nusmp_wizard", 10) && r.getCompound("nusmp_wizard").contains(RESOLVE, 99)) return r.getCompound("nusmp_wizard").getInt(RESOLVE);
        return RebirthRules.RESOLVE_UNKNOWN;
    }

    /** Lets the Tower Wizard treat this player as summoned (the wizard's own flag), so a mage who got a grimoire by another road can still be questioned. */
    static void markSummoned(Player p) {
        CompoundTag r = root(p);
        if (!r.contains("nusmp_wizard", 10)) r.put("nusmp_wizard", new CompoundTag());
        r.getCompound("nusmp_wizard").putBoolean("Summoned", true);
    }
}
