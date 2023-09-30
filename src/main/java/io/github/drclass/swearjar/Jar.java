package io.github.drclass.swearjar;

public class Jar implements Comparable<Jar> {
	private long userId;
    private long totalSwears;
    private long totalSlurs;
    private long totalPayout;
    private long currentPayout;

    public Jar(long userId, long totalSwears, long totalSlurs, long totalPayout, long currentPayout) {
        this.userId = userId;
        this.totalSwears = totalSwears;
        this.totalSlurs = totalSlurs;
        this.totalPayout = totalPayout;
        this.currentPayout = currentPayout;
    }

    @Override
    public String toString() {
        return userId + "," + totalSwears + "," + totalSlurs + "," + totalPayout + "," + currentPayout;
    }

    public static Jar fromCsv(String csvLine) {
        String[] values = csvLine.split(",");
        return new Jar(Long.parseLong(values[0]), Long.parseLong(values[1]), Long.parseLong(values[2]),
        		Long.parseLong(values[3]), Long.parseLong(values[4]));
    }

	public long getUserId() {
		return userId;
	}

	public void setUserId(long userId) {
		this.userId = userId;
	}

	public long getTotalSwears() {
		return totalSwears;
	}

	public void setTotalSwears(long totalSwears) {
		this.totalSwears = totalSwears;
	}

	public long getTotalSlurs() {
		return totalSlurs;
	}

	public void setTotalSlurs(long totalSlurs) {
		this.totalSlurs = totalSlurs;
	}

	public long getTotalPayout() {
		return totalPayout;
	}
	
	public String getTotalCurrentPayout() {
		return String.format("%,.2f", ((double) totalPayout) / 100);
	}

	public void setTotalPayout(long totalPayout) {
		this.totalPayout = totalPayout;
	}

	public long getCurrentPayout() {
		return currentPayout;
	}
	
	public String getFormattedCurrentPayout() {
		return String.format("%,.2f", ((double) currentPayout) / 100);
	}

	public void setCurrentPayout(long currentPayout) {
		this.currentPayout = currentPayout;
	}

	@Override
	public int compareTo(Jar o) {
		return (int) (currentPayout - o.currentPayout);
	}
}
