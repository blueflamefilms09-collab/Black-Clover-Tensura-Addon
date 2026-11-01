package com.newuniverse.nusmp.devil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.util.EnumSet;

/**
 * Why: everything the Devil Contract system remembers about one player (3.48.0). The saved part is the contract itself (tier, attribute, the level, the end of the union, the end of its cooldown, the
 * sacrifices); the live part (the key toggle, what was last sent, whether flight is on) is never saved, so every login starts with the level switched off. Times are absolute game times, so a cooldown or a
 * union keeps counting while the player is away. Only the server code writes it; the client holds its own copy of what the server sent.
 */
public final class DevilContract {
    // ---- saved ----
    public DevilTier tier = DevilTier.NONE;
    public DevilAttribute attribute = DevilAttribute.NONE;
    /** 0 to 100. */
    public float assimilation;
    /** Game time the union ends at (0 = not in the union). */
    public long unionUntil;
    /** Game time the union may begin again (0 = it may). */
    public long unionReadyAt;
    public final EnumSet<Sacrifice> sacrifices = EnumSet.noneOf(Sacrifice.class);

    // ---- live (not saved) ----
    public boolean assimilating;
    public boolean flightOn;
    /** What was last sent to the owner and to the players who can see them (-1 = nothing yet). */
    public int sentSelf = -1, sentVisual = -1;
    public long lastAsk = Long.MIN_VALUE / 2;

    /** Nothing worth saving: a player who never had a contract keeps no attachment. */
    public boolean isEmpty() { return tier == DevilTier.NONE && sacrifices.isEmpty() && unionReadyAt == 0 && unionUntil == 0 && assimilation == 0; }

    public boolean hasContract() { return tier != DevilTier.NONE; }

    public boolean inUnion() { return DevilRules.inUnion(unionUntil); }

    public int sacrificeMask() {
        int m = 0;
        for (Sacrifice s : sacrifices) m |= s.bit();
        return m;
    }

    public int eyes() {
        int n = 0;
        for (Sacrifice s : sacrifices) if (s.isEye()) n++;
        return n;
    }

    /** Back to the state of a body that just woke up: level and live flags off, the cooldown and the contract kept. */
    public void resetLive() {
        assimilation = 0;
        assimilating = false;
        unionUntil = 0;
        flightOn = false;
        sentSelf = -1;
        sentVisual = -1;
    }

    // ---------------------------------------------------------------- tag format
    public static CompoundTag toTag(DevilContract c) {
        CompoundTag t = new CompoundTag();
        t.putInt("v", 1);
        t.putString("tier", c.tier.name());
        t.putString("attr", c.attribute.name());
        t.putFloat("level", c.assimilation);
        t.putLong("union", c.unionUntil);
        t.putLong("ready", c.unionReadyAt);
        ListTag list = new ListTag();
        for (Sacrifice s : c.sacrifices) list.add(StringTag.valueOf(s.name()));
        t.put("sac", list);
        return t;
    }

    public static DevilContract fromTag(CompoundTag t) {
        DevilContract c = new DevilContract();
        c.tier = DevilTier.byName(t.getString("tier"));
        c.attribute = DevilAttribute.byName(t.getString("attr"));
        c.assimilation = DevilRules.clampF(t.getFloat("level"), 0f, 100f);
        c.unionUntil = Math.max(0L, t.getLong("union"));
        c.unionReadyAt = Math.max(0L, t.getLong("ready"));
        ListTag list = t.getList("sac", Tag.TAG_STRING);
        for (int i = 0; i < list.size() && i < Sacrifice.values().length * 2; i++) {
            Sacrifice s = Sacrifice.byName(list.getString(i));
            if (s != null) c.sacrifices.add(s);
        }
        return c;
    }
}
