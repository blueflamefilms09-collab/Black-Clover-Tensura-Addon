package com.newuniverse.nusmp.client.ui.move;

import com.newuniverse.nusmp.client.ui.move.GuiMoveLayout.State;
import com.newuniverse.nusmp.client.ui.move.GuiMoveMath.Place;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: one list of every custom on-screen GUI of the mod that a player can move, like {@link GuiScaleRegistry} is the list of those that can be sized. An entry has the id
 * used in the layout file, the name shown in edit mode, whether it is a HUD overlay or a panel of a screen, where it sits by default (a place, a margin and a size: this is
 * what edit mode shows for an element that is not on screen right now), how its place is applied, and the source files that draw it. A second list, {@link #FIXED}, holds
 * every custom GUI class that is deliberately NOT movable with the reason, so a test can prove that no custom GUI of the mod was forgotten. New GUIs are appended here and
 * nowhere else. The entries also hold the little per-frame notes the glue writes (the rectangle drawn last, the frame it was seen), plain fields so a frame costs no
 * allocation and no lookup beyond finding the entry.
 */
public final class GuiMoveRegistry {
    private GuiMoveRegistry() {}

    /** A HUD overlay drawn over the world, or a panel of a screen (the screen's design rectangle). */
    public enum Kind { HUD, PANEL }

    /**
     * How the place of an element is applied. BEGIN: the element calls {@code GuiMove.begin/end} around its drawing. LAYER: the GUI Size layer hook wraps the element's whole
     * layer and the element only reports its rectangle. SELF: the element places itself from the store (the System window, whose layout computes its own corner). SCREEN:
     * the screen hooks of {@code GuiScale} move and scale the whole open screen.
     */
    public enum How { BEGIN, LAYER, SELF, SCREEN }

    /** The tick that says "never seen". */
    public static final long NEVER = Long.MIN_VALUE;

    public static final class Element {
        public final String id, english, className;
        public final Kind kind;
        public final How how;
        /** Where the element sits when the player has not moved it, as a place, a margin from the edges (own pixels) and a size (own pixels). */
        public final Place defPlace;
        public final int margin, defW, defH;
        /** The source files (relative to {@code com/newuniverse/nusmp}) that register or draw it; the coverage test reads them. */
        public final List<String> sources;
        /** The GUI Size entry of the same id, or null: its size multiplies with the player's own size in edit mode. */
        public final GuiScaleRegistry.Entry scaleEntry;

        // ---- runtime, written by the glue on the client thread only
        /** What the player did, or null for "never touched". */
        public volatile State state;
        /** The unscaled size and default corner the element reported on its last frame (GUI pixels / own pixels). */
        public int dw, dh;
        public double defX, defY;
        /** The size from the GUI Size sliders and the total scale used on the last frame, and the corner it was drawn at. */
        public float gs = 1f, t = 1f;
        public double drawnX, drawnY;
        /** The client tick of the last frame it was drawn. */
        public long seenTick = NEVER;

        Element(String id, String english, Kind kind, How how, Place defPlace, int margin, int defW, int defH, String className, List<String> sources) {
            this.id = id;
            this.english = english;
            this.kind = kind;
            this.how = how;
            this.defPlace = defPlace;
            this.margin = margin;
            this.defW = defW;
            this.defH = defH;
            this.className = className;
            this.sources = sources;
            this.scaleEntry = GuiScaleRegistry.byId(id);
            this.dw = defW;
            this.dh = defH;
        }

        /** The language key of the name shown in edit mode. */
        public String nameKey() { return "gui_move.element." + id; }

        /** Has the player placed this element (so its place and size apply)? */
        public boolean placed() { State s = state; return s != null && s.placed; }

        public boolean hidden() { State s = state; return s != null && s.hidden; }
    }

    /** A custom GUI class that is not movable, and why. */
    public record Fixed(String source, String reason) {}

    private static final String P = "com.newuniverse.nusmp.";
    private static final List<Element> ALL = new ArrayList<>();
    private static final Map<String, Element> BY_ID = new HashMap<>();
    private static final Map<String, Element> BY_CLASS = new HashMap<>();
    private static final List<Fixed> FIXED = new ArrayList<>();

    private static List<String> src(String... s) { return List.of(s); }

    private static void hud(String id, String english, How how, Place def, int margin, int w, int h, String... sources) {
        add(new Element(id, english, Kind.HUD, how, def, margin, w, h, "", src(sources)));
    }

    /** A panel of a screen: the screen class comes from the GUI Size entry of the same id; w x h is the design rectangle (a container screen uses its own image size). */
    private static void panel(String id, String english, int w, int h, String... sources) {
        GuiScaleRegistry.Entry e = GuiScaleRegistry.byId(id);
        String cls = e == null ? "" : e.className();
        add(new Element(id, english, Kind.PANEL, How.SCREEN, new Place(0, true, true, 0, 0), 0, w, h, cls, src(sources)));
    }

    private static void add(Element e) {
        ALL.add(e);
        BY_ID.put(e.id, e);
        if (!e.className.isEmpty()) BY_CLASS.put(e.className, e);
    }

    private static void fixed(String source, String reason) { FIXED.add(new Fixed(source, reason)); }

    private static final Place TL = new Place(0, false, false, 0, 0), TR = new Place(1, false, false, 0, 0), BL = new Place(2, false, false, 0, 0),
            BR = new Place(3, false, false, 0, 0), TC = new Place(0, true, false, 0, 0), BC = new Place(2, true, false, 0, 0);

    static {
        // ---------------- HUD overlays (in the order they are drawn: edit mode picks the last one under the mouse first)
        hud("system_hud", "System window", How.SELF, TL, 6, 164, 116, "client/mega/system/SystemClient.java", "client/mega/system/SystemOverlay.java", "client/mega/system/SystemClientConfig.java");
        hud("hero_hud", "Hero weapon HUD", How.BEGIN, BL, 6, 152, 70, "client/heroweapon/HeroClient.java", "client/heroweapon/HeroHud.java");
        hud("curse_bar", "Corruption bar", How.BEGIN, new Place(2, false, false, 0, 114), 6, 92, 26, "client/heroweapon/curse/CurseClient.java", "client/heroweapon/curse/CurseOverlay.java");
        hud("djinn_panel", "Djinn panel", How.BEGIN, BC, 62, 206, 68, "client/relics/djinn/DjinnClient.java", "client/relics/djinn/DjinnScreenFx.java", "client/relics/djinn/gui/DjinnGui.java");
        hud("boss_bar", "Boss bar", How.BEGIN, new Place(0, true, false, 0, 0), 12, 182, 40, "client/gui/bossbar/ModBossBarOverlay.java", "client/gui/bossbar/CustomBarOverlay.java");
        hud("quest_tracker", "Quest tracker", How.LAYER, new Place(1, false, false, -4, 36), 6, 158, 70, "client/elite/hub/HubClient.java", "client/elite/hub/QuestOverlay.java");
        hud("relic_keys", "Relic key hints", How.LAYER, BR, 3, 260, 14, "client/relics/RelicClient.java", "client/relics/RelicActKeys.java");
        hud("song_meter", "Song meter", How.LAYER, BC, 46, 64, 13, "client/song/SongClient.java");
        hud("scale_readout", "Scale weights", How.BEGIN, BC, 56, 110, 24, "client/scalecast/ScaleHud.java");
        hud("floor_card", "System floor report", How.LAYER, new Place(0, true, false, 0, 6), 6, 276, 120, "client/mega/system/FloorCardClient.java");
        hud("page_toast", "Page unlocked toast", How.LAYER, TC, 8, 300, 62, "client/ui/theme/PageUnlockOverlay.java");
        hud("shishou_hud", "Shishou HUD", How.BEGIN, new Place(3, false, false, 6, 40), 6, 156, 80, "client/shishou/ShishouClient.java", "client/shishou/ShishouHud.java");
        // ---------------- panels of screens (centred by default)
        panel("four_kingdoms", "Four Kingdoms menu", 340, 236, "client/multiverse/FourKingdomsScreen.java");
        panel("codex", "Codex", 480, 300, "client/ui/CodexScreen.java");
        panel("compat_codex", "Compat codex", 480, 300, "client/compat/CompatCodexScreen.java");
        panel("engraving_codex", "Engravement codex", 540, 300, "client/engraving/EngravingCodexScreen.java");
        panel("avatar_action", "Avatar actions", 230, 190, "client/avatar/AvatarActionScreen.java");
        panel("questionnaire", "Tower Wizard questions", 360, 200, "client/wizard/QuestionnaireScreen.java");
        panel("avatar_builder", "Avatar creator", 256, 220, "client/avatar/AvatarBuilderScreen.java");
        panel("soul_forge", "Soul forge", 256, 220, "client/avatar/SoulForgeScreen.java");
        panel("avatar_inventory", "Avatar inventory", 176, 166, "avatar/inventory/client/AvatarInventoryScreen.java");
        panel("hub_quest", "Hub quests", 176, 166, "client/elite/hub/QuestScreen.java");
        panel("hub_shop", "Hub shop", 176, 166, "client/elite/hub/ShopScreen.java");
        panel("relic_forge", "Relic forge", 176, 166, "client/relics/forge/ForgeScreen.java");
        panel("grimoire_slot", "Grimoire slot", 176, 166, "client/grimoire/GrimoireSlotScreen.java");
        panel("requip_armoury", "Requip Armoury", 176, 166, "client/requip/RequipArmouryScreen.java");
        panel("shishou_stomach", "Shishou Stomach", 176, 168, "client/shishou/ShishouStomachScreen.java");

        // ---------------- custom GUI classes that stay where they are, with the reason
        fixed("client/ui/move/GuiMove.java", "the framework itself: it moves the others");
        fixed("client/ui/move/GuiMoveScreen.java", "the edit tool, drawn in raw screen pixels");
        fixed("client/ui/GuiScale.java", "the GUI Size framework: its hooks wrap the other GUIs");
        fixed("client/ui/GuiSizeScreen.java", "the GUI Size settings screen, the tool that sizes things");
        fixed("client/mega/system/SystemEditScreen.java", "the System window's own edit tool (kept; global edit mode moves the same place)");
        fixed("client/mega/system/SystemStatsScreen.java", "a full-window stat screen: nothing to move");
        fixed("client/multiverse/MultiverseStatusScreen.java", "a full-window status book: nothing to move");
        fixed("client/skilltree/shop/ShopScreen.java", "a full-window shop: nothing to move");
        fixed("client/heroweapon/forms/WeaponBookScreen.java", "a full-window book: nothing to move");
        fixed("client/elite/hub/DungeonMapScreen.java", "a full-window map: nothing to move");
        fixed("client/multiverse/MultiverseStatusClient.java", "the compact status panel takes the spot of another mod's placeholder text on its screen: it follows that text");
        fixed("client/multiverse/FourKingdomsSlot.java", "a button fixed to a slot of the inventory screen: it follows the inventory");
        fixed("client/manazone/ManaZoneClient.java", "a full-screen tint of the camera: nothing to move");
        fixed("client/skilltree/ItemsClient.java", "the full-width 'skill acquired' banner and flash: an announcement across the whole screen");
        fixed("client/ui/gs/GsNotices.java", "the alert crest of the great_spirit look is placed by its own client setting ui.greatSpirit.noticePos (corner + offset), moved on its own screen (/nusmp_gui_crest)");
        fixed("client/ui/gs/GsCrestScreen.java", "the crest's own move screen (drag, Save, Reset); client only");
        // GUI Size entries that GUI Move leaves alone (by id)
        fixed("gui_scale:multiverse_status", "a full-window status book: nothing to move");
        fixed("gui_scale:skill_shop", "a full-window shop: nothing to move");
        fixed("gui_scale:weapon_book", "a full-window book: nothing to move");
        fixed("gui_scale:dungeon_map", "a full-window map: nothing to move");
        fixed("gui_scale:system_stats", "a full-window stat screen: nothing to move");
        fixed("gui_scale:arts_bar", "no layer of this name is registered anywhere in this mod, so there is nothing to move");
    }

    public static List<Element> all() { return Collections.unmodifiableList(ALL); }

    public static List<Fixed> fixed() { return Collections.unmodifiableList(FIXED); }

    public static Element byId(String id) { return BY_ID.get(id); }

    public static Element byClass(String className) { return BY_CLASS.get(className); }

    /** The elements of one kind, in registry order. */
    public static List<Element> of(Kind kind) {
        List<Element> out = new ArrayList<>();
        for (Element e : ALL) if (e.kind == kind) out.add(e);
        return out;
    }

    /** class name to element, for the screen hook. */
    public static Map<String, Element> screenIndex() { return new LinkedHashMap<>(BY_CLASS); }

    /** The class-name prefix of this mod. */
    public static String prefix() { return P; }
}
