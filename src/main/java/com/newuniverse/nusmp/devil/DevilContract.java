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
    /** 3.76.0: the id of the devil of another world this contract is with ({@link WorldDevil}); "" for the old grimoire devils. */
    public String devil = "";
    /** One of One (docs/devil_double_magic.md): true once this contract was entered in the world's table of holders; a contract older than that (false) is only adopted if its devil is free, never dissolved. */
    public boolean registered;
    /** Creeping corruption of the price (docs/devil_double_magic.md), 0 to 1000 (permille of the screen); saved, so logging out keeps it. */
    public float corruption;

    /** Devil Awakening (docs/devil_mastery.md), saved: the skill's mastery in percent (-1 = not set yet: the server's start value), the UTC day and the effective seconds of use already counted that day, the game time the awakening may begin again, and the mastery of the devils that are held but not active (devil id to percent). */
    public float mastery = -1f;
    public int masteryDay;
    public float masteryUsed;
    public long awakenReadyAt;
    public final java.util.HashMap<String, Float> masteryStash = new java.util.HashMap<>();

    // ---- live (not saved) ----
    /** Devil Awakening, live: the game time the running awakening ends at, the game time of the last use that counts (a blow dealt or taken, an art, a kill), the last position sampled and the game time of the last move, the last kill counted, the last passive blow, the last stage seen, the mastery last sent. */
    public long awakenUntil, lastAct = Long.MIN_VALUE / 2, movedAt = Long.MIN_VALUE / 2, lastKillCredit = Long.MIN_VALUE / 2, lastPassiveHit = Long.MIN_VALUE / 2;
    public double lastX, lastZ;
    public int lastStage, sentMast = -1;
    public boolean assimilating;
    public boolean flightOn;
    /** What was last sent to the owner and to the players who can see them (-1 = nothing yet). */
    public int sentSelf = -1, sentVisual = -1;
    public long lastAsk = Long.MIN_VALUE / 2;
    /** 3.76.0, live: the ritual that is running (null = none), the devil whose offer is open and since when, the last ritual and the last attribute effect (game times). */
    public DevilRitualRun ritual;
    public String offer = "";
    public long offerAt, lastRitual = -1, lastEffect = Long.MIN_VALUE / 2, flashUntil;
    /** Live, not saved: the devil's figure of the running ritual (docs/devil_forms.md) and the game time it was made; null = none. */
    public java.util.UUID npc;
    public long npcAt;
    /** Live, not saved: the game time of the last bound duel this player opened (the cooldown between two). */
    public long lastDuel = Long.MIN_VALUE / 2;
    /** Live, not saved: the game time of the player's last kill (the corruption eases for a minute after one) and the corruption last sent. */
    public long lastKill = Long.MIN_VALUE / 2;
    public int sentCorruption = -1;
    /** Phase 2 (docs/devil_contract_phase2.md), live, never saved: the trial fight's entity, what it ended in ({@code Phase2Rules.TRIAL_*}) and for which devil, the game time of the last swap of the active devil, and the friendly-fire count. */
    public java.util.UUID trial;
    public int trialResult;
    public String trialDevil = "";
    public long lastSwap = Long.MIN_VALUE / 2, ffStart;
    public int ffHits;

    /** Nothing worth saving: a player who never had a contract keeps no attachment. */
    public boolean isEmpty() { return tier == DevilTier.NONE && corruption <= 0f && sacrifices.isEmpty() && unionReadyAt == 0 && unionUntil == 0 && assimilation == 0 && mastery < 0f && masteryStash.isEmpty() && awakenReadyAt == 0; }

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
        awakenUntil = 0;
        lastStage = 0;
        sentMast = -1;
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
        if (!c.devil.isEmpty()) t.putString("devil", c.devil);
        if (c.registered) t.putBoolean("reg", true);
        if (c.corruption > 0f) t.putFloat("cor", c.corruption);
        if (c.mastery >= 0f) t.putFloat("mast", c.mastery);
        if (c.masteryDay > 0) { t.putInt("mday", c.masteryDay); t.putFloat("mused", c.masteryUsed); }
        if (c.awakenReadyAt > 0) t.putLong("arest", c.awakenReadyAt);
        if (!c.masteryStash.isEmpty()) {
            CompoundTag st = new CompoundTag();
            for (java.util.Map.Entry<String, Float> e : c.masteryStash.entrySet()) st.putFloat(e.getKey(), e.getValue());
            t.put("mstash", st);
        }
        return t;
    }

    public static DevilContract fromTag(CompoundTag t) {
        DevilContract c = new DevilContract();
        c.tier = DevilTier.byName(t.getString("tier"));
        c.attribute = DevilAttribute.byName(t.getString("attr"));
        c.assimilation = DevilRules.clampF(t.getFloat("level"), 0f, 100f);
        c.unionUntil = Math.max(0L, t.getLong("union"));
        c.unionReadyAt = Math.max(0L, t.getLong("ready"));
        String devil = t.getString("devil");
        c.devil = WorldDevil.byId(devil) != null ? WorldDevil.byId(devil).id() : "";
        c.registered = t.getBoolean("reg");
        c.corruption = DevilRules.clampF(t.getFloat("cor"), 0f, 1000f);
        c.mastery = t.contains("mast") ? com.newuniverse.nusmp.devil.mastery.MasteryRules.clamp(t.getFloat("mast")) : -1f;
        c.masteryDay = Math.max(0, t.getInt("mday"));
        c.masteryUsed = DevilRules.clampF(t.getFloat("mused"), 0f, 1.0e6f);
        c.awakenReadyAt = Math.max(0L, t.getLong("arest"));
        CompoundTag st = t.getCompound("mstash");
        for (String k : st.getAllKeys()) if (WorldDevil.byId(k) != null && c.masteryStash.size() < 32) c.masteryStash.put(k, com.newuniverse.nusmp.devil.mastery.MasteryRules.clamp(st.getFloat(k)));
        ListTag list = t.getList("sac", Tag.TAG_STRING);
        for (int i = 0; i < list.size() && i < Sacrifice.values().length * 2; i++) {
            Sacrifice s = Sacrifice.byName(list.getString(i));
            if (s != null) c.sacrifices.add(s);
        }
        return c;
    }
}
