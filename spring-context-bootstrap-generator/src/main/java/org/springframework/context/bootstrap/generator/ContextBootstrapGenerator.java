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

package org.springframework.context.bootstrap.generator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.bootstrap.generator.bean.BeanRegistrationGenerator;
import org.springframework.context.bootstrap.generator.bean.BeanValueSupplier;
import org.springframework.context.bootstrap.generator.bean.ConstructorBeanValueSupplier;
import org.springframework.context.bootstrap.generator.bean.GenericBeanRegistrationGenerator;
import org.springframework.context.bootstrap.generator.bean.MethodBeanValueSupplier;
import org.springframework.context.bootstrap.generator.bean.SimpleBeanRegistrationGenerator;
import org.springframework.context.bootstrap.generator.processor.event.EventListenerProcessor;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.util.ReflectionUtils;

/**
 * A simple experiment to generate a bootstrap class that represents the state of a fully
 * initialized {@link BeanFactory}.
 *
 * @author Stephane Nicoll
 */
public class ContextBootstrapGenerator {

	private static final Log logger = LogFactory.getLog(ContextBootstrapGenerator.class);

	private final Map<String, ProtectedBootstrapClass> protectedBootstrapClasses = new HashMap<>();

	/**
	 * Generate the code that is required to restore the state of the specified
	 * {@link BeanFactory}.
	 * @param beanFactory the bean factory state to replicate in code
	 * @param packageName the root package for the main {@code ContextBoostrap} class
	 * @param excludeTypes the types to exclude
	 * @return a list of {@linkplain JavaFile java source files}
	 */
	public List<JavaFile> generateBootstrapClass(ConfigurableListableBeanFactory beanFactory, String packageName,
			Class<?>... excludeTypes) {
		DefaultBeanDefinitionSelector selector = new DefaultBeanDefinitionSelector(
				Arrays.stream(excludeTypes).map(Class::getName).collect(Collectors.toList()));
		List<JavaFile> bootstrapClasses = new ArrayList<>();
		bootstrapClasses.add(createClass(packageName, "ContextBootstrap",
				generateBootstrapMethod(beanFactory, packageName, selector)));
		for (ProtectedBootstrapClass protectedBootstrapClass : this.protectedBootstrapClasses.values()) {
			bootstrapClasses.add(protectedBootstrapClass.build());
		}
		return bootstrapClasses;
	}

	public JavaFile createClass(String packageName, String bootstrapClassName, MethodSpec bootstrapMethod) {
		return JavaFile.builder(packageName, TypeSpec.classBuilder(bootstrapClassName).addModifiers(Modifier.PUBLIC)
				.addMethod(bootstrapMethod).build()).build();
	}

	public MethodSpec generateBootstrapMethod(ConfigurableListableBeanFactory beanFactory, String packageName,
			BeanDefinitionSelector selector) {
		MethodSpec.Builder method = MethodSpec.methodBuilder("bootstrap").addModifiers(Modifier.PUBLIC)
				.addParameter(GenericApplicationContext.class, "context");
		String[] beanNames = beanFactory.getBeanDefinitionNames();
		for (String beanName : beanNames) {
			BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
			if (selector.select(beanName, beanDefinition)) {
				BeanRegistrationGenerator beanRegistrationGenerator = getBeanRegistrationGenerator(beanName,
						beanDefinition);
				if (beanRegistrationGenerator != null) {
					BeanValueSupplier beanValueSupplier = beanRegistrationGenerator.getBeanValueSupplier();
					if (beanValueSupplier.isAccessibleFrom(packageName)) {
						beanRegistrationGenerator.generateBeanRegistration(method);
					}
					else {
						String protectedPackageName = beanValueSupplier.getDeclaringType().getPackage().getName();
						ProtectedBootstrapClass protectedBootstrapClass = this.protectedBootstrapClasses
								.computeIfAbsent(protectedPackageName, ProtectedBootstrapClass::new);
						protectedBootstrapClass.addBeanRegistrationMethod(beanName, beanValueSupplier.getType(),
								beanRegistrationGenerator);
						CodeBlock.Builder code = CodeBlock.builder();
						ClassName protectedClassName = ClassName.get(protectedPackageName, "ContextBootstrap");
						code.add("$T.$L(context)", protectedClassName,
								ProtectedBootstrapClass.registerBeanMethodName(beanName, beanValueSupplier.getType()));
						method.addStatement(code.build());
					}
				}
			}
		}
		// Event listeners
		new EventListenerProcessor(beanFactory).registerEventListeners(method);
		return method.build();
	}

	private BeanRegistrationGenerator getBeanRegistrationGenerator(String beanName, BeanDefinition beanDefinition) {
		ResolvableType beanType = beanDefinition.getResolvableType();
		BeanValueSupplier beanValueSupplier = getBeanValueSupplier(beanDefinition);
		if (beanValueSupplier != null) {
			if (beanType.hasGenerics()) {
				return new GenericBeanRegistrationGenerator(beanName, beanDefinition, beanValueSupplier);
			}
			else {
				return new SimpleBeanRegistrationGenerator(beanName, beanDefinition, beanValueSupplier);
			}
		}
		return null;
	}

	private BeanValueSupplier getBeanValueSupplier(BeanDefinition beanDefinition) {
		// Remove CGLIB classes
		if (beanDefinition instanceof RootBeanDefinition) {
			Executable factoryExecutable = getField(beanDefinition, "resolvedConstructorOrFactoryMethod",
					Executable.class);
			if (factoryExecutable instanceof Method) {
				return new MethodBeanValueSupplier(beanDefinition, (Method) factoryExecutable);
			}
			else if (factoryExecutable instanceof Constructor) {
				return new ConstructorBeanValueSupplier(beanDefinition, (Constructor<?>) factoryExecutable);
			}
		}
		logger.error("Failed to handle bean with definition " + beanDefinition);
		return null;
	}

	private <T> T getField(BeanDefinition beanDefinition, String fieldName, Class<T> targetType) {
		Field field = ReflectionUtils.findField(RootBeanDefinition.class, fieldName);
		ReflectionUtils.makeAccessible(field);
		return targetType.cast(ReflectionUtils.getField(field, beanDefinition));
	}

}
