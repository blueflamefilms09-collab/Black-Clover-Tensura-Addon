package com.newuniverse.nusmp.dragonballs;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * The CI self-tests of the taught words (prefix {@code dbwords.}, docs/dragon_ball_words.md). They need no other mod:
 * <ul>
 *   <li>{@code dbwords.rules}: the phrase of each set is cut into pieces that add up to the whole phrase after the last ball, never repeat a word and never skip one; the line is pasteable and the mask maths hold.</li>
 *   <li>{@code dbwords.teach}: a stand-in player who holds the balls one by one learns each of them exactly once (the saved mask grows by one bit, a second tick changes nothing), and the saved row is wiped by the operator call.</li>
 * </ul>
 */
public final class DbWordsSelfTests {
    private DbWordsSelfTests() {}

    public static void register() {
        SelfTests.register("dbwords.rules", DbWordsSelfTests::rules);
        SelfTests.register("dbwords.teach", DbWordsSelfTests::teach);
    }

    private static Result rules(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        int balls = DbConfig.count();
        for (BallSet s : BallSet.values()) {
            String phrase = DbTeach.phrase(s);
            List<String> words = PhraseLessons.words(phrase);
            if (words.isEmpty()) { bad.add(s.id() + " has no phrase to teach"); continue; }
            List<String> all = new ArrayList<>();
            for (int n = 1; n <= balls; n++) all.addAll(PhraseLessons.fresh(words, balls, n));
            if (!all.equals(words)) bad.add(s.id() + ": the pieces do not add up to the phrase: " + all + " vs " + words);
            if (!PhraseLessons.known(words, balls, balls).equals(words)) bad.add(s.id() + ": the last ball does not complete the phrase");
            if (!PhraseLessons.fresh(words, balls, balls).isEmpty()) bad.add(s.id() + ": the last ball adds words of its own");
            String line = PhraseLessons.line(words);
            if (line.isEmpty() || !Character.isUpperCase(line.charAt(0)) || !line.equalsIgnoreCase(String.join(" ", words))) bad.add(s.id() + ": the pasteable line is wrong: " + line);
        }
        if (PhraseLessons.count(PhraseLessons.with(PhraseLessons.with(0, 3), 3)) != 1 || PhraseLessons.has(PhraseLessons.with(0, 9), 9)) bad.add("the star mask is wrong");
        return bad.isEmpty() ? Result.ok(balls + " balls: both phrases (" + PhraseLessons.words(DbTeach.phrase(BallSet.EARTH)).size() + " and " + PhraseLessons.words(DbTeach.phrase(BallSet.SUPER)).size()
                + " words) are taught in pieces and complete on the last ball; teachWords=" + DbConfig.teachWords()) : Result.fail(String.join("; ", bad));
    }

    private static Result teach(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        FakePlayer p = SelfTests.standIn(server.overworld(), "dbwords_teach", 8.5, 100, 8.5);
        DbLessons l = DbLessons.get(server);
        try {
            l.forget(p.getUUID());
            int balls = DbConfig.count();
            if (DbConfig.teachWords()) {
                for (int star = 1; star <= balls; star++) {
                    DbTeach.teach(p, BallSet.EARTH, star);
                    int got = PhraseLessons.count(l.mask(p.getUUID(), BallSet.EARTH));
                    if (got != star) bad.add("after ball " + star + " the player knows " + got);
                    DbTeach.teach(p, BallSet.EARTH, star);
                    if (PhraseLessons.count(l.mask(p.getUUID(), BallSet.EARTH)) != star) bad.add("ball " + star + " was taught twice");
                }
                if (l.mask(p.getUUID(), BallSet.SUPER) != 0) bad.add("the Earth balls taught the Super call");
            }
            if (l.learn(p.getUUID(), BallSet.SUPER, 99) != 0) bad.add("a star outside 1..8 was accepted");
        } finally {
            l.forget(p.getUUID());
        }
        if (l.mask(p.getUUID(), BallSet.EARTH) != 0) bad.add("the row was not wiped");
        return bad.isEmpty() ? Result.ok("teachWords=" + DbConfig.teachWords() + ": each ball is learned once, in order, and the saved row is wiped") : Result.fail(String.join("; ", bad));
    }
}
