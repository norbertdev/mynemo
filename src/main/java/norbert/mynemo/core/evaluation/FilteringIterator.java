/*
 * Copyright 2015 Norbert
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package norbert.mynemo.core.evaluation;

import java.util.NoSuchElementException;

import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;

public class FilteringIterator implements LongPrimitiveIterator {

	private final LongPrimitiveIterator delegate;
	private boolean hasNext;
	private final FastIDSet maskedId;
	private long next;

	public FilteringIterator(LongPrimitiveIterator delegate, FastIDSet maskedId) {
		this.delegate = delegate;
		this.maskedId = maskedId;
		nextMatch();
	}

	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public Long next() {
		return nextLong();
	}

	@Override
	public long nextLong() {
		if (!hasNext) {
			throw new NoSuchElementException();
		}

		return nextMatch();
	}

	private long nextMatch() {
		long oldMatch = next;

		while (delegate.hasNext()) {
			long o = delegate.next();

			if (!maskedId.contains(o)) {
				hasNext = true;
				next = o;

				return oldMatch;
			}
		}

		hasNext = false;

		return oldMatch;
	}

	@Override
	public long peek() {
		if (!hasNext) {
			throw new NoSuchElementException();
		}

		return next;
	}

	/**
	 * Throws an UnsupportedOperationException.
	 */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void skip(int n) {
		delegate.skip(n);
	}
}
