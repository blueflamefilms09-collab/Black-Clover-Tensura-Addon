package com.newuniverse.nusmp.book;

import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * Guide P2 #6: the hook every feature that follows a grimoire cast listens to, instead of {@code GrimoireBook.castPage} calling about twenty unrelated features directly.
 * Posted on the game bus ({@code NeoForge.EVENT_BUS}), on the server thread, only for a player (SERVER setting guide_fixes.grimoireCastEvent; off = no event, the old direct calls).
 * <ul>
 * <li>{@link Pre}: every check passed (sealed, cooldown, recovering), nothing paid yet. Cancel it to refuse the cast: nothing is paid, no cooldown starts, no mastery is earned.</li>
 * <li>{@link Post}: the cast ran (the spell, the cooldown, the mastery point are done). Not cancellable. {@link GrimoireCastListeners} is the built-in listener.</li>
 * </ul>
 * Listeners must be quick (they run inside the cast, once per cast) and must not keep the player or the instance.
 */
public abstract class GrimoireCastEvent extends Event {
    private final GrimoireBook book;
    private final ServerPlayer player;
    private final ManasSkillInstance instance;
    private final BookPage page;
    private final int mode;

    protected GrimoireCastEvent(GrimoireBook book, ServerPlayer player, ManasSkillInstance instance, BookPage page, int mode) {
        this.book = book;
        this.player = player;
        this.instance = instance;
        this.page = page;
        this.mode = mode;
    }

    public GrimoireBook book() { return book; }
    public ServerPlayer player() { return player; }
    public ManasSkillInstance instance() { return instance; }
    public BookPage page() { return page; }
    public int mode() { return mode; }

    /** Before the cast is paid. Cancelling refuses it (nothing is paid or started). */
    public static class Pre extends GrimoireCastEvent implements ICancellableEvent {
        public Pre(GrimoireBook book, ServerPlayer player, ManasSkillInstance instance, BookPage page, int mode) {
            super(book, player, instance, page, mode);
        }
    }

    /** After the cast ran. */
    public static class Post extends GrimoireCastEvent {
        public Post(GrimoireBook book, ServerPlayer player, ManasSkillInstance instance, BookPage page, int mode) {
            super(book, player, instance, page, mode);
        }
    }
}
