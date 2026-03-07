package advRocketry.BlockEntities;

import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import advRocketry.Data.DataStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

abstract public class EntityMultiblockMachineMasterWithData extends EntityMultiblockMachineMaster {

    protected List<BlockPos> dataTiles = new ArrayList<>();

    public EntityMultiblockMachineMasterWithData(BlockEntityType<?> p_155228_, BlockPos p_155229_, BlockState p_155230_) {
        super(p_155228_, p_155229_, p_155230_);
    }

    @Override
    public void addStructureTiles(BlockEntity tile) {
        if (tile instanceof EntityDataStorageBlock) {
            dataTiles.add(tile.getBlockPos());
        }
        super.addStructureTiles(tile);
    }

    @Override
    public void onStructureComplete() {
        if (!this.level.isClientSide) {
            this.dataTiles.clear();
        }
        super.onStructureComplete();
    }

    /**
     * @param type       the data type
     * @param dataTiles  the list of data tiles to consider
     * @param exactMatch true: type must match exactly, false: only base type must match
     * @return amount of data of the given type
     */
    public int getData(String type, List<EntityDataStorageBlock> dataTiles, boolean exactMatch) {
        int data = 0;
        for (EntityDataStorageBlock i : dataTiles) {
            DataStack stack = i.dataStorage.getDataStack();
            if (stack != null) {
                if (exactMatch && DataStack.isSameType(type, stack.type))
                    data += stack.amount;
                if (!exactMatch && DataStack.isSameBaseType(type, stack.type))
                    data += stack.amount;
            }
        }
        return data;
    }


    /**
     * extracts data of the given type
     * @param type       the data type
     * @param dataTiles  the list of data tiles to consider
     * @param exactMatch true: type must match exactly, false: only base type must match
     * @return DataStack extracted
     */
    public DataStack extractData(String type, int toConsume, List<EntityDataStorageBlock> dataTiles, boolean exactMatch) {
        int consumed = 0;
        for (EntityDataStorageBlock i : dataTiles) {
            int remaining = toConsume - consumed;
            if (remaining == 0)
                break;
            DataStack stack = i.dataStorage.extractData(remaining, true);
            if (stack != null) {
                if (exactMatch && DataStack.isSameType(type, stack.type))
                    consumed += i.dataStorage.extractData(remaining, false).amount;
                if (!exactMatch && DataStack.isSameBaseType(type, stack.type))
                    consumed += i.dataStorage.extractData(remaining, false).amount;
            }
        }
        if (consumed == 0)
            return null;

        String targetType = type;
        if (!exactMatch)
            targetType = DataStack.split(targetType).getFirst();
        return new DataStack(targetType, consumed);
    }

    public List<EntityDataStorageBlock> getDataTiles() {
        List<EntityDataStorageBlock> tiles = new ArrayList();
        for (BlockPos pos : dataTiles) {
            if (level.isLoaded(pos)) {
                if (level.getBlockEntity(pos) instanceof EntityDataStorageBlock dataStorageBlock) {
                    tiles.add(dataStorageBlock);
                }
            }
        }
        return tiles;
    }
}
