package com.newuniverse.nusmp.heroweapon.weapon.strike;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.heroweapon.weapon.axe.AxeNumbers;
import com.newuniverse.nusmp.heroweapon.weapon.claw.ClawNumbers;
import com.newuniverse.nusmp.heroweapon.weapon.hammer.HammerNumbers;
import com.newuniverse.nusmp.heroweapon.weapon.whip.WhipNumbers;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: the forms files of the four striking weapons are shipped data that nothing but the running game reads, and a wrong skill id, a missing parent or a stat over the budget
 * would only show as a skipped form. This test reads each shipped file from the source tree and judges it with {@link FormsCheck} against what the weapon's own numbers table says
 * it has (its skill ids, its ultimate, its passives), so a form that names a skill the weapon does not have, a skill no form can reach, or a stat over the budget fails the build. It also pins the forms each weapon is documented to
 * have and that only the Claw and the Axe carry a (single, small) curse form.
 */
class StrikeFormsTest {
    private static final Path FORMS = Path.of("src/main/resources/data/nusmp/hero/forms");

    private static JsonObject read(String itemId) throws IOException {
        return JsonParser.parseString(Files.readString(FORMS.resolve(itemId + ".json"), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void theHammerFileIsCorrect() throws IOException {
        FormsCheck.Known known = new FormsCheck.Known("vassal_hammer", HammerNumbers.IDS, HammerNumbers.ULTIMATES, HammerNumbers.PASSIVES, 3, 5, false);
        assertEquals(List.of(), FormsCheck.problems(read("vassal_hammer"), known));
    }

    @Test
    void theClawFileIsCorrect() throws IOException {
        FormsCheck.Known known = new FormsCheck.Known("vassal_claw", ClawNumbers.IDS, ClawNumbers.ULTIMATES, ClawNumbers.PASSIVES, 3, 5, true);
        assertEquals(List.of(), FormsCheck.problems(read("vassal_claw"), known));
    }

    @Test
    void theAxeFileIsCorrect() throws IOException {
        FormsCheck.Known known = new FormsCheck.Known("vassal_axe", AxeNumbers.IDS, AxeNumbers.ULTIMATES, AxeNumbers.PASSIVES, 3, 5, true);
        assertEquals(List.of(), FormsCheck.problems(read("vassal_axe"), known));
    }

    @Test
    void theWhipFileIsCorrect() throws IOException {
        FormsCheck.Known known = new FormsCheck.Known("vassal_whip", WhipNumbers.IDS, WhipNumbers.ULTIMATES, WhipNumbers.PASSIVES, 3, 5, false);
        assertEquals(List.of(), FormsCheck.problems(read("vassal_whip"), known));
    }

    private static List<String> formIds(JsonObject root) {
        List<String> out = new ArrayList<>();
        for (JsonElement f : root.getAsJsonArray("forms")) out.add(f.getAsJsonObject().get("id").getAsString());
        return out;
    }

    private static List<String> curseIds(JsonObject root) {
        List<String> out = new ArrayList<>();
        for (JsonElement f : root.getAsJsonArray("forms")) if (f.getAsJsonObject().get("curse").getAsBoolean()) out.add(f.getAsJsonObject().get("id").getAsString());
        return out;
    }

    @Test
    void theShippedFilesHaveTheFormsTheDocsPromise() throws IOException {
        assertEquals(List.of("base", "stone_hammer", "anvil_hammer", "synthesis_hammer"), formIds(read("vassal_hammer")));
        assertEquals(List.of("base", "bone_claws", "rope_claws", "dragon_head_claws", "cursed_claw"), formIds(read("vassal_claw")));
        assertEquals(List.of("base", "timber_axe", "pipe_axe", "shine_axe", "curse_axe"), formIds(read("vassal_axe")));
        assertEquals(List.of("base", "vine_whip", "storm_whip", "morning_star_whip", "hourglass_whip"), formIds(read("vassal_whip")));
    }

    @Test
    void onlyTheClawAndTheAxeCarryASingleSmallCurseForm() throws IOException {
        assertEquals(List.of("cursed_claw"), curseIds(read("vassal_claw")));
        assertEquals(List.of("curse_axe"), curseIds(read("vassal_axe")));
        assertEquals(List.of(), curseIds(read("vassal_hammer")));
        assertEquals(List.of(), curseIds(read("vassal_whip")));
    }
}
