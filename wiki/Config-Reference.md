# Complete settings reference

[Wiki home](Home.md) · RC1

Look up any of the 128 settings here: 60 server and 68 client. For mode explanations and copyable examples, read [Configuration](Configuration.md#where-files-live).

## Contents

- [Server: gameplay and ownership](#server-gameplay-and-ownership)
- [Server: pressure and protection](#server-pressure-and-protection)
- [Server: ascent and recovery](#server-ascent-and-recovery)
- [Server sediment](#server-sediment)
- [Client HUD and condition effects](#client-hud-and-condition-effects)
- [Client audio and bubbles](#client-audio-and-bubbles)
- [Client ambience and particles](#client-ambience-and-particles)
- [Client lamps](#client-lamps)

## Server: gameplay and ownership

Exact TOML keys, defaults, accepted values and interactions. Use the active `deepdiving-server.toml`; see the configuration guide for global/world-file precedence.

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `difficulty` | `STANDARD` | ARCADE / STANDARD / HARDCORE | Condition-severity preset. ARCADE uses 0.45× severity and 0.65× managed gas use; STANDARD uses 1×/1×; HARDCORE uses 1.6× severity and 1× gas use. It does not rewrite pressure, ascent, equipment ratings or tissue settings. |
| `enabled` | `true` | true / false | Master switch for Deep Diving player simulation and features gated by it. Set false when another mod should own the diving simulation; items and recipes remain installed. This also restores ordinary Create helmet visibility behavior. |
| `physiology` | `true` | true / false | Enables tissue updates, oxygen reserve, toxicity, narcosis and decompression stress. False clears active physiology symptoms/readouts but leaves gas use and equipment-pressure handling separate; it is not an unlimited-air switch. It does not act as the operator reset command for saved tissues. |
| `equipmentPressureDamage` | `true` | true / false | Lets Deep Diving apply injury and damage above the worn equipment rating, including abrupt compression. False disables this damage path, while gas/physiology and the equipment-rating warning remain independent. |
| `manageCreateBacktanks` | `true` | true / false | True replaces Create underwater air use with one pressure-scaled drain. False leaves Create backtank consumption with Create. Deep Diving cylinders are still managed; this does not hand their consumption to Create. |
| `gasPerSecondAtSurface` | `8.0` | 0.1 to 100 | Base cylinder drain in mB per simulated second at 1 atm. Multiplied by ambient pressure and the Arcade gas factor. Does not change capacity, machine production or Create backtank air units. |
| `waterBreathingBypass` | `true` | true / false | True allows Water Breathing/Conduit Power to bypass compressed-gas use. Previous tissue loading still needs recovery. False removes this Deep Diving bypass; the shallow surface-snorkel system is separate. |
| `damageMultiplier` | `1.0` | 0 to 10 | Scales damage dealt by this mod's physiology and pressure paths. Zero removes those damage amounts but does not remove conditions, warnings, movement impairment, gas drain, vanilla drowning or other mods' damage. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `createHelmetNightVision` | `false` | true / false | True allows Create helmets to extend underwater fog visibility. False keeps ordinary water fog. This is Create visibility enhancement, not a Night Vision potion, and does not turn lamps or potion effects off. |
| `breathingBubbles` | `true` | true / false | Server permission for equipment-exhaust particles. Each client must also enable acoustics.exhaustBubbles. Does not add gas consumption or change breathing sound volume. |
| `pressureBloodParticles` | `true` | true / false | Server switch for shared pressure-injury particles. Separate from the client pressureBloodOverlay; neither switch changes the injury calculation. |
| `deepSeasCompartmentSupport` | `true` | true / false | Attempts optional dry Create Deep Seas compartment detection. Recognized dry interiors use surface-pressure breathing air in this game model. It does not change submarine oxygen stores, hull ratings or existing tissue loading. |
| `autoAvoidDuplicatePressureDamage` | `true` | true / false | True automatically suppresses this mod's equipment-pressure damage when Crush Depth is detected. It does not disable gas or the rest of physiology, and does not configure Crush Depth. |
| `externalPressureOwners` | `[]` | Installed mod-ID strings | List of installed mod IDs that should own player pressure damage. If any listed ID is actually loaded, Deep Diving suppresses its equipment-pressure damage. Display names and IDs for absent mods do not activate ownership; this list works independently of automatic Crush Depth detection. |

## Server: pressure and protection

Exact TOML keys, defaults, accepted values and interactions. Use the active `deepdiving-server.toml`; see the configuration guide for global/world-file precedence.

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `pressureMode` | `LINEAR` | LINEAR / REFERENCE | LINEAR uses blocksPerAtmosphere. REFERENCE uses referenceOceanDepth, referencePressure and pressureCurveExponent. Both begin at 1 atm and cap modeled pressure at 1,000 atm. |
| `blocksPerAtmosphere` | `10.0` | 0.1 to 65536 | Blocks of additional depth for each added atmosphere in LINEAR mode. Higher values make pressure rise more slowly. Ignored by REFERENCE mode or a dimension override that supplies its own scale. |
| `referenceOceanDepth` | `64.0` | 1 to 65536 | Depth in blocks at which a REFERENCE curve reaches referencePressure. Increasing it spreads the same pressure progression across a deeper ocean. Inactive in LINEAR mode. |
| `referencePressure` | `21.0` | 1.01 to 1000 | Absolute pressure in atm at referenceOceanDepth in REFERENCE mode, including the surface atmosphere. Higher values increase pressure at a given fraction of that depth. Inactive in LINEAR mode. |
| `pressureCurveExponent` | `1.35` | 0.25 to 4 | REFERENCE curve shape: 1 is straight; above 1 gives lower pressure in the shallow portion and a steeper deep portion relative to that same straight reference curve. Below 1 does the reverse. Pressure continues beyond the reference depth. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `surfaceMode` | `LOCAL_WATER` | LOCAL_WATER / FIXED | LOCAL_WATER searches the loaded column for the local water surface. FIXED measures submerged eye depth below fixedSurfaceY. A matching dimension profile supplies its own AUTO or fixed surface choice. |
| `fixedSurfaceY` | `63` | -65536 to 65536 | World-Y coordinate of the water surface used by global FIXED mode. This is a surface coordinate, not an allowed dive depth. Ignored by global LOCAL_WATER and replaced by a matching dimension override. |
| `surfaceSearchLimit` | `2048` | 64 to 8192 | Maximum vertical blocks searched above the eyes for local surface detection, bounded by build height and loaded chunks. Samples are cached for five seconds. Higher is useful for very tall water columns but can cost more work; fixed surfaces avoid the scan. |
| `dimensionPressureProfiles` | `[]` | Six-field profile strings | Optional six-field profile strings, one dimension per entry. A matching entry replaces that dimension's whole pressure profile; the last duplicate wins. AUTO means local water detection for that override, even when the global surface mode is FIXED. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `copperBacktankPressureRating` | `6.0` | 1 to 1000 | Absolute atm protection rating of a worn Create copper backtank with a Create diving helmet. Higher permits greater equipment pressure; this does not increase gas capacity or improve its air mixture. |
| `netheriteBacktankPressureRating` | `12.0` | 1 to 1000 | Absolute atm protection rating of a worn Create netherite backtank with a Create diving helmet. Changes pressure protection, not air capacity or oxygen/nitrogen exposure. |
| `advancedCylinderPressureRating` | `12.0` | 1 to 1000 | Absolute atm protection rating of a worn Advanced cylinder with a Create diving helmet. Default 12 atm corresponds to 110 blocks on the default linear curve. |
| `highCapacityCylinderPressureRating` | `21.0` | 1 to 1000 | Absolute atm protection rating of a worn High-Capacity cylinder with a Create diving helmet. Default 21 atm corresponds to 200 blocks on the default linear curve; the gas can become unsuitable much sooner. |
| `emergencyCylinderPressureRating` | `12.0` | 1 to 1000 | Absolute atm protection rating of a worn Emergency cylinder with a Create diving helmet. Carrying it as a spare supplies gas but does not upgrade the worn chest equipment's protection. |
| `unprotectedPressureRating` | `6.0` | 1 to 1000 | Game protection threshold in absolute atm when the required rated helmet/chest combination is missing. This is deliberately forgiving; it is not a realistic bodily pressure limit. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `suddenPressureInjury` | `true` | true / false | Enables abrupt-compression shock when pressure increases enough while exceeding the equipment rating. Its path requires Deep Diving to own equipment-pressure damage. False also prevents catastrophic exposure, which requires an active shock. |
| `pressureShockDelta` | `4.0` | 0.1 to 1000 | Minimum positive pressure jump in atm between valid simulation samples for abrupt-compression shock. Higher makes sudden injury less sensitive. The diver must also be above the equipment rating. |
| `catastrophicPressureExposure` | `true` | true / false | Allows the extreme-exposure damage event when shock is active and both catastrophic thresholds are met. False removes that extreme event while leaving ordinary shock/over-rating injury available. |
| `catastrophicPressureDelta` | `12.0` | 0.1 to 1000 | Minimum pressure increase in atm for the catastrophic event, in addition to an active shock and sufficient rating excess. Higher makes the event harder to trigger. |
| `catastrophicPressureExcess` | `8.0` | 0.1 to 1000 | Minimum amount in atm above the equipment rating for catastrophic exposure. This is excess pressure, not total ambient pressure. Higher requires more severe over-rating. |

## Server: ascent and recovery

Exact TOML keys, defaults, accepted values and interactions. Use the active `deepdiving-server.toml`; see the configuration guide for global/world-file precedence.

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `maximumAscentBlocksPerSecond` | `2.5` | 0.1 to 10 | Sustained upward speed threshold in blocks per second. Higher allows faster ascent before the rapid-ascent warning. Grace time and minimum continuous rise must also be satisfied; decompression ceilings are separate. |
| `ascentWarningGraceSeconds` | `3.0` | 0 to 30 | Required consecutive seconds above the ascent-speed threshold. Higher tolerates longer short adjustments. Slowing, stopping or descending resets the continuous fast-ascent history. |
| `ascentWarningMinimumRise` | `4.0` | 0 to 64 | Minimum blocks gained during that same continuous fast-ascent streak. Higher tolerates greater short rises. This does not alter stop spacing or permit crossing a decompression ceiling. |
| `allowDepthHold` | `true` | true / false | Permits the explicit V depth-hold action with a computer, helmet and managed available breathing supply. False disables this hotkey feature; passive fin resting is separate. Flight/riding/blackout and required-stop checks still apply. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `tissueRate` | `1.0` | 0.1 to 10 | Scales nitrogen/helium tissue time for both gas uptake and release. Higher can make decompression obligations form faster as well as clear faster. For shorter stops alone, change decompressionRate instead. |
| `decompressionRate` | `6.0` | 0.1 to 60 | Additional multiplier on off-gassing only, also multiplied by tissueRate. Higher shortens recovery/stop forecasts where the current mix and pressure permit off-gassing. Does not speed uptake or excuse crossing a ceiling. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `minimumOxygenPartialPressure` | `0.16` | 0.01 to 1 | Lower oxygen threshold in atm. Higher makes low-oxygen mixes become deficient sooner and increases their minimum usable depth. Multiply oxygen fraction by ambient pressure to compare. |
| `maximumOxygenPartialPressure` | `1.4` | 1.01 to 10 | Upper oxygen threshold in atm. Above this, oxygen toxicity accumulates. Higher extends oxygen-only maximum depths but does not change narcosis or worn equipment ratings. |
| `narcosisNitrogenPressure` | `3.2` | 0.1 to 1000 | Nitrogen partial-pressure threshold in atm. Lower makes narcosis begin shallower. Default air crosses 3.2 atm nitrogen pressure at about 31 blocks under the default linear curve. |
| `hypoxiaDepletionPerSecond` | `7.0` | 0.1 to 100 | Base oxygen-reserve loss per simulated second during oxygen deficit, further scaled by deficit and difficulty severity. Higher gives less time to restore breathing. This does not set cylinder consumption. |
| `oxygenRecoveryPerSecond` | `5.0` | 0.1 to 100 | Oxygen-reserve recovery per simulated second when adequate breathing is restored. Higher refills the reserve faster; it does not directly clear nitrogen/helium loading or oxygen toxicity. |

## Server sediment

Exact TOML keys, defaults, accepted values and interactions. Use the active `deepdiving-server.toml`; see the configuration guide for global/world-file precedence.

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `sediment.enabled` | `true` | true / false | Allows seabed-disturbance generation for everyone in the world. False stops this feature regardless of client particle choices. Other ambience and equipment exhaust are separate. |
| `sediment.mining` | `true` | true / false | Allows sediment plumes from mining eligible loose seabed material underwater. Does not affect movement-generated plumes when bottomMovement is enabled. |
| `sediment.bottomMovement` | `true` | true / false | Allows plumes from moving near a loose seabed. Disable this while retaining mining plumes if normal swimming stirs too much sediment. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `sediment.cloudRadius` | `3.5` | 0.5 to 10 | Base mining-cloud radius in blocks. Movement plumes use 60% of this radius. Larger values spread each disturbance farther. |
| `sediment.cloudLifetimeSeconds` | `18.0` | 2 to 120 | Base mining-cloud lifetime in seconds. Movement plumes use 70% of this lifetime. Higher makes disturbed areas remain cloudy longer. |
| `sediment.miningDensity` | `0.9` | 0 to 4 | Base density for mining plumes, scaled by the disturbance. Higher makes mining clouds denser. Zero effectively removes mining density without changing the movement setting. |
| `sediment.movementDensity` | `0.3` | 0 to 4 | Base density for movement plumes. Higher makes ordinary bottom travel cloudier. Fins and careful movement apply their own reductions. |
| `sediment.maximumOverlapDensity` | `2.0` | 0.1 to 8 | Cap on accumulated cloud density where plumes overlap. Higher allows denser combined obscuration; it is not a count of particles. |
| `sediment.maximumClouds` | `12` | 1 to 32 | Maximum retained nearby cloud instances. Oldest clouds are removed when the cap is reached. Lower bounds cloud bookkeeping; the client particle budget is a separate cap. |
| `sediment.nearbyRange` | `48.0` | 8 to 96 | Range in blocks for distributing/retaining nearby sediment disturbances. Larger values make more distant plumes relevant and can increase work. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `sediment.maximumHeightAboveBed` | `1.8` | 0.5 to 4 | Maximum clearance in blocks above an eligible loose bed for movement disturbance. Higher lets swimmers stir it from farther above. |
| `sediment.minimumMovementSpeed` | `0.035` | 0.005 to 0.5 | Swimming movement threshold in blocks per tick; grounded bottom travel uses one quarter of it. Higher requires faster movement to disturb the bed. At 20 TPS, the default 0.035 is about 0.7 blocks/second. |
| `sediment.carefulMovementMultiplier` | `0.4` | 0 to 1 | Density multiplier for careful movement. Lower reduces newly disturbed sediment more; 1 gives no reduction. Does not instantly remove existing clouds. |
| `sediment.finsMultiplier` | `0.65` | 0 to 1 | Density multiplier while fins are worn. Lower gives fins a stronger reduction of newly disturbed sediment. It does not clear old clouds or change lamp strength. |
| `sediment.movementIntervalTicks` | `12` | 4 to 100 | Interval between periodic movement-disturbance checks. Higher produces checks/plumes less often. At 20 TPS the default 12 ticks is 0.6 seconds. |

## Client HUD and condition effects

Exact TOML keys, defaults, accepted values and interactions. Use `config/deepdiving-client.toml`. Dotted keys identify their section.

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `showHud` | `true` | true / false | Shows the carried computer's HUD. False hides the panel without disabling the full computer, gas use or server conditions. |
| `compactHud` | `true` | true / false | True uses the concise default layout; false restores the expanded readout. Both retain the independent HUD size setting. |
| `hudSize` | `NORMAL` | SMALL / NORMAL / LARGE / EXTRA_LARGE | SMALL = 75%, NORMAL = 100%, LARGE = 125%, EXTRA_LARGE = 150%. Scales only the dive HUD, independently of Minecraft GUI scale. It can shrink further to fit an unusually small viewport. |
| `hudX` | `8` | 0 to 4096 | Horizontal offset of the outer HUD frame from the left edge, in GUI pixels. Larger moves it right, subject to keeping the whole panel on screen. |
| `hudY` | `8` | 0 to 4096 | Vertical offset of the outer HUD frame from the top edge, in GUI pixels. Larger moves it down, subject to keeping the whole panel on screen. |
| `swimmingOrbitCamera` | `true` | true / false | Adds the underwater orbit perspective after the normal front view in the perspective-key cycle. It lets the camera look around without changing swim direction. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `visualAndSoundEffects` | `true` | true / false | Master for condition/distress presentation such as impaired vision and distress audio. It does not disable server conditions, damage, the HUD, or the separately configured breathing/ambience/lamp systems. |
| `effectIntensity` | `1.0` | 0 to 1 | Overall condition-effect strength, from none to full. Lower makes symptoms visually/audibly gentler; it does not lower condition severity on the server. |
| `eyelidBlinks` | `true` | true / false | Allows the eyelid/blink presentation of relevant conditions. Disable if blinking obscures play; the underlying oxygen/toxicity state remains. |
| `pressureBloodOverlay` | `true` | true / false | Allows the local screen overlay for pressure injury. Separate from server pressureBloodParticles and the actual damage switch. |
| `cameraSway` | `true` | true / false | Allows condition-driven camera movement. reducedMotion overrides animated sway even when this is true. |
| `distressSounds` | `true` | true / false | Allows condition-driven distress sounds/muffling. Equipment breathing and environmental audio use separate settings. |
| `reducedMotion` | `false` | true / false | Uses steadier condition cues and disables camera sway/animated distortion or pulses. Does not disable the server's movement impairment or diving conditions. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `shaders.enabled` | `true` | true / false | Allows this mod's condition post-processing and its shader fallback cues. These affect the world view, keeping menus/computer text sharp. Independent of diving-lamp shader lighting. |
| `shaders.intensity` | `1.0` | 0 to 1 | Condition post-processing/fallback strength, multiplied by effectIntensity. Lower reduces the shader presentation without changing server warnings. |
| `shaders.quality` | `HIGH` | LOW / HIGH | HIGH uses the fuller condition filter. LOW uses fewer blur samples and omits color separation, reducing cost. It does not change another mod's shader-pack quality. |
| `shaders.centerCalm` | `0.75` | 0 to 1 | Moves blur/distortion toward the edges: 0 treats the view more evenly; 1 favors a calmer centre. Does not change HUD size or warning severity. |
| `shaders.hypoxia` | `true` | true / false | Enables the condition-shader contribution for low oxygen. Other condition channels remain independent. |
| `shaders.narcosis` | `true` | true / false | Enables the condition-shader contribution for nitrogen narcosis. Disabling it does not remove narcosis movement/gameplay effects. |
| `shaders.oxygenToxicity` | `true` | true / false | Enables the condition-shader contribution for oxygen toxicity. Other overlays and symptoms have their own gates. |
| `shaders.decompressionSickness` | `true` | true / false | Enables the condition-shader contribution for decompression illness. Does not remove a required stop or accumulated stress. |
| `shaders.pressureInjury` | `true` | true / false | Enables the condition-shader contribution for equipment-pressure injury. Does not control server pressure damage. |
| `shaders.shaderPackFallback` | `true` | true / false | Automatically uses gentler overlays instead of the full condition post-process when an Iris shader pack is active, or Oculus is detected. This does not turn the installed shader pack off. |
| `shaders.overlayFallback` | `true` | true / false | Allows condition-specific edge cues when the mod's post-processing cannot run. Keep this and shaders.enabled on if you want fallback warnings with a shader pack. |

## Client audio and bubbles

Exact TOML keys, defaults, accepted values and interactions. Use `config/deepdiving-client.toml`. Dotted keys identify their section.

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `computer.alarms` | `true` | true / false | Plays a quiet tone when a computer warning appears. Turning it off leaves the HUD/computer warning text available. |
| `computer.alarmVolume` | `0.15` | 0 to 1 | Alarm loudness. Lower quiets tones; zero silences them. Does not change breathing or ambience volume. |
| `computer.alarmCooldownSeconds` | `8` | 1 to 300 | Minimum ordinary alarm interval in seconds. Higher reduces repeat tones. An urgent escalation can interrupt the cooldown. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `acoustics.equipmentBreathing` | `true` | true / false | Enables diving-equipment breathing sounds. Separate from exhaust particles and environmental sound filtering. |
| `acoustics.breathingVolume` | `0.28` | 0 to 1 | Base loudness of equipment breathing. Lower quiets breathing without changing alarms or ambient audio. |
| `acoustics.exhaustBubbles` | `true` | true / false | Allows the mouth puff and trailing equipment-exhaust particles on this client. The server breathingBubbles switch must also be true. Bubble burst size/rise speed do not have separate config keys in RC1. |
| `acoustics.nearbyDivers` | `true` | true / false | Includes nearby divers in equipment breathing/exhaust presentation. False keeps local equipment effects without nearby-diver presentation; lamp.nearbyDivers is separate. |
| `acoustics.nearbyBreathingVolume` | `0.45` | 0 to 1 | Volume multiplier for other divers' breathing, with distance attenuation also applied. Does not multiply your own breathing volume. |
| `acoustics.soundFiltering` | `true` | true / false | Enables Deep Diving air/water sound transmission filtering. Disable if another sound mod should own underwater muffling; equipment breathing/exhaust can remain enabled. |
| `acoustics.filteringStrength` | `0.8` | 0 to 1 | Strength of underwater acoustic filtering. Lower makes air/water transmission less muffled; zero removes the strength of this filter. |
| `acoustics.transitionSeconds` | `0.35` | 0.05 to 3 | Seconds used to blend acoustic changes across the surface or an air pocket. Higher changes more slowly; it is not a breathing interval. |

## Client ambience and particles

Exact TOML keys, defaults, accepted values and interactions. Use `config/deepdiving-client.toml`. Dotted keys identify their section.

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `ambience.sounds` | `true` | true / false | Enables the mod's underwater environmental soundscape. Marine snow and luminous particles have independent switches. |
| `ambience.ambientVolume` | `0.1` | 0 to 1 | Upper volume level for environmental ambience. Lower makes the wash and cues quieter without changing equipment breathing. |
| `ambience.soundFrequency` | `1.0` | 0 to 3 | Multiplier for occasional environmental cue frequency. Higher makes cues more frequent. Zero disables occasional cues while the continuous wash remains. |
| `ambience.shallowDepth` | `8.0` | 0 to 4096 | Depth in blocks where the shallow-to-deep audio blend begins. Raise it to keep the shallow soundscape farther down. |
| `ambience.deepDepth` | `48.0` | 1 to 8192 | Depth in blocks for the deep soundscape blend. Runtime clamps it relative to shallowDepth, so a lower value does not reverse the transition. Also participates in depth blending for marine snow. |
| `ambience.transitionSeconds` | `1.2` | 0.1 to 10 | Blend time for changing ambience/particle presence. Higher smooths changes more slowly; does not change physiology simulation time. |
| `ambience.marineSnow` | `true` | true / false | Enables suspended marine-snow particles. Server sediment generation and breathing exhaust are separate features. |
| `ambience.marineSnowStartDepth` | `24.0` | 0 to 8192 | Depth in blocks at which snow begins to blend in. Higher keeps it out of shallower water. |
| `ambience.particleDensity` | `0.7` | 0 to 1 | Marine-snow target density as a fraction of its budget. Higher adds snow up to the active local/shared caps; zero removes snow density. |
| `ambience.particleLimit` | `64` | 0 to 256 | Combined local cap for marine snow and luminous specks. Zero disables both. Minecraft Decreased/Minimal particle settings reduce the effective budget further. |
| `ambience.bioluminescence` | `true` | true / false | Allows rare faint blue-green specks in ocean-tagged biomes, with at most two alive at once. Snow need not be enabled, but particle budgets must have room. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `sediment.particles` | `true` | true / false | Allows local temporary sediment plumes. Server sediment.enabled must also permit them. False removes this client's sediment-cloud rendering, including its cloud-based light attenuation; it does not rewrite biome fog. |
| `sediment.particleLimit` | `200` | 0 to 1000 | Local sediment-particle cap, also constrained by underwaterParticles.combinedLimit and Minecraft particle settings. Zero removes sediment particles without necessarily removing retained cloud-density calculations. |
| `sediment.spawnAttemptsPerTick` | `8` | 0 to 32 | Maximum local sediment spawn attempts per client tick. Lower limits how quickly a plume fills with particles; zero prevents new particle spawns. |
| `sediment.color` | `9930090` | 0 to 16777215 | Sediment RGB as a decimal integer. Default 9930090 is hex 97856A. Changes plume tint, not density, biome water color or shader-pack fog. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `underwaterParticles.combinedLimit` | `256` | 0 to 1024 | Shared hard particle cap for sediment, marine snow and luminous specks. Zero disables those particle populations; it does not govern breathing exhaust or particles from other mods. |

## Client lamps

Exact TOML keys, defaults, accepted values and interactions. Use `config/deepdiving-client.toml`. Dotted keys identify their section.

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `lamp.enabled` | `true` | true / false | Enables Deep Diving lamp illumination on this client. Does not change each item's stored on/off state or light from other mods. |
| `lamp.brightness` | `1.0` | 0 to 3 | Lamp brightness multiplier. Higher brightens active light; zero disables its contribution. Does not expand the beam range. |
| `lamp.range` | `16.0` | 3 to 24 | Base held-lamp reach in blocks before the item profile's reach multiplier. Long-throw/wide/inspection lamps modify it; stowed lamps use a shorter downward presentation and reducedEffects adds a cap. |
| `lamp.beamWidth` | `44.0` | 12 to 100 | Full spotlight angle in degrees for the adjustable balanced profile. Higher widens it. Specialized wide/long-throw/inspection profiles keep their own beam angles. |
| `lamp.color` | `WARM` | WARM / COOL / CUSTOM | WARM uses RGB FFF0D5, COOL uses C8E9FF, CUSTOM uses customColor. This changes light tint, not the lamp item texture. |
| `lamp.customColor` | `16773333` | 0 to 16777215 | RGB as a decimal integer, used only when color is CUSTOM. 16773333 is FFF0D5; 13167103 is C8E9FF. Range covers 000000–FFFFFF. |
| `lamp.volumetricIntensity` | `0.7` | 0 to 2 | Visible beam/scattering intensity. Higher makes the water beam more apparent. Zero removes this volumetric contribution; reducedEffects also removes volumetric ray marching. |
| `lamp.transitionSeconds` | `0.25` | 0.05 to 2 | Blend time when a lamp is activated, moved between held/stowed states or changes aim presentation. Higher makes changes ease more slowly. |
| `lamp.shadowSoftness` | `1.0` | 0 to 3 | Shadow-edge softness. Zero is crisp; larger values soften the edge and may cost more. Reduced effects uses the crisp path. |

| Setting | Default | Allowed values | What it changes |
| --- | --- | --- | --- |
| `lamp.nearbyDivers` | `true` | true / false | Allows illumination from other divers' active lamps. Independent of nearby equipment breathing. Nearby-light and total shadowed-light limits still apply. |
| `lamp.nearbyLightLimit` | `2` | 0 to 7 | Maximum nearby-diver lamp contributions, before the total lamp cap. Zero excludes nearby lamps while keeping your own. Reduced effects uses one total contribution. |
| `lamp.reducedEffects` | `false` | true / false | Performance preset: one shadowed lamp contribution, shorter range, crisp shadows and no volumetric ray marching. It does not alter server gameplay or the saved values of the other sliders. |
| `lamp.shaderLighting` | `true` | true / false | Allows lamp lighting/material/depth bridges with supported Iris shader packs. False stops those pack integrations, while ordinary non-pack lighting is separate. It does not control the physiology shader settings. |
| `lamp.entityShadows` | `true` | true / false | Allows entities to cast lamp shadows. Turning it off can reduce shadow work while retaining block occlusion. |
| `lamp.maximumShadowedLights` | `3` | 1 to 8 | Hard total light/shadow-map cap, including your own lamp. Excess distant lamps are omitted rather than rendered through walls. Lower can reduce cost; nearbyLightLimit still applies. |
| `lamp.shadowUpdatesPerFrame` | `2` | 1 to 8 | Maximum shadow maps refreshed in a rendered frame. Higher catches up with changing lamps faster but can increase frame cost. Reduced effects uses a one-update budget. |
| `lamp.shadowUpdatesPerSecond` | `20` | 2 to 30 | Target shadow refresh rate per lamp. Higher makes moving shadows update more smoothly but adds work, subject to the per-frame budget. |

[Back to wiki home](Home.md)
