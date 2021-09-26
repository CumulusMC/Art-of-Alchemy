package com.cumulusmc.artofalchemy.gui.handler;

import io.github.cottonmc.cotton.gui.SyncedGuiDescription;
import io.github.cottonmc.cotton.gui.widget.*;
import io.github.cottonmc.cotton.gui.widget.data.HorizontalAlignment;
import com.cumulusmc.artofalchemy.ArtOfAlchemy;
import com.cumulusmc.artofalchemy.gui.widget.WFormulaList;
import com.cumulusmc.artofalchemy.item.AbstractItemFormula;
import com.cumulusmc.artofalchemy.item.ItemJournal;
import com.cumulusmc.artofalchemy.network.AoAClientNetworking;
import com.cumulusmc.artofalchemy.util.AoAHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class HandlerJournal extends SyncedGuiDescription {

	Hand hand;
	WTextField searchBar;
	WButton clearButton;
	WFormulaList formulaList;
	ItemStack journal;

	Inventory inventory = new SimpleInventory(1) {
		@Override
		public boolean isValid(int slot, ItemStack stack) {
			return (stack.getItem() instanceof AbstractItemFormula) && !(stack.getItem() instanceof ItemJournal);
		}
	};

	@SuppressWarnings("MethodCallSideOnly")
	public HandlerJournal(int syncId, PlayerInventory playerInventory, ScreenHandlerContext ctx, Hand hand) {
		super(AoAHandlers.JOURNAL, syncId, playerInventory);
		blockInventory = inventory;

		this.hand = hand;
		this.journal = playerInventory.player.getStackInHand(hand);

		WGridPanel panel = new WGridPanel(1);
		this.setRootPanel(panel);
		panel.setSize(AoAHandlers.PANEL_WIDTH, AoAHandlers.PANEL_HEIGHT + 38);
		AoAHandlers.makeTitle(panel, new WLabel(journal.getName()));

		// Input Icon
		panel.add(
			new WSprite(new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/add_formula.png")),
			4 + 1,
			AoAHandlers.BASIS - 1,
			AoAHandlers.BASIS - 2,
			AoAHandlers.BASIS - 2
		);

		// Input Slot
		panel.add(
			WItemSlot.of(inventory, 0),
			4,
			AoAHandlers.BASIS - 2
		);

		searchBar = new WTextField() {
			public void setSize(int x, int y) {
				super.setSize(x, y);
			}

			@Override
			public void onKeyPressed(int ch, int key, int modifiers) {
				super.onKeyPressed(ch, key, modifiers);
				formulaList.refresh(journal, this.getText());
			}
		};
		panel.add(
			searchBar,
			AoAHandlers.BASIS + 8,
			AoAHandlers.BASIS - 4,
			(6 * AoAHandlers.BASIS) + 12,
			AoAHandlers.BASIS - 6
		);

		// Background
		panel.add(
			new WSprite(new Identifier(ArtOfAlchemy.MOD_ID, "textures/gui/rune_bg.png")),
			4,
			(2 * AoAHandlers.BASIS) + 10,
			9 * AoAHandlers.BASIS,
			5 * AoAHandlers.BASIS
		);

		formulaList = new WFormulaList(journal, hand);
		formulaList.refresh();
		panel.add(
			formulaList,
			0,
			2 * AoAHandlers.BASIS,
			9 * AoAHandlers.BASIS + 8,
			6 * AoAHandlers.BASIS - 2
		);

		clearButton = new WButton(new LiteralText("❌"));
		clearButton.setAlignment(HorizontalAlignment.CENTER);
		clearButton.setParent(panel);
		panel.add(
			clearButton,
			(8 * AoAHandlers.BASIS) + 6,
			AoAHandlers.BASIS - 4,
			AoAHandlers.BASIS + 2,
			AoAHandlers.BASIS + 2
		);
		clearButton.setOnClick(() -> {
			AoAClientNetworking.sendJournalSelectPacket(Registry.ITEM.getId(Items.AIR), hand);
		});
		clearButton.setEnabled(ItemJournal.getFormula(this.journal) != Items.AIR);
		
		panel.add(
			this.createPlayerInventoryPanel(),
			AoAHandlers.OFFSET,
			8 * AoAHandlers.BASIS
		);

		panel.validate(this);
	}

	@Override
	public void close(PlayerEntity player) {
		dropInventory(player, inventory);
		super.close(player);
	}

	@Override
	public void onSlotClick(int slotNumber, int button, SlotActionType action, PlayerEntity player) {
		if (slotNumber >= 0 && slotNumber < slots.size()) {
			Slot slot = getSlot(slotNumber);
			if (slot != null) {
				if (slot.getStack().getItem() instanceof ItemJournal) {
					return;
				}
			}
		}
		super.onSlotClick(slotNumber, button, action, player);
		tryAddPage();
		refresh(journal);
	}

	public void tryAddPage() {
		ItemStack stack = inventory.getStack(0);
		if (stack.getItem() instanceof AbstractItemFormula) {
			if (ItemJournal.addFormula(journal, AoAHelper.getTarget(stack))) {
				stack.decrement(1);
				inventory.markDirty();
				playerInventory.markDirty();
			}
		}
	}

	public void refresh(ItemStack journal) {
		if (journal == null) {
			journal = playerInventory.player.getStackInHand(hand);
		}
		this.journal = journal;
		if (this.journal.getItem() instanceof ItemJournal) {
			formulaList.refresh(this.journal, searchBar.getText());
			clearButton.setEnabled(ItemJournal.getFormula(this.journal) != Items.AIR);
		} else {
			this.close(playerInventory.player);
		}
	}

}
