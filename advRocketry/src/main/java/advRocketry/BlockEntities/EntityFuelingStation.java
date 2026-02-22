package advRocketry.BlockEntities;

import ARLib.blockentities.EntityFluidInputBlock;
import ARLib.gui.modules.guiModuleButton;
import ARLib.gui.modules.guiModuleEnergy;
import ARLib.network.PacketBlockEntity;
import ARLib.utils.BlockEntityBattery;
import advRocketry.Config;
import advRocketry.Items.ItemLinker;
import advRocketry.Registry;
import advRocketry.Rocket.EntityRocket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.network.PacketDistributor;

import static ARLib.gui.modules.guiModuleButton.BuiltinButtons.*;
import static advRocketry.Registry.ENTITY_FUELING_STATION;

public class EntityFuelingStation extends EntityFluidInputBlock implements ItemLinker.linkable, ItemLinker.linkableToEntity {

    public BlockPos linkedAssemblerPos = null;
    public EntityRocket linkedRocket = null;

    public BlockEntityBattery battery;

    public guiModuleButton drainFillToggleButton;
    public boolean isDrain;

    public static float maxDistance = 30;

    public EntityFuelingStation(BlockPos pos, BlockState blockState) {
        super(ENTITY_FUELING_STATION.get(), pos, blockState);

        battery = new BlockEntityBattery(this, 10000);

        guiHandler.modules.add(
                new guiModuleEnergy(11000, battery, guiHandler, 155, 7)
        );

        drainFillToggleButton = new guiModuleButton(11001, "text", guiHandler, 70, 10, 40, 15, BTN_GREEN, BTN_W, BTN_H) {
            @Override
            public void onButtonClicked() {
                CompoundTag info = new CompoundTag();
                info.put("toggleDrainFill", new CompoundTag());
                PacketDistributor.sendToServer(PacketBlockEntity.getBlockEntityPacket(EntityFuelingStation.this, info));
            }
        };

        guiHandler.modules.add(drainFillToggleButton);
    }

    @Override
    public void readServer(CompoundTag compoundTag, ServerPlayer serverPlayer) {
        super.readServer(compoundTag, serverPlayer);
        if (compoundTag.contains("toggleDrainFill")) {
            isDrain = !isDrain;
            setChanged();
        }
    }

    @Override
    public void readClient(CompoundTag compoundTag) {
        super.readClient(compoundTag);
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (linkedAssemblerPos != null)
            tag.put("linkedAssemblerPos", NbtUtils.writeBlockPos(linkedAssemblerPos));
        tag.putInt("energy", battery.getEnergyStored());
        tag.putBoolean("isDrain", isDrain);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("linkedAssemblerPos"))
            linkedAssemblerPos = NbtUtils.readBlockPos(tag, "linkedAssemblerPos").get();
        battery.setEnergy(tag.getInt("energy"));
        isDrain = tag.getBoolean("isDrain");
    }

    public void tick() {
        super.tick();
        if (!level.isClientSide) {
            if (linkedAssemblerPos != null) {
                linkedRocket = null;
                BlockEntity be = level.getBlockEntity(linkedAssemblerPos);
                if (be instanceof EntityRocketAssembler assembler) {
                    linkedRocket = assembler.currentRocket;
                } else
                    linkedAssemblerPos = null;
            }

            if (linkedRocket != null) {
                if (linkedRocket.getCurrentProgram() == null) {
                    if (battery.getEnergyStored() >= Config.INSTANCE.fueling_Station_Energy_Per_Tick) {
                        if (!isDrain) {
                            // FUEL the rocket
                            FluidStack available = myTank.drain(Config.INSTANCE.fueling_Station_Fuel_Per_Tick, FluidAction.SIMULATE);
                            int canFill = linkedRocket.fuelTank.fill(available, FluidAction.SIMULATE);
                            FluidStack drained = myTank.drain(canFill, FluidAction.EXECUTE);
                            linkedRocket.fuelTank.fill(drained, FluidAction.EXECUTE);
                            if (canFill > 0) {
                                setChanged();
                                battery.setEnergy(battery.getEnergyStored() - Config.INSTANCE.fueling_Station_Energy_Per_Tick);
                            }
                        } else {
                            // DRAIN the rocket fuel tanks
                            FluidStack available = linkedRocket.fuelTank.drain(Config.INSTANCE.fueling_Station_Fuel_Per_Tick, FluidAction.SIMULATE);
                            int canFill = myTank.fill(available, FluidAction.SIMULATE);
                            FluidStack drained = linkedRocket.fuelTank.drain(canFill, FluidAction.EXECUTE);
                            myTank.fill(drained, FluidAction.EXECUTE);
                            if (canFill > 0) {
                                setChanged();
                                battery.setEnergy(battery.getEnergyStored() - Config.INSTANCE.fueling_Station_Energy_Per_Tick);
                            }
                        }
                    }
                }
            }

            if (isDrain) {
                drainFillToggleButton.setBackgroundAndSync(BTN_BLACK, BTN_W, BTN_H);
                drainFillToggleButton.setTextAndSync("DRAIN");

                // try to output fluid to nearby fluid handlers
                if (!myTank.isEmpty()) {
                    for (Direction i : Direction.values()) {
                        IFluidHandler neighborFluidHandler = level.getCapability(Capabilities.FluidHandler.BLOCK, getBlockPos().relative(i), i.getOpposite());
                        if (neighborFluidHandler != null) {
                            int maxDrain = Config.INSTANCE.fueling_Station_Fuel_Per_Tick;
                            FluidStack available = myTank.drain(maxDrain, FluidAction.SIMULATE);
                            int canFill = neighborFluidHandler.fill(available, FluidAction.SIMULATE);
                            if (canFill > 0) {
                                neighborFluidHandler.fill(myTank.drain(canFill, FluidAction.EXECUTE), FluidAction.EXECUTE);
                                setChanged();
                            }
                        }
                    }
                }
            } else {
                drainFillToggleButton.setBackgroundAndSync(BTN_GREEN, BTN_W, BTN_H);
                drainFillToggleButton.setTextAndSync("FUEL");
            }

            if (linkedRocket != null) {
                if (linkedRocket.isRemoved() || linkedRocket.position().distanceTo(getBlockPos().getCenter()) >= maxDistance)
                    linkedRocket = null;
            }
        }
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, T t) {
        ((EntityFuelingStation) t).tick();
    }


    @Override
    public boolean link(BlockPos otherpos, Level otherLevel) {
        if (otherLevel == level) {
            Block otherBlock = level.getBlockState(otherpos).getBlock();
            if (otherBlock.equals(Registry.ROCKET_ASSEMBLER.get())) {
                if (otherpos.getCenter().distanceTo(getBlockPos().getCenter()) < maxDistance) {
                    linkedAssemblerPos = otherpos;
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean link(Entity e) {
        if (e instanceof EntityRocket rocket) {
            if (rocket.position().distanceTo(getBlockPos().getCenter()) < maxDistance) {
                if (rocket.level().equals(level)) {
                    linkedRocket = rocket;
                    linkedAssemblerPos = null;
                    return true;
                }
            }
        }
        return false;
    }
}
