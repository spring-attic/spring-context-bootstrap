/*
 * Copyright 2012-2021 the original author or authors.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.annotation.Order;
import org.springframework.util.ReflectionUtils;

/**
 * Default {@link BeanValueWriterSupplier} implementation, providing an instance based on
 * the actual class and resolved factory method.
 *
 * @author Stephane Nicoll
 */
@Order(Ordered.LOWEST_PRECEDENCE - 5)
public class DefaultBeanValueWriterSupplier implements BeanValueWriterSupplier {

	private static final Log logger = LogFactory.getLog(DefaultBeanValueWriterSupplier.class);

	@Override
	public BeanValueWriter get(BeanDefinition beanDefinition) {
		// Remove CGLIB classes
		Executable factoryExecutable = resolveBeanFactory(beanDefinition);
		if (factoryExecutable instanceof Method) {
			return new MethodBeanValueWriter(beanDefinition, (Method) factoryExecutable);
		}
		else if (factoryExecutable instanceof Constructor) {
			return new ConstructorBeanValueWriter(beanDefinition, (Constructor<?>) factoryExecutable);
		}
		return null;
	}

	private Executable resolveBeanFactory(BeanDefinition beanDefinition) {
		if (beanDefinition instanceof RootBeanDefinition) {
			RootBeanDefinition rootBeanDefinition = (RootBeanDefinition) beanDefinition;
			Method resolvedFactoryMethod = rootBeanDefinition.getResolvedFactoryMethod();
			if (resolvedFactoryMethod != null) {
				return resolvedFactoryMethod;
			}
			Executable resolvedConstructor = resolveConstructor(rootBeanDefinition);
			if (resolvedConstructor != null) {
				return resolvedConstructor;
			}
			Executable resolvedConstructorOrFactoryMethod = getField(beanDefinition,
					"resolvedConstructorOrFactoryMethod", Executable.class);
			if (resolvedConstructorOrFactoryMethod != null) {
				logger.error("resolvedConstructorOrFactoryMethod required for " + beanDefinition);
				return resolvedConstructorOrFactoryMethod;
			}
		}
		return null;
	}

	private Executable resolveConstructor(RootBeanDefinition beanDefinition) {
		Class<?> type = beanDefinition.getBeanClass();
		Constructor<?>[] constructors = type.getDeclaredConstructors();
		if (constructors.length == 1) {
			return constructors[0];
		}
		for (Constructor<?> constructor : constructors) {
			if (MergedAnnotations.from(constructor).isPresent(Autowired.class)) {
				return constructor;
			}
		}
		return null;
	}

	private <T> T getField(BeanDefinition beanDefinition, String fieldName, Class<T> targetType) {
		Field field = ReflectionUtils.findField(RootBeanDefinition.class, fieldName);
		ReflectionUtils.makeAccessible(field);
		return targetType.cast(ReflectionUtils.getField(field, beanDefinition));
	}

}
