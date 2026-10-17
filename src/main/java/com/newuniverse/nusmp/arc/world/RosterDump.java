package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Why: the advancement generator is a Python script and cannot read the Java roster, but every character of the arcs deserves "met" or "defeated"
 * advancements and every saga a "cleared" one, and the roster only gets its final shape once the characters of all builders are merged. So the roster is
 * handed to the script as a plain text file of {@code id|name|saga|role} lines ({@code tools/quests/arc_roster.txt}) that the command
 * {@code /multiverse arc dump} writes from the live roster: refresh it after a merge, run the generator, done. This pure class is the one place that knows the
 * line format (the writer here, the Python reader in {@code tools/quests/spec_arc.py}, and a test that pins both), so neither can drift. A name or an id
 * never contains the separator (ids are snake_case; a bar in a display name is replaced), and the order is the roster's own (the story's).
 */
public final class RosterDump {
    private RosterDump() {}

    public static final char SEP = '|';

    /** One line of the file for one character: {@code id|name|saga|role}, with the saga's lower-case id and the role as {@code boss} or {@code npc}. */
    public static String line(ArcCharacter c) {
        return c.id() + SEP + clean(c.name()) + SEP + c.saga().id() + SEP + c.role().name().toLowerCase(Locale.ROOT);
    }

    /** The whole file body: one line per character in roster order, a final newline, and a header comment the Python reader skips. */
    public static String text(Collection<ArcCharacter> roster) {
        StringBuilder b = new StringBuilder("# id|name|saga|role : written by /multiverse arc dump, read by tools/quests/spec_arc.py\n");
        for (ArcCharacter c : roster) b.append(line(c)).append('\n');
        return b.toString();
    }

    /** The lines of a roster (no header), for a command that echoes them. */
    public static List<String> lines(Collection<ArcCharacter> roster) {
        List<String> out = new ArrayList<>();
        for (ArcCharacter c : roster) out.add(line(c));
        return out;
    }

    private static String clean(String s) { return s.replace(SEP, '/').replace('\n', ' ').replace('\r', ' ').trim(); }
}
