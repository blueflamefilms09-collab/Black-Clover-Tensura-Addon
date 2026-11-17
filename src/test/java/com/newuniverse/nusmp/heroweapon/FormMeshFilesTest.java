package com.newuniverse.nusmp.heroweapon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.newuniverse.nusmp.client.relics.item.RangedStage;
import com.newuniverse.nusmp.heroweapon.forms.FormsSchema;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Why: a hero weapon form is drawn from the mesh {@code <weapon id>__<form id>} (docs/heroweapon/form_models.md), so a misnamed file is silently never drawn. Every
 * {@code weapon_meshes/*__*.txt} must name a real hero weapon and a form of that weapon's forms file (not {@code base}, and not a Legendary curse form, which keeps its
 * {@code _curse} look), and every non-pull mesh needs its skin and glow skin. With no such mesh shipped yet the file checks are vacuous; the name rules are also
 * exercised on hand-made names so a green run never means "nothing was looked at".
 */
class FormMeshFilesTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/anime_multiverse");
    private static final Path FORMS = Path.of("src/main/resources/data/anime_multiverse/hero/forms");
    private static final Pattern PULL = Pattern.compile("_pull[123]$");

    /** Null when {@code name} (a mesh id) is a valid form mesh id, else what is wrong. */
    static String check(String name) throws IOException {
        int cut = name.indexOf("__");
        if (cut <= 0) return name + ": no '<weapon>__<form>' in the name";
        String weapon = name.substring(0, cut);
        String form = PULL.matcher(name.substring(cut + 2)).replaceFirst("");
        HeroKind kind = HeroKind.byItemId(weapon);
        if (kind == null) return name + ": " + weapon + " is not a hero weapon id";
        if (form.equals("base")) return name + ": the base form has no form mesh (it uses " + weapon + ")";
        Path file = FORMS.resolve(kind.itemId() + ".json");
        if (!Files.exists(file)) return name + ": " + kind.itemId() + " has no forms file";
        HeroForm found = null;
        for (HeroForm f : FormsSchema.parseText(kind, Files.readString(file, StandardCharsets.UTF_8), null).forms()) if (f.id().equals(form)) found = f;
        if (found == null) return name + ": " + form + " is not a form of " + weapon;
        if (found.curse() && kind.isHoly()) return name + ": a Legendary curse form keeps the _curse look, it has no form mesh";
        return null;
    }

    @Test
    void everyFormMeshNamesAWeaponAndAForm() throws IOException {
        List<String> bad = new ArrayList<>();
        int seen = 0;
        Path dir = ASSETS.resolve("weapon_meshes");
        if (Files.isDirectory(dir)) {
            List<Path> list;
            try (Stream<Path> s = Files.list(dir)) {
                list = s.filter(p -> p.getFileName().toString().contains("__") && p.getFileName().toString().endsWith(".txt")).sorted().toList();
            }
            for (Path p : list) {
                seen++;
                String name = p.getFileName().toString();
                name = name.substring(0, name.length() - 4);
                String problem = check(name);
                if (problem != null) bad.add(problem);
                else if (!PULL.matcher(name).find()) {
                    if (!Files.exists(ASSETS.resolve("textures/entity/weapon/" + name + ".png"))) bad.add(name + ": the skin textures/entity/weapon/" + name + ".png is missing");
                    if (!Files.exists(ASSETS.resolve("textures/entity/weapon/" + name + "_glow.png"))) bad.add(name + ": the glow skin textures/entity/weapon/" + name + "_glow.png is missing");
                }
            }
        }
        System.out.println("[form meshes] " + seen + " form mesh files checked");
        assertTrue(bad.isEmpty(), "form mesh problems:\n" + String.join("\n", bad));
    }

    @Test
    void theNameRulesRejectWhatTheyShould() throws IOException {
        assertEquals(null, check("legendary_sword__flash_sword"), "a real form of a real weapon");
        assertEquals(null, check("legendary_bow__" + firstForm(HeroKind.BOW) + "_pull2"), "a bow draw stage resolves to its form");
        assertTrue(check("legendary_sword__base") != null, "base is refused");
        assertTrue(check("legendary_sword__no_such_form") != null, "an unknown form is refused");
        assertTrue(check("not_a_weapon__flash_sword") != null, "an unknown weapon is refused");
        assertTrue(check("legendary_sword") != null, "a plain id is not a form mesh");
    }

    /** The first non-base, non-curse form of a weapon (the name rules above need one that exists). */
    private static String firstForm(HeroKind kind) throws IOException {
        for (HeroForm f : FormsSchema.parseText(kind, Files.readString(FORMS.resolve(kind.itemId() + ".json"), StandardCharsets.UTF_8), null).forms())
            if (!f.id().equals("base") && !f.curse()) return f.id();
        throw new AssertionError(kind + " has no plain form");
    }

    @Test
    void aBowFormMeshResolvesItsDrawStagesLikeTheBaseBow() {
        assertEquals(List.of("legendary_bow__x_pull3", "legendary_bow__x_pull2", "legendary_bow__x_pull1", "legendary_bow__x"), RangedStage.chain("legendary_bow__x", RangedStage.PULL3));
        assertEquals(List.of("legendary_bow__x"), RangedStage.chain("legendary_bow__x", RangedStage.REST));
    }
}
