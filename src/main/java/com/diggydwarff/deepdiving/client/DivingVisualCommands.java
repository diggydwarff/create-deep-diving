package com.diggydwarff.deepdiving.client;

import com.diggydwarff.deepdiving.DeepDiving;
import com.mojang.brigadier.arguments.IntegerArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

import java.util.List;

@EventBusSubscriber(
    modid = DeepDiving.MOD_ID,
    value = Dist.CLIENT
)
public final class DivingVisualCommands {
    @SubscribeEvent
    public static void register(RegisterClientCommandsEvent event) {
        var root = Commands.literal("deepdivingvisual");

        root.then(
            Commands.literal("stop")
                .executes(
                    ctx -> {
                        DivingPostProcess.stopPreview();
                        ctx.getSource()
                            .sendSuccess(() -> Component.literal("Diving visual preview stopped."), false);
                        return 1;
                    }
                )
        );
        root.then(
            Commands.literal("status")
                .executes(
                    ctx -> {
                        ctx.getSource()
                            .sendSuccess(() -> Component.literal(DivingPostProcess.status()), false);
                        return 1;
                    }
                )
        );

        var preview = Commands.literal("preview");

        for (String condition :
                List.of(
                    "hypoxia",
                    "narcosis",
                    "toxicity",
                    "decompression",
                    "pressure",
                    "blackout",
                    "combined"
                )) {
            preview.then(
                Commands.literal(condition)
                    .then(
                        Commands.argument("severity", IntegerArgumentType.integer(1, 3))
                            .executes(
                                ctx -> {
                                    DivingPostProcess.preview(
                                        condition,
                                        IntegerArgumentType.getInteger(ctx, "severity")
                                    );
                                    ctx.getSource()
                                        .sendSuccess(
                                            () -> Component.literal(
                                                "Visual-only "
                                                        + condition
                                                        + " preview for 60 seconds. /deepdivingvisual stop"
                                                        + " to end."
                                            ),
                                            false
                                        );
                                    return 1;
                                }
                            )
                    )
            );
        }

        event.getDispatcher().register(root.then(preview));
    }

    private DivingVisualCommands() {}
}
