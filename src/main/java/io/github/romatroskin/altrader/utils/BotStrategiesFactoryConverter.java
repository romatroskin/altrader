package io.github.romatroskin.altrader.utils;

import com.beust.jcommander.IStringConverter;
import io.github.romatroskin.altrader.bot.BotStrategiesFactory;

/**
 * Created by romatroskin on 5/31/17.
 */
public class BotStrategiesFactoryConverter implements IStringConverter<BotStrategiesFactory> {
    @Override
    public BotStrategiesFactory convert(String s) {
        return BotStrategiesFactory.valueOf(s);
    }
}
