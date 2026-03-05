package advRocketry.Data;

import net.minecraft.nbt.CompoundTag;

public class DataStorage implements IDataStorage {
    DataStack stack = null;
    int capacity;

    public DataStorage(int capacity) {
        this.capacity = capacity;
    }

    public void setStackDirect(DataStack stack) {
        this.stack = stack;
    }

    public void onChange() {

    }

    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("capacity", capacity);
        if (stack != null)
            tag.put("stack", stack.saveToNbt());
        return tag;
    }

    public void readFromNbt(CompoundTag tag) {
        capacity = tag.getInt("capacity");
        if (tag.contains("stack"))
            stack = DataStack.createFromNbt(tag.getCompound("stack"));
    }

    @Override
    public int insertData(DataStack stackToInsert, boolean simulate) {
        if (!canReceive())
            return 0;
        if (stackToInsert == null)
            return 0;
        if (stack != null && !DataStack.isSameType(stackToInsert, stack))
            return 0;

        int existing = 0;
        if (stack != null)
            existing = stack.amount;
        int toInsert = Math.min(stackToInsert.amount, capacity - existing);

        if (!simulate && toInsert > 0) {
            if (stack == null)
                stack = new DataStack(stackToInsert.type, toInsert);
            else
                stack.grow(toInsert);
            onChange();
        }
        return toInsert;
    }

    @Override
    public DataStack extractData(int amount, boolean simulate) {
        if (!canExtract())
            return null;
        if (stack == null)
            return null;

        int existing = stack.amount;
        int toExtract = Math.min(amount, existing);

        if (!simulate && toExtract > 0) {
            stack.shrink(toExtract);
            if (stack.amount == 0)
                stack = null;
            onChange();
        }
        return new DataStack(stack.type, toExtract);
    }

    @Override
    public DataStack getDataStack() {
        return stack;
    }

    @Override
    public int getDataCapacity() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}
