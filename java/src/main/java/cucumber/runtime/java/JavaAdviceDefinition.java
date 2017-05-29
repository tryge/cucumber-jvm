package cucumber.runtime.java;

import cucumber.runtime.JdkPatternArgumentMatcher;
import cucumber.runtime.ParameterType;
import cucumber.runtime.StepDefinition;
import cucumber.runtime.Utils;
import gherkin.I18n;
import gherkin.formatter.Argument;
import gherkin.formatter.model.Step;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class JavaAdviceDefinition {
	final Method method;
	final Pattern pattern;
	final List<Class<? extends Annotation>> advices;
	final int timeout;
	final JdkPatternArgumentMatcher argumentMatcher;
	final List<ParameterType> parameterTypes;
	final ObjectFactory objectFactory;

	public JavaAdviceDefinition(Method method, Pattern pattern, List<Class<? extends Annotation>> advices, int timeout, ObjectFactory objectFactory) {
		this.method = method;
		this.parameterTypes = ParameterType.fromMethod(method);
		this.parameterTypes.remove(0); // remove the Runnable type
		this.pattern = pattern;
		this.advices = advices;
		this.argumentMatcher = new JdkPatternArgumentMatcher(pattern);
		this.timeout = timeout;
		this.objectFactory = objectFactory;
	}

	public int parameterCount() {
		return parameterTypes.size();
	}

	public StepDefinition advise(JavaStepDefinition stepDefinition) {
		return new AdvisedStepDefinition(this, stepDefinition);
	}
}

class AdvisedStepDefinition implements StepDefinition {
	private final JavaAdviceDefinition advice;
	private final JavaStepDefinition step;

	public AdvisedStepDefinition(JavaAdviceDefinition advice, JavaStepDefinition step) {
		this.advice = advice;
		this.step = step;
	}

	@Override
	public List<Argument> matchedArguments(Step step) {
		Pattern pattern = Pattern.compile(advice.pattern.toString() + "\\s*");

		Matcher matcher = pattern.matcher(step.getName());
		if (matcher.find()) {
			int adviceEnd = matcher.end();

			String advicePart = step.getName().substring(0, adviceEnd);
			String stepPart = step.getName().substring(adviceEnd).trim();

			Step modifiedStep = new Step(step.getComments(), step.getKeyword(), stepPart, step.getLine(), step.getRows(), step.getDocString());

			List<Argument> matchedAdviceArguments = advice.argumentMatcher.argumentsFrom(advicePart.trim());
			List<Argument> stepAdviceArguments = this.step.matchedArguments(modifiedStep);

			if (stepAdviceArguments == null) {
				return null;
			}

			matchedAdviceArguments.addAll(this.step.matchedArguments(modifiedStep));
			return matchedAdviceArguments;
		}

		return null;
	}

	@Override
	public String getLocation(boolean b) {
		return step.getLocation(b);
	}

	@Override
	public Integer getParameterCount() {
		return advice.parameterCount() + step.getParameterCount();
	}

	@Override
	public ParameterType getParameterType(int i, Type type) throws IndexOutOfBoundsException {
		int adviceParameters = advice.parameterCount();
		if (i < adviceParameters) {
			return advice.parameterTypes.get(i);
		} else {
			return step.getParameterType(i - adviceParameters, type);
		}
	}

	@Override
	public void execute(final I18n i18n, final Step matchedStep, Object[] objects) throws Throwable {
		int parameterCount = step.getParameterCount();

		final Object[] stepParameters = new Object[parameterCount];
		System.arraycopy(objects, objects.length - parameterCount, stepParameters, 0, parameterCount);

		Runnable wrappedStepExecution = new Runnable() {
			public void run() {
                            try {
				step.execute(i18n, matchedStep, stepParameters);
                            } catch(RuntimeException re) {
                                throw re;
                            } catch(Throwable th) {
                                throw new RuntimeException(th.getMessage(), th);
                            }
			}
		};
		RunnableStep runnableStep = new RunnableStep(matchedStep, wrappedStepExecution);

		Object[] adviceParameters = new Object[advice.parameterCount() + 1];
		adviceParameters[0] = runnableStep;
		System.arraycopy(objects, 0, adviceParameters, 1, advice.parameterCount());

		Object adviceObj = advice.objectFactory.getInstance(advice.method.getDeclaringClass());
		Utils.invoke(adviceObj, advice.method, advice.timeout, adviceParameters);
	}

	@Override
	public boolean isDefinedAt(StackTraceElement stackTraceElement) {
		return step.isDefinedAt(stackTraceElement);
	}

	@Override
	public String getPattern() {
		return advice.pattern.toString() + "\\s*" + step.getPattern();
	}
}
