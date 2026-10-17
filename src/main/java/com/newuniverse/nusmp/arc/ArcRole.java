package com.newuniverse.nusmp.arc;

/**
 * Why: one roster holds both the enemies and the friends of the arcs. A {@code BOSS} is its own entity class (moves, phases, a Charter Grimoire); an
 * {@code NPC} is the one shared entity {@code nusmp:arc_npc} whose look, lines and quests come from data, so thirty friendly characters do not need
 * thirty Java classes. Only ever APPEND entries.
 */
public enum ArcRole {
    BOSS, NPC
}
