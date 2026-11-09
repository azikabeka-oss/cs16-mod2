package com.cs16;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class ShopScreen extends Screen {
	public ShopScreen() { super(Text.literal("Магазин CS 1.6")); }

	@Override
	protected void init() {
		int cols = 4, w = 100, h = 20, gap = 4;
		int startX = this.width / 2 - (cols * (w + gap) - gap) / 2;
		int y0 = 36;

		ButtonWidget header = ButtonWidget.builder(Text.literal("МАГАЗИН (B — закрыть)"), b -> {})
			.dimensions(startX, 12, cols * (w + gap) - gap, h).build();
		header.active = false;
		this.addDrawableChild(header);

		int i = 0;
		for (Shop.Entry e : Shop.ENTRIES.values()) {
			int x = startX + (i % cols) * (w + gap);
			int y = y0 + (i / cols) * (h + gap);
			String id = e.id();
			this.addDrawableChild(ButtonWidget.builder(Text.literal(e.name() + " $" + e.price()),
					b -> this.client.player.networkHandler.sendChatCommand("buy " + id))
				.dimensions(x, y, w, h).build());
			i++;
		}
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
		if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_B) { this.close(); return true; }
		return super.keyPressed(input);
	}
}
