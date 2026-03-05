package advRocketry.Data;

import javax.annotation.Nullable;

public interface IDataStorage {
    ///  return data amount inserted, return 0 if dataStack is null
    int insertData(@Nullable DataStack dataStack, boolean simulate);

    /// return extracted DataStack
    @Nullable
    DataStack extractData(int amount, boolean simulate);

    @Nullable
    DataStack getDataStack();

    int getDataCapacity();

    int getRemainingCapacity();

    boolean canExtract();

    boolean canReceive();
}
