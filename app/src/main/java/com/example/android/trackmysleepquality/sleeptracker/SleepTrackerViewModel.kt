/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel( val database: SleepDatabaseDao, application: Application) : AndroidViewModel(application) {

    // Pomocu JOBa mozemo da prekinemo izvresenje koje obavlja Coroutines
    private var viewModelJob = Job()

    // Scope mora da zna na koji thread se vraca i naravno, sta  Coroutines radi preko JOBa
    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    // Treba mi aktivni podatak iz baze
    private var tonight = MutableLiveData<SleepNight?>()

    // Kao i svi podaci koji se u bazi nalaze
    private val nights = database.getAllNights()
    // Da bih lepo predstavio podatke koje dobijam iz baze, koristim LiveData Transformations
    val nightsString = Transformations.map(nights) {nights ->
        formatNights(nights, application.resources)
    }

    // didSet iz Swifta
    val startButtonVisible = Transformations.map(tonight) {
        null == it // samo kad nema trenutnog podatka startBtn je vidljiv
    }
    // didSet iz Swifta
    val stopButtonVisible = Transformations.map(tonight) {
        null != it // samo kad ima trenutnog podatka stopBtn je vidljiv
    }
    // didSet iz Swifta
    val clearButtonVisible = Transformations.map(nights) {
        it?.isNotEmpty() // vidljiv je samo ako ima prethodnih merenja sna
    }

    // prikazivanje snack bara
    private var _showSnackBarEvent = MutableLiveData<Boolean>()
    val showSnackBarEvent: LiveData<Boolean>
        get() = _showSnackBarEvent
    fun doneShowingSnacbar(){
        _showSnackBarEvent.value = false
    }

    // Navigacija ka drugom fragmentu
    private val _navigateToSleepQuality = MutableLiveData<SleepNight?>()
    val navigateToSleepQuality: LiveData<SleepNight?>
        get() = _navigateToSleepQuality
    // resetovanje navigacije
    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }


    override fun onCleared() {
        super.onCleared()
        // Kada prestane upotreba viewModela, a izvrsenje Coroutines nema gde da se vrati, mora da se canceluje
        viewModelJob.cancel()
    }

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        // LAUNCH omogucava da se ne zablokira MAIN thread dok se ne obavi neko izvrsenje u njegovom bloku
        uiScope.launch {
            tonight.value = getTonightFromDatabase()
        }
    }

    // SUSPEND dodajem s'obzirom da ovu metodu zovem iz unutrasnjosti Coroutines, a takodje necu ni da mi blokira UI
    private suspend fun getTonightFromDatabase(): SleepNight? {
        // za vracanje trazenog podataka ovde pravim jos jedan Coroutines na sledeci nacin
        return withContext(Dispatchers.IO) {
            var night = database.getTonight()
            if (night?.endTimeMili != night?.startTimeMili) {
                night = null
            }
            night
        }
    }


    // Funkcija za poceatak pracenja, opet preko Coroutines
    fun onStartTracking() {
        uiScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            tonight.value = getTonightFromDatabase()
        }
    }
    // Uz poceatak pracenja ide prateca suspend funkcija
    private suspend fun insert(night: SleepNight) {
        withContext(Dispatchers.IO) {
            database.insert(night)
        }
    }


    // Funkcija za zavrsetak pracenja, opet preko Coroutines
    fun onStopTracking() {
        uiScope.launch {
            val oldNight = tonight.value ?: return@launch
            oldNight.endTimeMili = System.currentTimeMillis()
            update(oldNight)

            // okidanje navigacije
            _navigateToSleepQuality.value = oldNight
        }
    }
    // Uz zavrsetak pracenja ide prateca suspend funkcija
    private suspend fun update(night: SleepNight){
        withContext(Dispatchers.IO) {
            database.update(night)
        }
    }


    // Funkcija clear, opet preko Coroutines
    fun onClear() {
        uiScope.launch {
            clear()
            tonight.value = null
            _showSnackBarEvent.value = true
        }
    }
    // Uz clear ide prateca suspend funkcija
    private  suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }
}



