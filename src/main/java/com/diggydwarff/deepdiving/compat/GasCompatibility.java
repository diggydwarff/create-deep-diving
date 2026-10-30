package com.diggydwarff.deepdiving.compat;

import com.diggydwarff.deepdiving.gas.Gases;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Declared breathing gases share one internal representation; cryogenic fluids are never guessed by
 * name.
 */
public final class GasCompatibility {
    private static final String[] NAMES = {"compressed_air", "oxygen", "nitrogen", "helium"};

    public static int kind(FluidStack stack) {
        if (stack.isEmpty()) return -1;
        for (int i = 0; i < NAMES.length; i++) {
            if (stack.getFluid().isSame(Gases.all()[i].source.get())
                    || stack.getFluid().is(tag("c", NAMES[i]))
                    || i == 1 && stack.getFluid().is(tag("create_submarine", "oxygen"))) return i;
        }
        return -1;
    }

    public static FluidStack canonical(FluidStack stack) {
        int kind = kind(stack);
        return kind < 0
                ? FluidStack.EMPTY
                : new FluidStack(Gases.all()[kind].source.get(), stack.getAmount());
    }

    private static TagKey<Fluid> tag(String namespace, String name) {
        return TagKey.create(
                Registries.FLUID, ResourceLocation.fromNamespaceAndPath(namespace, name));
    }

    private GasCompatibility() {}
}
