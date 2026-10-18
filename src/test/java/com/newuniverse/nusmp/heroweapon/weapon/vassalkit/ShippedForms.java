package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Why: the forms tests must read the file that really ships, so this test helper reads a weapon's forms file from the class path (Gradle) or straight from the source tree (the local harness). */
public final class ShippedForms {
    private ShippedForms() {}

    public static JsonObject load(String itemId) {
        String rel = "data/nusmp/hero/forms/" + itemId + ".json";
        try (InputStream in = ShippedForms.class.getResourceAsStream("/" + rel)) {
            if (in != null) return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException ignored) {
            // fall back to the source tree below
        }
        try {
            return JsonParser.parseString(Files.readString(Path.of("src/main/resources/" + rel), StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException("cannot read the forms file " + rel + " (looked on the class path and under src/main/resources)", e);
        }
    }
}
