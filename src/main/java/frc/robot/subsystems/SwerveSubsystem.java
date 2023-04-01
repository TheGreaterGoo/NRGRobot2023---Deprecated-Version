// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import java.lang.StackWalker.Option;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;

import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;
import com.ctre.phoenix.sensors.AbsoluteSensorRange;
import com.ctre.phoenix.sensors.CANCoder;
import com.kauailabs.navx.frc.AHRS;
import com.nrg948.preferences.RobotPreferences;
import com.nrg948.preferences.RobotPreferencesLayout;
import com.nrg948.preferences.RobotPreferencesValue;

import edu.wpi.first.math.controller.HolonomicDriveController;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveDriveOdometry;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.math.trajectory.constraint.SwerveDriveKinematicsConstraint;
import edu.wpi.first.util.datalog.DoubleLogEntry;
import edu.wpi.first.util.sendable.Sendable;
import edu.wpi.first.util.sendable.SendableBuilder;
import edu.wpi.first.wpilibj.DataLogManager;
import edu.wpi.first.wpilibj.SPI;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj.smartdashboard.Field2d;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Robot;
import frc.robot.Constants.RobotConstants;
import frc.robot.drive.SwerveDrive;
import frc.robot.drive.SwerveModule;
import frc.robot.parameters.SwerveAngleEncoder;
import frc.robot.parameters.SwerveDriveParameters;
import frc.robot.parameters.SwerveMotors;
import frc.robot.util.SwerveModuleVelocities;
import frc.robot.util.SwerveModuleVoltages;

@RobotPreferencesLayout(groupName = "Drive", column = 0, row = 1, width = 2, height = 3)
public class SwerveSubsystem extends SubsystemBase {
  private static final String PREFERENCES_GROUP = "Drive";

  @RobotPreferencesValue
  public static RobotPreferences.EnumValue<SwerveDriveParameters> PARAMETERS = new RobotPreferences.EnumValue<SwerveDriveParameters>(
      PREFERENCES_GROUP, "Robot Base", SwerveDriveParameters.Competition2023);

  @RobotPreferencesValue
  public static RobotPreferences.BooleanValue ENABLE_DRIVE_TAB = new RobotPreferences.BooleanValue(
      PREFERENCES_GROUP, "Enable Drive Tab", false);

  @RobotPreferencesValue
  public static RobotPreferences.BooleanValue ENABLE_FIELD_TAB = new RobotPreferences.BooleanValue(
      PREFERENCES_GROUP, "Enable Field Tab", false);

  private static final byte NAVX_UPDATE_FREQUENCY_HZ = 50;

  // 4 pairs of motors for drive & steering.
  private final WPI_TalonFX frontLeftDriveMotor = new WPI_TalonFX(
      PARAMETERS.getValue().getMotorId(SwerveMotors.FrontLeftDrive));
  private final WPI_TalonFX frontLeftSteeringMotor = new WPI_TalonFX(
      PARAMETERS.getValue().getMotorId(SwerveMotors.FrontLeftSteering));

  private final WPI_TalonFX frontRightDriveMotor = new WPI_TalonFX(
      PARAMETERS.getValue().getMotorId(SwerveMotors.FrontRightDrive));
  private final WPI_TalonFX frontRightSteeringMotor = new WPI_TalonFX(
      PARAMETERS.getValue().getMotorId(SwerveMotors.FrontRightSteering));

  private final WPI_TalonFX backLeftDriveMotor = new WPI_TalonFX(
      PARAMETERS.getValue().getMotorId(SwerveMotors.BackLeftDrive));
  private final WPI_TalonFX backLeftSteeringMotor = new WPI_TalonFX(
      PARAMETERS.getValue().getMotorId(SwerveMotors.BackLeftSteering));

  private final WPI_TalonFX backRightDriveMotor = new WPI_TalonFX(
      PARAMETERS.getValue().getMotorId(SwerveMotors.BackRightDrive));
  private final WPI_TalonFX backRightSteeringMotor = new WPI_TalonFX(
      PARAMETERS.getValue().getMotorId(SwerveMotors.BackRightSteering));

