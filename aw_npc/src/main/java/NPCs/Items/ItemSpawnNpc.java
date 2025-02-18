package NPCs.Items;

import NPCs.Registry;
import NPCs.WorkerNPC;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class ItemSpawnNpc extends Item {
    public ItemSpawnNpc() {
        super(new Properties());
    }

    public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel() instanceof ServerLevel l)
            Registry.ENTITY_WORKER.get().spawn(l,context.getClickedPos().above(), MobSpawnType.SPAWN_EGG);
        return InteractionResult.SUCCESS;
    }

}
