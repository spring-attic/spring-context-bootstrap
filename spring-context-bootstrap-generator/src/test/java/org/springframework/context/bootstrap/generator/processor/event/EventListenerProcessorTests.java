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

import java.util.function.Consumer;

import com.squareup.javapoet.MethodSpec;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EventListenerProcessor}.
 *
 * @author Stephane Nicoll
 */
class EventListenerProcessorTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	@Test
	void generateWithNoEventListener() {
		this.contextRunner.run(assertMethod((method) -> assertThat(method).doesNotContain("EventListener")));
	}

	@Test
	void generateWithSimpleEventListener() {
		this.contextRunner.withUserConfiguration(SimpleEventListener.class).run(assertMethod((method) -> {
			assertThat(method).contains(
					"org.springframework.context.event.EventListenerRegistrar eventListenerRegistrar = new org.springframework.context.event.EventListenerRegistrar();");
			assertThat(method).contains(
					"eventListenerRegistrar.register(context, org.springframework.context.event.EventListenerMetadata.forAnnotatedMethod(\"eventListenerProcessorTests.SimpleEventListener\", org.springframework.context.bootstrap.generator.processor.event.EventListenerProcessorTests.SimpleEventListener.class, \n"
							+ "          \"onEvent\", org.springframework.context.event.ContextRefreshedEvent.class));");
		}));
	}

	@Test
	void generateWithSeveralListenersOnTheSameBean() {
		this.contextRunner.withUserConfiguration(DoubleEventListener.class).run(assertMethod((method) -> {
			assertThat(method).contains("EventListenerRegistrar eventListenerRegistrar = new ");
			assertThat(method).contains(
					"forAnnotatedMethod(\"doubleListener\", org.springframework.context.bootstrap.generator.processor.event.EventListenerProcessorTests.DoubleEventListener.class, \n"
							+ "          \"firstEvent\", org.springframework.context.event.ApplicationContextEvent.class));");
			assertThat(method).contains(
					"forAnnotatedMethod(\"doubleListener\", org.springframework.context.bootstrap.generator.processor.event.EventListenerProcessorTests.DoubleEventListener.class, \n"
							+ "          \"secondEvent\"));");
		}));
	}

	private static ContextConsumer<AssertableApplicationContext> assertMethod(Consumer<String> method) {
		return (context) -> {
			MethodSpec.Builder code = MethodSpec.methodBuilder("test");
			new EventListenerProcessor(context.getSourceApplicationContext().getBeanFactory())
					.registerEventListeners(code);
			method.accept(code.build().toString());
		};
	}

	public static class SimpleEventListener {

		@EventListener
		public void onEvent(ContextRefreshedEvent event) {

		}

	}

	@Component("doubleListener")
	public static class DoubleEventListener {

		@EventListener
		public void firstEvent(ApplicationContextEvent event) {

		}

		@EventListener(classes = ContextRefreshedEvent.class)
		public void secondEvent() {

		}

	}

}
