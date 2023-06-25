package com.example.projet_pmr

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import io.github.sceneview.ar.ArSceneView
import io.github.sceneview.ar.arcore.LightEstimationMode
import io.github.sceneview.ar.node.ArModelNode
import io.github.sceneview.ar.node.PlacementMode
import io.github.sceneview.math.Position
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@ExperimentalGetImage
class NavActivity : AppCompatActivity() {

    private lateinit var sceneView: ArSceneView
    private lateinit var modelNode: ArModelNode
    private lateinit var scanButton: Button

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private var isModelPlaced = false

    private val handler = Handler()
    private var isCameraRunning = false
    private var imageAnalysis: ImageAnalysis? = null
    private lateinit var cameraProvider: ProcessCameraProvider

    private val startDelay = 2000L // Délai initial de 2 secondes
    private val stopDelay = 500L // Durée pendant laquelle la caméra reste active
    private var scene = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav)

        sceneView = findViewById<ArSceneView>(R.id.sceneView).apply {
            this.lightEstimationMode = LightEstimationMode.ENVIRONMENTAL_HDR_NO_REFLECTIONS
        }

        scanButton = findViewById<Button>(R.id.scanButton)
        scanButton.setOnClickListener {
            if (scene == true){
                sceneView.removeChild(modelNode)
            }
            if (allPermissionsGranted()) {
                startCameraWithDelay()
            } else {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.CAMERA), REQUEST_CODE_PERMISSIONS
                )
            }
        }
        // Initialize the camera executor
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Initialize the barcode scanner
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)

        // Demande la permission d'utiliser la caméra si on ne l'a pas
    }

    private fun allPermissionsGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        if (isCameraRunning) return

        val intent = intent
        val navigation = intent.getStringExtra("navigation")


        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis!!.setAnalyzer(cameraExecutor, BarcodeAnalyzer { barcode ->
                // Process the scanned QR code
                Log.d(TAG, "Scanned QR Code: $barcode")
                runOnUiThread {
                    Toast.makeText(this, "Scanned QR Code: $barcode", Toast.LENGTH_SHORT).show()
                    if (navigation != null) {
                        if (navigation.contains(barcode)) {
                            placeModel(barcode)
                        } else {
                            val intent = Intent(this, MapActivity::class.java)
                            intent.putExtra("currentPosition", barcode)
                            startActivity(intent)
                        }
                    }
                }
            })

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, imageAnalysis)
                isCameraRunning = true
            } catch (e: Exception) {
                // Gestion des erreurs...
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun stopCamera() {
        if (!isCameraRunning) return

        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
        cameraProvider.unbindAll()
        isCameraRunning = false
    }

    private fun startCameraWithDelay() {
        stopCamera()
        handler.postDelayed({
            startCamera()
            handler.postDelayed({
                stopCamera()
            }, stopDelay) // Arrêter la caméra après 1 seconde
        }, startDelay) // Délai initial de 2 secondes
    }

    private fun placeModel(barcode: String) {
        // Charger et placer l'objet uniquement lorsqu'on appuie sur le bouton
        val fileLocation: String
        val intent = intent
        val navigation = intent.getSerializableExtra("currentPath") as? MutableList<Point> ?: mutableListOf() //Récupération de l'itinéraire
        val posIndex: Int = navigation.indexOf(barcode as Point)
        val nextPos: Point = navigation.get(posIndex!! + 1) //Position à venir

        if (barcode.x < nextPos.x) { //Si la prochaine position est en dessous
            fileLocation = "models/down_arrow.glb"
        } else if (barcode.x > nextPos.x) { //Si la prochaine position est au dessus
            fileLocation = "models/up_arrow.glb"
        } else if (barcode.y < nextPos.y) { //Si la prochaine position est à gauche
            fileLocation = "models/left_arrow.glb"
        } else { //Si la prochaine position est à droite
            fileLocation = "models/right_arrow.glb"
        }
        modelNode = ArModelNode(PlacementMode.INSTANT).apply {
            loadModelGlbAsync(
                glbFileLocation = fileLocation,
                scaleToUnits = 1f,
                centerOrigin = Position(-0.5f),
            ) {
                sceneView.planeRenderer.isVisible = true
            }
        }
        sceneView.addChild(modelNode)
        scene = true
    }

    override fun onDestroy() {
        super.onDestroy()
        sceneView.destroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
    }
}

@ExperimentalGetImage
class BarcodeAnalyzer(private val barcodeListener: (barcode: String) -> Unit) :
    ImageAnalysis.Analyzer {

    private val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()

    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(
                mediaImage, imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner.process(inputImage)
                .addOnSuccessListener(OnSuccessListener<List<Barcode>> { barcodes ->
                    for (barcode in barcodes) {
                        barcodeListener(barcode.rawValue ?: "")
                    }
                })
                .addOnFailureListener(OnFailureListener {
                    Log.e(TAG, "Barcode scanning failed", it)
                })
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    companion object {
        private const val TAG = "BarcodeAnalyzer"
    }
}
