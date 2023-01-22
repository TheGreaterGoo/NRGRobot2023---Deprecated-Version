// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.parameters;

import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.trajectory.constraint.SwerveDriveKinematicsConstraint;
import edu.wpi.first.math.util.Units;
import static frc.robot.Constants.*;
import static frc.robot.parameters.MotorParameters.Falcon500;
import static frc.robot.parameters.SwerveModuleParameters.MK4Standard;

/**
 * An enum representing the properties for the swerve drive base of a specific
 * robot instance.
 */
public enum SwerveDriveParameters {

  /** The 2022 competition robot. */
  Competition2022(
      67.5853,
      Units.inchesToMeters(26.3),
      Units.inchesToMeters(19.5),
      MK4Standard,
      Falcon500,
      1.0,
      1.0);

  /**
   * A scaling factor used to adjust from theoretical maximums given that any
   * physical system generally cannot achieve them.
   */
  private static final double SCALE_FACTOR = 0.80;

  private final double robotMass;
  private final double wheelDistanceX;
  private final double wheelDistanceY;
  private final SwerveModuleParameters swerveModule;
  private final MotorParameters motor;

  private double maxDriveSpeed;
  private double maxDriveAcceleration;

  private final double driveKs;
  private final double driveKv;
  private final double driveKa;

  private double maxSteeringSpeed;
  private double maxSteeringAcceleration;

  private final double steeringKs;
  private final double steeringKv;
  private final double steeringKa;

  private double maxRobotRotationalSpeed;

  private final Translation2d[] wheelPositions;
  private final SwerveDriveKinematics kinematics;
  private final SwerveDriveKinematicsConstraint kinematicsConstraint;
  private final TrapezoidProfile.Constraints steeringConstraints;

  /**
   * Constructs an instance of this enum.
   * <p>
   * <b>NOTE:</b> The distance between wheels are expressed in the NWU coordinate
   * system relative to the robot frame as shown below.
   * <p>
   * 
   * <pre>
   * <code>
   *            ^
   * +--------+ | x
   * |O      O| |
   * |        | |
   * |        | |
   * |O      O| |
   * +--------+ |
   *            |
   * <----------+
   *  y       (0,0)
   * </code>
   * </pre>
   * 
   * @param robotMass      The mass of the robot in Kg.
   * @param wheelDistanceX The distance between the wheels along the X axis in
   *                       meters.
   * @param wheelDistanceY The distance between the wheels along the Y axis in
   *                       meters
   * @param swerveModule   The swerve module used by the robot.
   * @param motor          The motor used by swerve module on the robot.
   */
  private SwerveDriveParameters(
      double robotMass,
      double wheelDistanceX,
      double wheelDistanceY,
      SwerveModuleParameters swerveModule,
      MotorParameters motor,
      double driveKs,
      double steeringKs) {
    this.robotMass = robotMass;
    this.wheelDistanceX = wheelDistanceX;
    this.wheelDistanceY = wheelDistanceY;
    this.swerveModule = swerveModule;
    this.motor = motor;

    this.maxDriveSpeed = SCALE_FACTOR * this.swerveModule.calculateMaxDriveSpeed(this.motor);
    this.maxDriveAcceleration = SCALE_FACTOR
        * this.swerveModule.calculateMaxDriveAcceleration(this.motor, this.robotMass);

    this.driveKs = driveKs;
    this.driveKv = (RobotConstants.kMaxBatteryVoltage - this.driveKs) / this.maxDriveSpeed;
    this.driveKa = (RobotConstants.kMaxBatteryVoltage - this.driveKs) / this.maxDriveAcceleration;

    this.maxSteeringSpeed = SCALE_FACTOR * this.swerveModule.calculateMaxSteeringSpeed(this.motor);
    this.maxSteeringAcceleration = SCALE_FACTOR
        * this.swerveModule.calculateMaxSteeringAcceleration(this.motor, this.robotMass);

    this.steeringKs = steeringKs;
    this.steeringKv = (RobotConstants.kMaxBatteryVoltage - this.steeringKs) / this.maxSteeringSpeed;
    this.steeringKa = (RobotConstants.kMaxBatteryVoltage - this.steeringKs) / this.maxSteeringAcceleration;

    this.maxRobotRotationalSpeed = this.maxDriveSpeed / Math.hypot(this.wheelDistanceX, this.wheelDistanceY);

    this.wheelPositions = new Translation2d[] {
        new Translation2d(this.wheelDistanceX / 2.0, this.wheelDistanceY / 2),
        new Translation2d(this.wheelDistanceX / 2.0, -this.wheelDistanceY / 2),
        new Translation2d(-this.wheelDistanceX / 2.0, this.wheelDistanceY / 2),
        new Translation2d(-this.wheelDistanceX / 2.0, -this.wheelDistanceY / 2),
    };

    this.kinematics = new SwerveDriveKinematics(wheelPositions);
    this.kinematicsConstraint = new SwerveDriveKinematicsConstraint(kinematics, this.maxDriveSpeed);
    this.steeringConstraints = new TrapezoidProfile.Constraints(this.maxSteeringSpeed, this.maxSteeringAcceleration);
  }

  /**
   * Returns the mass of the robot in Kg.
   * 
   * @return The mass of the robot in Kg.
   */
  public double getRobotMass() {
    return this.robotMass;
  }

  /**
   * Returns the maximum rotational speed of the robot in rad/s.
   * 
   * @return The maximum rotation speed of the robot in rad/s.
   */
  public double getMaxRobotRotationalSpeed() {
    return this.maxRobotRotationalSpeed;
  }

