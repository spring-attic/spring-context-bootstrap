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

import java.util.function.Supplier;

/**
 * A helper class for bean value suppliers that throw checked exceptions.
 *
 * @author Stephane Nicoll
 */
public final class ExceptionHandler {

	private ExceptionHandler() {
	}

	public static <T> Supplier<T> wrapException(SmartSupplier<T> delegate) {
		return () -> {
			try {
				return delegate.get();
			}
			catch (Exception ex) {
				throw new RuntimeException(ex.getMessage(), ex);
			}
		};
	}

	public interface SmartSupplier<T> {

		T get() throws Exception;

	}

}
