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

import java.util.Collection;

import org.apache.mahout.cf.taste.common.Refreshable;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.eval.DataModelBuilder;
import org.apache.mahout.cf.taste.eval.RecommenderEvaluator;
import org.apache.mahout.cf.taste.impl.common.FastByIDMap;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.apache.mahout.cf.taste.impl.common.LongPrimitiveIterator;
import org.apache.mahout.cf.taste.impl.model.GenericPreference;
import org.apache.mahout.cf.taste.impl.model.GenericUserPreferenceArray;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.model.Preference;
import org.apache.mahout.cf.taste.model.PreferenceArray;

/**
 * <p>
 * This class is a {@link DataModel} that acts as a proxy for another data
 * model. It simulates missing preferences for a given user. This class is also
 * its own {@link DataModelBuilder}.
 * </p>
 * <p>
 * The missing items are found by comparing the model given to the {link
 * {@link #buildDataModel(FastByIDMap)} to the delegate model. Once the missing
 * preferences are found, the data from the map is ignored, only the data from
 * the model given to the constructor is used. Thus, if this class is given to
 * an instance of
 * {@link RecommenderEvaluator#evaluate(org.apache.mahout.cf.taste.eval.RecommenderBuilder, DataModelBuilder, DataModel, double, double)
 * RecommenderEvaluator.evaluate(…)} , the <code>evaluationPercentage</code>
 * must be 1.
 * </p>
 */
