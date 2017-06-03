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

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.fusesource.jansi.Ansi.Color.BLUE;
import static org.fusesource.jansi.Ansi.ansi;
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

        System.out.println(ansi().fg(BLUE).a(String.format("======== Strategy: %s", toString())).fgDefault());

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
                System.out.println(ansi().fgBlue().a(String.format("[== New Tick || %3$s || time: %1$td/%1$tm/%1$tY %1$tH:%1$tM:%1$tS, close price: %2$.8f ==]",
                        newTick.getEndTime().toGregorianCalendar(), newTick.getClosePrice().toDouble(), ticker.getCurrencyPair().base.getDisplayName()))
                        .fgDefault());

                final Balance counterBalance = poloniexExchange.getAccountService()
                        .getAccountInfo().getWallet().getBalance(pair.counter);
                final Balance baseBalance = poloniexExchange.getAccountService()
                        .getAccountInfo().getWallet().getBalance(pair.base);
                final int endIndex = series.getEnd();

                System.out.println(String.format("+ Available Balance %1$s: %2$.8f", pair.counter.getDisplayName(), counterBalance.getAvailable().doubleValue()));
                System.out.println(String.format("+ Available Balance %1$s: %2$.8f", pair.base.getDisplayName(), baseBalance.getAvailable().doubleValue()));
                if(strategy.shouldEnter(endIndex)) {
                    Decimal buyAmount = Decimal.valueOf(counterBalance.getAvailable().subtract(BigDecimal.valueOf(0.0001f))
                            .multiply(BigDecimal.valueOf(0.33f)).doubleValue()).dividedBy(newTick.getClosePrice());
                    boolean entered = tradingRecord.enter(endIndex, newTick.getClosePrice(), buyAmount);
                    if (entered) {
                        Order entry = tradingRecord.getLastEntry();
                        System.out.println(String.format("[== Entered with %4$s on %1$d (price=%2$.8f, amount=%3$.8f) ==]", entry.getIndex(),
                                entry.getPrice().toDouble(), entry.getAmount().toDouble(), pair.base.getDisplayName()));

                        LimitOrder order = new LimitOrder.Builder(BID, pair).tradableAmount(
                                BigDecimal.valueOf(buyAmount.toDouble())).limitPrice(BigDecimal.valueOf(entry.getPrice().toDouble())).build();
                        String orderInfo = poloniexExchange.getTradeService().placeLimitOrder(order);
                        System.out.println(ansi().fgGreen().a(String.format("[== Placed order BUY #%1$s, %3$s, price=%2$.8f ==]", orderInfo,
                                order.getLimitPrice().doubleValue(), pair.base.getDisplayName())).fgDefault());
                    }
                } else if(strategy.shouldExit(endIndex)) {
                    if(tradingRecord.getLastEntry() == null) {
                        System.out.println(String.format("[== Should exit with %1$s, but not entered yet ==]", pair.base.getDisplayName()));
                    } else {
                        Decimal sellTotal = newTick.getClosePrice().multipliedBy(tradingRecord.getLastEntry().getAmount());
                        Decimal buyTotal = tradingRecord.getLastEntry().getPrice().multipliedBy(tradingRecord.getLastEntry().getAmount());
                        System.out.println(String.format("[== Should exit with %1$s, was bought %2$.8f without fee, want to sell %3$.8f ==]",
                                pair.base.getDisplayName(), buyTotal.toDouble(), sellTotal.toDouble()));
                        if(sellTotal.isGreaterThan(buyTotal.plus(buyTotal.multipliedBy(Decimal.valueOf(0.01f))))) {
                            Decimal sellAmount = tradingRecord.getLastEntry().getAmount();
                            boolean exited = tradingRecord.exit(endIndex, newTick.getClosePrice(), sellAmount);
                            if (exited) {
                                Order exit = tradingRecord.getLastExit();
                                System.out.println(String.format("[== Exited with %4$s on %1$d (price=%2$.8f, amount=%3$.8f) ==]", exit.getIndex(),
                                        exit.getPrice().toDouble(), exit.getAmount().toDouble(), pair.base.getDisplayName()));

                                LimitOrder order = new LimitOrder.Builder(ASK, pair).tradableAmount(
                                        BigDecimal.valueOf(sellAmount.toDouble())).limitPrice(BigDecimal.valueOf(exit.getPrice().toDouble())).build();
                                String orderInfo = poloniexExchange.getTradeService().placeLimitOrder(order);
                                System.out.println(ansi().fgRed().a(String.format("[== Placed order SELL #%1$s, %3$s, price=%2$.8f ==]", orderInfo,
                                        order.getLimitPrice().doubleValue(), pair.base.getDisplayName())).fgDefault());
                            }
                        }
                    }
                }

                if(tradingRecord.getTradeCount() > 0) {
                    System.out.println(String.format("+ %2$s Trades: %1$d", tradingRecord.getTradeCount(), pair.base.getDisplayName()));
                    AnalysisCriterion profitTradesRatio = new AverageProfitableTradesCriterion();
                    System.out.println(String.format("+ %2$s Profitable trades ratio: %1$.8f",
                            profitTradesRatio.calculate(series, tradingRecord), pair.base.getDisplayName()));
                    AnalysisCriterion rewardRiskRatio = new RewardRiskRatioCriterion();
                    System.out.println(String.format("+ %2$s Reward-risk ratio: %1$.8f",
                            rewardRiskRatio.calculate(series, tradingRecord), pair.base.getDisplayName()));
                    AnalysisCriterion vsBuyAndHold = new VersusBuyAndHoldCriterion(new TotalProfitCriterion());
                    System.out.println(String.format("+ %2$s Our profit vs buy-and-hold profit: %1$.8f",
                            vsBuyAndHold.calculate(series, tradingRecord), pair.base.getDisplayName()));

                    System.out.println(String.format("== %1$s ================================", pair.base.getDisplayName()));
                    tradingRecord.getTrades().forEach(trade -> {
                                double buyPrice = trade.getEntry().getPrice().toDouble();
                                double sellPrice = trade.getExit().getPrice().toDouble();
                                System.out.println(ansi().fgDefault()
                                        .a(sellPrice <= buyPrice ? ansi().fgRed() : ansi().fgGreen())
                                        .a(String.format("BOUGHT: %1$.8f || SOLD: %2$.8f", buyPrice, sellPrice))
                                        .fgDefault()
                                );
                            }

                    );
                    System.out.println("================================================================");
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