  // 4 CANcoders for the steering angle.
  private final CANCoder frontLeftAngle = new CANCoder(
      PARAMETERS.getValue().getAngleEncoderId(SwerveAngleEncoder.FrontLeft));
  private final CANCoder frontRightAngle = new CANCoder(
      PARAMETERS.getValue().getAngleEncoderId(SwerveAngleEncoder.FrontRight));
  private final CANCoder backLeftAngle = new CANCoder(
      PARAMETERS.getValue().getAngleEncoderId(SwerveAngleEncoder.BackLeft));
  private final CANCoder backRightAngle = new CANCoder(
      PARAMETERS.getValue().getAngleEncoderId(SwerveAngleEncoder.BackRight));

  private final SwerveModule frontLeftModule = createSwerveModule(
      frontLeftDriveMotor, frontLeftSteeringMotor, frontLeftAngle, "Front Left");
  private final SwerveModule frontRightModule = createSwerveModule(
      frontRightDriveMotor, frontRightSteeringMotor, frontRightAngle, "Front Right");
  private final SwerveModule backLeftModule = createSwerveModule(
      backLeftDriveMotor, backLeftSteeringMotor, backLeftAngle, "Back Left");
  private final SwerveModule backRightModule = createSwerveModule(
      backRightDriveMotor, backRightSteeringMotor, backRightAngle, "Back Right");

  private final SwerveModule[] modules = { frontLeftModule, frontRightModule, backLeftModule, backRightModule };

  private final AHRS ahrs = new AHRS(SPI.Port.kMXP, NAVX_UPDATE_FREQUENCY_HZ);

  private final SwerveDriveKinematics kinematics = PARAMETERS.getValue().getKinematics();

  private final SwerveDrive drivetrain;
  private final SwerveDrivePoseEstimator odometry;
  private final Field2d field = new Field2d();

  // The current sensor state updated by the periodic method.
  private Rotation2d rawOrientation;
  private Rotation2d rawOrientationOffset = new Rotation2d();
  private Rotation2d rawTilt;
  private Rotation2d tiltOffset;
  private double tiltVelocity;
  private boolean wasNavXCalibrating;
  private Optional <PhotonVisionSubsystemBase> visionSource = Optional.empty();
  private Optional <Pose3d> targetPose = Optional.empty();

  private DoubleLogEntry rawOrientationLog = new DoubleLogEntry(DataLogManager.getLog(),
      "/SwerveSubsystem/rawOrientation");
  private DoubleLogEntry rawOrientationOffsetLog = new DoubleLogEntry(DataLogManager.getLog(),
      "/SwerveSubsystem/rawOrientationOffset");
  private DoubleLogEntry rawTiltLog = new DoubleLogEntry(DataLogManager.getLog(), "/SwerveSubsystem/rawTilt");
  private DoubleLogEntry tiltOffsetLog = new DoubleLogEntry(DataLogManager.getLog(), "/SwerveSubsystem/tiltOffset");
  private DoubleLogEntry tiltVelocityLog = new DoubleLogEntry(DataLogManager.getLog(), "/SwerveSubsystem/tiltVelocity");

  // Simulation support.
  private final boolean isSimulation;
  private Rotation2d simOrientation = new Rotation2d();

  /**
   * Creates a {@link SwerveModule} object and intiailizes its motor controllers.
   * 
   * @param driveMotor    The drive motor controller.
   * @param steeringMotor The steering motor controller.
   * @param wheelAngle    An absolute encoder that measures the wheel angle.
   * @param name          The name of the module.
   * 
   * @return An initialized {@link SwerveModule} object.
   */
  private static SwerveModule createSwerveModule(
      WPI_TalonFX driveMotor,
      WPI_TalonFX steeringMotor,
      CANCoder wheelAngle,
      String name) {

    driveMotor.setNeutralMode(NeutralMode.Brake);
    steeringMotor.setNeutralMode(NeutralMode.Brake);
    wheelAngle.configAbsoluteSensorRange(AbsoluteSensorRange.Signed_PlusMinus180);

    final double drivePulsesPerMeter = PARAMETERS.getValue().getDrivePulsesPerMeter();

    return new SwerveModule(
        PARAMETERS.getValue(),
        driveMotor,
        () -> driveMotor.getSelectedSensorPosition() / drivePulsesPerMeter,
        // The WPI_TalonFX reports the velocity in pulses per 100ms, so we need to
        // multiply by 10 to convert to pulses per second.
        () -> (driveMotor.getSelectedSensorVelocity() * 10) / drivePulsesPerMeter,
        steeringMotor,
        () -> Rotation2d.fromDegrees(wheelAngle.getAbsolutePosition()),
        () -> Math.toRadians(wheelAngle.getVelocity()),
        name);
  }