  /**
   * Returns the distance between the wheels along the X-axis in meters.
   * 
   * @return The distance between the wheels along the X-axis in meters.
   */
  public double getWheelDistanceX() {
    return this.wheelDistanceX;
  }

  /**
   * Returns the distance between the wheels along the Y-axis in meters.
   * 
   * @return The distance between the wheels along the Y-axis in meters.
   */
  public double getWheelDistanceY() {
    return this.wheelDistanceY;
  }

  /**
   * Returns the wheel positions relative to the robot center.
   * 
   * @return The wheel positions relative to the robot center.
   */
  public Translation2d[] getWheelPositions() {
    return this.wheelPositions;
  }

  /**
   * Returns the swerve module used by the robot.
   * 
   * @return The swerve module used by the robot.
   */
  public SwerveModuleParameters getSwerveModule() {
    return this.swerveModule;
  }

  /**
   * Returns the motor used by swerve module on the robot.
   * 
   * @return The motor used by swerve module on the robot.
   */
  public MotorParameters getMotorParameters() {
    return this.motor;
  }

  /**
   * Returns the maximum drive speed in m/s of a swerve module.
   * 
   * @return The maximum drive speed.
   */
  public double getMaxDriveSpeed() {
    return this.maxDriveSpeed;
  }

  /**
   * Returns the maximum drive acceleration in m/s^2 of a swerve module.
   * 
   * @return The maximum drive acceleration.
   */
  public double getMaxDriveAcceleration() {
    return this.maxDriveAcceleration;
  }

  /**
   * Returns the kS feedforward control constant for translation in Volts.
   * <p>
   * This is the voltage needed to overcome the internal friction of the motor.
   * 
   * @return The kS feedforward control constant for translation in Volts.
   */
  public double getDriveKs() {
    return this.driveKs;
  }

  /**
   * Returns the kV feedforward control constant for translation in Volt * seconds
   * per meter.
   * <p>
   * This is used to calculate the voltage needed to maintain a constant velocity.
   * 
   * @return The kV feedforward control constant for translation in Volt * seconds
   *         per meter.
   */
  public double getDriveKv() {
    return this.driveKv;
  }

  /**
   * Returns the kA feedforward control constant for translation in Volt *
   * seconds^2 per meter.
   * <p>
   * This is used to calculate the voltage needed to maintain a constant
   * acceleration.
   * 
   * @return The kA feedforward control constant for translation in Volt *
   *         seconds^2 per meter.
   */
  public double getDriveKa() {
    return this.driveKa;
  }

  /**
   * Returns the pulses per meter of the integrated encoder.
   * 
   * @return The pulses per meter of the integrated encoder.
   */
  public double getDrivePulsesPerMeter() {
    return this.swerveModule.calculateDrivePulsesPerMeter(this.motor);
  }

  /**
   * Returns a {@link SwerveDriveKinematics} object used to convert chassis speeds
   * to individual module states.
   * 
   * @return A {@link SwerveDriveKinematics} object used to convert chassis speeds
   *         to individual module states.
   */
  public SwerveDriveKinematics getKinematics() {
    return this.kinematics;
  }

  /**
   * Returns a {@link SwerveDriveKinematicsConstraint} object used to enforce
   * swerve drive kinematics constraints when following a trajectory.
   * 
   * @return A {@link SwerveDriveKinematicsConstraint} object used to enforce
   *         swerve drive kinematics constraints when following a trajectory.
   */
  public SwerveDriveKinematicsConstraint getKinematicsConstraint() {
    return this.kinematicsConstraint;
  }

  /**
   * Returns the maximum steering speed in rad/s of a swerve module.
   * 
   * @return The maximum steering speed.
   */
  public double getMaxSteeringSpeed() {
    return this.maxSteeringSpeed;
  }

  /**
   * Returns the maximum steering acceleration in rad/s^2 of a swerve module.
   * 
   * @return The maximum steering acceleration.
   */
  public double getMaxSteeringAcceleration() {
    return this.maxSteeringAcceleration;
  }

  /**
   * Returns the kS feedforward control constant for rotation in Volts.
   * <p>
   * This is the voltage needed to overcome the internal friction of the motor.
   * 
   * @return The kS feedforward control constant for rotation in Volts.
   */
  public double getSteeringKs() {
    return this.steeringKs;
  }

  /**
   * Returns the kV feedforward control constant for rotation in Volt * seconds
   * per radian.
   * <p>
   * This is used to calculate the voltage needed to maintain a constant steering
   * velocity.
   * 
   * @return The kV feedforward control constant for translation in Volt * seconds
   *         per radian.
   */
  public double getSteeringKv() {
    return this.steeringKv;
  }

  /**
   * Returns the kA feedforward control constant for rotation in Volt *
   * seconds^2 per radian.
   * <p>
   * This is used to calculate the voltage needed to maintain a constant
   * rotational acceleration.
   * 
   * @return The kA feedforward control constant for translation in Volt *
   *         seconds^2 per radian.
   */
  public double getSteeringKa() {
    return this.steeringKa;
  }

  /**
   * Returns a {@link TrapezoidProfile.Constraints} object used to enforce
   * velocity and acceleration constraints on the {@link ProfiledPIDController}
   * used to reach the goal wheel angle.
   * 
   * @return A {@link TrapezoidProfile.Constraints} object used to enforce
   *         velocity and acceleration constraints on the controller used to reach
   *         the goal wheel angle.
   */
  public TrapezoidProfile.Constraints getSteeringConstraints() {
    return steeringConstraints;
  }
}
