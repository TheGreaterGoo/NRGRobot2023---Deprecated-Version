// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import java.util.Map;

import com.nrg948.preferences.RobotPreferences;
import com.nrg948.preferences.RobotPreferencesLayout;

import edu.wpi.first.cscore.HttpCamera;
import edu.wpi.first.cscore.HttpCamera.HttpCameraKind;
import edu.wpi.first.cscore.VideoSource;
import edu.wpi.first.hal.HALUtil;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandScheduler;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import edu.wpi.first.wpilibj2.command.button.Trigger;
import frc.robot.Constants.ColorConstants;
import frc.robot.Constants.OperatorConstants.XboxControllerPort;
import frc.robot.commands.DriveAndAutoRotate;
import frc.robot.commands.DriveAndOrientToCube;
import frc.robot.commands.DriveAndStrafeToCube;
import frc.robot.commands.DriveWithController;
import frc.robot.commands.FlameCycle;
import frc.robot.commands.IndexByController;
import frc.robot.commands.IntakeByController;
import frc.robot.commands.Scoring;
import frc.robot.commands.ShootByController;
import frc.robot.subsystems.ShooterSubsystem.GoalShooterRPM;
import frc.robot.subsystems.IndexerSubsystem;
import frc.robot.subsystems.Subsystems;

/**
 * This class is where the bulk of the robot should be declared. Since
 * Command-based is a
 * "declarative" paradigm, very little robot logic should actually be handled in
 * the {@link Robot}
 * periodic methods (other than the scheduler calls). Instead, the structure of
 * the robot (including
 * subsystems, commands, and trigger mappings) should be declared here.
 */
@RobotPreferencesLayout(groupName = "Preferences", column = 0, row = 0, width = 2, height = 1)
public class RobotContainer {

	// The robot's subsystems and commands are defined here...
	private final Subsystems subsystems = new Subsystems();

	// Operator Xbox controllers.
	private final CommandXboxController driveController = new CommandXboxController(XboxControllerPort.DRIVER);
	private final CommandXboxController manipulatorController = new CommandXboxController(
			XboxControllerPort.MANIPULATOR);

	private final RobotAutonomous autonomous = new RobotAutonomous(subsystems);
	private boolean enableManualControl = false;

	/**
	 * The container for the robot. Contains subsystems, OI devices, and commands.
	 */
	public RobotContainer() {
		DriverStation.silenceJoystickConnectionWarning(true);

		subsystems.drivetrain
				.setDefaultCommand(new DriveWithController(subsystems.drivetrain, driveController));

		CommandScheduler.getInstance().schedule(new FlameCycle(subsystems.leds));

		initShuffleboard();

		configureCommandBindings();

	}

	/**
	 * Use this method to define your trigger->command mappings. Triggers can be
	 * created via the
	 * {@link Trigger#Trigger(java.util.function.BooleanSupplier)} constructor with
	 * an arbitrary
	 * predicate, or via the named factories in {@link
	 * edu.wpi.first.wpilibj2.command.button.CommandGenericHID}'s subclasses for
	 * {@link
	 * CommandXboxController
	 * Xbox}/{@link edu.wpi.first.wpilibj2.command.button.CommandPS4Controller
	 * PS4} controllers or
	 * {@link edu.wpi.first.wpilibj2.command.button.CommandJoystick Flight
	 * joysticks}.
	 */
	private void configureCommandBindings() {

		driveController.rightBumper()
				.whileTrue(new DriveAndOrientToCube(subsystems.drivetrain, subsystems.cubeVision, driveController));
		driveController.leftBumper()
				.whileTrue(new DriveAndStrafeToCube(subsystems.drivetrain, subsystems.cubeVision, driveController));
		// TODO: Once we're done with testing the autonomous motion commands, change
		// this to call resetOrientation().
		driveController.start().onTrue(Commands.runOnce(() -> subsystems.drivetrain.resetPosition(new Pose2d())));
		driveController.back().onTrue(Scoring.prepForMatch(subsystems));

		driveController.a()
				.whileTrue(new DriveAndAutoRotate(subsystems.drivetrain, driveController, Math.toRadians(180)));
		driveController.b()
				.whileTrue(new DriveAndAutoRotate(subsystems.drivetrain, driveController, Math.toRadians(-90)));
		driveController.x()
				.whileTrue(new DriveAndAutoRotate(subsystems.drivetrain, driveController, Math.toRadians(90)));
		driveController.y()
				.whileTrue(new DriveAndAutoRotate(subsystems.drivetrain, driveController, Math.toRadians(0)));

		new Trigger(() -> subsystems.indexer.isCubeDetected())
				.onTrue(Commands.runOnce(() -> subsystems.leds.fillAndCommitColor(ColorConstants.GREEN),
						subsystems.leds));
		new Trigger(() -> subsystems.indexer.isCubeDetected())
				.onFalse(Commands.runOnce(() -> subsystems.leds.fillAndCommitColor(ColorConstants.RED),
						subsystems.leds));
		new Trigger(() -> HALUtil.getFPGAButton())
				.onTrue(Commands.either(
						Commands.runOnce(() -> subsystems.leds.stop()),
						Commands.runOnce(() -> subsystems.leds.start()),
						() -> subsystems.leds.isEnabled()).ignoringDisable(true));
		
		subsystems.indexer.setDefaultCommand(
				new IndexByController(subsystems.indexer, manipulatorController));
		subsystems.shooter.setDefaultCommand(
				new ShootByController(subsystems.shooter, manipulatorController));
		subsystems.intake.setDefaultCommand(
				new IntakeByController(subsystems.intake, manipulatorController));

		manipulatorController.leftStick().onTrue(
				Commands.runOnce(() -> subsystems.leds.setGamePieceColor(), subsystems.leds));
		manipulatorController.povUp().whileTrue(
				Commands.parallel(
						Commands.startEnd(subsystems.intake::up, subsystems.intake::disable, subsystems.intake),
						Commands.startEnd(subsystems.indexer::feed, subsystems.indexer::disable, subsystems.indexer))); // was
																														// Commands.startEnd(subsystems.indexer::feed,
																														// subsystems.indexer::disable,
																														// subsystems.indexer)
		manipulatorController.povDown().whileTrue(Scoring.outake(subsystems));
		manipulatorController.a().whileTrue(Scoring.spinToRPM(subsystems, GoalShooterRPM.HYBRID));
		manipulatorController.b().whileTrue(Scoring.spinToRPM(subsystems, GoalShooterRPM.MID));
		manipulatorController.y().whileTrue(Scoring.spinToRPM(subsystems, GoalShooterRPM.HIGH));
		manipulatorController.x().whileTrue(Scoring.spinToRPM(subsystems, GoalShooterRPM.MAX_POWER));
		manipulatorController.leftBumper().whileTrue(Scoring.spinToRPM(subsystems, GoalShooterRPM.FAR_HYBRID));
		manipulatorController.rightBumper().whileTrue(Scoring.intake(subsystems));
		// manipulatorController.leftStick().onTrue(new RainbowCycle(subsystems.leds);
		// manipulatorController.rightStick().onTrue(new FlameCycle(subsystems.leds));

	}

