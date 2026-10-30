# Diving mechanics and recovery

[Wiki home](Home.md) · RC1

Understand depth, gas exposure, warning conditions and decompression. Numerical examples use Standard defaults.

## Contents

- [Depth, pressure and gas mixtures](#depth-pressure-and-gas-mixtures)
- [Conditions and recovery](#conditions-and-recovery)
- [Ascent and decompression](#ascent-and-decompression)

## Depth, pressure and gas mixtures

Choose a mixture for its oxygen and nitrogen exposure at your actual pressure.

### Depth and pressure

The default linear profile is:

```text
Pressure in atm = 1 + depth in blocks / 10
```

| Depth below the surface | Default pressure |
| ---: | ---: |
| 0 blocks | 1 atm |
| 10 blocks | 2 atm |
| 30 blocks | 4 atm |
| 40 blocks | 5 atm |
| 100 blocks | 11 atm |
| 200 blocks | 21 atm |

World Y alone does not tell you depth: the water surface and your eye height matter. Local-water sampling is the default. Server owners can choose a fixed surface, a reference-ocean curve, or per-dimension profiles. A reference curve can give deep custom oceans a different progression; the computer, gas tooltips and decompression calculations use the same curve.

### Presets and custom mixtures

The blender provides four presets. Oxygen and helium can also be adjusted in **one-percent steps**; nitrogen is the remainder.

| Preset | Oxygen | Nitrogen | Helium | Oxygen-only maximum depth under default settings, rounded down |
| --- | ---: | ---: | ---: | ---: |
| Air | 21% | 79% | 0% | 56 blocks |
| Nitrox | 32% | 68% | 0% | 33 blocks |
| Trimix | 18% | 37% | 45% | 67 blocks |
| Heliox | 21% | 0% | 79% | 56 blocks |

These maxima describe only the upper oxygen limit. They do not include narcosis, worn equipment rating, gas duration or decompression. Check the current cylinder tooltip when the server uses custom settings.

The default oxygen partial-pressure interval is **0.16–1.4 atm**. Partial pressure is ambient pressure multiplied by the gas fraction. More oxygen can improve a shallower mixture but reaches the upper oxygen limit sooner. Low-oxygen custom mixes can have a **minimum usable depth** as well as a maximum. The tooltip rounds the minimum upward and maximum downward.

Helium replaces nitrogen and reduces nitrogen narcosis; it does not remove pressure limits, oxygen toxicity or decompression. The default nitrogen-narcosis threshold is **3.2 atm nitrogen pressure**. With air and the default linear curve, it begins around **31 blocks** of depth, before air reaches its oxygen-only maximum.

Purge a partly filled cylinder before changing to a different mixture. Purging vents its remaining gas. Topping up with the same mixture preserves what is already inside.

## Conditions and recovery

Identify the underlying condition in the computer; surfacing does not instantly clear every condition.

Physiology is a simplified game model with four nitrogen/helium tissue compartments and persistent residual loading. The oxygen reserve shown by the computer is a game resource, not a blood-oxygen measurement.

| Warning or condition | What causes it in the mod | Response in the game |
| --- | --- | --- |
| Low gas reserve | Selected supply falls below 15% capacity | Check time remaining and prepare a filled, enabled reserve or end the dive |
| Backup cylinder active | An inventory/offhand backup or emergency supply is selected | Check which supply is being used and its remaining time |
| No breathing supply | Diving helmet is underwater without available supply or a breathing bypass | Restore a filled compatible supply; check helmet, slot and spare enablement |
| Low oxygen / hypoxia | No breathable supply after reserve air runs low, or insufficient oxygen at current pressure | Restore breathing with a mixture suitable for current pressure |
| Oxygen toxicity | Excess oxygen partial pressure; exposure accumulates | Reduce the excessive exposure and allow toxicity to recover |
| Narcosis | Nitrogen partial pressure exceeds the configured threshold | Reduce nitrogen exposure, using the computer to check mixture and depth |
| Ascending too fast | Sustained ascent crosses the server's speed, time and rise thresholds | Slow the ascent; required decompression stops still apply |
| Decompression required | Residual tissue loading imposes a ceiling | Follow the stop guidance and wait for the next step to clear |
| Decompression recovery | Decompression stress remains after the ceiling clears | Allow recovery; a clear ceiling does not instantly remove existing illness |
| Equipment pressure limit / pressure injury | Ambient pressure exceeds the worn equipment's protection | Return to pressure within the equipment's rating and allow injury to recover |
| Sudden pressure exposure | An abrupt, insufficiently protected compression transition | Equip rated gear before entering deep water; check the world's pressure profile |
| Blackout | Severe oxygen deficit or accumulated oxygen toxicity | Restore suitable breathing and allow reserve/toxicity to recover; inventory access remains available |

Symptoms can include altered vision, eyelid blinks, camera sway, muffled sound, impaired movement and damage. Recovery is gradual. Blackout uses separate onset/recovery thresholds so it does not rapidly flicker on and off.

By default, Water Breathing and Conduit Power bypass compressed-gas requirements and preserve tank gas. They do not erase earlier tissue loading or existing decompression obligations. Breathable air also does not instantly clear prior loading.

Physiology persists through ordinary logout. Death resets the diver's physiology. Spectator mode and Creative without a diving helmet bypass simulation; wearing a Create diving helmet opts Creative into gas use, warnings and physiology for testing.

## Ascent and decompression

A rapid-ascent warning and a required decompression stop are independent systems.

Ordinary short up/down adjustments are allowed a grace period. Standard defaults require all of the following before a rapid-ascent warning:

- More than **2.5 blocks per second** upward.
- **Three consecutive seconds** of fast ascent.
- At least **four blocks gained** during that continuous ascent.

Slowing, stopping or descending resets the fast-ascent history. This grace period does not permit crossing a decompression ceiling.

The computer rounds required stops to **three-block increments**. A stop depth of 12 means stay at least 12 blocks below the surface while that ceiling remains. `DESCEND TO …` takes priority if you are too shallow. Approach the indicated stop, maintain breathing supply, and follow the countdown for the next step. Being much deeper is not a shortcut: off-gassing and the estimate depend on the mixture and pressure where you actually are. A missing countdown can mean you need to move to the stop or restore supply.

When the next step clears, move to the newly indicated depth. V depth hold helps you stay in place, but you release and retoggle it when choosing a new depth. Passive fins do not automatically force a multi-block descent to a deeper stop.

The default `decompressionRate = 6.0` accelerates off-gassing for shorter game stops without accelerating gas uptake. `tissueRate` scales tissue time more generally. World owners can change these separately. Existing worlds retain saved settings, so an older ascent limit may need updating manually.

[Back to wiki home](Home.md)
