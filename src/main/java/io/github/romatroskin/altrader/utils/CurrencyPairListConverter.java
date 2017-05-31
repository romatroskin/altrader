package io.github.romatroskin.altrader.utils;

import com.beust.jcommander.IStringConverter;
import org.knowm.xchange.currency.CurrencyPair;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by romatroskin on 5/31/17.
 */
public class CurrencyPairListConverter implements IStringConverter<List<CurrencyPair>> {
    @Override
    public List<CurrencyPair> convert(String s) {
        final String [] currencies = s.split(",");
        final List<CurrencyPair> currencyPairs = new ArrayList<>();
        for(String currency : currencies) {
            currencyPairs.add(new CurrencyPair(currency));
        }

        return currencyPairs;
    }
}
