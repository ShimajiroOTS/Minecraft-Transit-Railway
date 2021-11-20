package mtr.block;

import mtr.MTR;
import mtr.data.Platform;
import mtr.data.RailwayData;
import mtr.data.Route;
import mtr.packet.PacketTrainDataGuiServer;
import net.fabricmc.fabric.api.block.entity.BlockEntityClientSerializable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.*;

public class BlockTrainScheduleSensor extends Block implements BlockEntityProvider {

	public static final BooleanProperty POWERED = BooleanProperty.of("powered");

	public BlockTrainScheduleSensor(Settings settings) {
		super(settings);
		setDefaultState(getStateManager().getDefaultState().with(POWERED, false));
	}

	@Override
	public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
		return IBlock.checkHoldingBrush(world, player, () -> {
			final BlockEntity entity = world.getBlockEntity(pos);
			if (entity instanceof TileEntityTrainScheduleSensor) {
				((TileEntityTrainScheduleSensor) entity).sync();
				PacketTrainDataGuiServer.openScheduleSensorScreenS2C((ServerPlayerEntity) player, pos);
			}
		});
	}

	@Override
	public void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
		world.setBlockState(pos, state.with(POWERED, false));
	}

	@Override
	public boolean emitsRedstonePower(BlockState state) {
		return true;
	}

	@Override
	public int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
		return state.get(POWERED) ? 15 : 0;
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
		return type == MTR.TRAIN_SCHEDULE_SENSOR_TILE_ENTITY ? BlockTrainScheduleSensor.TileEntityTrainScheduleSensor::tick : null;
	}

	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
		return new TileEntityTrainScheduleSensor(pos, state);
	}

	@Override
	protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
		builder.add(POWERED);
	}

	public static class TileEntityTrainScheduleSensor extends BlockEntity implements BlockEntityClientSerializable {

		private int seconds = 10;
		private static final String KEY_SECONDS = "seconds";

		public TileEntityTrainScheduleSensor(BlockPos pos, BlockState state) {
			super(MTR.TRAIN_SCHEDULE_SENSOR_TILE_ENTITY, pos, state);
		}

		public int getSeconds() {
			return seconds;
		}

		public void setSeconds(int seconds) {
			this.seconds = seconds;
			markDirty();
			sync();
		}

		@Override
		public void readNbt(NbtCompound nbtCompound) {
			super.readNbt(nbtCompound);
			fromClientTag(nbtCompound);
		}

		@Override
		public NbtCompound writeNbt(NbtCompound nbtCompound) {
			super.writeNbt(nbtCompound);
			toClientTag(nbtCompound);
			return nbtCompound;
		}

		@Override
		public void fromClientTag(NbtCompound nbtCompound) {
			seconds = nbtCompound.getInt(KEY_SECONDS);
		}

		@Override
		public NbtCompound toClientTag(NbtCompound nbtCompound) {
			nbtCompound.putInt(KEY_SECONDS, seconds);
			return nbtCompound;
		}

		public static <T extends BlockEntity> void tick(World world, BlockPos pos, BlockState state, T blockEntity) {
			if (world != null && !world.isClient) {
				final boolean isActive = IBlock.getStatePropertySafe(state, POWERED) && world.getBlockTickScheduler().isScheduled(pos, state.getBlock());

				if (isActive || !(state.getBlock() instanceof BlockTrainScheduleSensor) || !(blockEntity instanceof BlockTrainScheduleSensor.TileEntityTrainScheduleSensor)) {
					return;
				}

				final RailwayData railwayData = RailwayData.getInstance(world);
				if (railwayData == null) {
					return;
				}

				final Platform platform = RailwayData.getClosePlatform(railwayData.platforms, pos, 4, 4, 0);
				if (platform == null) {
					return;
				}

				final Set<Route.ScheduleEntry> schedules = railwayData.getSchedulesAtPlatform(platform.id);
				if (schedules == null) {
					return;
				}

				final List<Route.ScheduleEntry> scheduleList = new ArrayList<>(schedules);
				if (!scheduleList.isEmpty()) {
					Collections.sort(scheduleList);
					if ((scheduleList.get(0).arrivalMillis - System.currentTimeMillis()) / 1000 == ((TileEntityTrainScheduleSensor) blockEntity).seconds) {
						world.setBlockState(pos, state.with(POWERED, true));
						world.getBlockTickScheduler().schedule(pos, state.getBlock(), 20);
					}
				}
			}
		}
	}
}
