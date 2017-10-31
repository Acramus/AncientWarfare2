package net.shadowmage.ancientwarfare.automation.render;

import net.minecraftforge.common.property.IExtendedBlockState;
import net.shadowmage.ancientwarfare.automation.block.BlockWindmillBlade;
import net.shadowmage.ancientwarfare.automation.render.property.AutomationProperties;
import net.shadowmage.ancientwarfare.automation.tile.torque.multiblock.TileWindmillBlade;
import net.shadowmage.ancientwarfare.core.render.property.CoreProperties;

public class WindmillBladeAnimationRenderer extends BaseAnimationRenderer<TileWindmillBlade> {
    @Override
    protected IExtendedBlockState handleState(TileWindmillBlade blade, float partialTicks, IExtendedBlockState state) {
        state = state.withProperty(BlockWindmillBlade.FORMED, blade.isFormed());
        state = state.withProperty(BlockWindmillBlade.IS_CONTROL, blade.isControl());
        state = state.withProperty(BlockWindmillBlade.SIZE, blade.getWindmillSize());
        state = state.withProperty(BlockWindmillBlade.ROTATION, blade.getRotation(partialTicks));
        state = state.withProperty(CoreProperties.UNLISTED_HORIZONTAL_FACING, blade.getDirection());
        state = state.withProperty(AutomationProperties.DYNAMIC, true);

        return state;
    }

    @Override
    protected int getModelHashCode(IExtendedBlockState exState) {
        return 31 + Float.floatToIntBits(exState.getValue(BlockWindmillBlade.ROTATION));
    }
}