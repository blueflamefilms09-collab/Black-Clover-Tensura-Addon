package com.newuniverse.nusmp.heroweapon.weapon.carriage;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 5.0.0: the carriage entity follows the weapon's form, picks up on sneak + right click, and the item is not drawn while the carriage stands. */
class CarriageFollowsFormTest {
    private static String read(String p) throws Exception { return Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/" + p)); }

    @Test
    void theEntityCarriesTheFormAndRefreshesItFromTheBond() throws Exception {
        String e = read("heroweapon/weapon/carriage/CarriageEntity.java");
        assertTrue(e.contains("DATA_FORM") && e.contains("setForm(HeroBonds.of(owner).form())") && e.contains("summon(ServerLevel level, ServerPlayer owner, CarriageNumbers.Chassis chassis, Vec3 spot, float yaw, String form)"));
        assertTrue(e.contains("sp.isShiftKeyDown() && sp.getUUID().equals(ownerUuid())"), "the owner cannot pick the carriage up");
    }

    @Test
    void theRendererDrawsTheMeshAndSkinOfTheForm() throws Exception {
        String m = read("client/heroweapon/carriage/CarriageMesh.java"), r = read("client/heroweapon/carriage/CarriageRenderer.java");
        assertTrue(m.contains("\"vassal_carriage__\" + form") && r.contains("CarriageMesh.get(entity.form())"));
        for (String form : new String[]{"war_carriage", "queens_chariot", "filolial_carriage", "hourglass_sand_carriage"}) {
            assertTrue(Files.exists(Path.of("src/main/resources/assets/anime_multiverse/weapon_meshes/vassal_carriage__" + form + ".txt")), form + " mesh");
            assertTrue(Files.exists(Path.of("src/main/resources/assets/anime_multiverse/textures/entity/weapon/vassal_carriage__" + form + ".png")), form + " skin");
        }
    }

    @Test
    void theItemIsNotDrawnInTheHandWhileTheCarriageStands() throws Exception {
        String w = read("client/WeaponRenderer.java");
        assertTrue(w.contains("carriageStands(stack, ctx, mc)") && w.contains("CarriageEntity.standingFor("));
        assertTrue(read("client/heroweapon/HeroClientConfig.java").contains("\"carriageInWorld\", true"));
    }
}
