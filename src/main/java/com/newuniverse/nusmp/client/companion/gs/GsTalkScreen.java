package com.newuniverse.nusmp.client.companion.gs;

import com.mojang.blaze3d.platform.NativeImage;
import com.newuniverse.nusmp.arc.ArcNpcEntity;
import com.newuniverse.nusmp.client.companion.CompanionScreens;
import com.newuniverse.nusmp.client.companion.TalkHost;
import com.newuniverse.nusmp.client.companion.TalkPainter;
import com.newuniverse.nusmp.client.playermodel.SkinPath;
import com.newuniverse.nusmp.client.ui.gs.GsNoticeQueue;
import com.newuniverse.nusmp.client.ui.gs.GsNotices;
import com.newuniverse.nusmp.client.ui.gs.GsUser;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;
import com.newuniverse.nusmp.companion.CompanionRules;
import com.newuniverse.nusmp.companion.bond.Bond;
import com.newuniverse.nusmp.companion.net.CompanionNet;
import com.newuniverse.nusmp.companion.talk.TalkView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The Great Spirit talk screen of a companion: a framed portrait with expressions, her name plate, a rapport meter of three leaves, the four tabs (Talk, Interact, Bond, Pouch), her words typed out in a box,
 * real item slots for gifts and for the pouch, stance icons, a follow switch, a heal-ready ring and a small bond story. It DECIDES NOTHING, exactly like the classic screen: every click is an ask (the id of a
 * choice on the view the server sent, or the vanilla "carry this slot" that the server already handles) and the server answers with a whole new view or closes the talk. All the drawing is
 * {@link GsTalkPainter} over {@link GsTalkLayout}; this class resolves the view into a reused {@link GsTalkFrame} when it arrives (texts once, never per frame), runs the clocks (typing, mood, rapport glide,
 * ripple), handles clicks and drags, and draws the item icons and tooltips through {@link GsCanvasImpl}.
 * <p>
 * Cost per frame: one layout hit test, about 200 fills/sprites, up to 9 pouch + 9 hotbar (or 36 picker) item icons, one text block with the compile animation. It allocates nothing at rest; the 36-slot
 * inventory scan runs every 5 ticks only while the Bond or Pouch tab is open; no packet is added (a pick, a close and the vanilla carried-slot packet).
 */
public final class GsTalkScreen extends Screen implements TalkHost {
    private static final String[] TIER_KEYS = {"stranger", "acquaintance", "friend", "close"};
    private static final String[] STANCE_KEYS = {"guard", "aggressive", "passive"};
    private static final Map<Item, String> IDS = new HashMap<>();

    private final int entityId;
    private TalkView view;
    private int tab, scroll;
    private boolean closedByServer, toldServer;

    private GsTalkLayout layout;
    private int lw = -1, lh = -1, lt = -1;
    private boolean textDirty = true;
    private final GsTalkFrame frame = new GsTalkFrame();
    private final GsCanvasImpl canvas = new GsCanvasImpl(this::itemFor);
    private final float openAt = UiTheme.realNow();

    // the companion
    private String companionKey = "";
    private ResourceLocation faceTex;
    private FaceInfo face = FaceInfo.NONE;
    private final MoodTimer moodTimer = new MoodTimer();
    private final RapportMeter.Track track = new RapportMeter.Track();

    // the options of the view, by kind
    private TalkView.Option oFive, oGift, oLikes, oPanel, oJoin, oWait, oPut, oTakeAll, oUseOn, oUseOff;
    private final TalkView.Option[] oStance = new TalkView.Option[3], oTake = new TalkView.Option[9];
    private final List<TalkView.Option> rowOpts = new ArrayList<>();
    private Set<String> ids = Set.of();

    // texts resolved once
    private final String[] moodNames = new String[Mood.values().length];
    private final Component[] tipRing = new Component[4], tipStance = new Component[3], tipTaste = new Component[4];
    private Component tipPortrait, tipGiftSlot, tipGive, tipMeter, tipMeterMax, tipFollowOn, tipFollowOff, tipMood;
    private String hintBond = "", mainText = "", scrollHint = "";

    // clocks
    private float typeStart, storyAt = -1000f, invAt = -1000f;
    private int storySeen = -1;
    private int hover = GsHit.NONE;

    // gifts and drags
    private int heldIndex = -1;
    private String pendingGiftId, pendingGiftName;
    private int dragIndex = -1, dragFromX, dragFromY;
    private boolean dragMoved;
    private final ItemStack[] tasteStacks = new ItemStack[8];
    private final List<String> tmpItems = new ArrayList<>();
    private final List<GiftMemory.Reaction> tmpReactions = new ArrayList<>();

