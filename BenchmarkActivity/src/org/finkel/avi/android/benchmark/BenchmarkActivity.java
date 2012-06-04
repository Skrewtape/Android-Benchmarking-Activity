package org.finkel.avi.android.benchmark;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public abstract class BenchmarkActivity extends Activity {

	// We initialize these once to save time in the future.
	private Resources mResources;
	private ListView mTestList;
	private Button mControlButton;
	private View mOverlay;

	private Random mRandom = new Random();

	// This is the actual task that will run.
	private TestRunnerAsyncTask mTask;

	/**
	 * Override this method to provide the base class with a list of BenchmarkTest instances
	 * that represent the code you want to time.  This method will be called only once per
	 * test run, so don't worry about caching instances if you don't need to for your own
	 * reasons.
	 */
	public abstract List<BenchmarkTest> getBenchmarkTests();

	/**
	 * Override this method to provide the base class with the number of times to run each
	 * test instance.
	 */
	public abstract int getNumRunsPerTest();


	@Override
	public void onCreate(Bundle savedInstanceState) {
		// Basic stuff
		super.onCreate(savedInstanceState);
		setContentView(R.layout.ba_main);

		// Get the tests from the subclass.  We call this just once per activity run.
		final List<BenchmarkTest> tests = getBenchmarkTests();

		// Fill in all of our instance-globals for future use.
		mResources = getResources();
		mTestList = (ListView)findViewById(R.id.ba_test_results);
		mOverlay = findViewById(R.id.ba_test_result_overlay);
		mControlButton = (Button)findViewById(R.id.ba_test_control_button);

		// Set up the list view of all of the tests.
		mTestList.setAdapter(new ArrayAdapter<BenchmarkTest>(this, 0, tests){
			@Override
			public View getView(int position, View convertView, ViewGroup parent)
			{
				View view = convertView;
				if (view == null) {
					view = getLayoutInflater().inflate(R.layout.ba_test_list_item, null);
				}

				BenchmarkTest test = getItem(position);
				// Set the "static" values
				((TextView)view.findViewById(R.id.ba_item_title)).setText(test.getName());
				((ProgressBar)view.findViewById(R.id.ba_item_progress)).setMax(getNumRunsPerTest());
				// Set the "dynamic" values
				updateTestStats(view, test);

				return view;
			}
		});

		// Set up the bun/cancel button
		mControlButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(final View button) {

				// If we have a running task, then we cancel it now.
				// The task will fix up the UI once it's finished running.
				if (mTask != null) {
					mTask.cancel(true);
					return;
				}

				// Otherwise, we close up the UI
				mOverlay.setVisibility(View.VISIBLE);
				mControlButton.setText(R.string.ba_cancel_run);

				// And run the tests
				mTask = new TestRunnerAsyncTask();
				mTask.execute(tests.toArray(new BenchmarkTest[0]));
			}
		});
	}

	/**
	 * Update the list item at the given position with the statistics from the
	 * given test.
	 */
	private void updateTestStats(int index, BenchmarkTest test) {

		// Determine if the view for this test is even visible.  We
		// can't do anything if it isn't.
		int firstIndex = mTestList.getFirstVisiblePosition();
		if (
			(index >= firstIndex) &&
			(index < (firstIndex + mTestList.getChildCount()))
		) {
			updateTestStats(mTestList.getChildAt(index - firstIndex), test);
		}
	}

	/**
	 * Update the given list item view with the statistics from the
	 * given test.
	 */
	private void updateTestStats(final View view, BenchmarkTest test) {
		// Get the number of runs this test has had.
		final int runCount = test.getRunCount();

		// Default value for the stats string is nothing.
		final String statsString;
		// But if the test has had some runs, then set some information in there.
		if (runCount > 0) {
			statsString = mResources.getString(
				R.string.ba_item_stats_fmt,
				formatTime(test.getMinRun()),
				formatTime(test.getMaxRun()),
				formatTime(test.getRunMean())
			);
		}
		else {
			statsString = "";
		}

		// Finally, clean up the UI.
		// If I could make sure this method was always called from the UI thread, I wouldn't need this.
		runOnUiThread(new Runnable(){
			@Override
			public void run() {
				((ProgressBar)view.findViewById(R.id.ba_item_progress)).setProgress(runCount);
				((TextView)view.findViewById(R.id.ba_item_stats)).setText(statsString);
			}
		});
	}

	/**
	 * Format a time duration.
	 * @param durationNs Duration in nanoseconds.
	 * @return Duration represented as either nanoseconds, milliseconds or seconds, depending on
	 *		 the order of magnitude of the duration.
	 */
	private String formatTime(long durationNs) {
		int numDigits = (int)(Math.log10(durationNs) + 1);
		if (numDigits < 6) {
			return mResources.getString(R.string.ba_time_ns_fmt, durationNs);
		}
		if (numDigits < 10) {
			return mResources.getString(R.string.ba_time_ms_fmt, ((double)durationNs / 1000000.0d));
		}
		return mResources.getString(R.string.ba_time_sec_fmt, ((double)durationNs / 1000000000.0d));
	}

	/**
	 * AsyncTask that runs all of the tests, in random order.
	 */
	private class TestRunnerAsyncTask extends AsyncTask<BenchmarkTest, Integer, Void> {

		// Keeps track of which tests we can still run.
		private HashSet<Integer> mRemainingTestIndices;
		// Array of all tests -- this array is in the same order as the list view
		private BenchmarkTest[] mTests;

		@Override
		protected void onPreExecute() {
			// Clear out the stored data
			mRemainingTestIndices = new HashSet<Integer>();
		}

		@Override
		protected Void doInBackground(BenchmarkTest... tests) {

			// Store the tests provided
			mTests = tests;

			// Set things up on a per-test basis
			for (int i = 0; i < mTests.length; i++) {
				BenchmarkTest test = mTests[i];
				// Clear any of the stats already stored in the test.
				test.clearResults();
				// Run any user-provided setup before we start our run.
				test.preTestSetup();

				mRemainingTestIndices.add(i);

				// This will clear all of the stats display since we've just cleared the stats.
				updateTestStats(i, test);

				// In hindsight, all of this setup should probably go in onPreExecute, but I don't
				// have access to the tests then.  Unless I move those parameters into this class'
				// constructor, which isn't a terrible idea.
			}

			Integer[] testIndices = mRemainingTestIndices.toArray(new Integer[0]);

			// Keep running until we get cancelled or run out of work to do.
			while (!isCancelled() && (testIndices.length > 0)) {
				int index = testIndices[mRandom.nextInt(testIndices.length)];
				BenchmarkTest test = tests[index];

				// Here we run the actual test and log how long it took.
				long start = System.nanoTime();
				test.runTestInstance();
				test.logTestResult(System.nanoTime() - start);

				// Remove that test from our list of viable tests if we've now run it enough times.
				if (test.getRunCount() >= getNumRunsPerTest()) {
					mRemainingTestIndices.remove(index);
					testIndices = mRemainingTestIndices.toArray(new Integer[0]);
				}
				// Maybe we should batch these up so we don't run this every single time.
				publishProgress(index);
			}

			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... testIndices) {
			// Update the UI for each test.
			for (Integer i : testIndices) {
				updateTestStats(i, mTests[i]);
			}
		}

		@Override
		protected void onPostExecute(Void result) {
			// Get rid of the overlay that disables the list from scrolling
			mOverlay.setVisibility(View.GONE);
			// Set the button text back to the normal setting.
			mControlButton.setText(R.string.ba_run_tests);
			// And let ourself know that we're done running.
			mTask = null;
		}

		@Override
		protected void onCancelled() {
			// Is this legal?  I'm actually surprised onPostExecute() isn't
			// always called when cancelled, or that there isn't some method that's
			// always called when an asynctask is done, but in any case, here we are.
			// Cleanup is the same in both cases.
			onPostExecute(null);
		}
	}
}