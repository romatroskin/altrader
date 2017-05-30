package io.github.romatroskin.altrader.bot;

import eu.verdelhan.ta4j.*;
import eu.verdelhan.ta4j.analysis.criteria.AverageProfitableTradesCriterion;
import eu.verdelhan.ta4j.analysis.criteria.RewardRiskRatioCriterion;
import eu.verdelhan.ta4j.analysis.criteria.TotalProfitCriterion;
import eu.verdelhan.ta4j.analysis.criteria.VersusBuyAndHoldCriterion;
import io.github.romatroskin.altrader.strategies.*;
import org.joda.time.DateTime;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.account.Balance;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.knowm.xchange.poloniex.PoloniexExchange;
import org.knowm.xchange.poloniex.dto.marketdata.PoloniexChartData;
import org.knowm.xchange.poloniex.dto.marketdata.PoloniexMarketData;
import org.knowm.xchange.poloniex.dto.marketdata.PoloniexTicker;
import org.knowm.xchange.poloniex.service.PoloniexMarketDataService;

import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.knowm.xchange.dto.Order.OrderType.ASK;
import static org.knowm.xchange.dto.Order.OrderType.BID;

/**
 * Created by romatroskin on 5/25/17.
 */
public enum BotStrategiesFactory implements BotTaskRunner {
    cci_correction(1) {
        @Override
        public void start(CurrencyPair pair, ExchangeSpecification spec) throws Exception {
            this.runStrategy(pair, spec, CCICorrectionStrategy.class);
        }
    },
    global_extrema(1) {
        @Override
        public void start(CurrencyPair pair, ExchangeSpecification spec) throws Exception {
            this.runStrategy(pair, spec, GlobalExtremaStrategy.class);
        }
    },
    moving_momentum(1) {
        @Override
        public void start(CurrencyPair pair, ExchangeSpecification spec) throws Exception {
            this.runStrategy(pair, spec, MovingMomentumStrategy.class);
        }
    },
    rsi2(1) {
        @Override
        public void start(CurrencyPair pair, ExchangeSpecification spec) throws Exception {
            this.runStrategy(pair, spec, RSI2Strategy.class);
        }
    },
    ichimoku(1) {
        @Override
        public void start(CurrencyPair pair, ExchangeSpecification spec) throws Exception {
            this.runStrategy(pair, spec, IchimokuCloudTradingStrategy.class);
        }
    };

