package BetterPipes;

import BetterPipes.Pipe.BlockPipe;
import BetterPipes.Pipe.EntityPipe;
import BetterPipes.Tank.BlockTank;
import BetterPipes.Tank.EntityTank;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;

import java.util.function.Supplier;

public class Registry {
    public static final net.neoforged.neoforge.registries.DeferredRegister<Block> BLOCKS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK, "betterpipes");
    public static final net.neoforged.neoforge.registries.DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, "betterpipes");
    public static final net.neoforged.neoforge.registries.DeferredRegister<Item> ITEMS = net.neoforged.neoforge.registries.DeferredRegister.create(BuiltInRegistries.ITEM, "betterpipes");

    public static void registerBlockItem(String name, Supplier<Block> b){
        ITEMS.register(name,() -> new BlockItem(b.get(), new Item.Properties()));
    }

    public static final Supplier<Block> PIPE = BLOCKS.register(
            "pipe",
            () -> new BlockPipe(BlockBehaviour.Properties.of().noOcclusion().strength(1.0f).instabreak())
    );
    public static final Supplier<BlockEntityType<EntityPipe>> ENTITY_PIPE = BLOCK_ENTITIES.register(
            "entity_pipe",
            () -> BlockEntityType.Builder.of(EntityPipe::new, PIPE.get()).build(null)
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
        registerBlockItem("pipe", PIPE);
        registerBlockItem("tank", TANK);

        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }

}
