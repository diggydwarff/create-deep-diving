package com.diggydwarff.deepdiving.compat;

import com.diggydwarff.deepdiving.machine.GasMachine;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

public final class GasDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(
        DisplayLinkContext context,
        DisplayTargetStats stats
    ) {
        if (!(context.getSourceBlockEntity() instanceof GasMachine machine)) {
            return EMPTY;
        }

        var lines = new java.util.ArrayList<MutableComponent>();

        lines.add(Component.literal(GasMachine.statusText(machine.status)));

        for (var tank : machine.tanks) {
            if (!tank.isEmpty()) {
                lines.add(
                    Component.literal(
                        tank.getFluid().getHoverName().getString()
                                + " "
                                + tank.getFluidAmount()
                                + " mB"
                    )
                );
            }
        }

        lines.add(Component.literal(machine.target.summary()));
        return lines;
    }

    @Override
    public Component getName() {
        return Component.translatable("display_source.deepdiving.gas_status");
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
