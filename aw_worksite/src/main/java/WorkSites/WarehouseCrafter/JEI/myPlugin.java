package WorkSites.WarehouseCrafter.JEI;

import ARLib.network.PacketBlockEntity;
import ARLib.utils.RecipePart;
import ResearchSystem.Config.RecipeConfig;
import WorkSites.Main;
import WorkSites.WarehouseCrafter.MenuWarehouseCrafter;
import com.google.gson.Gson;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IUniversalRecipeTransferHandler;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static WorkSites.Registry.MENU_WAREHOUSE_CRAFTER;
import static WorkSites.Registry.WAREHOUSE_CRAFTER;

@JeiPlugin
public class myPlugin implements IModPlugin {

    IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(Main.MODID, "plugin");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {

    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {

    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(WAREHOUSE_CRAFTER.get(), RecipeTypes.CRAFTING);
        registration.addRecipeCatalyst(WAREHOUSE_CRAFTER.get(), ResearchSystem.jei.RealNiceJeiCategory.recipeType);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {

        IUniversalRecipeTransferHandler<MenuWarehouseCrafter> whateverbs = new IUniversalRecipeTransferHandler<>() {
            @Override
            public @NotNull Class<MenuWarehouseCrafter> getContainerClass() {
                return MenuWarehouseCrafter.class;
            }

            @Override
            public @NotNull Optional<MenuType<MenuWarehouseCrafter>> getMenuType() {
                return Optional.of(MENU_WAREHOUSE_CRAFTER.get());
            }


            @Override
            /**
             * @param container   the container to act on
             * @param recipe      the raw recipe instance
             * @param recipeSlots the view of the recipe slots, with information about the ingredients
             * @param player      the player, to do the slot manipulation
             * @param maxTransfer if true, transfer as many items as possible. if false, transfer one set
             * @param doTransfer  if true, do the transfer. if false, check for errors but do not actually transfer the items
             * @return a recipe transfer error if the recipe can't be transferred. Return null on success.
             *
             * @since 19.8.1
             */
            @Nullable
            public IRecipeTransferError transferRecipe(
                    MenuWarehouseCrafter container,
                    Object recipe,
                    IRecipeSlotsView recipeSlots,
                    Player player,
                    boolean maxTransfer,
                    boolean doTransfer
            ) {
                // I send the recipe as json to the server so the server can place the items
                if (doTransfer) {
                    Gson gson = new Gson();
                    String data = "";
                    if (recipe instanceof RecipeConfig.Recipe h) {
                        List<List<String>> requiredItems = new ArrayList<>();
                        for (int y = 0; y < h.pattern.size(); y++) {
                            for (int x = 0; x < h.pattern.get(y).length(); x++) {
                                List<String> allowedParts = new ArrayList<>();
                                String c = String.valueOf(h.pattern.get(y).charAt(x));
                                if (h.keys.containsKey(c)) {
                                    RecipeConfig.RecipeInput i = h.keys.get(c);
                                    allowedParts.add(gson.toJson(i.input));
                                } else {
                                    allowedParts.add(gson.toJson(new RecipePart()));
                                }
                                requiredItems.add(allowedParts);
                            }
                        }
                        data = gson.toJson(requiredItems);
                    }
                    if (recipe instanceof RecipeHolder h) {
                        if (h.value() instanceof ShapelessRecipe s) {
                            List<Ingredient> ings = new ArrayList<>(s.getIngredients());
                            List<List<String>> requiredItems = new ArrayList<>();
                            for (int i = 0; i < 9; i++) {
                                List<String> allowedParts = new ArrayList<>();
                                if (i < ings.size()) {
                                    for (ItemStack allowed : ings.get(i).getItems()) {
                                        allowedParts.add(gson.toJson(new RecipePart(BuiltInRegistries.ITEM.getKey(allowed.getItem()).toString(), 1)));
                                    }
                                } else {
                                    allowedParts.add(gson.toJson(new RecipePart()));
                                }
                                requiredItems.add(allowedParts);
                            }
                            data = gson.toJson(requiredItems);
                        }

                        if (h.value() instanceof ShapedRecipe s) {
                            List<List<String>> requiredItems = new ArrayList<>();
                            ShapedRecipePattern pattern = s.pattern;

                            // if pattern is 2x2 the list is of size 4
                            // if pattern is 3x3 the list is of size 9
                            List<Ingredient> ings = pattern.ingredients();

                            for (int i = 0; i < 9; i++) {
                                List<String> allowedParts = new ArrayList<>();
                                int y = i / 3;
                                int x = i % 3;
                                if (x < pattern.width() && y < pattern.height()) {
                                    int patternIndex = x + pattern.width() * y;
                                    for (ItemStack allowed : ings.get(patternIndex).getItems()) {
                                        allowedParts.add(gson.toJson(new RecipePart(BuiltInRegistries.ITEM.getKey(allowed.getItem()).toString(), 1)));
                                    }
                                } else {
                                    allowedParts.add(gson.toJson(new RecipePart()));
                                }
                                requiredItems.add(allowedParts);
                            }
                            data = gson.toJson(requiredItems);
                        }
                    }

                    if (!data.isEmpty()) {
                        CompoundTag info = new CompoundTag();
                        info.putString("data", data);
                        CompoundTag moveItemsTag = new CompoundTag();
                        moveItemsTag.put("moveItems", info);
                        PacketDistributor.sendToServer(
                                PacketBlockEntity.getBlockEntityPacket(Minecraft.getInstance().level, container.CLIENT_myBlockPos, moveItemsTag)
                        );
                    }
                }
                return null;
            }
        };
        registration.addUniversalRecipeTransferHandler(whateverbs);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {

    }
}