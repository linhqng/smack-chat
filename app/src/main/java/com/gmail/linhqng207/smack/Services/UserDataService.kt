package com.gmail.linhqng207.smack.Services

import android.graphics.Color
import com.gmail.linhqng207.smack.Controller.App
import java.util.*
import kotlin.collections.ArrayList

object UserDataService {
    var id = ""
    var avatarColor = ""
    var avatarName = ""
    var email = ""
    var name = ""

    fun logout() {
         id = ""
         avatarColor = ""
         avatarName = ""
         email = ""
         name = ""

        App.prefs.authToken = ""
        App.prefs.userEmail = ""
        App.prefs.isLoggedIn = false

        MessageService.clearMessages()
        MessageService.clearChannels()
    }

    fun returnAvatarColor(component: String) : Int {
        val strippedColor = component.replace("[", "")
            .replace("]","")
            .replace(",","").toString()


        val rgba = strippedColor.split(" ").map(String::toDouble).map{it * 255}.map(Double::toInt)



        return Color.rgb(rgba[0],rgba[1],rgba[2])
    }


}