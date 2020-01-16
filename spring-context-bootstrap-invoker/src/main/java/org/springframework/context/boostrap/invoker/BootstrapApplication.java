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
import java.util.Collection;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.ResourceLoader;

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
		SpringApplication application = new BootstrapSpringApplication();
		application.setApplicationContextClass(this.contextClass);
		application.setInitializers(Arrays.asList(this.bootstraper, new ConditionEvaluationReportLoggingListener()));
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
	 * An extension of {@link SpringApplication} that does not handle primary sources at
	 * all.
	 */
	static class BootstrapSpringApplication extends SpringApplication {

		BootstrapSpringApplication() {
			super((ResourceLoader) null, Object.class);
		}

		@Override
		public void addPrimarySources(Collection<Class<?>> additionalPrimarySources) {
			throw new UnsupportedOperationException("Sources can't be set.");
		}

		@Override
		public void setSources(Set<String> sources) {
			throw new UnsupportedOperationException("Sources can't be set.");
		}

		@Override
		protected void load(ApplicationContext context, Object[] sources) {
			// this effectively ignore any source that was registered.
		}

	}

}
