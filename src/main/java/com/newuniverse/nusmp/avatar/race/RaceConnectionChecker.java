package com.newuniverse.nusmp.avatar.race;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.blackclover.GrimoirePages;
import com.newuniverse.nusmp.relics.Attune;
import com.newuniverse.nusmp.skill.NUSkills;
import io.github.manasmods.manascore.skill.api.SkillAPI;
import io.github.manasmods.tensura.util.EnergyHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.List;

/**
 * Why: a race may demand things of its creator ("holds a devil contract", "has a DanMachi Falna", "carries an attuned relic") and the owner wants those
 * requirements checked on the server before a race is authorised, so a client can never pick a race it does not qualify for. The tags in the JSON map to
 * predicates through {@link ConditionEvaluator}'s registry; this class only supplies the real player behind them (a {@link ConditionEvaluator.Ctx} that reads
 * the inventory, the skills, the grimoire, the devil pact, the Faith / god skills, the relic attunement, the EP and the dimension). Every read is defensive:
 * a source that cannot be read counts as not satisfied.
 */
public final class RaceConnectionChecker {
    private RaceConnectionChecker() {}

    private static final Logger LOG = LogUtils.getLogger();

    public record Result(boolean ok, List<String> failed) {}

    public static Result check(ServerPlayer p, AvatarRace race) {
        ConditionEvaluator.Result r = ConditionEvaluator.evaluate(new PlayerCtx(p), race.connectionConditions(), LOG::warn);
        return new Result(r.ok(), r.failed());
    }

    /** The skills whose owner counts as holding a DanMachi Falna: a god's blessing or the Faith skill (this mod has no other Falna record). */
    private static List<ResourceLocation> falnaSkills() {
        return List.of(NUSkills.HESTIA.getId(), NUSkills.FREYA.getId(), NUSkills.HEPHAESTUS.getId(), NUSkills.LOKI.getId(), NUSkills.FAITH.getId());
    }

    private record PlayerCtx(ServerPlayer p) implements ConditionEvaluator.Ctx {
        @Override public boolean hasItem(String itemId) {
            ResourceLocation id = ResourceLocation.tryParse(itemId);
            if (id == null) return false;
            var item = BuiltInRegistries.ITEM.get(id);
            for (ItemStack s : p.getInventory().items) if (s.is(item)) return true;
            for (ItemStack s : p.getInventory().armor) if (s.is(item)) return true;
            for (ItemStack s : p.getInventory().offhand) if (s.is(item)) return true;
            return false;
        }

        @Override public boolean hasSkill(String skillId) {
            ResourceLocation id = ResourceLocation.tryParse(skillId);
            return id != null && SkillAPI.getSkillsFrom(p).getSkill(id).isPresent();
        }

        @Override public boolean hasGrimoire() { return GrimoirePages.grimoireOf(p).isPresent(); }

        @Override public boolean hasMagic(String magicId) {
            return GrimoirePages.grimoireOf(p).map(i -> {
                var m = GrimoirePages.magicOf(i);
                return m != null && m.name().equalsIgnoreCase(magicId);
            }).orElse(false);
        }

        @Override public boolean hasDevilContract() { return com.newuniverse.nusmp.book.ForbiddenMagic.devilOf(p) != null; }

        @Override public boolean hasFalna() {
            for (ResourceLocation id : falnaSkills()) if (SkillAPI.getSkillsFrom(p).getSkill(id).isPresent()) return true;
            return false;
        }

        @Override public boolean hasAttunedRelic() { return Attune.carried(p, ItemStack.EMPTY) >= 1; }

        @Override public double ep() { return EnergyHelper.getMaxEP(p); }

        @Override public String dimension() { return p.level().dimension().location().toString(); }
    }
}
