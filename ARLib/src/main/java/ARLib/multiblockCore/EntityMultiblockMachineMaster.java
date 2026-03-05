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
    // do not store the entities, store the positions
    // the blockentities can unload if the neighbor chunk is not loaded
    // they need to be fetched fresh every tick when required
    // check if the chunk is loaded before fetching blockEntity, or it could cause every tick the chunks to force load
    protected List<BlockPos> energyOutTiles = new ArrayList<>();
    protected List<BlockPos> energyInTiles = new ArrayList<>();
    protected List<BlockPos> itemInTiles = new ArrayList<>();
    protected List<BlockPos> itemOutTiles = new ArrayList<>();
    protected List<BlockPos> fluidInTiles = new ArrayList<>();
    protected List<BlockPos> fluidOutTiles = new ArrayList<>();

    public EntityMultiblockMachineMaster(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    public List<EntityEnergyInputBlock> getEnergyInputTiles() {
        List<EntityEnergyInputBlock> tiles = new ArrayList<>();
        for (BlockPos pos : energyInTiles) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EntityEnergyInputBlock i)
                tiles.add(i);
        }
        return tiles;
    }

    public List<EntityEnergyOutputBlock> getEnergyOutputTiles() {
        List<EntityEnergyOutputBlock> tiles = new ArrayList<>();
        for (BlockPos pos : energyOutTiles) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EntityEnergyOutputBlock i)
                tiles.add(i);
        }
        return tiles;
    }

    public List<EntityItemInputBlock> getItemInTiles() {
        List<EntityItemInputBlock> tiles = new ArrayList<>();
        for (BlockPos pos : itemInTiles) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EntityItemInputBlock i) {
                tiles.add(i);
            }
        }
        return tiles;
    }

    public List<EntityItemOutputBlock> getItemOutTiles() {
        List<EntityItemOutputBlock> tiles = new ArrayList<>();
        for (BlockPos pos : itemOutTiles) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EntityItemOutputBlock i) {
                tiles.add(i);
            }
        }
        return tiles;
    }

    public List<EntityFluidInputBlock> getFluidInTiles() {
        List<EntityFluidInputBlock> tiles = new ArrayList<>();
        for (BlockPos pos : fluidInTiles) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EntityFluidInputBlock i) {
                tiles.add(i);
            }
        }
        return tiles;
    }

    public List<EntityFluidOutputBlock> getFluidOutTiles() {
        List<EntityFluidOutputBlock> tiles = new ArrayList<>();
        for (BlockPos pos : fluidOutTiles) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof EntityFluidOutputBlock i) {
                tiles.add(i);
            }
        }
        return tiles;
    }

    public int getTotalEnergyStored(List<EntityEnergyInputBlock> energyInTiles) {
        int totalEnergy = 0;
        for (EntityEnergyInputBlock i : energyInTiles) {
            totalEnergy += i.energyStorage.getEnergyStored();
        }
        return totalEnergy;
    }

    public int getMaxEnergyStored(List<EntityEnergyInputBlock> energyInTiles) {
        int totalEnergy = 0;
        for (EntityEnergyInputBlock i : energyInTiles) {
            totalEnergy += i.energyStorage.getMaxEnergyStored();
        }
        return totalEnergy;
    }

    public void consumeEnergy(int energyToConsume, List<EntityEnergyInputBlock> energyInTiles) {
        int consumed = 0;
        for (EntityEnergyInputBlock i : energyInTiles) {
            consumed += i.energyStorage.extractEnergy(energyToConsume - consumed, false);
            if (consumed == energyToConsume) {
                return;
            }
        }
    }

    public ItemFluidStacks consumeInput(List<RecipePartWithProbability> inputs, boolean simulate, List<EntityFluidInputBlock> fluidInTiles, List<EntityItemInputBlock> itemInTiles) {
        ItemFluidStacks consumedElements = new ItemFluidStacks();
        for (RecipePartWithProbability input : inputs) {
            String identifier = input.id;
            int totalToConsume = input.getRandomAmount();
            if (totalToConsume > 0) {
                ItemFluidStacks ret = InventoryUtils.consumeElements(fluidInTiles.stream().map(x -> x.myTank).toList(), itemInTiles.stream().map(x -> x.inventory).toList(), identifier, totalToConsume, simulate);
                consumedElements.fluidStacks.addAll(ret.fluidStacks);
                consumedElements.itemStacks.addAll(ret.itemStacks);
            }
        }
        return consumedElements;
    }


    public void produceOutput(List<RecipePartWithProbability> outputs, List<EntityFluidOutputBlock> fluidOutTiles, List<EntityItemOutputBlock> itemOutTiles) {
        for (RecipePartWithProbability output : outputs) {
            String identifier = output.id;
            int totalToProduce = output.getRandomAmount();
            if (totalToProduce > 0) {
                InventoryUtils.createElements(fluidOutTiles.stream().map(x -> x.myTank).toList(), itemOutTiles.stream().map(x -> x.inventory).toList(), identifier, totalToProduce, level.registryAccess());
            }
        }
    }

    public boolean hasinputs(List<RecipePart> inputs, List<EntityFluidInputBlock> fluidInTiles, List<EntityItemInputBlock> itemInTiles) {
        return InventoryUtils.hasInputs(itemInTiles.stream().map(x -> x.inventory).toList(), fluidInTiles.stream().map(x -> x.myTank).toList(), inputs);
    }

    public boolean canFitOutputs(List<RecipePart> outputs, List<EntityFluidOutputBlock> fluidOutTiles, List<EntityItemOutputBlock> itemOutTiles) {
        return InventoryUtils.canFitElements(itemOutTiles.stream().map(x -> x.inventory).toList(), fluidOutTiles.stream().map(x -> x.myTank).toList(), outputs, level.registryAccess());
    }


    public void addStructureTiles(BlockEntity tile) {
        // make sure order is correct, out tiles extend in tiles!
        if (tile instanceof EntityEnergyOutputBlock t)
            energyOutTiles.add(t.getBlockPos());
        else if (tile instanceof EntityEnergyInputBlock t)
            energyInTiles.add(t.getBlockPos());
        else if (tile instanceof EntityItemOutputBlock t)
            itemOutTiles.add(t.getBlockPos());
        else if (tile instanceof EntityItemInputBlock t)
            itemInTiles.add(t.getBlockPos());
        else if (tile instanceof EntityFluidOutputBlock t)
            fluidOutTiles.add(t.getBlockPos());
        else if (tile instanceof EntityFluidInputBlock t)
            fluidInTiles.add(t.getBlockPos());
    }

    public void scan_tiles() {
        Object[][][] structure = getStructure();
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
    public void onStructureComplete() {
        if (!level.isClientSide) {
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
