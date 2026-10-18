package com.newuniverse.nusmp.client.skilltree.layout;

/**
 * Why: Minecraft does not give a screen the window size, it gives the window size divided by the GUI scale, rounded up, and the scale itself is chosen by a rule that never lets the
 * GUI fall below 320 x 240. A layout test that only tries round numbers would miss exactly the sizes players have (427 x 240 from a 1280 x 720 window at scale 3, 480 x 270 from
 * 1920 x 1080 at scale 4). This is the game's own arithmetic, copied here so the tests can feed the layouts every size the game can produce.
 */
final class GuiScales {
    private GuiScales() {}

    /** The scale Minecraft picks for a framebuffer and a requested scale (0 = auto), as {@code Window.calculateScale(guiScale, false)} does. */
    static int scale(int fbW, int fbH, int requested) {
        int i = 1;
        while (i != requested && i < fbW && i < fbH && fbW / (i + 1) >= 320 && fbH / (i + 1) >= 240) i++;
        return i;
    }

    /** The GUI size {@code {width, height}} for a framebuffer at a scale: the division rounded up, as {@code Window.setGuiScale} does. */
    static int[] gui(int fbW, int fbH, int requested) {
        int s = scale(fbW, fbH, requested);
        return new int[]{(int) Math.ceil(fbW / (double) s), (int) Math.ceil(fbH / (double) s)};
    }

    /** Real window sizes players use, from the smallest the game accepts to 4K, each tried at auto and at GUI scale 1 to 5. */
    static final int[][] WINDOWS = {{640, 480}, {800, 600}, {854, 480}, {1024, 768}, {1280, 720}, {1366, 768}, {1600, 900}, {1920, 1080}, {2560, 1440}, {3840, 2160}};

    /** Every distinct GUI size the windows above produce at every scale, as {width, height} pairs. */
    static java.util.List<int[]> allSizes() {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (int[] w : WINDOWS) {
            for (int req = 0; req <= 5; req++) {
                int[] g = gui(w[0], w[1], req);
                if (seen.add(g[0] + "x" + g[1])) out.add(g);
            }
        }
        return out;
    }
}
