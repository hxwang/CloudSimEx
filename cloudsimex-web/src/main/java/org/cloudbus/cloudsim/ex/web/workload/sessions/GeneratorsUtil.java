package org.cloudbus.cloudsim.ex.web.workload.sessions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.ex.web.IterableNumberGenerator;
import org.uncommons.maths.number.NumberGenerator;

/**
 * Miscellaneous utility functions for working with generators
 * 
 * @author nikolay.grozev
 * 
 */
public class GeneratorsUtil {

    private GeneratorsUtil() {
    }

    public static Map<String, NumberGenerator<Double>> parseGenerators(final InputStream in) throws IOException {
	return toGenerators(parseStream(in));
    }

    public static Map<String, NumberGenerator<Double>> toGenerators(final Map<String, List<Double>> values) {
	Map<String, NumberGenerator<Double>> result = new HashMap<>();
	for (Map.Entry<String, List<Double>> e : values.entrySet()) {
	    result.put(e.getKey(), new IterableNumberGenerator<>(e.getValue()));
	}
	return result;
    }

    public static Map<String, List<Double>> parseStream(final InputStream in) throws IOException {
	BufferedReader br = new BufferedReader(new InputStreamReader(in));
	String line = br.readLine();
	List<String> headers = new ArrayList<>();
	for (String s : line.replaceAll("\"", "").split("\\s*,\\s*")) {
	    headers.add(s.trim());
	}

	Map<String, List<Double>> values = new HashMap<String, List<Double>>();

	while ((line = br.readLine()) != null) {
	    List<Double> lineValues = new ArrayList<>();
	    for (String s : line.split("\\s*,\\s*")) {
		lineValues.add(Double.parseDouble(s.replaceAll("\"", "").trim()));
	    }
	    for (int i = 0; i < headers.size(); i++) {
		if (!values.containsKey(headers.get(i))) {
		    values.put(headers.get(i), new ArrayList<Double>());
		}
		values.get(headers.get(i)).add(lineValues.get(i));
	    }
	}
	return values;
    }

}