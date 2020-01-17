/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.bootstrap.generator.processor.event;

import java.lang.reflect.Method;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import org.springframework.context.ApplicationListener;

/**
 * Write the necessary code to register an event listener.
 *
 * @author Stephane Nicoll
 */
public class EventListenerRegistrationGenerator {

	private static ClassName METADATA = ClassName.get("org.springframework.context.event", "EventListenerMetadata");

	private final String beanName;

	private final Class<?> type;

	private final Method method;

	private final String eventListenerFactoryBeanName;

	EventListenerRegistrationGenerator(String beanName, Class<?> type, Method method,
			String eventListenerFactoryBeanName) {
		this.beanName = beanName;
		this.type = type;
		this.method = method;
		this.eventListenerFactoryBeanName = eventListenerFactoryBeanName;
	}

	/**
	 * Generate the necessary {@code statements} to register an
	 * {@link ApplicationListener} in the context. Expect local variables to be available
	 * for the {@code context} and the {@code eventListenerRegistrar}.
	 * @param method the method to use to add the registration statement(s)
	 */
	public void generateEventListenerRegistration(MethodSpec.Builder method) {
		CodeBlock.Builder code = CodeBlock.builder();
		code.add("eventListenerRegistrar.register(context, $T.forAnnotatedMethod($S, $T.class, \n", METADATA,
				this.beanName, this.type);
		code.add("$>$>");
		if (this.eventListenerFactoryBeanName != null) {
			code.add("$S, ", this.eventListenerFactoryBeanName);
		}
		code.add("$S", this.method.getName());
		Class<?>[] parameterTypes = this.method.getParameterTypes();
		if (parameterTypes.length > 0) {
			code.add(", ");
		}
		for (int i = 0; i < parameterTypes.length; i++) {
			code.add("$T.class", parameterTypes[i]);
			if (i < parameterTypes.length - 1) {
				code.add(", ");
			}
		}
		code.add("))$<$<");
		method.addStatement(code.build());
	}

}
