package com.newuniverse.nusmp.arc.npc;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.blackclover.Devil;

import java.util.List;

/**
 * Why: the Black Bulls of the owner's brief (Yami, Asta, Noelle and their squad) are friends the player meets in the Sanctuary Hub, not enemies, so each of
 * them is only a record for the one shared NPC entity {@code nusmp:arc_npc}: a name, two colours, the magic they fight with, and the moment they show up. Their
 * words live in {@code data/nusmp/arc/dialogue/<id>.json} and their look in the art kit's {@code tools/arc/char_<id>.py}; no Java class per person. They are
 * listed together in one place because they come as one group with one rule (below), so the lead registers them with a single call from
 * {@code ArcCharacters.bootstrap()}.
 *
 * <p>The rule of their arrival: the Hub shows an NPC when the prerequisites of its record are satisfied for any online player, so the squad gathers in the
 * order the story met them instead of crowding the plaza on day one. Yami, Asta, Noelle and Finral stand there from the start (they explain the dungeon);
 * Magna, Luck and Charmy come after Revchi Salik has fallen, Vanessa, Gordon and Zora after Heath Grice, Gauche and Grey after Lotus Whomalt, Secre and Nacht
 * after Vetto. These are the ids the early-saga builder uses for those bosses; an id that is not in the roster is simply skipped by the story rules, so a
 * boss that never gets merged only means its friends are there from the start, never that they are locked out.
 *
 * <p>Pure on purpose (no Minecraft types): the records, the order and the gates are unit tested without a game, and the colours are checked to be readable on the
 * dark chat background because the name tag, the speaker name and the spawn egg all take them. The tier and the stats of a record are meaningless for an NPC
 * (it cannot be fought), so tier stays at the default and the stats are zero, which is what {@code /multiverse arc info} should print for a friend.
 */
public final class BlackBullsNpcs {
    private BlackBullsNpcs() {}

    /** Where they come from. */
    public static final String SOURCE = "Black Clover";

    /** The bosses of the early saga whose defeat brings the next friends to the Hub (see the class comment). */
    public static final String GATE_1 = "revchi_salik", GATE_2 = "heath_grice", GATE_3 = "lotus_whomalt", GATE_4 = "vetto";

    // ================================================================ stand in the Hub from the first day
    /** The captain. Violet from the cover of his Dark Magic grimoire, gold from its trim, both lifted so the name reads on a dark chat. */
    public static final ArcCharacter YAMI = npc("yami_sukehiro", "Yami Sukehiro", "Captain of the Black Bulls", 0xB89AD0, 0xE6C98A, "dark").build();

    /** The boy with no magic and an anti-magic sword. Ash-grey for his hair, green for his eyes; Liebe is the devil of his five-leaf grimoire. */
    public static final ArcCharacter ASTA = npc("asta", "Asta", "Anti-Magic Swordsman", 0xC9CED8, 0x78C28C, "anti_magic").devil(Devil.LIEBE).build();

    /** Water Magic and the Valkyrie Dress: water blue and the silver of House Silva. */
    public static final ArcCharacter NOELLE = npc("noelle_silva", "Noelle Silva", "Water Mage of House Silva", 0x9CC8F0, 0xDDE8F6, "water").build();

    /** The squad's portals. A green of his cover, a gold of its trim. He stands at the start because the Hub's gates and the floors are what he explains. */
    public static final ArcCharacter FINRAL = npc("finral_roulacase", "Finral Roulacase", "Spatial Mage", 0x78B898, 0xE0C878, "spatial").build();

    // ================================================================ after the first early boss
    /** Fire Magic. Orange for the flame, a lighter orange for the spark. */
    public static final ArcCharacter MAGNA = npc("magna_swing", "Magna Swing", "Fire Mage", 0xF0703A, 0xFFB070, "flame").after(GATE_1).build();

