package org.finkel.avi.android.benchmark;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an example subclass of the BenchmarkActivity that demonstrates one way of using it.
 */
public class SampleBenchmarkConsumerActivity extends BenchmarkActivity {

	@SuppressWarnings("serial")
	@Override
	public List<BenchmarkTest> getBenchmarkTests() {
		return new ArrayList<BenchmarkTest>(){{
			add(new BenchmarkTest("Plusses") {
				@Override
				public void runTestInstance() {
					String[] input = getTestData();
					setResult(input[0] + ":" + input[1] + "=" + input[2]);
				}
			});
			add(new BenchmarkTest("Format") {
				@Override
				public void runTestInstance() {
					String[] input = getTestData();
					setResult(String.format("%s:%s=%s", input[0], input[1], input[2]));
				}
			});
			add(new BenchmarkTest("Builder - Init 50") {
				@Override
				public void runTestInstance() {
					String[] input = getTestData();
					StringBuilder builder = new StringBuilder(50);
					builder.setLength(0);
					builder.append(input[0]);
					builder.append(":");
					builder.append(input[1]);
					builder.append("=");
					builder.append(input[2]);
					setResult(builder.toString());
				}
			});
			add(new BenchmarkTest("Builder - No Init") {
				@Override
				public void runTestInstance() {
					String[] input = getTestData();
					StringBuilder builder = new StringBuilder();
					builder.setLength(0);
					builder.append(input[0]);
					builder.append(":");
					builder.append(input[1]);
					builder.append("=");
					builder.append(input[2]);
					setResult(builder.toString());
				}
			});
			add(new BenchmarkTest("Cached Builder") {
				private StringBuilder mBuilder;

				@Override
				public void preTestSetup()
				{
					mBuilder = new StringBuilder(50);
				}

				@Override
				public void runTestInstance() {
					String[] input = getTestData();
					mBuilder.setLength(0);
					mBuilder.append(input[0]);
					mBuilder.append(":");
					mBuilder.append(input[1]);
					mBuilder.append("=");
					mBuilder.append(input[2]);
					setResult(mBuilder.toString());
				}
			});
		}};
	}

	@Override
	public int getNumRunsPerTest() { return 5000; }

	// This stuff is all here so the compiler doesn't optimize it all away.

	String[][] mTestData = new String[][]{
		new String[]{"a", "b", "c"},
		new String[]{"x", "y", "z"},
		new String[]{"y", "y", "z"}
	};

	private String[] getTestData() { return mTestData[(int)(Math.random() * 3)]; }

	private String mResult;

	private void setResult(String result) { mResult = result; }

	public String getResult() { return mResult; }
}