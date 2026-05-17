package advRocketry.SpaceSuit;

import advRocketry.Config;
import advRocketry.Items.ItemPortablePressureTank;
import advRocketry.Main;
import advRocketry.Registry.Fluids;
import advRocketry.Registry.GeneralRegistry;
import advRocketry.Utils.ClientUtils;
import advRocketry.Utils.ItemUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.List;

public abstract class SpaceSuit extends ArmorItem implements ISpaceSuitInventory {
    public static final List<ArmorMaterial.Layer> spaceSuitLayers = List.of(
            new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Main.MODID, "space_suit"))
    );
    public static final HashMap<ArmorItem.Type, Integer> protection = new HashMap<>();

    static {
        protection.put(Type.HELMET, 5);
        protection.put(Type.CHESTPLATE, 5);
        protection.put(Type.LEGGINGS, 5);
        protection.put(Type.BOOTS, 5);
    }


    public SpaceSuit(Type type, Properties properties) {
        super(GeneralRegistry.SPACE_SUIT_MATERIAL, type, properties);
    }

    public static void sharedJetpackTick(Player player){
        player.addDeltaMovement(new Vec3(0,player.getGravity() + 0.04,0));
        player.resetFallDistance();

        // Get the direction the player is looking (a normalized vector where x, y, z are between -1.0 and 1.0)
        Vec3 lookDirection = player.getLookAngle();
        double forwardSpeed = 0.15; // Adjust this to change how fast they fly horizontally
        // Check if they are actually pressing W (zza > 0) or S (zza < 0)
        if (player.zza != 0) {
            // If pressing S, move backwards instead of forwards
            double directionMultiplier = player.zza > 0 ? 1.0 : -0.5;
            // Add horizontal momentum based on look direction
            player.addDeltaMovement(new Vec3(
                    lookDirection.x * forwardSpeed * directionMultiplier,
                    0, // Handled separately or slightly modified by look pitch
                    lookDirection.z * forwardSpeed * directionMultiplier
            ));
        }
    }

    public static void serverTick() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        HolderLookup.Provider provider = server.registryAccess();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ItemStack chestPlateStack = player.getItemBySlot(EquipmentSlot.CHEST);
            if (chestPlateStack.getItem() instanceof ChestPlate chestPlateItem) {
                if (chestPlateItem.isJetpackActive(chestPlateStack)) {

                    ItemStackHandler inventory = ISpaceSuitInventory.loadInventory(chestPlateStack, provider);
                    ItemStack jetpackStack = inventory.getStackInSlot(ChestPlate.jetpack_slot);
                    if (!(jetpackStack.getItem() instanceof Jetpack)) {
                        chestPlateItem.setJetpackActive(chestPlateStack, false);
                        continue;
                    }

                    CompoundTag cachedData = chestPlateItem.getCachedData(chestPlateStack);
                    int hydrogen_required = Config.INSTANCE.jetpack_hydrogen_per_tick;
                    int oxygen_required = Config.INSTANCE.jetpack_oxygen_per_tick;
                    int hydrogen_available = cachedData.contains("hydrogen") ? cachedData.getInt("hydrogen") : 0;
                    int oxygen_available = cachedData.contains("oxygen") ? cachedData.getInt("oxygen") : 0;

                    if (hydrogen_available >= hydrogen_required && oxygen_available >= oxygen_required) {

                        // drain oxygen
                        int toDrain = oxygen_required;
                        for (int i = 0; i < inventory.getSlots(); i++) {
                            ItemStack stack = inventory.getStackInSlot(i);
                            if (stack.getItem() instanceof ItemPortablePressureTank) {
                                IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
                                if (fluidHandler.getFluidInTank(0).getFluid().equals(Fluids.OXYGEN.get())) {
                                    toDrain -= fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE).getAmount();
                                }
                            }
                        }

                        // drain hydrogen from jetpack
                        toDrain = hydrogen_required;
                        ItemStackHandler jetpackInventory = ISpaceSuitInventory.loadInventory(jetpackStack,provider);
                        for (int i = 0; i < jetpackInventory.getSlots(); i++) {
                            ItemStack stack = jetpackInventory.getStackInSlot(i);
                            if (stack.getItem() instanceof ItemPortablePressureTank) {
                                IFluidHandler fluidHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
                                if (fluidHandler.getFluidInTank(0).getFluid().equals(Fluids.HYDROGEN.get())) {
                                    toDrain -= fluidHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE).getAmount();
                                }
                            }
                        }

                        // save everything back to the itemstacks
                        // save jetpack stack first, it holds a reference to the stack in the main inventory
                        // save main inventory after jetpack is saved
                        ISpaceSuitInventory.saveInventory(jetpackInventory, jetpackStack, player.registryAccess());
                        ISpaceSuitInventory.saveInventory(inventory, chestPlateStack, player.registryAccess());

                        sharedJetpackTick(player);
                    }else{
                        // out of fuel
                        chestPlateItem.setJetpackActive(chestPlateStack, false);
                    }
                }
            }
        }
    }

    public static void clientTick() {
        Player player = ClientUtils.getSinglePlayer();
        ItemStack chestPlateStack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chestPlateStack.getItem() instanceof ChestPlate chestPlate) {
            if (ClientUtils.getOptions().keyJump.isDown()) {
                if (!chestPlate.isJetpackActive(chestPlateStack) && chestPlate.hasJetpack(chestPlateStack)) {
                    ChestPlate.ActivateJetpack.sendActivate(true);
                }
            } else {
                if(chestPlate.isJetpackActive(chestPlateStack)){
                    ChestPlate.ActivateJetpack.sendActivate(false);
                }
            }

            if(chestPlate.isJetpackActive(chestPlateStack)){
                sharedJetpackTick(player);
            }
        }
    }

    public abstract int getInventorySlots();

    public abstract boolean isItemValid(ItemStack stack, int slot);
}