    /** Lightning Magic. Lightning yellow and the pale blue of his eyes. */
    public static final ArcCharacter LUCK = npc("luck_voltia", "Luck Voltia", "Lightning Mage", 0xFFE35A, 0xBFE8FF, "lightning").after(GATE_1).build();

    /** Cotton and Food Magic. Cream and biscuit. The mod has a Cotton Sheep entity, so her look can carry a sheep later. */
    public static final ArcCharacter CHARMY = npc("charmy_pappitson", "Charmy Pappitson", "Cotton and Food Mage", 0xF4EAD0, 0xF0B070, "cotton").after(GATE_1).build();

    // ================================================================ after Heath Grice
    /** Thread Magic and Rouge. Pink for her hair, red for the thread of fate. The spelling is the wiki's ({@code Enoteca}); the owner's brief said Enotecia. */
    public static final ArcCharacter VANESSA = npc("vanessa_enoteca", "Vanessa Enoteca", "Witch of the Red Thread", 0xE890B8, 0xFF7A90, "thread").after(GATE_2).build();

    /** Poison Magic. A poison green and a pale one. */
    public static final ArcCharacter GORDON = npc("gordon_agrippa", "Gordon Agrippa", "Poison Mage", 0x9CC070, 0xD8E8B0, "poison").after(GATE_2).build();

    /** Ash Magic (traps). Red for his hair, ash grey for the magic. */
    public static final ArcCharacter ZORA = npc("zora_ideale", "Zora Ideale", "Ash Mage", 0xE06A58, 0xB8B8C8, "ash").after(GATE_2).build();

    // ================================================================ after Lotus Whomalt
    /** Mirror Magic. The violet-blue of the glass in his mirror, a pale silver for the frame. */
    public static final ArcCharacter GAUCHE = npc("gauche_adlai", "Gauche Adlai", "Mirror Mage", 0x9A8CE0, 0xD6DCF8, "mirror").after(GATE_3).build();

    /** Transmutation Magic. The light blue of her hair. */
    public static final ArcCharacter GREY = npc("grey", "Grey", "Transmutation Mage", 0x9AB6D8, 0xE0ECF8, "transmutation").after(GATE_3).build();

    // ================================================================ after Vetto
    /** Sealing Magic. The cyan neon of the Seal Magic effects ({@code SecreSeals.CYAN}), lightened for reading. */
    public static final ArcCharacter SECRE = npc("secre_swallowtail", "Secre Swallowtail", "Keeper of Seals", 0x58C8F0, 0xB8F0FF, "sealing").after(GATE_4).build();

    /** Shadow Magic and four devils. A grey-violet shadow; Walgner is the one of his four devils that the mod's {@link Devil} list knows. */
    public static final ArcCharacter NACHT = npc("nacht_faust", "Nacht Faust", "Devil Host", 0x8E8EB0, 0xC8C8E8, "shadow").devil(Devil.WALGNER).after(GATE_4).build();

    /** Every Black Bull, in the order they gather in the Hub (the order of the roster lines, and of the doc). */
    private static final List<ArcCharacter> ALL = List.of(YAMI, ASTA, NOELLE, FINRAL, MAGNA, LUCK, CHARMY, VANESSA, GORDON, ZORA, GAUCHE, GREY, SECRE, NACHT);

    /** The fourteen characters, in arrival order. Immutable. */
    public static List<ArcCharacter> all() { return ALL; }

    /** Adds them all to the roster. Safe to call twice (registering the same record again is a no-op). */
    public static void registerAll() {
        for (ArcCharacter c : ALL) ArcRoster.register(c);
    }

    /** A friend of the squad: saga {@code ALLIES}, no stats, the colours and magic given. */
    private static ArcCharacter.Builder npc(String id, String name, String title, int main, int accent, String magic) {
        return ArcCharacter.npc(id, name).title(title).source(SOURCE).saga(ArcSaga.ALLIES).colors(main, accent).magic(magic).stats(0, 0, 0, 0);
    }
}
