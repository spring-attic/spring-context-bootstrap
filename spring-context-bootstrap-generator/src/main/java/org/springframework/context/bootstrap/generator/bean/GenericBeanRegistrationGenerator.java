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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.bootstrap.infrastructure.BeanDefinitionCustomizers;
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

	private final BeanDefinition beanDefinition;

	private final BeanValueSupplier beanValueSupplier;

	public GenericBeanRegistrationGenerator(String beanName, BeanDefinition beanDefinition,
			BeanValueSupplier beanValueSupplier) {
		this.beanName = beanName;
		this.beanDefinition = beanDefinition;
		this.beanValueSupplier = beanValueSupplier;
	}

	@Override
	public void generateBeanRegistration(MethodSpec.Builder method) {
		ResolvableType beanType = this.beanDefinition.getResolvableType();
		String beanId = getBeanIdentifier(this.beanName, beanType.toClass());
		String variable = beanId + "BeanDef";
		method.addStatement("$T $L = new RootBeanDefinition()", RootBeanDefinition.class, variable);
		CodeBlock.Builder targetType = CodeBlock.builder();
		targetType.add("$L.setTargetType(", variable);
		TypeHelper.generateResolvableTypeFor(targetType, beanType);
		targetType.add(")");
		method.addStatement(targetType.build());
		CodeBlock.Builder instanceSupplier = CodeBlock.builder();
		instanceSupplier.add("$L.setInstanceSupplier(", variable);
		this.beanValueSupplier.handleValueSupplier(instanceSupplier);
		instanceSupplier.add(")");
		method.addStatement(instanceSupplier.build());
		handleMetadata(method, variable);
		method.addStatement("context.registerBeanDefinition($S, $L)", this.beanName, variable);
	}

	@Override
	public BeanValueSupplier getBeanValueSupplier() {
		return this.beanValueSupplier;
	}

	private void handleMetadata(MethodSpec.Builder method, String variable) {
		if (this.beanDefinition.isPrimary()) {
			method.addStatement(customization(variable, CodeBlock.builder().add("primary()")));
		}
		if (this.beanDefinition.getRole() != BeanDefinition.ROLE_APPLICATION) {
			method.addStatement(
					customization(variable, CodeBlock.builder().add("role($L)", this.beanDefinition.getRole())));
		}
	}

	private CodeBlock customization(String variable, CodeBlock.Builder customizerMethod) {
		return CodeBlock.builder().add("$T.", BeanDefinitionCustomizers.class).add(customizerMethod.build())
				.add(".customize($L)", variable).build();
	}

	// rationalize
	private String getBeanIdentifier(String beanName, Class<?> type) {
		String target = (isValidName(beanName)) ? beanName : type.getSimpleName();
		return StringUtils.uncapitalize(target);
	}

	private static boolean isValidName(String className) {
		return SourceVersion.isIdentifier(className) && !SourceVersion.isKeyword(className);
	}

}
