package com.qrtransfer.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var scanButton: Button
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var saveButton: Button

    private var cameraProvider: ProcessCameraProvider? = null
    private var barcodeScanner: BarcodeScanner? = null
    private var isScanning = false

    private val scannedDataList = mutableListOf<String>()
    private var totalQRCodes = 0
    private var decodedFileData: ByteArray? = null
    private var isCompressed = false

    private val cameraPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, R.string.camera_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        initBarcodeScanner()
        checkCameraPermission()
    }

    private fun initViews() {
        previewView = findViewById(R.id.previewView)
        scanButton = findViewById(R.id.scanButton)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        saveButton = findViewById(R.id.saveButton)

        scanButton.setOnClickListener {
            if (isScanning) {
                stopScanning()
            } else {
                startScanning()
            }
        }

        saveButton.setOnClickListener {
            saveDecodedFile()
        }
    }

    private fun initBarcodeScanner() {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startCamera()
        } else {
            cameraPermissionRequest.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this)) { imageProxy ->
                        if (isScanning) {
                            processImageProxy(imageProxy)
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this,
                    cameraSelector,
                    preview,
                    imageAnalyzer
                )
            } catch (exc: Exception) {
                Toast.makeText(this, "相机启动失败: ${exc.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun startScanning() {
        isScanning = true
        scannedDataList.clear()
        totalQRCodes = 0
        decodedFileData = null
        isCompressed = false
        
        scanButton.text = getString(R.string.stop_scan)
        statusText.text = "开始扫描二维码..."
        progressBar.max = 100
        progressBar.progress = 0
        saveButton.isEnabled = false
    }

    private fun stopScanning() {
        isScanning = false
        scanButton.text = getString(R.string.scan_qr)
        statusText.text = "扫描已停止"
    }

    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )

            barcodeScanner?.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawData = barcode.rawValue
                        if (rawData != null) {
                            processQRCodeData(rawData)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // 扫描失败，忽略
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun processQRCodeData(rawData: String) {
        try {
            // 解析JSON数据
            val jsonData = org.json.JSONObject(rawData)
            val index = jsonData.getInt("index")
            val total = jsonData.getInt("total")
            val data = jsonData.getString("data")
            
            // 检查是否压缩
            if (jsonData.has("compressed")) {
                isCompressed = jsonData.getBoolean("compressed")
            }

            // 更新总数
            if (totalQRCodes == 0) {
                totalQRCodes = total
                progressBar.max = total
            }

            // 检查是否已经扫描过这个二维码
            if (index !in scannedDataList.indices || scannedDataList.getOrNull(index) != data) {
                // 确保列表足够大
                while (scannedDataList.size <= index) {
                    scannedDataList.add("")
                }
                scannedDataList[index] = data

                // 更新UI
                val scannedCount = scannedDataList.count { it.isNotEmpty() }
                statusText.text = getString(R.string.scanned_count, scannedCount, totalQRCodes)
                progressBar.progress = scannedCount

                // 检查是否全部扫描完成
                if (scannedCount == totalQRCodes) {
                    onAllQRCodesScanned()
                }
            }
        } catch (e: Exception) {
            // JSON解析失败，可能不是我们的二维码格式
        }
    }

    private fun onAllQRCodesScanned() {
        isScanning = false
        scanButton.text = getString(R.string.scan_qr)
        statusText.text = getString(R.string.decode_complete)
        
        // 解码并重建文件
        decodeAndRebuildFile()
    }

    private fun decodeAndRebuildFile() {
        try {
            statusText.text = getString(R.string.processing)
            
            // 合并所有数据块
            val combinedData = scannedDataList.joinToString("")
            
            // Base64解码
            val base64Decoded = Base64.getDecoder().decode(combinedData)
            
            // 如果压缩了，进行解压
            decodedFileData = if (isCompressed) {
                decompressGzip(base64Decoded)
            } else {
                base64Decoded
            }
            
            statusText.text = "解码成功！文件大小: ${decodedFileData?.size} 字节"
            saveButton.isEnabled = true
            
        } catch (e: Exception) {
            statusText.text = "解码失败: ${e.message}"
            Toast.makeText(this, "解码失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun decompressGzip(data: ByteArray): ByteArray {
        val inputStream = GZIPInputStream(ByteArrayInputStream(data))
        val outputStream = ByteArrayOutputStream()
        
        val buffer = ByteArray(1024)
        var len: Int
        while (inputStream.read(buffer).also { len = it } > 0) {
            outputStream.write(buffer, 0, len)
        }
        
        inputStream.close()
        outputStream.close()
        
        return outputStream.toByteArray()
    }

    private fun saveDecodedFile() {
        val fileData = decodedFileData ?: return
        
        try {
            // 创建输出目录
            val outputDir = File(getExternalFilesDir(null), "QRTransfer")
            outputDir.mkdirs()
            
            // 保存文件
            val outputFile = File(outputDir, "decoded_file")
            outputFile.writeBytes(fileData)
            
            Toast.makeText(
                this,
                "文件已保存到: ${outputFile.absolutePath}",
                Toast.LENGTH_LONG
            ).show()
            
            statusText.text = "文件已保存"
            
        } catch (e: Exception) {
            Toast.makeText(this, "保存失败: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        barcodeScanner?.close()
        cameraProvider?.unbindAll()
    }
}
