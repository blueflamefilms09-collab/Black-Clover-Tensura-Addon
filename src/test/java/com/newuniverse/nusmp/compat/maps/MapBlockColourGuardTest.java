package com.newuniverse.nusmp.compat.maps;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a custom block registered with no {@code mapColor} is {@code MapColor.NONE}, which vanilla maps (and mods that follow the rule) look THROUGH. This guard scans
 * every block registration in {@code src/main/java} (a {@code DeferredBlock<..> X = SOMETHING.register("id", ...)} statement, a {@code BLOCKS.register("id", ..)} call, a
 * {@code registerBlock}/{@code registerSimpleBlock}), decides from the source whether the block gets a colour (in the statement, in a helper method of the same file such as
 * {@code masonry()}, or in the block class's own source), compares the colour with {@code tools/compat/map_block_colours.json}, and fails when a block has none (unless its
 * id is whitelisted in that JSON with a reason), when the table lacks a registered block, or lists one that is not registered. Re-run it after merging other builders'
 * work (the {@code elite/} package adds blocks): see docs/compat/maps.md section 2. Source-text based on purpose: it needs neither Minecraft nor a bootstrapped registry.
 */
class MapBlockColourGuardTest {
    private static final Path SRC = Paths.get("src/main/java");
    private static final Path TABLE = Paths.get("tools/compat/map_block_colours.json");

    /** The vanilla MapColor constants a table entry may use (NONE is not allowed: it is the problem). */
    static final Set<String> VANILLA = Set.of("GRASS", "SAND", "WOOL", "FIRE", "ICE", "METAL", "PLANT", "SNOW", "CLAY", "DIRT", "STONE", "WATER", "WOOD", "QUARTZ",
            "COLOR_ORANGE", "COLOR_MAGENTA", "COLOR_LIGHT_BLUE", "COLOR_YELLOW", "COLOR_LIGHT_GREEN", "COLOR_PINK", "COLOR_GRAY", "COLOR_LIGHT_GRAY", "COLOR_CYAN",
            "COLOR_PURPLE", "COLOR_BLUE", "COLOR_BROWN", "COLOR_GREEN", "COLOR_RED", "COLOR_BLACK", "GOLD", "DIAMOND", "LAPIS", "EMERALD", "PODZOL", "NETHER",
            "TERRACOTTA_WHITE", "TERRACOTTA_ORANGE", "TERRACOTTA_MAGENTA", "TERRACOTTA_LIGHT_BLUE", "TERRACOTTA_YELLOW", "TERRACOTTA_LIGHT_GREEN", "TERRACOTTA_PINK",
            "TERRACOTTA_GRAY", "TERRACOTTA_LIGHT_GRAY", "TERRACOTTA_CYAN", "TERRACOTTA_PURPLE", "TERRACOTTA_BLUE", "TERRACOTTA_BROWN", "TERRACOTTA_GREEN",
            "TERRACOTTA_RED", "TERRACOTTA_BLACK", "CRIMSON_NYLIUM", "CRIMSON_STEM", "CRIMSON_HYPHAE", "WARPED_NYLIUM", "WARPED_STEM", "WARPED_HYPHAE",
            "WARPED_WART_BLOCK", "DEEPSLATE", "RAW_IRON", "GLOW_LICHEN");

    /** One block registration found in the sources. */
    record Reg(String id, String file, String statement, String colour) {
        boolean hasColour() { return colour != null; }
    }

    // ---------------------------------------------------------------- the scan (also used by the other tests of this package)
    static List<Reg> scan(Path root) throws IOException {
        Map<String, String> byName = new HashMap<>();                         // simple class name -> source text
        List<Path> files;
        try (Stream<Path> w = Files.walk(root)) { files = w.filter(p -> p.toString().endsWith(".java")).sorted().toList(); }
        for (Path f : files) byName.put(f.getFileName().toString().replace(".java", ""), Files.readString(f, StandardCharsets.UTF_8));
        List<Reg> out = new ArrayList<>();
        Pattern call = Pattern.compile("\\b\\w*BLOCKS\\w*\\s*\\.\\s*register(?:Simple)?(?:Block)?\\s*\\(\\s*\"([a-z0-9_/.]+)\"|\\.\\s*register(?:Simple)?Block\\s*\\(\\s*\"([a-z0-9_/.]+)\"");
        for (Path f : files) {
            String src = byName.get(f.getFileName().toString().replace(".java", ""));
            Matcher m = call.matcher(src);
            while (m.find()) {
                String id = m.group(1) != null ? m.group(1) : m.group(2);
                String stmt = statement(src, m.start());
                out.add(new Reg(id, root.relativize(f).toString().replace('\\', '/'), stmt, colourOf(stmt, src, byName)));
            }
        }
        return out;
    }

    /** The text from {@code from} to the ';' that ends the statement (parentheses and string literals are respected). */
    static String statement(String src, int from) {
        int depth = 0;
        boolean str = false;
        for (int i = from; i < src.length(); i++) {
            char c = src.charAt(i);
            if (str) { if (c == '\\') i++; else if (c == '"') str = false; continue; }
            if (c == '"') str = true;
            else if (c == '(') depth++;
            else if (c == ')') depth--;
            else if (c == ';' && depth <= 0) return src.substring(from, i);
        }
        return src.substring(from);
    }

    private static final Pattern COLOUR = Pattern.compile("mapColor\\s*\\(\\s*(?:[\\w.]*\\.)?MapColor\\.(\\w+)");
    private static final Pattern NEW_CLASS = Pattern.compile("new\\s+(\\w+)\\s*\\(|\\b(\\w+)::new");
    private static final Pattern CALL = Pattern.compile("\\b([a-z]\\w*)\\s*\\(\\s*\\)");