	/**
	 * Returns the autonomous command selected in the chooser.
	 *
	 * @return the command to run in autonomous
	 */
	public Command getAutonomousCommand() {
		return autonomous.getAutonomousCommand();
	}

	/** Adds the Shuffleboard tabs for the robot. */
	private void initShuffleboard() {
		// The "Operator" tab contains UI elements that enable the drive team to set up
		// and operate the robot.
		ShuffleboardTab operatorTab = Shuffleboard.getTab("Operator");

		autonomous.addShuffleboardLayout(operatorTab)
				.withPosition(0, 0)
				.withSize(2, 3);

		VideoSource video = new HttpCamera(
				"photonvision_Port_1183_MJPEG_Server",
				"http://10.9.48.11:1183/?action=stream",
				HttpCameraKind.kMJPGStreamer);
		operatorTab.add("PhotonVision", video)
				.withWidget(BuiltInWidgets.kCameraStream)
				.withPosition(2, 0)
				.withSize(4, 3);

		ShuffleboardLayout gridLayout = operatorTab.getLayout("Grid", BuiltInLayouts.kGrid)
				.withPosition(6, 0)
				.withSize(2, 3);

		gridLayout.addBoolean("Left High", () -> driveController.getHID().getPOV() == 315)
				.withPosition(0, 0);
		gridLayout.addBoolean("Left Mid", () -> driveController.getHID().getPOV() == 270)
				.withPosition(0, 1);
		gridLayout.addBoolean("Left Low", () -> driveController.getHID().getPOV() == 225)
				.withPosition(0, 2);
		gridLayout.addBoolean("Center High", () -> driveController.getHID().getPOV() == 0)
				.withPosition(1, 0);
		gridLayout.addBoolean("Center Mid", () -> driveController.getHID().getPOV() == -1)
				.withPosition(1, 1);
		gridLayout.addBoolean("Center Low", () -> driveController.getHID().getPOV() == 180)
				.withPosition(1, 2);
		gridLayout.addBoolean("Right High", () -> driveController.getHID().getPOV() == 45)
				.withPosition(2, 0);
		gridLayout.addBoolean("Right Mid", () -> driveController.getHID().getPOV() == 90)
				.withPosition(2, 1);
		gridLayout.addBoolean("Right Low", () -> driveController.getHID().getPOV() == 135)
				.withPosition(2, 2);

		ShuffleboardLayout indicatorLayout = operatorTab.getLayout("Indicators", BuiltInLayouts.kGrid)
				.withPosition(8, 0)
				.withSize(1, 3)
				.withProperties(Map.of("Number of Columns", 1, "Number of Rows", 4));
		indicatorLayout.addBoolean("Manual ] Mode", () -> enableManualControl)
				.withPosition(0, 0);
		indicatorLayout.addBoolean("Automatic Scoring Mode", () -> driveController.getHID().getLeftBumper())
				.withPosition(0, 1);
		indicatorLayout.addBoolean("Is Purple?", () -> subsystems.leds.isYellow())
				.withPosition(0, 2);

		// The "Preferences" tab UI elements that enable configuring robot-specific
		// settings.
		RobotPreferences.addShuffleBoardTab();

		// The subsystem-specific tabs are added for testing and should be disabled by
		// default.
		subsystems.drivetrain.addShuffleboardTab();
		subsystems.cubeVision.addShuffleboardTab();
		subsystems.aprilTag.addShuffleboardTab();
		subsystems.shooter.addShuffleBoardTab(subsystems.indexer::isCubeDetected);
	}
}