  /** Creates a new SwerveSubsystem. */
  public SwerveSubsystem() {
    isSimulation = Robot.isSimulation();

    initializeSensorState();

    drivetrain = new SwerveDrive(PARAMETERS.getValue(), modules, () -> getOrientation());
    odometry = new SwerveDrivePoseEstimator(kinematics, getOrientation(), drivetrain.getModulesPositions(), new Pose2d());
  }

  /**
   * Initializes the sensor state.
   */
  private void initializeSensorState() {
    ahrs.reset();
    wasNavXCalibrating = true;

    updateSensorState();
    tiltOffset = Rotation2d.fromDegrees(-3.5); // For 2022 robot
  }

  /**
   * Updates the sensor state.
   * <p>
   * This method **MUST* be called by the {@link #periodic()} method to ensure the
   * sensor state is up to date.
   */
  private void updateSensorState() {
    rawOrientation = !isSimulation ? Rotation2d.fromDegrees(-ahrs.getAngle()) : simOrientation;
    rawTilt = Rotation2d.fromDegrees(ahrs.getRoll());

    if (wasNavXCalibrating && !ahrs.isCalibrating()) {
      tiltOffset = rawTilt;
      System.out.println("Tilt offset: " + tiltOffset.getDegrees());
      wasNavXCalibrating = false;
      tiltOffsetLog.append(tiltOffset.getDegrees());
    }

    tiltVelocity = ahrs.getRawGyroY();

    rawOrientationLog.append(rawOrientation.getDegrees());
    rawTiltLog.append(rawTilt.getDegrees());
    tiltVelocityLog.append(tiltVelocity);

  }

  /**
   * Returns the maximum drive speed in m/s of a swerve module.
   * 
   * @return The maximum drive speed.
   */
  public double getMaxSpeed() {
    return PARAMETERS.getValue().getMaxDriveSpeed();
  }

  /**
   * Returns the maximum drive acceleration in m/s^2 of a swerve module.
   * 
   * @return The maximum drive acceleration.
   */
  public double getMaxAcceleration() {
    return PARAMETERS.getValue().getMaxDriveAcceleration();
  }

  /**
   * Returns the swerve drive kinematics for this subsystem.
   * 
   * @return The swerve drive kinematics.
   */
  public SwerveDriveKinematics getKinematics() {
    return kinematics;
  }

  /**
   * Returns a {@link SwerveDriveKinematicsConstraint} object used to enforce
   * swerve drive kinematics constraints when following a trajectory.
   * 
   * @return A {@link SwerveDriveKinematicsConstraint} object used to enforce
   *         swerve drive kinematics constraints when following a trajectory.
   */
  public SwerveDriveKinematicsConstraint getKinematicsConstraint() {
    return PARAMETERS.getValue().getKinematicsConstraint();
  }

  /**
   * Returns the drive constraints.
   * 
   * @return The drive constraints.
   */
  public TrapezoidProfile.Constraints getDriveConstraints() {
    return new TrapezoidProfile.Constraints(getMaxSpeed(), getMaxAcceleration());
  }

  /**
   * Returns a {@link TrapezoidProfile.Constraints} object used to enforce
   * velocity and acceleration constraints on the {@link ProfiledPIDController}
   * used to reach the goal robot orientation.
   * 
   * @return A {@link TrapezoidProfile.Constraints} object used to enforce
   *         velocity and acceleration constraints on the controller used to reach
   *         the goal robot orientation.
   */
  public TrapezoidProfile.Constraints getRotationalConstraints() {
    return PARAMETERS.getValue().getRotationalConstraints();
  }

  /**
   * Creates a HolonomicDriveController for the subsystem.
   * 
   * @return A HolonomicDriveController.
   */
  public HolonomicDriveController createDriveController() {
    ProfiledPIDController thetaController = new ProfiledPIDController(
        1.0, 0.0, 0.0, getRotationalConstraints());

    thetaController.enableContinuousInput(-Math.PI, Math.PI);

    return new HolonomicDriveController(
        new PIDController(1.0, 0.0, 0.0),
        new PIDController(1.0, 0.0, 0.0),
        thetaController);
  }

  /**
   * Drives the robot based on joystick inputs.
   * 
   * @param xSpeed        Speed of the robot in the x direction.
   * @param ySpeed        Speed of the robot in the y direction.
   * @param rSpeed        Rotation speed of the robot.
   * @param fieldRelative Whether the x and y values are relative to field.
   */
  public void drive(double xSpeed, double ySpeed, double rSpeed, boolean fieldRelative) {
    drivetrain.drive(xSpeed, ySpeed, rSpeed, fieldRelative);
  }

