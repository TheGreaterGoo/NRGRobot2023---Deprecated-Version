// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import java.util.List;
import java.util.Map;

import com.nrg948.autonomous.AutonomousCommandMethod;
import com.pathplanner.lib.PathConstraints;
import com.pathplanner.lib.PathPlanner;
import com.pathplanner.lib.PathPlannerTrajectory;
import com.pathplanner.lib.auto.PIDConstants;
import com.pathplanner.lib.auto.SwerveAutoBuilder;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import frc.robot.subsystems.Subsystems;
import frc.robot.subsystems.SwerveSubsystem;

public final class Autos {
  @AutonomousCommandMethod(name = "Follow S-Curve Path")
  public static CommandBase followSCurvePath(Subsystems subsystems) {
    return Commands.sequence(
        new InstantCommand(() -> subsystems.drivetrain.resetPosition(new Pose2d())),
        FollowTrajectory.fromWaypoints(
            subsystems.drivetrain,
            new Pose2d(0, 0, new Rotation2d(0)),
            List.of(new Translation2d(1, 1), new Translation2d(2, 1)),
            new Pose2d(3, 0,
                new Rotation2d(0))));
  }

  @AutonomousCommandMethod(name = "Drive Straight For 3 Meters")
  public static CommandBase driveStraight3Meters(Subsystems subsystems) {
    return Commands.sequence(
        new InstantCommand(() -> subsystems.drivetrain.resetPosition(new Pose2d())),
        new ProfiledDriveStraight(subsystems.drivetrain, new Translation2d(3.0, Rotation2d.fromDegrees(0))));
  }

  @AutonomousCommandMethod(name = "Test Path")
  public static CommandBase followTestPath(Subsystems subsystems) {
    SwerveSubsystem drivetrain = subsystems.drivetrain;
    List<PathPlannerTrajectory> pathGroup = PathPlanner.loadPathGroup(
      "Test", 
      new PathConstraints(drivetrain.getMaxSpeed()*0.5, drivetrain.getMaxAcceleration()));
    SwerveAutoBuilder autoBuilder = new SwerveAutoBuilder(
      drivetrain::getPosition,
      drivetrain::resetPosition,
      drivetrain.getKinematics(),
      new PIDConstants(1.0, 0, 0),
      new PIDConstants(1.0, 0, 0),
      drivetrain::setModuleStates,
      Map.of(),
      true,
      drivetrain);
      
    return autoBuilder.fullAuto(pathGroup).andThen(new AssistedBalanceOnChargeStation(drivetrain));
  }

  private Autos() {
    throw new UnsupportedOperationException("This is a utility class!");
  }
}
