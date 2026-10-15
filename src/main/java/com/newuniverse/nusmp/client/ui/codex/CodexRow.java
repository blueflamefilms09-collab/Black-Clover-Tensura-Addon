package com.newuniverse.nusmp.client.ui.codex;

import net.minecraft.world.item.ItemStack;

/**
 * Why: one line item of a generated Codex page, as the page builders produce it. {@code title} and {@code text} may be lang keys (translated
 * when the page is laid out, so a language change or a resource pack's lang file applies without a rebuild of the data); {@code keys} is the
 * discovery gate ("a|b", empty = always shown); {@code note} is a dim extra line (a live rule value, a spell's cost and cooldown).
 */
public record CodexRow(String title, String text, String keys, ItemStack icon, String note) {
    public CodexRow {
        title = title == null ? "" : title;
        text = text == null ? "" : text;
        keys = keys == null ? "" : keys;
        icon = icon == null ? ItemStack.EMPTY : icon;
        note = note == null ? "" : note;
    }

    public static CodexRow of(String title, String text) { return new CodexRow(title, text, "", ItemStack.EMPTY, ""); }
}
