package com.newuniverse.nusmp.client.elite.forge;

import com.newuniverse.nusmp.elite.forge.rules.MinigameType;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Why: the four minigame screens share a contract so that one host screen ({@link ForgeMinigameScreen}) can run any sequence: a view is created for a step, started
 * ({@link #begin}, which starts its clock), fed the mouse every client tick, asked to draw itself into a rectangle, told about clicks, drags and keys, and finally asked
 * whether it {@link #finished} and for the {@link #input} to report. A view plays the game with the SAME pure rules the server re-scores with
 * ({@code elite.forge.rules}); it never computes the score it sends, because it sends none.
 */
interface MinigameView {
    MinigameType type();

    /** Starts the step: the clock begins now. */
    void begin(long seed, int difficulty);

    /** One client tick (20 per second) with the mouse position and whether the left button is held. */
    void tick(int mouseX, int mouseY, boolean mouseDown);

    void render(GuiGraphics g, Font font, int x, int y, int w, int h, int mouseX, int mouseY, boolean mouseDown);

    /** A mouse press inside the host's panel; returns true when the view used it. */
    boolean click(int x, int y, int w, int h, double mx, double my, int button);

    /** The mouse moved with a button held. */
    default void drag(int x, int y, int w, int h, double mx, double my) { }

    default void release(double mx, double my, int button) { }

    /** A key (GLFW code); returns true when the view used it. */
    default boolean key(int keyCode) { return false; }

    boolean finished();

    /** What was played, in the format of the matching {@code Minigame}. */
    int[] input();

    /** The translation key of the one-line hint shown above the game. */
    String hintKey();

    /** A running estimate 0..1000 for the little score bar. */
    int liveScore();
}
