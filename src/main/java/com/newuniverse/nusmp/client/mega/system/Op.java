package com.newuniverse.nusmp.client.mega.system;

/**
 * Why: the holographic window is drawn with exactly three kinds of mark (a filled rectangle, a vertical gradient, a line of text), and the part of the work that
 * is hard to get right without seeing it is WHERE each mark goes. So the layout produces a plain list of these marks and the game class only replays the list
 * on a {@code GuiGraphics}. That split has three payoffs: the layout is pure and unit tested (nothing leaves its panel, the bar adds up to its width, the toasts
 * slide in from the left), the very same list can be dumped and drawn by a script ({@code tools/system/preview_overlay.py}) so the owner can SEE the window
 * without starting the game, and drawing can never be half-wrong in a way the tests do not know about, because there is nothing in the drawing but this list.
 * Coordinates are in GUI pixels of the window's own space (before the player's scale). Colours are ARGB ints exactly as {@code GuiGraphics} takes them.
 */
public sealed interface Op {
    /** A filled rectangle from (x1, y1) up to but not including (x2, y2). */
    record Fill(int x1, int y1, int x2, int y2, int argb) implements Op {}

    /** A rectangle shaded from {@code top} to {@code bottom}. */
    record Gradient(int x1, int y1, int x2, int y2, int top, int bottom) implements Op {}

    /** A line of text whose top-left corner is (x, y); {@code shadow} draws the game's drop shadow under it. */
    record Text(int x, int y, String text, int argb, boolean shadow) implements Op {}
}
