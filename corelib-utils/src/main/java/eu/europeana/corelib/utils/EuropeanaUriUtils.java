/*
 * Copyright 2007-2012 The Europeana Foundation
 *
 *  Licenced under the EUPL, Version 1.1 (the "Licence") and subsequent versions as approved
 *  by the European Commission;
 *  You may not use this work except in compliance with the Licence.
 * 
 *  You may obtain a copy of the Licence at:
 *  http://joinup.ec.europa.eu/software/page/eupl
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under
 *  the Licence is distributed on an "AS IS" basis, without warranties or conditions of
 *  any kind, either express or implied.
 *  See the Licence for the specific language governing permissions and limitations under
 *  the Licence.
 */

package eu.europeana.corelib.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * EuropeanaID creator class
 * 
 * @author yorgos.mamakis@ kb.nl
 * 
 */

public class EuropeanaUriUtils {

	private final static String REPLACEMENT = "_";

	private EuropeanaUriUtils() {

	}

	/**
	 * Create the EuropeanaID from the collection ID and record ID
	 * 
	 * @param collectionId
	 *            The collection ID
	 * @param recordId
	 *            The record ID (unique local identifier of a collection record)
	 * @return The Europeana compatible ID
	 */

	public static String createSanitizedEuropeanaId(String collectionId, String recordId) {
		return "/" + sanitizeCollectionId(collectionId) + "/" + sanitizeRecordId(recordId);
	}

	public static String createEuropeanaId(String collectionId, String recordId){
		return "/" + collectionId + "/" + recordId;
	}
	
	private static String sanitizeRecordId(String recordId) {

		recordId = StringUtils.startsWith(recordId, "http://") ? StringUtils
				.substringAfter(
						StringUtils.substringAfter(recordId, "http://"), "/")
				: recordId;
		Pattern pattern = Pattern.compile("[^a-zA-Z0-9_]");
		Matcher matcher = pattern.matcher(recordId);
		recordId = matcher.replaceAll(REPLACEMENT);
		return recordId;
	}

	private static String sanitizeCollectionId(String collectionId) {
		Pattern pattern = Pattern.compile("[a-zA-Z]");
		Matcher matcher = pattern.matcher(collectionId.substring(
				collectionId.length() - 1, collectionId.length()));
		return matcher.find() ? StringUtils.substring(collectionId, 0,
				collectionId.length() - 1) : collectionId;
	}

	public static String encode(String value) {
		if (StringUtils.isNotBlank(value)) {
			try {
				value = URLEncoder.encode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
		}
		return value;
	}

	public static String decode(String value) {
		if (StringUtils.isNotBlank(value)) {
			try {
				value = URLDecoder.decode(value, "UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
		}
		return value;
	}
	
	public static boolean isUri(String str) {
	    return StringUtils.startsWith(str, "http://")
	           || StringUtils.startsWith(str, "https://")
	           || StringUtils.startsWith(str, "urn:")
	           || StringUtils.startsWith(str, "#");
	}
}
