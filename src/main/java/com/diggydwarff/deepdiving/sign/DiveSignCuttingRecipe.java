package com.diggydwarff.deepdiving.sign;

import com.diggydwarff.deepdiving.DeepDiving;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;

/** Changing the pictogram keeps a player's caption, arrow, ink and anvil name. */
public final class DiveSignCuttingRecipe extends StonecutterRecipe {
    public static final RecipeSerializer<DiveSignCuttingRecipe> SERIALIZER =
            new SingleItemRecipe.Serializer<DiveSignCuttingRecipe>(DiveSignCuttingRecipe::new) {};

    public DiveSignCuttingRecipe(String group, Ingredient ingredient, ItemStack result) {
        super(group, ingredient, result);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return DeepDiving.DIVE_SIGN_CUTTING.get();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        ItemStack result = super.assemble(input, registries);
        for (var component :
                java.util.List.of(DataComponents.CUSTOM_DATA, DataComponents.CUSTOM_NAME)) {
            copyComponent(input.item(), result, component);
        }
        return result;
    }

    private static <T> void copyComponent(
            ItemStack source,
            ItemStack target,
            net.minecraft.core.component.DataComponentType<T> type) {
        if (source.has(type)) target.set(type, source.get(type));
    }
}
