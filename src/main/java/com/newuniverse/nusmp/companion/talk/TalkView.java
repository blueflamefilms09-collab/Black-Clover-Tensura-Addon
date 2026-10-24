package com.newuniverse.nusmp.companion.talk;

import com.newuniverse.nusmp.companion.CompanionRules;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Why: one screen of a conversation, built by the SERVER and sent whole to the client: who is speaking (a name, a line under it, the colour of the frame), what she says, and the choices on the four tabs (Talk,
 * Interact, Bond, Pouch). Every text is a {@link Component} (a lang key and its arguments), so a dedicated server needs no lang file and each player reads their own language. The client can only send back the id
 * of a choice that is on this view. Limits ({@link TalkRules}): an id is at most 40 characters, a view has at most 40 options, ids beginning {@code k:} / {@code g:} / {@code x:} are reserved. A choice that cannot
 * be taken now is an {@link Option#blocked} one with the reason. The bond system also puts a small status on the view (rapport, the stance, whether she may use the pouch, the nine pouch slots) for the Bond and
 * Pouch tabs to draw. A closed view ({@link #closed()}) ends the conversation.
 */
public final class TalkView {
    /** One choice. {@code blockedReason} is null for a choice that can be taken. */
    public record Option(String id, Component label, int tab, Component blockedReason) {
        public boolean blocked() { return blockedReason != null; }

        public static Option of(String id, Component label, int tab) { return new Option(id, label, tab, null); }

        public static Option blocked(String id, Component label, int tab, Component reason) { return new Option(id, label, tab, reason == null ? Component.empty() : reason); }
    }

    /** "Not shown" for the three status numbers. */
    public static final int NONE = -1;
    public static final int TALK = TalkRules.TALK, INTERACT = TalkRules.INTERACT, BOND = TalkRules.BOND, POUCH = TalkRules.POUCH;

    private static final TalkView CLOSED = new TalkView(Component.empty(), Component.empty(), 0xFFFFFF, Component.empty(), List.of(), NONE, NONE, NONE, List.of(), true);

    public final Component name, sub, text;
    public final int color;
    public final List<Option> options;
    /** Rapport 0..99 (or {@link #NONE}), the stance ordinal (or NONE: not offered), 1/0 for the pouch use switch (or NONE), and the pouch contents (empty or nine stacks). */
    public final int rapport, stance, useIt;
    public final List<ItemStack> pouch;
    private final boolean closed;

    private TalkView(Component name, Component sub, int color, Component text, List<Option> options, int rapport, int stance, int useIt, List<ItemStack> pouch, boolean closed) {
        this.name = name;
        this.sub = sub;
        this.color = color & 0xFFFFFF;
        this.text = text;
        this.options = options;
        this.rapport = rapport;
        this.stance = stance;
        this.useIt = useIt;
        this.pouch = pouch;
        this.closed = closed;
    }

    public boolean isClosed() { return closed; }

    /** The view that ends the conversation. */
    public static TalkView closed() { return CLOSED; }

    /** The ids of this view's options (what the server will accept back). */
    public Set<String> ids() {
        Set<String> out = new HashSet<>();
        for (Option o : options) out.add(o.id());
        return out;
    }

    public static Builder builder(Component name, Component sub, int color) { return new Builder(name, sub, color); }

    // ================================================================ builder
    public static final class Builder {
        private final Component name, sub;
        private final int color;
        private Component text = Component.empty();
        private final List<Option> options = new ArrayList<>();
        private final Set<String> seen = new HashSet<>();
        private int rapport = NONE, stance = NONE, useIt = NONE;
        private List<ItemStack> pouch = List.of();

        private Builder(Component name, Component sub, int color) {
            this.name = name == null ? Component.empty() : name;
            this.sub = sub == null ? Component.empty() : sub;
            this.color = color;
        }

        public Builder text(Component t) {
            text = t == null ? Component.empty() : t;
            return this;
        }

        /** Adds a choice of the companion's own (the id must not begin with k:, g: or x:). */
        public Builder add(String id, Component label, int tab) { return put(Option.of(id, label, tab), false); }

        /** Adds a choice that cannot be taken now, with the reason. */
        public Builder addBlocked(String id, Component label, int tab, Component reason) { return put(Option.blocked(id, label, tab, reason), false); }

        /** Adds the button of the companion's own panel (the id {@code g:panel}, on the Interact tab); see {@code Panelable}. */
        public Builder panel(Component label, Component blockedReason) {
            return put(blockedReason == null ? Option.of("g:panel", label, INTERACT) : Option.blocked("g:panel", label, INTERACT, blockedReason), true);
        }

        /** For the bond system only (the reserved {@code k:} ids). */
        public Builder addSystem(Option o) { return put(o, true); }

        public Builder status(int rapport, int stance, int useIt, List<ItemStack> pouch) {
            this.rapport = rapport < 0 ? NONE : Math.min(rapport, 99);
            this.stance = stance < 0 ? NONE : stance;
            this.useIt = useIt < 0 ? NONE : (useIt > 0 ? 1 : 0);
            this.pouch = pouch == null ? List.of() : List.copyOf(pouch);
            return this;
        }

        private Builder put(Option o, boolean system) {
            String problem = TalkRules.idProblem(o.id(), system);
            if (problem != null) throw new IllegalArgumentException(problem);
            if (options.size() >= TalkRules.MAX_OPTIONS) throw new IllegalArgumentException("a talk view has at most " + TalkRules.MAX_OPTIONS + " options");
            if (!seen.add(o.id())) throw new IllegalArgumentException("two options with the id " + o.id());
            options.add(new Option(o.id(), o.label() == null ? Component.empty() : o.label(), TalkRules.cleanTab(o.tab()), o.blockedReason()));
            return this;
        }

        public TalkView build() {
            List<ItemStack> slots = pouch.size() > CompanionRules.POUCH_SLOTS ? pouch.subList(0, CompanionRules.POUCH_SLOTS) : pouch;
            return new TalkView(name, sub, color, text, List.copyOf(options), rapport, stance, useIt, List.copyOf(slots), false);
        }
    }

    // ================================================================ wire form
    public static final StreamCodec<RegistryFriendlyByteBuf, TalkView> STREAM_CODEC = StreamCodec.of(TalkView::write, TalkView::read);

    private static void write(RegistryFriendlyByteBuf b, TalkView v) {
        ComponentSerialization.STREAM_CODEC.encode(b, v.name);
        ComponentSerialization.STREAM_CODEC.encode(b, v.sub);
        b.writeInt(v.color);
        ComponentSerialization.STREAM_CODEC.encode(b, v.text);
        b.writeVarInt(v.options.size());
        for (Option o : v.options) {
            b.writeUtf(o.id(), TalkRules.WIRE_ID);
            ComponentSerialization.STREAM_CODEC.encode(b, o.label());
            b.writeByte(o.tab());
            b.writeBoolean(o.blocked());
            if (o.blocked()) ComponentSerialization.STREAM_CODEC.encode(b, o.blockedReason());
        }
        b.writeByte(v.rapport + 1);
        b.writeByte(v.stance + 1);
        b.writeByte(v.useIt + 1);
        b.writeVarInt(v.pouch.size());
        for (ItemStack s : v.pouch) ItemStack.OPTIONAL_STREAM_CODEC.encode(b, s);
    }

    private static TalkView read(RegistryFriendlyByteBuf b) {
        Component name = ComponentSerialization.STREAM_CODEC.decode(b);
        Component sub = ComponentSerialization.STREAM_CODEC.decode(b);
        int color = b.readInt();
        Component text = ComponentSerialization.STREAM_CODEC.decode(b);
        int n = b.readVarInt();
        if (n < 0 || n > TalkRules.MAX_OPTIONS) throw new IllegalArgumentException("a talk view has at most " + TalkRules.MAX_OPTIONS + " options, got " + n);
        List<Option> options = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            String id = b.readUtf(TalkRules.WIRE_ID);
            Component label = ComponentSerialization.STREAM_CODEC.decode(b);
            int tab = TalkRules.cleanTab(b.readByte());
            Component reason = b.readBoolean() ? ComponentSerialization.STREAM_CODEC.decode(b) : null;
            options.add(new Option(id, label, tab, reason));
        }
        int rapport = b.readUnsignedByte() - 1, stance = b.readUnsignedByte() - 1, useIt = b.readUnsignedByte() - 1;
        int slots = b.readVarInt();
        if (slots < 0 || slots > CompanionRules.POUCH_SLOTS) throw new IllegalArgumentException("a pouch has at most " + CompanionRules.POUCH_SLOTS + " slots, got " + slots);
        List<ItemStack> pouch = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) pouch.add(ItemStack.OPTIONAL_STREAM_CODEC.decode(b));
        return new TalkView(name, sub, color, text, List.copyOf(options), rapport < 0 ? NONE : rapport, stance < 0 ? NONE : stance, useIt < 0 ? NONE : useIt, List.copyOf(pouch), false);
    }
}
