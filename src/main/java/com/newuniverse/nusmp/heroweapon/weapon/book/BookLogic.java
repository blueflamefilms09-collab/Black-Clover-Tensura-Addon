package com.newuniverse.nusmp.heroweapon.weapon.book;

import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.Forgetful;
import com.newuniverse.nusmp.heroweapon.weapon.glassworld.GlassSelfTest;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.List;
import java.util.UUID;

/**
 * Why: the behaviour of the Vassal Book (Ethnobalt's living spellbook), the CAST weapon of the set: it is read, not swung. The wielder takes its pages in the
 * order they are written, an offence chapter with the use key and a support chapter with sneak and use, the action bar always shows what comes next, a turn of
 * the page skips one that does not suit the moment, and when every page of both chapters has been read the volume is complete and Libraria (the white room) may
 * fall. The seven techniques are in {@link BookSkills}, the six pages in {@link BookPages}, the numbers in {@link BookNumbers}, the words in {@link BookText},
 * the deck rules in {@link PageDeck}, {@link BookDecks} and {@link Volume}. This class is the part the shared machinery calls: it lists the skills and rebuilds
 * the volume when the form changes to one with other pages. It also registers the self-test {@code hero.weapon.book}, which reads a whole volume and casts
 * Libraria, and clears its per-wielder state when a wielder leaves or the server stops.
 */
public final class BookLogic implements HeroLogic, Forgetful {
    private final List<HeroSkill> skills = BookSkills.all();

    public BookLogic() {
        SelfTests.register("hero.weapon.book", server -> GlassSelfTest.run(server, HeroKind.BOOK, BookNumbers.table(), BookLogic::libraryRound));
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> BookState.clearAll());
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent e) -> BookState.forget(e.getEntity().getUUID()));
    }

    @Override
    public HeroKind kind() { return HeroKind.BOOK; }

    @Override
    public List<HeroSkill> skills() { return skills; }

    @Override
    public void onFormChanged(HeroCtx c, HeroForm from, HeroForm to) {
        BookState.volume(c);                                          // sets the decks to the pages of the new form; progress is kept when the pages did not change
    }

    @Override
    public void forget(UUID wielder) {
        BookState.forget(wielder);
    }

    /** The self-test's own round: an unread volume refuses Libraria; a read one casts it once and is used up. */
    private static void libraryRound(GlassSelfTest.Probe p) {
        HeroCtx c = p.ctx();
        HeroSkill libraria = HeroLogics.skill(BookNumbers.LIBRARIA);
        if (libraria == null) {
            p.bad().add("the library round could not find Libraria");
            return;
        }
        Volume v = BookState.volume(c);
        v.consume();
        if (libraria.cast(c)) p.bad().add("Libraria was cast from an unread volume");
        int guard = 0;
        while (!v.complete(0) && guard++ < 20) {
            v.offense().draw();
            v.support().draw();
        }
        if (!v.complete(0)) p.bad().add("reading every page did not complete the volume");
        if (p.level().getEntity(p.foe().getId()) != p.foe()) return;            // the dummy could not be placed (its chunk is not loaded): Libraria has no foe to need
        if (!libraria.cast(c)) p.bad().add("Libraria refused a complete volume with a foe in reach");
        if (v.pagesRead() != 0) p.bad().add("Libraria did not use the volume up");
        if (libraria.cast(c)) p.bad().add("Libraria was cast twice on one volume");
    }
}
