package dev.xdark.clientapi.util;

import static dev.xdark.clientapi.util.ClientBridge.instanceStub;

import dev.xdark.clientapi.Side;
import dev.xdark.clientapi.SidedApi;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
@SidedApi(Side.BOTH)
public interface Rotation {

  Rotation NONE = instanceStub(),
      CLOCKWISE_90 = instanceStub(),
      CLOCKWISE_180 = instanceStub(),
      COUNTERCLOCKWISE_90 = instanceStub();

  Rotation add(Rotation o);

  EnumFacing rotate(EnumFacing facing);

  int rotate(int rot, int p_rotate_2_);
}
