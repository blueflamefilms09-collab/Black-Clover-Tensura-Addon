package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the switch of the rebuilt rooms and its saved flag are config and save-format facts that the unit-test shim cannot load (they need NeoForge), so they are pinned on the source:
 * {@code gen.roomsRebuilt} is a SERVER config (it decides what the generator places) defaulting to true in its own file, registered once at start-up; the sector remembers whether it was built
 * with the rebuilt rooms and writes that flag only when it is true (an older save is byte for byte the same); a placed sector is checked against the rooms it was built with; the plan is made from
 * the shipped templates and only the piece's template id changes ({@link RoomVariants}); and the building code is server-only (no client class).
 */
class RoomsConfigSourceTest {
    private static final Path GEN = Path.of("src/main/java/com/newuniverse/nusmp/labyrinth/mega/gen");

    private static String code(String file) throws IOException {
        return Files.readString(GEN.resolve(file), StandardCharsets.UTF_8).replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("//[^\\n]*", "");
    }

    @Test
    void theSwitchIsAServerConfigOnByDefaultInItsOwnFile() throws IOException {
        String s = code("RoomsSettings.java");
        assertTrue(s.contains(".define(\"roomsRebuilt\", true)"), "gen.roomsRebuilt defaults to true");
        assertTrue(s.contains("ModConfig.Type.SERVER") && s.contains("\"nusmp-labyrinth-mega-rooms-server.toml\""), "a server config in its own file");
        assertFalse(s.contains("ModConfig.Type.CLIENT"), "it is not a client config");
        assertTrue(s.contains("b.push(\"gen\")"), "section [gen]");
        assertTrue(code("MegaGenRegistry.java").contains("RoomsSettings.register()") && code("MegaGenRegistry.java").contains("RoomsSelfTests.register()"), "registered at start-up with its self-test");
    }

    @Test
    void theSectorRemembersTheRoomsItWasBuiltWithAndOldSavesAreUnchanged() throws IOException {
        String d = code("GenData.java");
        assertTrue(d.contains("public boolean rooms;"), "the stat has the flag");
        assertTrue(d.contains("if (rooms) t.putBoolean(\"rr\", true);"), "written only when true: an older save is byte for byte the same");
        assertTrue(d.contains("s.rooms = t.getBoolean(\"rr\");"), "read back (an absent tag is false)");
        assertTrue(code("ExpansionQueue.java").contains("j.built.rebuilt()"), "the finished sector saves the flag it was built with");
        assertTrue(code("MegaCommand.java").contains("RoomsSettings.sectorRebuilt(server, s.id)"), "verify checks a placed sector against its own rooms");
    }

    @Test
    void onlyTheTemplateIdOfAPieceChangesAndAMissingVariantFallsBack() throws IOException {
        String g = code("SectorGenerator.java");
        assertTrue(g.contains("RoomVariants.assign(ids, r.seed, RoomVariants.table())"), "the rooms are chosen from the plan's own seed");
        assertTrue(g.contains("String templateId = rebuilt ? readable(mgr, use[p.index], p.shape.id) : p.shape.id;"), "the piece is built from the chosen template");
        assertTrue(g.contains("mgr.get(rl).isPresent()") && g.contains("return fallback;"), "a variant the game cannot read falls back to the shipped template");
        assertTrue(g.contains("markers.add(marker)") && g.contains("p.shape.markers"), "the markers still come from the plan's shape");
        assertTrue(code("GeometryCheck.java").contains("built.templates().get(i)"), "the world check reads the template the piece was really built from");
    }

    @Test
    void theNewGenerationCodeIsServerOnly() throws IOException {
        for (String f : new String[] {"RoomVariants.java", "RoomsSettings.java", "RoomsSelfTests.java", "RareStands.java"}) {
            String c = code(f);
            assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d") || c.contains("nusmp.client") || c.contains("Minecraft.getInstance"), f + " touches client code");
        }
        for (String f : new String[] {"ArmourStandGate.java", "ArmourStandRarity.java", "RareStandSet.java"}) {
            String c = code("../../props/" + f);
            assertFalse(c.contains("net.minecraft.client") || c.contains("com.mojang.blaze3d") || c.contains("nusmp.client") || c.contains("Minecraft.getInstance"), f + " touches client code");
        }
    }
}
