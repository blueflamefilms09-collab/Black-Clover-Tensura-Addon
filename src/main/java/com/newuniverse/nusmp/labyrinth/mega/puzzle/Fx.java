package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import java.util.List;

/**
 * Why: a rule never touches the world; it answers every action with a list of EFFECTS the world layer carries out (a note, a spark on a cell, a line of text, a penalty, the reward). Keeping
 * the effects as plain data is what lets the tests assert "a wrong step costs something, a right one never does" and "the puzzle never asks for a penalty of more than severity 3" without a
 * server. {@code actor} is the player an effect is meant for (-1: the whole party), {@code cell} the cell it concerns (-1: none), {@code n} a number whose meaning depends on the kind,
 * {@code key} a short name (a lang key suffix, an instrument, a token, a penalty kind) and {@code more} its arguments. Pure.
 */
public record Fx(Kind kind, int actor, int cell, int n, String key, List<String> more) {
    public enum Kind {
        /** A note on a cell: {@code n} the pitch step (0 to 24), {@code key} the instrument (harp, bell, bass, chime). */
        TONE,
        /** A flash on a cell: {@code n} 1 good, 0 bad. */
        SPARK,
        /** A line of text to the actor (or the party): lang key suffix {@code key} of this puzzle (or of the common set), {@code n} and {@code more} are its arguments ({@code $x} in more = a lang key). */
        SAY,
        /** A cost: {@code n} the severity 1 to 3, {@code key} the kind (hurt, dark, drain, slow, hunger, ambush). The world scales it by depth and caps it so it can never kill. */
        PENALTY,
        /** The puzzle is beaten: the world pays the reward. */
        SOLVED,
        /** The puzzle went back to its starting state (after too many mistakes): the world redraws everything. */
        RESET,
        /** Give the actor a token item ({@code key}). */
        GIVE,
        /** Take one token item ({@code key}) from the actor's hand (the offering was accepted). */
        CONSUME,
        /** Put a question to the party with answer buttons: {@code key} the question, {@code n} its number, {@code more} the answers' lang key suffixes in the order shown. */
        ASK,
        /** A floor tile gives way for a while (the world digs a one-block pit and puts the tile back after a few seconds). */
        PIT,
        /** The actor is put back at the puzzle's starting row (never a trap: it is a walk back). */
        RETURN,
        /** The chest was a mimic: the world lets a weak monster out of it. */
        MIMIC;
    }

    public static Fx tone(int cell, int note, String instrument) { return new Fx(Kind.TONE, -1, cell, note, instrument, List.of()); }

    public static Fx spark(int cell, boolean good) { return new Fx(Kind.SPARK, -1, cell, good ? 1 : 0, "", List.of()); }

    public static Fx say(int actor, String key, int n, String... more) { return new Fx(Kind.SAY, actor, -1, n, key, List.of(more)); }

    public static Fx penalty(int actor, int severity, String kind) { return new Fx(Kind.PENALTY, actor, -1, Math.max(1, Math.min(3, severity)), kind, List.of()); }

    public static Fx solved() { return new Fx(Kind.SOLVED, -1, -1, 0, "", List.of()); }

    public static Fx reset() { return new Fx(Kind.RESET, -1, -1, 0, "", List.of()); }

    public static Fx give(int actor, int cell, String token) { return new Fx(Kind.GIVE, actor, cell, 1, token, List.of()); }

    public static Fx consume(int actor, String token) { return new Fx(Kind.CONSUME, actor, -1, 1, token, List.of()); }

    public static Fx ask(String question, int number, List<String> answers) { return new Fx(Kind.ASK, -1, -1, number, question, List.copyOf(answers)); }

    public static Fx pit(int cell) { return new Fx(Kind.PIT, -1, cell, 120, "", List.of()); }

    public static Fx back(int actor) { return new Fx(Kind.RETURN, actor, -1, 0, "", List.of()); }

    public static Fx mimic(int actor, int cell) { return new Fx(Kind.MIMIC, actor, cell, 0, "", List.of()); }
}
