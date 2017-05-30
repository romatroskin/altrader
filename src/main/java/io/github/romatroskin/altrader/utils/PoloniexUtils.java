package io.github.romatroskin.altrader.utils;

import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import org.joda.time.DateTime;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.poloniex.PoloniexExchange;
import org.knowm.xchange.poloniex.dto.marketdata.PoloniexChartData;
import org.knowm.xchange.poloniex.service.PoloniexChartDataPeriodType;
import org.knowm.xchange.poloniex.service.PoloniexMarketDataService;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Created by romatroskin on 5/25/17.
 */
public class PoloniexUtils {
    public static ExchangeSpecification getSpec(String apiKey, String secretKey) {
        final ExchangeSpecification spec = new ExchangeSpecification(PoloniexExchange.class);
        spec.setApiKey(apiKey);
        spec.setSecretKey(secretKey);
//        spec.setApiKey("VGQY43TS-CE6SWS66-85XXI63W-YLLVMEJ8");
//        spec.setSecretKey("dee184677b07746e99769405745f6ddea4781046432c9847b4a5a1a5423ffaa8845b170011585e48ff98288bc871548a1cfc31581d7e1634ca36bfaa4eac74e4");
        return spec;
    }

    public static TimeSeries loadTimeSeries(CurrencyPair pair, ExchangeSpecification spec) throws IOException {
        final long now = new Date().getTime() / 1000;
        final PoloniexExchange poloniexExchange = (PoloniexExchange) ExchangeFactory.INSTANCE.createExchange(spec);
        final PoloniexMarketDataService marketDataService = (PoloniexMarketDataService)
                poloniexExchange.getMarketDataService();

        final PoloniexChartData[] chartData = marketDataService.
                getPoloniexChartData(pair, now - TimeUnit.DAYS.toSeconds(30), now, PoloniexChartDataPeriodType.PERIOD_300);
        List<Tick> tickList = Arrays.stream(chartData).map(chart -> new Tick(new DateTime(chart.getDate()),
                chart.getOpen().doubleValue(), chart.getHigh().doubleValue(), chart.getLow().doubleValue(),
                chart.getClose().doubleValue(), chart.getVolume().doubleValue())).collect(Collectors.toList());
        return new TimeSeries(tickList);
    }
}
