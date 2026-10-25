package com.newuniverse.nusmp.relics;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** 3.12.0: the pieces of "head and feet relics are worn in the Curios slots" are joined up, and stay cheap and server-side. */
class HeadFeetWiringTest {
    static final Path JAVA = Path.of("src/main/java/com/newuniverse/nusmp/relics");

    private static String read(String file) throws IOException { return Files.readString(JAVA.resolve(file)); }

    @Test
    void theRightClickAndTheOncePerSecondPassUseTheRules() throws IOException {
        String equip = read("RelicCurioEquip.java");
        assertTrue(equip.contains("HeadFeetMoveRules.redirectOnUse(") && equip.contains("HeadFeetCurios.redirectSlot("), "the right-click asks the rules and the setting");
        assertTrue(equip.contains("SLOT_FULL && headFeet"), "a taken place is told and not put on as armour");
        String engine = read("RelicEngine.java");
        assertTrue(engine.contains("HeadFeetCurios.tick(p)"), "the move runs inside the existing once-a-second pass");
        assertTrue(engine.contains("HeadFeetCurios.shares(Relics.locatedList(p))"), "the kept armour points are asked in the same pass");
        assertTrue(engine.contains("HeadFeetCurios.forget(id)") && engine.contains("HeadFeetCurios.clear()"), "the back-off table is dropped on logout and on server stop");
        assertTrue(read("RelicSystem.java").contains("HeadFeetSelfTests.register()"));
    }

    @Test
    void theClientDoesNotPredictAnArmourSwapTheServerRefuses() throws IOException {
        String a = read("RelicArmorItem.java");
        int client = a.indexOf("level.isClientSide && !player.isShiftKeyDown() && HeadFeetCurios.redirectSlot(def) != null"), sup = a.indexOf("return super.use(level, player, hand)");
        assertTrue(client > 0 && sup > client, "the client returns before the vanilla swap for a relic the server redirects (else its armour slot shows a helmet the server never gave)");
    }

    @Test
    void theMoveNeverLosesTheStackAndAnsweringIsBounded() throws IOException {
        String m = read("HeadFeetCurios.java");
        int copy = m.indexOf("m.setStackInSlot(pick.index(), worn.copy())"), check = m.indexOf("m.getStackInSlot(pick.index()).isEmpty()"), clear = m.indexOf("p.setItemSlot(s, ItemStack.EMPTY)");
        assertTrue(copy > 0 && check > copy && clear > check, "copy into the Curios place first, check it took, only then empty the armour slot");
        assertTrue(m.contains("NOT_BEFORE") && m.contains("retryAt("), "a try that found no place backs off");
        assertTrue(m.contains("isAdopted()"), "older items of the mod (not in the tags) are never redirected");
        assertFalse(m.contains("getEntitiesOfClass") || m.contains("getChunk") || m.contains("Minecraft.getInstance"), "no scan, no client class");
    }

    @Test
    void theSettingIsAServerConfigWithTheOldBehaviourWhenOff() throws IOException {
        String c = read("RelicPowerConfig.java");
        assertTrue(c.contains("\"headFeetInCurios\"") && c.contains(".define(\"headFeetInCurios\", true)"), "on by default");
        assertTrue(Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/NUSMP.java")).contains("RelicPowerConfig.SPEC, \"nusmp-relicpower-server.toml\""), "registered as a SERVER config");
    }

    @Test
    void theSelfTestIsInTheSmokeList() throws IOException {
        String smoke = Files.readString(Path.of(".github/workflows/smoke.yml"));
        assertTrue(smoke.contains("relic.headfeet"), "the smoke test expects the new prefix");
    }

    @Test
    void theLangLinesExistInTheFragmentAndTheGameFile() throws IOException {
        String frag = Files.readString(Path.of("tools/lang/relics_wear.json")), lang = Files.readString(Path.of("src/main/resources/assets/nusmp/lang/en_us.json"));
        for (String key : new String[]{"message.nusmp.relic_wear.moved", "message.nusmp.relic_wear.slot_full"}) {
            assertTrue(frag.contains("\"" + key + "\""), key);
            assertTrue(lang.contains("\"" + key + "\""), key);
        }
    }
}
