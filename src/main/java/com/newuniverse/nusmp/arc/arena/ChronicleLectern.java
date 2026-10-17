package com.newuniverse.nusmp.arc.arena;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.arc.ArcCharacter;
import com.newuniverse.nusmp.arc.ArcConfig;
import com.newuniverse.nusmp.arc.ArcRoster;
import com.newuniverse.nusmp.arc.ArcSaga;
import com.newuniverse.nusmp.arc.hub.ChronicleSite;
import com.newuniverse.nusmp.arc.story.ArcStory;
import com.newuniverse.nusmp.arc.world.ArcWorldConfig;
import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;

import java.util.List;

/**
 * Why: the lectern of the Chronicle is where a player READS the story and CHOOSES whom the Chronicle Sigil carries them to, so the game side of it is three small
 * things: a click on the lectern shows the player's story as chat lines (one page per saga: each boss as defeated, open or locked with the names it still waits
 * for, the open ones with a click-to-choose button, and a button for the next saga), the choice is stored per player in the arena save (a plain boss id), and the
 * sigil asks {@link #pickOf} which boss that is right now (a stale choice falls back to the next boss of the story, see {@link ChroniclePick}). All the rules
 * (what is open, what may be chosen, how a stale choice resolves) are the pure {@link ChronicleView} and {@link ChroniclePick}; this class only turns their rows
 * into chat lines and the lectern's click into a call. The lectern block is vanilla: its own screen would show an empty book, so the click is taken here.
 */
public final class ChronicleLectern {
    private ChronicleLectern() {}

    private static final Logger LOG = LogUtils.getLogger();

    private static boolean registered;

    public static synchronized void register(IEventBus bus) {
        if (registered) return;
        registered = true;
        bus.addListener(EventPriority.HIGH, ChronicleLectern::onUse);
    }

    // ---------------------------------------------------------------- the story as the sigil reads it
    /** The rows of this player's story: every boss of the roster with its state. */
    public static List<ChronicleView.Row> rows(ServerPlayer p) {
        return ChronicleView.rows(ArcRoster.all(), ArcStory.data(p), ArcConfig.storyGate());
    }

    /** The boss the sigil carries this player to now (the stored choice if it can still be fought, else the next boss of the story), or null. */
    public static ArcCharacter pickOf(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return null;
        String id = ChroniclePick.choose(rows(p), ArcArenaSave.get(server).pick(p.getUUID()), ArcWorldConfig.arenaReplay());
        return id == null ? null : ArcRoster.byId(id);
    }

    /** Stores a choice if it may be made now. Returns the character, or null (the player has been told why). */
    public static ArcCharacter choose(ServerPlayer p, String id) {
        MinecraftServer server = p.getServer();
        ArcCharacter c = id == null ? null : ArcRoster.byId(id);
        if (server == null || c == null || !ChroniclePick.pickable(rows(p), c.id(), ArcWorldConfig.arenaReplay())) {
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.chronicle.cannot_pick", "The Chronicle cannot carry you to that one yet.").withStyle(ChatFormatting.GRAY));
            return null;
        }
        ArcArenaSave save = ArcArenaSave.get(server);
        save.pick(p.getUUID(), c.id());
        p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.chronicle.picked", "The Chronicle Sigil will carry you to %s.", c.fullName()).withStyle(ChatFormatting.GOLD));
        return c;
    }

    /** The next boss that can be fought after the current choice, stored (what a sneaking click on the sigil does). */
    public static ArcCharacter cycle(ServerPlayer p) {
        MinecraftServer server = p.getServer();
        if (server == null) return null;
        ArcArenaSave save = ArcArenaSave.get(server);
        ArcCharacter now = pickOf(p);
        String next = ChroniclePick.cycle(rows(p), now == null ? save.pick(p.getUUID()) : now.id(), ArcWorldConfig.arenaReplay());
        if (next == null) {
            p.displayClientMessage(Component.translatableWithFallback("message.nusmp.arc.chronicle.nobody", "The Chronicle has nobody to carry you to.").withStyle(ChatFormatting.GRAY), true);
            return null;
        }
        save.pick(p.getUUID(), next);
        ArcCharacter c = ArcRoster.byId(next);
        if (c != null) p.displayClientMessage(Component.translatableWithFallback("message.nusmp.arc.chronicle.picked_short", "The sigil now carries you to %s.", c.fullName()).withStyle(ChatFormatting.GOLD), true);
        return c;
    }

