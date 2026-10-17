package com.newuniverse.nusmp.client.avatar;

import com.newuniverse.nusmp.avatar.menu.ForgeLayout.Rect;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Why: one page of the Soul Forge screen (Identity, Appearance, Traits, Skills, Aptitudes) is a self-contained set of controls drawn inside the left module.
 * The screen slides the module in and out, so a page never has to know about the slide: it is laid out once for the body rectangle it is given, drawn with
 * mouse coordinates already shifted into its own frame, and told about clicks only while it sits still. This base class is the contract plus do-nothing
 * defaults, so a page only writes the events it cares about.
 */
abstract class ForgePage {
    protected final ForgeHost h;
    protected Rect body = Rect.EMPTY;

    ForgePage(ForgeHost host) { this.h = host; }

    /** The accent colour (0xRRGGBB) of the module's frame and heading. */
    abstract int accent();

    /** The heading text of the module. */
    abstract String title();

    /** Places the controls inside {@code area} (the module's body); called when the window changes and when the page is shown. */
    void layout(Rect area) { this.body = area; }

    abstract void render(GuiGraphics g, int mx, int my, float partial);

    boolean mouseClicked(double mx, double my, int button) { return false; }

    boolean mouseReleased(double mx, double my, int button) { return false; }

    boolean mouseDragged(double mx, double my, int button) { return false; }

    boolean mouseScrolled(double mx, double my, double dy) { return false; }

    boolean keyPressed(int key, int scan, int mods) { return false; }

    /** One client tick while the page is shown. */
    void tick() {}

    /** One client tick whichever page is shown (for animations that must finish even if the creator looks at another page). */
    void tickAlways() {}

    /** The module finished sliding in. */
    void shown() {}

    /** The module started to slide out (or the screen closes): let go of focus and drags. */
    void hidden() {}
}
