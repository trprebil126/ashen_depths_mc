package com.ashendepths;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

public final class EmberitePortalBlock extends NetherPortalBlock {
	private static final int MAX_FRAME_SIZE = 21;
	private static final int PORTAL_UPDATE_FLAGS = 18;
	public static final ResourceKey<Level> DIMENSION_KEY =
			ResourceKey.create(Registries.DIMENSION, AshenDepths.id("ashen_depths"));

	public EmberitePortalBlock(Properties properties) {
		super(properties);
	}

	public static void registerActivationEvent() {
		UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			ItemStack heldItem = player.getItemInHand(hand);
			BlockPos clickedPos = hit.getBlockPos();
			if (!heldItem.is(Items.LAVA_BUCKET) || !level.getBlockState(clickedPos).is(ModContent.EMBERITE_BLOCK)) {
				return InteractionResult.PASS;
			}

			PortalFrame frame = findFrame(level, clickedPos);
			if (frame == null) {
				return InteractionResult.PASS;
			}

			if (!level.isClientSide()) {
				BlockState portalState = ModContent.EMBERITE_PORTAL.defaultBlockState()
						.setValue(AXIS, frame.axis());
				for (int widthOffset = 0; widthOffset < frame.width(); widthOffset++) {
					for (int heightOffset = 0; heightOffset < frame.height(); heightOffset++) {
						BlockPos portalPos = frame.bottomLeft().relative(frame.widthDirection(), widthOffset).above(heightOffset);
						level.setBlock(portalPos, portalState, PORTAL_UPDATE_FLAGS);
					}
				}

				if (!player.getAbilities().instabuild) {
					player.setItemInHand(hand, Items.BUCKET.getDefaultInstance());
				}
			}

			return InteractionResult.SUCCESS;
		});
	}

	@Override
	public TeleportTransition getPortalDestination(ServerLevel source, Entity entity, BlockPos portalPos) {
		boolean returningHome = source.dimension().equals(DIMENSION_KEY);
		ResourceKey<Level> targetKey = returningHome ? Level.OVERWORLD : DIMENSION_KEY;
		ServerLevel destination = source.getServer().getLevel(targetKey);
		if (destination == null) {
			throw new IllegalStateException("Ashen Depths portal destination is not loaded: " + targetKey.identifier());
		}

		int scale = returningHome ? 8 : 1;
		int targetX = returningHome ? safeMultiply(portalPos.getX(), scale) : Math.floorDiv(portalPos.getX(), 8);
		int targetZ = returningHome ? safeMultiply(portalPos.getZ(), scale) : Math.floorDiv(portalPos.getZ(), 8);
		Vec3 targetPosition = findOrCreatePortal(destination, targetX, targetZ);
		return new TeleportTransition(
				destination,
				targetPosition,
				entity.getDeltaMovement(),
				entity.getYRot(),
				entity.getXRot(),
				TeleportTransition.PLAY_PORTAL_SOUND
		);
	}

	private static PortalFrame findFrame(Level level, BlockPos clickedPos) {
		for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
			Direction widthDirection = Direction.get(Direction.AxisDirection.POSITIVE, axis);
			BlockPos leftFrame = clickedPos;
			int scanned = 0;
			while (scanned < MAX_FRAME_SIZE + 2
					&& isEmberiteFrame(level, leftFrame.relative(widthDirection.getOpposite()))) {
				leftFrame = leftFrame.relative(widthDirection.getOpposite());
				scanned++;
			}

			BlockPos rightFrame = leftFrame;
			scanned = 1;
			while (scanned <= MAX_FRAME_SIZE + 2 && isEmberiteFrame(level, rightFrame.relative(widthDirection))) {
				rightFrame = rightFrame.relative(widthDirection);
				scanned++;
			}

			int width = scanned - 2;
			if (width < 2 || width > MAX_FRAME_SIZE) {
				continue;
			}

			BlockPos bottomLeft = leftFrame.relative(widthDirection).above();
			for (int height = 3; height <= MAX_FRAME_SIZE; height++) {
				if (isValidFrame(level, bottomLeft, widthDirection, width, height)) {
					return new PortalFrame(bottomLeft, axis, widthDirection, width, height);
				}
			}
		}

		return null;
	}

	private static boolean isValidFrame(Level level, BlockPos bottomLeft, Direction widthDirection, int width, int height) {
		for (int x = -1; x <= width; x++) {
			BlockPos column = bottomLeft.relative(widthDirection, x);
			if (!isEmberiteFrame(level, column.below()) || !isEmberiteFrame(level, column.above(height))) {
				return false;
			}

			for (int y = 0; y < height; y++) {
				BlockPos pos = column.above(y);
				if (x == -1 || x == width) {
					if (!isEmberiteFrame(level, pos)) {
						return false;
					}
				} else if (!level.getBlockState(pos).canBeReplaced()) {
					return false;
				}
			}
		}
		return true;
	}

	private static boolean isEmberiteFrame(Level level, BlockPos pos) {
		return level.getBlockState(pos).is(ModContent.EMBERITE_BLOCK);
	}

	private static Vec3 findOrCreatePortal(ServerLevel level, int targetX, int targetZ) {
		BlockPos existingPortal = findNearbyPortal(level, targetX, targetZ);
		if (existingPortal != null) {
			return new Vec3(existingPortal.getX() + 0.5, existingPortal.getY(), existingPortal.getZ() + 0.5);
		}

		for (int radius = 0; radius <= 16; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}

					int x = targetX + dx;
					int z = targetZ + dz;
					int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
					int minimumY = Math.max(level.getMinY() + 2, surfaceY - 64);
					for (int groundY = surfaceY - 1; groundY >= minimumY; groundY--) {
						BlockPos ground = new BlockPos(x, groundY, z);
						if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
								|| !level.getFluidState(ground).isEmpty()) {
							continue;
						}

						Direction.Axis axis = Direction.Axis.X;
						BlockPos frameStart = new BlockPos(x - 2, groundY + 1, z);
						if (createPortalFrame(level, frameStart, axis)) {
							BlockPos portalBase = frameStart.relative(Direction.EAST).above();
							return new Vec3(portalBase.getX() + 1.0, portalBase.getY(), portalBase.getZ() + 0.5);
						}
					}
				}
			}
		}

		throw new IllegalStateException("Could not find safe ground for an Ashen Depths portal near " + targetX + ", " + targetZ);
	}

	private static BlockPos findNearbyPortal(ServerLevel level, int targetX, int targetZ) {
		for (int radius = 0; radius <= 16; radius++) {
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
						continue;
					}

					int x = targetX + dx;
					int z = targetZ + dz;
					int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
					int minimumY = Math.max(level.getMinY(), surfaceY - 4);
					int maximumY = Math.min(level.getMaxY(), surfaceY + 8);
					for (int y = minimumY; y < maximumY; y++) {
						BlockPos pos = new BlockPos(x, y, z);
						if (level.getBlockState(pos).is(ModContent.EMBERITE_PORTAL)) {
							return pos;
						}
					}
				}
			}
		}
		return null;
	}

	private static boolean createPortalFrame(ServerLevel level, BlockPos frameStart, Direction.Axis axis) {
		Direction widthDirection = Direction.get(Direction.AxisDirection.POSITIVE, axis);
		for (int widthOffset = 0; widthOffset < 4; widthOffset++) {
			BlockPos ground = frameStart.relative(widthDirection, widthOffset).below();
			if (!level.getBlockState(ground).isFaceSturdy(level, ground, Direction.UP)
					|| !level.getFluidState(ground).isEmpty()) {
				return false;
			}
		}

		for (int widthOffset = 1; widthOffset <= 2; widthOffset++) {
			for (int heightOffset = 1; heightOffset <= 3; heightOffset++) {
				BlockPos interior = frameStart.relative(widthDirection, widthOffset).above(heightOffset);
				level.setBlock(interior, Blocks.AIR.defaultBlockState(), 3);
			}
		}

		for (int widthOffset = 0; widthOffset < 4; widthOffset++) {
			BlockPos bottom = frameStart.relative(widthDirection, widthOffset);
			level.setBlock(bottom, ModContent.EMBERITE_BLOCK.defaultBlockState(), 3);
			level.setBlock(bottom.above(4), ModContent.EMBERITE_BLOCK.defaultBlockState(), 3);
		}
		for (int heightOffset = 1; heightOffset < 4; heightOffset++) {
			level.setBlock(frameStart.above(heightOffset), ModContent.EMBERITE_BLOCK.defaultBlockState(), 3);
			level.setBlock(frameStart.relative(widthDirection, 3).above(heightOffset), ModContent.EMBERITE_BLOCK.defaultBlockState(), 3);
		}

		BlockState portalState = ModContent.EMBERITE_PORTAL.defaultBlockState()
				.setValue(AXIS, axis);
		for (int widthOffset = 1; widthOffset <= 2; widthOffset++) {
			for (int heightOffset = 1; heightOffset <= 3; heightOffset++) {
				level.setBlock(frameStart.relative(widthDirection, widthOffset).above(heightOffset), portalState, PORTAL_UPDATE_FLAGS);
			}
		}
		return true;
	}

	private static int safeMultiply(int value, int factor) {
		return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, (long) value * factor));
	}

	private record PortalFrame(BlockPos bottomLeft, Direction.Axis axis, Direction widthDirection, int width, int height) {
	}
}
