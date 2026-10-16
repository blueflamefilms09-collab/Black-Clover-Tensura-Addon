# Nacht Faust: Shadow Travel and the four Devil Unite modes (1.43.0)

Nothing here was run in game. Pure logic is unit-tested (ShadowLightTest, ShadowSearchTest, UniteModesTest); Minecraft-facing code was type-checked against API stubs only.

## NEW
- Pages appended to the Shadow book: `shadow_travel`, `unite_equus`, `unite_felis`, `unite_gallus_call` (the call needs a page of its own because the old Gallus page has a 45 s cooldown and cannot be "used twice").
- `shadow/*`: ShadowLight (light rule), ShadowSearch (bounded candidate order), ShadowWorld/ShadowTravel (cast), UniteKind (stat table), UniteSlot/UniteState (mode bookkeeping, stats, overlay), UniteWatcher, UniteSignature, UniteEvents, UniteAbilities, ShadowSight, ShadowPackProps, DamageClass, CallMath, ShadowConfig, ShadowMana, ShadowDamage.
- Overlays for all four modes: 20 GeoLite models (`assets/nusmp/geo/devils`), built by `tools/gen_devil_assets.py`, drawn by `client/shadow/DevilOverlay` from `ModeArmorLayer`. The pack wolf is `PropKind.SHADOW_PACK_1` (`client/shadow/ShadowPackPainter`).
- `ModeArmor.Mode` appended: DEVIL_EQUUS, DEVIL_CANIS, DEVIL_FELIS, DEVIL_GALLUS.
## REPLACEMENT: none. KEPT: every old page and overlay; WikiSpells untouched.
## Changed shared files (appends only): PropKind, PropRegistry, PropPainterRegistry, ModeArmor, ModeArmorLayer (one switch case), ElementBooks.shadow(), tools/mode_preview (stubs fixed, devil modes added).

## How it works
- Shadow Travel: shadow = max(block light, sky light - skyDarken) <= `maxLight` (3) at feet and head. Caster must stand in shadow (or be in a Unite mode). Target = aimed block face, else nearest shadow in a cone via at most `searchCap` (400) checks. Range 48, extra mana 0.1% of max per block (cap 8%). Refused when sealed, rooted (Slowness VII+), riding. Teleport never changes blocks, so the Dungeon guard has nothing to refuse. Cooldown 120 ticks (before the global cooldown multiplier).
- Old pages Canis/Gallus/Canis x Felis have no state of their own: `UniteWatcher` recognises the fresh effect set they apply (`UniteSignature`) and starts the mode (overlay + stats; Canis also the pack and spikes). Canis x Felis shows the Canis armour. Config `extrasOnOldPages=false` leaves them plain buffs with only the overlay.
- Equus: +10 armor, +6 toughness, 40% off physical damage (`DamageClass`), knockback immune, -25% speed. Canis: +20% speed, +25% attack, 3-5 shadow wolves (EP scaled), spikes every 2 s on up to 4 enemies within 6. Felis: +30% speed, jump, night vision, 20% dodge (10% vs bosses), non-boss mobs beyond 8 blocks drop you, outline of living things within 24 for the wearer only (data packet setting the glow bit). Gallus: call and root through BalanceLaw.beginControl/controlTicks.
- Unite Gallus also gets +15% speed, jump, safe fall.

## Config (`nusmp-shadow-server.toml`)
shadowTravel.maxLight, travelRange, searchCap, followers, manaPerBlock, requireShadow; uniteModes.overlays, extrasOnOldPages, dodgeVsBoss, felisSight, packMax.

## Unverified / not done
- Players do not see Felis "faint" (needs a skin render change); only mobs lose track.
- Outline packet trick (entity data id 0 glow bit) and the translucent grow-in of GeoLite pieces are untested in game. Animation clips (tail, wings, wolf) are not previewed; rotation signs may need flipping.
- "Shadow of own cast" is read as "in a Unite mode".
- The old Java mode preview was already broken (relic stubs missing); its stubs were completed and it renders again.
