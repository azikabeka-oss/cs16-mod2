package com.cs16;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Shop {
	public record Entry(String id, String name, int price, Supplier<List<ItemStack>> items) {}

	public static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

	public static void init() {
		// оружие
		Weapons.ALL.forEach((id, g) ->
			add(id, id.toUpperCase(), g.price, () -> List.of(new ItemStack(g))));
		add("knife", "Нож", 0, () -> List.of(new ItemStack(Weapons.KNIFE)));
		// не оружие
		add("kevlar", "Бронежилет", 650, () -> List.of(new ItemStack(Items.IRON_CHESTPLATE)));
		add("kevlar_helmet", "Жилет + каска", 1000,
			() -> List.of(new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_HELMET)));
		add("defuse_kit", "Набор сапёра", 200, () -> List.of(new ItemStack(Items.SHEARS)));
		add("medkit", "Аптечка", 400, () -> List.of(new ItemStack(Items.GOLDEN_APPLE)));
		add("flashbang", "Светошумовая", 200, () -> List.of(new ItemStack(Items.SNOWBALL, 2)));
		add("grenade", "Граната", 300, () -> List.of(new ItemStack(Items.WIND_CHARGE, 2)));
	}

	private static void add(String id, String name, int price, Supplier<List<ItemStack>> items) {
		ENTRIES.put(id, new Entry(id, name, price, items));
	}
}
