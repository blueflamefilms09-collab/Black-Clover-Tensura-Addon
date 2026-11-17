package com.newuniverse.nusmp.client.heroweapon;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * Why: the one-model-per-form look (docs/heroweapon/form_models.md) runs inside WeaponRenderer.meshId, for every hero weapon every frame, so it must stay cheap and
 * switchable: the hook reads the synced component, honours the client setting and the classic models, joins the id only through the cached helper (no string
 * concatenation in the per-frame lookup), and the component is written where the look is. Nothing here can run Minecraft, so the source is read.
 */
class FormModelsSourceScanTest {
    private static final Path MAIN = Path.of("src/main/java/com/newuniverse/nusmp");

    private static String read(String rel) throws IOException { return Files.readString(MAIN.resolve(rel), StandardCharsets.UTF_8); }

    private static String between(String s, String from, String to) {
        int a = s.indexOf(from);
        assertTrue(a >= 0, "missing: " + from);
        int b = s.indexOf(to, a);
        assertTrue(b > a, "missing: " + to);
        return s.substring(a, b);
    }

    @Test
    void theRendererHookIsCheapAndSwitchable() throws IOException {
        String src = read("client/WeaponRenderer.java");
        String hook = between(src, "static String meshId(ItemStack stack, String id)", "private static final java.util.HashMap<String, java.util.HashMap<String, String>> FORM_IDS");
        assertTrue(hook.contains("HeroComponents.form(stack)"), "the hook reads the hero_form component");
        assertTrue(hook.contains("HeroClientConfig.formModels()"), "the hook honours heroweapon.formModels");
        assertTrue(hook.contains("!com.newuniverse.nusmp.client.heroweapon.HeroClientConfig.classicModels()"), "the classic hero models never use form meshes");
        assertTrue(hook.contains("formMeshId(id, form)") && hook.contains("mesh(formMesh) != null"), "the form mesh is used only when it exists");
        assertTrue(hook.indexOf("formMeshId(") < hook.indexOf("\"curse\".equals"), "the form mesh comes before the curse look, which stays the fallback");
        assertFalse(hook.contains("\"__\""), "no string concatenation in the per-frame lookup: the joined id comes from the cache");
        assertFalse(hook.replace("id + \"_curse\"", "").contains(" + "), "no new concatenation in the per-frame lookup (only the old curse rule's id + \"_curse\")");
        String join = between(src, "static String formMeshId(String id, String form)", "@Override");
        assertTrue(join.indexOf("byForm.get(form)") < join.indexOf("\"__\""), "the cache is read before the id is joined");
        assertTrue(join.contains(".size() > "), "the cache is bounded (a form id comes from the server)");
    }

    @Test
    void theComponentIsRegisteredSyncedAndSetWithTheLook() throws IOException {
        String comps = read("heroweapon/HeroComponents.java");
        assertTrue(comps.contains("registerComponentType(\"hero_form\"") && comps.contains("registerComponentType(\"hero_look\""), "hero_form sits next to hero_look");
        assertTrue(comps.substring(comps.indexOf("\"hero_form\"")).split("\\)\\);")[0].contains("networkSynchronized"), "hero_form is synced to other clients");
        assertTrue(read("heroweapon/forms/FormLoadout.java").contains("stack.set(HeroComponents.FORM.get(), formId)"), "a form change writes the component with the look");
        assertTrue(read("heroweapon/bond/HeroLoan.java").contains("st.set(HeroComponents.FORM.get(), f.id())"), "a fresh projection carries the form");
    }

    @Test
    void theSettingIsAClientConfigDefaultOn() throws IOException {
        String cfg = read("client/heroweapon/HeroClientConfig.java");
        assertTrue(cfg.contains("define(\"formModels\", true)"), "heroweapon.formModels defaults to on");
        assertFalse(read("heroweapon/HeroComponents.java").contains("HeroClientConfig"), "common code does not touch the client config");
    }
}
