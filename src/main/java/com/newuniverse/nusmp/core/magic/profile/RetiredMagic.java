package com.newuniverse.nusmp.core.magic.profile;

import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.BookPage;
import com.newuniverse.nusmp.book.GrimoireBook;
import io.github.manasmods.manascore.skill.api.ManasSkill;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.manascore.skill.api.Skills;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: until 1.92.0 a player who held the grimoires of two attributes that have no hybrid of their own was automatically given a third, "custom" (merged) magic made of the two. The owner removed that
 * feature (1.93.0): two attributes now simply stay two attributes, each with its own grimoire, pages, effects and armour. What cannot simply vanish is what old worlds saved: the 64 skills that
 * cast the merged magics ({@code nusmp:merged_00} .. {@code merged_63}) may sit in a player's saved skill list, and a skill id the registry no longer knows is dropped (or logged) by the skill
 * system on load. So the 64 ids stay registered, as inert {@link RetiredBook}s with no pages, and a player who still holds one has it forgotten at login. The merge generator itself (definitions,
 * commands, syncing, the combined armour and effects) is gone. Nothing of any parent magic was ever altered by it.
 */
public final class RetiredMagic {
    private RetiredMagic() {}

    /** How many merged skills the old feature registered. */
    public static final int SLOTS = 64;

    private static final List<DeferredHolder<ManasSkill, RetiredBook>> ALL = new ArrayList<>();

    /** "merged_07" for slot 7 (the id the old feature used, so saved skills still resolve). */
    public static String pathOf(int slot) { return String.format(java.util.Locale.ROOT, "merged_%02d", slot); }

    /** Called once from NUSkills' static initialiser with its DeferredRegister. */
    public static List<DeferredHolder<ManasSkill, RetiredBook>> registerAll(DeferredRegister<ManasSkill> skills) {
        if (ALL.isEmpty()) for (int k = 0; k < SLOTS; k++) ALL.add(skills.register(pathOf(k), RetiredBook::new));
        return List.copyOf(ALL);
    }

    public static void register(IEventBus gameBus) { gameBus.addListener(RetiredMagic::onLogin); }

    /** A player who still holds one of the retired skills (saved by an older version) has it forgotten. Never throws. */
    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || ALL.isEmpty()) return;
        try {
            Skills skills = SkillAPI.getSkillsFrom(p);
            for (DeferredHolder<ManasSkill, RetiredBook> h : ALL) {
                ResourceLocation id = h.getId();
                skills.getSkill(id).ifPresent(i -> skills.forgetSkill(i.getSkillId()));
            }
        } catch (RuntimeException | LinkageError ex) {
            SafeCast.fault("retired-magic-login", ex);
        }
    }

    /** The inert skill that holds one retired id: no pages, no grimoire, no cast. */
    public static final class RetiredBook extends GrimoireBook {
        public static final ResourceLocation ICON = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/skill/grimoire/merged.png");

        public RetiredBook() { super(MagicType.FLAME, 0xFFB8D8FF, ICON); }

        @Override protected List<BookPage> familyPages() { return List.of(); }

        @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

        @Override public String magicId() { return "retired"; }

        @Override public String magicLabel() { return "Retired magic"; }

        @Override public MutableComponent getName() { return Component.translatable("nusmp.merge.retired"); }

        @Override public MutableComponent getSkillDescription() { return Component.translatable("nusmp.merge.retired.description"); }

        @Override protected boolean holdingBook(ServerPlayer p) { return false; }

        @Override protected boolean givesGrimoireItem() { return false; }

        @Override protected boolean universalPages() { return false; }

        @Override protected MagicType summonMagic() { return null; }
    }
}
