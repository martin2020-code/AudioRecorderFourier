package com.example.audiorecorderfourier

import android.media.MediaPlayer
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.chip.Chip
import kotlinx.android.synthetic.main.activity_audio_player.*

class AudioPlayerActivity : AppCompatActivity() {
    private lateinit var mediaPlayer: MediaPlayer

    private lateinit var btnPlay : ImageButton
    private lateinit var btnBackward : ImageButton
    private lateinit var btnForward : ImageButton
    private lateinit var speedChip: Chip
    private lateinit var seekBar: SeekBar

    private lateinit var runnable: Runnable
    private lateinit var handler: Handler
    private var delay = 1000L
    private var jumpValue = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_player)

        var filePath = intent.getStringExtra("filepath")
        var fileName = intent.getStringExtra("filename")

        mediaPlayer = MediaPlayer()
        mediaPlayer.apply {
            setDataSource(filePath)
            prepare()
        }

        btnBackward = findViewById(R.id.btnBackward)
        btnForward = findViewById(R.id.btnForward)
        btnPlay = findViewById(R.id.btnPlay)
        speedChip = findViewById(R.id.chip)
        seekBar = findViewById(R.id.seekBar)

        handler = Handler(Looper.getMainLooper())
        runnable = Runnable{
            seekBar.progress = mediaPlayer.currentPosition
            handler.postDelayed(runnable, delay)
        }

        btnPlay.setOnClickListener{
            playPausePlayer()
        }

        playPausePlayer()
        seekBar.max = mediaPlayer.duration

        mediaPlayer.setOnCompletionListener {
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle, theme)
            handler.removeCallbacks(runnable)
        }

        btnForward.setOnClickListener(){
            mediaPlayer.seekTo(mediaPlayer.currentPosition + jumpValue)
            seekBar.progress += jumpValue
        }

        btnBackward.setOnClickListener(){
            mediaPlayer.seekTo(mediaPlayer.currentPosition - jumpValue)
            seekBar.progress -= jumpValue
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                if (p2)
                    mediaPlayer.seekTo(p1)
            }

            override fun onStartTrackingTouch(p0: SeekBar?) {}

            override fun onStopTrackingTouch(p0: SeekBar?) {}

        })
    }

    private fun playPausePlayer(){
        if(!mediaPlayer.isPlaying){
            mediaPlayer.start()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_pause_circle, theme)
            handler.postDelayed(runnable, delay)
        }else{
            mediaPlayer.pause()
            btnPlay.background = ResourcesCompat.getDrawable(resources, R.drawable.ic_play_circle, theme)
            handler.removeCallbacks(runnable)
        }
    }
}