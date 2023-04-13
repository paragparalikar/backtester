package com.whiteowl.core.bar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap.SimpleEntry;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.ta4j.core.Bar;
import org.ta4j.core.BaseBar;
import org.ta4j.core.num.DoubleNum;

import com.whiteowl.core.util.Tuple2;

import lombok.SneakyThrows;

public class BarDataLoader {
	private static final Duration TIMEPERIOD = Duration.ofMinutes(1);
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
	private static final Comparator<Bar> COMPARATOR = Comparator.comparing(Bar::getBeginTime);
	private static final BarRepository REPOSITORY = new BarRepository();
	
	public static void main(String[] args) throws IOException, InterruptedException {
		Files.list(Paths.get("C:\\trading\\data\\NIFTY 50\\options\\files\\weekly")).parallel()
			.forEach(BarDataLoader::process);
	}
	
	@SneakyThrows
	private static void process(Path path) {
		final Map<Tuple2<String, LocalDate>,List<Bar>> data = Files.lines(path)
			.filter(BarDataLoader::predicate)
			.map(BarDataLoader::parse)
			.collect(Collectors.groupingBy(
					entry -> Tuple2.of(entry.getKey(), entry.getValue().getBeginTime().toLocalDate()), 
					Collectors.mapping(Entry::getValue, Collectors.toList())));
		for(Tuple2<String, LocalDate> key : data.keySet()) {
			final List<Bar> bars = data.get(key);
			bars.sort(COMPARATOR);
			REPOSITORY.saveAll(key.getKey(), bars);
		}
		System.out.printf("Extracted %d codes from file %s\n", data.size(), path.toString());
	}
	
	private static Entry<String, Bar> parse(String line) {
		final String[] tokens = line.split(",");
		final String code = tokens[0];
		final LocalDateTime localDateTime = LocalDateTime.parse(tokens[1], FORMATTER);
		final ZonedDateTime beginTime = ZonedDateTime.of(localDateTime, ZoneId.systemDefault());
		final Bar bar = BaseBar.builder()
				.timePeriod(TIMEPERIOD)
				.endTime(beginTime.plus(TIMEPERIOD))
				.openPrice(DoubleNum.valueOf(tokens[2]))
				.highPrice(DoubleNum.valueOf(tokens[3]))
				.lowPrice(DoubleNum.valueOf(tokens[4]))
				.closePrice(DoubleNum.valueOf(tokens[5]))
				.volume(DoubleNum.valueOf(tokens[6]))
				.openInterest(DoubleNum.valueOf(tokens[7]))
				.build();
		return new SimpleEntry<>(code, bar);
	}
	
	private static boolean predicate(String line) {
		if(line.startsWith("NIFTY,")) return true;
		if(line.startsWith("BANKNIFTY,")) return true;
		if(line.startsWith("INDIAVIX,")) return true;
		if(line.startsWith("NIFTYWK")) return true;
		if(line.startsWith("BANKNIFTYWK")) return true;
		return false;
	}

}
