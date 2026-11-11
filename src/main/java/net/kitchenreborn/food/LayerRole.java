package net.kitchenreborn.food;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;

/** What a layer is for. Food types use roles for end rules (a sandwich needs bread on both ends). */
public enum LayerRole implements StringRepresentable {
	BREAD("bread"),
	FILLING("filling"),
	SPREAD("spread"),
	TOPPING("topping"),
	BASE("base");

	public static final Codec<LayerRole> CODEC = StringRepresentable.fromEnum(LayerRole::values);

	private final String serializedName;

	LayerRole(String serializedName) {
		this.serializedName = serializedName;
	}

	@Override
	public String getSerializedName() {
		return serializedName;
	}
}
