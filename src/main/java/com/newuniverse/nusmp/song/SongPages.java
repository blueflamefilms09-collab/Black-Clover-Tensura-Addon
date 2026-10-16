package com.newuniverse.nusmp.song;

import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import java.util.List;

/**
 * Why: Kahono's Song Magic is played, not aimed. Each mode page, when cast, only SETS the song (stored on the skill, shown on the
 * action bar, answered by a short chime); the effect then comes from the player singing, see {@link SongTicker}. The signature page
 * Crescendo arms a loud, released burst of the current mode. The pages are appended after the generic five, so older mode numbers do
 * not move.
 */
public final class SongPages {
    /** Tag key: the page index the last mode page was cast from (used as the cast mode of the damage helper). */
    public static final String TAG_PAGE = "SongPage";
    /** Persistent-data key on the singer: game time until which Crescendo may be charged and released. */
    public static final String CRESCENDO_UNTIL = "nusmp_song_cres";

    private SongPages() {}

    /** The five new pages, in registration order (never reorder; append only). */
    public static List<BookPage> pages() {
        return List.of(
                modePage(SongMode.HEAL),
                modePage(SongMode.LULLABY),
                modePage(SongMode.BARRIER),
                modePage(SongMode.REQUIEM),
                BookPage.signature("song_crescendo", "Crescendo", SongPages::crescendo).withCooldown(600));
    }

    /** A mode page is cheap: choosing a song costs almost nothing, the singing pays. */
    private static BookPage modePage(SongMode m) {
        return new BookPage(m.pageId, m.label, m.label, 2, 40, 20, (b, i, p, mode) -> setMode(b, i, p, mode, m));
    }

    static boolean setMode(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int page, SongMode m) {
        SongSkill.setMode(p, i, m);
        i.getOrCreateTag().putInt(TAG_PAGE, page);
        p.getPersistentData().putLong(CRESCENDO_UNTIL, 0L);
        p.displayClientMessage(Component.literal("Song: " + m.label + ". Sing to begin.").withStyle(ChatFormatting.LIGHT_PURPLE), true);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.8f, 0.8f + 0.2f * m.id);
        return true;
    }

    /** Arms Crescendo: for a while, a loud held note followed by letting go releases a large pulse of the current mode. */
    static boolean crescendo(GrimoireBook b, ManasSkillInstance i, ServerPlayer p, int page) {
        SongMode m = SongSkill.mode(i);
        if (m == SongMode.NONE) { GrimoireBook.fail(p, "Choose a song first."); return false; }
        p.getPersistentData().putLong(CRESCENDO_UNTIL, p.level().getGameTime() + SongConfig.crescendoWindow());
        p.displayClientMessage(Component.literal("Crescendo: sing loudly for " + SongConfig.crescendoTicks() / 20 + " seconds, then stop.")
                .withStyle(ChatFormatting.LIGHT_PURPLE), true);
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0f, 0.7f);
        return true;
    }
}
