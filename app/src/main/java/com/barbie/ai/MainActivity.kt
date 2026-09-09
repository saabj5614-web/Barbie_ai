package com.barbie.ai

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.view.Gravity
import android.view.View
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
    private val history = ArrayList<String>()
    private val bg = 0xFF0A0710.toInt(); private val panel = 0xFF191222.toInt(); private val pink = 0xFFFF37AB.toInt(); private val white = 0xFFFFFFFF.toInt()
    private val screenRequest = 31

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); tts=TextToSpeech(this,this); val p=getPreferences(0); backendUrl=p.getString("backend_url","")?:""; history.addAll(p.getStringSet("chat_history",emptySet())?:emptySet()); buildUi() }
    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()
    private fun box(c:Int,r:Float)=android.graphics.drawable.GradientDrawable().apply{setColor(c);cornerRadius=dp(r.toInt()).toFloat()}
    private fun text(s:String,size:Float,c:Int)=TextView(this).apply{text=s;textSize=size;setTextColor(c);setPadding(dp(12),dp(8),dp(12),dp(8))}

    private fun buildUi(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(bg)}
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(12),dp(10),dp(8),dp(10));background=box(panel,24f)}
        val avatar=ImageView(this).apply{setImageBitmap(BarbieBrand.bitmap());scaleType=ImageView.ScaleType.CENTER_CROP;background=box(pink,30f)}
        header.addView(avatar,LinearLayout.LayoutParams(dp(58),dp(58)))
        val title=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(10),0,0,0)}
        title.addView(text("Barbie AI",21f,white).apply{typeface=android.graphics.Typeface.DEFAULT_BOLD})
        status=text(if(backendUrl.isBlank())"Ready • backend pending" else "Online • ready",12f,pink);title.addView(status);header.addView(title,LinearLayout.LayoutParams(0,-2,1f))
        header.addView(text("☰",27f,white).apply{gravity=Gravity.CENTER;setOnClickListener{showMenu(it)}},LinearLayout.LayoutParams(dp(50),dp(52)))
        root.addView(header,LinearLayout.LayoutParams(-1,dp(80)))
        scroll=ScrollView(this).apply{isFillViewport=true;setPadding(dp(10),dp(8),dp(10),dp(5))};chat=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};scroll.addView(chat);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        addAssistant("Assalam-o-alaikum! Main Barbie AI hoon. Aap type karein ya mic dabayein. Koi fixed command yaad rakhne ki zaroorat nahi.")
        val composer=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(dp(8),dp(7),dp(8),dp(10));background=box(panel,22f)}
        composer.addView(text("＋",27f,white).apply{gravity=Gravity.CENTER;setOnClickListener{openFiles()}},LinearLayout.LayoutParams(dp(42),dp(54)))
        input=EditText(this).apply{hint="Barbie ko kuch bhi batao...";setHintTextColor(0xFF82758A.toInt());setTextColor(white);textSize=16f;maxLines=4;setPadding(dp(14),dp(5),dp(8),dp(5));background=box(0xFF261D30.toInt(),22f)}
        composer.addView(input,LinearLayout.LayoutParams(0,dp(54),1f))
        composer.addView(text("🎙",21f,white).apply{gravity=Gravity.CENTER;setOnClickListener{listen()}},LinearLayout.LayoutParams(dp(50),dp(54)))
        composer.addView(text("➤",24f,white).apply{gravity=Gravity.CENTER;background=box(pink,18f);setOnClickListener{sendTyped()}},LinearLayout.LayoutParams(dp(50),dp(50)))
        root.addView(composer,LinearLayout.LayoutParams(-1,dp(74)));setContentView(root)
    }

    private fun showMenu(anchor:View){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(7),dp(7),dp(7),dp(7));background=box(0xFF1F1628.toInt(),20f)}
        val pop=PopupWindow(box,dp(285),-2,true).apply{elevation=dp(18).toFloat()}
        listOf("💬 Chat History" to {showHistory()},"✨ New Chat" to {newChat()},"👁 Screen Share" to {startScreen()},"⚙ Barbie Settings" to {settings()}).forEach{(label,action)->box.addView(text(label,15f,white).apply{setPadding(dp(16),dp(14),dp(20),dp(14));setOnClickListener{pop.dismiss();action()}})}
        pop.showAsDropDown(anchor,-dp(235),dp(6))
    }
    private fun saveHistory(v:String){val x=v.trim();if(x.isBlank())return;history.remove(x);history.add(0,x);while(history.size>50)history.removeAt(history.lastIndex);getPreferences(0).edit().putStringSet("chat_history",history.toSet()).apply()}
    private fun showHistory(){val a=if(history.isEmpty())arrayOf("Abhi koi history nahi hai")else history.toTypedArray();AlertDialog.Builder(this).setTitle("Chat History").setItems(a){_,i->if(history.isNotEmpty())input.setText(history[i])}.setNegativeButton("Close",null).show()}
    private fun newChat(){chat.removeAllViews();addAssistant("Nayi chat shuru ho gayi. Batao, Barbie kya kaam kare?");status.text="New chat • ready"}
    private fun settings(){AlertDialog.Builder(this).setTitle("Barbie AI").setMessage("Natural-language chat, voice, screen share aur smart phone actions. API keys APK mein nahi hoti.").setPositiveButton("Backend"){_,_->backendDialog()}.setNegativeButton("Close",null).show()}

    private fun addBubble(v:String,mine:Boolean){val row=LinearLayout(this).apply{gravity=if(mine)Gravity.END else Gravity.START;setPadding(dp(3),dp(4),dp(3),dp(4))};val b=text(v,16f,white);b.background=box(if(mine)0xFFB4267D.toInt()else panel,19f);row.addView(b,LinearLayout.LayoutParams(dp(330),-2));chat.addView(row);scroll.post{scroll.fullScroll(View.FOCUS_DOWN)}}
    private fun addAssistant(v:String)=addBubble(v,false);private fun addUser(v:String)=addBubble(v,true)
    private fun sendTyped(){val v=input.text.toString().trim();if(v.isBlank())return;input.text.clear();saveHistory(v);sendToBackend(v,false)}
    private fun listen(){try{status.text="Sun rahi hoon...";startActivityForResult(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply{putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);putExtra(RecognizerIntent.EXTRA_PROMPT,"Barbie ko bolo...")},20)}catch(_:Exception){status.text="Voice input available nahi hai"}}

    private fun sendToBackend(message:String,speakReply:Boolean){
        addUser(message);if(backendUrl.isBlank()){addAssistant("Backend abhi connect nahi hai. Pehle backend deploy hoga, phir Barbie natural zaban mein chat aur actions handle karegi.");status.text="Backend pending";return};status.text="Barbie soch rahi hai..."
        thread{try{val c=URL("$backendUrl/api/chat").openConnection() as HttpURLConnection;c.requestMethod="POST";c.doOutput=true;c.connectTimeout=15000;c.readTimeout=60000;c.setRequestProperty("Content-Type","application/json");val safe=message.replace("\\","\\\\").replace("\"","\\\"").replace("\n","\\n");c.outputStream.use{it.write("{\"message\":\"$safe\"}".toByteArray())};val body=c.inputStream.bufferedReader().use{it.readText()};val reply=stringField(body,"reply")?:"Barbie ko provider se jawab nahi mila.";val action=stringField(body,"type")?:"none";runOnUiThread{addAssistant(reply);status.text="Online • ready";executeAction(action,body);if(speakReply)speak(reply)};c.disconnect()}catch(_:Exception){runOnUiThread{addAssistant("Backend se connection nahi hua. Dobara try karein.");status.text="Connection issue"}}}
    }
    private fun stringField(json:String,key:String):String?{val r=Regex("\\\""+Regex.escape(key)+"\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"])*)\\\"").find(json)?:return null;return r.groupValues[1].replace("\\\"","\"").replace("\\n","\n")}
    private fun executeAction(type:String,json:String){when(type){"youtube_search"->openUrl("https://www.youtube.com/results?search_query="+Uri.encode(stringField(json,"query")?:""));"web_search"->openUrl("https://www.google.com/search?q="+Uri.encode(stringField(json,"query")?:""));"open_whatsapp"->openWhatsApp(stringField(json,"number")?:"",stringField(json,"message")?:"");"call_number"->dialNumber(stringField(json,"number")?:"");"open_files"->openFiles();"screen_share"->startScreen();"back"->BarbieActionService.instance?.pressBack();"home"->BarbieActionService.instance?.pressHome();"click_text"->BarbieActionService.instance?.clickText(stringField(json,"text")?:"");"github_request"->addAssistant("GitHub ka kaam request samajh li hai. Backend tools connect hone ke baad isay execute kiya jayega.")}}
    private fun openUrl(u:String){try{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(u)))}catch(_:Exception){}}
    private fun openWhatsApp(number:String,message:String){val n=number.filter{it.isDigit()};val u=if(n.isBlank())"https://wa.me/" else "https://wa.me/$n?text="+URLEncoder.encode(message,"UTF-8");openUrl(u)}
    private fun dialNumber(number:String){val n=number.filter{it.isDigit()||it=='+'};if(n.isBlank()){addAssistant("Call ke liye number chahiye.");return};if(checkSelfPermission(Manifest.permission.CALL_PHONE)!=PackageManager.PERMISSION_GRANTED){requestPermissions(arrayOf(Manifest.permission.CALL_PHONE),11);return};try{startActivity(Intent(Intent.ACTION_CALL,Uri.parse("tel:$n")))}catch(_:Exception){addAssistant("Call start nahi hui.")}}
    private fun openFiles(){startActivity(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="*/*";addCategory(Intent.CATEGORY_OPENABLE)})}
    private fun backendDialog(){val f=EditText(this).apply{hint="https://your-backend-url";setText(backendUrl);setTextColor(white)};AlertDialog.Builder(this).setTitle("Backend URL").setView(f).setPositiveButton("Save"){_,_->backendUrl=f.text.toString().trim().trimEnd('/');getPreferences(0).edit().putString("backend_url",backendUrl).apply();status.text="Backend saved"}.setNegativeButton("Cancel",null).show()}
    private fun startScreen(){val m=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager;startActivityForResult(m.createScreenCaptureIntent(),screenRequest)}
    private fun speak(v:String){tts.speak(v,TextToSpeech.QUEUE_FLUSH,null,"barbie")}
    override fun onInit(code:Int){if(code==TextToSpeech.SUCCESS)tts.language=Locale("ur","PK")}
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){super.onActivityResult(requestCode,resultCode,data);if(requestCode==screenRequest){if(resultCode==RESULT_OK&&data!=null){startService(Intent(this,BarbieScreenCoachService::class.java).apply{putExtra("result_code",resultCode);putExtra("projection_data",data)});status.text="Screen Share ON";addAssistant("Screen Share on ho gayi.")}return};if(requestCode==20&&resultCode==RESULT_OK){val s=data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?:return;saveHistory(s);sendToBackend(s,true)}}
    override fun onDestroy(){tts.shutdown();super.onDestroy()}
}
