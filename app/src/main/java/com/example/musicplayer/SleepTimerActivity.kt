package com.example.musicplayer

import android.content.ComponentName
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors

class SleepTimerActivity : AppCompatActivity() {

    private lateinit var tvTimerStatus: TextView
    private lateinit var etMinutes: EditText
    private var countDownTimer: CountDownTimer? = null

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sleep_timer)

        tvTimerStatus = findViewById(R.id.tvTimerStatus)
        etMinutes = findViewById(R.id.etMinutes)
        val btnStartTimer = findViewById<Button>(R.id.btnStartTimer)
        val btnCancelTimer = findViewById<Button>(R.id.btnCancelTimer)

        // 连接后台播放服务
        val sessionToken = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(this, sessionToken).buildAsync()
        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
        }, MoreExecutors.directExecutor())

        // 点击开始倒计时
        btnStartTimer.setOnClickListener {
            val inputText = etMinutes.text.toString()
            if (inputText.isNotEmpty()) {
                val minutes = inputText.toLongOrNull()
                if (minutes != null && minutes > 0) {
                    // 将分钟转换为毫秒 (分钟 * 60秒 * 1000毫秒)
                    val millisInFuture = minutes * 60 * 1000L
                    startTimer(millisInFuture)
                } else {
                    Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "请输入分钟数", Toast.LENGTH_SHORT).show()
            }
        }

        // 点击取消
        btnCancelTimer.setOnClickListener {
            countDownTimer?.cancel()
            tvTimerStatus.text = "未设置定时"
            etMinutes.text.clear()
        }
    }

    private fun startTimer(millisInFuture: Long) {
        countDownTimer?.cancel()

        countDownTimer = object : CountDownTimer(millisInFuture, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = millisUntilFinished / 1000 / 60
                val seconds = millisUntilFinished / 1000 % 60
                tvTimerStatus.text = String.format("倒计时: %02d:%02d", minutes, seconds)
            }

            override fun onFinish() {
                tvTimerStatus.text = "未设置定时"
                mediaController?.pause()
                Toast.makeText(this@SleepTimerActivity, "睡眠定时结束，已暂停播放", Toast.LENGTH_SHORT).show()
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
        controllerFuture?.let { MediaController.releaseFuture(it) }
    }
}