package advRocketry.Blocks;

import advRocketry.Rocket.ICustomWeightBlock;
import net.minecraft.world.level.block.Block;

public class RocketMotor extends Block implements ICustomWeightBlock {
    public RocketMotor() {
        super(Properties.of().noOcclusion());
    }

    public float getThrust() {
        return 5f;
    }

    public float getFuelRateMax() {
        return 50f;
    }

    @Override
    public float getWeightMultiplier() {
        return 3;
    }
}
