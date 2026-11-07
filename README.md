# Gold Backtest Lab 786

Android app project for Gold (XAUUSD) historical backtesting. This is a separate repository from ICT Baba Scanner.

## Build APK

Open the repository's **Actions** tab and run **Build Android APK**. When the workflow finishes, download the `Gold-Backtest-Lab-debug-apk` artifact.

## Current prototype

- Android app shell for XAUUSD backtesting
- CSV import from device storage
- Offline candle-by-candle backtest
- Strategy: EMA crossover (prototype; configurable in a future update)
- Trade statistics: win rate, net P/L, drawdown, trades

## CSV format

Use columns: `timestamp,open,high,low,close` (volume optional). Timestamps should be chronological. CSV candles are imported and stored locally for offline backtesting.

**Important:** This is a backtesting prototype, not financial advice. Results depend on the supplied historical data and do not guarantee future performance.
