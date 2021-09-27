package com.cumulusmc.artofalchemy.gui.handler;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.WBar.Direction;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import com.cumulusmc.artofalchemy.ArtOfAlchemy;
import com.cumulusmc.artofalchemy.essentia.EssentiaContainer;
import com.cumulusmc.artofalchemy.transport.HasEssentia;
import com.cumulusmc.artofalchemy.gui.widget.WEssentiaPanel;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class HandlerDistiller extends SyncedGuiDescription {

	final BlockPos pos;
	final WEssentiaPanel essentiaPanel;
	
	final int SIZE_FACTOR = 18;
	final int PAD = SIZE_FACTOR * 2;
	final int WIDTH = SIZE_FACTOR * 9;
	final int HEIGHT = (SIZE_FACTOR * 7) + 2; // KG: Why +2?
	final int ITEM_HEIGHT = SIZE_FACTOR * 3;
	final int FUEL_SIZE = (int) (SIZE_FACTOR * 0.8);
	final int FUEL_SIZE_PAD = (int) ((SIZE_FACTOR - FUEL_SIZE) / 2);

	@SuppressWarnings("MethodCallSideOnly")
	public HandlerDistiller(int syncId, PlayerInventory playerInventory, ScreenHandlerContext ctx) {
		super(AoAHandlers.DISTILLER, syncId, playerInventory, getBlockInventory(ctx), getBlockPropertyDelegate(ctx));

		pos = ctx.get((world, pos) -> pos, null);

		WGridPanel root = new WGridPanel(1);
		setRootPanel(root);
		root.setSize(WIDTH, HEIGHT);

		WSprite background = new WSprite(new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/rune_bg.png"));
		root.add(background, 0, 0, WIDTH, 5 * SIZE_FACTOR);

		WItemSlot azothSlot = WItemSlot.of(blockInventory, 0);
		root.add(azothSlot, PAD, SIZE_FACTOR * 1);
		
		WItemSlot fuelSlot = WItemSlot.of(blockInventory, 1);
		root.add(fuelSlot, PAD, SIZE_FACTOR * 3);
		
		WBar fuelBar = new WBar(new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/fire_off.png"),
				new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/fire_on.png"),
				2, 3, Direction.UP);
		
		root.add(fuelBar, PAD + FUEL_SIZE_PAD, PAD + FUEL_SIZE_PAD, FUEL_SIZE, FUEL_SIZE);

		WBar essentiaTankBar = new WBar(
			new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/tank_empty.png"),
			new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/tank_full.png"),
			4,
			6,
			Direction.UP
		);
		essentiaTankBar.withTooltip("gui." + ArtOfAlchemy.MOD_ID + ".mixed_essentia_tooltip");
		root.add(essentiaTankBar, 0, SIZE_FACTOR, PAD, ITEM_HEIGHT);
		
		// Alkahest tank - TODO
		WBar alkahestTankBar = new WBar(
				new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/tank_empty.png"),
				new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/tank_full.png"),
				5,
				6,
				Direction.UP
			);
		alkahestTankBar.withTooltip("gui." + ArtOfAlchemy.MOD_ID + ".alkahest_tooltip");
		
		
		root.add(alkahestTankBar, WIDTH - PAD, SIZE_FACTOR, PAD, ITEM_HEIGHT);
		

		WBar progressBar = new WBar(new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/progress_off.png"),
				new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/progress_magenta.png"), 0, 1, Direction.RIGHT);
		root.add(progressBar, 3 * SIZE_FACTOR, 2 * SIZE_FACTOR, 4 * SIZE_FACTOR, SIZE_FACTOR);

		WLabel title = new WLabel(new TranslatableText("block.artofalchemy.distillation_aparatus"), WLabel.DEFAULT_TEXT_COLOR);
		title.setHorizontalAlignment(HorizontalAlignment.CENTER);
		root.add(title, 0, 0, WIDTH, SIZE_FACTOR);

		EssentiaContainer essentia = getEssentia(ctx);
		essentiaPanel = new WEssentiaPanel(essentia);

		root.add(this.createPlayerInventoryPanel(), 0, 5 * 18);

		root.validate(this);

	}

	public void updateEssentia(int essentiaId, EssentiaContainer essentia, BlockPos pos) {
		if (pos.equals(this.pos)) {
			essentiaPanel.updateEssentia(essentia);
		}
	}

	private static EssentiaContainer getEssentia(ScreenHandlerContext ctx) {
		return ctx.get((world, pos) -> {
			BlockEntity be = world.getBlockEntity(pos);
			if (be instanceof HasEssentia) {
				return ((HasEssentia) be).getContainer(0);
			} else {
				return new EssentiaContainer();
			}
		}, new EssentiaContainer());
	}

}
