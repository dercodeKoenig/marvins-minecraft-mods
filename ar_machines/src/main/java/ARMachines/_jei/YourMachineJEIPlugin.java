package ARMachines._jei;

import ARMachines.MultiblockRegistry;
import ARMachines._jei.machineCategories.Lathe;
import ARMachines.lathe.LatheConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

@JeiPlugin
public class YourMachineJEIPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath("armachines", "plugin");
    }


@Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
    registration.addRecipeCategories(new Lathe());
    //registration.addRecipeCategories(new RollingMachine());
    //registration.addRecipeCategories(new Crystallizer());
    }


    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(MultiblockRegistry.BLOCK_LATHE.get()), Lathe.MACHINE_RECIPE_TYPE);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        IJeiRuntime runtime = jeiRuntime;
        LatheConfig.jeiRunnableOnConfigLoad = new Runnable() {
            @Override
            public void run() {
                runtime.getRecipeManager().addRecipes(Lathe.MACHINE_RECIPE_TYPE, LatheConfig.INSTANCE.recipes);
            }
        };
    }
}