// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands;

import java.util.function.Supplier;

import edu.wpi.first.math.controller.HolonomicDriveController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.SwerveSubsystem;

/**
 * A command to drive the robot on a straight line in using trapezoidal motion
 * profiling.
 */
public class DriveStraight extends CommandBase {
  private final SwerveSubsystem drivetrain;
  private final double distance;
  private final Rotation2d heading;
  private final HolonomicDriveController controller;
  private final TrapezoidProfile profile;
  private final Supplier<Rotation2d> orientationSupplier;
  private final Timer timer = new Timer();
  private Pose2d initialPose;
  private Rotation2d orientation;

  /**
   * Constructs an instance of this class.
   * 
   * @param drivetrain  The {@link SwerveSubsystem} representing the robot
   *                    drivetrain.
   * @param translation A {@link Translation2d} instance describing the line on
   *                    which to travel. This is a vector relative to the current
   *                    position.
   */
  public DriveStraight(SwerveSubsystem drivetrain, Translation2d translation) {
    this(drivetrain, translation, drivetrain.getMaxSpeed());
  }

  /**
   * Constructs an instance of this class.
   * 
   * @param drivetrain  The {@link SwerveSubsystem} representing the robot
   *                    drivetrain.
   * @param translation A {@link Translation2d} instance describing the line on
   *                    which to travel. This is a vector relative to the current
   *                    position.
   * @param maxSpeed    The maximum speed at which to travel.
   */
  public DriveStraight(SwerveSubsystem drivetrain, Translation2d translation, double maxSpeed) {
    this(drivetrain, translation, maxSpeed, () -> drivetrain.getPosition().getRotation());
  }

  /**
   * Constructs an instance of this class.
   * 
   * @param drivetrain  The {@link SwerveSubsystem} representing the robot
   *                    drivetrain.
   * @param translation A {@link Translation2d} instance describing the line on
   *                    which to travel. This is a vector relative to the current
   *                    position.
   * @param maxSpeed    The maximum speed at which to travel.
   * @param orientation The desired orientation at the end of the command.
   */
  public DriveStraight(
      SwerveSubsystem drivetrain,
      Translation2d translation,
      double maxSpeed,
      Rotation2d orientation) {
    this(drivetrain, translation, maxSpeed, () -> orientation);
  }

  /**
   * Constructs an instance of this class.
   * 
   * @param drivetrain          The {@link SwerveSubsystem} representing the robot
   *                            drivetrain.
   * @param translation         A {@link Translation2d} instance describing the
   *                            line on
   *                            which to travel. This is a vector relative to the
   *                            current
   *                            position.
   * @param maxSpeed            The maximum speed at which to travel.
   * @param orientationSupplier Supplies the desired orientation at the end of the
   *                            command.
   */
  private DriveStraight(
      SwerveSubsystem drivetrain,
      Translation2d translation,
      double maxSpeed,
      Supplier<Rotation2d> orientationSupplier) {
    this.drivetrain = drivetrain;
    this.distance = translation.getNorm();
    this.heading = translation.getAngle();
    this.controller = drivetrain.createDriveController();
    this.orientationSupplier = orientationSupplier;
    this.profile = new TrapezoidProfile(
        new TrapezoidProfile.Constraints(maxSpeed, drivetrain.getMaxAcceleration()),
        new TrapezoidProfile.State(distance, 0));

    addRequirements(drivetrain);
  }

  @Override
  public void initialize() {
    initialPose = drivetrain.getPosition();
    orientation = orientationSupplier.get();
    System.out.println("BEGIN ProfiledDriveStraight intitialPose = " + initialPose + ", orientation = " + orientation
        + ", distance = " + distance + ", heading = " + heading);
    timer.reset();
    timer.start();
  }

  @Override
  public void execute() {
    // Calculate the next state (position and velocity) of motion using the
    // trapezoidal profile.
    TrapezoidProfile.State state = profile.calculate(timer.get());

    // Determine the next position on the field by offsetting the initial position
    // by the distance moved along the line of travel.
    Translation2d offset = new Translation2d(state.position, heading);
    Pose2d nextPose = new Pose2d(initialPose.getTranslation().plus(offset), heading);

    // Calculate the swerve drive modules states needed to reach the next state.
    ChassisSpeeds speeds = controller.calculate(
        drivetrain.getPosition(), nextPose, state.velocity, orientation);

    drivetrain.setChassisSpeeds(speeds);
  }

  @Override
  public boolean isFinished() {
    return profile.isFinished(timer.get());
  }

  @Override
  public void end(boolean interrupted) {
    drivetrain.stopMotors();
    timer.stop();
    System.out.println("END ProfiledDriveStraight");
  }
}
