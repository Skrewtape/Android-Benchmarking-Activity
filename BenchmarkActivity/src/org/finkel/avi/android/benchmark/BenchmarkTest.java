package org.finkel.avi.android.benchmark;

/**
 * Subclasses of this class represent chunks of code that will be
 * run by the test harness over and over again and their speed measured.
 */
public abstract class BenchmarkTest
{
	private String mName;

	/**
	 * Every test has some name.  I don't enforce uniqueness, but you
	 * probably should.
	 */
	public BenchmarkTest(String name)
	{
		mName = name;
	}

	/**
	 * Override this method if you want your test to do some kind of internal
	 * initialization before the beginning of a test run (NOT before each test
	 * instance).
	 */
	public void preTestSetup()
	{
		// Default -- do nothing
	}

	/**
	 * Override this method with the logic you're benchmarking.
	 */
	public abstract void runTestInstance();

	/**
	 * Get the name of this test.
	 */
	public final String getName()
	{
		return mName;
	}

	/**
	 * Record the fact that a single run of this test took the given number of nanoseconds.
	 */
	public final void logTestResult(long testNs)
	{
		if (testNs < mMinRun) { mMinRun = testNs; }
		if (testNs > mMaxRun) { mMaxRun = testNs; }
		mRunCount++;
		mRunSum += testNs;
	}

	/**
	 * Reset test run statistics.
	 */
	public final void clearResults()
	{
		mMinRun   = Long.MAX_VALUE;
		mMaxRun   = 0;
		mRunSum   = 0;
		mRunCount = 0;
	}

	// All measured in nanoseconds
	private long mMinRun   = 0;
	private long mMaxRun   = Long.MAX_VALUE;
	private long mRunSum   = 0;
	private int  mRunCount = 0;

	/** Get the number of nanoseconds elapsed in the fastest run of this test. */
	public final long getMinRun()   { return mMinRun;   }
	/** Get the number of nanoseconds elapsed in the slowest run of this test. */
	public final long getMaxRun()   { return mMaxRun;   }
	/** Get the arithmetic mean (in nanoseconds) of all run durations of this test */
	public final long getRunMean()  { return mRunSum / mRunCount;  }
	/** Get the number of times this test has been run. */
	public final int  getRunCount() { return mRunCount; }
}
