package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why (docs/vfx_square_bug.md): nothing visual can be run here, so the square bug is pinned from the files. (1) The PNG audit: every ring, circle, sigil, ripple, halo and glow sprite of
 * textures/particle has transparent corners; the ones that carry a colour under their transparent texels are listed, because they are exactly the ones vanilla's additive type would draw as squares and
 * the fix has to handle; every sprite an aura asks for exists. (2) The source pins: a batch of the VFX buffer is found by texture AND blend and sets the whole state it draws with, and the aura's
 * additive render type goes through the audit.
 */
class SquareBugAssetsTest {
    private static final Path RES = Path.of("src/main/resources/assets/nusmp/textures");
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Pattern SPRITE_NAME = Pattern.compile("(ring|circle|halo|glow|ripple|sigil|haze|puff|bloom|flare|mote|spark|glint)");

    private record Img(int w, int h, int[] argb) {}

    private static Img read(Path p) throws IOException {
        BufferedImage img = ImageIO.read(p.toFile());
        assertNotNull(img, p.toString());
        int w = img.getWidth(), h = img.getHeight();
        int[] argb = new int[w * h];
        img.getRGB(0, 0, w, h, argb, 0, w);
        return new Img(w, h, argb);
    }

    private static List<Path> pngs(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".png")).sorted().toList();
        }
    }

    // ---------------------------------------------------------------- the PNG audit

    @Test
    void theSpritesThatWereSquaresHaveTransparentCornersAndCarryColourUnderThem() throws IOException {
        for (String name : SquareBugAudit.SPRITES) {
            Img i = read(RES.resolve("particle/" + name + ".png"));
            assertTrue(TextureAlpha.cornersTransparent(i.w, i.h, i.argb, 16), name + " has an opaque corner");
        }
        for (String name : new String[]{"air_ring", "flamefx_ring", "waterfx_ripple", "earthfam_rock_ring", "glow"}) {
            Img i = read(RES.resolve("particle/" + name + ".png"));
            assertEquals(TextureAlpha.Kind.COLOUR_UNDER_ALPHA, TextureAlpha.classify(i.w, i.h, i.argb),
                    name + " is the kind of sprite that vanilla's additive type draws as a solid square: if it was repainted black under its alpha, take it off this list on purpose");
        }
    }

    @Test
    void everySquareRingCircleGlowSpriteOfTheParticleFolderHasTransparentCorners() throws IOException {
        List<String> bad = new ArrayList<>();
        int checked = 0;
        for (Path p : pngs(RES.resolve("particle"))) {
            String name = p.getFileName().toString();
            name = name.substring(0, name.length() - 4);
            if (!SPRITE_NAME.matcher(name).find() || name.startsWith("wiki_shell_")) continue;           // the wiki shells wrap a sphere, they are not flat sprites
            Img i = read(p);
            if (i.w != i.h) continue;                                                                 // a band (wraps a ring or a body) or an atlas, not a square sprite
            checked++;
            if (!TextureAlpha.cornersTransparent(i.w, i.h, i.argb, 16)) bad.add(name);
        }
        assertTrue(checked > 80, "the audit should see the sprites (saw " + checked + ")");
        assertTrue(bad.isEmpty(), "square sprites with an opaque corner would draw as a solid square: " + bad);
    }

    @Test
    void everySpriteAnAuraOrPropPainterAsksForExists() throws IOException {
        Pattern ask = Pattern.compile("\\btex\\(\"([a-z0-9_/]+)\"\\)");
        List<String> missing = new ArrayList<>();
        int asked = 0;
        for (String dir : new String[]{"client/aura", "client/prop"}) {
            for (Path p : sources(SRC.resolve(dir))) {
                Matcher m = ask.matcher(Files.readString(p));
                while (m.find()) {
                    asked++;
                    if (!Files.exists(RES.resolve(m.group(1) + ".png"))) missing.add(p.getFileName() + " -> " + m.group(1));
                }
            }
        }
        assertTrue(asked > 100, "the scan should see the painters' sprites (saw " + asked + ")");
        assertTrue(missing.isEmpty(), "a missing sprite is drawn as the missing-texture checker (a square): " + missing);
    }

    private static List<Path> sources(Path dir) throws IOException {
        try (Stream<Path> s = Files.walk(dir)) {
            return s.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    // ---------------------------------------------------------------- the source pins

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    @Test
    void aBatchOfTheVertexBufferIsFoundByTextureAndBlendAndSetsItsWholeState() throws IOException {
        String src = code(Files.readString(SRC.resolve("vfx/client/VfxVertexBuffer.java")));
        assertTrue(src.contains("final ResourceLocation texture;") && src.contains("final VfxBlend blend;"), "a batch must remember its texture and its blend");
        assertTrue(src.contains("byTexture.get(tex)") && src.contains("row[k]") && src.contains("blend.ordinal()"), "the batch table must be keyed by texture and then by blend");
        assertTrue(src.contains("tex == lastTex && blend == lastBlend"), "the shortcut must compare both texture and blend");
        int loop = src.indexOf("for (Batch b : sorted)");
        assertTrue(loop > 0);
        String body = src.substring(loop, src.indexOf("for (Batch b : order)", loop));
        for (String need : new String[]{"RenderSystem.setShader(", "RenderSystem.setShaderColor(1f, 1f, 1f, 1f)", "RenderSystem.depthMask(false)", "RenderSystem.disableCull()", "RenderSystem.enableDepthTest()",
                "RenderSystem.setShaderTexture(0, tex)", "b.blend.apply()", "guard(b.texture, b.blend)"})
            assertTrue(body.contains(need), "each batch must set " + need + " itself, inside the loop");
        assertFalse(body.contains("RenderSystem.setShaderTexture(0, b.texture)"), "the bound texture must be the guarded one");
    }

    @Test
    void theAuraAdditiveTypeGoesThroughTheAuditAndTheAlphaWeightedTypeBlendsBySourceAlpha() throws IOException {
        String aura = code(Files.readString(SRC.resolve("client/aura/AuraRender.java")));
        Matcher m = Pattern.compile("public static RenderType additive\\(ResourceLocation t\\)\\s*\\{([^}]*)\\}").matcher(aura);
        assertTrue(m.find());
        assertTrue(m.group(1).contains("AdditiveTypes.of(t)") && !m.group(1).contains("RenderType.eyes("), "AuraRender.additive must go through AdditiveTypes");
        String types = code(Files.readString(SRC.resolve("client/NURenderTypes.java")));
        assertTrue(types.contains("blendFunc(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE)"), "the alpha-weighted additive type is SRC_ALPHA, ONE");
        assertTrue(types.contains("setShaderState(RENDERTYPE_EYES_SHADER)") && types.contains("setWriteMaskState(COLOR_WRITE)"), "it keeps the eyes shader and the colour-only write mask");
        String chooser = code(Files.readString(SRC.resolve("client/AdditiveTypes.java")));
        assertTrue(chooser.contains("AdditivePolicy.choose(TextureAudit.kind(tex), forced)"), "the choice comes from the pure policy");
    }

    @Test
    void theTimeMagicLayerDoesNotKnowAboutTheFix() throws IOException {
        String time = Files.readString(SRC.resolve("vfx/client/layer/TimeMagicLayer.java"));
        for (String forbidden : new String[]{"AdditiveTypes", "AdditivePolicy", "TextureAudit", "SoftCircle", "VfxTextureGuard", "safeBlend"})
            assertFalse(time.contains(forbidden), "TimeMagicLayer must stay untouched: " + forbidden);
    }
}
