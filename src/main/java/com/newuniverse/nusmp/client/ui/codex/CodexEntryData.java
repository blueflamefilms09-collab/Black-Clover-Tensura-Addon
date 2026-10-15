package com.newuniverse.nusmp.client.ui.codex;

/**
 * Why: one entry of a data-driven Codex section, as plain data with no Minecraft types, so the parsing, the merging and the ordering rules can be
 * unit tested without a game. The Codex used to be a Java list; a record that a resource pack can fill means a new page, rule or lore line is a
 * JSON file and nothing else (see docs/codex.md).
 *
 * @param section the directory under {@code codex/} the file sat in (it becomes a tab)
 * @param id      {@code namespace:name} of the file without ".json"; two files with the same section and id are the same entry, the later pack wins
 * @param title   a text or a lang key (translated when drawn, see {@link CodexData#looksLikeLangKey})
 * @param text    a text or a lang key
 * @param keys    discovery keys joined by "|" (empty = always shown); the entry reads "???" until the player has found any one of them
 * @param icon    "item:namespace:path" (empty = none)
 * @param order   sort position inside the tab (lower first), ties by title then id
 * @param remove  true = a pack that deletes an entry of an earlier pack instead of adding one
 */
public record CodexEntryData(String section, String id, String title, String text, String keys, String icon, int order, boolean remove) {
    public CodexEntryData {
        section = section == null ? "" : section;
        id = id == null ? "" : id;
        title = title == null ? "" : title;
        text = text == null ? "" : text;
        keys = keys == null ? "" : keys;
        icon = icon == null ? "" : icon;
    }
}
