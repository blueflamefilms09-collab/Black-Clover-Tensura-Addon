package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;

import java.util.List;

/**
 * Modification Magic (Morris Libardirt), 1.93.0 rebuild: the grimoire's pages. Until now this magic was one of the thirty generic wiki attributes (a bolt, a line, a field, a domain, a
 * guard); WikiBooks.attribute() hands it to {@link #create()}. The five generic page ids and their order are KEPT (attribute_bolt ... attribute_guard: saves and the mastery bits depend
 * on them) and their casts are re-implemented as the canon spells (Dissection Needles, Clawed Tendrils, Structural Unravel, Structure Shock, Re-Structure); five pages are appended
 * (Specimen Mark, Spell Rewrite, Test Subjects, Dissection Theatre, the Dissector's Frame). The generic implementation stays in WikiBooks for the other attributes.
 */
public final class ModificationBook {
    private ModificationBook() {}

    public static GrimoireBook create() {
        return new ElementBook(MagicType.MODIFICATION, WikiBooks.color(MagicType.MODIFICATION), TensuraDamageTypes.MAGIC_GENERIC, List.of(
                BookPage.starter(ModificationRules.BOLT, "Modification: Dissection Needles", ModificationArts::needles),
                BookPage.mid(ModificationRules.CUT, "Modification: Clawed Tendrils", ModificationArts::tendrils),
                BookPage.zone(ModificationRules.FIELD, "Modification: Structural Unravel", ModificationArts::unravel),
                BookPage.signature(ModificationRules.DOMAIN, "Modification: Structure Shock", ModificationArts::shock).withCooldown(900),
                BookPage.mid(ModificationRules.GUARD, "Modification: Re-Structure", ModificationArts::restructure),
                // 1.93.0: appended
                BookPage.mid(ModificationRules.THREAD, "Modification: Specimen Mark", ModificationArts::specimenThread).withCooldown(360),
                BookPage.mid(ModificationRules.REWRITE, "Modification: Spell Rewrite", ModificationArts::spellRewrite).withCooldown(380),
                BookPage.mid(ModificationRules.SUBJECTS, "Modification: Test Subjects", ModificationArts::testSubjects).withCooldown(600),
                BookPage.signature(ModificationRules.THEATRE, "Modification: Dissection Theatre", ModificationArts::dissectionTheatre).withCooldown(1500).withAnim("signature"),
                BookPage.signature(ModificationRules.FRAME, "Dissector's Frame", ModificationArts::dissectorFrame).withCooldown(900).withAnim("signature")))
                .plus(com.newuniverse.nusmp.book.ext.ExclusiveExt.pages(MagicType.MODIFICATION));
    }
}
