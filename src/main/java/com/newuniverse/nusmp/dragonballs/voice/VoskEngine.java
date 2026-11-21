package com.newuniverse.nusmp.dragonballs.voice;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: Vosk (offline speech recognition, Apache 2.0) is the default recogniser, but it must not be a dependency of this mod: no jar of it is bundled and nothing here names a Vosk class at compile time. Its
 * classes ({@code org.vosk.Model}, {@code org.vosk.Recognizer}) are found by reflection, first on the normal class path, then in the jars of {@code voice.libraryPath} (vosk and jna). The model is the folder
 * {@code voice.modelPath}. When either is missing, {@link #load} logs ONE info line that says how to turn it on and returns null: the voice feature stays inert and nothing crashes. Runs on the engine's worker
 * thread only. The recogniser works on a grammar (a JSON list of the ritual words and "[unk]"), which is accurate and cheap; words the model does not know are dropped by Vosk itself.
 */
public final class VoskEngine implements SpeechEngine.Recogniser {
    private static final Logger LOG = LogUtils.getLogger();
    private static final int CHUNK = 4000;                       // samples (250 ms) per acceptWaveForm call

    private final Class<?> recognizerClass;
    private final Object model;
    private final Method accept, result, finalResult, reset;
    private Object rec;

    private VoskEngine(Class<?> modelClass, Class<?> recognizerClass, Object model) throws ReflectiveOperationException {
        this.recognizerClass = recognizerClass;
        this.model = model;
        this.accept = recognizerClass.getMethod("acceptWaveForm", byte[].class, int.class);
        this.result = recognizerClass.getMethod("getResult");
        this.finalResult = recognizerClass.getMethod("getFinalResult");
        this.reset = recognizerClass.getMethod("reset");
        newRecognizer("");
    }

    /** The Vosk recogniser, or null (with one info line) when the classes or the model are not there. */
    public static VoskEngine load(Path libraryDir, Path modelDir) {
        String why = null;
        try {
            if (!Files.isDirectory(modelDir)) {
                why = "no model folder at " + modelDir;
            } else {
                ClassLoader cl = loader(libraryDir);
                if (cl == null) {
                    why = "the Vosk classes (org.vosk.Model) were not found on the class path or in " + libraryDir;
                } else {
                    Class<?> modelClass = cl.loadClass("org.vosk.Model");
                    Class<?> recClass = cl.loadClass("org.vosk.Recognizer");
                    quietLog(cl);
                    Object model = modelClass.getConstructor(String.class).newInstance(modelDir.toAbsolutePath().toString());
                    VoskEngine e = new VoskEngine(modelClass, recClass, model);
                    LOG.info("[dbvoice] speech recognition is on (Vosk, model {})", modelDir);
                    return e;
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException | IOException e) {
            why = "Vosk could not be started: " + e;
        }
        LOG.info("[dbvoice] spoken Dragon Ball words are off: {}. To turn them on, put the vosk-java jar (and jna) into {} and unpack a Vosk model (for English: vosk-model-small-en-us from https://alphacephei.com/vosk/models) into {}; "
                + "see docs/dragon_balls_voice.md. Typed chat works as always.", why, libraryDir, modelDir);
        return null;
    }

    private static ClassLoader loader(Path libDir) throws IOException {
        ClassLoader own = VoskEngine.class.getClassLoader();
        try {
            Class.forName("org.vosk.Model", false, own);
            return own;
        } catch (ClassNotFoundException | LinkageError notThere) {
            // fall through to the library folder
        }
        if (!Files.isDirectory(libDir)) return null;
        List<URL> urls = new ArrayList<>();
        try (DirectoryStream<Path> jars = Files.newDirectoryStream(libDir, "*.jar")) {
            for (Path j : jars) urls.add(j.toUri().toURL());
        }
        if (urls.isEmpty()) return null;
        URLClassLoader u = new URLClassLoader(urls.toArray(new URL[0]), own);
        try {
            u.loadClass("org.vosk.Model");
            return u;
        } catch (ClassNotFoundException e) {
            u.close();
            return null;
        }
    }

    /** Best effort: Kaldi prints a lot at its default log level. */
    private static void quietLog(ClassLoader cl) {
        try {
            Class<?> lib = cl.loadClass("org.vosk.LibVosk"), level = cl.loadClass("org.vosk.LogLevel");
            lib.getMethod("setLogLevel", level).invoke(null, level.getField("WARNINGS").get(null));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            // an older vosk without these: the log is just louder
        }
    }

    private void newRecognizer(String grammar) throws ReflectiveOperationException {
        closeRec();
        Class<?> modelClass = model.getClass();
        rec = grammar.isEmpty() ? recognizerClass.getConstructor(modelClass, float.class).newInstance(model, 16000f)
                : recognizerClass.getConstructor(modelClass, float.class, String.class).newInstance(model, 16000f, grammar);
    }

    private void closeRec() {
        if (rec instanceof AutoCloseable c) {
            try { c.close(); } catch (Exception e) { LOG.debug("[dbvoice] closing a recognizer failed: {}", e.toString()); }
        }
        rec = null;
    }

    @Override
    public void grammar(String json) {
        try { newRecognizer(json == null ? "" : json); } catch (ReflectiveOperationException | LinkageError | RuntimeException e) { throw new IllegalStateException("no recognizer: " + e, e); }
    }

    @Override
    public String recognise(short[] pcm) {
        if (rec == null) return "";
        try {
            StringBuilder out = new StringBuilder();
            for (int from = 0; from < pcm.length; from += CHUNK) {
                int n = Math.min(CHUNK, pcm.length - from);
                byte[] b = new byte[n * 2];
                for (int i = 0; i < n; i++) {
                    b[2 * i] = (byte) pcm[from + i];
                    b[2 * i + 1] = (byte) (pcm[from + i] >> 8);
                }
                if (Boolean.TRUE.equals(accept.invoke(rec, b, b.length))) append(out, (String) result.invoke(rec));   // Vosk found an endpoint inside the utterance
            }
            append(out, (String) finalResult.invoke(rec));
            reset.invoke(rec);
            return out.toString().trim();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("recognition failed: " + e, e);
        }
    }

    /** Adds the "text" of a Vosk JSON result, without Vosk's "[unk]" marks (sounds it could not place). */
    static void append(StringBuilder out, String json) {
        if (json == null) return;
        JsonElement t = JsonParser.parseString(json).getAsJsonObject().get("text");
        if (t == null || !t.isJsonPrimitive()) return;
        for (String w : t.getAsString().split("\\s+")) {
            if (w.isEmpty() || w.equals("[unk]")) continue;
            if (out.length() > 0) out.append(' ');
            out.append(w);
        }
    }

    @Override
    public void close() {
        closeRec();
        if (model instanceof AutoCloseable c) {
            try { c.close(); } catch (Exception e) { LOG.debug("[dbvoice] closing the model failed: {}", e.toString()); }
        }
    }
}
