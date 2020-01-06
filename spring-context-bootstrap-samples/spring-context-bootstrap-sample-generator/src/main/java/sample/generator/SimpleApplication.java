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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

import com.squareup.javapoet.JavaFile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.boostrap.invoker.BootstrapApplication;
import org.springframework.context.bootstrap.generator.ContextBootstrapGenerator;

@SpringBootApplication
public class SimpleApplication {

	public static void main(String[] args) throws IOException {
		startRegularApp(args);
	}

	private static void startRegularApp(String[] args) throws IOException {
		ConfigurableApplicationContext context = SpringApplication.run(SimpleApplication.class, args);
		List<JavaFile> javaFiles = new ContextBootstrapGenerator().generateBootstrapClass(context.getBeanFactory(),
				"sample.generator", SimpleApplication.class);
		// In IntelliJ IDEA, make sure that "working directory" is set to $MODULE_DIR$
		Path srcDirectory = FileSystems.getDefault().getPath(".").resolve("src/main/java");
		for (JavaFile javaFile : javaFiles) {
			javaFile.writeTo(srcDirectory);
		}
	}

	private static void startWithBootstrap(String[] args) {
		BootstrapApplication.forNonWebApplication((context) -> {
			// new ContextBootstrap().bootstrap(context);
		}).run(args);
	}

}
