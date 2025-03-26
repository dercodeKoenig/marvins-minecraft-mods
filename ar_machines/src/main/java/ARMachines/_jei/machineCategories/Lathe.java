package ARMachines._jei.machineCategories;

import ARLib.utils.MachineRecipe;
import ARMachines._jei.MachineRecipeCategory;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.recipe.RecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class Lathe extends MachineRecipeCategory {


    public static final RecipeType<MachineRecipe> MACHINE_RECIPE_TYPE = new RecipeType<>(
            ResourceLocation.fromNamespaceAndPath("armachines", "lathe"),
            MachineRecipe.class
    );

    @Override
    public RecipeType<MachineRecipe> getRecipeType() {
        return MACHINE_RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("Lathe");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return null;
    }
}