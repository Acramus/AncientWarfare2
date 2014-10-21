package net.shadowmage.ancientwarfare.vehicle.entity.movement;

import net.minecraft.util.Vec3;
import net.shadowmage.ancientwarfare.vehicle.entity.VehicleBase;
import net.shadowmage.ancientwarfare.vehicle.input.VehicleInputKey;

public class VehicleMoveHandlerTest extends VehicleMoveHandler
{

public VehicleMoveHandlerTest(VehicleBase vehicle)
  {
  super(vehicle);
  // TODO Auto-generated constructor stub
  }

@Override
public void updateVehicleMotion(boolean[] inputStates)
  {
  double rotation = 0;
  double forward = 0;
  if(inputStates[VehicleInputKey.FORWARD.ordinal()]){forward+=0.05d;}
  if(inputStates[VehicleInputKey.REVERSE.ordinal()]){forward-=0.05d;}
  if(inputStates[VehicleInputKey.LEFT.ordinal()]){rotation+=1.d;}
  if(inputStates[VehicleInputKey.RIGHT.ordinal()]){rotation-=1.d;}  
  /**
   * first move the vehicle forward along its current move vector
   */
  Vec3 forwardAxis = vehicle.getLookVec();
  double mx = forwardAxis.xCoord * forward;
  double mz = forwardAxis.zCoord * forward;
  vehicle.moveEntity(mx, -0.05d, mz);  
  /**
   * then rotate the vehicle towards its new orientation
   */
  vehicle.rotationYaw += rotation;
  }

}
