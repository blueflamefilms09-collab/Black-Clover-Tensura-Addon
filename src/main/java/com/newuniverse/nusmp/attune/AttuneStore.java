package com.newuniverse.nusmp.attune;

import com.mojang.serialization.Codec;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: what a player has attuned is saved with the player (a NeoForge attachment, keyed by the player like every other attachment, copied to the new body on death: meditation is of the mind). The tag
 * format is hand written and clamped: at most {@link AttuneRules#MAX_SLOTS} ids, each a valid id, so a damaged or edited save can never hand the rules something they could not make. Nothing is
 * written for a player who never meditated. Register {@link #ATTACHMENTS} on the mod bus.
 */
public final class AttuneStore {
    private AttuneStore() {}

    /** The saved part: the attuned power ids in slot order and the game time of the last change. Server thread only. */
    public static final class State {
        public final List<String> slots = new ArrayList<>(AttuneRules.MAX_SLOTS);
        public long lastChange;

        public boolean isEmpty() { return slots.isEmpty() && lastChange == 0; }

        public CompoundTag toTag() {
            CompoundTag t = new CompoundTag();
            ListTag l = new ListTag();
            for (String s : slots) l.add(StringTag.valueOf(s));
            t.put("slots", l);
            t.putLong("last", lastChange);
            return t;
        }

        public static State fromTag(CompoundTag t) {
            State s = new State();
            ListTag l = t.getList("slots", Tag.TAG_STRING);
            for (int i = 0; i < l.size() && s.slots.size() < AttuneRules.MAX_SLOTS; i++) {
                String id = l.getString(i);
                if (AttuneRules.validId(id) && !s.slots.contains(id)) s.slots.add(id);
            }
            s.lastChange = Math.max(0L, t.getLong("last"));
            return s;
        }
    }

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, NUSMP.MODID);

    public static final Codec<State> CODEC = CompoundTag.CODEC.xmap(State::fromTag, State::toTag);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<State>> STATE = ATTACHMENTS.register("attunement",
            () -> AttachmentType.builder(State::new).serialize(CODEC, s -> !s.isEmpty()).copyOnDeath().build());

    /** The state, created empty if there is none (use for writing). */
    public static State of(Player p) { return p.getData(STATE); }

    /** The state if the player has one, else null (use for reading: never creates one). */
    public static State peek(Player p) { return p.hasData(STATE) ? p.getData(STATE) : null; }
}