  /**
   * Sets the current module's states based on the chassis speed.
   * 
   * @param speeds The chassis speeds.
   */

  public void setChassisSpeeds(ChassisSpeeds speeds) {
    setChassisSpeeds(speeds, false);
  }

  /**
   * Sets the current module's states based on the chassis speed.
   * 
   * @param speeds           The chassis speeds.
   * @param adjustForGravity If true, use the tilt angle to adjust feedforward for
   *                         the effects of gravity.
   * @param tilt             The robot base tilt angle.
   */
  public void setChassisSpeeds(ChassisSpeeds speeds, boolean adjustForGravity) {
    drivetrain.setChassisSpeeds(speeds, adjustForGravity, getTilt());
  }

  /**
   * Returns the current chassis speed.
   * 
   * @return The chassis speed.
   */
  public ChassisSpeeds getChassisSpeeds() {
    return drivetrain.getChassisSpeeds();
  }

  public SwerveModuleState[] getModuleStates() {
    return drivetrain.getModuleStates();
  }

  public SwerveModulePosition[] getModulePositions() {
    return drivetrain.getModulesPositions();
  }

  /**
   * Returns the swerve module velocities.
   * 
   * @return The swerve module velocities.
   */
  public SwerveModuleVelocities[] getModuleVelocities() {
    return drivetrain.getModuleVelocities();
  }

  /**
   * Sets the swerve module states.
   * 
   * @param states An array of four {@link SwerveModuleState} objects in the
   *               order: front left, front right, back left, back right
   */

  public void setModuleStates(SwerveModuleState[] states) {
    setModuleStates(states, false);
  }

  public void setModuleStates(SwerveModuleState[] states, boolean adjustForGravity) {
    drivetrain.setModuleStates(states, adjustForGravity, getTilt());
  }

  /**
   * Sets the module motor voltages.
   * 
   * @param moduleVoltages The module motor voltages.
   */
  public void setModuleVoltages(SwerveModuleVoltages[] moduleVoltages) {
    drivetrain.setModuleVoltages(moduleVoltages);
  }

  // Stops motors from the subsystem - may need to remove this (not sure - Om)
  public void stopMotors() {
    drivetrain.stopMotor();
  }

  /**
   * Resets the robots position on the field.
   * 
   * @param initialPosition Sets the initial position.
   */
  public void resetPosition(Pose2d initialPosition) {
    rawOrientationOffset = initialPosition.getRotation().minus(rawOrientation);
    rawOrientationOffsetLog.append(rawOrientationOffset.getDegrees());
    odometry.resetPosition(getOrientation(), drivetrain.getModulesPositions(), initialPosition);
  }

  /**
   * Resets the orientation the robot.
   */
  public void resetOrientation() {
    Pose2d currentPos = odometry.getEstimatedPosition();
    Pose2d newPos2d = new Pose2d(currentPos.getTranslation(), new Rotation2d());
    resetPosition(newPos2d);
  }

  /**
   * Return current position & orientation of the robot on the field.
   * 
   * @return The current position and orientation of the robot.
   */
  public Pose2d getPosition() {
    return odometry.getEstimatedPosition();
  }

  /**
   * Returns the current position and orienation of the robot on the field in
   * 3-dimensional space.
   * 
   * @return The current position and orientation in 3-dimensional space.
   */
  public Pose3d getPosition3d() {
    Pose2d robotPose2d = getPosition();

    return new Pose3d(
        robotPose2d.getX(),
        robotPose2d.getY(),
        0.0,
        new Rotation3d(0.0, 0.0, robotPose2d.getRotation().getRadians()));
  }

  /**
   * Returns the field orientation of the robot as a {@link Rotation2d} object.
   * 
   * @return Gets the field orientation of the robot.
   */
  public Rotation2d getOrientation() {
    return rawOrientation.plus(rawOrientationOffset);
  }

  /**
   * Returns the corrected tilt of the robot as a {@link Rotation2d} object.
   * 
   * @return Gets the tilt of the robot (positive is nose up, negative is nose
   *         down).
   */
  public Rotation2d getTilt() {
    return rawTilt.minus(tiltOffset);
  }

  /**
   * Returns the tilt velocity of the robot in degrees per second per second.
   * 
   * @return Gets the tilt velocity of the robot.
   */
  public double getTiltVelocity() {
    return tiltVelocity;
  }

