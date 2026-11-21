package com.newuniverse.nusmp.dragonballs.voice;

import java.util.UUID;

/**
 * Why: the voice side only needs "give audio of player X, tell me when a text came out". Everything behind it (which recogniser, which thread) is hidden, so a stand-in can replace the real one in a
 * test and in the server self-test. Audio is 16 kHz mono 16-bit; it is never stored beyond the utterance being recognised and never leaves the process.
 */
public interface SpeechEngine {
    /** The part that really turns audio into text. Called on the engine's one worker thread only. */
    interface Recogniser {
        /** The text of one finished utterance ("" when nothing was recognised). */
        String recognise(short[] pcm16k);

        /** A new grammar (a JSON list of words), or "" for free recognition. Called on the worker thread, before the next utterance. */
        default void grammar(String json) {}

        default void close() {}
    }

    /** Where recognised texts go. Called on the worker thread: it must only hand the text to the server thread. */
    @FunctionalInterface
    interface Sink {
        void heard(UUID player, String text);
    }

    /** Is a recogniser loaded and running? False while it loads and for good when it could not be loaded. */
    boolean available();

    /** Voice thread: one 20 ms frame of a listened-to player, with its loudness (RMS 0..1). Cheap, never blocks. */
    void feed(UUID player, short[] pcm16k, float rms);

    /** Voice thread: the player stopped transmitting; an open utterance ends now. */
    void endTransmission(UUID player);

    /** The player is no longer listened to (or left): drop the half-heard utterance. */
    void forget(UUID player);

    /** Replaces the recogniser's grammar (applied before the next utterance). */
    void setGrammar(String json);

    /** Utterances thrown away because a queue was full or too old, since the start. */
    long dropped();

    void close();
}
