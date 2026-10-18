package com.newuniverse.nusmp.heroweapon.weapon.shield;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.heroweapon.HeroComponents;
import com.newuniverse.nusmp.heroweapon.HeroItems;
import com.newuniverse.nusmp.heroweapon.HeroKind;
import com.newuniverse.nusmp.heroweapon.HeroTrigger;
import com.newuniverse.nusmp.heroweapon.bond.HeroBonds;
import com.newuniverse.nusmp.heroweapon.forms.HeroForm;
import com.newuniverse.nusmp.heroweapon.forms.HeroForms;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogic;
import com.newuniverse.nusmp.heroweapon.skill.HeroLogics;
import com.newuniverse.nusmp.heroweapon.skill.HeroSkill;
import com.newuniverse.nusmp.labyrinth.mega.SelfTests;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: nothing of the hero weapons can be played by its author, so the Legendary Shield has a SELF-TEST that the CI server smoke test runs in the real game
 * ({@code /multiverse mega selftest}, key {@code hero.weapon.shield}). It checks, in this order: that every technique of the table is registered with the table's
 * cooldown, cost, name and ultimate flag; that every form of the weapon (from the forms system once it is merged, and from the packaged file
 * {@code data/nusmp/hero/forms/legendary_shield.json} either way) lists only techniques and passives that exist, on slots a blocking weapon has; and then, with a
 * stand-in player bonded to the Shield beside a mob that cannot move, that every technique can be cast once and every hook run once without throwing. It removes
 * what it spawned and releases the bond. If the stand-in cannot be bonded (the Shield is held by a real player) or the spawn chunk is not loaded, the third part
 * is skipped and says so.
 */
final class ShieldSelfTest {
    private ShieldSelfTest() {}

    private static final String PLAYER = "hero_shield_tester", FORMS_FILE = "/data/nusmp/hero/forms/legendary_shield.json";

    static SelfTests.Result run(MinecraftServer server) {
        List<String> bad = new ArrayList<>();
        checkTable(bad);
        int forms = checkForms(bad);
        if (!bad.isEmpty()) return fail(bad);
        String live = castAll(server, bad);
        if (!bad.isEmpty()) return fail(bad);
        return SelfTests.Result.ok(ShieldNumbers.all().size() + " techniques match their table, " + forms + " forms read, " + live);
    }

    private static SelfTests.Result fail(List<String> bad) {
        return SelfTests.Result.fail(bad.size() + " problem(s): " + String.join(" | ", bad.subList(0, Math.min(6, bad.size()))));
    }

    // ------------------------------------------------------------------ the table against the registered skills
    private static void checkTable(List<String> bad) {
        HeroLogic logic = HeroLogics.of(HeroKind.SHIELD);
        if (!(logic instanceof ShieldLogic)) bad.add("the registered logic of the Shield is " + logic.getClass().getSimpleName());
        for (ShieldNumbers.Spec s : ShieldNumbers.all()) {
            HeroSkill k = HeroLogics.skill(s.id());
            if (k == null) {
                bad.add(s.id() + " is not registered");
                continue;
            }
            if (k.cooldownTicks(null) != s.cooldown()) bad.add(s.id() + " cooldown " + k.cooldownTicks(null) + " is not the table's " + s.cooldown());
            if (Math.abs(k.magoiShare() - s.cost()) > 1.0E-9) bad.add(s.id() + " cost " + k.magoiShare() + " is not the table's " + s.cost());
            if (k.ultimate() != (s.kind() == ShieldNumbers.Kind.ULTIMATE)) bad.add(s.id() + " ultimate flag differs from the table");
            if (!s.name().equals(k.name())) bad.add(s.id() + " is named " + k.name() + ", the table says " + s.name());
        }
        if (HeroLogics.skillsOf(HeroKind.SHIELD).size() != ShieldNumbers.all().size())
            bad.add("the logic lists " + HeroLogics.skillsOf(HeroKind.SHIELD).size() + " skills, the table has " + ShieldNumbers.all().size());
    }

