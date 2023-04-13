package com.whiteowl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;

import org.ta4j.core.num.DoubleNum;
import org.ta4j.core.num.Num;

public final class Constant {
	public static final Num ZERO = DoubleNum.valueOf(0);
	public static final Num ONE = DoubleNum.valueOf(1);
	public static final Num TWO = DoubleNum.valueOf(2);
	public static final Num THREE = DoubleNum.valueOf(3);
	
	public static final LocalTime NSE_START_TIME = LocalTime.of(9, 15);
	public static final LocalTime NSE_END_TIME = LocalTime.of(15, 30);
	public static final LocalTime ZERODHA_SQUARE_OFF_TIME = LocalTime.of(15, 20);

	public static final Path HOME = Paths.get(System.getProperty("user.home"), ".backtester");

}
