package io.github.romatroskin.altrader.bot;

import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.poloniex.service.PoloniexChartDataPeriodType;

import java.util.concurrent.TimeUnit;

/**
 * Created by romatroskin on 5/25/17.
 */
public interface BotTaskRunner {
    void start(CurrencyPair pair, ExchangeSpecification spec) throws Exception;
    void stop();

    int DELAY = 15;
    int MAX_TICK_COUNT = 8640;
    long lookBackDaysInSeconds = TimeUnit.DAYS.toSeconds(30);
    PoloniexChartDataPeriodType period = PoloniexChartDataPeriodType.PERIOD_300;
}
