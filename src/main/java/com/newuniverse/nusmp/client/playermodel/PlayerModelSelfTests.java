package com.newuniverse.nusmp.client.playermodel;

import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfile;
import com.newuniverse.nusmp.labyrinth.mega.guard.DndProfiles;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: nothing of the player-model kit can be seen by its author, so what can be proved without a person is proved by the real game: the CI server smoke test boots the mod and runs
 * {@code render.playermodel}, which reads the kit's data out of the jar exactly as the client would ({@link PlayerModelAudit}): the hair table parses, every style's sheet exists and every box is
 * inside it with the scalp cap on the hat layer's UV, every look's skin (and glow map) exists at 64x64, every haircut it names exists, every extras model has its {@code x_<anchor>} roots and
 * textures, every look a body asks for by name is in the table, every converted arc boss has a look, and every old avatar hair id maps to a haircut. It touches no client class, so it runs on
 * the dedicated server; the drawing itself is checked by the unit tests of the pure parts and by the preview images (docs/playermodel.md). Registered from the mod constructor.
 *
 * <p>1.99.0 adds {@code render.playermodel2} (phase 2, docs/playermodel_phase2.md): the same data audit for the dungeon's humanoids ({@link DungeonAudit}: the merged tables, forty distinct archetype
 * skins, every Foundry mob a person with a look or a listed creature, the puppets' twelve skins, the loader of the abyssal bosses, the flag's rule) PLUS the parts only the real game can say:
 * every entity type the phase converts is registered, and every loaded Decennial profile that names a {@code look} resolves, in each of its three phases, to a look of the table (a humanoid
 * boss) or to its own geo model (a creature), so a profile of the next builder's two hundred bosses that names a look nobody drew fails here and not in front of a player.
 */
public final class PlayerModelSelfTests {
    private PlayerModelSelfTests() {}

    private static boolean registered;

    public static void register() {
        if (registered) return;
        registered = true;
        SelfTests.register("render.playermodel", server -> {
            List<String> bad = PlayerModelAudit.run(PlayerModelAudit::classpath);
            if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems, first: " + bad.get(0));
            return SelfTests.Result.ok("hair, skins, glow maps, extras and legacy hair ids all resolve");
        });
        SelfTests.register("render.playermodel2", server -> {
            List<String> bad = new ArrayList<>(DungeonAudit.run(PlayerModelAudit::classpath));
            for (String id : DungeonLooks.ENTITIES)
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(ResourceLocation.fromNamespaceAndPath("nusmp", id))) bad.add("the entity type nusmp:" + id + " is not registered");
            Map<String, PlayerLook> looks = PlayerLookTables.load(PlayerModelAudit::classpath).looks();
            com.newuniverse.nusmp.arc.ArcCharacters.bootstrap();                      // the arcs' friendly characters are people the kit drew in phase 1: each needs its look, or it would wear the default skin
            for (com.newuniverse.nusmp.arc.ArcCharacter c : com.newuniverse.nusmp.arc.ArcRoster.npcs())
                if (!looks.containsKey(c.id())) bad.add("the ally " + c.id() + " has no look: it would be drawn with the default skin");
            int named = 0, humanoid = 0;
            for (DndProfile p : DndProfiles.all()) {
                if (p.look().isEmpty()) continue;
                named++;
                boolean human = DungeonLooks.isHumanoidAbyssal(p.look());
                for (int phase = 1; phase <= 3; phase++) {
                    String id = DungeonLooks.guardian(p.look(), phase);
                    if (human && !looks.containsKey(id)) bad.add("the profile " + p.id() + " names the look " + p.look() + " whose phase " + phase + " ('" + id + "') is not in the table");
                    if (!human && !id.isEmpty() && !id.equals(DungeonLooks.FLOOR_WARDEN)) bad.add("the profile " + p.id() + ": the creature look " + p.look() + " must keep its own model, not '" + id + "'");
                }
                if (human) humanoid++;
            }
            if (!bad.isEmpty()) return SelfTests.Result.fail(bad.size() + " problems, first: " + bad.get(0));
            return SelfTests.Result.ok("dungeon looks, foundry people and creatures, " + named + " boss profiles with a look (" + humanoid + " humanoid), the flag rule all resolve");
        });
    }
}
