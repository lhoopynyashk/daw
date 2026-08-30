package dev.xdark.clientapi.event.render;

import dev.xdark.clientapi.Side;
import dev.xdark.clientapi.SidedApi;
import dev.xdark.clientapi.event.Cancellable;
import dev.xdark.clientapi.event.Event;
import dev.xdark.clientapi.event.EventBus;
import dev.xdark.clientapi.util.ClientBridge;

@SidedApi(Side.SERVER)
public interface OrientCamera extends Event, Cancellable {

  EventBus<OrientCamera> BUS = ClientBridge.busStub();

  float getPartialTicks();
}
