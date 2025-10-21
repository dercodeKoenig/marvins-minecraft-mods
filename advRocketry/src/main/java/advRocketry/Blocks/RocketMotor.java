package advRocketry.Blocks;

import net.minecraft.world.level.block.Block;

public class RocketMotor extends Block {
    public RocketMotor() {
        super(Properties.of().noOcclusion());
    }

    public float getThrust(){
        return 10f;
    }
}
