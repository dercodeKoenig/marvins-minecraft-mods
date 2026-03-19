package BetterPipes;

import BetterPipes.PipeBase.EntityPipe;
import BetterPipes.Pipes.BlockIronPipe;
import BetterPipes.Pipes.BlockWoodenPipe;
import BetterPipes.Pipes.EntityIronPipe;
import BetterPipes.Pipes.EntityWoodenPipe;
import BetterPipes.Tank.BlockTank;
import BetterPipes.Tank.EntityTank;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

public class Registry {
    public static final net.neoforged.neoforge.registries.DeferredRegister<Block> BLOCKS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK, "betterpipes");
    public static final net.neoforged.neoforge.registries.DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "betterpipes");
    public static final net.neoforged.neoforge.registries.DeferredRegister<Item> ITEMS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.ITEM, "betterpipes");

    public static void registerBlockItem(String name, Supplier<Block> b){
        ITEMS.register(name,() -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<Block> IRON_PIPE = BLOCKS.register(
            "iron_pipe",
            () -> new BlockIronPipe()
    );
    public static final Supplier<BlockEntityType<EntityIronPipe>> ENTITY_IRON_PIPE = BLOCK_ENTITIES.register(
            "entity_iron_pipe",
            () -> BlockEntityType.Builder.of(EntityIronPipe::new, IRON_PIPE.get()).build(null)
    );

    public static final Supplier<Block> WOODEN_PIPE = BLOCKS.register(
            "wooden_pipe",
            () -> new BlockWoodenPipe()
    );
    public static final Supplier<BlockEntityType<EntityWoodenPipe>> ENTITY_WOODEN_PIPE = BLOCK_ENTITIES.register(
            "entity_wooden_pipe",
            () -> BlockEntityType.Builder.of(EntityWoodenPipe::new, WOODEN_PIPE.get()).build(null)
    );

    public static final Supplier<Block> TANK = BLOCKS.register(
            "tank",
            () -> new BlockTank()
    );
    public static final Supplier<BlockEntityType<EntityTank>> ENTITY_TANK = BLOCK_ENTITIES.register(
            "entity_tank",
            () -> BlockEntityType.Builder.of(EntityTank::new, TANK.get()).build(null)
    );

    public static void register(IEventBus modBus) {
        registerBlockItem("iron_pipe", IRON_PIPE);
        registerBlockItem("wooden_pipe", WOODEN_PIPE);
        registerBlockItem("tank", TANK);

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

}
