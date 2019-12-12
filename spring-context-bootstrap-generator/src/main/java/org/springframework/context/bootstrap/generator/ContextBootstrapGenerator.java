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
import java.lang.reflect.Parameter;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.CodeBlock.Builder;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
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

	private final ConfigurableListableBeanFactory beanFactory;

	private final BeanDefinitionSelector selector;

	public ContextBootstrapGenerator(ConfigurableListableBeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.selector = new DefaultBeanDefinitionSelector();
	}

	public JavaFile generateBootstrapClass(String packageName) {
		return createClass(packageName, "ContextBootstrap", generateBootstrapMethod());
	}

	public JavaFile createClass(String packageName, String bootstrapClassName, MethodSpec bootstrapMethod) {
		return JavaFile.builder(packageName, TypeSpec.classBuilder(bootstrapClassName).addModifiers(Modifier.PUBLIC)
				.addMethod(bootstrapMethod).build()).build();
	}

	public MethodSpec generateBootstrapMethod() {
		MethodSpec.Builder method = MethodSpec.methodBuilder("bootstrap").addModifiers(Modifier.PUBLIC)
				.addParameter(GenericApplicationContext.class, "context");
		String[] beanNames = this.beanFactory.getBeanDefinitionNames();
		for (String beanName : beanNames) {
			handleBeanDefinition(method, beanName, this.beanFactory.getMergedBeanDefinition(beanName));
		}
		return method.build();
	}

	private void handleBeanDefinition(MethodSpec.Builder method, String beanName, BeanDefinition beanDefinition) {
		if (!this.selector.select(beanDefinition)) {
			return;
		}
		// Remove CGLIB classes
		Class<?> type = ClassUtils.getUserClass(beanDefinition.getResolvableType().getRawClass());
		CodeBlock.Builder code = CodeBlock.builder();
		code.add("context.registerBean($S, $T.class, ", beanName, type);
		boolean handled = handleBeanValueSupplier(code, beanDefinition, type);
		code.add(")"); // End of registerBean
		if (handled) {
			method.addStatement(code.build());
		}
	}

	private boolean handleBeanValueSupplier(Builder code, BeanDefinition beanDefinition, Class<?> type) {
		if (beanDefinition instanceof RootBeanDefinition) {
			Field field = ReflectionUtils.findField(RootBeanDefinition.class, "resolvedConstructorOrFactoryMethod");
			ReflectionUtils.makeAccessible(field);
			Object factoryExecutable = ReflectionUtils.getField(field, beanDefinition);
			if (factoryExecutable instanceof Method) {
				handleBeanFactoryMethodSupplier(code, (Method) factoryExecutable);
			}
			else if (factoryExecutable instanceof Constructor) {
				handleBeanConstructorSupplier(code, (Constructor<?>) factoryExecutable, type);
			}
			// TODO: handle FactoryBean
			else {
				logger.error("Failed to handle bean with definition " + beanDefinition);
				return false;
			}
		}
		else {
			code.add("$T::new)", type);
		}
		return true;
	}

	private void handleBeanFactoryMethodSupplier(CodeBlock.Builder code, Method factoryMethod) {
		code.add("() -> ");
		if (java.lang.reflect.Modifier.isStatic(factoryMethod.getModifiers())) {
			code.add("$T", factoryMethod.getDeclaringClass());
		}
		else {
			code.add("context.getBean($T.class)", factoryMethod.getDeclaringClass());
		}
		code.add(".$L(", factoryMethod.getName());
		handleParameters(code, factoryMethod.getParameters(),
				(i) -> ResolvableType.forMethodParameter(factoryMethod, i));
		code.add(")");
	}

	private void handleBeanConstructorSupplier(CodeBlock.Builder code, Constructor<?> constructor, Class<?> type) {
		Parameter[] parameters = constructor.getParameters();
		if (parameters.length == 0) {
			code.add("$T::new", type);
		}
		else {
			code.add("() -> new $T(", constructor.getDeclaringClass());
			handleParameters(code, parameters, (i) -> ResolvableType.forConstructorParameter(constructor, i));
			code.add(")"); // End of constructor
		}
	}

	private void handleParameters(CodeBlock.Builder code, Parameter[] parameters,
			Function<Integer, ResolvableType> parameterTypeFactory) {
		for (int i = 0; i < parameters.length; i++) {
			handleDependency(code, parameters[i], parameterTypeFactory.apply(i));
			if (i < parameters.length - 1) {
				code.add(", ");
			}
		}
	}

	private void handleDependency(CodeBlock.Builder code, Parameter parameter, ResolvableType parameterType) {
		Class<?> resolvedClass = parameterType.toClass();
		if (resolvedClass.isAssignableFrom(ObjectProvider.class)) {
			code.add("context.getBeanProvider($T.class)",
					parameterType.as(ObjectProvider.class).getGeneric(0).getRawClass());
		}
		else if (resolvedClass.isAssignableFrom(GenericApplicationContext.class)) {
			code.add("context");
		}
		else if (resolvedClass.isAssignableFrom(BeanFactory.class)) {
			code.add("context.getBeanFactory()");
		}
		else if (resolvedClass.isAssignableFrom(Environment.class)) {
			code.add("context.getEnvironment()");
		}
		else {
			code.add("context.getBean($T.class)", resolvedClass);
		}
	}

}
