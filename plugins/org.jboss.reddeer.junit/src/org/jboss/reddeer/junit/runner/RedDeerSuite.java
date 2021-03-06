/******************************************************************************* 
 * Copyright (c) 2016 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/ 
package org.jboss.reddeer.junit.runner;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jboss.reddeer.common.logging.Logger;
import org.jboss.reddeer.junit.extensionpoint.IAfterTest;
import org.jboss.reddeer.junit.extensionpoint.IBeforeTest;
import org.jboss.reddeer.junit.internal.configuration.SuiteConfiguration;
import org.jboss.reddeer.junit.internal.configuration.TestRunConfiguration;
import org.jboss.reddeer.junit.internal.extensionpoint.AfterTestInitialization;
import org.jboss.reddeer.junit.internal.extensionpoint.BeforeTestInitialization;
import org.jboss.reddeer.junit.internal.runner.EmptySuite;
import org.jboss.reddeer.junit.internal.runner.NamedSuite;
import org.jboss.reddeer.junit.internal.runner.RequirementsRunnerBuilder;
import org.jboss.reddeer.junit.internal.runner.TestsExecutionManager;
import org.jboss.reddeer.junit.internal.runner.TestsWithoutExecutionSuite;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunListener;
import org.junit.runners.Suite;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * 
 * Allows to run the tests (single or a suite) for each configuration file provided.
 * 
 * @author Lucia Jelinkova
 * 
 */
public class RedDeerSuite extends Suite {

	private static final Logger log = Logger.getLogger(RedDeerSuite.class);
	// this variable has to set within static initialization block in child
	// class
	// in order to add custom listeners
	protected static RunListener[] runListeners;

	private static List<IBeforeTest> beforeTestExtensions = RedDeerSuite.initializeBeforeTestExtensions();

	private static List<IAfterTest> afterTestExtensions = RedDeerSuite.initializeAfterTestExtensions();

	/**
	 * Called by the JUnit framework.
	 *
	 * @param clazz the clazz
	 * @param builder the builder
	 * @throws InitializationError the initialization error
	 */
	public RedDeerSuite(Class<?> clazz, RunnerBuilder builder) throws InitializationError {
		this(clazz, builder, new SuiteConfiguration());
	}

	/**
	 * The {@link EmptySuite} makes sure that the @BeforeClass and @AfterClass are not called on the suite class too
	 * often.
	 *
	 * @param clazz the clazz
	 * @param builder the builder
	 * @param config the config
	 * @throws InitializationError the initialization error
	 */
	protected RedDeerSuite(Class<?> clazz, RunnerBuilder builder, SuiteConfiguration config) throws InitializationError {
		super(EmptySuite.class, createSuite(clazz, config));
	}

	/**
	 * Creates a new suite for each configuration file found.
	 *
	 * @param clazz the clazz
	 * @param config the config
	 * @return the list
	 * @throws InitializationError the initialization error
	 */
	public static List<Runner> createSuite(Class<?> clazz, SuiteConfiguration config) throws InitializationError {
		log.info("Creating RedDeer suite...");
		TestsExecutionManager testsManager = new TestsExecutionManager();
		List<Runner> configuredSuites = new ArrayList<Runner>();
		boolean isSuite = isSuite(clazz);

		for (TestRunConfiguration testRunConfig : config.getTestRunConfigurations()) {
			log.info("Adding suite with name " + testRunConfig.getId() + " to RedDeer suite");
			if (isSuite) {
				configuredSuites.add(new NamedSuite(clazz, new RequirementsRunnerBuilder(testRunConfig, runListeners,
						beforeTestExtensions, afterTestExtensions, testsManager), testRunConfig.getId()));
			} else {
				configuredSuites.add(new NamedSuite(new Class[] { clazz }, new RequirementsRunnerBuilder(testRunConfig,
						runListeners, beforeTestExtensions, afterTestExtensions, testsManager), testRunConfig.getId()));
			}
		}

		if (!testsManager.allTestsAreExecuted()) {
			if (isSuite) {
				configuredSuites.add(new TestsWithoutExecutionSuite(clazz, testsManager));
			} else {
				configuredSuites.add(new TestsWithoutExecutionSuite(new Class[] { clazz }, testsManager));
			}
		}
		log.info("RedDeer suite created");
		return configuredSuites;
	}

	private static boolean isSuite(Class<?> clazz) {
		SuiteClasses annotation = clazz.getAnnotation(SuiteClasses.class);
		return annotation != null;
	}

	/* (non-Javadoc)
	 * @see org.junit.runners.ParentRunner#getName()
	 */
	@Override
	protected String getName() {
		return "Red Deer Suite";
	}

	/**
	 * Initializes all Before Test extensions
	 */
	private static List<IBeforeTest> initializeBeforeTestExtensions() {
		List<IBeforeTest> beforeTestExts;
		// check if eclipse is running
		try {
			Class.forName("org.eclipse.core.runtime.Platform");
			log.debug("Eclipse is running");
			beforeTestExts = BeforeTestInitialization.initialize();
		} catch (ClassNotFoundException e) {
			// do nothing extension is implemented only for eclipse right now
			log.debug("Eclipse is not running");
			beforeTestExts = new LinkedList<IBeforeTest>();
		}
		return beforeTestExts;
	}

	/**
	 * Initializes all After Test extensions
	 */
	private static List<IAfterTest> initializeAfterTestExtensions() {
		List<IAfterTest> afterTestExts;
		// check if eclipse is running
		try {
			Class.forName("org.eclipse.core.runtime.Platform");
			log.debug("Eclipse is running");
			afterTestExts = AfterTestInitialization.initialize();
		} catch (ClassNotFoundException e) {
			// do nothing extension is implemented only for eclipse right now
			log.debug("Eclipse is not running");
			afterTestExts = new LinkedList<IAfterTest>();
		}
		return afterTestExts;
	}

}
