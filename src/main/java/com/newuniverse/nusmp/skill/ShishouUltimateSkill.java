package com.newuniverse.nusmp.skill;

import com.newuniverse.nusmp.shishou.ShishouGrantService;
import com.newuniverse.nusmp.shishou.ShishouModes;
import com.newuniverse.nusmp.shishou.ShishouSkill;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * Shishou, the Teacher (Ultimate skill, 2.1.0): the talking sword's abilities as a Tensura skill, replacing the three keys he shipped with. The wielder is given it when he claims the sword and loses it when
 * the sword is no longer his ({@link ShishouGrantService}). It has {@link ShishouModes#COUNT} modes (Telekinesis, Sensory, Defence, Offence, Storage, Magic, Counsel), switched with the skill system's mode key like
 * every other multi-mode skill; the skill key activates the mode, and sneaking with it picks the next ability of a mode that holds several. All the work is the existing server-decided ability code of
 * {@code shishou/ShishouService} (the sword's own mana pays, per-ability cooldowns, caps); this class is only the registration and the names. The passive abilities need no mode: they are on once he has them.
 */
public class ShishouUltimateSkill extends NUSkill {
    static final SkillText TEXT = SkillText.of("shishou", "Shishou, the Teacher",
            "Ultimate. The talking sword's abilities as one skill: switch modes (Telekinesis, Sensory, Defence, Offence, Storage, Magic, Counsel) with the mode key, press the skill key to use the mode, sneak and press to pick the next ability of a mode. Hold him in your hand to cast; his own mana pays.");

    public ShishouUltimateSkill() { super(SkillType.ULTIMATE, TEXT, true); }

    @Override public int getModes(ManasSkillInstance instance) { return ShishouModes.COUNT; }

    @Override
    public int nextMode(LivingEntity entity, ManasSkillInstance instance, int mode, boolean reverse) { return ShishouModes.next(mode, reverse); }

    @Override
    public String getModeId(ManasSkillInstance instance, int mode) {
        ShishouModes.Mode m = ShishouModes.byIndex(mode);
        return "shishou." + (m == null ? "telekinesis" : m.id());
    }

    @Override
    public Component getModeName(ManasSkillInstance instance, int mode) {
        ShishouModes.Mode m = ShishouModes.byIndex(mode);
        if (m == null) return SkillMsg.mode("shishou.telekinesis", "Telekinesis");
        MutableComponent name = SkillMsg.mode("shishou." + m.id(), m.english());
        ShishouSkill cur = ShishouGrantService.picked(instance, mode, 100, -1L);                  // the client has no record: show the stored pick (a later press resolves it for real)
        if (cur != null && m.skills().size() > 1) name.append(": ").append(Component.translatableWithFallback(cur.langKey(), cur.english()));
        return name;
    }

    @Override
    public void onPressed(ManasSkillInstance instance, LivingEntity entity, int key, int mode) {
        if (entity instanceof ServerPlayer p && p.isAlive()) ShishouGrantService.press(p, instance, mode);
    }
}