public class PreferenceMaskerModelBuilder implements DataModelBuilder,
		DataModel {
	private static final long serialVersionUID = 1L;
	private final DataModel dataModel;
	/**
	 * Cache of preferences for items without the masked preferences. This cache
	 * is lazily filled: the arrays are created only if needed.
	 */
	private final FastByIDMap<PreferenceArray> fakePreferencesForItem;
	private PreferenceArray fakeUserPreferenceArray;
	/**
	 * Items that have only one preference: the target user one, and this
	 * preference is masked. These items must be taken in account in the methods
	 * "getItemIDs" and "getNumItems".
	 */
	private FastIDSet maskedItems;
	/**
	 * Preferences to mask. Because the value of the preference is never used,
	 * this field contains only the item id of the preference.
	 */
	private FastIDSet maskedPreferences;
	private final long targetUser;

	public PreferenceMaskerModelBuilder(DataModel dataModel, long targetUserId) {
		this.dataModel = dataModel;
		this.targetUser = targetUserId;
		fakePreferencesForItem = new FastByIDMap<>();
		maskedItems = new FastIDSet();
	}

	@Override
	public DataModel buildDataModel(FastByIDMap<PreferenceArray> trainingData) {
		// reset data from the previous call
		fakeUserPreferenceArray = null;
		maskedPreferences = null;
		fakePreferencesForItem.clear();
		maskedItems.clear();

		// find the missing preferences
		PreferenceArray newPreferences = trainingData.get(targetUser);

		try {
			maskedPreferences = dataModel.getItemIDsFromUser(targetUser)
					.clone();
		} catch (TasteException e) {
			// the taste exception have to be wrapped
			throw new IllegalStateException(e);
		}

		for (long itemId : newPreferences.getIDs()) {
			maskedPreferences.remove(itemId);
		}

		// find the items that have only one preference: the target user one
		for (Long preference : maskedPreferences) {
			try {
				if (dataModel.getNumUsersWithPreferenceFor(preference) == 1) {
					maskedItems.add(preference);
				}
			} catch (TasteException e) {
				// the taste exception have to be wrapped
				throw new IllegalStateException(e);
			}
		}

		return this;
	}

	@Override
	public LongPrimitiveIterator getItemIDs() throws TasteException {
		return new FilteringIterator(dataModel.getItemIDs(), maskedItems);
	}

	@Override
	public FastIDSet getItemIDsFromUser(long userID) throws TasteException {
		PreferenceArray prefs = getPreferencesFromUser(userID);
		int size = prefs.length();
		FastIDSet result = new FastIDSet(size);
		for (int i = 0; i < size; i++) {
			result.add(prefs.getItemID(i));
		}
		return result;
	}

	@Override
	public float getMaxPreference() {
		return dataModel.getMaxPreference();
	}

	@Override
	public float getMinPreference() {
		return dataModel.getMinPreference();
	}

	@Override
	public int getNumItems() throws TasteException {
		return dataModel.getNumItems() - maskedItems.size();
	}

	@Override
	public int getNumUsers() throws TasteException {
		return dataModel.getNumUsers();
	}

	@Override
	public int getNumUsersWithPreferenceFor(long itemID) throws TasteException {
		int result = dataModel.getNumUsersWithPreferenceFor(itemID);

		if (maskedPreferences.contains(itemID)) {
			result--;
		}

		return result;
	}

	@Override
	public int getNumUsersWithPreferenceFor(long itemID1, long itemID2)
			throws TasteException {

		int result = dataModel.getNumUsersWithPreferenceFor(itemID1, itemID2);

		// detect if the target user has been wrongly counted
		if (dataModel.getPreferenceValue(targetUser, itemID1) != null
				&& dataModel.getPreferenceValue(targetUser, itemID2) != null
				&& (maskedPreferences.contains(itemID1) || maskedPreferences
						.contains(itemID2))) {
			result--;
		}

		return result;
	}

	@Override
	public PreferenceArray getPreferencesForItem(long itemID)
			throws TasteException {

		if (!maskedPreferences.contains(itemID)) {
			return dataModel.getPreferencesForItem(itemID);
		}

		PreferenceArray result = fakePreferencesForItem.get(itemID);
		if (result != null) {
			// the array to provides has already been created
			return result;
		}

		// the array to provide must be created
		PreferenceArray realPreferenceArray = dataModel
				.getPreferencesForItem(itemID);
		PreferenceArray newPreferenceArray = new GenericUserPreferenceArray(
				realPreferenceArray.length() - 1);

		int indexFake = 0;
		for (int indexReal = 0; indexReal < realPreferenceArray.length(); indexReal++) {
			Preference realPreference = realPreferenceArray.get(indexReal);

			long currentUser = realPreference.getUserID();
			float currentValue = realPreference.getValue();
			if (currentUser != targetUser) {
				Preference newPreference = new GenericPreference(currentUser,
						itemID, currentValue);
				newPreferenceArray.set(indexFake, newPreference);
				indexFake++;
			}
		}

		// cache the array for later invocations
		fakePreferencesForItem.put(itemID, newPreferenceArray);

		return newPreferenceArray;
	}

	@Override
	public PreferenceArray getPreferencesFromUser(long userID)
			throws TasteException {

		if (userID != targetUser) {
			return dataModel.getPreferencesFromUser(userID);
		}
		if (fakeUserPreferenceArray != null) {
			return fakeUserPreferenceArray;
		}

		PreferenceArray realPreferenceArray = dataModel.getPreferencesFromUser(
				userID).clone();

		fakeUserPreferenceArray = new GenericUserPreferenceArray(
				realPreferenceArray.length() - maskedPreferences.size());
		int indexFake = 0;

		// copy only the existing preferences that are not in the
		// missing preferences
		for (int indexReal = 0; indexReal < realPreferenceArray.length(); indexReal++) {
			Preference realPreference = realPreferenceArray.get(indexReal);

			long currentItem = realPreference.getItemID();
			float currentValue = realPreference.getValue();
			if (!maskedPreferences.contains(currentItem)) {
				Preference newPreference = new GenericPreference(userID,
						currentItem, currentValue);
				fakeUserPreferenceArray.set(indexFake, newPreference);
				indexFake++;
			}
		}

		return fakeUserPreferenceArray;
	}

	@Override
	public Long getPreferenceTime(long userID, long itemID)
			throws TasteException {
		return null;
	}

	@Override
	public Float getPreferenceValue(long userID, long itemID)
			throws TasteException {
		if (userID == targetUser && maskedPreferences.contains(itemID)) {
			return null;
		}

		return dataModel.getPreferenceValue(userID, itemID);
	}

	@Override
	public LongPrimitiveIterator getUserIDs() throws TasteException {
		return dataModel.getUserIDs();
	}

	@Override
	public boolean hasPreferenceValues() {
		return true;
	}

	@Override
	public void refresh(Collection<Refreshable> alreadyRefreshed) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void removePreference(long userID, long itemID)
			throws TasteException {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setPreference(long userID, long itemID, float value)
			throws TasteException {
		throw new UnsupportedOperationException();
	}
}
