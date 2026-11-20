package com.newuniverse.nusmp.client.hub;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the hub's promises are about what the code does not contain: no tick handler of its own, no packet of its own, no config file of its own, no texture of its own, no per-frame list or string building, and no
 * lang key without a translation. A source scan keeps the next edit honest (docs/ui_hub.md, rules 9 and 11).
 */
class HubSourceScanTest {
    private static final Path DIR = Path.of("src/main/java/com/newuniverse/nusmp/client/hub");

    private static List<Path> files() throws IOException {
        try (Stream<Path> s = Files.list(DIR)) { return s.filter(p -> p.toString().endsWith(".java")).toList(); }
    }

    @Test
    void noOwnTickPacketOrConfigFile() throws IOException {
        for (Path p : files()) {
            String src = Files.readString(p);
            for (String banned : new String[] {"ClientTickEvent", "RenderGuiLayerEvent", "registerConfig(", "PacketDistributor", "sendToServer", "registerPayload", "ResourceLocation.fromNamespaceAndPath", "new ResourceLocation("})
                assertTrue(!src.contains(banned), p.getFileName() + " must not contain " + banned);
        }
    }

    @Test
    void screenDrawsNothingBeforeSuperRender() throws IOException {
        String src = Files.readString(DIR.resolve("MultiverseHubScreen.java"));
        int render = src.indexOf("public void render(");
        int sup = src.indexOf("super.render(", render);
        assertTrue(render > 0 && sup > render && sup - render < 120, "super.render must be the first statement of render");
        assertTrue(!src.contains("renderBlurredBackground"), "the hub background is flat: no blur");
    }

    @Test
    void everyLangKeyUsedHasATranslation() throws IOException {
        String frag = Files.readString(Path.of("tools/lang/hub.json"));
        Pattern use = Pattern.compile("tr\\(\"([a-z0-9.]+)\"");
        for (Path p : files()) {
            Matcher m = use.matcher(Files.readString(p));
            while (m.find()) {
                String key = m.group(1);
                if (key.endsWith(".")) continue;                                       // a prefix joined with a tab key at run time, checked below
                assertTrue(frag.contains("\"anime_multiverse.hub." + key + "\""), p.getFileName() + " uses a hub key without a translation: " + key);
            }
        }
        for (HubNodes.Tab t : HubNodes.Tab.values())
            for (String prefix : new String[] {"tab.", "desc.", "locked."})
                assertTrue(frag.contains("\"anime_multiverse.hub." + prefix + t.key + "\""), "missing hub." + prefix + t.key);
    }
}
