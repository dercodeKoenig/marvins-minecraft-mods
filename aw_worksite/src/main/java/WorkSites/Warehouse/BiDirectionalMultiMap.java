package WorkSites.Warehouse;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.*;

public class BiDirectionalMultiMap<k, v> {
    private final Map<k, v> forwardMap = new HashMap<>();
    private final Map<v, LinkedHashSet<k>> reverseMap = new HashMap<>();

    public void clear(){
        forwardMap.clear();
        reverseMap.clear();
    }

    public void put(k key, v value) {
        forwardMap.put(key, value);
        reverseMap.computeIfAbsent(value, k -> new LinkedHashSet<>()).add(key);
    }

    public v getFromKey(k key) {
        return forwardMap.get(key);
    }

    public LinkedHashSet<k> getFromvalue(v value) {
        return reverseMap.getOrDefault(value, new LinkedHashSet<>());
    }

    public void removeBykey(k key) {
        v val = forwardMap.remove(key);
        if (val != null) {
            Set<k> keys = reverseMap.get(val);
            keys.remove(key);
            if (keys.isEmpty()) {
                reverseMap.remove(val);
            }
        }
    }

    public void removeByValue(v val) {
        Set<k> keys = reverseMap.remove(val);
        if (keys != null) {
            for (k key : keys) {
                forwardMap.remove(key);
            }
        }
    }
}

