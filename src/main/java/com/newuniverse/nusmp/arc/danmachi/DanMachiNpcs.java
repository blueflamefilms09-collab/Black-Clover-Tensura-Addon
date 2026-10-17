package com.newuniverse.nusmp.arc.danmachi;

import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;

import java.util.List;

/**
 * Why: the friends of the Dungeon are six characters of the DanMachi saga that the player talks to, not fights: the white-haired boy, his goddess, the Sword Princess,
 * the supporter, the smith and the Guild adviser. An NPC of the arc layer is only data (an {@link ArcCharacter} record: its name, title, colours and saga) plus a model
 * the art kit builds from {@code tools/arc/char_<id>.py} and a dialogue file {@code data/nusmp/arc/dialogue/<id>.json}, all of which live under the character's id; so
 * the six are six records here, listed in the order the story meets them (the adviser first: she is at the Guild door; then the boy and his goddess; then the others), and
 * one call registers them all. The requests four of them make are in {@link QuestBook}; their roles are their dialogue (the shared NPC entity has no shop yet).
 */
public final class DanMachiNpcs {
    private DanMachiNpcs() {}

    public static final ArcCharacter EINA = ArcCharacter.npc("eina_tulle", "Eina Tulle").title("the Guild Adviser").source("DanMachi").saga(ArcSaga.DANMACHI)
            .colors(0x2C4A3A, 0xD0B060).build();
    public static final ArcCharacter BELL = ArcCharacter.npc("bell_cranel", "Bell Cranel").title("the White Rabbit").source("DanMachi").saga(ArcSaga.DANMACHI)
            .colors(0xE8E8F4, 0xD8404C).build();
    public static final ArcCharacter HESTIA = ArcCharacter.npc("hestia", "Hestia").title("the Hearth Goddess").source("DanMachi").saga(ArcSaga.DANMACHI)
            .colors(0x3A78D8, 0xF4F2EC).build();
    public static final ArcCharacter WELF = ArcCharacter.npc("welf_crozzo", "Welf Crozzo").title("the Smith").source("DanMachi").saga(ArcSaga.DANMACHI)
            .colors(0xC8782A, 0xFF8A3A).build();
    public static final ArcCharacter LILI = ArcCharacter.npc("liliruca_arde", "Liliruca Arde").title("Lili, the Supporter").source("DanMachi").saga(ArcSaga.DANMACHI)
            .colors(0xB4683C, 0xFFD88A).build();
    public static final ArcCharacter AIS = ArcCharacter.npc("ais_wallenstein", "Ais Wallenstein").title("the Sword Princess").source("DanMachi").saga(ArcSaga.DANMACHI)
            .colors(0xE0B040, 0x9AF0E0).build();

    private static final List<ArcCharacter> ALL = List.of(EINA, BELL, HESTIA, WELF, LILI, AIS);

    /** The six friends, in the order the story meets them. */
    public static List<ArcCharacter> all() { return ALL; }

    /** Adds the six to the roster (a second call does nothing: the same records are registered again). */
    public static void registerAll() {
        for (ArcCharacter c : ALL) ArcRoster.register(c);
    }
}
