package cucumber.runtime.java;

import gherkin.formatter.model.*;

public class RunnableStep extends Step implements Runnable {

	private Step step;
	private Runnable delegate;

	public RunnableStep(Step step, Runnable delegate) {
		super(step.getComments(), step.getKeyword(), step.getName(), step.getLine(), step.getRows(), step.getDocString());

		this.step = step;
		this.delegate = delegate;
	}

	@Override
	public void run() {
		delegate.run();
	}
}
