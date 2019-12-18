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

package org.springframework.context.bootstrap.generator.bean;

import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.function.Function;

import com.squareup.javapoet.CodeBlock;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;
import org.springframework.util.ClassUtils;

/**
 * Base {@link BeanValueSupplier} implementation.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractBeanValueSupplier implements BeanValueSupplier {

	private final BeanDefinition beanDefinition;

	private final Class<?> type;

	public AbstractBeanValueSupplier(BeanDefinition beanDefinition) {
		this.beanDefinition = beanDefinition;
		this.type = ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass());
	}

	@Override
	public final Class<?> getType() {
		return this.type;
	}

	@Override
	public boolean isAccessibleFrom(String packageName) {
		return isPublicClass(this.type) && isPublicClass(getDeclaringType());
	}

	protected boolean isPublicClass(Class<?> target) {
		boolean publicClass = Modifier.isPublic(target.getModifiers());
		if (!publicClass) {
			return false;
		}
		Class<?> declaringClass = target.getDeclaringClass();
		if (declaringClass == null) {
			return true;
		}
		return isPublicClass(declaringClass);
	}

	protected boolean hasCheckedException(Class<?>... exceptionTypes) {
		return Arrays.stream(exceptionTypes).anyMatch((ex) -> !RuntimeException.class.isAssignableFrom(ex));
	}

	protected void handleParameters(CodeBlock.Builder code, Parameter[] parameters,
			Function<Integer, ResolvableType> parameterTypeFactory) {
		for (int i = 0; i < parameters.length; i++) {
			ResolvableType parameterType = parameterTypeFactory.apply(i);
			ValueHolder userValue = this.beanDefinition.getConstructorArgumentValues().getIndexedArgumentValue(i,
					parameterType.toClass());
			if (userValue != null) {
				handleUserValue(code, userValue.getValue(), parameterType);
			}
			else {
				handleDependency(code, parameters[i], parameterType);
			}
			if (i < parameters.length - 1) {
				code.add(", ");
			}
		}
	}

	// workaround to account for the Spring Boot use case for now.
	private void handleUserValue(CodeBlock.Builder code, Object value, ResolvableType parameterType) {
		if (parameterType.isArray()) {
			code.add("new $T { ", parameterType.toClass());
			if (value instanceof char[]) {
				char[] array = (char[]) value;
				for (int i = 0; i < array.length; i++) {
					handleUserValue(code, array[i], ResolvableType.forClass(char.class));
					if (i < array.length - 1) {
						code.add(", ");
					}
				}
			}
			else if (value instanceof String[]) {
				String[] array = (String[]) value;
				for (int i = 0; i < array.length; i++) {
					handleUserValue(code, array[i], ResolvableType.forClass(String.class));
					if (i < array.length - 1) {
						code.add(", ");
					}
				}
			}
			code.add(" }");
		}
		else if (value instanceof Character) {
			code.add("'$L'", value);
		}
		else if (isPrimitiveOrWrapper(value)) {
			code.add("$L", value);
		}
		else if (value instanceof String) {
			code.add("$S", value);
		}
	}

	private boolean isPrimitiveOrWrapper(Object value) {
		Class<?> valueType = value.getClass();
		return (valueType.isPrimitive() || valueType == Double.class || valueType == Float.class
				|| valueType == Long.class || valueType == Integer.class || valueType == Short.class
				|| valueType == Character.class || valueType == Byte.class || valueType == Boolean.class);
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
