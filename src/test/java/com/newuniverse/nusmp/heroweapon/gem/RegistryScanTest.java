package com.newuniverse.nusmp.heroweapon.gem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Why: the reader of the core's registry file is tolerant on purpose, because its layout was unknown, and tolerant code is the kind that quietly reads a wrong
 * answer. This pins the layouts it accepts (weapon to holder or to a list of holders, which is what the core's saved data really is; holder to weapon; a kind next
 * to a holder; the holder as four ints or as text, nested anywhere and inside lists), the rule that every distinct holder is kept once in the order found, and the
 * noise it must read nothing from: text that is not a uuid, a weapon id that is a uuid by the game's lenient parser, a kind with no holder, an empty file, a tree
 * deeper than the limit.
 */
class RegistryScanTest {
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes()), B = UUID.nameUUIDFromBytes("b".getBytes()), C = UUID.nameUUIDFromBytes("c".getBytes());

    /** A stand-in for one saved value. */
    private record N(Map<String, RegistryScan.Node> map, List<RegistryScan.Node> list, String text, int[] ints) implements RegistryScan.Node {
        @Override public Map<String, RegistryScan.Node> compound() { return map; }
        @Override public List<RegistryScan.Node> list() { return list; }
        @Override public String text() { return text; }
        @Override public int[] ints() { return ints; }
    }

    private static RegistryScan.Node str(String s) { return new N(null, null, s, null); }

    private static RegistryScan.Node uuidInts(UUID u) {
        long m = u.getMostSignificantBits(), l = u.getLeastSignificantBits();
        return new N(null, null, null, new int[] {(int) (m >> 32), (int) m, (int) (l >> 32), (int) l});
    }

    private static RegistryScan.Node uuidText(UUID u) { return str(u.toString()); }

    private static RegistryScan.Node compound(Object... kv) {
        Map<String, RegistryScan.Node> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (RegistryScan.Node) kv[i + 1]);
        return new N(m, null, null, null);
    }

    private static RegistryScan.Node list(RegistryScan.Node... items) { return new N(null, new ArrayList<>(List.of(items)), null, null); }

    @Test
    void theCoresRealLayoutAWeaponIdMappedToAListOfUuidTextsIsRead() {
        // what hero/bond/HeroRegistry saves: {data: {holders: {<weapon id>: [<uuid text>, ...]}}, DataVersion: n}
        Map<HeroKind, List<UUID>> r = RegistryScan.parse(compound("data", compound("holders", compound(
                "legendary_shield", list(uuidText(A)),
                "vassal_scythe", list(uuidText(B), uuidText(C)),
                "legendary_bow", list())), "DataVersion", str("3955")));
        assertEquals(List.of(A), r.get(HeroKind.SHIELD));
        assertEquals(List.of(B, C), r.get(HeroKind.SCYTHE));
        assertNull(r.get(HeroKind.BOW), "an empty list is no holder");
        assertEquals(2, r.size());
    }

    @Test
    void aListEntryThatIsNotAUuidIsSkipped() {
        Map<HeroKind, List<UUID>> r = RegistryScan.parse(compound("holders", compound("legendary_sword", list(str("garbage"), uuidText(A), str("1-1-1-1-1"), uuidInts(B)))));
        assertEquals(List.of(A, B), r.get(HeroKind.SWORD));
    }

    @Test
    void aWeaponIdMappedToOneHolderIsRead() {
        Map<HeroKind, List<UUID>> r = RegistryScan.parse(compound("data", compound("holders", compound("legendary_shield", uuidInts(A), "legendary_bow", uuidText(B)))));
        assertEquals(List.of(A), r.get(HeroKind.SHIELD));
        assertEquals(List.of(B), r.get(HeroKind.BOW));
        assertEquals(2, r.size());
    }

    @Test
    void aHolderMappedToAWeaponIdIsRead() {
        Map<HeroKind, List<UUID>> r = RegistryScan.parse(compound("byPlayer", compound(A.toString(), str("legendary_sword"), B.toString(), str("vassal_scythe"))));
        assertEquals(List.of(A), r.get(HeroKind.SWORD));
        assertEquals(List.of(B), r.get(HeroKind.SCYTHE));
    }

    @Test
    void aKindNextToAHolderIsReadAnywhereAndInLists() {
        RegistryScan.Node entry1 = compound("kind", str("legendary_spear"), "uuid", uuidInts(C));
        RegistryScan.Node entry2 = compound("weapon", str("shield"), "holder", uuidText(A), "level", str("7"));
        Map<HeroKind, List<UUID>> r = RegistryScan.parse(compound("data", compound("entries", list(entry1, compound("deeper", list(entry2))))));
        assertEquals(List.of(C), r.get(HeroKind.SPEAR));
        assertEquals(List.of(A), r.get(HeroKind.SHIELD));
    }

    @Test
    void everyDistinctHolderIsKeptOnceInTheOrderFound() {
        Map<HeroKind, List<UUID>> r = RegistryScan.parse(list(compound("legendary_shield", uuidInts(A)), compound("legendary_shield", uuidInts(B)), compound("legendary_shield", uuidText(A))));
        assertEquals(List.of(A, B), r.get(HeroKind.SHIELD), "a repeated holder is not listed twice");
        assertEquals(1, r.size());
    }

    @Test
    void noiseIsNotRead() {
        assertTrue(RegistryScan.parse(compound()).isEmpty(), "an empty file");
        assertTrue(RegistryScan.parse(null).isEmpty(), "no file");
        assertTrue(RegistryScan.parse(compound("kind", str("not_a_weapon"), "uuid", uuidText(A))).isEmpty(), "a holder with no weapon");
        assertTrue(RegistryScan.parse(compound("kind", str("legendary_shield"), "uuid", str("not-a-uuid"))).isEmpty(), "a weapon with no holder");
        assertTrue(RegistryScan.parse(compound("kind", str("legendary_shield"), "count", str("4"))).isEmpty(), "a weapon next to something that is not a holder");
        assertTrue(RegistryScan.parse(compound("legendary_shield", str("1-1-1-1-1"))).isEmpty(), "the game's lenient parser would take this for a uuid; the canonical form is required");
        assertTrue(RegistryScan.parse(compound("note", str("legendary_shield"), "other", uuidText(A))).isEmpty(), "a weapon and a holder under keys that do not say so");
        assertTrue(RegistryScan.parse(compound("note", str("legendary_shield"), "uuid", uuidText(A))).isEmpty(), "a holder next to text that is not under a kind key");
        assertTrue(RegistryScan.parse(compound("kind", str("legendary_shield"), "other", uuidText(A))).isEmpty(), "a kind next to a uuid that is not under a holder key");
        assertTrue(RegistryScan.parse(compound("legendary_shield", new N(null, null, null, new int[] {1, 2, 3}))).isEmpty(), "three ints are not a uuid");
    }

    @Test
    void aTreeDeeperThanTheLimitIsNotFollowedForever() {
        RegistryScan.Node deep = compound("legendary_shield", uuidInts(A));
        for (int i = 0; i < 30; i++) deep = compound("level" + i, deep);
        assertTrue(RegistryScan.parse(deep).isEmpty(), "the limit protects against a runaway or hostile file");
        RegistryScan.Node shallow = compound("legendary_shield", uuidInts(A));
        for (int i = 0; i < 5; i++) shallow = compound("level" + i, shallow);
        assertEquals(List.of(A), RegistryScan.parse(shallow).get(HeroKind.SHIELD));
    }

    @Test
    void theUuidReadersAgreeAboutFourIntsAndText() {
        assertEquals(A, RegistryScan.uuidOf(uuidInts(A)));
        assertEquals(A, RegistryScan.uuidOf(uuidText(A)));
        assertNull(RegistryScan.uuidOf((RegistryScan.Node) null));
        assertNull(RegistryScan.uuidOf("legendary_shield"));
        assertNull(RegistryScan.uuidOf((String) null));
    }
}
