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

import org.springframework.core.ResolvableType;

/**
 * Shared generation helper.
 *
 * @author Stephane Nicoll
 */
abstract class TypeHelper {

	static void generateResolvableTypeFor(CodeBlock.Builder code, ResolvableType target) {
		if (!target.hasGenerics()) {
			code.add("$T.class", target.toClass());
		}
		else {
			code.add("$T.forClassWithGenerics($T.class, ", ResolvableType.class, target.toClass());
			for (int i = 0; i < target.getGenerics().length; i++) {
				ResolvableType parameter = target.getGeneric(i);
				generateResolvableTypeFor(code, parameter);
				if (i < target.getGenerics().length - 1) {
					code.add(", ");
				}
			}
			code.add(")");
		}
	}

}
