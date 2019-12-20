/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.context.boostrap.invoker;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

/**
 * A helper class that bootstraps an application using a {@code BootstrapContext}.
 *
 * @param <C> the {@link ApplicationContext} implementation type.
 * @author Stephane Nicoll
 */
public final class BootstrapApplication<C extends GenericApplicationContext> {

	private final Class<C> contextClass;

	private final ApplicationContextInitializer<C> bootstraper;

	private BootstrapApplication(Class<C> contextClass, ApplicationContextInitializer<C> bootstraper) {
		this.contextClass = contextClass;
		this.bootstraper = bootstraper;
	}

	public void run(String[] args) {
		SpringApplication application = new SpringApplication(BootstrapApplication.class);
		application.setApplicationContextClass(this.contextClass);
		application.setInitializers(Arrays.asList(this.bootstraper, new WorkaroundApplicationListener(),
				new ConditionEvaluationReportLoggingListener()));
		application.run(args);
	}

	public static BootstrapApplication<GenericApplicationContext> forNonWebApplication(
			ApplicationContextInitializer<GenericApplicationContext> bootstraper) {
		return new BootstrapApplication<>(GenericApplicationContext.class, bootstraper);
	}

	public static BootstrapApplication<ReactiveWebServerApplicationContext> forReactiveWebApplication(
			ApplicationContextInitializer<ReactiveWebServerApplicationContext> bootstraper) {
		return new BootstrapApplication<>(ReactiveWebServerApplicationContext.class, bootstraper);
	}

	public static BootstrapApplication<ServletWebServerApplicationContext> forServletWebApplication(
			ApplicationContextInitializer<ServletWebServerApplicationContext> bootstraper) {
		return new BootstrapApplication<>(ServletWebServerApplicationContext.class, bootstraper);
	}

	/**
	 * At the moment the ConfigurationClassBeanPostProcessor is registered anyway by the
	 * SpringApplication so this is an attempt to override that with something that
	 * doesn't do anything.
	 */
	static class WorkaroundApplicationListener implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext context) {
			context.registerBean(AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME, Object.class,
					Object::new);
		}

	}

}
