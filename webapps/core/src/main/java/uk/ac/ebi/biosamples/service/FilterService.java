package uk.ac.ebi.biosamples.service;

import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;


@Service
public class FilterService {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public MultiValueMap<String,String> getFilters(String[] filterStrings) {
		if (filterStrings == null) return new LinkedMultiValueMap<>();
		if (filterStrings.length == 0) return new LinkedMultiValueMap<>();
		//sort the array
		Arrays.sort(filterStrings);
		SortedSet<String> filterStringSet = new TreeSet<>(Arrays.asList(filterStrings));
		//strip the requestParams down to just the selected facet information
		MultiValueMap<String,String> filters = new LinkedMultiValueMap<>();
		for (String filterString : filterStringSet) {
			log.info("looking at filter string '"+filterString+"'");
			if (filterString.contains(":")) {
				String key = filterString.substring(0, filterString.indexOf(":"));
				String value = filterString.substring(filterString.indexOf(":")+1, filterString.length());
				//key = SolrSampleService.attributeTypeToField(key);
				filters.add(key, value);
				log.info("adding filter "+key+" = "+value);
			} else {
				String key=filterString;
				//key = SolrSampleService.attributeTypeToField(key);
				filters.add(key, null);
				log.info("adding filter "+key);
			}
		}
		return filters;
	}
}
