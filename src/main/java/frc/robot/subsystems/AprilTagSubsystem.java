// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.subsystems;

import java.util.List;

import org.photonvision.PhotonCamera;
import org.photonvision.targeting.PhotonPipelineResult;
import org.photonvision.targeting.PhotonTrackedTarget;

import com.nrg948.preferences.RobotPreferences;
import com.nrg948.preferences.RobotPreferencesLayout;
import com.nrg948.preferences.RobotPreferencesValue;

import edu.wpi.first.cscore.HttpCamera;
import edu.wpi.first.cscore.VideoSource;
import edu.wpi.first.cscore.HttpCamera.HttpCameraKind;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInWidgets;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

/**
 * This subsystem is responsible for getting target information from
 * PhotonVision.
 */
@RobotPreferencesLayout(groupName = "AprilTag", row = 1, column = 4, width = 2, height = 1)
public class AprilTagSubsystem extends PhotonVisionSubsystemBase {
  @RobotPreferencesValue
  public static final RobotPreferences.BooleanValue enableTab = new RobotPreferences.BooleanValue(
      "AprilTag", "Enable Tab", false);

  /** Creates a new PhotonVisionSubsystem. */
  public AprilTagSubsystem() {
    super("Back");
  }
  
  /**
   * Adds a tab for April Tag in Shuffleboard.
   */
  public void addShuffleboardTab() {
    if (!enableTab.getValue()) {
      return;
    }

    ShuffleboardTab visionTab = Shuffleboard.getTab("April Tag");
    ShuffleboardLayout targetLayout = visionTab.getLayout("Target Info", BuiltInLayouts.kList)
        .withPosition(0, 0)
        .withSize(2, 3);
    targetLayout.addBoolean("Has Target", this::hasTargets);
    targetLayout.addDouble("Distance", this::getDistanceToBestTarget);
    targetLayout.addDouble("Angle", this::getAngleToBestTarget);

    VideoSource video = new HttpCamera("photonvision_Port_1182_MJPEG_Server", "http://10.9.48.11:1182/?action=stream",
        HttpCameraKind.kMJPGStreamer);
    visionTab.add("April Tag", video)
        .withWidget(BuiltInWidgets.kCameraStream)
        .withPosition(2, 0)
        .withSize(4, 3);
  }
}