package NPCs.Items;

import NPCs.Registry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class ItemSpawnCombatNpc extends Item {
    public ItemSpawnCombatNpc() {
        super(new Properties());
    }

    public InteractionResult useOn(UseOnContext context) {
        if(context.getLevel() instanceof ServerLevel l)
            Registry.ENTITY_FIGHTER.get().spawn(l,context.getClickedPos().above(), MobSpawnType.SPAWN_EGG);
        return InteractionResult.SUCCESS;
    }

}
