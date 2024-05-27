/*
 * Copyright 2015-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.extension;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassNamePatternFilterUtils;

/**
 * @since 5.11
 */
class AutoDetectedExtension implements BeforeAllCallback, BeforeEachCallback, BeforeTestExecutionCallback,
		AfterAllCallback, AfterEachCallback, AfterTestExecutionCallback, ParameterResolver {

	private static final Logger logger = LoggerFactory.getLogger(AutoDetectedExtension.class);

	private static final String EXTENSIONS_AUTODETECTION_INCLUDE_PROPERTY_NAME = "junit.jupiter.extensions.autodetection.include";
	private static final String EXTENSIONS_AUTODETECTION_EXCLUDE_PROPERTY_NAME = "junit.jupiter.extensions.autodetection.exclude";
	private final Extension originalExtension;

	public AutoDetectedExtension(Extension originalExtension) {
		this.originalExtension = originalExtension;
	}

	public Extension getOriginalExtension() {
		return originalExtension;
	}

	public boolean isInstance(Class<?> classObject) {
		return classObject.isInstance(originalExtension);
	}

	@Override
	public void beforeAll(ExtensionContext context) throws Exception {
		if (isInstance(BeforeAllCallback.class) && validateTestPath(context)) {
			((BeforeAllCallback) originalExtension).beforeAll(context);
		}
	}

	@Override
	public void beforeEach(ExtensionContext context) throws Exception {
		if (isInstance(BeforeEachCallback.class) && validateTestPath(context)) {
			((BeforeEachCallback) originalExtension).beforeEach(context);
		}
	}

	@Override
	public void beforeTestExecution(ExtensionContext context) throws Exception {
		if (isInstance(BeforeTestExecutionCallback.class) && validateTestPath(context)) {
			((BeforeTestExecutionCallback) originalExtension).beforeTestExecution(context);
		}
	}

	@Override
	public void afterAll(ExtensionContext context) throws Exception {
		if (isInstance(AfterAllCallback.class) && validateTestPath(context)) {
			((AfterAllCallback) originalExtension).afterAll(context);
		}
	}

	@Override
	public void afterEach(ExtensionContext context) throws Exception {
		if (isInstance(AfterEachCallback.class) && validateTestPath(context)) {
			((AfterEachCallback) originalExtension).afterEach(context);
		}
	}

	@Override
	public void afterTestExecution(ExtensionContext context) throws Exception {
		if (isInstance(AfterTestExecutionCallback.class) && validateTestPath(context)) {
			((AfterTestExecutionCallback) originalExtension).afterTestExecution(context);
		}
	}

	@Override
	public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		if (isInstance(ParameterResolver.class) && validateTestPath(extensionContext)) {
			return ((ParameterResolver) originalExtension).supportsParameter(parameterContext, extensionContext);
		}
		return false;
	}

	@Override
	public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
			throws ParameterResolutionException {
		if (isInstance(ParameterResolver.class) && validateTestPath(extensionContext)) {
			((ParameterResolver) originalExtension).resolveParameter(parameterContext, extensionContext);
		}
		return null;
	}

	private boolean validateTestPath(ExtensionContext extensionContext) {
		if (extensionContext.getTestClass().isPresent()) {
			String testClassName = extensionContext.getTestClass().orElseThrow(
				() -> new JUnitException("testClass not exist")).getName();

			String include = extensionContext.getConfigurationParameter(
				EXTENSIONS_AUTODETECTION_INCLUDE_PROPERTY_NAME).orElse("*");
			String exclude = extensionContext.getConfigurationParameter(
				EXTENSIONS_AUTODETECTION_EXCLUDE_PROPERTY_NAME).orElse(null);

			if (ClassNamePatternFilterUtils.excludeMatchingClassNames(include).test(testClassName)) {
				logger.trace(() -> "package not included");
				return false;
			}

			if (!ClassNamePatternFilterUtils.excludeMatchingClassNames(exclude).test(testClassName)) {
				logger.trace(() -> "package excluded");
				return false;
			}
		}

		return true;
	}
}