    /** The MapColor constant name the block gets: from the statement, from a no-argument helper method of the same file, or from the block class source. Null = none found. */
    static String colourOf(String stmt, String fileSrc, Map<String, String> byName) {
        Matcher m = COLOUR.matcher(stmt);
        if (m.find()) return m.group(1);
        Matcher h = CALL.matcher(stmt);
        while (h.find()) {                                                    // a helper such as masonry()
            Matcher d = Pattern.compile("\\b(?:BlockBehaviour\\.)?Properties\\s+" + h.group(1) + "\\s*\\(\\s*\\)\\s*\\{").matcher(fileSrc);
            if (d.find()) {
                String body = fileSrc.substring(d.end(), Math.min(fileSrc.length(), d.end() + 600));
                Matcher c = COLOUR.matcher(body);
                if (c.find()) return c.group(1);
            }
        }
        Matcher n = NEW_CLASS.matcher(stmt);
        while (n.find()) {                                                    // the block class sets it in its own constructor
            String cls = n.group(1) != null ? n.group(1) : n.group(2);
            String body = byName.get(cls);
            if (body == null) continue;
            Matcher c = COLOUR.matcher(body);
            if (c.find()) return c.group(1);
        }
        return null;
    }

    private static JsonObject table() throws IOException {
        return JsonParser.parseString(Files.readString(TABLE, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    // ---------------------------------------------------------------- tests
    @Test
    void theScanFindsTheModsBlocks() throws IOException {
        List<Reg> regs = scan(SRC);
        assertTrue(regs.size() >= 20, "the scan should find the mod's blocks, found " + regs.size());
        Set<String> ids = new TreeSet<>();
        for (Reg r : regs) ids.add(r.id());
        for (String must : List.of("labyrinth_bricks", "dimensional_rift", "relic_forge", "arena_return_sigil", "grimoire_altar", "underworld_matter"))
            assertTrue(ids.contains(must), "not found by the scan: " + must);
    }

    @Test
    void everyRegisteredBlockHasAMapColour() throws IOException {
        JsonObject wl = table().getAsJsonObject("whitelist");
        List<String> bad = new ArrayList<>();
        for (Reg r : scan(SRC)) {
            if (!r.hasColour() && !wl.has(r.id())) bad.add(r.id() + " (" + r.file() + ")");
        }
        assertTrue(bad.isEmpty(), "blocks registered without a mapColor (a vanilla map looks through them): " + bad
                + ". Add mapColor(MapColor.X) at the registration and a row to tools/compat/map_block_colours.json, or whitelist the id there with a reason.");
    }

    @Test
    void theTableAndTheSourcesAgree() throws IOException {
        JsonObject blocks = table().getAsJsonObject("blocks");
        Map<String, Reg> regs = new LinkedHashMap<>();
        for (Reg r : scan(SRC)) {
            assertNull(regs.put(r.id(), r), "block id registered twice: " + r.id());
        }
        List<String> problems = new ArrayList<>();
        for (Map.Entry<String, JsonElement> e : blocks.entrySet()) {
            String id = e.getKey(), colour = e.getValue().getAsString();
            if (!VANILLA.contains(colour)) problems.add(id + ": '" + colour + "' is not a usable MapColor constant (NONE is the problem itself)");
            Reg r = regs.get(id);
            if (r == null) problems.add(id + ": in the table but not registered in the sources");
            else if (r.hasColour() && !colour.equals(r.colour())) problems.add(id + ": table says " + colour + " but the source says " + r.colour());
        }
        for (Reg r : regs.values()) {
            if (!blocks.has(r.id()) && !table().getAsJsonObject("whitelist").has(r.id())) problems.add(r.id() + ": registered (" + r.file() + ") but missing from the table");
            if (r.hasColour() && !VANILLA.contains(r.colour())) problems.add(r.id() + ": source colour " + r.colour() + " is not a usable MapColor constant");
        }
        assertTrue(problems.isEmpty(), "map colour table out of step with the code (docs/compat/maps.md section 2): " + problems);
    }

    @Test
    void theScannerRecognisesAMissingColour() {
        // negative control: a statement without a colour, with no helper and no class source, must report none
        String src = "public static final DeferredBlock<Block> X = BLOCKS.register(\"x\", () -> new Block(BlockBehaviour.Properties.of().strength(1f)));";
        String stmt = statement(src, src.indexOf("BLOCKS"));
        assertNull(colourOf(stmt, src, Map.of()));
        String with = "BLOCKS.register(\"y\", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)))";
        assertEquals("STONE", colourOf(with, with, Map.of()));
        String full = "BLOCKS.register(\"z\", () -> new Block(BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.METAL)))";
        assertEquals("METAL", colourOf(full, full, Map.of()));
        String helper = "private static BlockBehaviour.Properties masonry() { return BlockBehaviour.Properties.of().mapColor(MapColor.DEEPSLATE); }\n"
                + "BLOCKS.register(\"w\", () -> new Block(masonry()));";
        assertEquals("DEEPSLATE", colourOf(statement(helper, helper.indexOf("BLOCKS")), helper, Map.of()));
        assertEquals("GREEN", colourOf("BLOCKS.register(\"c\", () -> new CBlock(BlockBehaviour.Properties.of()))", "", Map.of("CBlock", "super(props.mapColor(MapColor.GREEN))")));
    }
}
