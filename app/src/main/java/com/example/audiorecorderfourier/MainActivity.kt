package com.example.audiorecorderfourier

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.*
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.room.Room
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.ObjectOutputStream
import java.text.SimpleDateFormat
import java.util.*

const val REQUEST_CODE = 200

class MainActivity : AppCompatActivity(), Timer.OnTimerTickListener {

    private lateinit var amplitudes: ArrayList<Float>
    private var permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO)
    private var permissionGranted = false

    private lateinit var recorder: MediaRecorder
    private var dirPath = ""
    private var filename = ""
    private var isRecording = false
    private var isPaused = false

    private var duration = ""

    private lateinit var vibrator: Vibrator

    private lateinit var timer: Timer

    private lateinit var db : AppDatabase

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        permissionGranted = ActivityCompat.checkSelfPermission(this, permissions[0]) == PackageManager.PERMISSION_GRANTED

        if (!permissionGranted)
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)

        db = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "audioRecords"
        ).build()

        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.peekHeight = 0
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        timer = Timer(this)
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        btnRecord.setOnClickListener{
            when{
                isPaused -> resumeRecorder()
                isRecording -> pauseRecorder()
                else -> startRecording()
            }

            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }

        btnList.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        btnDone.setOnClickListener{
            stopRecorder()
            Toast.makeText(this, "Record saved", Toast.LENGTH_SHORT).show()

            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetBG.visibility = View.VISIBLE
            filenameInput.setText(filename)
        }

        btnCancel.setOnClickListener {
            File("$dirPath$filename.mp3").delete()
            dismiss()
        }

        btnOK.setOnClickListener{
            dismiss()
            save()
        }

        bottomSheetBG.setOnClickListener{
            File("$dirPath$filename.mp3").delete()
            dismiss()
        }

        btnDelete.setOnClickListener{
            stopRecorder()
            File("$dirPath$filename.mp3").delete()
            Toast.makeText(this, "Record deleted", Toast.LENGTH_SHORT).show()
        }

        btnDelete.isClickable = false
    }

    private fun save(){
        val newFilename = filenameInput.text.toString()
        if (newFilename != filename){
            var newFile = File("$dirPath$newFilename.mp3")
            File("$dirPath$filename.mp3").renameTo(newFile)
        }

        var filePath = "$dirPath$newFilename.mp3"
        var timestamp = Date().time
        var ampsPath = "$dirPath$newFilename"

        try {
            var fos = FileOutputStream(ampsPath)
            var out = ObjectOutputStream(fos)
            out.writeObject(amplitudes)
            fos.close()
            out.close()
        }catch (e :IOException){}

        var record = AudioRecord(newFilename, filePath, timestamp, duration, ampsPath)

        GlobalScope.launch {
            db.audioRecordsDao().insert(record)
        }

        val filename_ = "/storage/self/primary/Documents/$newFilename.ipynb"
        var file_ = File(filename_)

        var codigo : String = """
            {
                 "cells": [
                  {
                   "cell_type": "code",
                   "execution_count": null,
                   "metadata": {},
                   "outputs": [],
                   "source": [
                    "interactive = True\n",
                    "default_directory = '/content/drive/MyDrive/FFT'\n",
                    "\n",
                    "from google.colab import drive\n",
                    "drive.mount('/content/drive')"
                   ]
                  },
                  {
                   "attachments": {},
                   "cell_type": "markdown",
                   "metadata": {},
                   "source": [
                    "determinar la carpecta donde esta el archivo de audio"
                   ]
                  },
                  {
                   "cell_type": "code",
                   "execution_count": null,
                   "metadata": {},
                   "outputs": [],
                   "source": [
                    "import subprocess\n",
                    "import os\n",
                    "\n",
                    "if not interactive:\n",
                    "  current_directory = 'modifica la ubicacion de la carpeta aqui'\n",
                    "  current_directory = f'/content/drive/MyDrive/{current_directory}'\n",
                    "else:\n",
                    "  if input('Cambiar el directorio por defecto?:') == 'si':\n",
                    "    current_directory = input()\n",
                    "  else:\n",
                    "    current_directory = default_directory\n",
                    "\n",
                    "os.chdir(current_directory)"
                   ]
                  },
                  {
                   "attachments": {},
                   "cell_type": "markdown",
                   "metadata": {},
                   "source": [
                    "Leer el archivo de audio y exportar los datos"
                   ]
                  },
                  {
                   "cell_type": "code",
                   "execution_count": null,
                   "metadata": {},
                   "outputs": [],
                   "source": [
                    "import numpy as np\n",
                    "import pandas as pd\n",
                    "import wave\n",
                    "from scipy.io.wavfile import read\n",
                    "\n",
                    "#Conversion de .mp3 a .wav\n",
                    "audio_file = '${newFilename}.mp3'\n",
                    "audio_file_wav = f'${newFilename}.wav'\n",
                    "\n",
                    "subprocess.run(['pip', 'install', 'ffmpeg-python'])\n",
                    "subprocess.call(['ffmpeg', '-i', audio_file, audio_file_wav])\n",
                    "\n",
                    "\n",
                    "#Lectura del archivo .wav\n",
                    "raw = wave.open('${newFilename}.wav')\n",
                    "width = raw.getsampwidth()\n",
                    "f_rate = raw.getframerate()\n",
                    "signal = raw.readframes(-1)\n",
                    "signal_size = np.frombuffer(signal, dtype=np.int16).size\n",
                    "t_audio = signal_size / (width*f_rate) * 2\n",
                    "raw.close()\n",
                    "\n",
                    "#exportar datos a un archivo .csv\n",
                    "t = np.linspace(0, t_audio, signal_size)\n",
                    "amplitud = read('${newFilename}.wav')[1]\n",
                    "\n",
                    "datos_df = pd.DataFrame({'tiempo (s)': t, 'amplitud': amplitud})\n",
                    "datos_df.to_csv('datos_completos.csv', index=False)"
                   ]
                  },
                  {
                   "cell_type": "markdown",
                   "metadata": {
                    "id": "znF49P0p3bbS"
                   },
                   "source": [
                    "Graficar la señal de audio"
                   ]
                  },
                  {
                   "cell_type": "code",
                   "execution_count": null,
                   "metadata": {
                    "id": "R6JclAfnWbBU"
                   },
                   "outputs": [],
                   "source": [
                    "t_min = 1.7 #modifica el tiempo minimo aqui\n",
                    "t_max = 1.855 #modifica el tiempo maximo aqui\n",
                    "\n",
                    "import matplotlib.pyplot as plt\n",
                    "\n",
                    "def ask_lims(msg, default):\n",
                    "    value = input(msg)\n",
                    "    if value == '':\n",
                    "        value = default\n",
                    "    else:\n",
                    "        value = float(value)\n",
                    "    return value\n",
                    "\n",
                    "#acotar los datos de archivo de audio\n",
                    "if interactive:\n",
                    "  while True:\n",
                    "      try:\n",
                    "          t_min = ask_lims('tiempo minimo:', 0)\n",
                    "          t_max = ask_lims('tiempo maximo:', t_audio) \n",
                    "      except:\n",
                    "          continue\n",
                    "      break\n",
                    "\n",
                    "cond = (t>=t_min) & (t<=t_max)\n",
                    "t_ = t[cond]\n",
                    "amplitud_ = amplitud[cond]\n",
                    "\n",
                    "#Exportar los datos acotados\n",
                    "datos_df = pd.DataFrame({'tiempo (s)': t_, 'amplitud': amplitud_})\n",
                    "datos_df.to_csv('datos.csv', index=False)\n",
                    "\n",
                    "#graficar los datos\n",
                    "fig = plt.figure(figsize=(6.4, 4.8))",
                    "plt.plot(t_, amplitud_)\n",
                    "plt.xlim(t_min, t_max)\n",
                    "plt.grid()\n",
                    "plt.xlabel('Tiempo (s)')\n",
                    "plt.ylabel('Amplitud')\n",
                    "plt.savefig('señal.png', dpi=500)\n",
                    "plt.show()"
                   ]
                  },
                  {
                   "cell_type": "markdown",
                   "metadata": {
                    "id": "kC_dftSS3hxt"
                   },
                   "source": [
                    "Transformada de fourier"
                   ]
                  },
                  {
                   "cell_type": "code",
                   "execution_count": null,
                   "metadata": {
                    "id": "c6jFQlqcWbBX"
                   },
                   "outputs": [],
                   "source": [
                    "frec_min = 350 #modifica la frecuencia minima aqui\n",
                    "frec_max = 480 #modifica la frecuencia maxima aqui\n",
                    "\n",
                    "from scipy.fft import fft\n",
                    "from scipy.signal import find_peaks\n",
                    "\n",
                    "\n",
                    "def fourier(t,x):\n",
                    "\n",
                    "    y = fft(x)\n",
                    "\n",
                    "    n_input = x.size\n",
                    "    n_output = y.size\n",
                    "\n",
                    "    PSD = np.abs(y) / n_input\n",
                    "    fase = np.angle(y) / n_input * 180 / np.pi\n",
                    "\n",
                    "    frec = 1 / ( t.max() - t.min() ) * np.arange(0,n_output)\n",
                    "\n",
                    "    return frec, PSD, fase\n",
                    "\n",
                    "#Realizar transformada de fourier\n",
                    "frec, PSD, fase = fourier(t, amplitud)\n",
                    "\n",
                    "\n",
                    "\n",
                    "#acotar los datos de la transformado\n",
                    "if interactive:\n",
                    "  while True:\n",
                    "      try:\n",
                    "          frec_min = ask_lims('Frecuencia minima:', 0)\n",
                    "          frec_max = ask_lims('Frecuencia maxima:', frec.max()) \n",
                    "      except:\n",
                    "          continue\n",
                    "      break\n",
                    "\n",
                    "cond = (frec>=frec_min) & (frec<=frec_max)\n",
                    "frec_ = frec[cond]\n",
                    "PSD_ = PSD[cond]\n",
                    "fase_ = fase[cond]\n",
                    "\n",
                    "\n",
                    "#Exportar los datos de la transformada\n",
                    "fft_df = pd.DataFrame({\n",
                    "    'Frecuencia (Hz)': frec_,\n",
                    "    'amplitud': PSD_,\n",
                    "    'Fase': fase_\n",
                    "})\n",
                    "\n",
                    "fft_df.to_csv('fft.csv', index=False)\n",
                    "\n",
                    "\n",
                    "\n",
                    "#Graficar los datos\n",
                    "fig = plt.figure(figsize=(6.4, 4.8))",
                    "plt.plot(frec_, PSD_ / PSD_.max(), '.-')\n",
                    "plt.grid()\n",
                    "plt.xlim(frec_min,frec_max)\n",
                    "plt.savefig('fourier_test', dpi=500)\n",
                    "plt.xlabel('Frecuencia (Hz)')\n",
                    "plt.ylabel('amplitud')\n",
                    "plt.show()"
                   ]
                  },
                  {
                   "cell_type": "markdown",
                   "metadata": {
                    "id": "6Jz1r-Cz3mrb"
                   },
                   "source": [
                    "Encontrar los picos"
                   ]
                  },
                  {
                   "cell_type": "code",
                   "execution_count": null,
                   "metadata": {
                    "id": "il4qKfpFWbBY"
                   },
                   "outputs": [],
                   "source": [
                    "if interactive:\n",
                    "  height = float(input('Filtrar desde:'))\n",
                    "else:\n",
                    "  height = 0.2 #modifica desde que altura filtra aqui\n",
                    "\n",
                    "peaks_idx = find_peaks(PSD_ / PSD_.max(), height=height)[0]\n",
                    "frec_[peaks_idx]"
                   ]
                  }
                 ],
                 "metadata": {
                  "kernelspec": {
                   "display_name": "Python 3",
                   "language": "python",
                   "name": "python3"
                  },
                  "language_info": {
                   "name": "python",
                   "version": "3.11.1 (tags/v3.11.1:a7a450f, Dec  6 2022, 19:58:39) [MSC v.1934 64 bit (AMD64)]"
                  },
                  "orig_nbformat": 4,
                  "vscode": {
                   "interpreter": {
                    "hash": "08f57244d2f3420109eb4a4246849699d07d1b1540cb0230b33c3c9bc0f89fba"
                   }
                  }
                 },
                 "nbformat": 4,
                 "nbformat_minor": 2
                }
              """.trimMargin()

        file_.writeText(codigo)
    }

    private fun dismiss(){
        bottomSheetBG.visibility = View.GONE
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        hideKeyBoard(filenameInput)

        Handler(Looper.getMainLooper()).postDelayed({
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        }, 100)
    }

    private fun hideKeyBoard(view: View){
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE)
            permissionGranted = grantResults[0] == PackageManager.PERMISSION_GRANTED
    }

    private fun pauseRecorder(){
        recorder.pause()
        isPaused = true
        btnRecord.setImageResource(R.drawable.ic_record)

        timer.pause()
    }

    private fun resumeRecorder(){
        recorder.resume()
        isPaused = false
        btnRecord.setImageResource(R.drawable.ic_pause)

        timer.start()
    }

    private fun startRecording(){
        if (!permissionGranted){
            ActivityCompat.requestPermissions(this, permissions, REQUEST_CODE)
            return
        }

        recorder = MediaRecorder()
        //dirPath = "${externalCacheDir?.absolutePath}/"
        //dirPath = "/storage/self/primary/Documents/FFT/"
        println(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath)

        ActivityCompat.requestPermissions(this, Array(1){ Manifest.permission.READ_EXTERNAL_STORAGE}, 23)
        dirPath = "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath}/"


        var simpleDateFormat = SimpleDateFormat("yyyy.MM.DD_hh.mm.ss")
        var date = simpleDateFormat.format(Date())
        filename = "audio_record_$date"


        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile("$dirPath$filename.mp3")

            try {
                prepare()
            }catch (e: IOException){}

            start()
        }

        btnRecord.setImageResource(R.drawable.ic_pause)
        isRecording = true
        isPaused = false

        timer.start()

        btnDelete.isClickable = true
        btnDelete.setImageResource(R.drawable.ic_delete)

        btnList.visibility = View.GONE
        btnDone.visibility = View.VISIBLE
    }

    private fun stopRecorder(){
        timer.stop()

        recorder.apply {
            stop()
            release()
        }

        isPaused = false
        isRecording = false

        btnList.visibility = View.VISIBLE
        btnDone.visibility = View.GONE

        btnDelete.isClickable = false
        btnDelete.setImageResource(R.drawable.ic_delete_disabled)
        btnRecord.setImageResource(R.drawable.ic_record)

        tvTimer.text = "00:00.00"
        amplitudes = waveformView.clear()
    }

    override fun onTimerTick(duration: String) {
        tvTimer.text = duration
        this.duration = duration.dropLast(3)
        waveformView.addAmplitude(recorder.maxAmplitude.toFloat())
    }
}