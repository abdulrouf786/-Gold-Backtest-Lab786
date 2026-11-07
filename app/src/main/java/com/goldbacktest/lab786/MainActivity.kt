package com.goldbacktest.lab786

import android.app.Activity
import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.widget.*
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.math.max
import kotlin.concurrent.thread

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var result: TextView
    private lateinit var timeframe: Spinner
    private var csvUri: Uri? = null
    private val pickCsv = 77
    private val timeframes = listOf("1 minute", "5 minutes", "15 minutes", "1 hour", "4 hours", "Daily")

    data class Stats(var candles:Long=0,var trades:Int=0,var wins:Int=0,var pnl:Double=0.0,var peak:Double=0.0,var maxDd:Double=0.0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,24,24,24); setBackgroundColor(0xFF101820.toInt()) }
        fun text(s:String,size:Float=18f)=TextView(this).apply { text=s; textSize=size; setTextColor(0xFFFFFFFF.toInt()); setPadding(0,10,0,10) }
        root.addView(text("🥇 GOLD BACKTEST LAB 786",24f))
        root.addView(text("XAUUSD • 10-Year Multi-Timeframe Backtest",15f))
        root.addView(text("Select timeframe:",16f))
        timeframe=Spinner(this).apply { adapter=ArrayAdapter(this@MainActivity,android.R.layout.simple_spinner_dropdown_item,timeframes) }
        root.addView(timeframe)
        val load=Button(this).apply { text="Select Gold CSV"; setOnClickListener {
            try {
                // Use Android's generic file chooser rather than document preview; this is
                // more compatible with large CSV files on some Android file-provider apps.
                val i=Intent(Intent.ACTION_GET_CONTENT).apply {
                    type="*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                startActivityForResult(Intent.createChooser(i,"Choose Gold CSV file"),pickCsv)
            } catch(e:Exception) {
                status.text="Cannot open file picker: ${e.message}"
            }
        } }
        root.addView(load)
        status=text("No CSV selected. Choose the Gold CSV. Large files are read as a stream, not loaded fully into RAM.",14f); root.addView(status)
        val run=Button(this).apply { text="Run Backtest (EMA 9 / EMA 21)"; setOnClickListener { runBacktest() } }; root.addView(run)
        result=text("Backtest results will appear here.",16f); root.addView(result)
        root.addView(text("CSV format: timestamp, open, high, low, close (Dukascopy CSV supported).",13f))
        root.addView(text("Timeframes: 1m • 5m • 15m • 1H • 4H • Daily",13f))
        setContentView(ScrollView(this).apply { addView(root) })
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==pickCsv) {
            if(resultCode==RESULT_OK && data?.data!=null) {
                val uri=data.data!!
                csvUri=uri
                status.text="File selected. Tap Run Backtest. The app will read it gradually."
                result.text="Ready • URI received"
            } else {
                status.text="File selection cancelled. Tap Select Gold CSV and choose the file from Downloads."
            }
        }
    }

    private fun runBacktest() {
        val uri=csvUri
        if(uri==null) { status.text="Please select the CSV file first."; return }
        result.text="Backtest running… Large file may take several minutes. Keep app open."
        thread {
            try {
                val stats=Stats()
                var prevCloseEma9=0.0; var prevCloseEma21=0.0
                var initialized=false
                var prevFast=0.0; var prevSlow=0.0
                var position=false; var entry=0.0
                val alpha9=2.0/10.0; val alpha21=2.0/22.0
                val stream=contentResolver.openInputStream(uri) ?: throw IllegalStateException("Unable to open selected CSV. Try selecting it again from Downloads.")
                stream.use {
                    BufferedReader(InputStreamReader(it, Charsets.UTF_8), 1024*1024).use { reader ->
                        var line=reader.readLine()
                        while(line!=null) {
                            val p=line.split(',')
                            if(p.size>=5) {
                                val close=try { p[4].trim().toDouble() } catch(_:Exception) { null }
                                if(close!=null && close.isFinite()) {
                                    if(!initialized) {
                                        prevCloseEma9=close; prevCloseEma21=close
                                        prevFast=close; prevSlow=close; initialized=true
                                    } else {
                                        val fast=close*alpha9+prevCloseEma9*(1-alpha9)
                                        val slow=close*alpha21+prevCloseEma21*(1-alpha21)
                                        if(!position && prevFast<=prevSlow && fast>slow) { position=true; entry=close }
                                        else if(position && prevFast>=prevSlow && fast<slow) {
                                            val trade=close-entry; stats.pnl+=trade; stats.trades++; if(trade>0) stats.wins++
                                            position=false; stats.peak=max(stats.peak,stats.pnl); stats.maxDd=max(stats.maxDd,stats.peak-stats.pnl)
                                        }
                                        prevCloseEma9=fast; prevCloseEma21=slow; prevFast=fast; prevSlow=slow
                                    }
                                    stats.candles++
                                }
                            }
                            line=reader.readLine()
                        }
                    }
                }
                runOnUiThread {
                    status.text="Processed ${stats.candles} candles • ${timeframes[timeframe.selectedItemPosition]} • Offline"
                    result.text="Strategy: EMA 9/21 crossover\nCandles: ${stats.candles}\nTrades: ${stats.trades}\nWins: ${stats.wins}\nWin rate: ${if(stats.trades>0) "%.1f".format(100.0*stats.wins/stats.trades) else "0.0"}%\nNet P/L: ${"%.2f".format(stats.pnl)} price units\nMax drawdown: ${"%.2f".format(stats.maxDd)} price units\n\nStreaming mode: low RAM. Spread, commission and slippage not included."
                }
            } catch(e:Exception) {
                runOnUiThread { status.text="CSV read error: ${e.message ?: e.javaClass.simpleName}" }
            }
        }
    }
}
