package advRocketry.Data;

import com.mojang.datafixers.util.Pair;
import net.minecraft.ResourceLocationException;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

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

    // split the type into the base type (mass, distance...) and the resource location of where it was collected
    public static Pair<String, ResourceLocation> split(String type) {
        String[] parts = type.split(":", 2);
        String baseType = parts[0];
        ResourceLocation resourceLocation = null;
        if (parts.length == 2)
            resourceLocation = ResourceLocation.parse(parts[1]);
        return Pair.of(baseType, resourceLocation);
    }

    // returns the type created from base type and dimension id
    public static String join(String baseType, ResourceLocation dimId) {
        if (dimId == null)
            return baseType;
        else
            return baseType + ":" + dimId.toString();
    }

    public static boolean isSameType(DataStack first, DataStack second) {
        return isSameType(first.type, second.type);
    }

    public static boolean isSameType(String first, String second) {
        return Objects.equals(first, second);
    }

    public static boolean isSameBaseType(DataStack first, DataStack second) {
        return isSameBaseType(first.type, second.type);
    }

    public static boolean isSameBaseType(String first, String second) {
        return Objects.equals(DataStack.split(first).getFirst(), DataStack.split(second).getFirst());
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
        return amount <= 0;
    }

    public DataStack copyWithCount(int amount) {
        return new DataStack(this.type, amount);
    }
}