    // ------------------------------------------------------------------ the forms
    private static int checkForms(List<String> bad) {
        int count = 0;
        for (HeroForm f : HeroForms.of(HeroKind.SHIELD)) {
            count++;
            checkForm(bad, f.id(), f.skills(), f.passives());
        }
        try (InputStream in = ShieldSelfTest.class.getResourceAsStream(FORMS_FILE)) {
            if (in == null) {
                bad.add("the forms file " + FORMS_FILE + " is not in the mod");
                return count;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonArray arr = root.getAsJsonArray("forms");
            if (arr == null || arr.isEmpty() || !"base".equals(arr.get(0).getAsJsonObject().get("id").getAsString())) {
                bad.add("the forms file does not start with the base form");
                return count;
            }
            Set<String> ids = new HashSet<>();
            for (JsonElement e : arr) if (!ids.add(e.getAsJsonObject().get("id").getAsString())) bad.add("form " + e.getAsJsonObject().get("id").getAsString() + " is listed twice");
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                String id = o.get("id").getAsString();
                Map<String, String> skills = new LinkedHashMap<>();
                if (o.has("skills")) for (Map.Entry<String, JsonElement> s : o.getAsJsonObject("skills").entrySet()) skills.put(s.getKey(), s.getValue().getAsString());
                List<String> passives = new ArrayList<>();
                if (o.has("passives")) for (JsonElement p : o.getAsJsonArray("passives")) passives.add(p.getAsString());
                checkForm(bad, id, skills, passives);
                if (o.has("parents")) for (JsonElement p : o.getAsJsonArray("parents")) if (!ids.contains(p.getAsString())) bad.add("form " + id + " has the unknown parent " + p.getAsString());
            }
        } catch (Exception ex) {
            bad.add("the forms file cannot be read: " + ex);
        }
        return count;
    }

    private static void checkForm(List<String> bad, String id, Map<String, String> skills, List<String> passives) {
        int ultimates = 0;
        for (Map.Entry<String, String> s : skills.entrySet()) {
            HeroTrigger t = HeroTrigger.bySlot(s.getKey());
            if (t == null) bad.add("form " + id + " uses the unknown slot " + s.getKey());
            if (t == HeroTrigger.USE) bad.add("form " + id + " uses the use slot, which a blocking shield cannot (the use key raises the guard)");
            HeroSkill k = HeroLogics.skill(s.getValue());
            if (k == null || !s.getValue().startsWith("shield.")) bad.add("form " + id + " lists " + s.getValue() + ", which is not a skill of the Shield");
            else if (k.ultimate()) ultimates++;
        }
        if (ultimates > 1) bad.add("form " + id + " lists " + ultimates + " ultimates");
        for (String p : passives) if (!ShieldNumbers.PASSIVES.contains(p)) bad.add("form " + id + " has the passive " + p + ", which the Shield does not implement");
    }

    // ------------------------------------------------------------------ the techniques and hooks, with a stand-in player
    private static String castAll(MinecraftServer server, List<String> bad) {
        ServerLevel level = server.overworld();
        BlockPos at = level.getSharedSpawnPos();
        if (!level.hasChunkAt(at)) return "casting skipped: the spawn chunk is not loaded";
        Vec3 pos = Vec3.atBottomCenterOf(at.above(2));
        FakePlayer fp = SelfTests.player(level, PLAYER, pos.x, pos.y, pos.z);
        boolean bound = false;
        Zombie dummy = null;
        try {
            if (HeroBonds.has(fp)) HeroBonds.release(fp);
            String why = HeroBonds.bind(fp, HeroKind.SHIELD);
            if (why != null) return "casting skipped: the stand-in could not take the Shield (" + why + ")";
            bound = true;
            HeroBonds.of(fp).setSummoned(true);
            ItemStack stack = HeroItems.stack(HeroKind.SHIELD);
            stack.set(HeroComponents.OWNER.get(), fp.getUUID().toString());
            fp.setItemInHand(InteractionHand.MAIN_HAND, stack);
            fp.moveTo(pos.x, pos.y, pos.z, 0f, 0f);
            dummy = EntityType.ZOMBIE.create(level);
            if (dummy == null) return "casting skipped: no dummy mob could be made";
            dummy.moveTo(pos.x, pos.y, pos.z + 3.0, 180f, 0f);
            dummy.setNoAi(true);
            level.addFreshEntity(dummy);
            HeroCtx c = HeroCtx.of(fp);
            if (c == null) {
                bad.add("no context for the bonded stand-in");
                return "";
            }
            int cast = 0, refused = 0;
            for (HeroSkill k : HeroLogics.skillsOf(HeroKind.SHIELD)) {
                try {
                    if (k.cast(c)) cast++;
                    else refused++;
                } catch (Throwable ex) {
                    bad.add(k.id() + " threw " + ex);
                }
                fp.setHealth(fp.getMaxHealth());
            }
            HeroLogic logic = HeroLogics.of(HeroKind.SHIELD);
            var blow = fp.damageSources().mobAttack(dummy);
            try {
                logic.onTick(c);
                logic.onDamaged(c, blow, 4f);
                logic.onBlocked(c, blow, 4f);
                logic.onBlocked(c, blow, 400f);
                logic.onKill(c, dummy);
                logic.onHit(c, dummy, 3f, true);
                logic.onFormChanged(c, c.form, c.form);
                logic.onDismiss(c);
            } catch (Throwable ex) {
                bad.add("a hook threw " + ex);
            }
            return cast + " techniques cast and " + refused + " refused without throwing, the hooks ran";
        } finally {
            if (dummy != null) dummy.discard();
            fp.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            fp.setHealth(fp.getMaxHealth());
            ShieldRuntime.forget(fp.getUUID());
            if (bound) HeroBonds.release(fp);
        }
    }
}
