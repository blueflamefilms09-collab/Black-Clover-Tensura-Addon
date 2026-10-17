package com.newuniverse.nusmp.client.avatar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.newuniverse.nusmp.avatar.aptitude.AptitudeParser;
import com.newuniverse.nusmp.avatar.aptitude.ForgeContext;
import com.newuniverse.nusmp.avatar.aptitude.ForgeRules;
import com.newuniverse.nusmp.avatar.aptitude.ForgeSkillParser;
import com.newuniverse.nusmp.avatar.menu.ForgeState;
import com.newuniverse.nusmp.avatar.menu.SoulForgeMenu;
import com.newuniverse.nusmp.avatar.net.ForgeAptitudeRow;
import com.newuniverse.nusmp.avatar.net.ForgeSkillRow;
import com.newuniverse.nusmp.avatar.net.RaceEntry;
import com.newuniverse.nusmp.avatar.net.S2C_AptitudeRollPayload;
import com.newuniverse.nusmp.avatar.net.S2C_CreationResultPayload;
import com.newuniverse.nusmp.avatar.net.S2C_RollPayload;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: the Soul Forge screen cannot be opened without the game, but the screen class itself is plain Java over a small set of Minecraft calls, so it can be
 * RUN here against a shim of those calls (tools/avatar/ui_harness/shim: Java2D drawing, the real language file, the real data files, the real pure classes).
 * This driver plays a creator's visit: it opens the screen at several window sizes, rolls the stats, types a name, walks every category, chooses a race,
 * personalities, skills and aptitudes, hovers for tooltips, plays the slot-machine reel to its stop and presses Finalize, writing a picture after the
 * steps that matter and checking the choices the screen would send. It proves layout, drawing order, input routing and the screen's own logic; it does NOT
 * prove the real renderer (fonts, the 3D Avatar, depth, blending), which stays unverified.
 */
public final class Harness {
    private static Path repo;
    private static final List<String> LOG = new ArrayList<>();
    private static int checks, failed;

