package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.KotodamaWords.Source;
import com.newuniverse.nusmp.book.KotodamaWords.Word;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.ability.skill.Skill;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * 0.47 Kotodama Magic (Word Soul), Zagred's grimoire: a God-class book (Tensura ULTIMATE). Creative only: it is never rolled,
 * and outside creative it answers only someone who has beaten the Zagred boss (see {@link KotodamaWords}).
 * <p>
 * Each page speaks one command word, exactly as typing the word alone in chat does. The pages cost nothing through Tensura and
 * set no cooldown of their own: {@link KotodamaWords#speak} charges the astronomical magicule cost (bridged with Aura) and keeps
 * the per-word cooldowns, so chat and pages share them.
 */
public class KotodamaBook extends GrimoireBook {
    private final List<BookPage> pages = com.newuniverse.nusmp.book.ext.Ext.join(List.of(
            word("halt", "Halt", Word.HALT),
            word("shatter", "Shatter", Word.SHATTER),
            word("heal", "Heal", Word.HEAL),
            word("underworld", "Devour", Word.SLUDGE),
            word("trident", "Trident", Word.TRIDENT),
            word("demon_swords", "Swords", Word.SWORDS),
            // 0.48 counter-words (appended)
            word("seal", "Seal", Word.SEAL),
            word("reject", "Reject", Word.REJECT),
            word("fall", "Fall", Word.FALL),
            word("reveal", "Reveal", Word.REVEAL),
            word("sleep", "Sleep", Word.SLEEP),
            word("petrify", "Petrify", Word.PETRIFY),
            word("cower", "Cower", Word.FEAR),
            word("banish", "Banish", Word.BANISH),
            word("reverse", "Reverse", Word.REVERSE),
            word("drain", "Drain", Word.DRAIN)), com.newuniverse.nusmp.book.ext.KotodamaExt.pages());

    public KotodamaBook() { super(MagicType.KOTODAMA, KotodamaWords.VIOLET, Skill.SkillType.ULTIMATE); }

    static BookPage word(String id, String name, Word w) {
        return new BookPage(id, name, name, 0, 0, 0, (b, i, p, m) -> KotodamaWords.speak(p, w, Source.GRIMOIRE));
    }

    @Override protected List<BookPage> familyPages() { return pages; }

    /** Every word is open from the start (no mastery gate): the cost and the creative / Zagred gate are the limit. */
    @Override
    public boolean usable(ManasSkillInstance i, LivingEntity e, int mode) {
        if (mode < familyCount()) {
            if (!isUnlocked(i, mode) && e instanceof ServerPlayer) unlock(i, mode);
            return true;
        }
        return super.usable(i, e, mode);
    }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.DARKNESS_ELEMENTAL; }
}
