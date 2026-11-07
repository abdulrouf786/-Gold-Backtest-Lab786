package com.goldbacktest.lab786

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.View
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.max

class MainActivity : Activity() {
    private var candles = mutableListOf<Candle>()
    private lateinit var status: TextView
    private lateinit var result: TextView
    private val pickCsv = 77
    data class Candle(val time:String,val open:Double,val high:Double,val low:Double,val close:Double)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24); setBackgroundColor(0xFF101820.toInt()) }
        fun text(s:String,size:Float=18f)=TextView(this).apply { text=s; textSize=size; setTextColor(0xFFFFFFFF.toInt()); setPadding(0,10,0,10) }
        root.addView(text("🥇 GOLD BACKTEST LAB 786",24f))
        root.addView(text("XAUUSD • Offline Historical Backtest",15f))
        val load=Button(this).apply { text="Import Gold CSV"; setOnClickListener { val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="text/*"; addCategory(Intent.CATEGORY_OPENABLE } ; startActivityForResult(i,pickCsv) } }
        root.addView(load)
        status=text("No data loaded. Import a CSV file to begin.",14f); root.addView(status)
        val run=Button(this).apply { text="Run Backtest (EMA 9 / EMA 21)"; setOnClickListener { runBacktest() } }; root.addView(run)
        result=text("Backtest results will appear here.",16f); root.addView(result)
        val note=text("CSV columns: timestamp, open, high, low, close. Data is saved for this session and the backtest works offline.",13f); root.addView(note)
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) { super.onActivityResult(requestCode,resultCode,data); if(requestCode==pickCsv && resultCode==RESULT_OK) { data?.data?.let { uri -> try { readCsv(uri) } catch(e:Exception) { status.text="CSV error: ${e.message}" } } } }
    private fun readCsv(uri:Uri) {
        candles.clear(); contentResolver.openInputStream(uri)?.use { stream -> BufferedReader(InputStreamReader(stream)).useLines { lines -> lines.drop(1).forEach { line -> val p=line.split(','); if(p.size>=5) try { candles.add(Candle(p[0],p[1].toDouble(),p[2].toDouble(),p[3].toDouble(),p[4].toDouble())) } catch(_:Exception){} } } }
        candles.sortBy { it.time }; status.text="Loaded ${candles.size} candles from CSV. Ready for offline backtest."
    }
    private fun ema(values:List<Double>,period:Int):List<Double> { if(values.isEmpty()) return emptyList(); val a=2.0/(period+1); val out= mutableListOf(values[0]); for(i in 1 until values.size) out.add(values[i]*a+out[i-1]*(1-a)); return out }
    private fun runBacktest() {
        if(candles.size<30) { result.text="Please import at least 30 valid candles."; return }
        val closes=candles.map{it.close}; val fast=ema(closes,9); val slow=ema(closes,21)
        var position=0; var entry=0.0; var pnl=0.0; var wins=0; var trades=0; var peak=0.0; var maxDd=0.0
        for(i in 1 until candles.size) {
            if(position==0 && fast[i-1]<=slow[i-1] && fast[i]>slow[i]) { position=1; entry=closes[i] }
            else if(position==1 && fast[i-1]>=slow[i-1] && fast[i]<slow[i]) { val trade=closes[i]-entry; pnl+=trade; trades++; if(trade>0) wins++; position=0; peak=max(peak,pnl); maxDd=max(maxDd,peak-pnl) }
        }
        if(position==1) { val trade=closes.last()-entry; pnl+=trade; trades++; if(trade>0) wins++; peak=max(peak,pnl); maxDd=max(maxDd,peak-pnl) }
        result.text="Strategy: EMA 9/21 crossover\nCandles: ${candles.size}\nTrades: $trades\nWins: $wins\nWin rate: ${if(trades>0) "%.1f".format(100.0*wins/trades) else "0.0"}%\nNet P/L: ${"%.2f".format(pnl)} price units\nMax drawdown: ${"%.2f".format(maxDd)} price units\n\nPrototype only; spread, commission and slippage not included."
    }
}
