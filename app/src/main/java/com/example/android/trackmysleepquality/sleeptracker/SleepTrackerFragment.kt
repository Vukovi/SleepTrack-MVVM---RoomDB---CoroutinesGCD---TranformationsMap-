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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.findNavController
import com.example.android.trackmysleepquality.R
import com.example.android.trackmysleepquality.database.SleepDatabase
import com.example.android.trackmysleepquality.databinding.FragmentSleepTrackerBinding
import com.google.android.material.snackbar.Snackbar

/**
 * A fragment with buttons to record start and end times for sleep, which are saved in
 * a database. Cumulative data is displayed in a simple scrollable TextView.
 * (Because we have not learned about RecyclerView yet.)
 */
class SleepTrackerFragment : Fragment() {


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Referenca na bindovanje sa layoutom
        val binding: FragmentSleepTrackerBinding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_sleep_tracker, container, false)

        // Referenca aplikacije za koju je zakacen i ovaj fragment
        val application = requireNotNull(this.activity).application

        // Referenca na data source baze, tj referenca na DAO file
        val dataSource = SleepDatabase.getInstance(application).sleepDatabaseDao

        // Factory za viewModel
        val viewModelFactory = SleepTrackerViewModelFactory(dataSource,application)

        // Referenca viewModela, naravno preko Providera
        val viewModel = ViewModelProviders.of(this, viewModelFactory).get(SleepTrackerViewModel::class.java)

        // Veza sa tagom <data>, tacnije sa viewModelom koji je u layoutu napravljen radi bindovanja sa LiveData
        binding.viewModel = viewModel

        // Da bi fragment u opste mogao da prati azuriranja bindovanih LiveData
        binding.setLifecycleOwner(this)


        // Kad je doslo do prestanka pracenja spavanja, onStopTracking iz njegovog viewModela
        viewModel.navigateToSleepQuality.observe(this, Observer { night ->
            if (night != null) {
                this.findNavController().navigate(SleepTrackerFragmentDirections.actionSleepTrackerFragmentToSleepQualityFragment(night.nightId))
                viewModel.doneNavigating()
            }
        })


        // prati kad se pojavljuje snack bar
        viewModel.showSnackBarEvent.observe(this, Observer { snacked ->
            if (snacked == true) {
                Snackbar.make(activity!!.findViewById(android.R.id.content), getString(R.string.cleared_message), Snackbar.LENGTH_SHORT).show()

                viewModel.doneShowingSnacbar()
            }
        })


        return binding.root
    }
}
