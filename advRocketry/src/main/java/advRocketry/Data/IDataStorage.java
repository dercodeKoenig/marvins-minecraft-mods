package advRocketry.Data;

import javax.annotation.Nullable;

public interface IDataStorage {
    ///  return data amount inserted
    int insertData(DataStack dataStack, boolean simulate);

    /// return extracted DataStack
    @Nullable
    DataStack extractData(int amount, boolean simulate);

    @Nullable
    DataStack getDataStack();

    int getDataCapacity();

    boolean canExtract();

    boolean canReceive();
}
