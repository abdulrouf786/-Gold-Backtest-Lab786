package com.goldbacktest.lab786

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.max

class MainActivity : Activity() {
    private var candles = mutableListOf<Candle>()
    private lateinit var status: TextView
    private lateinit var result: TextView
    private lateinit var timeframe: Spinner
    private val pickCsv = 77
    private val timeframes = listOf("1 minute", "5 minutes", "15 minutes", "1 hour", "4 hours", "Daily")
    data class Candle(val time:String,val open:Double,val high:Double,val low:Double,val close:Double)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24); setBackgroundColor(0xFF101820.toInt()) }
        fun text(s:String,size:Float=18f)=TextView(this).apply { text=s; textSize=size; setTextColor(0xFFFFFFFF.toInt()); setPadding(0,10,0,10) }
        root.addView(text("🥇 GOLD BACKTEST LAB 786",24f))
        root.addView(text("XAUUSD • 10-Year Multi-Timeframe Backtest",15f))
        root.addView(text("Select timeframe:",16f))
        timeframe=Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,timeframes) }
        root.addView(timeframe)
        val load=Button(this).apply { text="Import Gold CSV"; setOnClickListener { val i=Intent(Intent.ACTION_OPEN_DOCUMENT).apply { type="text/*"; addCategory(Intent.CATEGORY_OPENABLE) }; startActivityForResult(i,pickCsv) } }
        root.addView(load)
        status=text("No data loaded. Import historical CSV candles for the selected timeframe.",14f); root.addView(status)
        val run=Button(this).apply { text="Run Backtest (EMA 9 / EMA 21)"; setOnClickListener { runBacktest() } }; root.addView(run)
        result=text("Backtest results will appear here.",16f); root.addView(result)
        root.addView(text("CSV columns: timestamp, open, high, low, close. Choose matching timeframe CSV. Data is used offline after import.",13f))
        root.addView(text("Timeframes: 1m • 5m • 15m • 1H • 4H • Daily",13f))
        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) { super.onActivityResult(requestCode,resultCode,data); if(requestCode==pickCsv && resultCode==RESULT_OK) { data?.data?.let { uri -> try { readCsv(uri) } catch(e:Exception) { status.text="CSV error: ${e.message}" } } } }
    private fun readCsv(uri:Uri) {
        candles.clear()
        contentResolver.openInputStream(uri)?.use { stream -> BufferedReader(InputStreamReader(stream)).useLines { lines -> lines.drop(1).forEach { line ->
            val p=line.split(','); if(p.size>=5) try { candles.add(Candle(p[0],p[1].trim().toDouble(),p[2].trim().toDouble(),p[3].trim().toDouble(),p[4].trim().toDouble())) } catch(_:Exception){}
        } } }
        candles.sortBy { it.time }
        status.text="Loaded ${candles.size} candles • ${timeframes[timeframe.selectedItemPosition]} CSV • Ready for offline backtest."
    }
    private fun ema(values:List<Double>,period:Int):List<Double> { if(values.isEmpty()) return emptyList(); val a=2.0/(period+1); val out= mutableListOf(values[0]); for(i in 1 until values.size) out.add(values[i]*a+out[i-1]*(1-a)); return out }
    private fun runBacktest() {
        if(candles.size<30) { result.text="Please import at least 30 valid candles for the selected timeframe."; return }
        val closes=candles.map{it.close}; val fast=ema(closes,9); val slow=ema(closes,21)
        var position=0; var entry=0.0; var pnl=0.0; var wins=0; var trades=0; var peak=0.0; var maxDd=0.0
        for(i in 1 until candles.size) {
            if(position==0 && fast[i-1]<=slow[i-1] && fast[i]>slow[i]) { position=1; entry=closes[i] }
            else if(position==1 && fast[i-1]>=slow[i-1] && fast[i]<slow[i]) { val trade=closes[i]-entry; pnl+=trade; trades++; if(trade>0) wins++; position=0; peak=max(peak,pnl); maxDd=max(maxDd,peak-pnl) }
        }
        if(position==1) { val trade=closes.last()-entry; pnl+=trade; trades++; if(trade>0) wins++; peak=max(peak,pnl); maxDd=max(maxDd,peak-pnl) }
        result.text="Timeframe: ${timeframes[timeframe.selectedItemPosition]}\nStrategy: EMA 9/21 crossover\nCandles: ${candles.size}\nTrades: $trades\nWins: $wins\nWin rate: ${if(trades>0) "%.1f".format(100.0*wins/trades) else "0.0"}%\nNet P/L: ${"%.2f".format(pnl)} price units\nMax drawdown: ${"%.2f".format(maxDd)} price units\n\nPrototype only; spread, commission and slippage not included."
    }
}
