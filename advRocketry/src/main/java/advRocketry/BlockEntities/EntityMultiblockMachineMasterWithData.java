package advRocketry.BlockEntities;

import ARLib.blockentities.EntityEnergyInputBlock;
import ARLib.multiblockCore.EntityMultiblockMachineMaster;
import advRocketry.Data.DataStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.core.appender.db.jdbc.DataSourceConnectionSource;

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

    public int getData(String type, List<EntityDataStorageBlock> dataTiles){
        int data = 0;
        for(EntityDataStorageBlock i : dataTiles){
            DataStack stack = i.dataStorage.getDataStack();
            if(stack != null && stack.type.equals(type)){
                data+=stack.amount;
            }
        }
        return data;
    }

    public void consumeData(String type, int toConsume, List<EntityDataStorageBlock> dataTiles) {
        int consumed = 0;
        for (EntityDataStorageBlock i : dataTiles) {
            int remaining = toConsume - consumed;
            if(remaining == 0)
                return;
            DataStack stack = i.dataStorage.extractData(remaining, true);
            if (stack != null && stack.type.equals(type)) {
                consumed += i.dataStorage.extractData(remaining, false).amount;
            }
        }
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
