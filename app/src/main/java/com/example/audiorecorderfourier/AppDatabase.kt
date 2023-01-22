package com.example.audiorecorderfourier

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = arrayOf(AudioRecord::class), version = 1)
abstract  class AppDatabase : RoomDatabase() {
    abstract fun audioRecordsDao() : AudioRecordDao
}