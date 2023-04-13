package com.whiteowl.core.bar;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.Constant;

import lombok.NonNull;
import lombok.SneakyThrows;

public class BarRepository {
	private static final int BYTES = 6*Double.BYTES;
	private static final Duration TIMEPERIOD = Duration.ofMinutes(1);
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

	private String getPath(String code, LocalDate date) {
		return Constant.HOME.resolve(Paths.get("bars", FORMATTER.format(date), code)).toString();
	}

	@SneakyThrows
	public List<Bar> findByCodeAndDate(@NonNull String code, @NonNull LocalDate date) {
		final String path = getPath(code, date);
		if(!Files.exists(Paths.get(path))) return Collections.emptyList();
		synchronized(path) {
			try(RandomAccessFile file = new RandomAccessFile(path, "r")){
				final long length = file.length();
				final List<Bar> bars = new ArrayList<>();
				for(long position = 0; position <= length - BYTES; position += BYTES) {
					final LocalTime time = Constant.NSE_START_TIME.plusMinutes(position / BYTES);
					final ZonedDateTime beginTime = ZonedDateTime.of(date, time, ZoneId.systemDefault());
					bars.add(BaseBar.builder()
							.openPrice(DoubleNum.valueOf(file.readDouble()))
							.highPrice(DoubleNum.valueOf(file.readDouble()))
							.lowPrice(DoubleNum.valueOf(file.readDouble()))
							.closePrice(DoubleNum.valueOf(file.readDouble()))
							.volume(DoubleNum.valueOf(file.readDouble()))
							.openInterest(DoubleNum.valueOf(file.readDouble()))
							.endTime(beginTime.plus(TIMEPERIOD))
							.timePeriod(TIMEPERIOD)
							.build());
				}
				return bars;
			}
		}
	}
	
	@SneakyThrows
	public void saveAll(@NonNull String code, @NonNull List<Bar> bars) {
		final String path = getPath(code, bars.get(0).getBeginTime().toLocalDate());
		Files.createDirectories(Paths.get(path).getParent());
		try(RandomAccessFile file = new RandomAccessFile(path, "rw")){
			for (Bar bar : bars) {
				final long minutes = Duration.between(Constant.NSE_START_TIME, 
						bar.getBeginTime().toLocalTime()).toMinutes();
				if(minutes >= 0) {
					file.seek(minutes*BYTES);
					file.writeDouble(bar.getOpenPrice().doubleValue());
					file.writeDouble(bar.getHighPrice().doubleValue());
					file.writeDouble(bar.getLowPrice().doubleValue());
					file.writeDouble(bar.getClosePrice().doubleValue());
					file.writeDouble(bar.getVolume().doubleValue());
					file.writeDouble(bar.getOpenInterest().doubleValue());
				} else {
					System.err.println("Invalid time : " + bar);
				}
			}
		}
	}

}