    // ---------------------------------------------------------------- the click
    private static void onUse(PlayerInteractEvent.RightClickBlock e) {
        try {
            if (e.getLevel().isClientSide() || e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer sp)) return;
            if (!Labyrinth.inSanctuary(e.getLevel()) || !e.getLevel().getBlockState(e.getPos()).is(Blocks.LECTERN)) return;
            if (e.getPos().getX() != ChronicleSite.LECTERN_X || e.getPos().getZ() != ChronicleSite.LECTERN_Z || e.getPos().getY() != ChronicleSite.PAVE_Y + 1) return;
            e.setCanceled(true);
            e.setCancellationResult(InteractionResult.SUCCESS);
            if (!ArcWorldConfig.arenas()) {
                sp.displayClientMessage(Component.translatableWithFallback("message.nusmp.arc.chronicle.off", "The Chronicle is closed.").withStyle(ChatFormatting.GRAY), true);
                return;
            }
            List<ChronicleView.Page> pages = ChronicleView.pages(rows(sp));
            show(sp, ChronicleView.firstPage(pages, ArcStory.sagaOf(sp)));
        } catch (RuntimeException ex) {
            LOG.error("[nusmp] the Chronicle lectern failed: {}", ex.toString());
        }
    }

    // ---------------------------------------------------------------- the page
    /** Shows one saga's page as chat lines. */
    public static void show(ServerPlayer p, int pageIndex) {
        List<ChronicleView.Row> rows = rows(p);
        List<ChronicleView.Page> pages = ChronicleView.pages(rows);
        if (pages.isEmpty()) {
            p.sendSystemMessage(Component.translatableWithFallback("message.nusmp.arc.chronicle.empty", "The Chronicle is blank: no boss is written in it yet.").withStyle(ChatFormatting.GRAY));
            return;
        }
        int idx = Math.max(0, Math.min(pages.size() - 1, pageIndex));
        ChronicleView.Page page = pages.get(idx);
        ArcCharacter picked = pickOf(p);
        p.sendSystemMessage(Component.literal("== " + page.saga().title() + "  (" + page.defeated() + "/" + page.rows().size() + (page.cleared() ? ", cleared" : "") + ") ==")
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        for (ChronicleView.Row r : page.rows()) p.sendSystemMessage(line(r, picked != null && picked.id().equals(r.id()), ArcWorldConfig.arenaReplay()));
        MutableComponent foot = Component.literal(picked == null ? "The sigil has nobody to carry you to. " : "The sigil carries you to " + picked.name() + ". ").withStyle(ChatFormatting.GRAY);
        if (pages.size() > 1) {
            int next = ChronicleView.nextPage(pages, idx);
            foot.append(Component.literal("[ " + pages.get(next).saga().title() + " ]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/multiverse arc chronicle page " + next))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("Turn to the next saga")))));
        }
        p.sendSystemMessage(foot);
    }

    private static Component line(ChronicleView.Row r, boolean picked, boolean replay) {
        ChatFormatting colour = switch (r.state()) {
            case DEFEATED -> ChatFormatting.GREEN;
            case OPEN -> ChatFormatting.YELLOW;
            case LOCKED -> ChatFormatting.DARK_GRAY;
        };
        String mark = switch (r.state()) {
            case DEFEATED -> "[done] ";
            case OPEN -> "[open] ";
            case LOCKED -> "[locked] ";
        };
        MutableComponent out = Component.literal((picked ? "> " : "  ") + mark + r.name()).withStyle(colour);
        if (r.title() != null && !r.title().isEmpty()) out.append(Component.literal(", " + r.title()).withStyle(ChatFormatting.GRAY));
        if (r.state() == ChronicleView.State.LOCKED && !r.missing().isEmpty()) {
            out.append(Component.literal("  after " + String.join(", ", r.missing())).withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        }
        if (r.selectable(replay) && !picked) {
            out.append(Component.literal("  [ choose ]").withStyle(s -> s.withColor(ChatFormatting.AQUA).withBold(true)
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/multiverse arc chronicle pick " + r.id()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("The Chronicle Sigil will carry you to " + r.name())))));
        }
        return out;
    }

    /** The page number of a saga for the command (clamped by {@link #show}). */
    public static int pageOf(ServerPlayer p, ArcSaga saga) { return ChronicleView.firstPage(ChronicleView.pages(rows(p)), saga); }
}
