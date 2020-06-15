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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.scope.ScopedObject;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.EventListenerFactory;
import org.springframework.context.event.EventListenerMethodProcessor;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;

/**
 * Detect beans with methods annotated with {@link EventListener} and write the code
 * necessary to register them as listener. This essentially replaces what
 * {@link EventListenerMethodProcessor} is doing at runtime.
 *
 * @author Stephane Nicoll
 */
public class EventListenerProcessor {

	private static final ClassName REGISTRAR = ClassName.get("org.springframework.context.event",
			"EventListenerRegistrar");

	private static final Log logger = LogFactory.getLog(EventListenerProcessor.class);

	private final ConfigurableListableBeanFactory beanFactory;

	private final Map<String, EventListenerFactory> eventListenerFactories;

	public EventListenerProcessor(ConfigurableListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.eventListenerFactories = beanFactory.getBeansOfType(EventListenerFactory.class);
	}

	public void registerEventListeners(MethodSpec.Builder method) {
		List<EventListenerRegistrationGenerator> eventGenerators = new ArrayList<>();
		for (String beanName : this.beanFactory.getBeanDefinitionNames()) {
			eventGenerators.addAll(process(beanName));
		}
		if (eventGenerators.isEmpty()) {
			return; // No listener detected
		}
		method.addStatement("$T eventListenerRegistrar = new $T()", REGISTRAR, REGISTRAR);
		for (EventListenerRegistrationGenerator eventGenerator : eventGenerators) {
			eventGenerator.generateEventListenerRegistration(method);
		}
	}

	public List<EventListenerRegistrationGenerator> process(String beanName) {
		if (!ScopedProxyUtils.isScopedTarget(beanName)) {
			Class<?> type = null;
			try {
				type = AutoProxyUtils.determineTargetClass(this.beanFactory, beanName);
			}
			catch (Throwable ex) {
				// An unresolvable bean type, probably from a lazy bean - let's ignore it.
				if (logger.isDebugEnabled()) {
					logger.debug("Could not resolve target class for bean with name '" + beanName + "'", ex);
				}
			}
			if (type != null) {
				if (ScopedObject.class.isAssignableFrom(type)) {
					try {
						Class<?> targetClass = AutoProxyUtils.determineTargetClass(this.beanFactory,
								ScopedProxyUtils.getTargetBeanName(beanName));
						if (targetClass != null) {
							type = targetClass;
						}
					}
					catch (Throwable ex) {
						// An invalid scoped proxy arrangement - let's ignore it.
						if (logger.isDebugEnabled()) {
							logger.debug("Could not resolve target bean for scoped proxy '" + beanName + "'", ex);
						}
					}
				}
				try {
					return processBean(beanName, type);
				}
				catch (Throwable ex) {
					throw new BeanInitializationException(
							"Failed to process @EventListener " + "annotation on bean with name '" + beanName + "'",
							ex);
				}
			}
		}
		return null;
	}

	private List<EventListenerRegistrationGenerator> processBean(String beanName, Class<?> targetType) {
		List<EventListenerRegistrationGenerator> result = new ArrayList<>();
		if (AnnotationUtils.isCandidateClass(targetType, EventListener.class)) {
			Map<Method, EventListener> annotatedMethods = null;
			try {
				annotatedMethods = MethodIntrospector.selectMethods(targetType,
						(MethodIntrospector.MetadataLookup<EventListener>) (method) -> AnnotatedElementUtils
								.findMergedAnnotation(method, EventListener.class));
			}
			catch (Throwable ex) {
				// An unresolvable type in a method signature, probably from a lazy bean -
				// let's ignore it.
				if (logger.isDebugEnabled()) {
					logger.debug("Could not resolve methods for bean with name '" + beanName + "'", ex);
				}
			}

			if (CollectionUtils.isEmpty(annotatedMethods)) {
				if (logger.isTraceEnabled()) {
					logger.trace("No @EventListener annotations found on bean class: " + targetType.getName());
				}
			}
			else {
				// Non-empty set of methods
				for (Method method : annotatedMethods.keySet()) {
					for (Entry<String, EventListenerFactory> entry : this.eventListenerFactories.entrySet()) {
						if (entry.getValue().supportsMethod(method)) {
							String factoryBeanName = (!entry.getKey()
									.equals(AnnotationConfigUtils.EVENT_LISTENER_FACTORY_BEAN_NAME)) ? entry.getKey()
											: null;
							result.add(new EventListenerRegistrationGenerator(beanName, targetType, method,
									factoryBeanName));
							break;
						}
					}
				}
				if (logger.isDebugEnabled()) {
					logger.debug(annotatedMethods.size() + " @EventListener methods processed on bean '" + beanName
							+ "': " + annotatedMethods);
				}
			}
		}
		return result;
	}

}
