package cucumber.annotation;

import java.lang.annotation.*;

/**
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Advice {
    /**
     * @return the pattern that is used to match a step with this advice
     */
    String value();

    /**
     * @return the pointcut annotations (i.e. they have to be annotated with
     * <code>Pointcut</code>) that this advice may advise.
     */
    Class<? extends Annotation>[] pointcuts();

    /**
     * @return max amount of time this is allowed to run for. 0 (default) means no restriction.
     */
    int timeout() default 0;
}
