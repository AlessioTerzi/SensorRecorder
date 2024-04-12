package com.example.sensorrecorder

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.sensorrecorder.databinding.FragmentFirstBinding
import com.google.firebase.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment(), SensorEventListener, AdapterView.OnItemSelectedListener {

    private var _binding: FragmentFirstBinding? = null

    private lateinit var sensorManager: SensorManager
    private var mSensor: Sensor? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var isRecording = false

    private lateinit var file: FileWriter

    private lateinit var fileUri: File

    private lateinit var storage: FirebaseStorage




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sensorManager = requireActivity().getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        storage = Firebase.storage


        val spinner: Spinner = binding.camminateSpinner
        var context = this.context
        if(context != null) {
            ArrayAdapter.createFromResource(
                context,
                R.array.camminate_array,
                android.R.layout.simple_spinner_item
            ).also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }
        }

        binding.buttonFirst.setOnClickListener {
            //findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
            if(isRecording){

                Log.i("CLICK", "isRecording = true")
                binding.textviewFirst.setText(R.string.not_recording)
                sensorManager.unregisterListener(this)
                uploadToServer(file)
                file!!.close()
                isRecording = false
            } else {
                //var directory = requireContext().applicationContext.getDir("csv_files", Context.MODE_WORLD_WRITEABLE)
                var spinner = binding.camminateSpinner
                var camminata = spinner.selectedItem.toString()
                var numeroPassi = binding.stepsNumber.text.toString()
                var nomeUtente = binding.testerName.text.toString()

                if(camminata == "" || numeroPassi == "" || nomeUtente == ""){
                    Toast.makeText(this.context,"Fornire i dati richiesti prima di procedere",Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                binding.textviewFirst.setText(R.string.recording)

                var date = Date()
                var directory = requireContext().getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                var fileName = "$nomeUtente $numeroPassi passi $camminata $date.csv"
                fileUri = File(directory, fileName)

                Log.i("NOME FILE", nomeUtente + numeroPassi + camminata)

                try {
                    fileUri.createNewFile().toString()
                } catch (e:java.lang.Exception){
                    Log.i("CLICK - FILECREATION", e.message.toString())
                }

                file = FileWriter(fileUri)
                sensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL)
                isRecording = true

                Log.i("CLICK", "isRecording = false " + fileUri.absolutePath)
            }
        }
    }

    private fun uploadToServer(fileArg: FileWriter){
        binding.textviewFirst.setText(R.string.uploading)

        var ref = storage.reference.child(fileUri.name)
        var uploadTask = ref.putStream(FileInputStream(fileUri))

        uploadTask.addOnFailureListener {
            // Handle unsuccessful uploads
            binding.textviewFirst.setText(R.string.uploading_fail)
        }.addOnSuccessListener { taskSnapshot ->
            // taskSnapshot.metadata contains file metadata such as size, content-type, etc.
            binding.textviewFirst.setText(R.string.uploading_success)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onSensorChanged(p0: SensorEvent?) {
        if(p0==null) {
            Log.i("ON SENSOR CHANGED", "SensorEvent is NULL")
            return
        }
        var data = p0.values
        if(file==null) {
            Log.i("ON SENSOR CHANGED", "FileWriter parameter is NULL")
            return
        }

        //Log.i("ON SENSOR CHANGED", "data: " + data[1].toString())
        file!!.append(data[0].toString() + "," + data[1].toString() + "," + data[2].toString() + "," + p0.timestamp.toString() + "\n")
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
        ;
    }

    override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
        TODO("Not yet implemented")
    }

    override fun onNothingSelected(p0: AdapterView<*>?) {
        TODO("Not yet implemented")
    }
}