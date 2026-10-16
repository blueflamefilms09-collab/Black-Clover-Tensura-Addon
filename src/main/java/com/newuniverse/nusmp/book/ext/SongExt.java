package com.newuniverse.nusmp.book.ext;

import com.newuniverse.nusmp.book.BookPage;

import java.util.List;

/**
 * Why: Kahono's Song Magic modes live in the song package (they need the singing state and a ticker); this is only the hook that
 * appends them after the generic book's five pages, so older pages keep their mode numbers. Page ids: song_mode_heal,
 * song_mode_lullaby, song_mode_barrier, song_mode_requiem (open to anyone with Song Magic) and song_crescendo (Kahono's).
 */
public final class SongExt {
    private SongExt() {}

    public static List<BookPage> pages() { return com.newuniverse.nusmp.song.SongPages.pages(); }
}
