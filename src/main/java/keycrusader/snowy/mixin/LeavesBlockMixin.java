package keycrusader.snowy.mixin;

import net.minecraft.block.*;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.thrown.SnowballEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Random;

@Mixin(LeavesBlock.class)
public class LeavesBlockMixin extends Block {

    private static final BooleanProperty SNOWY = Properties.SNOWY;
    private static final BooleanProperty PERSISTENT_SNOWY = BooleanProperty.of("snowy_persistent");

    public LeavesBlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(at = @At("TAIL"), method = "<init>")
    public void constructor(CallbackInfo info) {
        this.setDefaultState(this.stateManager.getDefaultState().with(LeavesBlock.DISTANCE, 7).with(LeavesBlock.PERSISTENT, false).with(SNOWY, false).with(PERSISTENT_SNOWY, false));
    }

    @Inject(at = @At("TAIL"), method = "hasRandomTicks(Lnet/minecraft/block/BlockState;)Z", cancellable = true)
    public void hasRandomTicks(BlockState state, CallbackInfoReturnable<Boolean> info) {
        info.setReturnValue(true);
    }

    @Inject(at = @At("TAIL"), method = "randomTick(Lnet/minecraft/block/BlockState;Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/util/math/BlockPos;Ljava/util/Random;)V")
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo info) {
        if (world.isRaining() && world.getBiome(pos).getPrecipitation() == Biome.Precipitation.SNOW && world.getLightLevel(LightType.BLOCK, pos) < 10) {
            // Snowing and snow can settle
            if (world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, pos).getY() <= pos.getY()) {
                // Outside
                if (!state.get(SNOWY)) {
                    // Not currently snowy
                    world.setBlockState(pos, state.with(SNOWY, true));
                }
            }
        }
        else {
            // Not snowing
            if (state.get(SNOWY) && !state.get(PERSISTENT_SNOWY)) {
                // Is snowy
                if ((world.getLightLevel(LightType.SKY, pos) > 0 && world.getBiome(pos).doesNotSnow(pos)) || world.getLightLevel(LightType.BLOCK, pos) > 11) {
                    // Warm/near a light source, melt
                    world.setBlockState(pos, state.with(SNOWY, false));
                }
            }
        }
    }

    @Inject(at = @At("TAIL"), method = "Lnet/minecraft/block/LeavesBlock;appendProperties(Lnet/minecraft/state/StateManager$Builder;)V")
    public void appendProperties(StateManager.Builder<Block, BlockState> builder, CallbackInfo info) {
        builder.add(SNOWY).add(PERSISTENT_SNOWY);
    }

    @Override
    public void onProjectileHit(World world, BlockState state, BlockHitResult hit, ProjectileEntity projectile) {
        if (projectile instanceof SnowballEntity && !state.get(SNOWY)) {
            world.setBlockState(hit.getBlockPos(), state.with(SNOWY, true).with(PERSISTENT_SNOWY, true));
        }
    }
}
