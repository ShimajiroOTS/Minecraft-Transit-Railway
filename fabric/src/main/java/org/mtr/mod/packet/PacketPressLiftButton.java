package org.mtr.mod.packet;

import org.mtr.core.operation.PressLift;
import org.mtr.core.serializer.JsonReader;
import org.mtr.core.serializer.SerializedDataBase;
import org.mtr.core.servlet.Operation;
import org.mtr.core.tool.Utilities;
import org.mtr.mapping.tool.PacketBufferReceiver;

import javax.annotation.Nonnull;

public final class PacketPressLiftButton extends PacketRequestResponseBase {

	public PacketPressLiftButton(PacketBufferReceiver packetBufferReceiver) {
		super(packetBufferReceiver);
	}

	public PacketPressLiftButton(PressLift contentObject) {
		super(Utilities.getJsonObjectFromData(contentObject).toString());
	}

	private PacketPressLiftButton(String content) {
		super(content);
	}

	@Override
	protected PacketRequestResponseBase getInstance(String content) {
		return new PacketPressLiftButton(content);
	}

	@Override
	protected SerializedDataBase getDataInstance(JsonReader jsonReader) {
		return new PressLift(jsonReader);
	}

	@Nonnull
	@Override
	protected Operation getOperation() {
		return Operation.PRESS_LIFT;
	}

	@Override
	protected PacketRequestResponseBase.ResponseType responseType() {
		return PacketRequestResponseBase.ResponseType.NONE;
	}
}
