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

package org.springframework.context.event;

import java.lang.reflect.Method;

import org.springframework.context.ApplicationListener;
import org.springframework.util.ReflectionUtils;

/**
 * Captures the necessary metadata to build an {@link ApplicationListener} based on a
 * method annotated with {@link EventListener}.
 *
 * @author Stephane Nicoll
 */
public class EventListenerMetadata {

	private final String beanName;

	private final Class<?> type;

	private final String eventListenerFactoryBeanName;

	private final Method method;

	EventListenerMetadata(String beanName, Class<?> type, Method method, String eventListenerFactoryBeanName) {
		this.beanName = beanName;
		this.type = type;
		this.method = method;
		this.eventListenerFactoryBeanName = eventListenerFactoryBeanName;
	}

	public String getBeanName() {
		return this.beanName;
	}

	public Class<?> getType() {
		return this.type;
	}

	public String getEventListenerFactoryBeanName() {
		return this.eventListenerFactoryBeanName;
	}

	public Method getMethod() {
		return this.method;
	}

	public static EventListenerMetadata forAnnotatedMethod(String beanName, Class<?> type, String methodName,
			Class<?>... parameterTypes) {
		return forAnnotatedMethod(beanName, type, null, methodName, parameterTypes);
	}

	public static EventListenerMetadata forAnnotatedMethod(String beanName, Class<?> type,
			String eventListenerFactoryBeanName, String methodName, Class<?>... parameterTypes) {
		return new EventListenerMetadata(beanName, type, findMethod(type, methodName, parameterTypes),
				eventListenerFactoryBeanName);
	}

	private static Method findMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
		Method method = ReflectionUtils.findMethod(type, methodName, parameterTypes);
		if (method == null) {
			throw new IllegalStateException("No method named '" + methodName + "' found on " + type);
		}
		return method;
	}

}
