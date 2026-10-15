package com.newuniverse.nusmp.client.ui.codex;

import java.util.List;

/**
 * Why: a group of rows that stays together in one column of a generated page (a magic and its spells under one header). A plain page makes one
 * block per row with no header; {@link CodexLayout} places whole blocks, never half of one.
 */
public record CodexBlock(String header, List<CodexRow> rows) {
    public CodexBlock {
        header = header == null ? "" : header;
        rows = rows == null ? List.of() : List.copyOf(rows);
    }

    public static CodexBlock single(CodexRow row) { return new CodexBlock("", List.of(row)); }
}