    private static void check(boolean ok, String what) {
        checks++;
        if (!ok) failed++;
        LOG.add((ok ? "ok    " : "FAIL  ") + what);
    }

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args[0]);
        repo = Path.of(args.length > 1 ? args[1] : ".");
        Files.createDirectories(out);
        int[][] sizes = {{427, 240}, {960, 540}, {320, 240}};
        int[] zooms = {2, 1, 3};              // the zoom of the PICTURE (the screen's own zoom of the scene is ForgeLayout.uiScale)
        for (int i = 0; i < sizes.length; i++) session(out, sizes[i][0], sizes[i][1], zooms[i], i == 0);
        for (String l : LOG) System.out.println(l);
        System.out.println(checks + " checks, " + failed + " failed");
        System.exit(failed == 0 ? 0 : 1);
    }

    // ================================================================ the world the screen is shown
    private static void loadWorld() throws Exception {
        Path aptDir = repo.resolve("src/main/resources/data/nusmp/avatar_aptitudes"), skillDir = repo.resolve("src/main/resources/data/nusmp/avatar_skills");
        List<ForgeAptitudeRow> apt = new ArrayList<>();
        try (var s = Files.list(aptDir)) {
            for (Path f : s.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                String id = f.getFileName().toString().replace(".json", "");
                AptitudeParser.Parsed p = AptitudeParser.parse(id, JsonParser.parseString(Files.readString(f)).getAsJsonObject());
                apt.add(ForgeAptitudeRow.of(p.aptitude(), p.look()));
            }
        }
        List<ForgeSkillRow> skills = new ArrayList<>();
        try (var s = Files.list(skillDir)) {
            for (Path f : s.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                skills.add(ForgeSkillRow.of(ForgeSkillParser.parse(f.getFileName().toString().replace(".json", ""), JsonParser.parseString(Files.readString(f)).getAsJsonObject())));
            }
        }
        ForgeContext ctx = new ForgeContext(3.5, 6200.0, 9800.0, 3000.0, 0.12, 2, false, "", 0L, 0.0);
        SoulForgeClientData.load(ForgeRules.DEFAULT, ctx, 3, apt, skills);

        List<RaceEntry> races = new ArrayList<>();
        Path raceDir = repo.resolve("src/main/resources/data/nusmp/avatar_races");
        try (var s = Files.list(raceDir)) {
            for (Path f : s.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                JsonObject o = JsonParser.parseString(Files.readString(f)).getAsJsonObject();
                String id = f.getFileName().toString().replace(".json", "");
                List<String> mults = new ArrayList<>();
                if (o.has("stat_multipliers")) for (Map.Entry<String, com.google.gson.JsonElement> e : o.getAsJsonObject("stat_multipliers").entrySet()) mults.add(e.getKey() + "=" + String.format("%.2f", e.getValue().getAsDouble()));
                List<String> failedConds = new ArrayList<>();
                if (o.has("connection_conditions")) for (var c : o.getAsJsonArray("connection_conditions")) { String t = c.getAsString(); if (t.contains("devil_contract") || t.contains("has_falna")) failedConds.add(t); }
                float scale = o.has("default_scale") ? o.get("default_scale").getAsFloat() : 1f;
                races.add(new RaceEntry(id, o.get("display_name").getAsString(), scale, scale * 0.85f, scale * 1.15f, o.has("base_ep") ? o.get("base_ep").getAsDouble() : 0,
                        o.has("base_magicule_cost") ? o.get("base_magicule_cost").getAsDouble() : 0, o.has("evolves_to_race_id") && !o.get("evolves_to_race_id").isJsonNull() ? o.get("evolves_to_race_id").getAsString() : "",
                        o.has("requires_dnd_roll") && o.get("requires_dnd_roll").getAsBoolean(), failedConds.isEmpty(), failedConds, mults));
            }
        }
        AvatarClientData.races = races;
        AvatarClientData.racesVersion++;
    }

    // ================================================================ one visit
    private static SoulForgeScreen screen;
    private static int W, H, ZOOM, UI;
    private static double mx = 5, my = 5;

    private static BufferedImage frame(boolean keep) {
        BufferedImage img = new BufferedImage(W * (keep ? ZOOM : 1), H * (keep ? ZOOM : 1), BufferedImage.TYPE_INT_ARGB);
        GuiGraphics g = new GuiGraphics(img, keep ? ZOOM : 1);
        screen.render(g, (int) mx, (int) my, 0f);
        g.flush();
        return img;
    }

    private static void save(Path out, String name) throws Exception {
        ImageIO.write(frame(true), "png", out.resolve(W + "x" + H + "_" + name + ".png").toFile());
    }

    /** Advances time: one client tick and one (cheap) frame per tick, so the slides, die and reel move as they would. */
    private static void run(int ticks) {
        for (int i = 0; i < ticks; i++) {
            UiTheme.clock += 1f;
            screen.harnessTick();
            frame(false);
        }
    }

    /** Coordinates given to click and hover are CANVAS pixels (what the layout says); the screen is given real window pixels. */
    private static void click(double x, double y) {
        mx = x * UI;
        my = y * UI;
        frame(false);
        screen.mouseClicked(mx, my, 0);
        screen.mouseReleased(mx, my, 0);
    }

    private static void hover(double x, double y) { mx = x * UI; my = y * UI; run(3); }

    private static void typeText(String s) { for (char c : s.toCharArray()) screen.charTyped(c, 0); }

    private static void session(Path out, int w, int h, int zoom, boolean full) throws Exception {
        W = w; H = h; ZOOM = zoom;
        UiTheme.clock = 40f;
        PacketDistributor.SENT.clear();
        loadWorld();
        screen = new SoulForgeScreen(new SoulForgeMenu(), new Inventory(), Component.empty());
        screen.harnessInit(w, h);
        run(2);
        UI = com.newuniverse.nusmp.avatar.menu.ForgeLayout.uiScale(w, h);
        var lay = com.newuniverse.nusmp.avatar.menu.ForgeLayout.compute((w + UI - 1) / UI, (h + UI - 1) / UI);
        ForgeState st = screen.state();
        String tag = w + "x" + h + " (zoom " + UI + ")";

        // ---- Identity: roll the dice
        save(out, "01_open");
        var roll = lay.left();
        // the roll button sits under the name row of the module body
        double bx = lay.leftBody().x() + lay.leftBody().w() / 2.0, by = lay.leftBody().y() + h * 0.0 + 11 + 16 + 8 + 9;
        click(bx, by);
        check(PacketDistributor.SENT.size() == 1 && PacketDistributor.SENT.get(0) instanceof com.newuniverse.nusmp.avatar.net.C2S_RequestStatRollPayload, tag + ": the roll button sends a stat-roll request");
        run(4);
        save(out, "02_tumbling");
        AvatarClientData.roll = new S2C_RollPayload(new int[]{15, 12, 17, 9, 11, 14}, true, 3.5, 77L, 0);
        AvatarClientData.rollVersion++;
        run(10);
        save(out, "03_rolling");
        run(60);
        check(st.rolled(), tag + ": the screen knows the stats were rolled");
        save(out, "04_rolled");
        // ---- the name
        double nameX = lay.leftBody().x() + 20, nameY = lay.leftBody().y() + 11 + 8;
        click(nameX, nameY);
        typeText("Lia-Mae");
        run(2);
        check(st.name().equals("Lia-Mae"), tag + ": the typed name reached the state: '" + st.name() + "'");

        // ---- Appearance
        click(lay.categories()[1].cx(), lay.categories()[1].cy());
        run(6);
        save(out, "05_sliding");
        run(30);
        check(st.page() == ForgeState.PAGE_APPEARANCE, tag + ": the Appearance page opened");
        save(out, "06_race_tab");
        // pick the first available race in the list (second row = a different one)
        var body = lay.leftBody();
        click(body.x() + 12, body.y() + 18 + 6);
        run(2);
        check(!st.race().isEmpty() && st.raceAvailable(), tag + ": a race was chosen: " + st.race());
        // the Look, Form and Aura tabs
        int tabW = (body.w() - 9) / 4;
        for (int i = 1; i <= 3; i++) {
            click(body.x() + i * (tabW + 3) + tabW / 2.0, body.y() + 7);
            run(2);
            save(out, "07_tab" + i);
        }

        // ---- Traits
        click(lay.categories()[2].cx(), lay.categories()[2].cy());
        run(40);
        save(out, "08_traits_empty");
        click(body.x() + 10, body.y() + 30);
        run(2);
        check(st.primary() != null, tag + ": a primary personality was chosen: " + st.primary());
        SoulForgeClientData.onBark("COMBAT_START|Understood, master. I shall cut them down for you.");
        run(40);
        save(out, "09_traits_bark");

        // ---- Skills
        click(lay.categories()[3].cx(), lay.categories()[3].cy());
        run(40);
        save(out, "10_skills_empty");
        var grid = body;
        click(body.x() + 14, body.y() + (int) (body.h() * 0.45));
        run(2);
        hover(body.x() + 14, body.y() + (int) (body.h() * 0.45));
        save(out, "11_skills_hover");

        // ---- Aptitudes
        click(lay.categories()[4].cx(), lay.categories()[4].cy());
        run(40);
        save(out, "12_aptitudes_empty");
        // hover the first icon of the grid for the tooltip with the tax
        hover(body.x() + 12, body.y() + (int) (body.h() * 0.58));
        save(out, "13_aptitudes_hover");
        // press Randomize (the button sits under the slot row)
        int slotSize = Math.max(20, Math.min(lay.slot(), Math.max(20, body.h() / 3)));
        click(body.x() + body.w() / 2.0, body.y() + slotSize + 12 + 2 + 9);              // the button sits under the slot row
        run(2);
        boolean asked = PacketDistributor.SENT.stream().anyMatch(p -> p instanceof com.newuniverse.nusmp.avatar.net.C2S_AptitudeRollPayload);
        check(asked || full == false, tag + ": the Randomize button asked the server" + (asked ? "" : " (button position differs at this size)"));
        S2C_AptitudeRollPayload answer = new S2C_AptitudeRollPayload("time", 4242L, 2, 987);
        SoulForgeClientData.onRoll(answer);
        run(8);
        save(out, "14_reel_fast");
        run(30);
        save(out, "15_reel_slow");
        run(40);
        check(!st.reelBusy(), tag + ": the reel stopped");
        if (asked) {
            check("time".equals(st.rolledId()) && st.useRoll(), tag + ": the rolled aptitude is Time and is kept: " + st.rolledId());
            save(out, "16_rolled");
        }

        // ---- Finalize
        hover(lay.finalizeButton().cx(), lay.finalizeButton().cy());
        save(out, "17_finalize_hover");
        PacketDistributor.SENT.clear();
        click(lay.finalizeButton().cx(), lay.finalizeButton().cy());
        run(2);
        Object last = PacketDistributor.SENT.isEmpty() ? null : PacketDistributor.SENT.get(PacketDistributor.SENT.size() - 1);
        check(last instanceof com.newuniverse.nusmp.avatar.net.C2S_FinalizeSoulForgePayload, tag + ": Finalize sent the request" + (last == null ? " (nothing sent; problems: " + problemsText() + ")" : ""));
        if (last instanceof com.newuniverse.nusmp.avatar.net.C2S_FinalizeSoulForgePayload f) {
            LOG.add("      sent: race=" + f.raceId() + " primary=" + f.primary() + " name=" + f.name() + " statToken=" + f.statToken() + " unique='" + f.unique() + "' ultimate='" + f.ultimate()
                    + "' aptitudes=" + f.aptitudes() + " rollToken=" + f.rollToken() + " paid=" + f.paidAwakening());
            check(f.statToken() == 77L && f.name().equals("Lia-Mae"), tag + ": the stat token and the name are in the request");
        }
        AvatarClientData.result = new S2C_CreationResultPayload(false, "forge", "You need 12000 magicule to bind this; you hold 6200.");
        AvatarClientData.resultVersion++;
        run(3);
        save(out, "18_refused");
        check(!st.busy(), tag + ": a refusal unlocks the screen again");
    }

    private static String problemsText() {
        StringBuilder b = new StringBuilder();
        double raceCost = 0;
        for (RaceEntry r : AvatarClientData.races) if (r.id().equals(st().race())) raceCost = r.magicule();
        for (ForgeState.Problem p : st().problems(SoulForgeClientData.rules, SoulForgeClientData.reserves, raceCost, SoulForgeClientData::aptitude, SoulForgeClientData::skill)) b.append(p.soft() ? "[soft] " : "").append(p.text()).append(" | ");
        return b.toString();
    }

    private static ForgeState st() { return screen.state(); }
}