    public GsTalkScreen(int entityId, TalkView view) {
        super(Component.empty());
        this.entityId = entityId;
        java.util.Arrays.fill(tasteStacks, ItemStack.EMPTY);
        resolveTexts();
        loadFace();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            Inventory inv = mc.player.getInventory();
            heldIndex = inv.getItem(inv.selected).isEmpty() ? -1 : inv.selected;
        }
        this.view = view;
        learnCompanion(view);
        update(view);
    }

    @Override public int entityId() { return entityId; }

    private float clock() { return UiTheme.realNow() - openAt; }

    private static String t(String key, Object... args) { return Component.translatable(key, args).getString(); }

    // ================================================================ texts
    private void resolveTexts() {
        String[] tabs = {"talk", "interact", "bond", "pouch"};
        for (int i = 0; i < 4; i++) frame.tabNames[i] = t("nusmp.companion.tab." + tabs[i]);
        String[] labels = frame.labels;
        labels[GsTalkFrame.L_CLOSE] = t("nusmp.companion.close");
        labels[GsTalkFrame.L_GIVE] = t("nusmp.companion.gs.give");
        labels[GsTalkFrame.L_LIKES] = t("nusmp.companion.gs.likes");
        labels[GsTalkFrame.L_FIVE] = t("nusmp.companion.bond.five");
        labels[GsTalkFrame.L_FOLLOW] = t("nusmp.companion.gs.follow");
        labels[GsTalkFrame.L_WAIT] = t("nusmp.companion.gs.wait");
        labels[GsTalkFrame.L_PANEL] = t("nusmp.companion.gs.panel");
        labels[GsTalkFrame.L_USE_ON] = t("nusmp.companion.gs.use_on");
        labels[GsTalkFrame.L_USE_OFF] = t("nusmp.companion.gs.use_off");
        labels[GsTalkFrame.L_PUT] = t("nusmp.companion.gs.put");
        labels[GsTalkFrame.L_TAKE_ALL] = t("nusmp.companion.gs.take_all");
        labels[GsTalkFrame.L_TASTES] = t("nusmp.companion.gs.tastes");
        labels[GsTalkFrame.L_STORY] = t("nusmp.companion.gs.story");
        labels[GsTalkFrame.L_PICKER] = t("nusmp.companion.gs.picker");
        labels[GsTalkFrame.L_GIFT_HINT] = t("nusmp.companion.gs.gift_hint");
        labels[GsTalkFrame.L_HAND] = t("nusmp.companion.gs.hand");
        labels[GsTalkFrame.L_NO_STORY] = t("nusmp.companion.gs.story.none");
        labels[GsTalkFrame.L_RING_READY] = t("nusmp.companion.gs.ring.ready");
        labels[GsTalkFrame.L_RING_EMPTY] = t("nusmp.companion.gs.ring.empty");
        labels[GsTalkFrame.L_RING_OFF] = t("nusmp.companion.gs.ring.off");
        labels[GsTalkFrame.L_RING_LOCKED] = t("nusmp.companion.gs.ring.locked");
        for (int i = 0; i < 3; i++) labels[GsTalkFrame.L_STANCE0 + i] = t("nusmp.companion.stance." + STANCE_KEYS[i]);
        labels[GsTalkFrame.L_MOOD] = t("nusmp.companion.gs.mood");
        labels[GsTalkFrame.L_NO_TASTES] = t("nusmp.companion.gs.tastes.none");
        labels[GsTalkFrame.L_FOLLOWING] = t("nusmp.companion.gs.state.following");
        labels[GsTalkFrame.L_WAITING] = t("nusmp.companion.gs.state.waiting");
        labels[GsTalkFrame.L_PUT_HINT] = t("nusmp.companion.gs.put_hint");
        String[] moods = {"neutral", "happy", "shy", "angry", "hungry"};
        for (Mood m : Mood.values()) moodNames[m.ordinal()] = t("nusmp.companion.gs.mood." + moods[Math.min(m.ordinal(), moods.length - 1)]);
        tipPortrait = Component.translatable("nusmp.companion.gs.tip.portrait");
        tipGiftSlot = Component.translatable("nusmp.companion.gs.tip.gift_slot");
        tipGive = Component.translatable("nusmp.companion.gs.tip.give");
        tipMeter = Component.translatable("nusmp.companion.gs.tip.meter");
        tipMeterMax = Component.translatable("nusmp.companion.gs.tip.meter.max");
        tipFollowOn = Component.translatable("nusmp.companion.gs.tip.follow.on");
        tipFollowOff = Component.translatable("nusmp.companion.gs.tip.follow.off");
        tipMood = Component.translatable("nusmp.companion.gs.tip.mood");
        tipRing[PouchRing.State.READY.ordinal()] = Component.translatable("nusmp.companion.gs.tip.ring.ready");
        tipRing[PouchRing.State.EMPTY.ordinal()] = Component.translatable("nusmp.companion.gs.tip.ring.empty");
        tipRing[PouchRing.State.OFF.ordinal()] = Component.translatable("nusmp.companion.gs.tip.ring.off");
        tipRing[PouchRing.State.LOCKED.ordinal()] = Component.translatable("nusmp.companion.gs.tip.ring.locked");
        for (int i = 0; i < 3; i++) tipStance[i] = Component.translatable("nusmp.companion.bond.stance." + STANCE_KEYS[i]);
        tipTaste[GiftMemory.Reaction.LOVED.ordinal()] = Component.translatable("nusmp.companion.gs.tip.taste.loved");
        tipTaste[GiftMemory.Reaction.LIKED.ordinal()] = Component.translatable("nusmp.companion.gs.tip.taste.liked");
        tipTaste[GiftMemory.Reaction.HATED.ordinal()] = Component.translatable("nusmp.companion.gs.tip.taste.hated");
        tipTaste[GiftMemory.Reaction.REFUSED.ordinal()] = Component.translatable("nusmp.companion.gs.tip.taste.refused");
        hintBond = t("nusmp.companion.gs.hint.bond");
    }

    // ================================================================ the companion (name key, portrait)
    private void learnCompanion(TalkView v) {
        String key = null;
        if (v.sub.getContents() instanceof TranslatableContents tc) key = CompanionCues.prefixOf(tc.getKey());
        if (key == null && v.text.getContents() instanceof TranslatableContents tc) key = CompanionCues.prefixOf(tc.getKey());
        companionKey = key != null ? key : "name:" + v.name.getString();
    }

    /** Reads her skin's face once, when the screen opens: no skin (or a skin of another size) leaves the clover crest and her initial. */
    private void loadFace() {
        Minecraft mc = Minecraft.getInstance();
        Entity e = mc.level == null ? null : mc.level.getEntity(entityId);
        if (!(e instanceof ArcNpcEntity npc)) return;
        String id = npc.characterId();
        String[] tries = {id, id != null && id.startsWith("nusmp_") ? id.substring(6) : null};
        for (String name : tries) {
            if (name == null) continue;
            String path = SkinPath.resolve(name);
            if (path.isEmpty()) continue;
            ResourceLocation loc = ResourceLocation.fromNamespaceAndPath(SkinPath.NAMESPACE, path);
            if (mc.getResourceManager().getResource(loc).isEmpty()) continue;
            try (InputStream in = mc.getResourceManager().open(loc); NativeImage img = NativeImage.read(in)) {
                int w = img.getWidth(), h = img.getHeight();
                int[] argb = new int[w * h];
                for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                    int abgr = img.getPixelRGBA(x, y);
                    argb[y * w + x] = (abgr & 0xFF00FF00) | ((abgr & 0xFF) << 16) | ((abgr >> 16) & 0xFF);
                }
                FaceInfo fi = FaceInfo.from(argb, w, h);
                if (fi.has()) {
                    face = fi;
                    faceTex = loc;
                    return;
                }
            } catch (IOException | RuntimeException ex) {
                // no portrait: the crest stands in
            }
        }
    }

    // ================================================================ a view arrives
    /** A new screen of the conversation from the server (the answer to a pick). */
    @Override
    public void update(TalkView v) {
        this.view = v;
        float now = clock();
        frame.name = v.name.getString();
        frame.sub = v.sub.getString();
        frame.color = v.color;
        mainText = v.text.getString();
        textDirty = true;
        typeStart = now;
        sortOptions(v);

        // what the words mean, read from the lang key alone
        String key = v.text.getContents() instanceof TranslatableContents tc ? tc.getKey() : null;
        CompanionCues.Cue cue = CompanionCues.cueOf(key);
        Mood m = CompanionCues.moodOf(cue);
        if (m != null) moodTimer.set(m, now);
        eventsOf(cue, v, now);
        pendingGiftId = null;
        pendingGiftName = null;

        // rapport
        int tier = v.rapport >= 0 ? RapportMeter.tier(v.rapport) : 1;
        int before = track.rapport();
        int delta = track.update(v.rapport, now);
        frame.rapport = v.rapport;
        frame.tierName = v.rapport >= 0 ? t("nusmp.companion.tier." + TIER_KEYS[tier]) : "";
        frame.rapportText = v.rapport >= 0 ? Integer.toString(v.rapport) : "";
        frame.tierLine = frame.tierName;
        frame.toNext = v.rapport >= 0 ? RapportMeter.toNext(v.rapport) : 0;
        frame.nextText = frame.toNext > 0 ? "+" + frame.toNext : "";
        if (delta > 0) {
            moodTimer.set(Mood.HAPPY, now);
            CompanionScreens.story().add(companionKey, BondStory.Kind.RAPPORT, t("nusmp.companion.gs.ev.rapport", delta), gameTime());
            CompanionScreens.storyChanged();
            notice("companion:rapport", frame.name + ": " + t("nusmp.companion.gs.ev.rapport", delta), GsNoticeQueue.Kind.OK);
            if (RapportMeter.tier(before) < tier) {
                CompanionScreens.story().add(companionKey, BondStory.Kind.RAPPORT, t("nusmp.companion.gs.ev.tier", frame.tierName), gameTime());
                notice("companion:tier", frame.name + ": " + t("nusmp.companion.gs.ev.tier", frame.tierName), GsNoticeQueue.Kind.OK);
            }
            if (UiTheme.motion()) Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_CHIME, 1.2f));
        }

        // stance, follow, use
        frame.stance = v.stance;
        frame.useIt = v.useIt;
        frame.stanceOffered = false;
        for (int i = 0; i < 3; i++) {
            frame.stanceBlocked[i] = oStance[i] != null && oStance[i].blocked();
            if (oStance[i] != null) frame.stanceOffered = true;
        }
        frame.hasFive = oFive != null;
        frame.fiveBlocked = oFive != null && oFive.blocked();
        if (oFive != null) frame.labels[GsTalkFrame.L_FIVE] = oFive.label().getString();
        frame.hasPanel = oPanel != null;
        frame.panelBlocked = oPanel != null && oPanel.blocked();
        if (oPanel != null) frame.labels[GsTalkFrame.L_PANEL] = oPanel.label().getString();
        frame.hasFollow = oJoin != null || oWait != null;
        frame.following = oWait != null;
        TalkView.Option fo = oWait != null ? oWait : oJoin;
        frame.followBlocked = fo != null && fo.blocked();
        frame.hasGift = oGift != null;
        frame.giftBlocked = oGift != null && oGift.blocked();
        frame.hasLikes = oLikes != null;
        frame.hasPut = oPut != null;
        frame.putBlocked = oPut != null && oPut.blocked() && oPut.blockedReason().getContents() instanceof TranslatableContents bc && "nusmp.companion.bond.pouch.locked".equals(bc.getKey());
        frame.hasTakeAll = oTakeAll != null;
        frame.hasUse = oUseOn != null || oUseOff != null;

        // the pouch and its ring
        int n = CompanionRules.POUCH_SLOTS;
        int[] kinds = new int[n], values = new int[n];
        for (int i = 0; i < n; i++) {
            ItemStack s = i < v.pouch.size() ? v.pouch.get(i) : ItemStack.EMPTY;
            int[] kv = Bond.kindAndValue(s);
            kinds[i] = kv[0];
            values[i] = kv[1];
            frame.pouchCounts[i] = s.isEmpty() ? "" : Integer.toString(s.getCount());
        }
        frame.ring = PouchRing.of(v.useIt, kinds, values);
        frame.ringSlot = frame.ring == PouchRing.State.READY ? PouchRing.bestSlot(kinds, values) : -1;

        refreshNotebook();
        rebuildRows();
        invAt = -1000f;
    }

    private void sortOptions(TalkView v) {
        oFive = oGift = oLikes = oPanel = oJoin = oWait = oPut = oTakeAll = oUseOn = oUseOff = null;
        java.util.Arrays.fill(oStance, null);
        java.util.Arrays.fill(oTake, null);
        Set<String> all = new HashSet<>();
        for (TalkView.Option o : v.options) {
            all.add(o.id());
            switch (OptionKinds.of(o.id())) {
                case FIVE -> oFive = o;
                case GIFT -> oGift = o;
                case LIKES -> oLikes = o;
                case PANEL -> oPanel = o;
                case JOIN -> oJoin = o;
                case WAIT -> oWait = o;
                case PUT -> oPut = o;
                case TAKE_ALL -> oTakeAll = o;
                case USE_ON -> oUseOn = o;
                case USE_OFF -> oUseOff = o;
                case STANCE -> {
                    String s = OptionKinds.stanceOf(o.id());
                    for (int i = 0; i < 3; i++) if (STANCE_KEYS[i].equals(s)) oStance[i] = o;
                }
                case TAKE -> {
                    int slot = OptionKinds.takeSlot(o.id());
                    if (slot >= 0) oTake[slot] = o;
                }
                default -> { }
            }
        }
        ids = all;
    }

    /** Puts what the cue means into the bond story, the notebook and the crest. */
    private void eventsOf(CompanionCues.Cue cue, TalkView v, float now) {
        BondStory story = CompanionScreens.story();
        long gt = gameTime();
        GiftMemory.Reaction r = CompanionCues.reactionOf(cue);
        String item = pendingGiftName == null ? "?" : pendingGiftName;
        if (r != null && pendingGiftId != null) {
            CompanionScreens.gifts().record(companionKey, pendingGiftId, r);
            switch (r) {
                case LOVED -> {
                    story.add(companionKey, BondStory.Kind.LOVED, t("nusmp.companion.gs.ev.loved", item), gt);
                    notice("companion:loved", frame.name + ": " + t("nusmp.companion.gs.ev.loved", item), GsNoticeQueue.Kind.OK);
                }
                case LIKED -> story.add(companionKey, BondStory.Kind.LIKED, t("nusmp.companion.gs.ev.liked", item), gt);
                case HATED -> {
                    story.add(companionKey, BondStory.Kind.HATED, t("nusmp.companion.gs.ev.hated", item), gt);
                    notice("companion:hated", frame.name + ": " + t("nusmp.companion.gs.ev.hated", item), GsNoticeQueue.Kind.WARN);
                }
                default -> story.add(companionKey, BondStory.Kind.NOTE, t("nusmp.companion.gs.ev.refused", item), gt);
            }
            CompanionScreens.storyChanged();
            return;
        }
        switch (cue) {
            case FIVE -> story.add(companionKey, BondStory.Kind.FIVE, t("nusmp.companion.gs.ev.five"), gt);
            case JOIN_OK -> {
                story.add(companionKey, BondStory.Kind.JOINED, t("nusmp.companion.gs.ev.joined"), gt);
                notice("companion:joined", frame.name + ": " + t("nusmp.companion.gs.ev.joined"), GsNoticeQueue.Kind.INFO);
            }
            case WAIT -> {
                story.add(companionKey, BondStory.Kind.LEFT, t("nusmp.companion.gs.ev.left"), gt);
                notice("companion:left", frame.name + ": " + t("nusmp.companion.gs.ev.left"), GsNoticeQueue.Kind.INFO);
            }
            case STANCE -> {
                if (v.stance >= 0 && v.stance < 3) story.add(companionKey, BondStory.Kind.STANCE, t("nusmp.companion.gs.ev.stance", t("nusmp.companion.stance." + STANCE_KEYS[v.stance])), gt);
            }
            case PUT, TOOK -> story.add(companionKey, BondStory.Kind.POUCH, mainText, gt);
            default -> {
                return;
            }
        }
        CompanionScreens.storyChanged();
    }

    private static long gameTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? 0L : mc.level.getGameTime();
    }

    private static void notice(String key, String text, GsNoticeQueue.Kind kind) {
        if (GsNotices.active()) GsNotices.offer(key, text, kind);
    }

    private void rebuildRows() {
        rowOpts.clear();
        frame.rows.clear();
        for (TalkView.Option o : view.options) {
            if (OptionKinds.of(o.id()) != OptionKinds.Kind.TOPIC) continue;
            if (!(o.tab() == tab || (tab == 1 && o.tab() == 3))) continue;
            rowOpts.add(o);
            frame.rows.add(new GsTalkFrame.Row(o.label().getString(), o.blocked()));
        }
        frame.tab = tab;
        if (layout != null) scroll = Math.max(0, Math.min(scroll, layout.maxScroll(rowOpts.size())));
        frame.scroll = scroll;
        refreshHint();
    }

    private void refreshHint() {
        int vis = layout == null ? 1 : layout.visibleRows;
        int n = rowOpts.size();
        scrollHint = n > vis ? (scroll + 1) + "-" + Math.min(n, scroll + vis) + " / " + n : "";
        frame.hint = !scrollHint.isEmpty() ? scrollHint : tab == 2 && frame.hasGift ? hintBond : "";
    }

    private void refreshNotebook() {
        int cells = layout == null ? 8 : Math.max(1, layout.tasteCells());
        CompanionScreens.gifts().notebook(companionKey, Math.min(8, cells), tmpItems, tmpReactions);
        frame.tasteReactions.clear();
        for (int i = 0; i < 8; i++) {
            ItemStack st = ItemStack.EMPTY;
            if (i < tmpItems.size()) {
                try {
                    Item it = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(tmpItems.get(i))).orElse(null);
                    if (it != null) st = new ItemStack(it);
                } catch (RuntimeException ex) {
                    st = ItemStack.EMPTY;
                }
            }
            tasteStacks[i] = st;
            if (i < tmpItems.size()) frame.tasteReactions.add(tmpReactions.get(i));
        }
    }

    // ================================================================ items
    private static String itemId(ItemStack s) {
        Item it = s.getItem();
        String id = IDS.get(it);
        if (id == null) {
            if (IDS.size() > 512) IDS.clear();
            id = BuiltInRegistries.ITEM.getKey(it).toString();
            IDS.put(it, id);
        }
        return id;
    }

    private ItemStack itemFor(int ref) {
        Minecraft mc = Minecraft.getInstance();
        if (GsItemRef.isPouch(ref)) return view != null && ref < view.pouch.size() ? view.pouch.get(ref) : ItemStack.EMPTY;
        if (GsItemRef.isInventory(ref)) return mc.player == null ? ItemStack.EMPTY : mc.player.getInventory().getItem(GsItemRef.inventoryIndex(ref));
        if (ref == GsItemRef.GIFT) return heldIndex >= 0 && mc.player != null ? mc.player.getInventory().getItem(heldIndex) : ItemStack.EMPTY;
        if (GsItemRef.isTaste(ref)) return tasteStacks[GsItemRef.tasteIndex(ref)];
        return ItemStack.EMPTY;
    }

    /** Re-reads the player's 36 slots into the frame (every 5 ticks while the Bond or Pouch tab is open, and after anything that moves an item). */
    private void refreshInventory() {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null) return;
        Inventory inv = p.getInventory();
        boolean memory = CompanionScreens.gifts().count(companionKey) > 0;
        long filled = 0;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) {
                frame.inventoryReactions[i] = null;
            } else {
                filled |= 1L << i;
                frame.inventoryReactions[i] = memory ? CompanionScreens.gifts().get(companionKey, itemId(s)) : null;
            }
        }
        frame.inventoryFilled = filled;
        if (heldIndex >= 0 && inv.getItem(heldIndex).isEmpty()) heldIndex = -1;
        frame.heldIndex = heldIndex;
        ItemStack held = heldIndex >= 0 ? inv.getItem(heldIndex) : ItemStack.EMPTY;
        frame.heldCount = held.getCount();
        frame.heldReaction = held.isEmpty() || !memory ? null : CompanionScreens.gifts().get(companionKey, itemId(held));
    }

    /**
     * Makes the stack at an inventory index the one in her hand (the server's rules read the main hand): a hotbar slot is selected (the vanilla carried-slot packet), a main-inventory slot is swapped with the
     * selected hotbar slot (a vanilla inventory click). Returns the hotbar index the item now sits in, or -1.
     */
    private int equip(int idx) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.getConnection() == null || idx < 0 || idx >= 36) return -1;
        Inventory inv = p.getInventory();
        if (inv.getItem(idx).isEmpty()) return -1;
        if (idx < 9) {
            if (inv.selected != idx) {
                inv.selected = idx;
                mc.getConnection().send(new ServerboundSetCarriedItemPacket(idx));
            }
            return idx;
        }
        if (mc.gameMode == null) return -1;
        mc.gameMode.handleInventoryMouseClick(p.inventoryMenu.containerId, idx, inv.selected, ClickType.SWAP, p);
        return inv.selected;
    }

    private void give(int idx) {
        if (oGift == null || oGift.blocked()) return;
        int at = equip(idx);
        if (at < 0) return;
        ItemStack st = Minecraft.getInstance().player.getInventory().getItem(at);
        pendingGiftId = itemId(st);
        pendingGiftName = st.getHoverName().getString();
        heldIndex = at;
        frame.pickerOpen = false;
        click();
        send(oGift.id());
    }

    private void put(int idx) {
        if (oPut == null || oPut.blocked()) return;
        int at = equip(idx);
        if (at < 0) return;
        heldIndex = at;
        click();
        send(oPut.id());
    }

    // ================================================================ drawing
    @Override public boolean isPauseScreen() { return false; }

    private void ensureLayout() {
        if (layout != null && lw == width && lh == height && lt == tab) return;
        layout = GsTalkLayout.compute(width, height, tab);
        lw = width;
        lh = height;
        lt = tab;
        textDirty = true;
        scroll = Math.max(0, Math.min(scroll, layout.maxScroll(rowOpts.size())));
        frame.scroll = scroll;
        frame.tab = tab;
        refreshNotebook();
        rebuildRowsKeepScroll();
    }

    private void rebuildRowsKeepScroll() {
        int keep = scroll;
        rebuildRows();
        scroll = Math.max(0, Math.min(keep, layout.maxScroll(rowOpts.size())));
        frame.scroll = scroll;
        refreshHint();
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderTransparentBackground(g);
        ensureLayout();
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        float now = clock();
        boolean motion = UiTheme.motion();

        if (textDirty) {
            canvas.font(font);
            List<String> lines = TalkPainter.wrap(canvas, mainText, layout.dialogue[2] - 18, layout.dialogueLines);
            frame.dialogue = lines.toArray(new String[0]);
            textDirty = false;
        }
        int sv = CompanionScreens.storyVersion();
        if (sv != storySeen) {
            storySeen = sv;
            storyAt = now;
            rebuildStory();
        }
        if ((tab == 2 || tab == 3 || frame.pickerOpen) && now - invAt >= 5f) {
            invAt = now;
            refreshInventory();
        }

        frame.skin = GsUser.skin();
        frame.fancy = motion && UiThemeOptions.highQuality();
        frame.time = motion ? now : 0f;
        frame.tab = tab;
        frame.scroll = scroll;
        int tier = view.rapport >= 0 ? RapportMeter.tier(view.rapport) : 1;
        Mood mood = moodTimer.at(now, tier);
        frame.mood = mood;
        frame.moodAge = moodTimer.age(now);
        frame.moodName = moodNames[mood.ordinal()];
        frame.face = face;
        frame.rapportShown = motion ? track.shown(now) : Math.max(0, view.rapport);
        frame.rapportGlow = motion ? track.glow(now) : 0f;
        frame.litLeaf = motion ? track.litLeaf(now) : -1;
        frame.sinceRise = track.sinceRise(now);
        boolean typing = motion && UiThemeOptions.typing();
        int total = TypeWriter.total(frame.dialogue);
        frame.typed = TypeWriter.shown(total, now - typeStart, typing);
        frame.dialogueDone = TypeWriter.done(total, now - typeStart, typing);
        frame.storyAge = motion ? now - storyAt : 1000f;
        frame.heldIndex = heldIndex;
        frame.pickerOpen = frame.pickerOpen && frame.hasGift;

        int h = layout.hit(mx, my, frame);
        if (h != hover) {
            hover = h;
            int[] r = layout.rectOf(h, frame);
            if (r != null && h != GsHit.NONE) {
                frame.rippleX = mx;
                frame.rippleY = my;
                frame.rippleAt = now;
            }
        }
        frame.hover = hover;

        canvas.set(g, font, faceTex, face.texH());
        GsTalkPainter.paint(canvas, layout, frame);

        if (dragIndex >= 0 && dragMoved && mc.player != null) {
            ItemStack d = mc.player.getInventory().getItem(dragIndex);
            if (!d.isEmpty()) {
                g.pose().pushPose();
                g.pose().translate(0f, 0f, 300f);
                g.renderItem(d, mx - 8, my - 8);
                g.pose().popPose();
            }
        } else {
            tooltip(g, font, mx, my, hover);
        }
    }

    private void rebuildStory() {
        BondStory story = CompanionScreens.story();
        int n = Math.min(BondStory.MAX, story.size(companionKey));
        String[] lines = new String[n];
        BondStory.Kind[] kinds = new BondStory.Kind[n];
        for (int i = 0; i < n; i++) {
            BondStory.Event e = story.get(companionKey, i);
            lines[i] = e.text();
            kinds[i] = e.kind();
        }
        frame.story = lines;
        frame.storyKinds = kinds;
    }

    // ================================================================ tooltips
    private void tooltip(GuiGraphics g, Font font, int mx, int my, int h) {
        if (h == GsHit.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        if (GsHit.isPouch(h)) {
            int i = h - GsHit.POUCH;
            if (i < view.pouch.size() && !view.pouch.get(i).isEmpty()) g.renderTooltip(font, view.pouch.get(i), mx, my);
            return;
        }
        if ((GsHit.isPicker(h) || GsHit.isHotbar(h)) && mc.player != null) {
            int idx = GsHit.isPicker(h) ? h - GsHit.PICKER : h - GsHit.HOTBAR;
            ItemStack s = mc.player.getInventory().getItem(idx);
            if (s.isEmpty()) return;
            GiftMemory.Reaction r = frame.inventoryReactions[idx];
            if (r == null) {
                g.renderTooltip(font, s, mx, my);
            } else {
                List<Component> tip = new ArrayList<>(Screen.getTooltipFromItem(mc, s));
                tip.add(tipTaste[r.ordinal()]);
                g.renderComponentTooltip(font, tip, mx, my);
            }
            return;
        }
        if (GsHit.isTaste(h)) {
            int i = h - GsHit.TASTE;
            ItemStack s = tasteStacks[i];
            if (s.isEmpty() || i >= frame.tasteReactions.size()) return;
            List<Component> tip = new ArrayList<>(Screen.getTooltipFromItem(mc, s));
            tip.add(tipTaste[frame.tasteReactions.get(i).ordinal()]);
            g.renderComponentTooltip(font, tip, mx, my);
            return;
        }
        if (GsHit.isStance(h)) {
            g.renderTooltip(font, tipStance[h - GsHit.STANCE], mx, my);
            return;
        }
        if (GsHit.isRow(h)) {
            int i = GsHit.rowIndex(h);
            if (i < rowOpts.size() && rowOpts.get(i).blocked() && !rowOpts.get(i).blockedReason().getString().isEmpty()) g.renderTooltip(font, rowOpts.get(i).blockedReason(), mx, my);
            return;
        }
        switch (h) {
            case GsHit.PORTRAIT -> g.renderTooltip(font, tipPortrait, mx, my);
            case GsHit.METER -> g.renderTooltip(font, view.rapport >= RapportMeter.LEAF_AT[2] ? tipMeterMax : tipMeter, mx, my);
            case GsHit.MOOD -> g.renderTooltip(font, tipMood, mx, my);
            case GsHit.GIFT_SLOT -> g.renderTooltip(font, tipGiftSlot, mx, my);
            case GsHit.GIVE -> g.renderTooltip(font, tipGive, mx, my);
            case GsHit.RING -> g.renderTooltip(font, tipRing[frame.ring.ordinal()], mx, my);
            case GsHit.FOLLOW -> {
                TalkView.Option o = oWait != null ? oWait : oJoin;
                if (o != null && o.blocked() && !o.blockedReason().getString().isEmpty()) g.renderTooltip(font, o.blockedReason(), mx, my);
                else g.renderTooltip(font, frame.following ? tipFollowOn : tipFollowOff, mx, my);
            }
            case GsHit.FIVE -> blockedTip(g, font, oFive, mx, my);
            case GsHit.PANEL -> blockedTip(g, font, oPanel, mx, my);
            case GsHit.PUT -> blockedTip(g, font, oPut, mx, my);
            case GsHit.USE -> blockedTip(g, font, frame.useIt == 1 ? oUseOff : oUseOn, mx, my);
            default -> { }
        }
    }

    private static void blockedTip(GuiGraphics g, Font font, TalkView.Option o, int mx, int my) {
        if (o != null && o.blocked() && !o.blockedReason().getString().isEmpty()) g.renderTooltip(font, o.blockedReason(), mx, my);
    }

    // ================================================================ input
    @Override
    public boolean mouseClicked(double mxd, double myd, int button) {
        if (button != 0) return super.mouseClicked(mxd, myd, button);
        int mx = (int) mxd, my = (int) myd;
        int h = layout.hit(mx, my, frame);
        if (frame.pickerOpen && !GsTalkLayout.in(layout.picker, mx, my) && h != GsHit.GIFT_SLOT) {
            frame.pickerOpen = false;
            click();
            return true;
        }
        if (GsHit.isTab(h)) {
            int t = h - GsHit.TAB;
            if (t != tab) {
                tab = t;
                scroll = 0;
                frame.pickerOpen = false;
                click();
                ensureLayout();
                rebuildRows();
                invAt = -1000f;
                typeStart = clock();
            }
            return true;
        }
        if (GsHit.isRow(h)) {
            int i = GsHit.rowIndex(h);
            if (i < rowOpts.size() && !rowOpts.get(i).blocked()) ask(rowOpts.get(i));
            return true;
        }
        if (GsHit.isPouch(h)) {
            int i = h - GsHit.POUCH;
            if (oTake[i] != null) ask(oTake[i]);
            else if (heldIndex >= 0 && oPut != null) put(heldIndex);
            else if (oPut != null && !layout.stripShown) {
                frame.pickerOpen = true;
                invAt = -1000f;
                click();
            }
            return true;
        }
        if (GsHit.isPicker(h) || GsHit.isHotbar(h)) {
            int idx = GsHit.isPicker(h) ? h - GsHit.PICKER : h - GsHit.HOTBAR;
            if ((frame.inventoryFilled & (1L << idx)) != 0) {
                dragIndex = idx;
                dragFromX = mx;
                dragFromY = my;
                dragMoved = false;
            }
            return true;
        }
        if (GsHit.isStance(h)) {
            TalkView.Option o = oStance[h - GsHit.STANCE];
            if (o != null && !o.blocked()) ask(o);
            return true;
        }
        if (GsHit.isTaste(h)) return true;
        switch (h) {
            case GsHit.CLOSE -> {
                click();
                onClose();
            }
            case GsHit.DIALOGUE -> typeStart = clock() - TypeWriter.duration(TypeWriter.total(frame.dialogue)) - 1f;
            case GsHit.PORTRAIT -> moodTimer.set(Mood.HAPPY, clock());
            case GsHit.GIFT_SLOT -> {
                if (frame.hasGift && !frame.giftBlocked) {
                    frame.pickerOpen = !frame.pickerOpen;
                    invAt = -1000f;
                    click();
                }
            }
            case GsHit.GIVE -> {
                if (!frame.hasGift || frame.giftBlocked) return true;
                if (heldIndex >= 0) give(heldIndex);
                else {
                    frame.pickerOpen = true;
                    invAt = -1000f;
                    click();
                }
            }
            case GsHit.LIKES -> {
                if (oLikes != null && !oLikes.blocked()) ask(oLikes);
            }
            case GsHit.FIVE -> {
                if (oFive != null && !oFive.blocked()) ask(oFive);
            }
            case GsHit.PANEL -> {
                if (oPanel != null && !oPanel.blocked()) ask(oPanel);
            }
            case GsHit.FOLLOW -> {
                TalkView.Option o = oWait != null ? oWait : oJoin;
                if (o != null && !o.blocked()) ask(o);
            }
            case GsHit.USE -> {
                TalkView.Option o = frame.useIt == 1 ? (oUseOff != null ? oUseOff : oUseOn) : (oUseOn != null ? oUseOn : oUseOff);
                if (o != null && !o.blocked()) ask(o);
            }
            case GsHit.PUT -> {
                if (heldIndex >= 0) put(heldIndex);
                else if (!layout.stripShown) {                              // no room for the hotbar strip: the picker chooses the item
                    frame.pickerOpen = true;
                    invAt = -1000f;
                    click();
                }
            }
            case GsHit.TAKE_ALL -> {
                if (oTakeAll != null && !oTakeAll.blocked()) ask(oTakeAll);
            }
            default -> {
                return super.mouseClicked(mxd, myd, button);
            }
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragIndex >= 0 && button == 0) {
            if (Math.abs(mx - dragFromX) + Math.abs(my - dragFromY) > 4) dragMoved = true;
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mxd, double myd, int button) {
        if (dragIndex >= 0 && button == 0) {
            int idx = dragIndex, mx = (int) mxd, my = (int) myd;
            boolean moved = dragMoved;
            dragIndex = -1;
            dragMoved = false;
            int h = layout.hit(mx, my, frame);
            if (!moved) {                                                    // a plain click: this is the item she is given or the pouch gets
                heldIndex = idx;
                frame.heldIndex = idx;
                frame.pickerOpen = false;
                click();
                invAt = -1000f;
            } else if (h == GsHit.PORTRAIT || h == GsHit.GIVE) {
                give(idx);
            } else if (h == GsHit.GIFT_SLOT) {
                heldIndex = idx;
                frame.pickerOpen = false;
                click();
            } else if (h == GsHit.PUT || GsHit.isPouch(h)) {
                put(idx);
            }
            return true;
        }
        return super.mouseReleased(mxd, myd, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (rowOpts.size() > layout.visibleRows && GsTalkLayout.in(layout.list, (int) mx, (int) my)) {
            scroll = Math.max(0, Math.min(layout.maxScroll(rowOpts.size()), scroll - (int) Math.signum(sy)));
            frame.scroll = scroll;
            refreshHint();
            return true;
        }
        return super.mouseScrolled(mx, my, sx, sy);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && frame.pickerOpen) {                           // Escape closes the picker first
            frame.pickerOpen = false;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void ask(TalkView.Option o) {
        if (o == null || o.blocked() || !ids.contains(o.id())) return;
        click();
        send(o.id());
    }

    private void click() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
    }

    private void send(String id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) PacketDistributor.sendToServer(new CompanionNet.Pick(entityId, id));
    }

    /** The server closed the conversation. */
    @Override
    public void closeFromServer() {
        closedByServer = true;
        onClose();
    }

    @Override
    public void removed() {
        if (!closedByServer && !toldServer) {
            toldServer = true;
            Minecraft mc = Minecraft.getInstance();
            if (mc.getConnection() != null) PacketDistributor.sendToServer(new CompanionNet.Close(entityId));
        }
        super.removed();
    }
}
