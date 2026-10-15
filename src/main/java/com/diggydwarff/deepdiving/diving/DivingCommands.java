package com.diggydwarff.deepdiving.diving;

import com.diggydwarff.deepdiving.DeepDiving;
import com.diggydwarff.deepdiving.equipment.CylinderItem;
import com.diggydwarff.deepdiving.gas.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only helpers for testing dive profiles without waiting for every refill cycle. */
@EventBusSubscriber(modid = DeepDiving.MOD_ID)
public final class DivingCommands {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent e) {
        e.getDispatcher()
            .register(
                Commands.literal("deepdiving")
                    .requires(s -> s.hasPermission(2))
                    .then(
                        Commands.literal("status")
                            .executes(
                                c -> {
                                    var s = DiverService.state(
                                        c.getSource()
                                            .getPlayerOrException()
                                    );

                                    c.getSource()
                                        .sendSuccess(
                                            () -> Component.literal(
                                                String.format(
                                                    java
                                                            .util
                                                            .Locale
                                                            .ROOT,
                                                    "Depth %.1f | %.2f atm | O2 %.0f%% | Ceiling %.0f"
                                                            + " | Stress %.1f | %s",
                                                    s.depth,
                                                    s.pressure,
                                                    s.oxygenation,
                                                    s.ceiling,
                                                    s.decompressionStress,
                                                    s
                                                            .mixture.name()
                                                )
                                            ),
                                            false
                                        );
                                    return 1;
                                }
                            )
                    )
                    .then(
                        Commands.literal("reset")
                            .executes(
                                c -> {
                                    DiverService.reset(
                                        c.getSource()
                                            .getPlayerOrException()
                                    );
                                    c.getSource()
                                        .sendSuccess(() -> Component.literal("Diver physiology reset."), false);
                                    return 1;
                                }
                            )
                    )
                    .then(
                        Commands.literal("fill")
                            .then(
                                Commands.argument("oxygen", IntegerArgumentType.integer(1, 100))
                                    .then(
                                        Commands.argument("helium", IntegerArgumentType.integer(0, 99))
                                            .executes(
                                                c -> {
                                                    int
                                                            oxygen = IntegerArgumentType.getInteger(c, "oxygen");

                                                    int
                                                            helium = IntegerArgumentType.getInteger(c, "helium");

                                                    var p = c.getSource()
                                                        .getPlayerOrException();

                                                    var
                                                            stack = p.getMainHandItem();

                                                    if (!(stack.getItem()
                                                                    instanceof
                                                                    CylinderItem
                                                                                    item)
                                                        || oxygen
                                                                        + helium
                                                                > 100) {
                                                        c.getSource()
                                                            .sendFailure(
                                                                Component.literal(
                                                                    "Hold a diving cylinder; O2 + He must"
                                                                            + " not exceed 100%."
                                                                )
                                                            );
                                                        return 0;
                                                    }

                                                    stack.set(
                                                        DeepDiving
                                                                .SUPPLY.get(),
                                                        new CylinderSupply(
                                                            new GasMix(oxygen, helium),
                                                            item.capacity,
                                                            CylinderItem.supply(stack)
                                                                .backupEnabled()
                                                        )
                                                    );
                                                    c.getSource()
                                                        .sendSuccess(
                                                            () -> Component.literal(
                                                                "Cylinder filled: "
                                                                        + new GasMix(oxygen, helium).summary()
                                                            ),
                                                            false
                                                        );
                                                    return 1;
                                                }
                                            )
                                    )
                            )
                    )
            );
    }

    private DivingCommands() {}
}
