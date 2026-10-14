package com.newuniverse.nusmp.client.ui.theme;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

/**
 * The lang keys of the Great-Sage voice (assets/nusmp/lang: {@code nusmp.ui.*}). The voice is terse and analytical, like Tensura's Great
 * Sage / Raphael: "Analysis complete.", "Result: ...", "Notice: ...", "Warning: ...". Code never writes these lines literally.
 */
public final class UiLang {
    private UiLang() {}

    public static final String ANALYSIS_COMPLETE = "nusmp.ui.sage.analysis_complete", ANALYZING = "nusmp.ui.sage.analyzing",
            RESULT = "nusmp.ui.sage.result", NOTICE = "nusmp.ui.sage.notice", WARNING = "nusmp.ui.sage.warning",
            CONFIRMED = "nusmp.ui.sage.confirmed", RECORDS = "nusmp.ui.codex.sage.default";

    public static String get(String key, Object... args) { return Component.translatable(key, args).getString(); }

    public static boolean has(String key) { return Language.getInstance().has(key); }

    /** The one-line analysis of a Codex page (by its title); a page without its own line gets the generic one. */
    public static String codexNote(String pageTitle) {
        String key = "nusmp.ui.codex.sage." + UiMath.slug(pageTitle);
        return get(has(key) ? key : RECORDS);
    }
}
