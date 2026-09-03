package com.example.arucodetector

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2
import org.opencv.android.OpenCVLoader
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.objdetect.ArucoDetector
import org.opencv.objdetect.DetectorParameters
import org.opencv.objdetect.Objdetect
import java.util.ArrayList

class MainActivity : AppCompatActivity(), CvCameraViewListener2 {

    private companion object {
        private const val TAG = "ArUcoDetector"
        private const val CAMERA_PERMISSION_REQUEST_CODE = 101
    }

    private lateinit var cameraView: CameraBridgeViewBase
    private var detectors: List<Pair<String, ArucoDetector>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        cameraView = findViewById(R.id.camera_view)
        cameraView.visibility = SurfaceView.VISIBLE
        cameraView.setCvCameraViewListener(this)
        cameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_BACK)
        
        // ASPECT RATIO OPTIMIZATION: Use 16:9 ratio to prevent image stretching on modern devices
        // Standard HD 1280x720 is 16:9
        cameraView.setMaxFrameSize(1280, 720)

        if (!checkCameraPermission()) {
            requestCameraPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkCameraPermission()) {
            startOpenCvCamera()
        }
    }

    private fun startOpenCvCamera() {
        if (OpenCVLoader.initLocal()) {
            Log.d(TAG, "OpenCV initialized successfully")
            cameraView.post {
                cameraView.setCameraPermissionGranted()
                cameraView.enableView()
            }
        } else {
            Log.e(TAG, "OpenCV initialization failed")
        }
    }

    override fun onPause() {
        super.onPause()
        cameraView.disableView()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraView.disableView()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startOpenCvCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
        try {
            val parameters = DetectorParameters()
            // Optimized parameters for mobile handheld use
            parameters.set_minMarkerPerimeterRate(0.03) 
            parameters.set_adaptiveThreshConstant(7.0)
            parameters.set_errorCorrectionRate(0.6)

            // Support both 4x4 (notebook) and 6x6 (user's other markers)
            detectors = listOf(
                "4X4_100" to ArucoDetector(Objdetect.getPredefinedDictionary(Objdetect.DICT_4X4_100), parameters),
                "6X6_250" to ArucoDetector(Objdetect.getPredefinedDictionary(Objdetect.DICT_6X6_250), parameters)
            )
            
            Log.d(TAG, "Detectors initialized: 4x4 and 6x6")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing ArUco Detectors: ${e.message}")
        }
    }

    override fun onCameraViewStopped() {
        // Class-level Mat/List were removed as we use local ones for memory safety
    }

    override fun onCameraFrame(inputFrame: CvCameraViewFrame): Mat {
        val rgba = inputFrame.rgba()
        val gray = inputFrame.gray()

        var detectedAny = false
        var detectedDictName = ""

        for ((name, detector) in detectors) {
            val currentCorners = ArrayList<Mat>()
            val currentIds = Mat()

            try {
                detector.detectMarkers(gray, currentCorners, currentIds)

                if (!currentIds.empty()) {
                    detectedAny = true
                    detectedDictName = name
                    
                    // Display IDs and Dictionary Name
                    for (i in 0 until currentCorners.size) {
                        val id = currentIds.get(i, 0)[0].toInt()
                        val corner = currentCorners[i]
                        
                        // Extract 4 corners from Mat (1x4 CV_32FC2)
                        val p0 = Point(corner.get(0, 0)[0], corner.get(0, 0)[1])
                        val p1 = Point(corner.get(0, 1)[0], corner.get(0, 1)[1])
                        val p2 = Point(corner.get(0, 2)[0], corner.get(0, 2)[1])
                        val p3 = Point(corner.get(0, 3)[0], corner.get(0, 3)[1])
                        
                        // 1. Draw GREEN Box (Thick lines for visibility)
                        val green = Scalar(0.0, 255.0, 0.0, 255.0)
                        Imgproc.line(rgba, p0, p1, green, 4)
                        Imgproc.line(rgba, p1, p2, green, 4)
                        Imgproc.line(rgba, p2, p3, green, 4)
                        Imgproc.line(rgba, p3, p0, green, 4)

                        // 2. Draw RED ID Number in Center
                        val centerX = (p0.x + p1.x + p2.x + p3.x) / 4.0
                        val centerY = (p0.y + p1.y + p2.y + p3.y) / 4.0
                        Imgproc.putText(
                            rgba, "$id", Point(centerX - 15, centerY + 15),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, Scalar(255.0, 0.0, 0.0, 255.0), 3
                        )

                        // 3. Draw BLUE ID Label
                        Imgproc.putText(
                            rgba, "ID: $id", Point(p0.x, p0.y - 20),
                            Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, Scalar(0.0, 0.0, 255.0, 255.0), 3
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Detection error ($name): ${e.message}")
            } finally {
                currentIds.release()
                for (mat in currentCorners) mat.release()
            }
        }

        // Status Feedback
        val statusText = if (detectedAny) "ArUco Detected: $detectedDictName" else "No ArUco Detected"
        val statusColor = if (detectedAny) Scalar(0.0, 255.0, 0.0, 255.0) else Scalar(255.0, 255.0, 255.0, 255.0)

        // Line 1: Status
        Imgproc.putText(
            rgba, statusText, Point(30.0, 50.0),
            Imgproc.FONT_HERSHEY_SIMPLEX, 0.8, statusColor, 2
        )

        // Line 2: Developer Info
        Imgproc.putText(
            rgba, "Developer : Omid Shabanali", Point(30.0, 90.0),
            Imgproc.FONT_HERSHEY_SIMPLEX, 0.7, statusColor, 2
        )

        return rgba
    }
}
