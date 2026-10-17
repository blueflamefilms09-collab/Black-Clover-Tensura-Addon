package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerSpec;

/**
 * Why: a data marker of a template (a vanilla structure block in DATA mode, see {@code MarkerKind}) reduced to its parsed spec and the cell it
 * stands on in the template frame. The assembler needs only a few of them to plan (the PEDESTAL markers to count the relics of a vault, the
 * EXPANSION_SOCKET marker to find the end of the Way Onward); the generator turns all of them into world markers by turning the cell with the
 * piece, with no help from the game's own marker reader.
 *
 * @param spec the parsed {@code nusmp:<kind>[;key=value]*} metadata
 * @param cell the cell of the structure block in the template frame
 */
public record ShapeMarker(MarkerSpec spec, Vec cell) {
}
