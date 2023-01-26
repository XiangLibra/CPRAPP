/* Copyright 2022 Lin Yi. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

/** 本应用主要对 Tensorflow Lite Pose Estimation 示例项目的 MainActivity.kt
 *  文件进行了重写，示例项目中其余文件除了包名调整外基本无改动，原版权归
 *  The Tensorflow Authors 所有 */

package lyi.linyi.posemon

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Process
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import lyi.linyi.posemon.camera.CameraSource
import lyi.linyi.posemon.data.Device
import lyi.linyi.posemon.data.Camera
import lyi.linyi.posemon.ml.ModelType
import lyi.linyi.posemon.ml.MoveNet
import lyi.linyi.posemon.ml.PoseClassifier

class MainActivity : AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    /** 为视频画面创建一个 SurfaceView */
    private lateinit var surfaceView: SurfaceView

    /** 修改默认计算设备：CPU、GPU、NNAPI（AI加速器） */
    private var device = Device.CPU
    /** 修改默认摄像头：FRONT、BACK */
    private var selectedCamera = Camera.BACK

    /** 定义几个计数器 */
    private var forwardheadCounter = 0
    private var crosslegCounter = 0
    private var standardCounter = 0
    private var missingCounter = 0
//    private var KIM1Counter = 0
//    private var KIM2Counter = 0
//    private var KIM3Counter = 0
//    private var KIM4Counter = 0
    private var L165Counter = 0
    private var R165Counter = 0
    private var TGreater165Counter = 0
    private var TLess165Counter = 0

    /**定義一個歷史姿態寄存器*/
    private var poseRegister = "standard"

    /**設置一個用來顯示Debug 信息的 TextView */
    private lateinit var tvDebug: TextView


    private lateinit var ivStatus: ImageView

    private lateinit var tvFPS: TextView
    private lateinit var tvScore: TextView
    private lateinit var spnDevice: Spinner
    private lateinit var spnCamera: Spinner

    private var cameraSource: CameraSource? = null
    private var isClassifyPose = true

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                /**得到用戶相機授權後，程序開始運行 */
                openCamera()
            } else {
                /** 提示用户“未獲得相機權限制，應用無法運行” */
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission))
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }

    private var changeDeviceListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            changeDevice(position)
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {
            /** 如果用户未選擇運算設備，使用默認設備進行計算*/
        }
    }

    private var changeCameraListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(p0: AdapterView<*>?, view: View?, direction: Int, id: Long) {
            changeCamera(direction)
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
            /**如果使用用戶未選擇攝像頭，使用默認攝像頭進行拍攝 */
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /**程序運行時保持畫面常亮 */
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        tvScore = findViewById(R.id.tvScore)

        /** 用来顯示 Debug 信息 */
        tvDebug = findViewById(R.id.tvDebug)

        /**用來顯示當前坐姿狀態 */
        ivStatus = findViewById(R.id.ivStatus)

        tvFPS = findViewById(R.id.tvFps)
        spnDevice = findViewById(R.id.spnDevice)
        spnCamera = findViewById(R.id.spnCamera)
        surfaceView = findViewById(R.id.surfaceView)
        initSpinner()
        if (!isCameraPermissionGranted()) {
            requestPermission()
        }
    }

    override fun onStart() {
        super.onStart()
        openCamera()
    }

    override fun onResume() {
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() {
        cameraSource?.close()
        cameraSource = null
        super.onPause()
    }

    /** 检查相机权限是否有授权 */
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission(
            Manifest.permission.CAMERA,
            Process.myPid(),
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun openCamera() {
        /** 音频播放 */
//        val crosslegPlayer = MediaPlayer.create(this, R.raw.crossleg)
//        val forwardheadPlayer = MediaPlayer.create(this, R.raw.forwardhead)
        val standardPlayer = MediaPlayer.create(this, R.raw.standard)
        val armvoicePlayer= MediaPlayer.create(this, R.raw.armvoice)

        var crosslegPlayerFlag = true
        var forwardheadPlayerFlag = true
        var standardPlayerFlag = true

        var armvoicePlayerFlag=true



        var KIM1PlayerFlag = true
        var KIM2PlayerFlag = true
        var KIM3PlayerFlag = true
        var KIM4PlayerFlag = true


        if (isCameraPermissionGranted()) {
            if (cameraSource == null) {
                cameraSource =
                    CameraSource(surfaceView, selectedCamera, object : CameraSource.CameraSourceListener {
                        override fun onFPSListener(fps: Int) {

                            /** 解释一下，tfe_pe_tv 的意思：tensorflow example、pose estimation、text view */
                            tvFPS.text = getString(R.string.tfe_pe_tv_fps, fps)
                        }

                        /**對檢測結果進行處理*/
                        override fun onDetectedInfo(
                            personScore: Float?,
                            poseLabels: List<Pair<String, Float>>?
                        ) {
                            tvScore.text = getString(R.string.tfe_pe_tv_score, personScore ?: 0f)

                            /** 分析目標姿態，給出提示*/
                            if (poseLabels != null && personScore != null && personScore > 0.3) {
                                missingCounter = 0
                                val sortedLabels = poseLabels.sortedByDescending { it.second }
                                when (sortedLabels[0].first) {
//                                    "forwardhead" -> {
//                                        crosslegCounter = 0
//                                        standardCounter = 0
//                                        L165Counter  = 0
//                                        R165Counter  = 0
//                                        TGreater165Counter = 0
//                                        TLess165Counter = 0
//                                        if (poseRegister == "forwardhead") {
//                                            forwardheadCounter++
//                                        }
//                                        poseRegister = "forwardhead"
//
//                                        /** 显示当前坐姿状态：脖子前伸 */
//                                        if (forwardheadCounter > 60) {

                                            /** 播放提示音 */
//                                            if (forwardheadPlayerFlag) {
//                                                forwardheadPlayer.start()
//                                            }
//                                            standardPlayerFlag = true
//                                            crosslegPlayerFlag = true
//                                            forwardheadPlayerFlag = false
//
//                                            ivStatus.setImageResource(R.drawable.forwardhead_confirm)
//                                        } else if (forwardheadCounter > 10) {
//                                            ivStatus.setImageResource(R.drawable.forwardhead_suspect)
//                                        }
//
//                                        /** 显示 Debug 信息 */
//                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $forwardheadCounter")
//                                    }
//                                    "crossleg" -> {
//                                        forwardheadCounter = 0
//                                        standardCounter = 0
//                                        L165Counter  = 0
//                                        R165Counter  = 0
//                                        TGreater165Counter = 0
//                                        TLess165Counter = 0
//                                        if (poseRegister == "crossleg") {
//                                            crosslegCounter++
//                                        }
//                                        poseRegister = "crossleg"
//
//                                        /** 显示当前坐姿状态：翘二郎腿 */
//                                        if (crosslegCounter > 60) {
//
//                                            /** 播放提示音 */
//                                            if (crosslegPlayerFlag) {
//                                                crosslegPlayer.start()
//                                            }
//                                            standardPlayerFlag = true
//                                            crosslegPlayerFlag = false
//                                            forwardheadPlayerFlag = true
//                                            ivStatus.setImageResource(R.drawable.crossleg_confirm)
//                                        } else if (crosslegCounter > 10) {
//                                            ivStatus.setImageResource(R.drawable.crossleg_suspect)
//                                        }
//
//                                        /** 显示 Debug 信息 */
//                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $crosslegCounter")
//                                    }
                                    "左手小於165度" -> {
//                                        crosslegCounter = 0
//                                        standardCounter = 0




                                        if (poseRegister == "左手小於165度") {
                                            L165Counter++
                                        }
                                        poseRegister = "左手小於165度"

                                        /** 显示当前坐姿状态：脖子前伸 */
                                        if (L165Counter > 50) {

                                            /** 播放提示音 */
                                            if (armvoicePlayerFlag) {
                                            armvoicePlayer.start()
                                            }

                                            armvoicePlayerFlag=false
//                                            standardPlayerFlag = true
//                                            crosslegPlayerFlag = true
//                                            forwardheadPlayerFlag = true
//                                            KIM1PlayerFlag=false
//                                            KIM2PlayerFlag=true
//                                            KIM3PlayerFlag=true
//                                            KIM4PlayerFlag=true

                                            ivStatus.setImageResource(R.drawable.l165)
                                        } else if (L165Counter > 10) {
                                            R165Counter  = 0
                                            TGreater165Counter = 0
                                            TLess165Counter = 0
                                            ivStatus.setImageResource(R.drawable.no_target)
                                            armvoicePlayerFlag=true
                                        }

                                        /** 顯示 Debug 信息 */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $L165Counter")
                                    }
                                    "右手小於165度" -> {
//                                        crosslegCounter = 0
//                                        standardCounter = 0



                                        if (poseRegister == "右手小於165度") {
                                            R165Counter++
                                        }
                                        poseRegister = "右手小於165度"

                                        /** 顯示當前坐姿狀態：脖子前伸 */
                                        if (R165Counter > 50) {

                                            /** 播放提示音 */
                                            if (armvoicePlayerFlag) {
                                            armvoicePlayer.start()
                                            }

                                            armvoicePlayerFlag=false
//                                            standardPlayerFlag = true
//                                            crosslegPlayerFlag = true
//                                            forwardheadPlayerFlag = true
//                                            KIM1PlayerFlag=true
//                                            KIM2PlayerFlag=false
//                                            KIM3PlayerFlag=true
//                                            KIM4PlayerFlag=true

                                            ivStatus.setImageResource(R.drawable.r165)
                                        } else if (R165Counter > 10) {
                                            L165Counter  = 0

                                            TGreater165Counter = 0
                                            TLess165Counter = 0
                                            ivStatus.setImageResource(R.drawable.no_target)
                                            armvoicePlayerFlag=true
                                        }

                                        /** 顯示 Debug 信息 */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $R165Counter")
                                    }
                                    "雙手大於165度" -> {
//                                        crosslegCounter = 0
//                                        standardCounter = 0


                                        if (poseRegister == "雙手大於165度") {
                                            TGreater165Counter ++
                                        }
                                        poseRegister = "雙手大於165度"

                                        /** 顯示當前坐姿狀態：脖子前伸 */
                                        if (TGreater165Counter  > 50) {

                                            /** 播放提示音 */
                                            if (armvoicePlayerFlag) {
                                                armvoicePlayer.start()
                                            }
                                            armvoicePlayerFlag=false


//                                            standardPlayerFlag = true
//                                            crosslegPlayerFlag = true
//                                            forwardheadPlayerFlag = true
//                                            KIM1PlayerFlag=true
//                                            KIM2PlayerFlag=true
//                                            KIM3PlayerFlag=false
//                                            KIM4PlayerFlag=true

                                            ivStatus.setImageResource(R.drawable.tgreater165)
                                        } else if (TGreater165Counter  > 10) {
                                            L165Counter  = 0
                                            R165Counter = 0

                                            TLess165Counter = 0
                                            ivStatus.setImageResource(R.drawable.no_target)
                                            armvoicePlayerFlag=true
                                        }

                                        /** 顯示 Debug 信息 */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $TGreater165Counter")
                                    }
                                    "雙手小於165度" -> {
//                                        crosslegCounter = 0
//                                        standardCounter = 0



                                        if (poseRegister == "雙手小於165度") {
                                            TLess165Counter++
                                        }
                                        poseRegister = "雙手小於165度"

                                        /** 顯示當前坐姿狀態：脖子前伸 */
                                        if (TLess165Counter > 50) {

                                            /** 播放提示音 */
                                            if (armvoicePlayerFlag) {
                                                armvoicePlayer.start()
                                            }
                                            armvoicePlayerFlag=false

//                                            standardPlayerFlag = true
//                                            crosslegPlayerFlag = true
//                                            forwardheadPlayerFlag = true
//                                            KIM1PlayerFlag=true
//                                            KIM2PlayerFlag=true
//                                            KIM3PlayerFlag=true
//                                            KIM4PlayerFlag=false

                                            ivStatus.setImageResource(R.drawable.tless165)
                                        } else if (TLess165Counter > 10) {
                                            L165Counter  = 0
                                            R165Counter = 0
                                            TGreater165Counter = 0
                                            ivStatus.setImageResource(R.drawable.no_target)
                                            armvoicePlayerFlag=true
                                        }

                                        /** 顯示 Debug 信息 */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $TLess165Counter")
                                    }
                                    else -> {
                                        forwardheadCounter = 0
                                        crosslegCounter = 0
                                        L165Counter  = 0
                                        R165Counter  = 0
                                        TGreater165Counter = 0
                                        TLess165Counter = 0
                                        if (poseRegister == "standard") {
                                            standardCounter++
                                        }
                                        poseRegister = "standard"
                                        ivStatus.setImageResource(R.drawable.no_target)
                                        /** 顯示當前坐姿狀態：标准 */
                                        if (standardCounter > 20) {

                                            /** 播放提示音：坐姿标准 */
//                                            if (standardPlayerFlag) {
//                                                standardPlayer.start()
//                                            }
//                                            standardPlayerFlag = false
//                                            crosslegPlayerFlag = true
//                                            forwardheadPlayerFlag = true
//                                            KIM1PlayerFlag=true
//                                            KIM2PlayerFlag=true
//                                            KIM3PlayerFlag=true
//                                            KIM4PlayerFlag=true

                                           // ivStatus.setImageResource(R.drawable.standard)
                                        }

                                        /** 顯示 Debug 信息 */
                                        tvDebug.text = getString(R.string.tfe_pe_tv_debug, "${sortedLabels[0].first} $standardCounter")
                                    }
                                }


                            }
                            else {
                                missingCounter++
                                if (missingCounter > 30) {
                                    ivStatus.setImageResource(R.drawable.no_target)
                                }

                                /** 顯示 Debug 信息 */
                                tvDebug.text = getString(R.string.tfe_pe_tv_debug, "未偵測到人 $missingCounter")
                            }
                        }
                    }).apply {
                        prepareCamera()
                    }
                isPoseClassifier()
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera()
                }
            }
            createPoseEstimator()
        }
    }

    private fun isPoseClassifier() {
        cameraSource?.setClassifier(if (isClassifyPose) PoseClassifier.create(this) else null)
    }

    /** 初始化運算設備選項菜單（CPU、GPU、NNAPI） */
    private fun initSpinner() {
        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_device_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnDevice.adapter = adapter
            spnDevice.onItemSelectedListener = changeDeviceListener
        }

        ArrayAdapter.createFromResource(
            this,
            R.array.tfe_pe_camera_name, android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

            spnCamera.adapter = adapter
            spnCamera.onItemSelectedListener = changeCameraListener
        }
    }

    /** 在程序運行過程中切換運行計算設備*/
    private fun changeDevice(position: Int) {
        val targetDevice = when (position) {
            0 -> Device.CPU
            1 -> Device.GPU
            else -> Device.NNAPI
        }
        if (device == targetDevice) return
        device = targetDevice
        createPoseEstimator()
    }

    /**在程序運行過程中切換攝像頭 */
    private fun changeCamera(direaction: Int) {
        val targetCamera = when (direaction) {
            0 -> Camera.BACK
            else -> Camera.FRONT
        }
        if (selectedCamera == targetCamera) return
        selectedCamera = targetCamera

        cameraSource?.close()
        cameraSource = null
        openCamera()
    }

    private fun createPoseEstimator() {
        val poseDetector = MoveNet.create(this, device, ModelType.Thunder)
        poseDetector.let { detector ->
            cameraSource?.setDetector(detector)
        }
    }

    private fun requestPermission() {
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) -> {
                openCamera()
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.CAMERA
                )
            }
        }
    }

    /**顯示報錯信息*/
    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // pass
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}
