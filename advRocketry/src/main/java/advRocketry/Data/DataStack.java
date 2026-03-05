package advRocketry.Data;

import net.minecraft.nbt.CompoundTag;

import java.util.Objects;

public class DataStack {

    public String type;
    public int amount;

    public DataStack() {
    }

    public DataStack(String type, int amount) {
        this.type = type;
        this.amount = amount;
    }

    public static boolean isSameType(DataStack first, DataStack second) {
        return Objects.equals(first.type, second.type);
    }

    public static DataStack createFromNbt(CompoundTag tag) {
        DataStack stack = new DataStack();
        if (tag.contains("amount"))
            stack.amount = tag.getInt("amount");
        if (tag.contains("type"))
            stack.type = tag.getString("type");
        return stack;
    }

    public CompoundTag saveToNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("amount", amount);
        tag.putString("type", type);
        return tag;
    }

    public void grow(int amount) {
        this.amount += amount;
    }

    public void shrink(int amount) {
        grow(-amount);
        if (this.amount < 0)
            this.amount = 0;
    }

    public boolean isEmpty() {
        return amount == 0;
    }

    public DataStack copyWithCount(int amount) {
        return new DataStack(this.type, amount);
    }
}
