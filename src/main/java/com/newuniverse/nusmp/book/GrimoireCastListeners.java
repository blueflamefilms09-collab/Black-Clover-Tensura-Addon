package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.blackclover.MagicType;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;

/**
 * Guide P2 #6: what follows a finished grimoire cast, in the order {@code GrimoireBook.castPage} used to call it (the order is kept: Wave 6 and Wave 16 read the gear and mana
 * the earlier lines settle). {@link #onPost} is the built-in listener of {@link GrimoireCastEvent.Post} (registered in NUSMP); with the SERVER setting
 * guide_fixes.grimoireCastEvent off, {@code castPage} calls {@link #afterCast} itself and no event is posted. Each line is one feature; a new feature subscribes to the event instead of
 * adding a line here.
 */
public final class GrimoireCastListeners {
    private GrimoireCastListeners() {}

    public static void onPost(GrimoireCastEvent.Post e) {
        afterCast(e.book(), e.player(), e.instance(), e.page(), e.mode());
    }

    public static void afterCast(GrimoireBook book, ServerPlayer player, ManasSkillInstance instance, BookPage p, int mode) {
        MagicType castMagic = book.magicNow();
        com.newuniverse.nusmp.charset.CharacterSets.onCast(instance, p.id());      // character spell sets: the cast counts toward the rules that name this page (Spirit of Zephyr after the Spirit Dive)
        com.newuniverse.nusmp.charset.CharacterFlourishRest.after(player, instance, p);      // character spell sets (squad captains / Spade / Heart / Diamond / Seabed): the character's own colours around the cast
        CharacterFlairBulls.onCast(player, instance, p);      // Black Bulls character spell sets: a signature layer in his colour on top of the page's own effect (nothing for any other grimoire)
        com.newuniverse.nusmp.quests.QuestHooks.onCast(player, String.valueOf(castMagic), p.id(), p.costPercent());
        com.newuniverse.nusmp.labyrinth.mega.flow.FlowCasts.onCast(player, book.magicId(), p.id(), p.costPercent());      // 1.55.0: the Rukh flow: a finished cast is Light, Dark or neutral; an opposing one is rejected in a Holy place
        instance.getOrCreateTag().putInt("LastMode", mode);
        com.newuniverse.nusmp.item.MagicGear.onCast(player);
        com.newuniverse.nusmp.relics.wave.Wave6Events.onSpellCast(player);
        com.newuniverse.nusmp.spiritdark.SpiritDark.onCast(player, castMagic);      // Spirit of Darkness: the companion reacts to a Dark or Shadow cast (nothing for any other caster)
        com.newuniverse.nusmp.relics.wave.Wave16.onSpellCast(player, p.costPercent());      // 1.25.0: Brooch of Po'eur's free cast      // 1.24.0: Neket's robe stores a Mage Armor per cast
        DreamWorld.onCast(player, castMagic);                                     // 0.41: Spatial / Time / Anti-Magic / Dream tear a dream open
        MirrorWorks.echo(player);                                                 // 0.42: Real Doubles cast with you
        com.newuniverse.nusmp.mana.ManaLaw.onCast(player);                          // 1.5.0: negative mana corrupts
        com.newuniverse.nusmp.franchise.FranchiseEvents.onSpellCast(player);        // 1.111.0: franchise engravings of the held book (mana back, spell power)
        com.newuniverse.nusmp.entity.veldanava.skills.SolomonCasts.record(player, book, mode);      // 3.18.0: the cast log Solomon's Grand Spell Copy and Spell Chain read (one record, two deque operations)
    }
}
