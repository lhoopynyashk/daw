package dev.xdark.clientapi.entity;

import dev.xdark.clientapi.Side;
import dev.xdark.clientapi.SidedApi;
import dev.xdark.clientapi.inventory.ContainerLocalMenu;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.NonExtendable
@SidedApi(Side.BOTH)
public interface EntityPlayerSP extends AbstractClientPlayer {

  @SidedApi(Side.SERVER)
  String getServerBrand();

  @SidedApi(Side.SERVER)
  int getPermissionLevel();

  @SidedApi(Side.SERVER)
  void setXPStats(float currentXP, int maxXP, int level);

  @SidedApi(Side.SERVER)
  void setBowDrawSpeed(float speed);

  float getBowDrawSpeed();

  boolean isRidingHorse();

  boolean isCurrentViewEntity();

  boolean isRowingBoat();

  boolean isAutoJumpEnabled();

  @SidedApi(Side.SERVER)
  int displayContainerMenu(ContainerLocalMenu menu);

  @SidedApi(Side.SERVER)
  void closeScreen();

  @Override
  float getHealth();

  @Override
  float getMaxHealth();
}
