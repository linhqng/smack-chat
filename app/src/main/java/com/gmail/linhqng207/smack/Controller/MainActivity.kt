package com.gmail.linhqng207.smack.Controller

import android.content.*
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.gmail.linhqng207.smack.Adapters.MessageAdapter
import com.gmail.linhqng207.smack.Model.Channel
import com.gmail.linhqng207.smack.Model.Message
import com.gmail.linhqng207.smack.R
import com.gmail.linhqng207.smack.Services.AuthService
import com.gmail.linhqng207.smack.Services.MessageService
import com.gmail.linhqng207.smack.Services.UserDataService
import com.gmail.linhqng207.smack.Utilities.BROADCAST_USER_DATA_CHANGE
import com.gmail.linhqng207.smack.Utilities.SOCKET_URL
import io.socket.client.IO
import io.socket.emitter.Emitter
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.android.synthetic.main.nav_header_main.*

class MainActivity : AppCompatActivity() {

    val socket = IO.socket(SOCKET_URL)

    lateinit var channelAdapter: ArrayAdapter<Channel>
    lateinit var messageAdapter: MessageAdapter

    var selectedChannel : Channel? = null

    private fun setupAdapters(){
        channelAdapter = ArrayAdapter(this,android.R.layout.simple_list_item_1, MessageService.channels)
        channel_list.adapter = channelAdapter

        messageAdapter = MessageAdapter(this,MessageService.messages)
        messageListView.adapter = messageAdapter
        val layoutManager = LinearLayoutManager(this)
        messageListView.layoutManager = layoutManager
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        socket.connect()
        socket.on("channelCreated",onNewChannel)
        socket.on("messageCreated",onNewMessage)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        setupAdapters()

        LocalBroadcastManager.getInstance(this).registerReceiver(userDataChangeReceiver,
            IntentFilter(BROADCAST_USER_DATA_CHANGE))

        channel_list.setOnItemClickListener { _, _, i, _ ->
            selectedChannel = MessageService.channels[i]
            drawer_layout.closeDrawer(GravityCompat.START)
            updateWithChannel()
        }

        resetUINavHeader()
        if(App.prefs.isLoggedIn) {
            AuthService.findUserByEmail(this) {}
        }

    }

    override fun onResume() {


        super.onResume()
    }

    override fun onDestroy() {
        socket.disconnect()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(userDataChangeReceiver)
        super.onDestroy()
    }

    private val userDataChangeReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (App.prefs.isLoggedIn) {
                userNameNavHeader.text = UserDataService.name
                userEmailNavHeader.text = UserDataService.email
                val resourceId = resources.getIdentifier(UserDataService.avatarName, "drawable",
                    packageName)
                userImageNavHeader.setImageResource(resourceId)
                userImageNavHeader.setBackgroundColor(UserDataService.returnAvatarColor(UserDataService.avatarColor))
                loginBtnNavHeader.text = "Logout"

                MessageService.getChannels{
                    complete -> if(complete){

                    if(MessageService.channels.count() > 0){
                        selectedChannel = MessageService.channels[0]
                        channelAdapter.notifyDataSetChanged()
                        updateWithChannel()
                    }
                }
                }
            }
        }
    }

    fun updateWithChannel(){
        mainChannelName.text = "#${selectedChannel?.name}"
        if(selectedChannel != null){
            MessageService.getMessages(selectedChannel!!.id){
                complete ->
                if(complete){
                        messageAdapter.notifyDataSetChanged()
                        if(messageAdapter.itemCount > 0){
                            messageListView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                        }
                }
            }
        }

    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    fun loginBtnNavClicked(view: View){

        if(App.prefs.isLoggedIn){
            UserDataService.logout()
            channelAdapter.notifyDataSetChanged()
            messageAdapter.notifyDataSetChanged()
            resetUINavHeader()
            mainChannelName.text="#PleaseLogIn"
        }else{
            val loginIntent = Intent(this,
                LoginActivity::class.java)
                        startActivity(loginIntent)
        }

    }

    fun addChannelClicked(view: View){
        if(App.prefs.isLoggedIn){
            val builder = AlertDialog.Builder(this)
            val dialogView = layoutInflater.inflate(R.layout.add_channel_dialog, null)
            builder.setView(dialogView).setPositiveButton("Add"){
               _, _->

                val nameTextField = dialogView.findViewById<EditText>(R.id.addChannelNameTxt)
                val descTextField = dialogView.findViewById<EditText>(R.id.addChannelDescTxt)

                val channelName = nameTextField.text.toString()
                val channelDesc = descTextField.text.toString()

                socket.emit("newChannel", channelName,channelDesc)


            }.setNegativeButton("Cancel"){
               _, _->

            }.show()
        }
    }

    private val onNewChannel = Emitter.Listener { args ->
    if(App.prefs.isLoggedIn){
    runOnUiThread{
        val channelName = args[0] as String
        val channelDescription = args[1] as String
        val channelId = args[2] as String

        val newChannel = Channel(channelName,channelDescription,channelId)

        MessageService.channels.add(newChannel)
        channelAdapter.notifyDataSetChanged()
    }
}

    }

    private val onNewMessage = Emitter.Listener { args ->
    if(App.prefs.isLoggedIn){
    runOnUiThread {

        val channelId = args[2] as String

        if(channelId == selectedChannel?.id){
            val msgBody = args[0] as String

            val userName = args[3] as String
            val userAvatar = args[4] as String
            val userAvatarColor = args[5] as String
            val id = args[6] as String
            val timeStamp = args[7] as String

            val newMessage = Message(msgBody,userName,channelId,userAvatar,userAvatarColor,id,timeStamp)

            MessageService.messages.add(newMessage)
            messageAdapter.notifyDataSetChanged()
            messageListView.smoothScrollToPosition(messageAdapter.itemCount - 1)
        }


    }
}

    }

    fun sendMsgBtnClicked(view: View) {
        if(App.prefs.isLoggedIn && messageTextField.text.isNotEmpty() && selectedChannel !== null){
            val userId = UserDataService.id
            val channelId = selectedChannel!!.id
            socket.emit("newMessage",messageTextField.text.toString(),userId,channelId
            ,UserDataService.name, UserDataService.avatarName,UserDataService.avatarColor)

            messageTextField.text.clear()
            hideKeyboard()
        }

    }

    fun resetUINavHeader(){
        userNameNavHeader.text = "Good User"
        userEmailNavHeader.text = "gooduser@gmail.com"
        userImageNavHeader.setImageResource(R.drawable.profiledefault)
        userImageNavHeader.setBackgroundColor(Color.TRANSPARENT)
        loginBtnNavHeader.text = "Login"
    }

    fun hideKeyboard(){
        val inputManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        if(inputManager.isAcceptingText){
            inputManager.hideSoftInputFromWindow(currentFocus?.windowToken,0)
        }
    }

}
