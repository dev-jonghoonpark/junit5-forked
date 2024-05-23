/*
 * Copyright 2015-2024 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.api.extension;

import static org.apiguardian.api.API.Status.STABLE;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apiguardian.api.API;
import org.junit.platform.commons.JUnitException;
import org.junit.platform.commons.logging.Logger;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.platform.commons.util.ClassNamePatternFilterUtils;

@API(status = STABLE, since = "5.11")
public class AutoDetectedExtensionInvocationHandler implements InvocationHandler {

	private static final Logger logger = LoggerFactory.getLogger(AutoDetectedExtensionInvocationHandler.class);

	private static final String EXTENSIONS_AUTODETECTION_INCLUDE_PROPERTY_NAME = "junit.jupiter.extensions.autodetection.include";
	private static final String EXTENSIONS_AUTODETECTION_EXCLUDE_PROPERTY_NAME = "junit.jupiter.extensions.autodetection.exclude";

	private final Extension autoExtendedExtension;

	// TODO : add every names
	private static final Set<String> extensionMethodNameSet = Collections.unmodifiableSet(
		new HashSet<>(Arrays.asList("afterAll", "afterEach", "afterTestExecution", "beforeAll", "beforeEach",
			"beforeTestExecution", "supportsParameter", "resolveParameter")));;

	public AutoDetectedExtensionInvocationHandler(Extension autoExtendedExtension) {
		this.autoExtendedExtension = autoExtendedExtension;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (extensionMethodNameSet.contains(method.getName())) {
			for (Object arg : args) {
				if (arg instanceof ExtensionContext) {
					ExtensionContext extensionContext = (ExtensionContext) arg;

					if (extensionContext.getTestClass().isPresent()) {
						String testClassName = extensionContext.getTestClass().orElseThrow(
							() -> new JUnitException("testClass not exist")).getName();

						String include = extensionContext.getConfigurationParameter(
							EXTENSIONS_AUTODETECTION_INCLUDE_PROPERTY_NAME).orElse("*");
						String exclude = extensionContext.getConfigurationParameter(
							EXTENSIONS_AUTODETECTION_EXCLUDE_PROPERTY_NAME).orElse(null);

						if (ClassNamePatternFilterUtils.excludeMatchingClassNames(include).test(testClassName)) {
							logger.trace(() -> "package not included");
							return null;
						}

						if (!ClassNamePatternFilterUtils.excludeMatchingClassNames(exclude).test(testClassName)) {
							logger.trace(() -> "package excluded");
							return null;
						}
					}

					break;
				}
			}
		}

		return method.invoke(autoExtendedExtension, args);
	}

}
