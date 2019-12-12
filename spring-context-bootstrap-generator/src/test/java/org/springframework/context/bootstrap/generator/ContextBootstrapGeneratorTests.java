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

package org.springframework.context.bootstrap.generator;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.bootstrap.generator.sample.SimpleConfiguration;
import org.springframework.context.bootstrap.generator.test.ContextBootstrapGeneratorTester;
import org.springframework.context.bootstrap.generator.test.ContextBootstrapStructure;
import org.springframework.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ContextBootstrapGenerator}.
 *
 * @author Stephane Nicoll
 */
class ContextBootstrapGeneratorTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

	private ContextBootstrapGeneratorTester generatorTester;

	@BeforeEach
	void setup(@TempDir Path directory) {
		this.generatorTester = new ContextBootstrapGeneratorTester(directory);
	}

	@Test
	void bootstrapClassGeneratesStructure() {
		ContextBootstrapStructure structure = this.generatorTester.generate(this.contextRunner);
		assertThat(structure).contextBootstrap().lines().containsSubsequence("public class ContextBootstrap {",
				"  public void bootstrap(GenericApplicationContext context) {", "  }", "}");
		assertThat(structure).contextBootstrap().contains("import " + GenericApplicationContext.class.getName() + ";");
	}

	@Test
	void bootstrapClassWithBeanMethodAndNoParameter() {
		ContextBootstrapStructure structure = this.generatorTester
				.generate(this.contextRunner.withUserConfiguration(SimpleConfiguration.class));
		assertThat(structure).contextBootstrap().contains(
				"context.registerBean(\"simpleConfiguration\", SimpleConfiguration.class, SimpleConfiguration::new);",
				"context.registerBean(\"stringBean\", String.class, () -> context.getBean(SimpleConfiguration.class).stringBean());",
				"context.registerBean(\"integerBean\", Integer.class, () -> context.getBean(SimpleConfiguration.class).integerBean());");
	}

	@Test
	void bootstrapClassWithAutoConfiguration() {
		ContextBootstrapStructure structure = this.generatorTester.generate(
				this.contextRunner.withConfiguration(AutoConfigurations.of(ProjectInfoAutoConfiguration.class)));
		// NOTE: application context runner does not register auto-config as FQNs
		assertThat(structure).contextBootstrap().contains(
				"context.registerBean(\"projectInfoAutoConfiguration\", ProjectInfoAutoConfiguration.class, () -> new ProjectInfoAutoConfiguration(context.getBean(ProjectInfoProperties.class)));",
				"context.registerBean(\"spring.info-org.springframework.boot.autoconfigure.info.ProjectInfoProperties\", ProjectInfoProperties.class, ProjectInfoProperties::new);");
	}

}
