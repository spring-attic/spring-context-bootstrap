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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.springframework.context.bootstrap.generator.value.BeanValueSupplier;
import org.springframework.context.bootstrap.generator.value.ConstructorBeanValueSupplier;
import org.springframework.context.bootstrap.generator.value.MethodBeanValueSupplier;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * A simple experiment to generate a bootstrap class that represents the state of a fully
 * initialized {@link BeanFactory}.
 *
 * @author Stephane Nicoll
 */
public class ContextBootstrapGenerator {

	private static final Log logger = LogFactory.getLog(ContextBootstrapGenerator.class);

	private final BeanDefinitionSelector selector;

	private final Map<String, ProtectedBootstrapClass> protectedBootstrapClasses = new HashMap<>();

	public ContextBootstrapGenerator() {
		this.selector = new DefaultBeanDefinitionSelector();
	}

	public List<JavaFile> generateBootstrapClass(ConfigurableListableBeanFactory beanFactory, String packageName) {
		List<JavaFile> bootstrapClasses = new ArrayList<>();
		bootstrapClasses
				.add(createClass(packageName, "ContextBootstrap", generateBootstrapMethod(beanFactory, packageName)));
		for (ProtectedBootstrapClass protectedBootstrapClass : this.protectedBootstrapClasses.values()) {
			bootstrapClasses.add(protectedBootstrapClass.build());
		}
		return bootstrapClasses;
	}

	public JavaFile createClass(String packageName, String bootstrapClassName, MethodSpec bootstrapMethod) {
		return JavaFile.builder(packageName, TypeSpec.classBuilder(bootstrapClassName).addModifiers(Modifier.PUBLIC)
				.addMethod(bootstrapMethod).build()).build();
	}

	public MethodSpec generateBootstrapMethod(ConfigurableListableBeanFactory beanFactory, String packageName) {
		MethodSpec.Builder method = MethodSpec.methodBuilder("bootstrap").addModifiers(Modifier.PUBLIC)
				.addParameter(GenericApplicationContext.class, "context");
		String[] beanNames = beanFactory.getBeanDefinitionNames();
		for (String beanName : beanNames) {
			BeanDefinition beanDefinition = beanFactory.getMergedBeanDefinition(beanName);
			if (this.selector.select(beanDefinition)) {
				BeanValueSupplier beanValueSupplier = getBeanValueSupplier(beanDefinition);
				if (beanValueSupplier != null) {
					CodeBlock registrationStatement = generateBeanRegistrationStatement(beanName, beanValueSupplier);
					if (beanValueSupplier.isAccessibleFrom(packageName)) {
						method.addStatement(registrationStatement);
					}
					else {
						String protectedPackageName = beanValueSupplier.getDeclaringType().getPackage().getName();
						ProtectedBootstrapClass protectedBootstrapClass = this.protectedBootstrapClasses
								.computeIfAbsent(protectedPackageName, ProtectedBootstrapClass::new);
						protectedBootstrapClass.addBeanRegistrationMethod(beanName, beanValueSupplier.getType(),
								registrationStatement);
						CodeBlock.Builder code = CodeBlock.builder();
						ClassName protectedClassName = ClassName.get(protectedPackageName, "ContextBootstrap");
						code.add("$T.$L(context)", protectedClassName,
								ProtectedBootstrapClass.registerBeanMethodName(beanName, beanValueSupplier.getType()));
						method.addStatement(code.build());
					}
				}
			}
		}
		return method.build();
	}

	private BeanValueSupplier getBeanValueSupplier(BeanDefinition beanDefinition) {
		// Remove CGLIB classes
		Class<?> type = ClassUtils.getUserClass(beanDefinition.getResolvableType().getRawClass());
		if (beanDefinition instanceof RootBeanDefinition) {
			Field field = ReflectionUtils.findField(RootBeanDefinition.class, "resolvedConstructorOrFactoryMethod");
			ReflectionUtils.makeAccessible(field);
			Object factoryExecutable = ReflectionUtils.getField(field, beanDefinition);
			if (factoryExecutable instanceof Method) {
				return new MethodBeanValueSupplier(type, (Method) factoryExecutable);
			}
			else if (factoryExecutable instanceof Constructor) {
				return new ConstructorBeanValueSupplier(type, (Constructor<?>) factoryExecutable);
			}
			// TODO: handle FactoryBean
		}
		logger.error("Failed to handle bean with definition " + beanDefinition);
		return null;

	}

	public CodeBlock generateBeanRegistrationStatement(String beanName, BeanValueSupplier beanValueSupplier) {
		CodeBlock.Builder code = CodeBlock.builder();
		code.add("context.registerBean($S, $T.class, ", beanName, beanValueSupplier.getType());
		beanValueSupplier.handleValueSupplier(code);
		code.add(")"); // End of registerBean
		return code.build();
	}

}
