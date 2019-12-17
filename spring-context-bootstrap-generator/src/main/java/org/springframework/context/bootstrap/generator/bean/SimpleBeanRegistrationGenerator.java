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

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec.Builder;

import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

/**
 * A {@link BeanRegistrationGenerator} implementation that uses {@code registerBean}.
 *
 * @author Stephane Nicoll
 */
public class SimpleBeanRegistrationGenerator implements BeanRegistrationGenerator {

	private final String beanName;

	private final ResolvableType beanType;

	private final BeanValueSupplier beanValueSupplier;

	public SimpleBeanRegistrationGenerator(String beanName, ResolvableType beanType,
			BeanValueSupplier beanValueSupplier) {
		this.beanName = beanName;
		this.beanType = beanType;
		this.beanValueSupplier = beanValueSupplier;
	}

	@Override
	public void generateBeanRegistration(Builder method) {
		CodeBlock.Builder code = CodeBlock.builder();
		code.add("context.registerBean($S, $T.class, ", this.beanName,
				ClassUtils.getUserClass(this.beanType.toClass()));
		this.beanValueSupplier.handleValueSupplier(code);
		code.add(")"); // End of registerBean
		method.addStatement(code.build());
	}

	@Override
	public BeanValueSupplier getBeanValueSupplier() {
		return this.beanValueSupplier;
	}

}