  public void enablePoseEstimation (PhotonVisionSubsystemBase visionSource, Pose3d targetPose) {
    this.visionSource = Optional.of(visionSource);
    this.targetPose = Optional.of(targetPose);
  }

  public void disablePoseEstimation () {
    this.visionSource = Optional.empty();
    this.targetPose = Optional.empty();
  }

  @Override
  public void periodic() {
    // Read sensors to update subsystem state.
    updateSensorState();

    // Update the current module state.
    drivetrain.periodic();
    if (visionSource.isPresent()&&visionSource.get().hasTargets()){
      PhotonVisionSubsystemBase source = visionSource.get();
      Translation3d targetVector = new Translation3d(source.getDistanceToBestTarget(), new Rotation3d(0,0,Math.toRadians(-source.getAngleToBestTarget())));
      Pose3d cameraToTarget = new Pose3d(targetPose.get().getTranslation().minus(targetVector), new Rotation3d(0,0,getOrientation().getRadians()));
      Pose2d estimatedPose = cameraToTarget.transformBy(RobotConstants.FRONT_CAMERA_TO_ROBOT).toPose2d();
      odometry.addVisionMeasurement(estimatedPose, source.getTargetTimestamp());
    }

    // Update odometry last since this relies on the subsystem sensor and module
    // states.
    odometry.update(getOrientation(), drivetrain.getModulesPositions());

    // Send the robot and module location to the field
    Pose2d robotPose = getPosition();

    field.setRobotPose(robotPose);

    ArrayList<Pose2d> modulePoses = new ArrayList<Pose2d>(4);

    for (Translation2d wheelPosition : PARAMETERS.getValue().getWheelPositions()) {
      modulePoses.add(
          new Pose2d(
              wheelPosition.rotateBy(robotPose.getRotation())
                  .plus(robotPose.getTranslation()),
              robotPose.getRotation()));
    }

    field.getObject("Swerve Modules").setPoses(modulePoses);
  }

  @Override
  public void simulationPeriodic() {
    drivetrain.simulationPeriodic();

    ChassisSpeeds chassisSpeeds = kinematics.toChassisSpeeds(getModuleStates());

    simOrientation = new Rotation2d(
        simOrientation.getRadians() + (chassisSpeeds.omegaRadiansPerSecond * Robot.kDefaultPeriod));
  }

  /**
   * Adds a tab for swerve drive in Shuffleboard.
   */
  public void addShuffleboardTab() {
    if (ENABLE_DRIVE_TAB.getValue()) {
      ShuffleboardTab swerveDriveTab = Shuffleboard.getTab("Drive");

      drivetrain.addShuffleboardLayouts(swerveDriveTab);

      ShuffleboardLayout odometryLayout = swerveDriveTab.getLayout("Odometry", BuiltInLayouts.kList)
          .withPosition(6, 0)
          .withSize(3, 4);

      odometryLayout.add("Orientation", new Sendable() {
        @Override
        public void initSendable(SendableBuilder builder) {
          builder.setSmartDashboardType("Gyro");
          builder.addDoubleProperty("Value", () -> -getOrientation().getDegrees(), null);
        }
      }).withWidget(BuiltInWidgets.kGyro).withPosition(0, 0);

      ShuffleboardLayout positionLayout = odometryLayout.getLayout("Position", BuiltInLayouts.kGrid)
          .withProperties(Map.of("Number of columns", 4, "Number of rows", 1));

      positionLayout.addDouble("X", () -> odometry.getEstimatedPosition().getX())
          .withPosition(0, 0);
      positionLayout.addDouble("Y", () -> odometry.getEstimatedPosition().getY())
          .withPosition(1, 0);
      positionLayout.addDouble("Tilt", () -> getTilt().getDegrees())
          .withPosition(2, 0);
      positionLayout.addDouble("Tilt Velocity", () -> getTiltVelocity())
          .withPosition(3, 0);
    }

    if (ENABLE_FIELD_TAB.getValue()) {
      ShuffleboardTab fieldTab = Shuffleboard.getTab("Field");
      ShuffleboardLayout fieldLayout = fieldTab.getLayout("Field", BuiltInLayouts.kGrid)
          .withPosition(0, 0)
          .withSize(6, 4)
          .withProperties(Map.of("Number of columns", 1, "Number of rows", 1));

      fieldLayout.add(field);
    }
  }
}

