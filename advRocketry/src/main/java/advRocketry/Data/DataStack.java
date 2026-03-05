package advRocketry.Data;

import net.minecraft.nbt.CompoundTag;

import javax.annotation.Nullable;
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

    @Nullable
    public static DataStack createFromNbt(CompoundTag tag) {
        if (tag.contains("amount") && tag.contains("type")) {
            DataStack stack = new DataStack();
            stack.amount = tag.getInt("amount");
            stack.type = tag.getString("type");
            return stack;
        }
        return null;
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
