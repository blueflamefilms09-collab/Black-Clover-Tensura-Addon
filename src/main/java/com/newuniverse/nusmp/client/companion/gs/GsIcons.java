package com.newuniverse.nusmp.client.companion.gs;

/**
 * The little pixel icons of the Great Spirit talk screen (tabs, rapport leaves, stances, the follow switch, a taste hint): 9 by 9 bitmaps drawn as runs of filled rectangles, so they need no texture, scale
 * to any whole size and look the same in the game, the preview and the tests. '#' is the main colour, '+' the accent colour, '.' is empty. At most 9 rectangles a row, so an icon is at most 45 fills
 * (in practice about a dozen after merging runs).
 */
public final class GsIcons {
    private GsIcons() {}

    public enum Icon { TALK, HAND, HEART, BAG, SHIELD, SWORD, LEAF, PLAY, PAUSE, CROSS, GEM, ARROW, DIAMOND, STAR }

    public static final int SIZE = 9;

    private static final String[][] BITS = {
            /* TALK */ {".........", ".#######.", ".#+++++#.", ".#+++++#.", ".#+++++#.", ".#######.", "...##....", "..#......", "........."},
            /* HAND */ {"...#.#...", ".#.#.#.#.", ".#.#.#.#.", ".#######.", ".#######.", ".###+###.", "..#####..", "..#####..", "...###..."},
            /* HEART */ {".........", ".##...##.", "####.####", "#########", "#########", ".#######.", "..#####..", "...###...", "....#...."},
            /* BAG */ {"...###...", "....+....", "..#####..", ".#######.", "#########", "###+++###", "#########", ".#######.", "..#####.."},
            /* SHIELD */ {".#######.", "####+####", "####+####", "#########", "#########", ".#######.", "..#####..", "...###...", "....#...."},
            /* SWORD */ {"....#....", "...###...", "...#+#...", "...#+#...", "...#+#...", ".#######.", "....#....", "....#....", "...###..."},
            /* LEAF */ {"......###", "....#####", "...######", "..#####.#", ".#####.#.", ".####.#..", "#.##.#...", "#..#.....", ".#......."},
            /* PLAY */ {".........", "..#......", "..###....", "..#####..", "..#######", "..#####..", "..###....", "..#......", "........."},
            /* PAUSE */ {".........", ".###.###.", ".###.###.", ".###.###.", ".###.###.", ".###.###.", ".###.###.", ".###.###.", "........."},
            /* CROSS */ {"#.......#", ".#.....#.", "..#...#..", "...#.#...", "....#....", "...#.#...", "..#...#..", ".#.....#.", "#.......#"},
            /* GEM */ {"....#....", "...###...", "..#####..", ".#######.", "..#####..", "...###...", "....#....", ".........", "........."},
            /* ARROW */ {".........", ".#######.", "..#####..", "...###...", "....#....", ".........", ".........", ".........", "........."},
            /* DIAMOND */ {".........", ".........", "....#....", "...###...", "..#####..", "...###...", "....#....", ".........", "........."},
            /* STAR */ {"....#....", "....#....", "...###...", "#########", ".#######.", "..#####..", ".###.###.", ".##...##.", "#.......#"},
    };

    /** The rows of an icon (do not modify). */
    public static String[] rows(Icon i) { return BITS[i.ordinal()]; }

    /** How many filled rectangles one icon draws after merging runs (for the cost statement and the tests). */
    public static int rects(Icon i) {
        int n = 0;
        for (String row : BITS[i.ordinal()]) {
            char prev = '.';
            for (int x = 0; x < SIZE; x++) {
                char c = row.charAt(x);
                if (c != '.' && c != prev) n++;
                prev = c;
            }
        }
        return n;
    }

    /** Draws an icon with its top-left corner at x, y; {@code scale} whole pixels per texel; '#' in {@code main}, '+' in {@code accent} (0xAARRGGBB). */
    public static void paint(GsCanvas c, Icon icon, int x, int y, int scale, int main, int accent) {
        String[] rows = BITS[icon.ordinal()];
        for (int ry = 0; ry < SIZE; ry++) {
            String row = rows[ry];
            int x0 = 0;
            while (x0 < SIZE) {
                char ch = row.charAt(x0);
                if (ch == '.') {
                    x0++;
                    continue;
                }
                int x1 = x0 + 1;
                while (x1 < SIZE && row.charAt(x1) == ch) x1++;
                c.fill(x + x0 * scale, y + ry * scale, x + x1 * scale, y + (ry + 1) * scale, ch == '+' ? accent : main);
                x0 = x1;
            }
        }
    }
}
