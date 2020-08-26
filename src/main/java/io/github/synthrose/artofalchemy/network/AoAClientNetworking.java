package io.github.synthrose.artofalchemy.network;

import io.github.synthrose.artofalchemy.block.BlockPipe;
import io.github.synthrose.artofalchemy.blockentity.BlockEntityPipe;
import io.github.synthrose.artofalchemy.essentia.EssentiaContainer;
import io.github.synthrose.artofalchemy.essentia.EssentiaStack;
import io.github.synthrose.artofalchemy.gui.screen.EssentiaScreen;
import io.github.synthrose.artofalchemy.gui.screen.ScreenJournal;
import io.netty.buffer.Unpooled;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.network.ClientSidePacketRegistry;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Environment(EnvType.CLIENT)
public class AoAClientNetworking {

	@Environment(EnvType.CLIENT)
	public static void initializeClientNetworking() {
		ClientSidePacketRegistry.INSTANCE.register(AoANetworking.ESSENTIA_PACKET,
			(ctx, data) -> {
				int essentiaId = data.readInt();
				CompoundTag tag = data.readCompoundTag();
				BlockPos pos = data.readBlockPos();
				ctx.getTaskQueue().execute(() -> {
					EssentiaContainer container = new EssentiaContainer(tag);
					Screen screen = MinecraftClient.getInstance().currentScreen;
					if (screen instanceof EssentiaScreen) {
						((EssentiaScreen) screen).updateEssentia(essentiaId, container, pos);
					}
				});
			});
		
		ClientSidePacketRegistry.INSTANCE.register(AoANetworking.ESSENTIA_PACKET_REQ,
				(ctx, data) -> {
					int essentiaId = data.readInt();
					CompoundTag essentiaTag = data.readCompoundTag();
					CompoundTag requiredTag = data.readCompoundTag();
					BlockPos pos = data.readBlockPos();
					ctx.getTaskQueue().execute(() -> {
						EssentiaContainer container = new EssentiaContainer(essentiaTag);
						EssentiaStack required = new EssentiaStack(requiredTag);
						Screen screen = MinecraftClient.getInstance().currentScreen;
						if (screen instanceof EssentiaScreen) {
							((EssentiaScreen) screen).updateEssentia(essentiaId, container, required, pos);
						}
					});
				});

		ClientSidePacketRegistry.INSTANCE.register(AoANetworking.JOURNAL_REFRESH_PACKET,
				(ctx, data) -> {
					ItemStack journal = data.readItemStack();
					ctx.getTaskQueue().execute(() -> {
						Screen screen = MinecraftClient.getInstance().currentScreen;
						if (screen instanceof ScreenJournal) {
							((ScreenJournal) screen).refresh(journal);
						}
					});
				});

		ClientSidePacketRegistry.INSTANCE.register(AoANetworking.PIPE_FACE_UPDATE,
				(ctx, data) -> {
					Direction dir = data.readEnumConstant(Direction.class);
					BlockEntityPipe.IOFace face = data.readEnumConstant(BlockEntityPipe.IOFace.class);
					BlockPos pos = data.readBlockPos();
					ctx.getTaskQueue().execute(() -> {
						World world = MinecraftClient.getInstance().world;
						BlockEntity be = world.getBlockEntity(pos);
						if (be instanceof BlockEntityPipe) {
							((BlockEntityPipe) be).setFace(dir, face);
							BlockPipe.scheduleChunkRebuild(world, pos);
						}
					});
				});
	}

	public static void sendJournalSelectPacket(Identifier id, Hand hand) {
		PacketByteBuf data = new PacketByteBuf(Unpooled.buffer());
		data.writeIdentifier(id);
		data.writeEnumConstant(hand);
		ClientSidePacketRegistry.INSTANCE.sendToServer(AoANetworking.JOURNAL_SELECT_PACKET, data);
	}

}
