package org.cavarest.pilaf.model;

import java.util.ArrayList;
import java.util.List;

public class TestStory {
    private String name, description, backend;
    private List<Action> setup = new ArrayList<>();
    private List<Action> steps = new ArrayList<>();
    private List<Assertion> assertions = new ArrayList<>();
    private List<Action> cleanup = new ArrayList<>();

    public TestStory() {}
    public TestStory(String name) { this.name = name; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBackend() { return backend; }
    public void setBackend(String backend) { this.backend = backend; }
    public List<Action> getSetup() { return setup; }
    public void setSetup(List<Action> setup) { this.setup = setup; }
    public List<Action> getSteps() { return steps; }
    public void setSteps(List<Action> steps) { this.steps = steps; }
    public List<Assertion> getAssertions() { return assertions; }
    public void setAssertions(List<Assertion> assertions) { this.assertions = assertions; }
    public List<Action> getCleanup() { return cleanup; }
    public void setCleanup(List<Action> cleanup) { this.cleanup = cleanup; }

    public TestStory addSetupAction(Action action) { setup.add(action); return this; }
    public TestStory addStep(Action action) { steps.add(action); return this; }
    public TestStory addAssertion(Assertion assertion) { assertions.add(assertion); return this; }
    public TestStory addCleanupAction(Action action) { cleanup.add(action); return this; }
}
