package com.barbie.ai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var chat: LinearLayout
    private lateinit var scroll: ScrollView
    private lateinit var input: EditText
    private lateinit var status: TextView
    private lateinit var tts: TextToSpeech
    private var backendUrl = ""
    private val screenCoachRequest = 31
    private val history = ArrayList<String>()
    private val bg = Color.rgb(10, 7, 16)
    private val panel = Color.rgb(25, 18, 34)
    private val pink = Color.rgb(255, 55, 171)
    private val white = Color.WHITE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        val prefs = getPreferences(0)
        backendUrl = prefs.getString("backend_url", "") ?: ""
        history.addAll(prefs.getStringSet("chat_history", emptySet())?.toList() ?: emptyList())
        buildUi()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(16, 14, 10, 10); background = rounded(panel, 0, 0, 28, 28) }
        val avatar = ImageView(this).apply { setImageBitmap(BarbieBrand.bitmap()); scaleType = ImageView.ScaleType.CENTER_CROP; background = rounded(pink, 30, 30, 30, 30) }
        header.addView(avatar, LinearLayout.LayoutParams(58, 58))
        val head = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(12, 0, 0, 0) }
        head.addView(TextView(this).apply { text = "Barbie AI"; textSize = 21f; setTextColor(white); typeface = android.graphics.Typeface.DEFAULT_BOLD })
        status = TextView(this).apply { text = "Ready • aap jo chahein batayein"; textSize = 12f; setTextColor(pink) }
        head.addView(status); header.addView(head, LinearLayout.LayoutParams(0, -2, 1f))
        header.addView(TextView(this).apply { text = "☰"; textSize = 27f; gravity = Gravity.CENTER; setTextColor(white); setOnClickListener { showMenu(it) } }, LinearLayout.LayoutParams(52, 52))
        root.addView(header, LinearLayout.LayoutParams(-1, 84))
        scroll = ScrollView(this).apply { isFillViewport = true; setPadding(12, 8, 12, 6) }
        chat = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.BOTTOM }
        scroll.addView(chat, ViewGroup.LayoutParams(-1, -1)); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        addAssistantMessage("Assalam-o-alaikum! Main Barbie AI hoon. Type karke baat karein ya mic se bol dein. Koi fixed command yaad rakhne ki zaroorat nahi.")
        val composer = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(9, 8, 9, 12); background = rounded(panel, 24, 24, 0, 0) }
        composer.addView(TextView(this).apply { text = "＋"; textSize = 27f; gravity = Gravity.CENTER; setTextColor(white); setOnClickListener { openFiles() } }, LinearLayout.LayoutParams(42, 54))
        input = EditText(this).apply { hint = "Barbie ko kuch bhi batao..."; setHintTextColor(Color.rgb(125,115,135)); setTextColor(white); textSize = 16f; maxLines = 4; setPadding(15, 7, 10, 7); background = rounded(Color.rgb(38,29,48), 22,22,22,22) }
        composer.addView(input, LinearLayout.LayoutParams(0, 54, 1f))
        composer.addView(TextView(this).apply { text = "🎙"; textSize = 21f; gravity = Gravity.CENTER; setTextColor(white); setOnClickListener { listen() } }, LinearLayout.LayoutParams(50,54))
        composer.addView(TextView(this).apply { text = "➤"; textSize = 24f; gravity = Gravity.CENTER; setTextColor(white); background = rounded(pink,18,18,18,18); setOnClickListener { sendTyped() } }, LinearLayout.LayoutParams(52,52))
        root.addView(composer, LinearLayout.LayoutParams(-1, 76)); setContentView(root)
    }

    private fun rounded(color: Int, tl: Int, tr: Int, br: Int, bl: Int) = GradientDrawable().apply { setColor(color); cornerRadii = floatArrayOf(tl.toFloat(),tl.toFloat(),tr.toFloat(),tr.toFloat(),br.toFloat(),br.toFloat(),bl.toFloat(),bl.toFloat()) }

    private fun showMenu(anchor: View) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(8,8,8,8); background = rounded(Color.rgb(31,22,40),22,22,22,22) }
        val popup = PopupWindow(box, 310, -2, true).apply { elevation = 20f }
        val items = listOf(
            "💬 Chat History" to { showHistory() },
            "✨ New Chat" to { newChat() },
            "👁 Screen Share" to { startScreenCoach() },
            "🔎 Analyze Screen" to { analyzeCurrentScreen() },
            "⚙ Barbie Settings" to { settingsDialog() }
        )
        items.forEach { (label, action) -> box.addView(TextView(this).apply { text = label; textSize = 15f; setTextColor(white); setPadding(18,14,34,14); setOnClickListener { popup.dismiss(); action() } }) }
        popup.showAsDropDown(anchor, -250, 8)
    }

    private fun saveHistory(value: String) { val clean=value.trim(); if(clean.isBlank())return; history.remove(clean); history.add(0,clean); while(history.size>50)history.removeAt(history.lastIndex); getPreferences(0).edit().putStringSet("chat_history",history.toSet()).apply() }
    private fun showHistory(){val values=if(history.isEmpty())arrayOf("Abhi koi purani chat nahi hai")else history.toTypedArray();AlertDialog.Builder(this).setTitle("Chat History").setItems(values){_,which->if(history.isNotEmpty()){input.setText(history[which]);input.setSelection(input.length())}}.setNegativeButton("Close",null).show()}
    private fun newChat(){chat.removeAllViews();addAssistantMessage("Nayi chat shuru ho gayi. Batao, Barbie kya kaam kare?");status.text="New chat • ready"}
    private fun settingsDialog(){AlertDialog.Builder(this).setTitle("Barbie AI").setMessage("Text chat, voice input, screen share aur smart actions. Barbie natural zaban mein request samajhne ke liye backend AI use karegi. API keys APK mein nahi rakhi jati.").setPositiveButton("Backend"){_,_->backendDialog()}.setNegativeButton("Close",null).show()}

    private fun addBubble(message:String,mine:Boolean){val row=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=if(mine)Gravity.END else Gravity.START;setPadding(4,5,4,5)};val bubble=TextView(this).apply{text=message;textSize=16f;setTextColor(white);setPadding(16,12,16,12);background=rounded(if(mine)Color.rgb(180,38,125)else panel,20,20,20,20)};row.addView(bubble,LinearLayout.LayoutParams(-2,-2).apply{width=minOf(330,resources.displayMetrics.widthPixels-70)});chat.addView(row);scroll.post{scroll.fullScroll(View.FOCUS_DOWN)}}
    private fun addAssistantMessage(v:String)=addBubble(v,false)
    private fun addUserMessage(v:String)=addBubble(v,true)
    private fun sendTyped(){val v=input.text.toString().trim();if(v.isBlank())return;input.text.clear();saveHistory(v);sendMessage(v,false)}

    private fun sendMessage(value:String,speakReply:Boolean){addUserMessage(value);status.text="Barbie soch rahi hai...";when(val action=BarbieCommandRouter.route(value)){is YouTubeAction->{startActivity(Intent(Intent.ACTION_VIEW,BarbieCommandRouter.youtubeUri(action.query)));addAssistantMessage("YouTube search khol diya.");status.text="Ready"};is WhatsAppAction->{openWhatsApp(action.number,action.message);addAssistantMessage("WhatsApp khol diya. Send aap confirm karoge.");status.text="Ready"};is CallAction->{dialNumber(action.number);addAssistantMessage("Call action start kar di.");status.text="Ready"};is WebSearchAction->{startActivity(Intent(Intent.ACTION_VIEW,BarbieCommandRouter.webSearchUri(action.query)));addAssistantMessage("Search khol di.");status.text="Ready"};BackAction->{addAssistantMessage(if(BarbieActionService.instance?.pressBack()==true)"Back kar diya." else "Phone control permission ON karo.");status.text="Ready"};HomeAction->{addAssistantMessage(if(BarbieActionService.instance?.pressHome()==true)"Home par aa gayi." else "Phone control permission ON karo.");status.text="Ready"};is ClickTextAction->{addAssistantMessage(if(BarbieActionService.instance?.clickText(action.text)==true)"Screen par action kar diya." else "Woh text nahi mila ya permission OFF hai.");status.text="Ready"};is ChatAction->thread{askBackend(value,speakReply)}}}

    private fun askBackend(message:String,speakReply:Boolean){if(backendUrl.isBlank()){runOnUiThread{addAssistantMessage("Backend abhi connect nahi hai. Live backend ke baad free-form AI chat aur smart actions handle hongi.");status.text="Backend pending"};return};try{val c=URL("$backendUrl/api/chat").openConnection()as HttpURLConnection;c.requestMethod="POST";c.doOutput=true;c.connectTimeout=15000;c.readTimeout=60000;c.setRequestProperty("Content-Type","application/json");val safe=message.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");c.outputStream.use{it.write("{\"message\":\"$safe\"}".toByteArray())};val body=c.inputStream.bufferedReader().use{it.readText()};val m=Regex("\"(?:reply|response|message)\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").find(body);val reply=m?.groupValues?.get(1)?.replace("\\\"","\"")?.replace("\\n","\n")?:body;runOnUiThread{addAssistantMessage(reply);status.text="Online • ready";if(speakReply)speak(reply)};c.disconnect()}catch(_:Exception){runOnUiThread{addAssistantMessage("Backend se connection nahi hua. Dobara try karein.");status.text="Connection issue"}}}

    private fun listen(){val i=Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_PROMPT,"Barbie ko bolo...")};try{status.text="Sun rahi hoon...";startActivityForResult(i,20)}catch(_:Exception){status.text="Voice input available nahi hai"}}
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==screenCoachRequest){if(resultCode==RESULT_OK&&data!=null){startService(Intent(this,BarbieScreenCoachService::class.java).apply{putExtra("result_code",resultCode);putExtra("projection_data",data)});status.text="Screen share ON";addAssistantMessage("Screen share on ho gayi.")}else status.text="Screen permission cancel ho gayi";return};if(requestCode!=20||resultCode!=RESULT_OK)return;val spoken=data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?:return;saveHistory(spoken);sendMessage(spoken,true)}
    private fun backendDialog(){val field=EditText(this).apply{hint="https://your-backend-url";setText(backendUrl);setTextColor(white)};AlertDialog.Builder(this).setTitle("Barbie AI Backend").setMessage("Backend URL save hoga. API keys APK ke andar nahi hongi.").setView(field).setPositiveButton("Save"){_,_->backendUrl=field.text.toString().trim().trimEnd('/');getPreferences(0).edit().putString("backend_url",backendUrl).apply();status.text=if(backendUrl.isBlank())"Backend pending" else "Backend saved"}.setNegativeButton("Cancel",null).show()}
    private fun analyzeCurrentScreen(){if(backendUrl.isBlank()){addAssistantMessage("Pehle backend connect karo.");return};val file=File(cacheDir,"barbie_screen_latest.jpg");if(!file.exists()){addAssistantMessage("Pehle Screen Share ON karo.");return};status.text="Screen analyze ho rahi hai...";thread{try{val image=Base64.encodeToString(file.readBytes(),Base64.NO_WRAP);val prompt="Is screen par kya nazar aa raha hai? Roman Urdu mein short jawab do. Hindi/Devanagari use na karo.";val a=image.replace("\\","\\\\").replace("\"","\\\"");val p=prompt.replace("\\","\\\\").replace("\"","\\\"");val c=URL("$backendUrl/api/screen").openConnection()as HttpURLConnection;c.requestMethod="POST";c.doOutput=true;c.connectTimeout=15000;c.readTimeout=60000;c.setRequestProperty("Content-Type","application/json");c.outputStream.use{it.write("{\"imageBase64\":\"$a\",\"prompt\":\"$p\"}".toByteArray())};val body=c.inputStream.bufferedReader().use{it.readText()};val m=Regex("\"reply\"\\s*:\\s*\"((?:\\\\.|[^\"])*)\"").find(body);val reply=m?.groupValues?.get(1)?.replace("\\\"","\"")?.replace("\\n","\n")?:body;runOnUiThread{addAssistantMessage(reply);status.text="Online • ready";speak(reply)};c.disconnect()}catch(_:Exception){runOnUiThread{addAssistantMessage("Screen analysis abhi available nahi hai.");status.text="Screen analysis issue"}}}}
    private fun startScreenCoach(){val m=getSystemService(MEDIA_PROJECTION_SERVICE)as MediaProjectionManager;startActivityForResult(m.createScreenCaptureIntent(),screenCoachRequest)}
    private fun openFiles(){startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";addCategory(Intent.CATEGORY_OPENABLE)})}
    private fun openWhatsApp(number:String,message:String){val clean=number.filter{it.isDigit()};val uri=if(clean.isNotBlank())Uri.parse("https://wa.me/$clean?text=${URLEncoder.encode(message,"UTF-8")}")else Uri.parse("https://wa.me/");try{startActivity(Intent(Intent.ACTION_VIEW,uri));status.text="WhatsApp khol diya"}catch(_:Exception){status.text="WhatsApp available nahi hai"}}
    private fun dialNumber(number:String){val clean=number.filter{it.isDigit()||it=='+'};if(clean.isBlank()){status.text="Number nahi mila";return};if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.CALL_PHONE),11);status.text="Call permission chahiye";return};try{startActivity(Intent(Intent.ACTION_CALL,Uri.parse("tel:$clean")));status.text="Call start kar di"}catch(_:Exception){status.text="Call start nahi hui"}}
    private fun speak(value:String){if(::tts.isInitialized)tts.speak(value,TextToSpeech.QUEUE_FLUSH,null,"barbie")}
    override fun onInit(code:Int){if(code==TextToSpeech.SUCCESS){val urdu=Locale("ur","PK");tts.language=if(tts.isLanguageAvailable(urdu)>=TextToSpeech.LANG_AVAILABLE)urdu else Locale.US}}
    override fun onDestroy(){if(::tts.isInitialized)tts.shutdown();super.onDestroy()}
}
