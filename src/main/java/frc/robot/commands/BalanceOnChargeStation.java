// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.SwerveSubsystem;

/**
 * Command which balances the robot on the charge station at the end of auto.
 */
public class BalanceOnChargeStation extends CommandBase {
  private static final double CLIMB_SPEED = 0.3;
  private static final double BALANCE_THRESHOLD = 2.0; // degrees, "balanced" if within +/- TILT_EPSILON.
  private static final double TILT_MIN_VELOCITY = 2.0; // degrees per second

  private boolean wasTiltedUp = false;
  private double previousSpeed = 0;
  private int pauseCounter = 0;
  private SwerveSubsystem drivetrain;

  /** Creates a new BalanceOnChargeStation command. */
  public BalanceOnChargeStation(SwerveSubsystem drivetrain, boolean alreadyOnChargeStation) {
    this.drivetrain = drivetrain;
    wasTiltedUp = alreadyOnChargeStation;
    addRequirements(drivetrain);
  }

  // Called when the command is initially scheduled.
  @Override
  public void initialize() {
  }

  // Called every time the scheduler runs while the command is scheduled.
  @Override
  public void execute() {
    Rotation2d tiltAngle = drivetrain.getTilt();
    double tiltVelocity = drivetrain.getTiltVelocity();
    double currentSpeed = 0;
    
    if (!wasTiltedUp) {
      //If the robot has never gotten onto the charge station, the robot drives foward
      currentSpeed = CLIMB_SPEED;
      wasTiltedUp = tiltAngle.getDegrees() > 5;
    } else if (tiltAngle.getDegrees() > BALANCE_THRESHOLD) {
      // Robot is tilted upwards so we need to keep climbing until we start tilting
      // towards level in which case we stop.
      currentSpeed = (tiltVelocity < -TILT_MIN_VELOCITY) ? 0 : CLIMB_SPEED; 
    } else if (tiltAngle.getDegrees() < -BALANCE_THRESHOLD) {
      // Robot is tilted downwards so we need climb backwards until we start tilting
      // towards level in which case we stop.
      currentSpeed = (tiltVelocity > -TILT_MIN_VELOCITY) ? 0 : -CLIMB_SPEED;
    }

    if (previousSpeed != 0 && currentSpeed == 0) {
      pauseCounter = 50;
    } 
    if (pauseCounter > 0) {
      currentSpeed = 0;
    }

    drivetrain.drive(currentSpeed, 0, 0, false, false);
    pauseCounter--;
    previousSpeed = currentSpeed;
  }

  // Called once the command ends or is interrupted.
  @Override
  public void end(boolean interrupted) {
    drivetrain.stopMotors();
  }

  // Returns true when the command should end.
  @Override
  public boolean isFinished() {
    return false;
  }
}
