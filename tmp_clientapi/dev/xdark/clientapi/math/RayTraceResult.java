package dev.xdark.clientapi.math;

import static dev.xdark.clientapi.util.ClientBridge.instanceStub;

import dev.xdark.clientapi.Side;
import dev.xdark.clientapi.SidedApi;
import dev.xdark.clientapi.entity.Entity;
import dev.xdark.clientapi.util.EnumFacing;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
@SidedApi(Side.SERVER)
public interface RayTraceResult {

  BlockPos getPos();

  Entity getEntity();

  Type getType();

  EnumFacing getHitSide();

  Vec3d getHitVec();

  @SidedApi(Side.SERVER)
  interface Type {

    Type MISS = instanceStub(),
        BLOCK = instanceStub(),
        ENTITY = instanceStub();
  }
}
