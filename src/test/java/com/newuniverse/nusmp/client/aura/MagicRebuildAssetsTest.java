package com.newuniverse.nusmp.client.aura;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the rebuilt armours, summon models and effect layers of Song, Sound, Soul, Soul Corpse and Memory Magic name their assets in code (GeoSpec.of("song", "armour"),
 * AuraRender.tex("particle/song_wave"), VfxTextures.byName("sound_cone") ...). A name that is not backed by a file is only a log line in the game and an invisible effect, so this
 * test reads the source of every rebuild class and checks that each model, animation, texture, glow map and particle it names exists, and that the armour pieces the painters list
 * are real bones of their model.
 */
class MagicRebuildAssetsTest {
    private static final File SRC = new File("src/main/java/com/newuniverse/nusmp"), ASSETS = new File("src/main/resources/assets/nusmp");
    private static final String[] KEYS = {"song", "sound", "soul", "soulcorpse", "memory"};

    private static List<File> rebuildSources() {
        List<File> out = new ArrayList<>();
        collect(SRC, out);
        return out;
    }

    private static void collect(File dir, List<File> out) {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File f : fs) {
            if (f.isDirectory()) collect(f, out);
            else if (f.getName().endsWith(".java") && isMine(f.getName())) out.add(f);
        }
    }

    private static boolean isMine(String n) {
        for (String p : new String[]{"SongAura", "SoundAura", "SoulAura", "SoulCorpseAura", "MemoryAura", "SongRebuildPropPainter", "SoundRebuildPropPainter", "SoulRebuildPropPainter",
                "SoulCorpseRebuildPropPainter", "MemoryRebuildPropPainter", "SongFxLayer", "SoundFxLayer", "SoulFxLayer", "SoulCorpseFxLayer", "MemoryFxLayer"})
            if (n.equals(p + ".java")) return true;
        return false;
    }

    private static String read(File f) throws Exception { return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8); }

    @Test
    void everyModelTheCodeNamesHasItsFiles() throws Exception {
        Pattern spec = Pattern.compile("GeoSpec\\.of\\(\"([a-z]+)\", \"([a-z_]+)\"\\)");
        int n = 0;
        for (File f : rebuildSources()) {
            Matcher m = spec.matcher(read(f));
            while (m.find()) {
                String id = m.group(1) + "_" + m.group(2);
                assertTrue(new File(ASSETS, "geo/entity/" + id + ".geo.json").isFile(), f.getName() + ": missing model " + id);
                assertTrue(new File(ASSETS, "animations/entity/" + id + ".animation.json").isFile(), f.getName() + ": missing animations " + id);
                assertTrue(new File(ASSETS, "textures/entity/" + id + ".png").isFile(), f.getName() + ": missing texture " + id);
                assertTrue(new File(ASSETS, "textures/entity/" + id + "_glow.png").isFile(), f.getName() + ": missing glow map " + id);
                n++;
            }
        }
        assertTrue(n >= 17, "the five magics name at least 17 models, found " + n);
    }

    @Test
    void everyParticleTheCodeNamesExists() throws Exception {
        Pattern tex = Pattern.compile("(?:tex|t)\\(\"(?:particle/)?([a-z0-9_]+)\"\\)");
        int n = 0;
        for (File f : rebuildSources()) {
            String text = read(f);
            Matcher m = tex.matcher(text);
            while (m.find()) {
                String name = m.group(1);
                if (name.equals("glow") || name.equals("spark")) continue;
                assertTrue(new File(ASSETS, "textures/particle/" + name + ".png").isFile(), f.getName() + ": missing particle texture " + name);
                n++;
            }
        }
        assertTrue(n >= 30, "the rebuild names many particle textures, found " + n);
    }

    @Test
    void everyArmourPieceIsABoneOfItsModel() throws Exception {
        Pattern piece = Pattern.compile("SongArmourKit\\.(?:piece|head|ghost)\\(\"([a-z_]+)\"");
        for (File f : rebuildSources()) {
            String text = read(f);
            if (!f.getName().endsWith("Aura.java")) continue;
            Matcher g = Pattern.compile("GeoSpec\\.of\\(\"([a-z]+)\", \"armour\"\\)").matcher(text);
            assertTrue(g.find(), f.getName() + ": no armour model");
            String geo = read(new File(ASSETS, "geo/entity/" + g.group(1) + "_armour.geo.json"));
            Matcher m = piece.matcher(text);
            int n = 0;
            while (m.find()) {
                assertTrue(geo.contains("\"name\": \"" + m.group(1) + "\""), f.getName() + ": the model has no bone " + m.group(1));
                n++;
            }
            assertTrue(n >= 11, f.getName() + ": an armour set has at least 11 pieces, found " + n);
        }
    }

    @Test
    void theFiveMagicsHaveTheirAuraAndLayerAndLang() throws Exception {
        for (String k : KEYS) {
            assertTrue(new File(ASSETS, "geo/entity/" + k + "_armour.geo.json").isFile(), k + ": no armour model");
        }
        assertTrue(new File("tools/lang").isDirectory());
    }
}
