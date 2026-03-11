package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.*;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Dimension.Dimension;
import advRocketry.Dimension.DimensionManager;
import advRocketry.Dimension.PlanetDimension;
import advRocketry.Items.ItemBattery;
import advRocketry.Registry.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

import static advRocketry.Registry.BlockEntities.ENTITY_SOLAR_PANEL;

public class EntitySolarPanel extends BlockEntity implements ARLib.network.INetworkTagReceiver {

    public GuiHandlerBlockEntity guiHandler;
    public guiModuleText infoText;
    public ItemStackHandler inventory;
    public BlockEntityBattery battery;
    float partialRf = 0;


    public EntitySolarPanel(BlockPos pos, BlockState blockState) {
        super(ENTITY_SOLAR_PANEL.get(), pos, blockState);
        guiHandler = new GuiHandlerBlockEntity(this);
        battery = new BlockEntityBattery(this, 1000);
        guiHandler.modules.add(new guiModuleEnergy(0, battery, guiHandler, 10, 10));
        infoText = new guiModuleText(1, "", guiHandler, 30, 37, 0xff000000, false);
        guiHandler.modules.add(infoText);

        inventory = new ItemStackHandler(6) {
            @Override
            public void onContentsChanged(int slot) {
                setChanged();
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.getItem() instanceof ItemBattery;
            }
        };

        for (int i = 0; i < inventory.getSlots(); i++) {
            int x = 30 + i * 20;
            int y = 10;
            guiModuleItemHandlerSlot slot = new guiModuleItemHandlerSlot(100 + i, inventory, i, 0, 1, guiHandler, x, y);
            guiHandler.modules.add(slot);
        }
        guiHandler.modules.add(new guiModuleItemStackRender(99, new ItemStack(Items.ITEM_BATTERY.get(), 1), 0.9f, guiHandler, 150, 10));

        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 80, 1000, 1, 0, guiHandler));
        guiHandler.modules.addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 140, 2000, 1, 0, guiHandler));
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntitySolarPanel) t).tick();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("battery", battery.serializeNBT(registries));
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        battery.deserializeNBT(registries, tag.get("battery"));
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
    }

    public void popInventory() {
        if (!level.isClientSide) {
            for (int i = 0; i < inventory.getSlots(); i++) {
                Block.popResource(level,getBlockPos(),inventory.getStackInSlot(i));
                inventory.setStackInSlot(i, ItemStack.EMPTY);
            }
            setChanged();
        }
    }

    public float getGenerationSpeed() {

        if (!level.canSeeSky(getBlockPos().above()))
            return 0;

        ResourceLocation levelId = level.dimension().location();
        Dimension dim = DimensionManager.INSTANCE_SERVER.get(levelId);
        if (dim == null)
            return 0;

        double accumulatedStarlightIntensity = 0;
        Vec3 up;
        if (dim instanceof PlanetDimension planetDimension) {
            up = planetDimension.getGlobalAxisDirections(0, planetDimension.getLatitudeFromZPosition(getBlockPos().getZ())).up.normalize();
        } else {
            up = dim.getGlobalAxisDirections(0).up.normalize();
        }

        Vec3 myPos = dim.getPosition(0);
        for (ResourceLocation starId : dim.getCurrentMainStars()) {
            if (DimensionManager.INSTANCE_SERVER.get(starId) instanceof PlanetDimension star) {
                Vec3 planetToStar = star.getPosition(0).subtract(myPos);
                double distance = planetToStar.length();
                double dot = Math.max(0, up.dot(planetToStar.normalize()));
                double intensity = star.getRadiationIntensity();
                double atmModifier = 1 - (dim.getAtmosphereDensity() / (1 + dim.getAtmosphereDensity()));
                double finalIntensity = dot * intensity * atmModifier / (distance * distance);
                accumulatedStarlightIntensity += finalIntensity;
            }
        }

        // with normal atmosphere density (1) and sun intensity (2) and normal distance (1AU)
        // this should make it produce around 2.5rf/tick during noon
        double multiplier = 2.5;
        return (float) Math.min(10,accumulatedStarlightIntensity * multiplier);
    }

    public void tick() {
        if (!level.isClientSide) {
            guiHandler.serverTick();

            int remainingCapacity = battery.getMaxEnergyStored() - battery.getEnergyStored();
            if (remainingCapacity > 0) {
                float generationSpeed = getGenerationSpeed();
                partialRf += generationSpeed;
                int toProduce = (int) partialRf;
                partialRf -= toProduce;
                battery.receiveEnergy(toProduce, false);
                String text = battery.getEnergyStored() + " rf\n\n" +
                        (float) Math.round(generationSpeed * 1000) / 1000 + " rf / tick";
                infoText.setTextAndSync(text);
            }else{
                infoText.setTextAndSync(battery.getEnergyStored() + " rf");
            }


            int _25P = battery.getMaxEnergyStored() / 4;
            int _75P = _25P * 3;
            if(battery.getEnergyStored() > _75P + 5){
                // when mostly full, load batteries
                int batteryMaxChargePerTick = 1;
                int toTransfer = (battery.getEnergyStored() - _75P);
                int remainingToTransfer = toTransfer;
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if(stack.getItem() instanceof ItemBattery itemBattery){
                        double batteryRemainingCapacity = itemBattery.getCapacity(stack) - itemBattery.getEnergyStored(stack);
                        int maxInsert = Math.min((int)batteryRemainingCapacity, remainingToTransfer);
                        maxInsert = Math.min(batteryMaxChargePerTick, maxInsert);
                       remainingToTransfer -= Math.round(itemBattery.receiveEnergy(stack, maxInsert));
                    }
                }
                int extracted = toTransfer - remainingToTransfer;
                battery.extractEnergy(extracted, false);

            }else if(battery.getEnergyStored() < _25P - 5) {
                // when mostly empty, drain from batteries into internal storage
                int batteryMaxDrainPerTick = 2;
                int toTransfer = (_25P - battery.getEnergyStored());
                int remainingToTransfer = toTransfer;
                for (int i = 0; i < inventory.getSlots(); i++) {
                    ItemStack stack = inventory.getStackInSlot(i);
                    if (stack.getItem() instanceof ItemBattery itemBattery) {
                        if (itemBattery.getEnergyStored(stack) > 1) {
                            remainingToTransfer -= Math.round(itemBattery.extractEnergy(stack, Math.min(batteryMaxDrainPerTick, remainingToTransfer)));
                        }
                    }
                }
                int received = toTransfer - remainingToTransfer;
                battery.receiveEnergy(received, false);
            }

            // output to other energy handlers
            for (Direction i : Direction.allShuffled(level.random)) {
                if (i == Direction.UP)
                    continue;
                if(battery.getEnergyStored() == 0)
                    break;

                IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, getBlockPos().relative(i), i.getOpposite());
                if (neighbor instanceof BlockEntityBattery otherBattery && otherBattery.parent instanceof EntitySolarPanel otherPanel) {
                    int otherPanelEnergy = otherPanel.battery.getEnergyStored();
                    int myEnergy = battery.getEnergyStored();
                    int diff = myEnergy - otherPanelEnergy;
                    if(diff > 5){
                        // move some into the other panel
                        int toMove = diff / 2;
                        int received = neighbor.receiveEnergy(toMove,false);
                        battery.extractEnergy(received, false);
                    }
                } else if (neighbor != null) {
                    int received = neighbor.receiveEnergy(battery.getEnergyStored(),false);
                    battery.extractEnergy(received, false);
                }
            }
        }
    }

    public void openGui() {
        if (level.isClientSide)
            guiHandler.openGui(176, 168, true);
    }
}
