package com.newuniverse.nusmp.devil;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the contract is saved as a tag and copied to the new body on death (3.48.0), so the tag format is pinned: everything round-trips, the sacrifices survive (they are the part that must never be lost), an
 * unknown or damaged name never throws and never invents a tier, and a hand-edited level or time can not go out of range.
 */
class DevilStoreTagTest {
    @Test
    void aFullContractRoundTrips() {
        DevilContract c = new DevilContract();
        c.tier = DevilTier.HIGH;
        c.attribute = DevilAttribute.GRAVITY;
        c.assimilation = 63.5f;
        c.unionUntil = 123456L;
        c.unionReadyAt = 999999L;
        c.sacrifices.add(Sacrifice.HALF_HEALTH);
        c.sacrifices.add(Sacrifice.LOST_EYE_RIGHT);
        DevilContract d = DevilContract.fromTag(DevilContract.toTag(c));
        assertEquals(DevilTier.HIGH, d.tier);
        assertEquals(DevilAttribute.GRAVITY, d.attribute);
        assertEquals(63.5f, d.assimilation);
        assertEquals(123456L, d.unionUntil);
        assertEquals(999999L, d.unionReadyAt);
        assertEquals(c.sacrifices, d.sacrifices);
        assertEquals(c.sacrificeMask(), d.sacrificeMask());
        assertFalse(d.assimilating, "the key toggle is never saved: every login starts switched off");
        assertEquals(-1, d.sentSelf);
    }

    @Test
    void anEmptyContractIsEmptyAndASacrificeAloneIsNot() {
        assertTrue(new DevilContract().isEmpty());
        DevilContract c = new DevilContract();
        c.sacrifices.add(Sacrifice.LOST_EYE_LEFT);
        assertFalse(c.isEmpty(), "a lost eye is kept even with no devil");
        assertFalse(c.hasContract());
    }

    @Test
    void unknownNamesAndWildValuesNeverThrowAndNeverInventAThing() {
        CompoundTag t = new CompoundTag();
        t.putString("tier", "GODLIKE");
        t.putString("attr", "???");
        t.putFloat("level", 9999f);
        t.putLong("union", -5L);
        t.putLong("ready", -1L);
        ListTag list = new ListTag();
        list.add(StringTag.valueOf("HALF_HEALTH"));
        list.add(StringTag.valueOf("TWO_ARMS"));
        list.add(StringTag.valueOf("HALF_HEALTH"));
        t.put("sac", list);
        DevilContract c = DevilContract.fromTag(t);
        assertEquals(DevilTier.NONE, c.tier);
        assertEquals(DevilAttribute.NONE, c.attribute);
        assertEquals(100f, c.assimilation);
        assertEquals(0L, c.unionUntil);
        assertEquals(0L, c.unionReadyAt);
        assertEquals(1, c.sacrifices.size());
        assertTrue(c.sacrifices.contains(Sacrifice.HALF_HEALTH));
        assertNotNull(DevilContract.fromTag(new CompoundTag()), "an empty tag is an empty contract");
    }

    @Test
    void theEnumsAreStoredByNameAndOnlyAppended() {
        assertArrayEquals(new String[]{"NONE", "LOW", "MID", "HIGH", "SUPREME"}, names(DevilTier.values()), "tiers: append only");
        assertArrayEquals(new String[]{"HALF_HEALTH", "LOST_EYE_LEFT", "LOST_EYE_RIGHT", "FLESH", "SOUL_DEBT", "BRANDED"}, names(Sacrifice.values()), "sacrifices: append only (the network mask uses the position)");
        assertEquals(DevilTier.NONE, DevilTier.byOrdinal(-1));
        assertEquals(DevilTier.NONE, DevilTier.byOrdinal(99));
        assertEquals(DevilTier.SUPREME, DevilTier.byOrdinal(4));
        assertEquals(DevilAttribute.WORD, DevilAttribute.forMagic("SOMETHING_NEW"), "an unknown magic gets the neutral attribute");
        assertEquals(DevilAttribute.GRAVITY, DevilAttribute.forMagic("GRAVITY"));
        assertEquals(DevilAttribute.ANTI_MAGIC, DevilAttribute.forMagic("ANTI_MAGIC"));
        assertEquals(DevilAttribute.WORD, DevilAttribute.forMagic(null));
        assertEquals(Sacrifice.LOST_EYE_LEFT.bit() | Sacrifice.LOST_EYE_RIGHT.bit(), Sacrifice.EYES);
    }

    private static String[] names(Enum<?>[] values) {
        String[] out = new String[values.length];
        for (int i = 0; i < values.length; i++) out[i] = values[i].name();
        return out;
    }
}
