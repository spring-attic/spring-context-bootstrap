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

package sample.generator;

import java.util.Arrays;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.logging.ConditionEvaluationReportLoggingListener;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Start this application with the generated {@code ContextBootstrap.}
 *
 * @author Stephane Nicoll
 */
public class BootstrapSimpleApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(BootstrapSimpleApplication.class);
		application.setApplicationContextClass(GenericApplicationContext.class);
		application.setInitializers(Arrays.asList(new BootstrapApplicationListener(),
				new WorkaroundApplicationListener(), new ConditionEvaluationReportLoggingListener()));
		application.run(args);
	}

	static class BootstrapApplicationListener implements ApplicationContextInitializer<GenericApplicationContext> {

		@Override
		public void initialize(GenericApplicationContext context) {
			// new ContextBootstrap().bootstrap(context);
		}

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
