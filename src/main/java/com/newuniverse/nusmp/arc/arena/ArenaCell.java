package com.newuniverse.nusmp.arc.arena;

/**
 * Why: one block of an arena's column: its height above the walking surface ({@code dy} 0 is the topmost floor block, so a player stands at dy 1 and the
 * foundation lies at dy -1 and -2) and its role. Kept as a tiny record so a shape can return a list per column and a test can read it.
 */
public record ArenaCell(int dy, ArenaPart part) {}
