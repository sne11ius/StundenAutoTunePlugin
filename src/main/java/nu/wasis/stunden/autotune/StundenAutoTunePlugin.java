package nu.wasis.stunden.autotune;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import net.xeoh.plugins.base.annotations.PluginImplementation;
import nu.wasis.stunden.autotune.config.StundenAutoTunePluginConfiguration;
import nu.wasis.stunden.exception.InvalidConfigurationException;
import nu.wasis.stunden.model.Day;
import nu.wasis.stunden.model.Entry;
import nu.wasis.stunden.model.WorkPeriod;
import nu.wasis.stunden.plugin.ProcessPlugin;
import nu.wasis.stunden.util.DateUtils;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Duration;

@PluginImplementation
public class StundenAutoTunePlugin implements ProcessPlugin {

	private static final Logger LOG = Logger.getLogger(StundenAutoTunePlugin.class);
	
	private static final Random RANDOM = new Random();
	
	@Override
	public WorkPeriod process(final WorkPeriod workPeriod, final Object configuration) {
		LOG.info("Fixing work durations...");
		if (null == configuration || !(configuration instanceof StundenAutoTunePluginConfiguration)) {
			throw new InvalidConfigurationException("Configuration null or wrong type. You probably need to fix your configuration file.");
		}
		final StundenAutoTunePluginConfiguration myConfig = (StundenAutoTunePluginConfiguration) configuration;
		if (0 >= myConfig.getMinimumDailyWorkDuration()) {
			throw new InvalidConfigurationException("Param `minimumDailyWorkDuration' should be > 0. I'm outta here...");
		}
		for (final Day day : workPeriod.getDays()) {
			if (myConfig.getMinimumDailyWorkDuration() > day.getWorkDuration().getStandardHours()) {
				fixWorkDuration(myConfig,day);
			}
		}
		LOG.info("...done.");
		return workPeriod;
	}
	
	private void fixWorkDuration(final StundenAutoTunePluginConfiguration myConfig, final Day day) {
		final Duration missingDuration = createDuration(myConfig.getMinimumDailyWorkDuration()).minus(day.getWorkDuration());
		LOG.debug("Not enough hours (" + DateUtils.PERIOD_FORMATTER.print(day.getWorkDuration().toPeriod()) + ") for " + DateUtils.DATE_FORMATTER.print(day.getDate()) + ".");
		LOG.debug("Missing: " + DateUtils.PERIOD_FORMATTER.print(missingDuration.toPeriod()));
		final long totalMinutesToAdd = missingDuration.getStandardMinutes();
		if (Integer.MAX_VALUE < totalMinutesToAdd) {
			LOG.error("Would have to add too many minutes. Skipping this day --> it will not be fixed D:");
			return;
		}
		addDistributed(totalMinutesToAdd, day.getEntries());
		LOG.debug("Fixed work duration for this day: " + DateUtils.PERIOD_FORMATTER.print(day.getWorkDuration().toPeriod()));
		final int stillMissingMinutes = (int) (DateTimeConstants.MINUTES_PER_HOUR - (day.getWorkDuration().getStandardMinutes() % 60));
		if (DateTimeConstants.MINUTES_PER_HOUR != stillMissingMinutes) {
			addMinutesToRandomEntry(stillMissingMinutes, day.getEntries());
			LOG.debug("Ultimate work duration for this day: " + DateUtils.PERIOD_FORMATTER.print(day.getWorkDuration().toPeriod()));
		}
	}
	
	private void addDistributed(final long totalMinutesToAdd, final Collection<Entry> entries) {
		final int minutesToAdd = ((int) totalMinutesToAdd) / entries.size();
		LOG.debug("Adding " + minutesToAdd + " minutes to each entry...");
		for (final Entry entry : entries) {
			entry.setEnd(entry.getEnd().plusMinutes(minutesToAdd));
		}
	}
	
	private void addMinutesToRandomEntry(final int minutes, final List<Entry> entries) {
		LOG.debug("This day still needs some love...");
		LOG.debug("Still missing minutes: " + minutes);
		final Entry randomEntry =entries.get(RANDOM.nextInt(entries.size()));
		randomEntry.setEnd(randomEntry.getEnd().plusMinutes(minutes));
	}
	
	private Duration createDuration(final int hours) {
		final DateTime start = new DateTime();
		final DateTime end = start.plusHours(hours);
		return new Duration(start, end);
	}

	@Override
	public Class<?> getConfigurationClass() {
		return StundenAutoTunePluginConfiguration.class;
	}

}
