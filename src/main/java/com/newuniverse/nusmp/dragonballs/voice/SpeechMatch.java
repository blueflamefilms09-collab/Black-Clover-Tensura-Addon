package com.newuniverse.nusmp.dragonballs.voice;

import com.newuniverse.nusmp.dragonballs.DbRules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Why: a speech recogniser never writes the Namekian chant the way the config does ("takara doppo" for "takkaradoppo"). Before a recognised text is handed to the chat logic every heard word is pulled
 * onto the closest word of the ritual vocabulary: case and punctuation ignored, a per-word edit distance (0 for words of up to 3 letters, 1 for 4 to 7, 2 for 8 and more, measured on the vocabulary word),
 * a simple sound key (so "takkaradoppo" and "takaradopo" are the same word) and words that were split or fused ("takara doppo" / "ideyoshenron"). The result is plain normalised text that the unchanged chat
 * rules ({@link DbRules#matchesAny}) read exactly like typed words. Pure: no Minecraft type, no state. Cost: at most 64 words x the vocabulary (about 200 words) x a 40-letter edit distance per utterance.
 */
public final class SpeechMatch {
    private SpeechMatch() {}

    public static final int MAX_TOKENS = 64, MAX_WORD = 40, MAX_VOCAB = 400;

    /** Edit distance allowed for a vocabulary word of this length. */
    public static int tolerance(int len) { return len <= 3 ? 0 : len <= 7 ? 1 : 2; }

    /** Levenshtein distance, or max+1 as soon as it is certainly above max. */
    public static int distance(String a, String b, int max) {
        int n = a.length(), m = b.length();
        if (Math.abs(n - m) > max || n > MAX_WORD || m > MAX_WORD) return max + 1;
        int[] prev = new int[m + 1], cur = new int[m + 1];
        for (int j = 0; j <= m; j++) prev[j] = j;
        for (int i = 1; i <= n; i++) {
            cur[0] = i;
            int rowMin = cur[0];
            for (int j = 1; j <= m; j++) {
                int c = Math.min(Math.min(cur[j - 1], prev[j]) + 1, prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
                cur[j] = c;
                if (c < rowMin) rowMin = c;
            }
            if (rowMin > max) return max + 1;
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[m];
    }

    /** A rough sound key: c/q/k alike, ph/f, z/s, every vowel one vowel, doubled letters once. "takkaradoppo" and "takaradopo" give the same key. */
    public static String phoneticKey(String w) {
        String s = w.toLowerCase(Locale.ROOT).replace("ph", "f").replace("ck", "k").replace("x", "ks");
        StringBuilder b = new StringBuilder(s.length());
        char last = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == 'c' || c == 'q') c = 'k';
            else if (c == 'z') c = 's';
            else if ("eiouy".indexOf(c) >= 0) c = 'a';
            if (c != last) b.append(c);
            last = c;
        }
        return b.toString();
    }

    /** Does the heard word stand for the target (vocabulary) word? */
    public static boolean wordMatches(String heard, String target) {
        if (heard.equals(target)) return true;
        int tol = tolerance(target.length());
        if (distance(heard, target, tol) <= tol) return true;
        return heard.length() >= 5 && target.length() >= 5 && phoneticKey(heard).equals(phoneticKey(target));
    }

    /** The vocabulary word closest to the heard word (exact first, then the smallest distance), or null. */
    static String best(String heard, List<String> vocab) {
        String bestW = null;
        int bestD = Integer.MAX_VALUE;
        for (String v : vocab) {
            if (heard.equals(v)) return v;
            if (!wordMatches(heard, v)) continue;
            int d = distance(heard, v, 4);
            if (d < bestD) { bestD = d; bestW = v; }
        }
        return bestW;
    }

    /** The distinct single words of some phrases (normalised), in order: the ritual vocabulary. */
    public static List<String> vocabulary(Collection<? extends String> phrases) {
        Set<String> out = new LinkedHashSet<>();
        if (phrases != null)
            for (String p : phrases)
                for (String w : DbRules.norm(p).split(" "))
                    if (!w.isEmpty() && w.length() <= MAX_WORD && out.size() < MAX_VOCAB) out.add(w);
        return new ArrayList<>(out);
    }

    /**
     * The heard text with every word that is close to a vocabulary word replaced by that word, normalised (lower case, no punctuation). Words close to nothing stay as heard. Order of tries per word:
     * exact, two or three heard words joined ("takara doppo"), one close word, one word that is really two vocabulary words fused.
     */
    public static String canonicalize(String spoken, List<String> vocab) {
        String norm = DbRules.norm(spoken);
        if (norm.isEmpty()) return "";
        String[] t = norm.split(" ");
        int n = Math.min(t.length, MAX_TOKENS);
        Set<String> exact = new HashSet<>(vocab);
        StringBuilder out = new StringBuilder(norm.length() + 16);
        for (int i = 0; i < n; ) {
            String w = t[i];
            if (exact.contains(w)) { add(out, w); i++; continue; }
            String hit = null;
            int used = 1;
            for (int k = 2; k <= 3 && hit == null && i + k <= n; k++) {
                String j = String.join("", Arrays.copyOfRange(t, i, i + k));
                if (j.length() >= 5 && j.length() <= MAX_WORD) {
                    String b = best(j, vocab);
                    if (b != null) { hit = b; used = k; }
                }
            }
            if (hit == null) hit = best(w, vocab);
            if (hit != null) { add(out, hit); i += used; continue; }
            String split = w.length() >= 6 ? split(w, exact) : null;
            add(out, split != null ? split : w);
            i++;
        }
        return out.toString();
    }

    private static String split(String w, Set<String> exact) {
        for (int c = 3; c <= w.length() - 3; c++) {
            String a = w.substring(0, c), b = w.substring(c);
            if (exact.contains(a) && exact.contains(b)) return a + " " + b;
        }
        return null;
    }

    private static void add(StringBuilder b, String w) {
        if (b.length() > 0) b.append(' ');
        b.append(w);
    }

    /** The grammar a Vosk recogniser is given: a JSON list of the words and "[unk]" (the catch-all for every other sound). The words are letters and digits only, so no escaping is needed. */
    public static String grammarJson(List<String> vocab) {
        StringBuilder b = new StringBuilder("[");
        for (String w : vocab) b.append('"').append(w).append("\",");
        return b.append("\"[unk]\"]").toString();
    }
}
