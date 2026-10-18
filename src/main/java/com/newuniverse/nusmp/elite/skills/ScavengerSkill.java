package com.newuniverse.nusmp.elite.skills;

import com.newuniverse.nusmp.elite.skills.rules.ExtraRules;
import com.newuniverse.nusmp.skill.EliteBridge;
import com.newuniverse.nusmp.skill.SkillMsg;
import com.newuniverse.nusmp.skill.SkillText;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Scavenger (Extra, designed for this mod; the original addon's source was not available): nothing is wasted. PASSIVE: a foe you kill sometimes leaves more behind: with a 20% chance (35%
 * mastered) one random stack of its drops is dropped a second time (never more than one extra stack per kill, never from a player). PRESS: Sniff Out makes every dropped item within 32
 * blocks (48 mastered) glow for twenty seconds and tells you how many there are and how far the nearest is. The drop handler is in {@link EliteSkillEvents}; the config key
 * {@code scavengerDrops} switches the duplication off. The relic "Mantle of the Scavenger" is a different, older thing and is untouched. Numbers: {@link ExtraRules.Scavenger}.
 */
public class ScavengerSkill extends EliteSkill {
    static final SkillText TEXT = SkillText.of("scavenger", "Scavenger",
            "Extra. Your kills sometimes leave one more stack of loot. Press to Sniff Out every dropped item around you: they glow for twenty seconds.");

    public ScavengerSkill() { super(SkillType.EXTRA, TEXT, false); }

    @Override
    protected List<EliteMode> modes() {
        return List.of(EliteMode.of("scavenger.sniff", "Sniff Out", ExtraRules.Scavenger.SNIFF_COST, ExtraRules.Scavenger.SNIFF_COOLDOWN));
    }

    @Override protected int color() { return 0xFFB8A070; }

    @Override
    protected boolean use(ManasSkillInstance instance, ServerPlayer player, int mode) {
        ServerLevel level = player.serverLevel();
        double r = ExtraRules.Scavenger.sniffRadius(masteredBy(instance, player));
        Vec3 here = player.position();
        List<ItemEntity> found = new ArrayList<>(level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(r), e -> e.isAlive() && e.distanceToSqr(here) <= r * r));
        if (found.isEmpty()) {
            EliteBridge.fail(player, SkillMsg.t("scavenger.nothing", "Nothing lies around to sniff out."));
            return false;
        }
        found.sort(Comparator.comparingDouble(e -> e.distanceToSqr(here)));
        if (found.size() > ExtraRules.Scavenger.SNIFF_MAX_ITEMS) found = new ArrayList<>(found.subList(0, ExtraRules.Scavenger.SNIFF_MAX_ITEMS));
        List<UUID> glowing = new ArrayList<>();
        for (ItemEntity e : found) {
            if (!e.hasGlowingTag()) {                                     // only items this skill lit are unlit again
                e.setGlowingTag(true);
                glowing.add(e.getUUID());
            }
        }
        // the glow ends after twenty seconds: a two-pulse zone whose second pulse puts the light out
        EliteBridge.startZone(player, ExtraRules.Scavenger.SNIFF_TICKS, 2, (owner, lvl, index) -> {
            if (index == 0) return;
            for (UUID id : glowing) {
                if (lvl.getEntity(id) instanceof ItemEntity item) item.setGlowingTag(false);
            }
        });
        EliteBridge.sound(level, here, SoundEvents.FOX_SNIFF, 1.0f, 0.9f);
        EliteBridge.aura(level, player, false);
        EliteBridge.actionbar(player, SkillMsg.t("scavenger.sniffed", "You sniff out %s items; the nearest is %s blocks away.", found.size(),
                (int) Math.round(Math.sqrt(found.get(0).distanceToSqr(here)))).withStyle(ChatFormatting.GOLD));
        return true;
    }

    /** A kill by the owner: maybe one more stack of the drops. Called by the elite drop handler; {@code drops} is the live drop collection. */
    static void duplicate(ServerPlayer killer, LivingEntity victim, Collection<ItemEntity> drops, boolean mastered) {
        if (!EliteSkillsConfig.get(EliteSkillsConfig.SCAVENGER_DROPS) || victim instanceof Player || drops.isEmpty()) return;
        if (ExtraRules.Scavenger.extraStacks(ExtraRules.Scavenger.duplicates(killer.getRandom().nextDouble(), mastered), drops.size()) <= 0) return;
        List<ItemEntity> list = new ArrayList<>(drops);
        ItemEntity pick = list.get(killer.getRandom().nextInt(list.size()));
        ItemStack stack = pick.getItem();
        if (stack.isEmpty()) return;
        ItemEntity copy = new ItemEntity(victim.level(), pick.getX(), pick.getY(), pick.getZ(), stack.copy());
        copy.setDefaultPickUpDelay();
        drops.add(copy);
        EliteBridge.spray(killer.serverLevel(), false, pick.position().add(0, 0.4, 0), 6, 0.3, 0.3, 0.3, 0.03);
    }
}
