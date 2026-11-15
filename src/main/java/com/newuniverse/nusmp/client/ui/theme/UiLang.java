package com.newuniverse.nusmp.client.ui.theme;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * The lang keys of the Great-Sage voice (assets/anime_multiverse/lang: {@code anime_multiverse.ui.*}). The voice is terse and analytical, like Tensura's Great
 * Sage / Raphael: "Analysis complete.", "Result: ...", "Notice: ...", "Warning: ...". Code never writes these lines literally.
 */
public final class UiLang {
    private UiLang() {}

    public static final String ANALYSIS_COMPLETE = "anime_multiverse.ui.sage.analysis_complete", ANALYZING = "anime_multiverse.ui.sage.analyzing",
            RESULT = "anime_multiverse.ui.sage.result", NOTICE = "anime_multiverse.ui.sage.notice", WARNING = "anime_multiverse.ui.sage.warning",
            CONFIRMED = "anime_multiverse.ui.sage.confirmed", RECORDS = "anime_multiverse.ui.codex.sage.default";

    public static String get(String key, Object... args) { return Component.translatable(key, args).getString(); }

    public static boolean has(String key) { return Language.getInstance().has(key); }

    /** The one-line analysis of a Codex page (by its title); a page without its own line gets the generic one. */
    public static String codexNote(String pageTitle) {
        String key = "anime_multiverse.ui.codex.sage." + UiMath.slug(pageTitle);
        return get(has(key) ? key : RECORDS);
    }
}
