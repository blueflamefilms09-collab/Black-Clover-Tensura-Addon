package com.newuniverse.nusmp.heroweapon.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.Sin;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

/**
 * Why: the forms of the bonded weapon reach the Weapon Book through a packet, so a writer and a reader that disagree would show wrong or garbled forms (or kick the
 * player). A round trip of many random forms proves they agree, the checksum is proved stable and sensitive, and the reader is proved to refuse absurd sizes.
 */
class FormsWireTest {
    /** A byte-buffer implementation of both directions, with the same varint rule as the game's buffer. */
    private static final class Buf implements FormsWire.Out, FormsWire.In {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final DataOutputStream out = new DataOutputStream(bytes);
        DataInputStream in;

        void rewind() { in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())); }

        @Override public void utf(String s) { try { out.writeUTF(s); } catch (IOException e) { throw new IllegalStateException(e); } }

        @Override public void varInt(int v) {
            try {
                while ((v & ~0x7F) != 0) {
                    out.writeByte((v & 0x7F) | 0x80);
                    v >>>= 7;
                }
                out.writeByte(v);
            } catch (IOException e) { throw new IllegalStateException(e); }
        }

        @Override public void dbl(double d) { try { out.writeDouble(d); } catch (IOException e) { throw new IllegalStateException(e); } }

        @Override public void bool(boolean b) { try { out.writeBoolean(b); } catch (IOException e) { throw new IllegalStateException(e); } }

        @Override public String utf() { try { return in.readUTF(); } catch (IOException e) { throw new IllegalStateException(e); } }

        @Override public int varInt() {
            try {
                int v = 0, shift = 0, b;
                do {
                    b = in.readUnsignedByte();
                    v |= (b & 0x7F) << shift;
                    shift += 7;
                } while ((b & 0x80) != 0 && shift < 35);
                return v;
            } catch (IOException e) { throw new IllegalStateException(e); }
        }

        @Override public double dbl() { try { return in.readDouble(); } catch (IOException e) { throw new IllegalStateException(e); } }

        @Override public boolean bool() { try { return in.readBoolean(); } catch (IOException e) { throw new IllegalStateException(e); } }
    }

    private static HeroForm random(Random r, int n) {
        String[] keys = HeroForm.STAT_KEYS.toArray(new String[0]);
        Map<String, Double> stats = new LinkedHashMap<>(), bonus = new LinkedHashMap<>();
        for (int i = 0; i < r.nextInt(5); i++) stats.put(keys[r.nextInt(keys.length)], r.nextInt(2000) / 100.0 - 5);
        for (int i = 0; i < r.nextInt(3); i++) bonus.put(keys[r.nextInt(keys.length)], r.nextInt(100) / 100.0);
        List<HeroForm.Requirement> req = new ArrayList<>();
        for (int i = 0; i < r.nextInt(5); i++)
            req.add(r.nextBoolean() ? new HeroForm.Requirement("minecraft:item_" + r.nextInt(99), "", 4 + r.nextInt(250)) : new HeroForm.Requirement("", "c:tag_" + r.nextInt(9) + "/x", 4 + r.nextInt(60)));
        Map<String, String> skills = new LinkedHashMap<>();
        String[] slots = {"use", "sneak", "key1", "key2", "key3"};
        for (int i = 0; i < r.nextInt(4); i++) skills.put(slots[r.nextInt(5)], "shield.skill_" + r.nextInt(30));
        List<String> parents = new ArrayList<>(), passives = new ArrayList<>();
        for (int i = 0; i < r.nextInt(3); i++) parents.add("parent_" + i);
        for (int i = 0; i < r.nextInt(4); i++) passives.add("passive_" + i);
        boolean curse = r.nextInt(4) == 0;
        String text = "Name \u00e9\u4e2d " + n + " ".repeat(r.nextInt(3)) + "x".repeat(r.nextInt(200));
        return new HeroForm(HeroKind.SHIELD, "form_" + n, text, "Desc " + n, r.nextInt(6), parents, req, r.nextInt(401), stats, bonus, skills, passives, curse ? "curse" : "base", curse,
                curse ? Sin.values()[r.nextInt(Sin.values().length)] : null, curse ? r.nextInt(101) : 0);
    }

    @Test
    void manyRandomFormsSurviveARoundTrip() {
        Random r = new Random(20261018L);
        for (int round = 0; round < 200; round++) {
            List<HeroForm> forms = new ArrayList<>();
            for (int i = 0, n = 1 + r.nextInt(12); i < n; i++) forms.add(random(r, i));
            Buf b = new Buf();
            FormsWire.writeForms(b, forms);
            b.rewind();
            List<HeroForm> back = FormsWire.readForms(b, HeroKind.SHIELD);
            assertEquals(forms, back, "round " + round);
        }
    }

    @Test
    void theSyntheticBaseSurvivesToo() {
        Buf b = new Buf();
        FormsWire.writeForms(b, List.of(HeroForm.base(HeroKind.CHAIN)));
        b.rewind();
        assertEquals(List.of(HeroForm.base(HeroKind.CHAIN)), FormsWire.readForms(b, HeroKind.CHAIN));
    }

    @Test
    void theChecksumIsStableAndSensitiveToEachField() {
        Random r = new Random(7);
        List<HeroForm> forms = new ArrayList<>();
        for (int i = 0; i < 6; i++) forms.add(random(r, i));
        int sum = FormsWire.checksum(forms);
        assertEquals(sum, FormsWire.checksum(new ArrayList<>(forms)), "equal forms, equal sum");
        HeroForm f = forms.get(2);
        HeroForm renamed = new HeroForm(f.kind(), f.id(), f.name() + "!", f.desc(), f.tier(), f.parents(), f.requires(), f.mastery(), f.stats(), f.abilityBonus(), f.skills(), f.passives(), f.look(), f.curse(), f.sin(), f.corruptionPerSecond());
        HeroForm harder = new HeroForm(f.kind(), f.id(), f.name(), f.desc(), f.tier(), f.parents(), f.requires(), f.mastery() + 1, f.stats(), f.abilityBonus(), f.skills(), f.passives(), f.look(), f.curse(), f.sin(), f.corruptionPerSecond());
        for (HeroForm changed : List.of(renamed, harder)) {
            List<HeroForm> other = new ArrayList<>(forms);
            other.set(2, changed);
            assertNotEquals(sum, FormsWire.checksum(other));
        }
        List<HeroForm> fewer = new ArrayList<>(forms);
        fewer.remove(5);
        assertNotEquals(sum, FormsWire.checksum(fewer));
        assertEquals(FormsWire.checksum(forms), FormsWire.checksum(forms), "no hidden state");
    }

    @Test
    void theReaderRefusesAbsurdSizes() {
        Buf huge = new Buf();
        huge.varInt(1_000_000);
        huge.rewind();
        try {
            FormsWire.readForms(huge, HeroKind.SHIELD);
            assertTrue(false, "a million forms must be refused");
        } catch (IllegalArgumentException expected) {
            // good
        }
        Buf negative = new Buf();
        negative.varInt(-1);
        negative.rewind();
        try {
            FormsWire.readForms(negative, HeroKind.SHIELD);
            assertTrue(false, "a negative count must be refused");
        } catch (IllegalArgumentException expected) {
            // good
        }
        Buf longText = new Buf();
        longText.varInt(1);
        longText.utf("x".repeat(601));
        longText.rewind();
        try {
            FormsWire.readForms(longText, HeroKind.SHIELD);
            assertTrue(false, "an overlong text must be refused");
        } catch (IllegalArgumentException expected) {
            // good
        }
    }
}
