package com.example.catscam

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.*
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import android.media.MediaPlayer
import android.view.MotionEvent
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private var playSoundPet: MediaPlayer? = null
    private var playSoundMouse: MediaPlayer? = null
    private var playSoundWhistle: MediaPlayer? = null

    private lateinit var mInterstitialAd: InterstitialAd

    lateinit var mAdView : AdView

    var fotoapparat: Fotoapparat? = null
    val filename = "${System.currentTimeMillis()}.png"
    val sd = File(Environment.getExternalStoragePublicDirectory(
        Environment.DIRECTORY_DCIM), "Camera")
    val dest = File(sd, filename)
    var fotoapparatState : FotoapparatState? = null
    var cameraStatus : CameraState? = null
    var flashState: FlashState? = null

    private val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //CREATE MEDIA PLAYER
        playSoundPet = MediaPlayer.create(this, R.raw.catmeow)
        playSoundPet?.setOnPreparedListener{}

        playSoundMouse = MediaPlayer.create(this, R.raw.mouse)
        playSoundMouse?.setOnPreparedListener{}

        playSoundWhistle = MediaPlayer.create(this, R.raw.whistle)
        playSoundWhistle?.setOnPreparedListener{}

        //BUTTONS TO PLAY SOUND
        play_pet_sound.setOnTouchListener { _, event ->
            handleTouchPetSound(event)
            true
        }

        play_mouse_sound.setOnTouchListener { _, event ->
            handleTouchMouseSound(event)
            true
        }

        play_whistle_sound.setOnTouchListener { _, event ->
            handleTouchWhistleSound(event)
            loadAds()
            true
        }

        //BUTTONS TO PLAY SOUND END

        createFotoapparat()

        cameraStatus = CameraState.BACK
        flashState = FlashState.OFF
        fotoapparatState = FotoapparatState.OFF

        fab_camera.setOnClickListener {
            print("Taking photo")
            takePhoto()
        }

        fab_switch_camera.setOnClickListener {
            switchCamera()
        }

        fab_flash.setOnClickListener {
            changeFlashState()
            loadAds()
        }

        //Google AdMob Starts

        MobileAds.initialize(this,getString(R.string.AdMobAppId))

        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId= getString(R.string.AdMobAppInterstitialId)
        mInterstitialAd.loadAd(AdRequest.Builder().build())

        MobileAds.initialize(this) {}
        mAdView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        mAdView.loadAd(adRequest)

        //Google AdMob Ends

    }

    // FUNS TO SET PLAYERS
    private fun handleTouchPetSound(event: MotionEvent){
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                playSoundPet?.start()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                playSoundPet?.pause()
                playSoundPet?.seekTo(0)
            } else -> {
            print("other")
            }
        }
    }

    private fun handleTouchMouseSound(event: MotionEvent){
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                playSoundMouse?.start()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                playSoundMouse?.pause()
                playSoundMouse?.seekTo(0)
            } else -> {
            print("other")
            }
        }
    }

    private fun handleTouchWhistleSound(event: MotionEvent){
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                playSoundWhistle?.start()
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                playSoundWhistle?.pause()
                playSoundWhistle?.seekTo(0)
            } else -> {
            print("other")
        }
        }
    }

    // FUNS TO SET PLAYERS ENDS

    private fun loadAds() {
        if(mInterstitialAd.isLoaded)
            mInterstitialAd.show()
    }

    private fun createFotoapparat(){
        val cameraView = findViewById<CameraView>(R.id.camera_view)

        fotoapparat = Fotoapparat(
            context = this,
            view = cameraView,
            scaleType = ScaleType.CenterCrop,
            lensPosition = back(),
            logger = loggers(
                logcat()
            ),
            cameraErrorCallback = { error ->
                println("Recorder errors: $error")
            }
        )
    }

    private fun changeFlashState() {
        fotoapparat?.updateConfiguration(
            CameraConfiguration(
                flashMode = if(flashState == FlashState.TORCH) off() else torch()
            )
        )

        if(flashState == FlashState.TORCH) flashState = FlashState.OFF
        else flashState = FlashState.TORCH
    }

    private fun switchCamera() {
        fotoapparat?.switchTo(
            lensPosition =  if (cameraStatus == CameraState.BACK) front() else back(),
            cameraConfiguration = CameraConfiguration()
        )

        if(cameraStatus == CameraState.BACK) cameraStatus = CameraState.FRONT
        else cameraStatus = CameraState.BACK
    }

    private fun takePhoto() {
        if (hasNoPermissions()) {

            val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions,0)
        } else {
            println("Has all permissions!")
            fotoapparat
                ?.takePicture()
                ?.saveToFile(dest)
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onStart() {
        super.onStart()

        println("Onstart")

        if (hasNoPermissions()) {
            requestPermission()
        }else{
            fotoapparat?.start()
            fotoapparatState = FotoapparatState.ON
        }
    }

    private fun hasNoPermissions(): Boolean{
        return ContextCompat.checkSelfPermission(this,
            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this,
            Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission(){
        ActivityCompat.requestPermissions(this, permissions,0)
    }

    override fun onStop() {
        super.onStop()
        fotoapparat?.stop()
        FotoapparatState.OFF
    }

    override fun onPause() {
        super.onPause()
        println("OnPause")
    }

    override fun onResume() {
        super.onResume()
        println("OnResume")

        loadAds()

        println(fotoapparatState)

        if(!hasNoPermissions() && fotoapparatState == FotoapparatState.OFF){
            val intent = Intent(baseContext, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}

enum class CameraState{
    FRONT, BACK
}

enum class FlashState{
    TORCH, OFF
}

enum class FotoapparatState{
    ON, OFF
}
