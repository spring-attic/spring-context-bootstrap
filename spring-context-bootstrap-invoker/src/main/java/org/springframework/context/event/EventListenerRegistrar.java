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

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Register an {@link ApplicationListener} based on {@link EventListenerMetadata}.
 *
 * @author Stephane Nicoll
 */
public class EventListenerRegistrar {

	private static final EventListenerFactory DEFAULT_EVENT_LISTENER_FACTORY = new DefaultEventListenerFactory();

	private final EventExpressionEvaluator evaluator = new EventExpressionEvaluator();

	public void register(GenericApplicationContext context, EventListenerMetadata metadata) {
		EventListenerFactory factory = determineEventListenerFactory(context,
				metadata.getEventListenerFactoryBeanName());
		// Note: the original code request the type to the BeanFactory based on the bean
		// name but we can't do that at this point as the context hasn't refreshed yet.
		Method methodToUse = AopUtils.selectInvocableMethod(metadata.getMethod(), metadata.getType());
		ApplicationListener<?> applicationListener = factory.createApplicationListener(metadata.getBeanName(),
				metadata.getType(), methodToUse);
		if (applicationListener instanceof ApplicationListenerMethodAdapter) {
			((ApplicationListenerMethodAdapter) applicationListener).init(context, this.evaluator);
		}
		context.addApplicationListener(applicationListener);
	}

	private EventListenerFactory determineEventListenerFactory(GenericApplicationContext context,
			String factoryBeanName) {
		return (factoryBeanName != null) ? context.getBean(factoryBeanName, EventListenerFactory.class)
				: DEFAULT_EVENT_LISTENER_FACTORY;
	}

}
