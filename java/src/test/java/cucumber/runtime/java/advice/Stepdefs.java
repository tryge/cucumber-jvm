package cucumber.runtime.java.advice;

import cucumber.annotation.en.Given;

public class Stepdefs {
    @Timed @Deferred
    @Given("^there are (\\d+) cookies$")
    public void there_are_cookies(int arg1) {
    }

    @Timed @Deferred
    @Given("^(\\d) and (\\d+) cookies$")
    public void nth_time_getting_x_cookies(int arg1, int arg2) {
    }
}
