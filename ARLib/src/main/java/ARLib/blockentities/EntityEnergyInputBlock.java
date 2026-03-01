package ARLib.blockentities;

import ARLib.gui.GuiHandlerBlockEntity;
import ARLib.gui.modules.guiModuleEnergy;
import ARLib.utils.BlockEntityBattery;
import ARLib.network.INetworkTagReceiver;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import static ARLib.ARLibRegistry.ENTITY_ENERGY_INPUT_BLOCK;

public class EntityEnergyInputBlock extends BlockEntity implements INetworkTagReceiver {

    public BlockEntityBattery energyStorage;
    public GuiHandlerBlockEntity guiHandler;

    public EntityEnergyInputBlock(BlockPos p_155229_, BlockState p_155230_) {
        this(ENTITY_ENERGY_INPUT_BLOCK.get(), p_155229_, p_155230_);
    }

    public EntityEnergyInputBlock(BlockEntityType type, BlockPos p_155229_, BlockState p_155230_) {
        super(type, p_155229_, p_155230_);
        energyStorage = new BlockEntityBattery(this, 10000);
        energyStorage.canReceive = true;
        energyStorage.canExtract = false;
        this.guiHandler = new GuiHandlerBlockEntity(this);
        this.guiHandler.getModules().add(new guiModuleEnergy(0, energyStorage, this.guiHandler, 10, 10));
    }


    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(registries, tag.get("Energy"));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Energy", energyStorage.serializeNBT(registries));
    }

    @Override
    public void readServer(CompoundTag tagIn, ServerPlayer p) {
        this.guiHandler.readServer(tagIn);
    }

    @Override
    public void readClient(CompoundTag tagIn) {
        this.guiHandler.readClient(tagIn);
    }

    public void signalOpenGui(ServerPlayer player) {
        guiHandler.signalOpenGui(player, 100, 74, true);
    }

    public static <x extends BlockEntity> void tick(Level level, BlockPos blockPos, BlockState blockState, x t) {
        if (!level.isClientSide)
            ((EntityEnergyInputBlock) t).guiHandler.serverTick();
    }

}
