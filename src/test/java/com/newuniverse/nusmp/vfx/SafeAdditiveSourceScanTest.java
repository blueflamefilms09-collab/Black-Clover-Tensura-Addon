package com.newuniverse.nusmp.vfx;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 1.110.0 (docs/visual_upgrade/safe_additive.md): no client class may build an additive / emissive render type itself. Every pass asks {@code client/SafeAdditive}; only the three files below may
 * name the vanilla types or their blend shards. The allow-list is the whole exception list: SafeAdditive (the front door), AdditiveTypes (the audit that picks the type) and NURenderTypes (the one
 * place that builds custom composite states). The Time Magic layer builds no render type of its own and stays out of this.
 */
class SafeAdditiveSourceScanTest {
    private static final Path SRC = Path.of("src/main/java/com/newuniverse/nusmp");
    private static final Set<String> ALLOW = Set.of("client/SafeAdditive.java", "client/AdditiveTypes.java", "client/NURenderTypes.java");
    private static final String[] BANNED = {"RenderType.eyes(", "RenderType.lightning(", "RenderType.entityTranslucentEmissive(", "RenderType.energySwirl(", "RenderType.dragonExplosionAlpha(",
            "ADDITIVE_TRANSPARENCY", "LIGHTNING_TRANSPARENCY", "RENDERTYPE_EYES_SHADER", "RENDERTYPE_LIGHTNING_SHADER", "RENDERTYPE_ENERGY_SWIRL_SHADER"};

    private static String code(String src) { return src.replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", ""); }

    @Test
    void noClassOutsideTheCentralHelperBuildsAnAdditiveRenderType() throws IOException {
        List<String> bad = new ArrayList<>();
        try (Stream<Path> s = Files.walk(SRC)) {
            for (Path p : s.filter(f -> f.toString().endsWith(".java")).toList()) {
                String rel = SRC.relativize(p).toString().replace('\\', '/');
                if (ALLOW.contains(rel)) continue;
                String c = code(Files.readString(p));
                for (String b : BANNED) if (c.contains(b)) bad.add(rel + " uses " + b);
            }
        }
        assertTrue(bad.isEmpty(), "build the type through client/SafeAdditive: " + bad);
    }

    @Test
    void theHelperRoutesToTheAuditedTypes() throws IOException {
        String h = code(Files.readString(SRC.resolve("client/SafeAdditive.java")));
        assertTrue(h.contains("AdditiveTypes.of(tex)") && h.contains("VfxSafeBlendConfig.safeBlend()"), "the helper uses the audit and the safeBlend config");
        String t = code(Files.readString(SRC.resolve("client/AdditiveTypes.java")));
        assertTrue(t.contains("NURenderTypes.additiveAlpha(tex)"));
    }

    @Test
    void theTimeMagicLayerStaysUntouched() throws IOException {
        String time = Files.readString(SRC.resolve("vfx/client/layer/TimeMagicLayer.java"));
        assertFalse(time.contains("SafeAdditive"), "TimeMagicLayer must not know about the helper");
    }
}
