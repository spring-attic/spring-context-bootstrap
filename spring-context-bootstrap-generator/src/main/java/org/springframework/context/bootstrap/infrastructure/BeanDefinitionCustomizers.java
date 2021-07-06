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

package org.springframework.context.bootstrap.infrastructure;

import org.springframework.beans.factory.config.BeanDefinitionCustomizer;
import org.springframework.beans.factory.support.AbstractBeanDefinition;

/**
 * Provide {@link BeanDefinitionCustomizer} implementations for common use-cases.
 *
 * @author Stephane Nicoll
 */
public abstract class BeanDefinitionCustomizers {

	public static BeanDefinitionCustomizer primary() {
		return (beanDefinition) -> beanDefinition.setPrimary(true);
	}

	public static BeanDefinitionCustomizer synthetic() {
		return (beanDefinition) -> {
			if (beanDefinition instanceof AbstractBeanDefinition) {
				((AbstractBeanDefinition) beanDefinition).setSynthetic(true);
			}
		};
	}

	public static BeanDefinitionCustomizer role(int role) {
		return (beanDefinition) -> beanDefinition.setRole(role);
	}

}
