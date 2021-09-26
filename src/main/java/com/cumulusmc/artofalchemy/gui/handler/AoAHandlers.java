package com.cumulusmc.artofalchemy.gui.handler;

import com.cumulusmc.artofalchemy.ArtOfAlchemy;
import com.cumulusmc.artofalchemy.block.*;
import com.cumulusmc.artofalchemy.item.ItemJournal;
import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.widget.WGridPanel;
import io.github.cottonmc.cotton.gui.widget.WLabel;
import io.github.cottonmc.cotton.gui.widget.WSprite;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import net.fabricmc.fabric.api.screenhandler.v1.ScreenHandlerRegistry;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.lang.reflect.InvocationTargetException;

public class AoAHandlers {
	// Constants
	public static int PANEL_WIDTH = 176;
	public static int PANEL_HEIGHT = 180;
	public static final int OFFSET = 7;
	public static final int BASIS = 18;

	public static ScreenHandlerType<HandlerCalcinator>  CALCINATOR;
	public static ScreenHandlerType<HandlerDissolver>   DISSOLVER;
	public static ScreenHandlerType<HandlerSynthesizer> SYNTHESIZER;
	public static ScreenHandlerType<HandlerAnalyzer>	ANALYZER;
	public static ScreenHandlerType<HandlerProjector>   PROJECTOR;
	public static ScreenHandlerType<HandlerJournal>	 JOURNAL;

	public static void registerHandlers() {
		CALCINATOR = ScreenHandlerRegistry.registerExtended(BlockCalcinator.getId(), defaultFactory(HandlerCalcinator.class));
		DISSOLVER = ScreenHandlerRegistry.registerExtended(BlockDissolver.getId(), defaultFactory(HandlerDissolver.class));
		SYNTHESIZER = ScreenHandlerRegistry.registerExtended(BlockSynthesizer.getId(), defaultFactory(HandlerSynthesizer.class));
		PROJECTOR = ScreenHandlerRegistry.registerExtended(BlockProjector.getId(), defaultFactory(HandlerProjector.class));
		ANALYZER = ScreenHandlerRegistry.registerExtended(BlockAnalyzer.getId(),
				(syncId, inventory, buf) -> new HandlerAnalyzer(syncId, inventory, ScreenHandlerContext.EMPTY));
		JOURNAL = ScreenHandlerRegistry.registerExtended(ItemJournal.getId(),
				(syncId, inventory, buf) -> new HandlerJournal(syncId, inventory, ScreenHandlerContext.EMPTY,
						buf.readEnumConstant(Hand.class)));
	}

	// I'm going to forget what this does in 2 weeks and then my eyes will glaze over when I try to fix some really dumb bug
	// I am a pile of garbage pretending to be a programmer and I should not be allowed near computers
	private static <T extends ScreenHandler> ScreenHandlerRegistry.ExtendedClientHandlerFactory<T> defaultFactory(Class<T> klass) {
		return (syncId, inventory, buf) -> {
			try {
				return klass.getDeclaredConstructor(int.class, PlayerInventory.class, ScreenHandlerContext.class)
						.newInstance(syncId, inventory, ScreenHandlerContext.create(inventory.player.world, buf.readBlockPos()));
			} catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
				return null;
			}
		};
	}
	
	public static WGridPanel makePanel(SyncedGuiDescription gui) {
		WGridPanel panel = new WGridPanel(1);
		gui.setRootPanel(panel);
		panel.setSize(AoAHandlers.PANEL_WIDTH, AoAHandlers.PANEL_HEIGHT);
		makeBackground(panel);
		return panel;
	}
	
	public static void makeBackground(WGridPanel panel) {
		WSprite background = new WSprite(new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/rune_bg.png"));
		panel.add(background, AoAHandlers.OFFSET, AoAHandlers.OFFSET, 9 * AoAHandlers.BASIS, (5 * AoAHandlers.BASIS) - AoAHandlers.OFFSET);
	}
	public static void makeTitle(WGridPanel panel, TranslatableText text) {
		WLabel title = new WLabel(text, WLabel.DEFAULT_TEXT_COLOR);
		title.setHorizontalAlignment(HorizontalAlignment.CENTER);
		panel.add(title, 0, AoAHandlers.OFFSET, 9 * AoAHandlers.BASIS, AoAHandlers.BASIS);
	}
	public static void addInventory(WGridPanel panel, SyncedGuiDescription gui) {
		panel.add(gui.createPlayerInventoryPanel(), AoAHandlers.OFFSET, (5 * AoAHandlers.BASIS) - AoAHandlers.OFFSET);
	}
}
