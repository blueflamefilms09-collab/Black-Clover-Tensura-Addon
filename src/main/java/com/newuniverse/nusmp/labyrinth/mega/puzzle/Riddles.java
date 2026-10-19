package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import java.util.List;

/**
 * Why: the riddle puzzles draw from a bank, and the bank is data the owner can extend: a riddle is an id, and its texts are lang keys {@code message.nusmp.mega.puzzle.riddle.<id>.q} and
 * {@code .a0 .a1 .a2} (the first answer, a0, is the right one; the rule shuffles the order shown, so the position of the right button changes). The item riddles are one per token
 * ({@code item.<token>.q}). Adding a riddle is: add its id here and its four lines to the lang file; the JUnit test demands that every id has all four lines. Pure.
 */
public final class Riddles {
    private Riddles() {}

    /** The choice riddles (answered with buttons in chat). */
    public static final List<String> CHOICE = List.of("echo", "footsteps", "map", "piano", "towel", "fire", "coffin", "river", "ton", "silence", "cloud", "flame", "future", "clock", "umbrella", "darkness");

    public static String question(String id) { return "riddle." + id + ".q"; }

    public static String answer(String id, int canonicalIndex) { return "riddle." + id + ".a" + canonicalIndex; }

    public static String itemQuestion(String token) { return "item." + token + ".q"; }
}
