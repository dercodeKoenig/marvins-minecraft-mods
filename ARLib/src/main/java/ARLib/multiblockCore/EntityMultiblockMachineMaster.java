package ARLib.multiblockCore;

import ARLib.blockentities.*;
import ARLib.utils.InventoryUtils;
import ARLib.utils.ItemFluidStacks;
import ARLib.utils.RecipePart;
import ARLib.utils.RecipePartWithProbability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.ArrayList;
import java.util.List;

public abstract class EntityMultiblockMachineMaster extends EntityMultiblockMaster {
    protected List<EntityEnergyOutputBlock> energyOutTiles = new ArrayList<>();
    protected List<EntityEnergyInputBlock> energyInTiles = new ArrayList<>();
    protected List<EntityItemInputBlock> itemInTiles = new ArrayList<>();
    protected List<EntityItemOutputBlock> itemOutTiles = new ArrayList<>();
    protected List<EntityFluidInputBlock> fluidInTiles = new ArrayList<>();
    protected List<EntityFluidOutputBlock> fluidOutTiles = new ArrayList<>();

    public EntityMultiblockMachineMaster(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }


    public int getTotalEnergyStored() {
        int totalEnergy = 0;
        for (IEnergyStorage i : energyInTiles) {
            totalEnergy += i.getEnergyStored();
        }
        return totalEnergy;
    }

    public int getMaxEnergyStored() {
        int totalEnergy = 0;
        for (IEnergyStorage i : energyInTiles) {
            totalEnergy += i.getMaxEnergyStored();
        }
        return totalEnergy;
    }

    public void consumeEnergy(int energyToConsume) {
        int consumed = 0;
        for (IEnergyStorage i : energyInTiles) {
            consumed += i.extractEnergy(energyToConsume - consumed, false);
            if (consumed == energyToConsume) {
                return;
            }
        }
    }

    public ItemFluidStacks consumeInput(List<RecipePartWithProbability> inputs, boolean simulate) {
        ItemFluidStacks consumedElements = new ItemFluidStacks();
        for (RecipePartWithProbability input : inputs) {
            String identifier = input.id;
            int totalToConsume = input.getRandomAmount();
            if (totalToConsume > 0) {
                ItemFluidStacks ret = InventoryUtils.consumeElements(this.fluidInTiles, this.itemInTiles, identifier, totalToConsume, simulate);
                consumedElements.fluidStacks.addAll(ret.fluidStacks);
                consumedElements.itemStacks.addAll(ret.itemStacks);
            }
        }
        return consumedElements;
    }


    public void produceOutput(List<RecipePartWithProbability> outputs) {
        for (RecipePartWithProbability output : outputs) {
            String identifier = output.id;
            int totalToProduce = output.getRandomAmount();
            if (totalToProduce > 0) {
                InventoryUtils.createElements(this.fluidOutTiles, this.itemOutTiles, identifier, totalToProduce, level.registryAccess());
            }
        }
    }

    // both using the max possible inputs/outputs for p >= 1
    public boolean hasinputs(List<RecipePart> inputs) {
        return InventoryUtils.hasInputs(this.itemInTiles, this.fluidInTiles, inputs);
    }

    public boolean canFitOutputs(List<RecipePart> outputs) {
        return InventoryUtils.canFitElements(this.itemOutTiles, this.fluidOutTiles, outputs, level.registryAccess());
    }

void checkTilesStillValidAndRescan(){
    for(EntityEnergyOutputBlock i:energyOutTiles){
        if(i.isRemoved()){
            scan_tiles();
            return;
        }
    }
    for(EntityEnergyInputBlock i:energyInTiles){
        if(i.isRemoved()){
            scan_tiles();
            return;
        }
    }

    for(EntityItemInputBlock i:itemInTiles){
        if(i.isRemoved()){
            scan_tiles();
            return;
        }
    }
    for(EntityItemOutputBlock i:itemOutTiles){
        if(i.isRemoved()){
            scan_tiles();
            return;
        }
    }

    for(EntityFluidInputBlock i:fluidInTiles){
        if(i.isRemoved()){
            scan_tiles();
            return;
        }
    }
    for(EntityFluidOutputBlock i:fluidOutTiles){
        if(i.isRemoved()){
            scan_tiles();
            return;
        }
    }

}

    void addStructureTiles(BlockEntity tile) {
        // make sure order is correct, out tiles extend in tiles!
        if (tile instanceof EntityEnergyOutputBlock t)
            energyOutTiles.add(t);
        else if (tile instanceof EntityEnergyInputBlock t)
            energyInTiles.add(t);
        else if (tile instanceof EntityItemOutputBlock t)
            itemOutTiles.add(t);
        else if (tile instanceof EntityItemInputBlock t)
            itemInTiles.add(t);
        else if (tile instanceof EntityFluidOutputBlock t)
            fluidOutTiles.add(t);
        else if (tile instanceof EntityFluidInputBlock t)
            fluidInTiles.add(t);
    }

    void scan_tiles() {
        Object[][][] structure = getStructure();
        boolean[][][] hideBlocks = hideBlocks();
        Direction front = getFront();
        if (front == null) return;

        Vec3i offset = getControllerOffset(structure);

        for (int y = 0; y < structure.length; y++) {
            for (int z = 0; z < structure[y].length; z++) {
                for (int x = 0; x < structure[y][z].length; x++) {
                    int globalX = getBlockPos().getX() + (x - offset.getX()) * front.getStepZ() - (z - offset.getZ()) * front.getStepX();
                    int globalY = getBlockPos().getY() - y + offset.getY();
                    int globalZ = getBlockPos().getZ() - (x - offset.getX()) * front.getStepX() - (z - offset.getZ()) * front.getStepZ();
                    BlockPos globalPos = new BlockPos(globalX, globalY, globalZ);

                    addStructureTiles(level.getBlockEntity(globalPos));
                }
            }
        }
    }

    @Override
    public void onStructureComplete(){
        if(!level.isClientSide){
            energyInTiles.clear();
            energyOutTiles.clear();
            itemInTiles.clear();
            itemOutTiles.clear();
            fluidInTiles.clear();
            fluidOutTiles.clear();

            scan_tiles();
        }
    }
}
