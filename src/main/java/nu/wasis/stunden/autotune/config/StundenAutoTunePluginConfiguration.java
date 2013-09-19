package nu.wasis.stunden.autotune.config;

public class StundenAutoTunePluginConfiguration {

	private int minimumDailyWorkDuration;
	private boolean addMissingDays;
	private String defaultProjectName;
	
	public int getMinimumDailyWorkDuration() {
		return minimumDailyWorkDuration;
	}

	public boolean getAddMissingDays() {
		return addMissingDays;
	}

	public String getDefaultProjectName() {
		return defaultProjectName;
	}

}
