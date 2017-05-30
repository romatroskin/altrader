package io.github.romatroskin.altrader.bot;

import eu.verdelhan.ta4j.AnalysisCriterion;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.analysis.criteria.*;
import io.github.romatroskin.altrader.strategies.*;
import io.github.romatroskin.altrader.utils.PoloniexUtils;
import org.joda.time.Period;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.poloniex.PoloniexExchange;
import org.knowm.xchange.poloniex.service.PoloniexMarketDataService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by romatroskin on 5/25/17.
 */
public enum BotAnalyzeFactory implements BotTaskRunner {
    workflow {
        final AnalysisCriterion profitCriterion = new TotalProfitCriterion();
        @Override
        public void start(CurrencyPair pair, ExchangeSpecification spec) throws IOException {
            final TimeSeries series = PoloniexUtils.loadTimeSeries(pair, spec);
            final List<TimeSeries> subseries = series.split(Period.hours(6), Period.weeks(1));
            final Map<Strategy, String> strategies = buildStrategiesMap(series);

            for (TimeSeries slice : subseries) {
                // For each sub-series...
                System.out.println("Sub-series: " + slice.getSeriesPeriodDescription());
                for (Map.Entry<Strategy, String> entry : strategies.entrySet()) {
                    final Strategy strategy = entry.getKey();
                    final String name = entry.getValue();
                    // For each strategy...
                    final TradingRecord tradingRecord = slice.run(strategy);
                    final double profit = profitCriterion.calculate(slice, tradingRecord);
                    System.out.println("\tProfit for " + name + ": " + profit);
                }
                final Strategy bestStrategy = profitCriterion.chooseBest(slice, new ArrayList<>(strategies.keySet()));
                System.out.println("\t\t--> Best strategy: " + strategies.get(bestStrategy) + "\n");
            }
        }

        private Map<Strategy, String> buildStrategiesMap(TimeSeries series) {
            final HashMap<Strategy, String> strategies = new HashMap<Strategy, String>();
            strategies.put(CCICorrectionStrategy.buildStrategy(series), "CCI Correction");
            strategies.put(GlobalExtremaStrategy.buildStrategy(series), "Global Extrema");
            strategies.put(MovingMomentumStrategy.buildStrategy(series), "Moving Momentum");
            strategies.put(RSI2Strategy.buildStrategy(series), "RSI-2");
            strategies.put(IchimokuCloudTradingStrategy.buildStrategy(series), "Ichimoku Cloud");
            return strategies;
        }
    },
    strategy {
        final TotalProfitCriterion totalProfit = new TotalProfitCriterion();
        @Override
        public void start(CurrencyPair pair, ExchangeSpecification spec) throws IOException {
            final TimeSeries series = PoloniexUtils.loadTimeSeries(pair, spec);
            final Strategy strategy = buildStrategy(series);
            final TradingRecord tradingRecord = series.run(strategy);

            System.out.println("Total profit: " + totalProfit.calculate(series, tradingRecord));
            // Number of ticks
            System.out.println("Number of ticks: " + new NumberOfTicksCriterion().calculate(series, tradingRecord));
            // Average profit (per tick)
            System.out.println("Average profit (per tick): " + new AverageProfitCriterion().calculate(series, tradingRecord));
            // Number of trades
            System.out.println("Number of trades: " + new NumberOfTradesCriterion().calculate(series, tradingRecord));
            // Profitable trades ratio
            System.out.println("Profitable trades ratio: " + new AverageProfitableTradesCriterion().calculate(series, tradingRecord));
            // Maximum drawdown
            System.out.println("Maximum drawdown: " + new MaximumDrawdownCriterion().calculate(series, tradingRecord));
            // Reward-risk ratio
            System.out.println("Reward-risk ratio: " + new RewardRiskRatioCriterion().calculate(series, tradingRecord));
            // Total transaction cost
            System.out.println("Total transaction cost (from $1000): " + new LinearTransactionCostCriterion(1000, 0.005).calculate(series, tradingRecord));
            // Buy-and-hold
            System.out.println("Buy-and-hold: " + new BuyAndHoldCriterion().calculate(series, tradingRecord));
            // Total profit vs buy-and-hold
            System.out.println("Custom strategy profit vs buy-and-hold strategy profit: " + new VersusBuyAndHoldCriterion(totalProfit).calculate(series, tradingRecord));
        }

        private Strategy buildStrategy(TimeSeries series) {
            switch (strategie) {
                case cci_correction: return CCICorrectionStrategy.buildStrategy(series);
                case global_extrema: return GlobalExtremaStrategy.buildStrategy(series);
                case moving_momentum: return MovingMomentumStrategy.buildStrategy(series);
                case rsi2: return RSI2Strategy.buildStrategy(series);
                case ichimoku: return IchimokuCloudTradingStrategy.buildStrategy(series);
                default: return new Strategy(null, null);
            }
        }
    };

    @Override
    public void stop() {

    }

    BotStrategiesFactory strategie;
    public void setStrategie(BotStrategiesFactory strategie) {
        this.strategie = strategie;
    }
}
