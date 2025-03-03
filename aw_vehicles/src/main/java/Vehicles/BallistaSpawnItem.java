package Vehicles;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class BallistaSpawnItem extends Item {
    public BallistaSpawnItem(Properties properties) {
        super(properties);
    }

    public InteractionResult useOn(UseOnContext context) {
        Ballista ballista = new Ballista(Registry.ENTITY_BALLISTA.get(), context.getLevel());
        ballista.setYRot(context.getRotation());
        ballista.setPos(context.getClickLocation());
        context.getLevel().addFreshEntity(ballista);
        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }

}
