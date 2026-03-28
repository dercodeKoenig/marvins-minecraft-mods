package advRocketry.BlockEntities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleEnergy;
import ARLib.gui.modules.guiModulePlayerInventorySlot;
import ARLib.network.INetworkTagReceiver;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockEntityBattery;
import ARLib.utils.SimpleFluidContainer;
import advRocketry.Config;
import advRocketry.Items.ItemLinker;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Blocks.RocketFluidLoader.IS_DRAIN;
import static advRocketry.Registry.BlockEntities.ENTITY_ROCKET_FLUID_LOADER;

public class EntityRocketFluidLoader extends EntityRocketInfrastructureBase implements INetworkTagReceiver {

    public static int TRANSFER_SPEED = 50;

    public BlockEntityBattery battery;
    public FluidTank tank;
    public ItemStackHandler fluidHandlerItemInventory;
    public SimpleFluidContainer simpleFluidContainer;

    public GuiHandlerBlockEntity guiHandler;
    public guiModuleButton drainFillToggleButton;

    public boolean shouldOutputSignal = false;


    public EntityRocketFluidLoader(BlockPos pos, BlockState blockState) {
        super(ENTITY_ROCKET_FLUID_LOADER.get(), pos, blockState);

        guiHandler = new GuiHandlerBlockEntity(this);

        fluidHandlerItemInventory = new ItemStackHandler(4) {
            public void onContentsChanged(int slot) {
                setChanged();
            }
        };
        tank = new FluidTank(4000) {
            @Override
            public void onContentsChanged() {
                setChanged();
            }
        };

        simpleFluidContainer = new SimpleFluidContainer(tank, fluidHandlerItemInventory);
        guiHandler.modules.addAll(simpleFluidContainer.makeGuiModules(0, 10, 10, guiHandler));

        guiHandler.getModules().addAll(guiModulePlayerInventorySlot.makePlayerHotbarModules(7, 125, 100, 0, 1, guiHandler));
        guiHandler.getModules().addAll(guiModulePlayerInventorySlot.makePlayerInventoryModules(7, 65, 200, 0, 1, guiHandler));

        battery = new BlockEntityBattery(this, 10000);
        guiHandler.modules.add(new guiModuleEnergy(11000, battery, guiHandler, 155, 7));

        drainFillToggleButton = new guiModuleButton(11001, "text", guiHandler, 70, 10, 40, 15, BTN_GREEN, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.put("toggleDrainFill", new CompoundTag());
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityRocketFluidLoader.this, info));
            }
        };

        guiHandler.modules.add(drainFillToggleButton);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityRocketFluidLoader) t).tick();
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        guiHandler.readServer(compoundTag);
        if (compoundTag.contains("toggleDrainFill")) {
            level.setBlock(getBlockPos(), getBlockState().setValue(IS_DRAIN, !getBlockState().getValue(IS_DRAIN)), 3);
            setChanged();
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        guiHandler.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("battery", battery.serializeNBT(registries));
        simpleFluidContainer.saveAdditional(tag, registries);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        battery.deserializeNBT(registries, tag.get("battery"));
        simpleFluidContainer.loadAdditional(tag, registries);
    }

    public void popInventory() {
        if (!level.isClientSide) {
            simpleFluidContainer.popItems(level, getBlockPos());
            setChanged();
        }
    }

    public void setOutputSignal(boolean signal) {
        if (signal != shouldOutputSignal) {
            shouldOutputSignal = signal;
            setChanged();
        }
    }

    /// tries to load fluid into the rocket
    /// returns 1 if success, 0 if there is no fluid to load, -1 if no fluid could be loaded because rocket is full
    public int loadFluid(EntityRocket linkedRocket) {
        if (tank.isEmpty())
            return 0;

        FluidStack canExtract = tank.drain(TRANSFER_SPEED, IFluidHandler.FluidAction.SIMULATE);
        for (BlockEntity be : linkedRocket.blockEntities.values()) {
            if (be instanceof EntityPressureTank pressureTank) {
                int canFill = pressureTank.tank.fill(canExtract, IFluidHandler.FluidAction.SIMULATE);
                if (canFill > 0) {
                    pressureTank.tank.fill(tank.drain(canFill, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    linkedRocket.onBlockEntityChanged(pressureTank.getBlockPos());
                    return 1;
                }
            }
        }
        return -1;
    }


    /// tries to load fluid from the rocket into the internal inventory
    /// returns: 1 if success, 0 if rocket has no fluid, -1 if rocket has fluid but we could not unload them
    public int unloadFluid(EntityRocket linkedRocket) {
        boolean isEmpty = true;
        for (BlockEntity be : linkedRocket.blockEntities.values()) {
            if (be instanceof EntityPressureTank pressureTank) {
                FluidStack canExtract = pressureTank.tank.drain(TRANSFER_SPEED, IFluidHandler.FluidAction.SIMULATE);
                if (!canExtract.isEmpty())
                    isEmpty = false;
                int canFill = tank.fill(canExtract, IFluidHandler.FluidAction.SIMULATE);
                if (canFill > 0) {
                    tank.fill(pressureTank.tank.drain(canFill, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    linkedRocket.onBlockEntityChanged(pressureTank.getBlockPos());
                    return 1;
                }
            }
        }
        if (isEmpty)
            return 0;
        else
            return -1;
    }

    public void tick() {

        if (!level.isClientSide) {
            guiHandler.serverTick();
            super.serverTick();

            simpleFluidContainer.performPossibleFluidTransfer();

            boolean isDrain = getBlockState().getValue(IS_DRAIN);

            if (linkedRocket != null) {
                if (linkedRocket.getCurrentProgram() == null) {
                    if (battery.getEnergyStored() >= Config.INSTANCE.fluid_Loader_Energy_Per_Tick) {
                        if (!isDrain) {
                            // FILL the rocket

                            int res = loadFluid(linkedRocket);
                            // -1 = rocket full
                            // 0 = no fluid loaded ( fluid loader empty )
                            // 1 = fluid was loaded
                            if (res != -1) {
                                setOutputSignal(false);
                                if (res == 1) // load success
                                    battery.extractEnergy(Config.INSTANCE.fluid_Loader_Energy_Per_Tick, false);
                            } else {
                                // loading failed because rocket is full
                                setOutputSignal(true);
                            }
                        } else {
                            // DRAIN the rocket
                            int res = unloadFluid(linkedRocket);
                            // -1 = unload fail ( inventory full )
                            // 1 = fluid unloaded
                            // 0 = rocket empty
                            if (res != 0) {
                                setOutputSignal(false);
                                if (res == 1) // unload success
                                    battery.extractEnergy(Config.INSTANCE.fluid_Loader_Energy_Per_Tick, false);
                            } else {
                                // rocket is empty
                                setOutputSignal(true);
                            }
                        }
                    }
                }
            }

            /*
            // when drain, output fluid to nearby fluid handlers
            if (isDrain) {
                if (!tank.isEmpty()) {
                    for (Direction i : Direction.values()) {
                        IFluidHandler fluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, getBlockPos().relative(i), i.getOpposite());
                        if (fluidHandler != null) {
                            FluidStack canExtract = tank.drain(TRANSFER_SPEED, IFluidHandler.FluidAction.SIMULATE);
                            int canFill = fluidHandler.fill(canExtract, IFluidHandler.FluidAction.SIMULATE);
                            if (canFill > 0) {
                                fluidHandler.fill(tank.drain(canFill, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                            }
                        }
                    }
                }
            }
             */

            if (isDrain) {
                drainFillToggleButton.setBackgroundAndSync(BTN_BLACK, BTN_W, BTN_H);
                drainFillToggleButton.setTextAndSync("UNLOAD");
            } else {
                drainFillToggleButton.setBackgroundAndSync(BTN_GREEN, BTN_W, BTN_H);
                drainFillToggleButton.setTextAndSync("LOAD");
            }
        }
    }
}
