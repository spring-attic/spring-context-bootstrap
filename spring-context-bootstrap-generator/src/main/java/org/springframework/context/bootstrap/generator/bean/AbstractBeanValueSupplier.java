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
import java.util.function.Function;

import com.squareup.javapoet.CodeBlock;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.Environment;

/**
 * Base {@link BeanValueSupplier} implementation.
 *
 * @author Stephane Nicoll
 */
public abstract class AbstractBeanValueSupplier implements BeanValueSupplier {

	private final Class<?> type;

	public AbstractBeanValueSupplier(Class<?> type) {
		this.type = type;
	}

	@Override
	public final Class<?> getType() {
		return this.type;
	}

	@Override
	public boolean isAccessibleFrom(String packageName) {
		return Modifier.isPublic(this.type.getModifiers());
	}

	protected void handleParameters(CodeBlock.Builder code, Parameter[] parameters,
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
