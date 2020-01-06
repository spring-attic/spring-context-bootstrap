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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.util.ClassUtils;

/**
 * A selector that discard {@link BeanDefinition} instances the bootstrap class is
 * replacing.
 *
 * @author Stephane Nicoll
 */
public class DefaultBeanDefinitionSelector implements BeanDefinitionSelector {

	private final List<String> excludeTypes;

	public DefaultBeanDefinitionSelector(List<String> excludeTypes) {
		this.excludeTypes = new ArrayList<>(excludeTypes);
		this.excludeTypes.add("org.springframework.context.annotation.ConfigurationClassPostProcessor");
	}

	@Override
	public Boolean select(BeanDefinition beanDefinition) {
		String target = ClassUtils.getUserClass(beanDefinition.getResolvableType().toClass()).getName();
		return !this.excludeTypes.contains(target);
	}

}
