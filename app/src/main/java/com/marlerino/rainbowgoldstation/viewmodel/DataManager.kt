package com.marlerino.rainbowgoldstation.viewmodel

import android.content.Context
import java.util.Random

class DataManager(private val context: Context) {

    class Session(
        val computer: String, val time: String, val positions: List<Int>,
        val code: String
    )

    private val sharedPreferences =
        context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
    private val editor = sharedPreferences.edit()

    private fun initComputers() {
        val computers = arrayOf(
            "A123",
            "B456",
            "C789",
            "D123",
            "E456",
            "F789",
            "J123",
            "K456",
            "L789",
            "M123",
            "N456",
            "O789",
        )
        for (idx in computers.indices) {
            editor.putString("computer_$idx", computers[idx]).apply()
        }
    }

    fun getCurrentComputer(): String {
        val currentComputer = sharedPreferences.getString("session_computer", "")!!
        if (currentComputer != "") {
            return currentComputer
        }
        val random = Random()
        val num = random.nextInt(12)
        var computer = sharedPreferences.getString("computer_$num", "")!!
        if (computer == "") {
            initComputers()
            computer = sharedPreferences.getString("computer_$num", "")!!
        }
        editor.putString("session_computer", computer).apply()
        return computer
    }

    fun saveSession(time: String, code: String, positions: List<Int>) {
        editor.putString("session_computer", sharedPreferences.getString("session_computer", ""))
            .apply()
        editor.putString("session_time", time).apply()
        editor.putString("session_code", code).apply()
        val sb = StringBuilder()
        sb.append(positions[0])
        sb.append("_")
        sb.append(positions[1])
        sb.append("_")
        sb.append(positions[2])
        editor.putString("session_positions", sb.toString()).apply()
    }

    fun clearSession() {
        editor.putString("session_computer", "").apply()
        editor.putString("session_time", "").apply()
        editor.putString("session_code", "").apply()
        editor.putString("session_positions", "").apply()
    }

    fun loadSession(): Session {
        val positions = mutableListOf<Int>()
        for (pos in sharedPreferences.getString("session_positions", "")!!.split('_')) {
            if (pos != "") {
                positions.add(Integer.parseInt(pos))
            }
        }
        return Session(
            sharedPreferences.getString("session_computer", "")!!,
            sharedPreferences.getString("session_time", "")!!,
            positions,
            sharedPreferences.getString("session_code", "")!!
        )
    }
    fun saveRun(){
        editor.putInt("run_num", sharedPreferences.getInt("run_num", 0) + 1).apply()
    }
    fun getRunNum():Int{
    return sharedPreferences.getInt("run_num", 0)
    }

    fun getConfig(): String {
        return sharedPreferences.getString("config", "")!!
    }

    fun saveConfig(config:String){
        editor.putString("config", config).apply()
    }

}