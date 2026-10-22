package com.newuniverse.nusmp.charset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Why: the owner asked that the basic grimoire (the one bound to nobody) be built on the anime but differ from the characters' spells (docs/magic/basic_grimoires.md). The character spell sets
 * already hide the pages they mark {@code exclusive}, but an audit of the shared books found a few signature spells of named characters that no set marks (the character has no set that lists
 * the page, or the same spell also sits in another attribute's book). This list RESERVES them: the page stays registered and keeps its id, order and cast (rule 2), the basic grimoire does not
 * show it, and the owner's own grimoire shows it again even when his set does not list it (it then opens by the old mastery order, so no page is lost to anybody).
 *
 * <p>Pure (no game classes), read by {@link CharacterSetRegistry}: {@code hidden} (basic book only), {@code opens} and {@code listed} (owner). The master switch is the existing server config
 * {@code magic.basicBookHidesSignatures}: false lists everything in the basic book as before. Cost: two map reads.
 */
public final class BasicSignatures {
    private BasicSignatures() {}

    /** One reserved page: it lives in {@code book}, its canon owner is the CanonBook id {@code owner}. */
    public record Entry(String book, String page, String owner, String note) {
        public String key() { return book + "|" + page; }
    }

    private static final List<Entry> ALL = new ArrayList<>();
    private static final Map<String, Entry> BY_KEY = new HashMap<>();

    private static void reserve(String book, String page, String owner, String note) {
        Entry e = new Entry(book, page, owner, note);
        ALL.add(e);
        BY_KEY.put(e.key(), e);
    }

    static {
        // Crimson Lion / Vermillion: the Crimson Lion King is Fuegoleon's (docs/attributes/flame_rebuild.md); his set does not list it
        reserve("book_fire", "crimson_lion_king", "fuegoleon", "Fuegoleon's Crimson Lion King (flame_rebuild.md); no set lists it");
        // Noelle's dragon forms (owner's picture 126, docs/magic/noelle_forms.md); her set leaves them to the Valkyrie lane. The Mermaid Form stays open to every Water mage (the owner's decision in noelle_forms.md)
        reserve("book_water", "sea_dragon_god_form", "noelle", "Noelle's Dragon Form (picture 126)");
        reserve("book_water", "black_dragon_form", "noelle", "Noelle's Black Dragon Form (picture 126)");
        // pages already locked to one character by CharacterSpells but still listed in another book
        reserve("book_sword", "ripper_cut", "jack", "Slash Magic: Ripper Cut is Jack the Ripper's; locked to him, listed in the Sword book");
        reserve("book_creation", "painted_menagerie", "rill", "Painting Magic: Painted Menagerie is Rill's; locked to him, listed in the Creation book");
        reserve("book_plant", "yggdrasil", "william", "World Tree Magic: Yggdrasil is William's; locked to him, listed in the Plant book");
        reserve("book_plant", "briar_prison", "charlotte", "Briar Magic: Briar Prison is Charlotte's; locked to her, listed in the Plant book");
        // named signature spells (wiki spell pages) that sit in a book beside the owner's set without being in it
        reserve("book_spatial", "fallen_angel_gate", "finral", "Spatial Magic: Fallen Angel Gate, Finral's portal");
        reserve("book_spatial", "door_of_fate", "finral", "Compound Magic: Door of Fate, Finral with Vanessa");
        reserve("book_lightning", "thunder_boots", "luck", "Lightning Creation Magic: Thunder God's Boots, Luck's");
        reserve("book_lightning", "pulsaranta", "gadjah", "Lightning Magic: Pulsaranta, Gadjah's");
        reserve("book_painting", "souterrain_giant_arm", "rill", "Souterrain Giant's Strong Arm, the arm of Rill's Souterrain Giant");
        reserve("book_sealing", "trinity_seal", "secre", "Trinity Seal Magic, the Seal Magic wiki's spell (Secre's attribute)");
        reserve("book_cherry_blossom", "dance_of_100_million_cherry_blossoms", "kirsch", "Cherry Blossom Magic: Dance of 100 Million Cherry Blossoms, Kirsch's ultimate");
        reserve("book_body", "body_compression", "dante", "Body Magic: Body Compression, Dante's");
        // the same spell listed under another attribute's book (a wiki-named spell of a character with a set)
        reserve("book_curse", "aufwachen_dachs", "gordon", "Poison Curse Magic: Aufwachen Dachs, Gordon's (also mode 0 of the Poison book)");
        reserve("book_curse", "curse_workers_neighbor", "gordon", "Curse-Worker's Neighbor, Gordon's");
        reserve("book_curse", "ash_absorbing_formation", "zora", "Ash Curse Magic: Ash Absorbing Formation, Zora's");
        reserve("book_curse", "ultra_giant_bull", "henry", "Compound Magic: Ultra Giant Bull, Henry's (The Raging Black Bull)");
        reserve("book_curse_warding", "decaying_world", "vanica", "Curse-Warding Magic: Decaying World, Megicula's and Vanica's");
        reserve("book_food", "glutton_s_banquet", "charmy", "Food Magic: Glutton's Banquet, Charmy's");
        reserve("book_cotton", "gourmets_bite", "charmy", "Gourmet's Bite, Charmy's");
        reserve("book_eye", "reflect_iris", "gauche", "Union Magic: Reflect Iris, Gauche's mirror array");
    }

    /** Every reserved page, in declaration order. */
    public static List<Entry> all() { return List.copyOf(ALL); }

    /** The entry for a page, or null. */
    public static Entry entry(String book, String page) { return BY_KEY.get(book + "|" + page); }

    /** Is this page kept out of the basic grimoire? */
    public static boolean hides(String book, String page) { return BY_KEY.containsKey(book + "|" + page); }

    /** The CanonBook id this page is reserved for, or null. */
    public static String owner(String book, String page) {
        Entry e = BY_KEY.get(book + "|" + page);
        return e == null ? null : e.owner();
    }

    /** Is the page reserved for this character (so his own grimoire shows it even when his set does not list it)? */
    public static boolean reservedFor(String canon, String book, String page) {
        Entry e = BY_KEY.get(book + "|" + page);
        return e != null && canon != null && e.owner().equals(canon.toLowerCase(Locale.ROOT));
    }
}
