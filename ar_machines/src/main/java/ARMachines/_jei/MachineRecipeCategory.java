package ARMachines._jei;

import ARLib.utils.MachineRecipe;
import ARLib.utils.RecipePart;
import ARLib.utils.RecipePartWithProbability;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static ARLib.utils.ItemUtils.*;

public abstract class MachineRecipeCategory implements IRecipeCategory<MachineRecipe> {

    public MachineRecipeCategory() {
    }

    @Override
    public Component getTitle() {
        return Component.translatable("Machine Recipe");
    }
    @Override
    public int getWidth(){
        return 140;
    }
    @Override
    public int getHeight(){
        return 100;
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return null;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MachineRecipe recipe, IFocusGroup focuses) {
        // Define inputs
        int rn = 0;
        for (RecipePart input : recipe.inputs) {
            String inputIdOrTag = input.id;
            int amount = input.amount;

            ItemStack iStack = getItemStackFromid(inputIdOrTag, amount);
            FluidStack fStack = getFluidStackFromId(inputIdOrTag, amount);

            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.INPUT, 0+rn%3*20, 20 + rn/3 * 20);
            rn+=1;

            if (iStack != null) {
                slot.addItemStack(iStack);
            } else if (fStack != null) {
                slot.addFluidStack(fStack.getFluid(), fStack.getAmount());
            } else {
                ResourceLocation tagLocation = ResourceLocation.tryParse(inputIdOrTag);
                TagKey<Fluid> fluidTag = TagKey.create(Registries.FLUID, tagLocation);
                List<FluidStack> fluidStacks = new ArrayList<>();

                BuiltInRegistries.FLUID.getTag(fluidTag).ifPresent(tag -> {
                    for (Holder<Fluid> fluidHolder : tag) {
                        Fluid fluid = fluidHolder.value();
                        fluidStacks.add(new FluidStack(fluid, amount));
                    }
                });

                if (!fluidStacks.isEmpty()) {
                    for (FluidStack f : fluidStacks) {
                        slot.addFluidStack(f.getFluid(), f.getAmount());
                    }
                } else {
                    slot.addIngredients(Ingredient.of(TagKey.create(Registries.ITEM, tagLocation)));
                }
            }
        }

        // Define outputs
        rn = 0;
        for (RecipePartWithProbability output : recipe.outputs) {

            String outputId = output.id;
            int amount = output.amount;

            ItemStack iStack = getItemStackFromid(outputId, amount);
            FluidStack fStack = getFluidStackFromId(outputId, amount);

            IRecipeSlotBuilder slot = builder.addSlot(RecipeIngredientRole.OUTPUT,80+rn%3*20, 20 + rn/3 * 20);
            rn+=1;

            if (iStack != null) {
                slot.addItemStack(iStack);
            }
            if (fStack != null) {
                slot.addFluidStack(fStack.getFluid(), fStack.getAmount());
            }
        }
    }

    @Override
    public void draw(MachineRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Optional: Draw custom text or graphics, such as energy cost.
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                Component.translatable("Energy per tick: "+ recipe.energyPerTick),
                0, 0,
                0xFF404040,false
        );
        guiGraphics.blit(ResourceLocation.fromNamespaceAndPath("arlib", "textures/gui/arrow_right.png"), 65, 70, 10, 12, 0f, 0f, 12, 16, 12, 16);
    }

    @Override
    public boolean isHandled(MachineRecipe recipe) {
        // Define whether this recipe should be shown or filtered out.
        return true;
    }
}
