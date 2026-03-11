package advRocketry.Data;

import net.minecraft.core.Direction;

public interface IDataStorageProvider {
    IDataStorage getDataStorage(Direction face);
}