    <T> void runStrategy(CurrencyPair pair, ExchangeSpecification spec, Class<T> strategyClazz) throws Exception {
        final long now = new Date().getTime() / 1000;

        System.out.println(String.format("- Strategy: %s", toString()));

        final PoloniexExchange poloniexExchange = (PoloniexExchange) ExchangeFactory.INSTANCE.createExchange(spec);
        final PoloniexMarketDataService marketDataService = (PoloniexMarketDataService)
                poloniexExchange.getMarketDataService();

        final PoloniexChartData[] chartData = marketDataService.
                getPoloniexChartData(pair, now - lookBackDaysInSeconds, now, period);
        final List<Tick> tickList = Arrays.stream(chartData).map(chart -> new Tick(new DateTime(chart.getDate()),
                chart.getOpen().doubleValue(), chart.getHigh().doubleValue(), chart.getLow().doubleValue(),
                chart.getClose().doubleValue(), chart.getVolume().doubleValue())).collect(Collectors.toList());
        final TimeSeries series = new TimeSeries(tickList);
        series.setMaximumTickCount(MAX_TICK_COUNT);

        final Method buildStrategyMethod = strategyClazz.getMethod("buildStrategy", TimeSeries.class);
        Strategy strategy = (Strategy) buildStrategyMethod.invoke(null, series);
        final TradingRecord tradingRecord = new TradingRecord();
        final Runnable runnable = () -> {
            try {
                final PoloniexTicker ticker = marketDataService.getPoloniexTicker(pair);
                final PoloniexMarketData marketData = ticker.getPoloniexMarketData();
                final Tick newTick = new Tick(DateTime.now(), marketData.getLowestAsk().doubleValue(),
                        marketData.getHigh24hr().doubleValue(), marketData.getLow24hr().doubleValue(),
                        marketData.getHighestBid().doubleValue(), marketData.getBaseVolume().doubleValue());
                series.addTick(newTick);
                System.out.println(String.format("[== New Tick || time: %1$td/%1$tm/%1$tY %1$tH:%1$tM:%1$tS, close price: %2$.8f ==]",
                        newTick.getEndTime().toGregorianCalendar(), newTick.getClosePrice().toDouble()));

                final Balance counterBalance = poloniexExchange.getAccountService()
                        .getAccountInfo().getWallet().getBalance(pair.counter);
                final Balance baseBalance = poloniexExchange.getAccountService()
                        .getAccountInfo().getWallet().getBalance(pair.base);
                final int endIndex = series.getEnd();
                if(strategy.shouldEnter(endIndex)) {
                    Decimal buyAmount = Decimal.valueOf(counterBalance.getAvailable()
                            .multiply(BigDecimal.valueOf(0.33f)).doubleValue()).dividedBy(newTick.getClosePrice());
                    boolean entered = tradingRecord.enter(endIndex, newTick.getClosePrice(), buyAmount);
                    if (entered) {
                        Order entry = tradingRecord.getLastEntry();
                        System.out.println(String.format("[== Entered on %1$d (price=%2$.8f, amount=%3$.8f) ==]", entry.getIndex(),
                                entry.getPrice().toDouble(), entry.getAmount().toDouble()));

                        LimitOrder order = new LimitOrder.Builder(BID, pair).tradableAmount(
                                BigDecimal.valueOf(buyAmount.toDouble())).limitPrice(
                                        BigDecimal.valueOf(entry.getPrice().toDouble())).build();
                        String orderInfo = poloniexExchange.getTradeService().placeLimitOrder(order);
                        System.out.println(String.format("[== Placed order BUY #%1$s, price=%2$.8f ==]", orderInfo,
                                order.getLimitPrice().doubleValue()));
                    }
                } else if(strategy.shouldExit(endIndex)) {
                    Decimal sellAmount = Decimal.valueOf(baseBalance.getAvailable().doubleValue())
                            .multipliedBy(Decimal.valueOf(0.33f));
                    boolean exited = tradingRecord.exit(endIndex, newTick.getClosePrice(), sellAmount);
                    if (exited) {
                        Order exit = tradingRecord.getLastExit();
                        System.out.println(String.format("[== Exited on %1$d (price=%2$.8f, amount=%3$.8f) ==]", exit.getIndex(),
                                exit.getPrice().toDouble(), exit.getAmount().toDouble()));

                        LimitOrder order = new LimitOrder.Builder(ASK, pair).tradableAmount(
                                BigDecimal.valueOf(sellAmount.toDouble())).limitPrice(
                                        BigDecimal.valueOf(exit.getPrice().toDouble())).build();
                        String orderInfo = poloniexExchange.getTradeService().placeLimitOrder(order);
                        System.out.println(String.format("[== Placed order SELL #%1$s, price=%2$.8f ==]", orderInfo,
                                order.getLimitPrice().doubleValue()));
                    }
                }

                if(tradingRecord.getTradeCount() > 0) {
                    System.out.println(String.format("+ Trades: %1$d", tradingRecord.getTradeCount()));
                    AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
                    System.out.println(String.format("+ Profitable trades ratio: %1$.8f",
                            profitTradesRatio.calculate(series, tradingRecord)));
                    AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
                    System.out.println(String.format("+ Reward-risk ratio: %1$.8f",
                            rewardRiskRatio.calculate(series, tradingRecord)));
                    AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
                    System.out.println(String.format("+ Our profit vs buy-and-hold profit: %1$.8f",
                            vsBuyAndHold.calculate(series, tradingRecord)));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        this.scheduler.scheduleWithFixedDelay(runnable, 0, DELAY, TimeUnit.SECONDS);
    }
    ScheduledExecutorService scheduler;
    BotStrategiesFactory(int threadsCount) {
        this.scheduler = Executors.newScheduledThreadPool(threadsCount);
    }

    @Override
    public void stop() {
        this.scheduler.shutdown();
    }
}
