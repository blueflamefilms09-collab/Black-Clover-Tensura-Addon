package com.newuniverse.nusmp.dragonballs.voice;

import com.newuniverse.nusmp.dragonballs.BallSet;
import com.newuniverse.nusmp.dragonballs.DbConfig;
import com.newuniverse.nusmp.dragonballs.DbRules;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests.Result;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The CI self-tests of the spoken Dragon Ball words (the smoke list entry is the prefix {@code dbvoice.}). Neither Simple Voice Chat nor Vosk has to be installed:
 * <ul>
 *   <li>{@code dbvoice.rules}: the fuzzy matcher turns mangled versions of every default phrase back into text the unchanged chat rules accept; the gate, the utterance limits and the resampler hold.</li>
 *   <li>{@code dbvoice.bridge}: the bridge does not throw for an unknown player or empty text; a stand-in recogniser behind the bounded engine turns a burst of fake audio into exactly one text.</li>
 * </ul>
 */
public final class DbVoiceSelfTests {
    private DbVoiceSelfTests() {}

    public static void register() {
        SelfTests.register("dbvoice.rules", DbVoiceSelfTests::rules);
        SelfTests.register("dbvoice.bridge", DbVoiceSelfTests::bridge);
    }

    private static Result rules(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        List<String> vocab = SpeechMatch.vocabulary(new DefaultVocabulary().words());
        if (vocab.isEmpty()) bad.add("the vocabulary is empty");
        for (BallSet s : BallSet.values())
            for (String ph : s.defaultPhrases()) {
                if (!DbRules.matchesAny(SpeechMatch.canonicalize(ph.toUpperCase() + "!!", vocab), s.defaultPhrases())) bad.add("the phrase '" + ph + "' does not survive the matcher");
            }
        if (!DbRules.matchesAny(SpeechMatch.canonicalize("takara doppo pakkora doppo yaikanan", vocab), BallSet.EARTH.defaultPhrases())) bad.add("a mangled Namekian chant is not recognised");
        if (!DbRules.matchesAny(SpeechMatch.canonicalize("come fourth dragon of the gods", vocab), BallSet.SUPER.defaultPhrases())) bad.add("a mangled Super call is not recognised");
        if (DbRules.matchesAny(SpeechMatch.canonicalize("what a lovely day", vocab), BallSet.EARTH.defaultPhrases())) bad.add("ordinary talk summons the dragon");
        List<java.util.UUID> a = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        if (VoiceGate.select(List.of(), a, java.util.Set.of(), 2).size() != 2 || !VoiceGate.select(List.of(), a, java.util.Set.of(), 0).isEmpty()) bad.add("the listener cap is wrong");
        if (!VoiceGate.eligible(true, 0, 7, true) || VoiceGate.eligible(false, 6, 7, false) || VoiceGate.eligible(false, 7, 7, true) || !VoiceGate.eligible(false, 7, 7, false)) bad.add("the gate rules are wrong");
        if (!Utterance.admit(Utterance.MAX_QUEUED - 1) || Utterance.admit(Utterance.MAX_QUEUED)) bad.add("the queue limit per player is wrong");
        if (Resampler.to16k(new short[960], 48000).length != 320) bad.add("the resampler does not turn 960 samples at 48 kHz into 320");
        return bad.isEmpty() ? Result.ok("phrases survive the matcher, mangled chants match, talk does not, gate / limits / resampler hold; vocabulary " + vocab.size() + " words")
                : Result.fail(String.join("; ", bad));
    }

    private static Result bridge(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        try {
            if (SpeechBridge.heard(UUID.randomUUID(), "come forth shenron")) bad.add("an unknown player was heard");
            if (SpeechBridge.heard(UUID.randomUUID(), "")) bad.add("empty text was heard");
            if (SpeechBridge.heard(null, "x") || SpeechBridge.heard(UUID.randomUUID(), null)) bad.add("null input was heard");
        } catch (RuntimeException e) {
            bad.add("the bridge threw: " + e);
        }
        CountDownLatch got = new CountDownLatch(1);
        AtomicReference<String> text = new AtomicReference<>();
        BoundedEngine engine = new BoundedEngine(() -> pcm -> "hello", (id, t) -> { text.set(t); got.countDown(); });
        try {
            UUID id = UUID.randomUUID();
            long until = System.currentTimeMillis() + 3000;
            while (!engine.available() && System.currentTimeMillis() < until) Thread.onSpinWait();
            if (!engine.available()) {
                bad.add("the stand-in engine did not start");
            } else {
                short[] loud = new short[320];
                java.util.Arrays.fill(loud, (short) 8000);
                for (int i = 0; i < 12; i++) engine.feed(id, loud, 0.25f);
                engine.endTransmission(id);
                if (!got.await(3, TimeUnit.SECONDS) || !"hello".equals(text.get())) bad.add("the stand-in engine gave no text");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            bad.add("interrupted");
        } finally {
            engine.close();
        }
        return bad.isEmpty() ? Result.ok("the bridge ignores unknown players and empty text; the bounded engine turns fake audio into one text (voice.enabled=" + DbVoiceConfig.enabled() + ", dragonBalls.enabled=" + DbConfig.enabled() + ")")
                : Result.fail(String.join("; ", bad));
    }
}
