package com.newuniverse.nusmp.grimoire;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 4.89.2: the grimoire book model is drawn solid (cut-out, both sides), never with the default translucent culled item type that made it see-through. */
class GrimoireSolidRenderTypeTest {
    @Test
    void bothTheShellAndTheResolvedViewDrawSolid() throws Exception {
        String src = Files.readString(Path.of("src/main/java/com/newuniverse/nusmp/client/grimoire/GrimoireBakedModel.java"));
        assertTrue(src.contains("entityCutoutNoCull(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)"));
        int n = src.split("getRenderTypes\\(net\\.minecraft\\.world\\.item\\.ItemStack stack, boolean fabulous\\)", -1).length - 1;
        assertEquals(2, n, "the shell and the view both override the item render type");
    }
}
