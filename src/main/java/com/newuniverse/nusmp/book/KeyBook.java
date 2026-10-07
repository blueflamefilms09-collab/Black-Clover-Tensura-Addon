package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.vfx.VfxShape;
import io.github.manasmods.manascore.network.api.util.Changeable;
import io.github.manasmods.manascore.skill.api.ManasSkillInstance;
import io.github.manasmods.tensura.damage.TensuraDamageTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;

/**
 * 0.54 Key Magic: lock, unlock and open doors. Starters throw keys, the mids lock a mage, seal a body behind a keyhole or step through a
 * door, the zones close a room, the signature opens Janus Abigail's magical space and the daily is Doom's Gate. See KeyArts.
 */
public class KeyBook extends GrimoireBook {
    static final int COLOR = 0xFFFFD04A;

    private final List<BookPage> pages = List.of(
            BookPage.starter("key_strike", "Key Strike", ElementBook.bolt(8f, 1.6, 0.5, 14, true, 0, VfxShape.KEY_FX1, null, ElementBook.NONE)),
            BookPage.starter("skeleton_keys", "Skeleton Keys", ElementBook.volley(3, 4f, 1.5, true, VfxShape.KEY_FX1,
                    ElementBook.effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0)))),
            BookPage.mid("janus_baptism", "Janus Baptism", KeyArts::janusBaptism),
            BookPage.mid("keyhole_prison", "Keyhole Prison", KeyArts::keyholePrison),
            BookPage.mid("door_step", "Door Step", KeyArts::doorStep).withAnim("out"),
            BookPage.mid("master_key", "Master Key", KeyArts::masterKey),
            BookPage.mid("bolted_door", "Bolted Door", KeyArts::boltedDoor),
            BookPage.mid("unlock", "Unlock", KeyArts::unlock),
            BookPage.zone("locked_domain", "Locked Domain", KeyArts::lockedDomain),
            BookPage.zone("key_armory", "Key Armory", KeyArts::keyArmory),
            BookPage.signature("janus_abigail", "Janus Abigail", KeyArts::janusAbigail).withAnim("signature"),
            BookPage.daily("doom_s_gate", "Doom's Gate", KeyArts::doomsGate).withCooldown(24000).withAnim("signature"),
            // 0.54: summoned models and a render layer (KeySummons, prop.KeyProps, client.prop.KeyPropPainter, client.aura.KeyAura)
            BookPage.zone("gate_of_keys", "Gate of Keys", KeySummons::gateOfKeys).withAnim("out"),
            BookPage.zone("key_guard", "Key Guard", KeySummons::keyGuard).withCooldown(400).withAnim("up"));

    public KeyBook() { super(MagicType.KEY, COLOR); }
    @Override protected List<BookPage> familyPages() { return pages; }
    @Override public ResourceKey<DamageType> damageType() { return TensuraDamageTypes.MAGIC_GENERIC; }

    /** Master Key: melee hits add damage and lock the foe's legs and magic for 1.5 s. */
    @Override
    public boolean onDamageEntity(ManasSkillInstance i, LivingEntity owner, LivingEntity target, DamageSource source, Changeable<Float> amount) {
        if (source.getDirectEntity() == owner && owner.level().getGameTime() < i.getOrCreateTag().getLong(KeyArts.MASTER)) {
            amount.set(amount.get() + BalanceLaw.damage(target, 4f, masteryFrac(i)));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            EnergyBridge.effect(target, "silence", 30, 0);
        }
        return true;
    }
}
