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

import javax.lang.model.SourceVersion;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;

import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.ResolvableType;
import org.springframework.util.StringUtils;

/**
 * A {@link BeanRegistrationGenerator} implementation that creates a
 * {@link RootBeanDefinition} as there is no easier way to register a bean with generics.
 *
 * @author Stephane Nicoll
 */
public class GenericBeanRegistrationGenerator implements BeanRegistrationGenerator {

	private final String beanName;

	private final ResolvableType beanType;

	private final BeanValueSupplier beanValueSupplier;

	public GenericBeanRegistrationGenerator(String beanName, ResolvableType beanType,
			BeanValueSupplier beanValueSupplier) {
		this.beanName = beanName;
		this.beanType = beanType;
		this.beanValueSupplier = beanValueSupplier;
	}

	@Override
	public void generateBeanRegistration(MethodSpec.Builder method) {
		String beanId = getBeanIdentifier(this.beanName, this.beanType.toClass());
		String variable = beanId + "BeanDef";
		method.addStatement("$T $L = new RootBeanDefinition()", RootBeanDefinition.class, variable);
		CodeBlock.Builder targetType = CodeBlock.builder();
		targetType.add("$L.setTargetType(", variable);
		handleGenericType(targetType, this.beanType);
		targetType.add(")");
		method.addStatement(targetType.build());
		CodeBlock.Builder instanceSupplier = CodeBlock.builder();
		instanceSupplier.add("$L.setInstanceSupplier(", variable);
		this.beanValueSupplier.handleValueSupplier(instanceSupplier);
		instanceSupplier.add(")");
		method.addStatement(instanceSupplier.build());
		method.addStatement("context.registerBeanDefinition($S, $L)", this.beanName, variable);
	}

	@Override
	public BeanValueSupplier getBeanValueSupplier() {
		return this.beanValueSupplier;
	}

	private void handleGenericType(CodeBlock.Builder code, ResolvableType target) {
		code.add("$T.forClassWithGenerics($T.class, ", ResolvableType.class, target.toClass());
		for (int i = 0; i < target.getGenerics().length; i++) {
			ResolvableType parameter = target.getGeneric(i);
			if (parameter.hasGenerics()) {
				handleGenericType(code, parameter);
			}
			else {
				code.add("$T.class", parameter.toClass());
			}
			if (i < target.getGenerics().length - 1) {
				code.add(", ");
			}
		}
		code.add(")");
	}

	// rationalize
	private String getBeanIdentifier(String beanName, Class<?> type) {
		String target = (SourceVersion.isName(beanName)) ? beanName : type.getSimpleName();
		return StringUtils.uncapitalize(target);
	}

}
