package Vehicles.Ballista;

import Vehicles.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

import static Vehicles.Ballista.Ballista.CONSTRUCTION_PROGRESS;
import static Vehicles.Ballista.Ballista.IS_BROKEN;
import static Vehicles.Utils.getStackTagOrEmpty;

public class BallistaSpawnItem extends Item {
    public BallistaSpawnItem(Properties properties) {
        super(properties);
    }

    public InteractionResult useOn(UseOnContext context) {
        Ballista ballista = new Ballista(Registry.ENTITY_BALLISTA.get(), context.getLevel());
        ballista.setYRot(context.getRotation());
        ballista.setPos(context.getClickLocation());
        context.getLevel().addFreshEntity(ballista);

        CompoundTag tag = getStackTagOrEmpty(context.getItemInHand());
        if(tag.contains("isBroken"))
            ballista.getEntityData().set(IS_BROKEN, tag.getBoolean("isBroken"));
        if(tag.contains("constructionProgress"))
            ballista.getEntityData().set(CONSTRUCTION_PROGRESS, tag.getInt("constructionProgress"));

        context.getLevel().playSound(null,context.getClickedPos(), SoundEvents.WOOD_PLACE, SoundSource.BLOCKS,1,1);

        context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS;
    }



}
