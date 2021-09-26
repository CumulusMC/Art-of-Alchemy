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

public class HandlerDissolver extends SyncedGuiDescription {

	final BlockPos pos;
	final WEssentiaPanel essentiaPanel;

	@SuppressWarnings("MethodCallSideOnly")
	public HandlerDissolver(int syncId, PlayerInventory playerInventory, ScreenHandlerContext ctx) {
		super(AoAHandlers.DISSOLVER, syncId, playerInventory, getBlockInventory(ctx), getBlockPropertyDelegate(ctx));

		pos = ctx.get((world, pos) -> pos, null);

		WGridPanel panel = AoAHandlers.makePanel(this);
		AoAHandlers.makeTitle(panel, new TranslatableText("block.artofalchemy.dissolution_chamber"));
		AoAHandlers.addInventory(panel, this);
		AoAHandlers.addCentralProgressBar(panel, new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/progress_magenta.png"));

		// Item Input
		panel.add(
			WItemSlot.of(blockInventory, 0),
			2 * AoAHandlers.BASIS + 7,
			2 * AoAHandlers.BASIS + 4
		);

		WBar tankBar = new WBar(
			new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/tank_empty.png"),
			new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/tank_full.png"),
			0,
			1,
			Direction.UP
		);
		tankBar.withTooltip("gui." + ArtOfAlchemy.MOD_ID + ".alkahest_tooltip");
		panel.add(
			tankBar,
			0,
			AoAHandlers.BASIS,
			2 * AoAHandlers.BASIS,
			3 * AoAHandlers.BASIS + 6
		);

		WDynamicLabel alert = new WDynamicLabel(() -> {
			switch (propertyDelegate.get(4)) {
			case 2:
				return I18n.translate("gui." + ArtOfAlchemy.MOD_ID + ".alkahest_warning");
			case 3:
				return I18n.translate("gui." + ArtOfAlchemy.MOD_ID + ".buffer_warning");
			default:
				return "";
			}
		}, 0xFF5555);
		alert.setAlignment(HorizontalAlignment.CENTER);
		panel.add(
			alert,
			0,
			0 * AoAHandlers.BASIS,
			9 * AoAHandlers.BASIS,
			AoAHandlers.BASIS
		);

		this.essentiaPanel = new WEssentiaPanel(getEssentia(ctx));
		panel.add(
				this.essentiaPanel,
			6 * AoAHandlers.BASIS - 1,
			AoAHandlers.BASIS - AoAHandlers.OFFSET,
			3 * AoAHandlers.BASIS,
			4 * AoAHandlers.BASIS
		);

		panel.validate(this);
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
