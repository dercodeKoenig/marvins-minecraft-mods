package advRocketry.Data;

import net.minecraft.nbt.CompoundTag;

public class DataStack {

    public DataType type;
    public int amount;

    public DataStack() {
    }

    public DataStack(DataType type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public static boolean isSameType(DataStack first, DataStack second) {
        if(first == null || second == null)
            return false;
        return first.type == second.type;
    }

    public static DataStack createFromNbt(CompoundTag tag) {
        DataStack stack = new DataStack();
        if (tag.contains("amount"))
            stack.amount = tag.getInt("amount");
        if (tag.contains("type"))
            stack.type = DataType.values()[tag.getInt("type")];
        return stack;
    }

    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("amount", amount);
        tag.putInt("type", type.ordinal());
        return tag;
    }

    public void grow(int amount) {
        this.amount += amount;
    }

    public void shrink(int amount) {
        grow(-amount);
        if(this.amount < 0)
            this.amount = 0;
    }

    public DataStack copyWithCount(int amount){
        return new DataStack(this.type, amount);
    }

    public enum DataType {
        distance,
        mass,
        composition
    }
}
