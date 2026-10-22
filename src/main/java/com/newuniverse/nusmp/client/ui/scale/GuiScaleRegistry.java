package com.newuniverse.nusmp.client.ui.scale;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: one list of every GUI of the mod that has a size setting, with the id used in the config file, the class (screens) or layer (HUD) it belongs to, and
 * the size the layout was designed for. Pure data; new GUIs are appended here and nowhere else.
 */
public final class GuiScaleRegistry {
    public enum Kind { SCREEN, HUD }

    /** How a HUD element is scaled: by a render-layer hook (LAYER), or by the element asking {@code GuiScale.hud(id)} itself (SITE). */
    public enum Mode { NONE, LAYER, SITE }

    /**
     * @param designW  screens: the panel width the layout was built for; 0 = the whole window, -1 = the container's own image size
     * @param layer    HUD in LAYER mode: the path of the layer id in the nusmp namespace
     */
    public record Entry(String id, Kind kind, Mode mode, String className, String layer, int designW, int designH, GuiScaleMath.Anchor anchor, String english) {}

    private static final String P = "com.newuniverse.nusmp.";
    private static final List<Entry> ALL = new ArrayList<>();

    private static void screen(String id, String cls, int w, int h, String english) {
        ALL.add(new Entry(id, Kind.SCREEN, Mode.NONE, P + cls, "", w, h, GuiScaleMath.Anchor.CENTER, english));
    }

    private static void layer(String id, String layer, GuiScaleMath.Anchor a, String english) {
        ALL.add(new Entry(id, Kind.HUD, Mode.LAYER, "", layer, 0, 0, a, english));
    }

    private static void site(String id, GuiScaleMath.Anchor a, String english) {
        ALL.add(new Entry(id, Kind.HUD, Mode.SITE, "", "", 0, 0, a, english));
    }

    static {
        // screens
        screen("multiverse_status", "client.multiverse.MultiverseStatusScreen", 0, 0, "Multiverse status");
        screen("four_kingdoms", "client.multiverse.FourKingdomsScreen", 340, 236, "Four Kingdoms menu");
        screen("codex", "client.ui.CodexScreen", 480, 300, "Codex");
        screen("compat_codex", "client.compat.CompatCodexScreen", 480, 300, "Compat codex");
        screen("skill_shop", "client.skilltree.shop.ShopScreen", 0, 0, "Skill shop");
        screen("weapon_book", "client.heroweapon.forms.WeaponBookScreen", 0, 0, "Weapon forms book");
        screen("avatar_action", "client.avatar.AvatarActionScreen", 230, 190, "Avatar actions");
        screen("avatar_builder", "client.avatar.AvatarBuilderScreen", -1, -1, "Avatar creator");
        screen("soul_forge", "client.avatar.SoulForgeScreen", -1, -1, "Soul forge");
        screen("avatar_inventory", "avatar.inventory.client.AvatarInventoryScreen", -1, -1, "Avatar inventory");
        screen("questionnaire", "client.wizard.QuestionnaireScreen", 360, 200, "Tower Wizard questions");
        screen("dungeon_map", "client.elite.hub.DungeonMapScreen", 0, 0, "Dungeon map");
        screen("hub_quest", "client.elite.hub.QuestScreen", -1, -1, "Hub quests");
        screen("hub_shop", "client.elite.hub.ShopScreen", -1, -1, "Hub shop");
        screen("relic_forge", "client.relics.forge.ForgeScreen", -1, -1, "Relic forge");
        screen("grimoire_slot", "client.grimoire.GrimoireSlotScreen", -1, -1, "Grimoire slot");
        screen("system_stats", "client.mega.system.SystemStatsScreen", 0, 0, "System stat points");
        screen("requip_armoury", "client.requip.RequipArmouryScreen", -1, -1, "Requip Armoury");
        screen("engraving_codex", "client.engraving.EngravingCodexScreen", 540, 300, "Engravement codex");
        screen("shishou_stomach", "client.shishou.ShishouStomachScreen", -1, -1, "Shishou Stomach");
        // HUD overlays scaled by a render-layer hook
        layer("quest_tracker", "hub_quest_tracker", GuiScaleMath.Anchor.TOP_RIGHT, "Quest tracker");
        layer("arts_bar", "line_arts_bar", GuiScaleMath.Anchor.BOTTOM_CENTER, "Arts bar");
        layer("relic_keys", "relic_hud", GuiScaleMath.Anchor.BOTTOM_RIGHT, "Relic key hints");
        layer("song_meter", "song_meter", GuiScaleMath.Anchor.BOTTOM_CENTER, "Song meter");
        layer("floor_card", "system_floor_card", GuiScaleMath.Anchor.TOP_CENTER, "System floor card");
        layer("page_toast", "page_unlock", GuiScaleMath.Anchor.TOP_CENTER, "Page unlocked toast");
        // HUD overlays that apply the scale themselves (they already have their own position, anchor or size)
        site("system_hud", GuiScaleMath.Anchor.TOP_LEFT, "System window");
        site("hero_hud", GuiScaleMath.Anchor.BOTTOM_LEFT, "Hero weapon HUD");
        site("djinn_panel", GuiScaleMath.Anchor.BOTTOM_CENTER, "Djinn panel");
        site("boss_bar", GuiScaleMath.Anchor.TOP_CENTER, "Boss bar");
    }

    private GuiScaleRegistry() {}

    public static List<Entry> all() { return Collections.unmodifiableList(ALL); }

    public static List<Entry> of(Kind kind) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : ALL) if (e.kind() == kind) out.add(e);
        return out;
    }

    public static Entry byId(String id) {
        for (Entry e : ALL) if (e.id().equals(id)) return e;
        return null;
    }

    public static Entry byClass(String className) {
        for (Entry e : ALL) if (e.kind() == Kind.SCREEN && e.className().equals(className)) return e;
        return null;
    }

    public static Entry byLayer(String layerPath) {
        for (Entry e : ALL) if (e.mode() == Mode.LAYER && e.layer().equals(layerPath)) return e;
        return null;
    }

    /** class name to entry, for the screen hook. */
    public static Map<String, Entry> screenIndex() {
        Map<String, Entry> m = new LinkedHashMap<>();
        for (Entry e : ALL) if (e.kind() == Kind.SCREEN) m.put(e.className(), e);
        return m;
    }

    /** Screens of the mod that are deliberately not scaled (they are the tools that place or size things, drawn in raw screen pixels). */
    public static boolean exempt(String className) {
        return className.equals(P + "client.mega.system.SystemEditScreen") || className.equals(P + "client.ui.GuiSizeScreen");
    }

    /** The overrides as the config file holds them: id to value (0 = follow global), parsed from raw config values; unknown ids are dropped. */
    public static Map<String, Double> parseOverrides(Map<String, ?> raw) {
        Map<String, Double> out = new LinkedHashMap<>();
        for (Entry e : ALL) {
            double v = raw == null ? 0.0 : GuiScaleMath.parse(raw.get(e.id()));
            out.put(e.id(), Double.isNaN(v) ? 0.0 : v <= 0.0 ? 0.0 : GuiScaleMath.clamp(v));
        }
        return out;
    }
}
