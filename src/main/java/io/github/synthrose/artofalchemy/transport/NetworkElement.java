package io.github.synthrose.artofalchemy.transport;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Set;

public interface NetworkElement {
	boolean hasNodes(World world, BlockPos pos);
	Set<NetworkNode> getNodes(World world, BlockPos pos);

	/**
	 * Query whether this element is connected to a neighbouring element.
	 *
	 * @param world the world instance.
	 * @param pos the block position identifying this element.
	 * @param dir the direction in which the other element is located.
	 *
	 * @return whether the two elements are connected.
	 */
	boolean isConnected(World world, BlockPos pos, Direction dir);

	/**
	 * Query whether this element is connected to another element.
	 *
	 * @param world the world instance.
	 * @param pos the block position identifying this element.
	 * @param other the block position identifying the other element.
	 *
	 * @return whether the two elements are connected.
	 */
	boolean isConnected(World world, BlockPos pos, BlockPos other);

	/**
	 * Query the set of surrounding blocks that have an established
	 * connection to this element.
	 *
	 * @param world the world instance.
	 * @param pos the block position identifying this element.
	 *
	 * @return the set of block positions towards which a connection is established.
	 */
	Set<BlockPos> getConnections(World world, BlockPos pos);
}
