package com.newuniverse.nusmp.avatar.net;

/**
 * Why: strings that come from a client (an id, a name) are cut and cleaned before anything reads them, so a hostile packet cannot make the server hold
 * or print a megabyte of text or control codes. Pure.
 */
public final class NetText {
    private NetText() {}

    public static final int ID_MAX = 64, NAME_MAX = 48;

    /** Strips control characters and cuts to max characters. Never null. */
    public static String clean(String s, int max) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(Math.min(s.length(), max));
        for (int i = 0; i < s.length() && b.length() < max; i++) {
            char c = s.charAt(i);
            if (c >= 32 && c != 127 && c != '§') b.append(c);
        }
        return b.toString();
    }
}
