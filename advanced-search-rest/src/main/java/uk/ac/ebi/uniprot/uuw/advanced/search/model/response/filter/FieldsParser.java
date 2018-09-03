package uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;

import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.FieldMaps;

public class FieldsParser {
	private static final String COLON = ":";
	private static final String ALL = "all";
	private static final String XREF = "xref";
	private static final String FEATURE = "feature";
	private static final String COMMENT = "comment";
	private static final String COMMA = ",";

	private static final String DR = "dr";
	private static final String FT = "ft";
	private static final String CC = "cc";
	private static Set<String> DEFAULTFILTERS =new HashSet<>();
	static {
		DEFAULTFILTERS.add("accession");
		DEFAULTFILTERS.add("id");
		DEFAULTFILTERS.add("protein_name");
		DEFAULTFILTERS.add("gene");
		DEFAULTFILTERS.add("organism");
		DEFAULTFILTERS.add("length");
		DEFAULTFILTERS.add("mass");
		DEFAULTFILTERS.add("score");
	}
	
	public static boolean isDefaultFilters(Map<String, List<String>> filters) {
		boolean notDefault= filters.keySet().stream()
		.anyMatch(val ->!DEFAULTFILTERS.contains(val) );
		
		return !notDefault;
	}

	public static Map<String, List<String>> parse(String fields) {
		if (Strings.isNullOrEmpty(fields)) {
			return Collections.emptyMap();
		}
		Map<String, List<String>> filters = new HashMap<>();
		String tokens[] = fields.split(COMMA);
		for (String token : tokens) {
			String field = FieldMaps.INSTANCE.getField(token);
			if (field.startsWith(CC) || field.startsWith(COMMENT)) {
				addTypedField(filters, COMMENT, CC, field);

			} else if (field.startsWith(FEATURE) || field.startsWith(FT)) {
				addTypedField(filters, FEATURE, FT, field);
			} else if (field.startsWith(XREF) || field.startsWith(DR)) {
				addTypedField(filters, XREF, DR, field);
			} else {
				filters.put(field, Collections.emptyList());
			}
		}
		return filters;

	}

	private static void addTypedField(Map<String, List<String>> filters, String type, String abbr, String token) {
		if (token.equals(type)) {
			putMap(filters, token, ALL);
		} else if (token.startsWith(abbr + COLON)) {
			String value = token.substring(token.indexOf(COLON) + 1);
			putMap(filters, type, value);
		} else if (token.startsWith(type + COLON)) {
			String value = token.substring(token.indexOf(COLON) + 1);
			putMap(filters, type, value);

		} else {
			filters.put(token, Collections.emptyList());
		}
	}

	private static void putMap(Map<String, List<String>> filters, String key, String value) {
		List<String> values = filters.get(key);
		if (values == null) {
			values = new ArrayList<>();
			filters.put(key, values);
		}
		values.add(value);
	}
}
