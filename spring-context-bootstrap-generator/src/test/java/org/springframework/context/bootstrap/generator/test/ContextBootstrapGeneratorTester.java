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

package org.springframework.context.bootstrap.generator.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.squareup.javapoet.JavaFile;

import org.springframework.boot.test.context.runner.AbstractApplicationContextRunner;
import org.springframework.context.bootstrap.generator.ContextBootstrapGenerator;

/**
 * A tester for {@link ContextBootstrapGenerator}.
 *
 * @author Stephane Nicoll
 */
public class ContextBootstrapGeneratorTester {

	private final Path directory;

	private final String packageName;

	public ContextBootstrapGeneratorTester(Path directory, String packageName) {
		this.directory = directory;
		this.packageName = packageName;
	}

	public ContextBootstrapGeneratorTester(Path directory) {
		this(directory, "com.example");
	}

	public ContextBootstrapStructure generate(AbstractApplicationContextRunner<?, ?, ?> runner) {
		Path srcDirectory = generateSrcDirectory();
		runner.run((context) -> {
			JavaFile javaFile = new ContextBootstrapGenerator(context.getSourceApplicationContext().getBeanFactory())
					.generateBootstrapClass(this.packageName);
			writeSource(srcDirectory, javaFile);
		});
		return new ContextBootstrapStructure(srcDirectory, this.packageName);
	}

	private Path generateSrcDirectory() {
		try {
			return Files.createTempDirectory(this.directory, "bootstrap-");
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to create generate source structure ", ex);
		}
	}

	private void writeSource(Path srcDirectory, JavaFile javaFile) {
		try {
			javaFile.writeTo(srcDirectory);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to write source code to disk ", ex);
		}
	}

}
